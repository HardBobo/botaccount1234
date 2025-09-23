import java.util.ArrayList;

public class TimeManager {
    // Compute per-move think time (milliseconds) based on remaining time, increment, and position complexity.
    // - Avoids flag by reserving a safety buffer.
    // - Allocates more time for complex positions (more legal moves, checks/captures), less for simple ones.
    public static long computeThinkTimeMs(Piece[][] board, boolean whiteToMove, long timeLeftMs, long incMs, int plyCountSoFar) {
        // Fallbacks if time not provided
        if (timeLeftMs <= 0) return Math.max(10, (int)(incMs * 0.2));

        // Estimate moves to go depending on phase (use piece count as proxy)
        int whitePieces = Board.pieceTracker.getPieceCount(true);
        int blackPieces = Board.pieceTracker.getPieceCount(false);
        int totalPieces = whitePieces + blackPieces; // includes pawns
        int movesToGo; // approximate remaining moves for this side
        if (totalPieces >= 24) movesToGo = 35;        // opening / early middlegame
        else if (totalPieces >= 14) movesToGo = 25;   // middlegame
        else movesToGo = 16;                          // endgame

        // Base allocation from remaining time plus a portion of increment
        // Less conservative when there's time: at least ~5% of remaining time
        long baseAlloc = Math.max(timeLeftMs / Math.max(10, movesToGo), (long)(timeLeftMs * 0.05));
        long incPortion = (long)(incMs * 0.8);
        long alloc = baseAlloc + incPortion;

        // Complexity factor based on number of legal moves and checks/captures
        ArrayList<Zug> pseudo = MoveFinder.possibleMoves(whiteToMove, board);
        // Filter illegal moves
        pseudo.removeIf(z -> !MoveFinder.isLegalMove(z, board, whiteToMove));
        int legal = pseudo.size();
        // Baseline moves ~30; clamp factor between 0.6 and 1.8
        double factor = clamp(0.6, 1.8, legal / 30.0);
        // Slightly boost if in check (tactical complexity)
        if (MoveFinder.inCheck(board, whiteToMove)) factor = Math.min(1.8, factor + 0.2);
        // Slightly boost if there are many captures/promotions among legals
        int forcing = 0;
        for (Zug z : pseudo) {
            if (MoveFinder.isCapture(board, z) || MoveFinder.promotionQ(z, board)) forcing++;
        }
        if (forcing > legal / 3) factor = Math.min(1.8, factor + 0.15);

        alloc = (long)(alloc * factor);

        // Safety rules to avoid flag
        long safetyBuffer = Math.max(100, incMs / 2); // keep a small margin
        long maxSpend = Math.max(50, (long)(timeLeftMs * 0.6)); // allow up to 60% in extreme cases
        long calmFloor = Math.min(800, Math.max(300, timeLeftMs / 40)); // e.g., at 20s -> 500ms
        long minSpend = calmFloor;

        // Panic mode: very low on time
        if (timeLeftMs < Math.max(4000, incMs * 2)) {
            // under ~4s left, prefer faster moves but still consider increment
            alloc = Math.min(alloc, Math.max(80, (long)(incMs * 1.0)));
        }

        // Apply clamps
        alloc = clamp(minSpend, Math.min(maxSpend, timeLeftMs - safetyBuffer), alloc);

        return Math.max(10, alloc);
    }

    private static long clamp(long min, long max, long v) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static double clamp(double min, double max, double v) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}