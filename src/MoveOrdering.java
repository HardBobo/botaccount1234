import java.util.ArrayList;

public class MoveOrdering {
    public MoveOrdering() {

    }
    private static int evaluateMove(Zug zug, Piece[][] board, boolean isWhite) {
        int score = 0;
        Piece movingPiece = board[zug.startY][zug.startX];
        Piece targetPiece;
        int richtung = board[zug.startY][zug.startX].isWhite() ? -1 : 1;
        if(Spiel.enPassant(zug, board)) {
            targetPiece = board[zug.endY - richtung][zug.endX];
        } else {
            targetPiece = board[zug.endY][zug.endX];
        }

        // schlagen bonus krasser wenn arsch angreifer geiler verteidiger
        if (!(targetPiece instanceof Empty)) {
            int victimValue = targetPiece.getValue();
            int attackerValue = movingPiece.getValue();
            score += 1000 + victimValue - attackerValue; // Schlagzüge priorisieren
        }

        // promotion krass
        if (movingPiece instanceof Bauer && Spiel.promotion(zug, board)) {
            score += 800;
        }

        // rochade bewerten, wenn king safety von eval erkannt
        if (Spiel.rochade(zug, board)) {
            score += 50;

        }

         //schachzug krasser
         if (Spiel.inCheck(board, !isWhite)) {
             score += 20;
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
