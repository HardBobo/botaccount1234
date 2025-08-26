import java.util.ArrayList;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class PerftMoveGenTest {
        static Piece [][] temp;
    public static void main(String[] args) throws NoSuchAlgorithmException {
        temp = new Piece[8][8];
        temp = Board.fenToBoard("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - ");
//        MoveFinder.doMove(new Zug("d5e6"), temp);
//        MoveFinder.doMove(new Zug("e8g8"), temp);
//        MoveFinder.doMove(new Zug("e6d7"), temp);
        perftDivide(temp, 5, true); // Will show all root moves and their node counts
    }

    public static void perftDivide(Piece [][] board, int depth, boolean isWhite) throws NoSuchAlgorithmException {
        ArrayList<Zug> moves = MoveFinder.possibleMoves(isWhite, board);

        moves.removeIf(zug -> !MoveFinder.isLegalMove(zug, board, isWhite));

        MoveOrdering.orderMoves(moves, board, isWhite);

        int total = 0;
        for (Zug zug : moves) {
            MoveInfo info = MoveFinder.saveMoveInfo(zug, board);
            String text = Board.boardToString(board, isWhite);
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Bytes vom String holen und durch den Hasher jagen
            byte[] hashBytes = md.digest(text.getBytes());

            // in Hex-String umwandeln
            String hashHex = HexFormat.of().formatHex(hashBytes);

            System.out.println("Hash: " + hashHex);

            boolean success = MoveFinder.doMove(zug, board);

            if(!success)
                continue;

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

        moves.removeIf(zug -> !MoveFinder.isLegalMove(zug, board, isWhite));

        MoveOrdering.orderMoves(moves, board, isWhite);

        int count = 0;
        for (Zug zug : moves) {
            MoveInfo info = MoveFinder.saveMoveInfo(zug, board);
            boolean success = MoveFinder.doMove(zug, board);

            if(!success)
                continue;

            count += perft(board, depth - 1, !isWhite);
            MoveFinder.undoMove(zug, board, info);
        }
        return count;
    }
}
