import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

public class PerftMoveGenTest {
    private static long startTime = 0;
    private static long startHash;
    public static void main(String[] args) throws NoSuchAlgorithmException {
        Board.loadFEN("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - ");
        
        System.out.println("Running Perft test with PieceTracker optimization...");
        System.out.println("White pieces: " + Long.bitCount(Board.bitboards.occW));
        System.out.println("Black pieces: " + Long.bitCount(Board.bitboards.occB));
        System.out.println();

        Zobrist.initZobrist();
        startHash = Zobrist.computeHash(Board.bitboards, true);
        
        perftDivide(null, 5, true, startHash); // Will show all root moves and their node counts
    }

    public static void perftDivide(Piece [][] board, int depth, boolean isWhite, long hash) throws NoSuchAlgorithmException {
        startTime = System.currentTimeMillis();

        ArrayList<Zug> moves = MoveFinder.possibleMoves(isWhite, board);

        moves.removeIf(zug -> !Board.bitboards.isLegalMove(zug, isWhite));

        MoveOrdering.orderMoves(moves, board, isWhite);

        int total = 0;
        for (Zug zug : moves) {
            long oldHash = hash;
            MoveInfo info = MoveFinder.saveMoveInfo(zug, null);

            long newHash = MoveFinder.doMoveUpdateHash(zug, null, info, oldHash);
            long recomputed = Zobrist.computeHash(Board.bitboards, !isWhite);
            if (newHash != recomputed) {
                throw new RuntimeException("Hash mismatch at root move " + zug.processZug() + ": newHash=" + newHash + ", recomputed=" + recomputed);
            }

            int nodes = perft(null, depth - 1, !isWhite, newHash);
            System.out.println(zug.processZug() + ": " + nodes);

            MoveFinder.undoMove(zug, null, info);
            total += nodes;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        double nps = (total * 1000.0) / (elapsed + 1);
        System.out.println("Nodes: " + total);
        System.out.println("Time elapsed: " + elapsed + " ms");
        System.out.println("Speed: " + (long)nps + " nodes/s");

        System.out.println("Total: " + total);
    }

    private static int perft(Piece [][] board, int depth, boolean isWhite, long hash) {
        if (depth == 0) return 1;

        ArrayList<Zug> moves = MoveFinder.possibleMoves(isWhite, board);

        moves.removeIf(zug -> !Board.bitboards.isLegalMove(zug, isWhite));

        MoveOrdering.orderMoves(moves, board, isWhite);

        int count = 0;
        for (Zug zug : moves) {
            long oldHash = hash;
            MoveInfo info = MoveFinder.saveMoveInfo(zug, null);
            long newHash = MoveFinder.doMoveUpdateHash(zug, null, info, oldHash);

            long recomputed = Zobrist.computeHash(Board.bitboards, !isWhite);
            if (newHash != recomputed) {
                throw new RuntimeException("Hash mismatch at recursive move " + zug.processZug() + ": newHash=" + newHash + ", recomputed=" + recomputed);
            }

            count += perft(null, depth - 1, !isWhite, newHash);
            MoveFinder.undoMove(zug, null, info);
        }
        return count;
    }
}
