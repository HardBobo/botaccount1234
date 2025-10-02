import java.util.concurrent.TimeUnit;

/**
 * Runs two searches from start position at depth 5:
 *  - Legacy PST evaluation (NNUE disabled via Evaluation.forcePstOnly)
 *  - NNUE evaluation
 * Prints best score and NPS for each.
 */
public final class StartposDepth5Bench {
    public static void main(String[] args) {
        // Initialize Zobrist hashing
        Zobrist.initZobrist();
        // Ensure NNUE is attempted
        try { Nnue.tryAutoLoad(); } catch (Throwable ignored) {}

        // Common setup: start position
        Board.setupStartPosition();

        // Depth to search
        int depth = 5;

        // Legacy PST only
        Evaluation.setForcePstOnly(true);
        MoveFinder.transpositionTable.clear();
        MoveFinder.resetNodeCounter();
        long start = System.nanoTime();
        int pstScore = MoveFinder.negamax(depth, -1000000, 1000000, Board.whiteToMove, 0L);
        long pstTimeNs = System.nanoTime() - start;
        long pstNodes = MoveFinder.getNodeCounter();
        double pstNps = pstNodes / (pstTimeNs / 1e9);

        // NNUE (if available)
        Evaluation.setForcePstOnly(false);
        // Rebuild NNUE incremental accumulators for startpos
        try { if (Nnue.isUsable()) Nnue.rebuildIncremental(); } catch (Throwable ignored) {}
        MoveFinder.transpositionTable.clear();
        MoveFinder.resetNodeCounter();
        start = System.nanoTime();
        int nnueScore = MoveFinder.negamax(depth, -1000000, 1000000, Board.whiteToMove, 0L);
        long nnueTimeNs = System.nanoTime() - start;
        long nnueNodes = MoveFinder.getNodeCounter();
        double nnueNps = nnueNodes / (nnueTimeNs / 1e9);

        System.out.println("=== Startpos Depth 5 ===");
        System.out.printf("PST:  score=%d  time=%d ms  nodes=%d  NPS=%.0f\n",
                pstScore, TimeUnit.NANOSECONDS.toMillis(pstTimeNs), pstNodes, pstNps);
        if (Nnue.isUsable()) {
            System.out.printf("NNUE: score=%d  time=%d ms  nodes=%d  NPS=%.0f\n",
                    nnueScore, TimeUnit.NANOSECONDS.toMillis(nnueTimeNs), nnueNodes, nnueNps);
        } else {
            System.out.println("NNUE: not loaded");
        }
    }
}
