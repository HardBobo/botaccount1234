import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Minimal CLI to load a FEN and print NNUE evaluation in centipawns.
 * Usage examples (from project root or src):
 *   javac src\*.java && java -cp src NnueEval "<FEN string>"
 *   javac *.java && java NnueEval "<FEN string>"
 * Optional network path:
 *   java NnueEval --net "path/to/quantised.bin" "<FEN>"
 */
public final class NnueEval {
    public static void main(String[] args) throws IOException {
        String netPathOverride = null;
        String fenArg = null;

        // Parse optional --net <path> and fen
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--net".equals(a) || "--path".equals(a)) {
                if (i + 1 < args.length) { netPathOverride = args[++i]; }
            } else {
                // First non-flag is FEN; if more remain, join with space
                StringBuilder sb = new StringBuilder(a);
                for (int j = i + 1; j < args.length; j++) {
                    sb.append(' ').append(args[j]);
                }
                fenArg = sb.toString();
                break;
            }
        }

        // If no FEN in args, read one line from stdin
        if (fenArg == null) {
            System.out.println("Enter FEN on a single line:");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            fenArg = br.readLine();
            if (fenArg == null || fenArg.isBlank()) {
                System.err.println("No FEN provided.");
                return;
            }
        }

        // Load NNUE: try config/env, optional override, and common fallback locations
        boolean loaded = false;
        try { Nnue.tryAutoLoad(); } catch (Throwable ignored) {}
        if (!Nnue.isUsable() && netPathOverride != null) {
            loaded = Nnue.loadFromPath(netPathOverride);
        }
        if (!Nnue.isUsable()) {
            // Try project-root nnue folder
            File rootNet = new File("nnue/quantised.bin");
            if (rootNet.exists()) loaded = Nnue.loadFromPath(rootNet.getPath());
        }
        if (!Nnue.isUsable()) {
            // If running from src, try parent nnue folder
            File parentNet = new File("../nnue/quantised.bin");
            if (parentNet.exists()) loaded = Nnue.loadFromPath(parentNet.getPath());
        }
        if (!Nnue.isUsable() && !loaded) {
            System.err.println("Could not load NNUE; set nnue.enabled=true and nnue.path in bot.properties, or pass --net <path>.");
            return;
        }

        // Load FEN into engine bitboards
        Board.loadFEN(fenArg.trim());

        // Evaluate from side-to-move perspective
        int evalCp = Nnue.evaluate(Board.whiteToMove);
        System.out.println("FEN: " + fenArg);
        System.out.println("Side to move: " + (Board.whiteToMove ? "w" : "b"));
        System.out.println("NNUE eval (cp): " + evalCp);
    }
}