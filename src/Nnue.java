import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * NNUE integration scaffold for the engine.
 *
 * This class wires up optional NNUE evaluation. Once a supported network format
 * is confirmed, the parser and forward pass will be implemented here.
 */
public final class Nnue {
    private static volatile boolean usable = false; // becomes true once a network is loaded and supported
    private static volatile String loadedPath = null;

    // Minimal placeholders for network state; to be replaced with real structures
    private static byte[] rawNetworkBytes = null;

    private Nnue() {}

    public static boolean isUsable() {
        return usable;
    }

    public static String getLoadedPath() {
        return loadedPath;
    }

    /**
     * Attempt to auto-load NNUE if enabled via config/env.
     * Safe to call multiple times.
     */
    public static synchronized void tryAutoLoad() {
        Config cfg = Config.getInstance();
        if (!cfg.isNnueEnabled()) {
            // Explicitly disabled
            return;
        }
        if (usable) return; // already loaded
        String path = cfg.getNnuePath();
        if (path == null) {
            System.err.println("NNUE enabled but no path configured (nnue.path or NNUE_PATH)");
            return;
        }
        loadFromPath(path);
    }

    /**
     * Load an NNUE network from disk. Currently only validates presence and stores raw bytes.
     * A real parser will replace this once the file format is confirmed.
     */
    public static synchronized boolean loadFromPath(String path) {
        File f = new File(path);
        if (!f.exists() || !f.isFile()) {
            System.err.println("NNUE file not found: " + path);
            usable = false;
            loadedPath = null;
            rawNetworkBytes = null;
            return false;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            long len = f.length();
            if (len <= 0 || len > Integer.MAX_VALUE) {
                System.err.println("NNUE file has unsupported size: " + len);
                usable = false;
                loadedPath = null;
                rawNetworkBytes = null;
                return false;
            }
            byte[] data = new byte[(int) len];
            int read = fis.read(data);
            if (read != len) {
                System.err.println("Failed to read full NNUE file: read=" + read + " expected=" + len);
                usable = false;
                loadedPath = null;
                rawNetworkBytes = null;
                return false;
            }

            // Placeholder minimal sanity checks could go here (e.g., magic header)
            // For now, just stash the bytes and mark not usable until parser implemented
            rawNetworkBytes = data;
            loadedPath = f.getAbsolutePath();
            usable = false; // keep false until we implement a real parser
            System.out.println("NNUE file loaded (raw), awaiting parser implementation: " + loadedPath);
            return true;
        } catch (IOException e) {
            System.err.println("Error loading NNUE file: " + e.getMessage());
            usable = false;
            loadedPath = null;
            rawNetworkBytes = null;
            return false;
        }
    }

    /**
     * Evaluate the current board from the perspective of isWhite using NNUE.
     * Precondition: isUsable() == true
     */
    public static int evaluate(boolean isWhite) {
        if (!usable) {
            throw new IllegalStateException("NNUE evaluate called but NNUE is not usable");
        }
        // TODO: Implement forward pass once format is confirmed.
        // Returning 0 as a placeholder is dangerous for strength, so keep NNUE unusable instead.
        return 0;
    }
}
