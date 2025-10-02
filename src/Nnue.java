import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * NNUE integration for the engine using the "simple" bullet layout:
 *   - l0w:  HIDDEN_SIZE x 768 i16, column-major by feature
 *   - l0b:  HIDDEN_SIZE i16
 *   - l1w:  (2 * HIDDEN_SIZE) i16
 *   - l1b:  1 i16
 * File is padded to a multiple of 64 bytes (ignored).
 * Quantisation constants: QA=255, QB=64, SCALE=400.
 */
public final class Nnue {
    private static volatile boolean usable = false;     // true when network parsed OK
    private static volatile String loadedPath = null;
    private static volatile NnueNetwork NET = null;
    private static volatile boolean FLIP_SIGN = false;
    private static volatile String MAPPING = "direct"; // direct | premirror

    private Nnue() {}

    public static boolean isUsable() {
        return usable;
    }

    public static String getLoadedPath() {
        return loadedPath;
    }

    /** Attempt to auto-load NNUE from config/env; safe to call multiple times. */
    public static synchronized void tryAutoLoad() {
        Config cfg = Config.getInstance();
        if (!cfg.isNnueEnabled()) return;
        if (usable) return;
        // Read debug knobs as well
        FLIP_SIGN = cfg.getNnueFlipSign();
        MAPPING = cfg.getNnueMapping();
        String path = cfg.getNnuePath();
        if (path == null) {
            System.err.println("NNUE enabled but no path configured (nnue.path or NNUE_PATH)");
            return;
        }
        loadFromPath(path);
    }

    /** Load and parse quantised.bin written by bullet. */
    public static synchronized boolean loadFromPath(String path) {
        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            System.err.println("NNUE file not found: " + path);
            usable = false; loadedPath = null; NET = null; return false;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            long len = f.length();
            if (len <= 0 || len > Integer.MAX_VALUE) {
                System.err.println("NNUE file has unsupported size: " + len);
                usable = false; loadedPath = null; NET = null; return false;
            }
            byte[] data = new byte[(int) len];
            int read = fis.read(data);
            if (read != len) {
                System.err.println("Failed to read full NNUE file: read=" + read + " expected=" + len);
                usable = false; loadedPath = null; NET = null; return false;
            }
            NnueNetwork net = NnueNetwork.loadRaw(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
            NET = net;
            loadedPath = f.getAbsolutePath();
            // Refresh debug knobs from config at load time
            Config cfg = Config.getInstance();
            FLIP_SIGN = cfg.getNnueFlipSign();
            MAPPING = cfg.getNnueMapping();
            usable = true;
            System.out.println("NNUE loaded: H=" + net.H + " from " + loadedPath + " (mapping=" + MAPPING + ", flipSign=" + FLIP_SIGN + ")");
            return true;
        } catch (Exception e) {
            System.err.println("Error loading NNUE file: " + e.getMessage());
            usable = false; loadedPath = null; NET = null; return false;
        }
    }

    /** Evaluate the current Board from the perspective of isWhite. */
    public static int evaluate(boolean isWhite) {
        if (!usable || NET == null) {
            throw new IllegalStateException("NNUE evaluate called but NNUE is not usable");
        }
        BoardApi boardApi = new BoardAdapter(isWhite);
        int cp = NET.evaluate(boardApi, MAPPING);
        return FLIP_SIGN ? -cp : cp;
    }

    /** Adapter from engine bitboards to NNUE BoardApi. */
    private static final class BoardAdapter implements BoardApi {
        private final boolean stmWhite;
        BoardAdapter(boolean isWhiteToMove) { this.stmWhite = isWhiteToMove; }
        @Override public boolean sideToMoveIsWhite() { return stmWhite; }
        @Override public void forEachPiece(PieceConsumer consumer) {
            // White pieces
            for (int t = 0; t < 6; t++) {
                long bb = Board.bitboards.w[t];
                while (bb != 0) {
                    int sq = Long.numberOfTrailingZeros(bb);
                    bb &= bb - 1;
                    consumer.accept(new Piece(true, t), sq);
                }
            }
            // Black pieces
            for (int t = 0; t < 6; t++) {
                long bb = Board.bitboards.b[t];
                while (bb != 0) {
                    int sq = Long.numberOfTrailingZeros(bb);
                    bb &= bb - 1;
                    consumer.accept(new Piece(false, t), sq);
                }
            }
        }
    }

