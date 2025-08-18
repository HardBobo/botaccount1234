import java.util.*;
public class Spiel {
    public static final Board BOARD = new Board();
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
        BOARD.setupBoard();
    }

    public void playMove(Zug zug){
        if(spielstatus == running){
            //System.out.println((weissAmZug ? "Weiß" : "Schwarz") + " ist am Zug.");
            boolean bauerDoppelZug;
            if(isValid(zug, weissAmZug, Board.brett)) {
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
                gameOverCheck();
                nextTurn();
            }
            else
                System.out.println("ungültiger Zug: " + zug.processZug());
        } else {
            resultOutput();
        }
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

    public static void gameOverCheck(){
        if(!hasPossibleMove(!weissAmZug, Board.brett)){
            if(allSeenSquares(weissAmZug, Board.brett).contains(kingCoordinates(!weissAmZug, Board.brett))){
                spielstatus = weissAmZug ? whitewin : blackwin;
            }
            else
                spielstatus = draw;
        }
    }
    public static boolean hasPossibleMove(boolean white, Piece[][] board) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];
                if (piece.isWhite() == white) {
                    for (int zielY = 0; zielY < 8; zielY++) {
                        for (int zielX = 0; zielX < 8; zielX++) {
                            Zug zug = new Zug(x, y, zielX, zielY);
                            if (isValid(zug, white, board)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    public static boolean isValid(Zug zug, boolean white, Piece[][] board){
        //ausserhalb
        if (zug.startX < 0 || zug.startX > 7 || zug.startY < 0 || zug.startY > 7 ||
                zug.endX < 0 || zug.endX > 7 || zug.endY < 0 || zug.endY > 7) {
            //System.out.println("ausserhalb");
            return false;
        }
        //leere figur
        Piece startFigur = board[zug.startY][zug.startX];
        if (startFigur instanceof Empty) {
            //System.out.println("keine Figur hier");
            return false;
        }
        //gegnerische figur
        if (startFigur.isWhite() != white) {
            //System.out.println("gegnerische figur");
            return false;
        }
        //eigene figur schlagen
        Piece zielFigur = board[zug.endY][zug.endX];
        if (!(zielFigur instanceof Empty) && zielFigur.isWhite() == white) {
            //System.out.println("du kannst keine eigene figur schlagen");
            return false;
        }

        Piece figur = board[zug.startY][zug.startX];

        //bauernzüge
        if (figur instanceof Bauer) {
            if (!((Bauer) figur).istZugMoeglich(zug.startX, zug.startY, zug.endX, zug.endY, zug.promoteTo, board)) {
                return false;
            }
        }
        //springerzüge
        if (figur instanceof Springer) {
            if (!((Springer) figur).istZugMoeglich(zug.startX, zug.startY, zug.endX, zug.endY, board)) {
                return false;
            }
        }
        //turmzüge
        if(figur instanceof Turm) {
            if(!((Turm) figur).istZugMoeglich(zug.startX, zug.startY, zug.endX, zug.endY, board)){
                return false;
            }
        }
        //läuferzüge
        if(figur instanceof Laeufer) {
            if(!((Laeufer) figur).istZugMoeglich(zug.startX, zug.startY, zug.endX, zug.endY, board)){
                return false;
            }
        }
        //damenzüge
        if(figur instanceof Dame) {
            if(!((Dame) figur).istZugMoeglich(zug.startX, zug.startY, zug.endX, zug.endY, board)){
                return false;
            }
        }
        //königszüge
        if(figur instanceof Koenig) {
            if(!((Koenig) figur).istZugMoeglich(zug.startX, zug.startY, zug.endX, zug.endY, board)){
                return false;
            }
        }
        //schach

        return !inCheckAfterMove(zug, board, startFigur.isWhite());
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

                if (!figur.getType().equals("empty") && figur.isWhite() == enemyIsWhite) {
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
        MoveInfo info = moveFinder.saveMoveInfo(zug, board);
        moveFinder.doMove(zug, board);
        if(allSeenSquares(!isWhite, board).contains(kingCoordinates(isWhite, board))){
            inCheck = true;
        }
        moveFinder.undoMove(zug, board, info);
        return inCheck;
    }
    public static boolean inCheck(Piece [][] board, boolean isWhite){
        return allSeenSquares(!isWhite, board).contains(kingCoordinates(isWhite, board));
    }
    public static boolean isCapture(Zug zug, Piece [][] board){
        Piece captured = enPassant(zug, board) ? board[zug.startY][zug.endX] : board[zug.endY][zug.endX];
        return !(captured instanceof Empty);
    }
}