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

    /** Rebuild incremental accumulators from the current Board state. */
    public static void rebuildIncremental() {
        if (!usable || NET == null) return;
        BoardApi boardApi = new BoardAdapter(Board.whiteToMove);
        NET.rebuildIncrementalFromBoard(boardApi);
    }

    /** Apply NNUE incremental delta for an executed move (call after bitboards apply). */
    public static void onMoveApplied(Zug z, MoveInfo info) {
        if (!usable || NET == null) return;
        NET.onMoveApplied(z, info);
    }

    /** Undo NNUE incremental delta for a reverted move (call after bitboards undo). */
    public static void onMoveUndone(Zug z, MoveInfo info) {
        if (!usable || NET == null) return;
        NET.onMoveUndone(z, info);
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

        // Incremental accumulator state (absolute, STM-independent)
        private static final class Inc {
            short[] stmAcc; // absolute mapping 'stm' bucket
            short[] ntmAcc; // absolute mapping 'ntm' bucket
            Inc(int H) { this.stmAcc = new short[H]; this.ntmAcc = new short[H]; }
        }

        final int H;                // hidden size
        final short[][] l0w;        // [768][H] feature weights (feature-major)
        final short[] l0b;          // H bias
        final short[] l1w;          // 2 * H output weights
        final short  l1b;           // 1 output bias

        private NnueNetwork(int H, short[][] l0w, short[] l0b, short[] l1w, short l1b) {
            this.H = H; this.l0w = l0w; this.l0b = l0b; this.l1w = l1w; this.l1b = l1b;
        }

        // Current incremental accumulators (built from Board), or null if not initialized
        private Inc inc;

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
            // Load weights in column-major order (feature-major) as bullet stores them
            short[][] l0w = new short[768][H]; // [feature][hidden]
            for (int feat = 0; feat < 768; feat++) {
                for (int h = 0; h < H; h++) {
                    l0w[feat][h] = bb.getShort();
                }
            }
            short[] l0b = new short[H];
            for (int i = 0; i < H; i++) l0b[i] = bb.getShort();
            short[] l1w = new short[2 * H];
            for (int i = 0; i < 2 * H; i++) l1w[i] = bb.getShort();
            short l1b = bb.getShort();
            return new NnueNetwork(H, l0w, l0b, l1w, l1b);
        }

        int evaluate(BoardApi board, String mappingMode) {
            // If incremental accumulators are available, use them (absolute, STM-independent)
            if (inc != null) {
                boolean whiteToMove = board.sideToMoveIsWhite();
                short[] us = whiteToMove ? inc.stmAcc : inc.ntmAcc;
                short[] them = whiteToMove ? inc.ntmAcc : inc.stmAcc;
                return evaluateBuckets(us, them);
            }

            // Fallback: build STM-aware accumulators on the fly (slower)
            short[] stmAcc = new short[H];
            short[] ntmAcc = new short[H];
            System.arraycopy(l0b, 0, stmAcc, 0, H);
            System.arraycopy(l0b, 0, ntmAcc, 0, H);

            boolean whiteToMove = board.sideToMoveIsWhite();
            
            // Extract features exactly as bullet Chess768 does (STM-aware)
            board.forEachPiece((piece, sq) -> {
                int bulletPiece = (piece.isWhite ? 0 : 8) + piece.typeIndex;
                boolean pieceIsStm = (whiteToMove && piece.isWhite) || (!whiteToMove && !piece.isWhite);
                int cRel = pieceIsStm ? 0 : 1;
                int pc = 64 * (bulletPiece & 7);
                int sqA1 = sq ^ 56;
                int sqStm = whiteToMove ? sqA1 : (sqA1 ^ 56);
                int stmIdx = (cRel == 0 ? 0 : 384) + pc + sqStm;
                int ntmIdx = (cRel == 0 ? 384 : 0) + pc + (sqStm ^ 56);
                for (int h = 0; h < H; h++) {
                    stmAcc[h] += l0w[stmIdx][h];
                    ntmAcc[h] += l0w[ntmIdx][h];
                }
            });

            return evaluateBuckets(stmAcc, ntmAcc);
        }

        private int evaluateBuckets(short[] usAcc, short[] themAcc) {
            long output = 0;
            for (int h = 0; h < H; h++) output += (long)screlu(usAcc[h]) * (long)l1w[h];
            for (int h = 0; h < H; h++) output += (long)screlu(themAcc[h]) * (long)l1w[H + h];
            output /= QA;
            output += l1b;
            output = output * SCALE / ((long)QA * (long)QB);
            if (output > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            if (output < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            return (int)output;
        }
        private static int screlu(short x) {
            int y = x;
            if (y < 0) y = 0; if (y > QA) y = QA; return y * y;
        }

        // ---- Incremental API ----
        void rebuildIncrementalFromBoard(BoardApi board) {
            inc = new Inc(H);
            // start from bias
            System.arraycopy(l0b, 0, inc.stmAcc, 0, H);
            System.arraycopy(l0b, 0, inc.ntmAcc, 0, H);
            // Add all pieces with absolute mapping (STM-independent)
            board.forEachPiece((piece, sq) -> addPieceAbs(piece.isWhite, piece.typeIndex, sq));
        }

        private void addPieceAbs(boolean isWhite, int pieceType, int sq) {
            int c = isWhite ? 0 : 1;
            int pc = 64 * pieceType;
            int sqA1 = sq ^ 56;
            int stmIdx = (c == 0 ? 0 : 384) + pc + sqA1;
            int ntmIdx = (c == 0 ? 384 : 0) + pc + (sqA1 ^ 56);
            for (int h = 0; h < H; h++) {
                inc.stmAcc[h] += l0w[stmIdx][h];
                inc.ntmAcc[h] += l0w[ntmIdx][h];
            }
        }

        private void removePieceAbs(boolean isWhite, int pieceType, int sq) {
            int c = isWhite ? 0 : 1;
            int pc = 64 * pieceType;
            int sqA1 = sq ^ 56;
            int stmIdx = (c == 0 ? 0 : 384) + pc + sqA1;
            int ntmIdx = (c == 0 ? 384 : 0) + pc + (sqA1 ^ 56);
            for (int h = 0; h < H; h++) {
                inc.stmAcc[h] -= l0w[stmIdx][h];
                inc.ntmAcc[h] -= l0w[ntmIdx][h];
            }
        }

        void onMoveApplied(Zug z, MoveInfo info) {
            if (inc == null) return;
            int from = infoSquareFrom(z);
            int to = infoSquareTo(z);
            boolean moverW = info.movingPieceWhite;
            int movingType = info.movingPieceType;

            // Remove moving piece at from
            removePieceAbs(moverW, movingType, from);

            // Handle captures (normal or en passant)
            if (!info.squareMovedOntoWasEmpty || info.wasEnPassant) {
                int capSq;
                if (info.wasEnPassant && info.capEnPassantBauerCoords != null) {
                    capSq = info.capEnPassantBauerCoords.y * 8 + info.capEnPassantBauerCoords.x;
                } else {
                    capSq = to;
                }
                removePieceAbs(info.capturedPieceWhite, info.capturedPieceType, capSq);
            }

            // Handle rook move in castling
            if (info.rookMoved) {
                int y = moverW ? 7 : 0;
                int rookFrom = Bitboards.sq(info.rookStartX, y);
                int rookTo   = Bitboards.sq(info.rookEndX, y);
                removePieceAbs(moverW, 3, rookFrom);
                addPieceAbs(moverW, 3, rookTo);
            }

            // Add moving piece at destination (promotion if any)
            if (info.wasPromotion) {
                addPieceAbs(moverW, info.promotionType, to);
            } else {
                addPieceAbs(moverW, movingType, to);
            }
        }

        void onMoveUndone(Zug z, MoveInfo info) {
            if (inc == null) return;
            int from = infoSquareFrom(z);
            int to = infoSquareTo(z);
            boolean moverW = info.movingPieceWhite;
            int movingType = info.movingPieceType;

            // Undo moving piece at destination
            if (info.wasPromotion) {
                removePieceAbs(moverW, info.promotionType, to);
                addPieceAbs(moverW, 0, from); // restore pawn
            } else {
                removePieceAbs(moverW, movingType, to);
                addPieceAbs(moverW, movingType, from);
            }

            // Restore rook for castling
            if (info.rookMoved) {
                int y = moverW ? 7 : 0;
                int rookFrom = Bitboards.sq(info.rookStartX, y);
                int rookTo   = Bitboards.sq(info.rookEndX, y);
                removePieceAbs(moverW, 3, rookTo);
                addPieceAbs(moverW, 3, rookFrom);
            }

            // Restore captured piece
            if (info.wasEnPassant && info.capEnPassantBauerCoords != null) {
                int capSq = info.capEnPassantBauerCoords.y * 8 + info.capEnPassantBauerCoords.x;
                addPieceAbs(!moverW, 0, capSq);
            } else if (!info.squareMovedOntoWasEmpty) {
                addPieceAbs(info.capturedPieceWhite, info.capturedPieceType, to);
            }
        }

        private static int infoSquareFrom(Zug z) { return z.startY * 8 + z.startX; }
        private static int infoSquareTo(Zug z) { return z.endY * 8 + z.endX; }
    }
}
