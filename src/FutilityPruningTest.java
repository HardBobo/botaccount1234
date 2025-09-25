import java.lang.reflect.Field;
import java.util.ArrayList;

public class FutilityPruningTest {

    private static void resetNodeCount() throws Exception {
        Field f = MoveFinder.class.getDeclaredField("nodeCount");
        f.setAccessible(true);
        f.setLong(null, 0L);
    }
    private static long getNodeCount() throws Exception {
        Field f = MoveFinder.class.getDeclaredField("nodeCount");
        f.setAccessible(true);
        return f.getLong(null);
    }

    private static void printHeader(String title){
        System.out.println();
        System.out.println("==== " + title + " ====");
    }

    public static void main(String[] args) throws Exception {
        Zobrist.initZobrist();
        MoveFinder.setSearchDeadline(Long.MAX_VALUE);

        // Position: start position (quiet, not in check)
        Board.brett = new Piece[8][8];
        Board.setupBoard(Board.brett);
        long hash = Zobrist.computeHash(Board.brett, true);

        printHeader("Frontier Futility (depth 1) should prune");
        // Baseline (no futility trigger): extremely low alpha
        resetNodeCount();
        MoveFinder.transpositionTable.clear();
        MoveFinder.negamax(Board.brett, 1, Integer.MIN_VALUE / 2 + 1, Integer.MAX_VALUE / 2 - 1, true, hash);
        long nodesBaselineD1 = getNodeCount();
        System.out.println("Depth 1 baseline nodes: " + nodesBaselineD1);

        // Futility trigger: high alpha so staticEval + 300 <= alpha likely holds, but far from mate range
        int eval = Evaluation.evaluation(Board.brett, true);
        int alphaHigh = eval + 10000; // high enough for pruning, but not near mate
        resetNodeCount();
        MoveFinder.transpositionTable.clear();
        MoveFinder.negamax(Board.brett, 1, alphaHigh, alphaHigh + 1000, true, hash);
        long nodesPrunedD1 = getNodeCount();
        System.out.println("Depth 1 pruned nodes:  " + nodesPrunedD1);
        System.out.println("D1 pruned < baseline?  " + (nodesPrunedD1 < nodesBaselineD1));

        printHeader("Extended Futility (depth 2) should prune");
        // Baseline depth 2
        resetNodeCount();
        MoveFinder.transpositionTable.clear();
        MoveFinder.negamax(Board.brett, 2, Integer.MIN_VALUE / 2 + 1, Integer.MAX_VALUE / 2 - 1, true, hash);
        long nodesBaselineD2 = getNodeCount();
        System.out.println("Depth 2 baseline nodes: " + nodesBaselineD2);

        // Futility depth 2 trigger (staticEval + 500 <= alpha)
        int alphaHighD2 = eval + 10000;
        resetNodeCount();
        MoveFinder.transpositionTable.clear();
        MoveFinder.negamax(Board.brett, 2, alphaHighD2, alphaHighD2 + 1000, true, hash);
        long nodesPrunedD2 = getNodeCount();
        System.out.println("Depth 2 pruned nodes:  " + nodesPrunedD2);
        System.out.println("D2 pruned < baseline?  " + (nodesPrunedD2 < nodesBaselineD2));

        printHeader("Move-level Futility at depth 1 (skip quiet, non-check moves)");
        // Choose alpha to avoid node-level futility (alpha < eval + 300) but enable move-level (alpha >= eval + 150)
        int alphaForMoveLevel = eval + 200; // < eval+300, >= eval+150

        // Baseline (very low alpha) - explores all legal moves (no move-level pruning)
        resetNodeCount();
        MoveFinder.transpositionTable.clear();
        MoveFinder.negamax(Board.brett, 1, Integer.MIN_VALUE / 2 + 1, Integer.MAX_VALUE / 2 - 1, true, hash);
        long nodesBaselineMoveLevel = getNodeCount();
        System.out.println("Depth 1 baseline nodes (move-level): " + nodesBaselineMoveLevel);

        // With move-level futility enabled at depth 1
        resetNodeCount();
        MoveFinder.transpositionTable.clear();
        MoveFinder.negamax(Board.brett, 1, alphaForMoveLevel, alphaForMoveLevel + 1000, true, hash);
        long nodesMoveLevel = getNodeCount();
        System.out.println("Depth 1 move-level nodes:            " + nodesMoveLevel);
        System.out.println("D1 move-level pruned < baseline?     " + (nodesMoveLevel < nodesBaselineMoveLevel));

        System.out.println();
        System.out.println("Note: These are functional checks. Final effectiveness should be evaluated by speed-ups and playing strength tests.");
    }
}
