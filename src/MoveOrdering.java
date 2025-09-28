import java.util.ArrayList;

public class MoveOrdering {

    private static final int [] pieceValues = {100, 300, 310, 500, 900, 0};

    public MoveOrdering() {

    }
    private static int evaluateMove(Zug zug, Piece[][] board, boolean isWhite) {
        int score = 0;
        // Piece values based on bitboards
        int from = zug.startY * 8 + zug.startX;
        boolean moverWhite = (Board.bitboards.occW & (1L << from)) != 0L;
        int attackerType = Board.bitboards.pieceTypeAt(from, moverWhite);

        boolean isCapture = Board.bitboards.isCapture(zug);
        if (isCapture) {
            int to = zug.endY * 8 + zug.endX;
            int victimType;
            if (((Board.bitboards.occ >>> to) & 1L) != 0L) {
                boolean victimWhite = (Board.bitboards.occW & (1L << to)) != 0L;
                victimType = Board.bitboards.pieceTypeAt(to, victimWhite);
            } else {
                victimType = 0; // en passant captures a pawn
            }
            int victimValue = pieceValues[victimType];
            int attackerValue = pieceValues[attackerType];
            score += 1000 + victimValue - attackerValue;
        }

        if (attackerType == 0 && Board.bitboards.willPromote(zug)) {
            score += 800;
        }

        // simple castle bonus by king 2-square move
        if (attackerType == 5 && Math.abs(zug.endX - zug.startX) == 2) {
            score += 50;
        }
        return score;
    }
    public static void orderMoves(ArrayList<Zug> moves, Piece[][] board, boolean isWhite) {
        // Züge nach ihrer Bewertung sortieren (absteigende Reihenfolge)
        moves.sort((move1, move2) -> {
            int score1 = evaluateMove(move1, board, isWhite);
            int score2 = evaluateMove(move2, board, isWhite);
            return Integer.compare(score2, score1); // Absteigende Sortierung
        });
    }
}
