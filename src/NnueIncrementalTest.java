import java.util.*;

/**
 * NnueIncrementalTest
 *
 * Compares NNUE incremental evaluation against a full recompute over a
 * sequence of random legal moves from a given position.
 *
 * Usage (PowerShell/CMD):
 *   java -cp out NnueIncrementalTest [FEN|startpos] [steps] [seed]
 *
 * Defaults:
 *   position = startpos
 *   steps    = 100
 *   seed     = random
 */
public class NnueIncrementalTest {
    public static void main(String[] args) {
        try {
            // Load NNUE
            Nnue.tryAutoLoad();
            if (!Nnue.isUsable()) {
                System.err.println("NNUE not usable. Ensure NNUE_ENABLED=1 and NNUE_PATH set, or bot.properties configured.");
                return;
            }

            // Parse arguments
            String fen = null;
            int steps = 100;
            Long seed = null;

            if (args.length > 0) {
                // Join all args first, then try to split last two as steps/seed if numeric
                String joined = String.join(" ", args).trim();
                // Try to pull trailing numbers for steps/seed
                String[] parts = joined.split("\\s+");
                try {
                    // Try last token as seed
                    long s = Long.parseLong(parts[parts.length - 1]);
                    seed = s;
                    // Try prior token as steps
                    if (parts.length >= 2) {
                        int st = Integer.parseInt(parts[parts.length - 2]);
                        steps = st;
                        // Remaining prefix is fen or 'startpos'
                        fen = String.join(" ", Arrays.copyOf(parts, parts.length - 2));
                    } else {
                        // Only seed provided; then fen=joined
                        fen = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
                    }
                } catch (NumberFormatException e) {
                    // No trailing numbers; treat entire joined as fen or 'startpos'
                    fen = joined;
                }
            }

            boolean isWhite;
            if (fen == null || fen.isEmpty() || fen.equalsIgnoreCase("startpos")) {
                Board.setupStartPosition();
                isWhite = true;
            } else {
                Board.loadFEN(fen);
                isWhite = Board.whiteToMove;
            }

            // Initialize Zobrist keys once before hashing
            Zobrist.initZobrist();

            // Build incremental state from current board
            Nnue.rebuildIncremental();

            // Initial evals
            int inc0 = Nnue.evaluate(isWhite);
            int full0 = evalFullViaReflection(isWhite);
            System.out.println("Initial eval inc=" + inc0 + " full=" + full0 + " diff=" + (inc0 - full0));

            Random rng = (seed != null) ? new Random(seed) : new Random();

            long hash = Zobrist.computeHash(Board.bitboards, isWhite);

            int posCount = 1; // include initial position
            long sumAbsDiff = Math.abs(inc0 - full0);
            int maxAbsDiff = Math.abs(inc0 - full0);
            int bigDiffs = (Math.abs(inc0 - full0) > 4) ? 1 : 0; // threshold 4 cp

            // Stack for undo
            Deque<Zug> moveStack = new ArrayDeque<>();
            Deque<MoveInfo> infoStack = new ArrayDeque<>();
            Deque<Long> hashStack = new ArrayDeque<>();

            // Walk a random path of legal moves; at each ply compare inc vs full
            for (int i = 0; i < steps; i++) {
                ArrayList<Zug> moves = MoveFinder.possibleMoves(isWhite);
                // Filter illegal moves without lambdas (to avoid effectively-final capture issues)
                for (Iterator<Zug> it = moves.iterator(); it.hasNext(); ) {
                    Zug mz = it.next();
                    if (!Board.bitboards.isLegalMove(mz, isWhite)) it.remove();
                }
                if (moves.isEmpty()) {
                    System.out.println("No legal moves at step " + i + ", stopping.");
                    break;
                }

                Zug z = moves.get(rng.nextInt(moves.size()));
                MoveInfo inf = MoveFinder.saveMoveInfo(z);
                hashStack.push(hash);
                moveStack.push(z);
                infoStack.push(inf);

                hash = MoveFinder.doMoveUpdateHash(z, inf, hash);

                // Eval after move
                int inc = Nnue.evaluate(!isWhite);
                int full = evalFullViaReflection(!isWhite);
                int diff = inc - full;

                posCount++;
                sumAbsDiff += Math.abs(diff);
                if (Math.abs(diff) > maxAbsDiff) maxAbsDiff = Math.abs(diff);
                if (Math.abs(diff) > 4) bigDiffs++;

                // Advance side
                isWhite = !isWhite;
            }

            // Undo all moves to clean up
            while (!moveStack.isEmpty()) {
                Zug z = moveStack.pop();
                MoveInfo inf = infoStack.pop();
                MoveFinder.undoMove(z, inf);
                hash = hashStack.pop();
                isWhite = !isWhite;
            }

            double avgAbs = posCount > 0 ? (sumAbsDiff / (double) posCount) : 0.0;
            System.out.println("Positions checked: " + posCount);
            System.out.println("Average |inc - full|: " + String.format("%.2f", avgAbs) + " cp");
            System.out.println("Max |inc - full|: " + maxAbsDiff + " cp");
            System.out.println(">4 cp diffs: " + bigDiffs);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // Use reflection to force a full recompute path inside NNUE by temporarily
    // nulling the private 'inc' field on the loaded Nnue.NET instance.
    private static int evalFullViaReflection(boolean isWhite) throws Exception {
        // Access Nnue.NET
        Class<?> clsNnue = Nnue.class;
        java.lang.reflect.Field fNET = clsNnue.getDeclaredField("NET");
        fNET.setAccessible(true);
        Object net = fNET.get(null);
        if (net == null) throw new IllegalStateException("NNUE NET is null");

        // Flip inc to null, evaluate, then restore
        java.lang.reflect.Field fInc = net.getClass().getDeclaredField("inc");
        fInc.setAccessible(true);
        Object oldInc = fInc.get(net);
        try {
            fInc.set(net, null);
            return Nnue.evaluate(isWhite);
        } finally {
            fInc.set(net, oldInc);
        }
    }
}
