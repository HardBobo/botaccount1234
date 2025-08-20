import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class PerftMoveGenTest {
        static Piece [][] temp;
    public static void main(String[] args) throws NoSuchAlgorithmException {
        temp = new Piece[8][8];
        Board.setupBoard(temp);
        perftDivide(temp, 5, true); // Will show all root moves and their node counts
    }

    public static void perftDivide(Piece [][] board, int depth, boolean isWhite) throws NoSuchAlgorithmException {
        ArrayList<Zug> moves = MoveFinder.possibleMoves(isWhite, board);
        MoveOrdering.orderMoves(moves, board, isWhite);

        int total = 0;
        for (Zug zug : moves) {
            MoveInfo info = MoveFinder.saveMoveInfo(zug, board);
            MoveFinder.doMove(zug, board);

            if(Spiel.inCheck(board, isWhite)){
                MoveFinder.undoMove(zug, board, info);
                continue;
            }

            int nodes = perft(board, depth - 1, !isWhite);
            System.out.println(zug.processZug() + ": " + nodes);

            MoveFinder.undoMove(zug, board, info);
            total += nodes;
        }
        System.out.println("Total: " + total);
    }

    private static int perft(Piece [][] board, int depth, boolean isWhite) {
        if (depth == 0) return 1;

        ArrayList<Zug> moves = MoveFinder.possibleMoves(isWhite, board);
        // IMPORTANT: Ensure these are *legal* moves, not just pseudo-legal
        MoveOrdering.orderMoves(moves, board, isWhite);

        int count = 0;
        for (Zug zug : moves) {
            MoveInfo info = MoveFinder.saveMoveInfo(zug, board);
            MoveFinder.doMove(zug, board);

            if(Spiel.inCheck(board, isWhite)){
                MoveFinder.undoMove(zug, board, info);
                continue;
            }

            count += perft(board, depth - 1, !isWhite);
            MoveFinder.undoMove(zug, board, info);
        }
        return count;
    }
}
