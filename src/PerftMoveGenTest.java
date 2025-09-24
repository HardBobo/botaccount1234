import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

public class PerftMoveGenTest {
    private static long startTime = 0;
    private static long startHash;
    public static void main(String[] args) throws NoSuchAlgorithmException {
        // Use Board.brett directly so PieceTracker works properly
        Board.brett = Board.fenToBoard("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - ");
        
        System.out.println("Running Perft test with PieceTracker optimization...");
        System.out.println("White pieces tracked: " + Board.pieceTracker.getAllPieces(true).size());
        System.out.println("Black pieces tracked: " + Board.pieceTracker.getAllPieces(false).size());
        System.out.println();

        startHash = Zobrist.computeHash(Board.brett, true);
        
        perftDivide(Board.brett, 5, true, startHash); // Will show all root moves and their node counts
    }

    public static void perftDivide(Piece [][] board, int depth, boolean isWhite, long hash) throws NoSuchAlgorithmException {
        startTime = System.currentTimeMillis();

        ArrayList<Zug> moves = MoveFinder.possibleMoves(isWhite, board);

        moves.removeIf(zug -> !Spiel.isLegalMove(zug, board, isWhite));

        MoveOrdering.orderMoves(moves, board, isWhite);

        int total = 0;
        for (Zug zug : moves) {
            MoveInfo info = MoveFinder.saveMoveInfo(zug, board);
            
            MoveFinder.doMoveUpdateHash(zug, board, info, hash);

            int nodes = perft(board, depth - 1, !isWhite, hash);
            System.out.println(zug.processZug() + ": " + nodes);

            MoveFinder.undoMove(zug, board, info);
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

        moves.removeIf(zug -> !Spiel.isLegalMove(zug, board, isWhite));

        MoveOrdering.orderMoves(moves, board, isWhite);

        int count = 0;
        for (Zug zug : moves) {
            MoveInfo info = MoveFinder.saveMoveInfo(zug, board);
            MoveFinder.doMoveUpdateHash(zug, board, info, hash);

            count += perft(board, depth - 1, !isWhite, hash);
            MoveFinder.undoMove(zug, board, info);
        }
        return count;
    }
}