    // --- Minimal interfaces for the evaluator ---
    public interface BoardApi {
        boolean sideToMoveIsWhite();
        void forEachPiece(PieceConsumer consumer);
    }
    public interface PieceConsumer { void accept(Piece piece, int sq0to63); }
    public static final class Piece {
        public final boolean isWhite; public final int typeIndex; // 0=P..5=K
        public Piece(boolean isWhite, int typeIndex) { this.isWhite = isWhite; this.typeIndex = typeIndex; }
    }

    // --- The network representation and evaluator ---
    private static final class NnueNetwork {
        // Quantisation constants from simple.rs
        private static final int QA = 255;
        private static final int QB = 64;
        private static final int SCALE = 400;

        final int H;                // hidden size
        final short[] l0w;          // H * 768 (column-major)
        final short[] l0b;          // H
        final short[] l1w;          // 2 * H
        final short  l1b;           // 1

        private NnueNetwork(int H, short[] l0w, short[] l0b, short[] l1w, short l1b) {
            this.H = H; this.l0w = l0w; this.l0b = l0b; this.l1w = l1w; this.l1b = l1b;
        }

        static NnueNetwork loadRaw(ByteBuffer bb) {
            int totalBytes = bb.remaining();
            int bytesNoPad = totalBytes - (totalBytes % 64); // quantised.bin is padded to 64 bytes
            int totalShorts = bytesNoPad / 2;
            // layout shorts = 768*H + H + 2H + 1 = 771H + 1
            if (totalShorts < 1 || (totalShorts - 1) % 771 != 0) {
                // fallback to H=128 if inference fails
                return loadRawWithH(bb, 128);
            }
            int H = (totalShorts - 1) / 771;
            return loadRawWithH(bb, H);
        }

        private static NnueNetwork loadRawWithH(ByteBuffer bb, int H) {
            bb.rewind();
            short[] l0w = new short[H * 768];
            for (int i = 0; i < l0w.length; i++) l0w[i] = bb.getShort();
            short[] l0b = new short[H];
            for (int i = 0; i < H; i++) l0b[i] = bb.getShort();
            short[] l1w = new short[2 * H];
            for (int i = 0; i < 2 * H; i++) l1w[i] = bb.getShort();
            short l1b = bb.getShort();
            return new NnueNetwork(H, l0w, l0b, l1w, l1b);
        }

        int evaluate(BoardApi board, String mappingMode) {
            // Initialize accumulators with bias (exact copy from bullet_eval)
            short[] stmAcc = new short[H];
            short[] ntmAcc = new short[H];
            System.arraycopy(l0b, 0, stmAcc, 0, H);
            System.arraycopy(l0b, 0, ntmAcc, 0, H);

            // Use exact bullet Chess768 feature mapping (ignore mappingMode)
            board.forEachPiece((piece, sq) -> {
                // Recreate bullet piece encoding: bit 3 = color, bits 0-2 = type 
                int bulletPiece = (piece.isWhite ? 0 : 8) + piece.typeIndex;
                
                // Extract color and piece type offset exactly as bullet does
                int c = (bulletPiece & 8) > 0 ? 1 : 0; // 0=white, 1=black
                int pc = 64 * (bulletPiece & 7);       // piece type offset
                
                // Calculate STM and NTM indices exactly as bullet Chess768
                int stmIdx = (c == 0 ? 0 : 384) + pc + sq;      // [0,384][c] + pc + sq
                int ntmIdx = (c == 0 ? 384 : 0) + pc + (sq ^ 56); // [384,0][c] + pc + (sq^56)
                
                // Add features to accumulators (column-major: H * feature + row)
                for (int h = 0; h < H; h++) {
                    stmAcc[h] += l0w[H * stmIdx + h];
                    ntmAcc[h] += l0w[H * ntmIdx + h];
                }
            });

            // Compute final evaluation exactly as bullet_eval
            long output = 0;
            
            // STM accumulator part
            for (int h = 0; h < H; h++) {
                output += (long)screlu(stmAcc[h]) * (long)l1w[h];
            }
            
            // NTM accumulator part  
            for (int h = 0; h < H; h++) {
                output += (long)screlu(ntmAcc[h]) * (long)l1w[H + h];
            }
            
            // Reduce quantization from QA*QA*QB to QA*QB
            output /= QA;
            
            // Add bias
            output += l1b;
            
            // Apply eval scale and remove quantization
            output *= SCALE;
            output /= (long)QA * (long)QB;
            
            // Clamp to int range
            if (output > Integer.MAX_VALUE) output = Integer.MAX_VALUE;
            if (output < Integer.MIN_VALUE) output = Integer.MIN_VALUE;
            
            return (int)output;
        }

        private static int screlu(short x) {
            int y = x;
            if (y < 0) y = 0; if (y > QA) y = QA; return y * y;
        }
    }
}
