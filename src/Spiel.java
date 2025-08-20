import java.util.*;
public class Spiel {
    public static boolean weissAmZug;
    public static int spielstatus;
    public static int running;
    public static int draw;
    public static int whitewin;
    public static int blackwin;

    public Spiel() {
        blackwin = 3;
        whitewin = 2;
        draw = 1;
        running = 0;
        spielstatus = 0;
        weissAmZug = true;
        Board.setupBoard(Board.brett);
    }

    public void playMove(Zug zug){
            //System.out.println((weissAmZug ? "Weiß" : "Schwarz") + " ist am Zug.");
    boolean bauerDoppelZug;
        //bauerdoppel wird gespeichert für en passant danach
        bauerDoppelZug = bauerDoppel(zug, Board.brett);
        //rochade
        if(rochade(zug, Board.brett)){
            if (kurze(zug)) { // kurze
                kurzeRochade(zug);
            } else { // lange
                langeRochade(zug);
            }
        }
        // en passant
        if (enPassant(zug, Board.brett)) {
            enPassantExe(zug);
        }
        //normal
        normalTurnExe(zug);
        //promotion
        if(promotion(zug, Board.brett)){
            boolean isWhite = Board.brett[zug.endY][zug.endX].isWhite();
            promotionExe(zug, isWhite);
        }
        resetPawnEnPassant(Board.brett);
        if (bauerDoppelZug) {
            bauerEnPassantPossible(zug);
        }
        if(Board.brett[zug.endY][zug.endX] instanceof Turm turm){
            turm.setKannRochieren(false);
        }
        if(Board.brett[zug.endY][zug.endX] instanceof Koenig koenig){
            koenig.setKannRochieren(false);
        }
        nextTurn();
    }

    public static void nextTurn() {
        weissAmZug = !weissAmZug;
    }
    public static boolean bauerDoppel(Zug zug, Piece [][] board){
        return board[zug.startY][zug.startX] instanceof Bauer && Math.abs(zug.endY - zug.startY) == 2;
    }
    public static boolean rochade(Zug zug, Piece [][] board){
        return board[zug.startY][zug.startX] instanceof Koenig && Math.abs(zug.endX - zug.startX) == 2;
    }
    public static boolean kurze(Zug zug){
        return zug.endX > zug.startX;
    }
    public static void kurzeRochade(Zug zug){
        Board.brett[zug.endY][5] = Board.brett[zug.endY][7];
        Board.brett[zug.endY][7] = new Empty();
    }
    public static void langeRochade(Zug zug){
        Board.brett[zug.endY][3] = Board.brett[zug.endY][0];
        Board.brett[zug.endY][0] = new Empty();
    }
    public static boolean enPassant(Zug zug, Piece [][] board){
        return board[zug.startY][zug.startX] instanceof Bauer
                && Math.abs(zug.startX - zug.endX) == 1
                && board[zug.endY][zug.endX] instanceof Empty;
    }
    public static void enPassantExe(Zug zug){
        int richtung = Board.brett[zug.startY][zug.startX].isWhite() ? -1 : 1;
        Board.brett[zug.endY - richtung][zug.endX] = new Empty();
    }
    public static void normalTurnExe(Zug zug){
        Board.brett[zug.endY][zug.endX] = Board.brett[zug.startY][zug.startX];
        Board.brett[zug.startY][zug.startX] = new Empty();
    }
    public static void bauerEnPassantPossible(Zug zug){
        ((Bauer) Board.brett[zug.endY][zug.endX]).setEnPassantPossible(true);
    }
    public static boolean promotion(Zug zug, Piece [][] board){
        if(board[zug.endY][zug.endX] instanceof Bauer){
            boolean isWhite = board[zug.endY][zug.endX].isWhite();
            return (isWhite && zug.endY == 0) || (!isWhite && zug.endY == 7);
        }
        return false;
    }
    public static void promotionExe(Zug zug, boolean isWhite){
        char c = zug.promoteTo;
        switch (c){
            case 'q' -> Board.brett[zug.endY][zug.endX] = new Dame(isWhite);
            case 'r' -> Board.brett[zug.endY][zug.endX] = new Turm(isWhite);
            case 'b' -> Board.brett[zug.endY][zug.endX] = new Laeufer(isWhite);
            case 'n' -> Board.brett[zug.endY][zug.endX] = new Springer(isWhite);
        }
    }
    public static void resultOutput(){
        if(spielstatus == whitewin)
            System.out.println("Weiß gewinnt");
        if(spielstatus == blackwin)
            System.out.println("Schwarz gewinnt");
        if(spielstatus == draw)
            System.out.println("Unentschieden");
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
    public static Set<Koordinaten> allSeenSquares(boolean enemyIsWhite, Piece[][] board) {
        Set<Koordinaten> bedroht = new HashSet<>();

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece figur = board[y][x];

                if (!(figur instanceof Empty) && figur.isWhite() == enemyIsWhite) {
                    bedroht.addAll(figur.bedrohteFelder(x, y, board));
                }
            }
        }
        return bedroht;
    }
    public static boolean imBrett(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }
    public static void resetPawnEnPassant(Piece[][] board){
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (board[y][x] instanceof Bauer) {
                    ((Bauer) board[y][x]).setEnPassantPossible(false);
                }
            }
        }
    }
    public static boolean inCheckAfterMove(Zug zug, Piece [][] board, boolean isWhite){
        boolean inCheck = false;
        MoveInfo info = MoveFinder.saveMoveInfo(zug, board);
        MoveFinder.doMove(zug, board);
        Koordinaten coords = kingCoordinates(isWhite, board);
        if(isSquareAttacked(board, coords.x, coords.y, !isWhite)){
            inCheck = true;
        }
        MoveFinder.undoMove(zug, board, info);
        return inCheck;
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
    public static boolean inCheck(Piece [][] board, boolean isWhite){
        Koordinaten coords = kingCoordinates(isWhite, board);
        return isSquareAttacked(board, coords.x, coords.y, !isWhite);
    }
    public static boolean isCapture(Zug zug, Piece [][] board){
        Piece captured = enPassant(zug, board) ? board[zug.startY][zug.endX] : board[zug.endY][zug.endX];
        return !(captured instanceof Empty);
    }
}