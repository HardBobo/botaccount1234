import java.util.*;
public class Spiel {

    public Spiel() {
        Board.setupBoard(Board.brett);
    }
    public static boolean isPseudoLegal(Zug zug, boolean white, Piece[][] board){
        //eigene figur schlagen
        Piece zielFigur = board[zug.endY][zug.endX];
        //System.out.println("du kannst keine eigene figur schlagen");
        return zielFigur instanceof Empty || zielFigur.isWhite() != white;
    }
    public static Koordinaten kingCoordinates(boolean white, Piece[][] board){
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if(board[i][j] instanceof Koenig && board[i][j].isWhite() == white){
                    return new Koordinaten(j, i);
                }
            }
        }
        return null;
    }

    public static boolean imBrett(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public static boolean isSquareAttacked(Piece[][] board, int x, int y, boolean enemyIsWhite) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];

                // richtige farbe + keine leeren felder
                if (piece instanceof Empty || piece.isWhite() != enemyIsWhite) {
                    continue;
                }

                // kann figur nach x y ziehen
                final boolean diagonal = Math.abs(col - x) == Math.abs(row - y);
                switch (piece) {
                    case Bauer bauer -> {
                        int dir = enemyIsWhite ? -1 : 1;
                        if (row + dir == y && (col + 1 == x || col - 1 == x)) {
                            return true;
                        }
                    }
                    case Springer springer -> {
                        int dx = Math.abs(col - x);
                        int dy = Math.abs(row - y);
                        if (dx * dy == 2) {
                            return true;
                        }
                    }
                    case Laeufer laeufer -> {
                        if (diagonal && pathClear(board, col, row, x, y)) {
                            return true;
                        }
                    }
                    case Turm turm -> {
                        if ((col == x || row == y) && pathClear(board, col, row, x, y)) {
                            return true;
                        }
                    }
                    case Dame dame -> {
                        if ((diagonal ||
                                (col == x || row == y)) &&
                                pathClear(board, col, row, x, y)) {
                            return true;
                        }
                    }
                    case Koenig koenig -> {
                        if (Math.abs(col - x) <= 1 && Math.abs(row - y) <= 1) {
                            return true;
                        }
                    }
                    default -> { /* Empty wird schon oben gefiltert */ }
                }
            }
        }
        return false;
    }
    private static boolean pathClear(Piece[][] board, int startX, int startY, int endX, int endY) {
        int dx = Integer.compare(endX, startX);
        int dy = Integer.compare(endY, startY);

        int x = startX + dx;
        int y = startY + dy;

        while (x != endX || y != endY) {
            if (!(board[y][x] instanceof Empty)) {
                return false;
            }
            x += dx;
            y += dy;
        }
        return true;
    }
}