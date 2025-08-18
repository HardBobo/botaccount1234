import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Main {
        static Piece [][] temp;
    public static void main(String[] args) throws NoSuchAlgorithmException {
        Board.setupBoard();
        temp = Board.copy(Board.brett);
        moveFinder.doMove(new Zug("a2a4"), temp);
        moveFinder.doMove(new Zug("b7b5"), temp);
        moveFinder.doMove(new Zug("a4b5"), temp);
        moveFinder.doMove(new Zug("a7a6"), temp);
        moveFinder.doMove(new Zug("b5a6"), temp);
        moveFinder.doMove(new Zug("c8b7"), temp);
//        moveFinder.doMove(new Zug("a6b7"), temp);
//        moveFinder.doMove(new Zug("a8a1"), temp);
        perftDivide(temp, 4, true); // Will show all root moves and their node counts
    }

    public static void perftDivide(Piece [][] board, int depth, boolean isWhite) throws NoSuchAlgorithmException {
        ArrayList<Zug> moves = moveFinder.possibleMoves(isWhite, board);
        MoveOrdering.orderMoves(moves, board, isWhite);

        int total = 0;
        for (Zug zug : moves) {
            MoveInfo info = moveFinder.saveMoveInfo(zug, board);
            String text = Arrays.deepToString(board);
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Bytes vom String holen und durch den Hasher jagen
            byte[] hashBytes = md.digest(text.getBytes());

            // in Hex-String umwandeln
            String hashHex = HexFormat.of().formatHex(hashBytes);

            System.out.println("Hash: " + hashHex);
            moveFinder.doMove(zug, board);

            int nodes = perft(board, depth - 1, !isWhite);
            System.out.println(zug.processZug() + ": " + nodes);

            moveFinder.undoMove(zug, board, info);
            total += nodes;
        }
        System.out.println("Total: " + total);
    }

    private static int perft(Piece [][] board, int depth, boolean isWhite) {
        if (depth == 0) return 1;

        ArrayList<Zug> moves = moveFinder.possibleMoves(isWhite, board);
        // IMPORTANT: Ensure these are *legal* moves, not just pseudo-legal
        MoveOrdering.orderMoves(moves, board, isWhite);

        int count = 0;
        for (Zug zug : moves) {
            MoveInfo info = moveFinder.saveMoveInfo(zug, board);
            moveFinder.doMove(zug, board);
            count += perft(board, depth - 1, !isWhite);
            moveFinder.undoMove(zug, board, info);
        }
        return count;
    }
}
