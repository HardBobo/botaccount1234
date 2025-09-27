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
    public static Koordinaten kingCoordinates(boolean white){
        // Use PieceTracker for efficient king lookup instead of scanning the entire board
        return Board.pieceTracker.getKing(white);
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
    public static void newGame(){
        Board.setupBoard(Board.brett);
        MoveFinder.transpositionTable.clear();
        Zobrist.initZobrist();
        LichessBotStream.startHash = Zobrist.computeHash(Board.brett, true);
    }
    
    // Game logic helper methods moved from MoveFinder
    public static boolean wasEnPassant(Zug zug, Piece[][] board, Piece squareMovedOnto){
        if(board[zug.startY][zug.startX] instanceof Bauer && squareMovedOnto instanceof Empty && Math.abs(zug.endX - zug.startX) == 1){
            return true;
        }
        return false;
    }
    
    public static boolean rochade(Zug zug, Piece [][] board){
        return board[zug.startY][zug.startX] instanceof Koenig && Math.abs(zug.endX - zug.startX) == 2;
    }
    
    public static boolean kurze(Zug zug){
        return zug.endX > zug.startX;
    }
    
    public static boolean kurzePossible(Zug zug, Piece [][] board){
        int startX = zug.startX;
        int startY = zug.startY;
        boolean gegner = !board[startY][startX].isWhite();
        return !isSquareAttacked(board, startX, startY, gegner)
                && !isSquareAttacked(board, startX + 1, startY, gegner);
    }
    
    public static void kurzeRochade(Zug zug, Piece [][] board){
        ((Koenig) board[zug.startY][zug.startX]).setKannRochieren(false);
        ((Turm) board[zug.startY][7]).setKannRochieren(false);
        board[zug.endY][5] = board[zug.endY][7];
        board[zug.endY][7] = new Empty();
    }
    
    public static boolean langePossible(Zug zug, Piece [][] board){
        int startX = zug.startX;
        int startY = zug.startY;
        boolean gegner = !board[startY][startX].isWhite();
        return !isSquareAttacked(board, startX, startY, gegner)
                && !isSquareAttacked(board, startX - 1, startY, gegner);
    }
    
    public static void langeRochade(Zug zug, Piece [][] board){
        ((Koenig) board[zug.startY][zug.startX]).setKannRochieren(false);
        ((Turm) board[zug.startY][0]).setKannRochieren(false);
        board[zug.endY][3] = board[zug.endY][0];
        board[zug.endY][0] = new Empty();
    }
    
    public static boolean enPassant(Zug zug, Piece [][] board){
        return board[zug.startY][zug.startX] instanceof Bauer && Math.abs(zug.startX - zug.endX) == 1 &&
                board[zug.endY][zug.endX] instanceof Empty && board[zug.startY][zug.endX] instanceof Bauer b && b.isEnPassantPossible();
    }
    
    public static void enPassantExe(Zug zug, Piece [][] board){
        board[zug.startY][zug.endX] = new Empty();
    }
    
    public static void normalTurnExe(Zug zug, Piece [][] board){
        board[zug.endY][zug.endX] = board[zug.startY][zug.startX];
        board[zug.startY][zug.startX] = new Empty();
    }
    
    public static boolean promotion(Zug zug, Piece [][] board){
        if(board[zug.endY][zug.endX] instanceof Bauer){
            boolean isWhite = board[zug.endY][zug.endX].isWhite();
            return (isWhite && zug.endY == 0) || (!isWhite && zug.endY == 7);
        }
        return false;
    }
    
    public static boolean promotionQ(Zug zug, Piece [][] board){
        if(board[zug.endY][zug.endX] instanceof Bauer){
            boolean isWhite = board[zug.endY][zug.endX].isWhite();
            return (isWhite && zug.endY == 0) || (!isWhite && zug.endY == 7) && zug.promoteTo == 'q';
        }
        return false;
    }
    
    public static void promotionExe(Zug zug, Piece [][] board, boolean isWhite){
        char c = zug.promoteTo;
        switch (c){
            case 'q' -> board[zug.endY][zug.endX] = new Dame(isWhite);
            case 'r' -> board[zug.endY][zug.endX] = new Turm(isWhite);
            case 'b' -> board[zug.endY][zug.endX] = new Laeufer(isWhite);
            case 'n' -> board[zug.endY][zug.endX] = new Springer(isWhite);
        }
    }
    
    public static boolean isCapture(Piece [][] board, Zug zug) {
        Piece captured = enPassant(zug, board) ? board[zug.startY][zug.endX] : board[zug.endY][zug.endX];
        return !(captured instanceof Empty);
    }
    
    public static boolean willPromote(Zug zug, Piece[][] board) {
        // Prüfe die STARTPOSITION
        if(board[zug.startY][zug.startX] instanceof Bauer b) {
            boolean isWhite = b.isWhite();
            return (isWhite && zug.endY == 0) || (!isWhite && zug.endY == 7);
        }
        return false;
    }
    
    public static boolean bauerDoppel(Zug zug, Piece [][] board){
        return Math.abs(zug.endY - zug.startY) == 2 && board[zug.startY][zug.startX] instanceof Bauer;
    }
    
    public static boolean inCheck(Piece[][] board, boolean isWhite) {
        Koordinaten coords = kingCoordinates(isWhite);
        return isSquareAttacked(board, coords.x, coords.y, !isWhite);
    }
    
    public static boolean[] getCastleRights(Piece[][] board) {
        // [whiteShort, whiteLong, blackShort, blackLong]
        boolean[] rights = new boolean[4];

        // Weiß kurze Rochade (König e1, Turm h1)
        if (board[7][4] instanceof Koenig k
                && k.isWhite()
                && board[7][7] instanceof Turm t
                && t.isWhite()
                && k.kannRochieren()
                && t.kannRochieren()) {
            rights[0] = true;
        }

        // Weiß lange Rochade (König e1, Turm a1)
        if (board[7][4] instanceof Koenig k
                && k.isWhite()
                && board[7][0] instanceof Turm t
                && t.isWhite()
                && k.kannRochieren()
                && t.kannRochieren()) {
            rights[1] = true;
        }

        // Schwarz kurze Rochade (König e8, Turm h8)
        if (board[0][4] instanceof Koenig k && !k.isWhite() &&
                board[0][7] instanceof Turm t && !t.isWhite()
                && k.kannRochieren()
                && t.kannRochieren()) {
            rights[2] = true;
        }

        // Schwarz lange Rochade (König e8, Turm a8)
        if (board[0][4] instanceof Koenig k
                && !k.isWhite()
                && board[0][0] instanceof Turm t
                && !t.isWhite()
                && k.kannRochieren()
                && t.kannRochieren()) {
            rights[3] = true;
        }

        return rights;
    }

    public static void resetPawnEnPassant(Piece[][] board){
        // Use PieceTracker to iterate only over pawns of both colors
        // and clear their en passant flags if set.
        PieceTracker tracker = Board.pieceTracker;
        // White pawns
        for (Koordinaten pos : tracker.getPawns(true)) {
            Piece p = board[pos.y][pos.x];
            if (p instanceof Bauer b && b.isEnPassantPossible()) {
                b.setEnPassantPossible(false);
            }
        }
        // Black pawns
        for (Koordinaten pos : tracker.getPawns(false)) {
            Piece p = board[pos.y][pos.x];
            if (p instanceof Bauer b && b.isEnPassantPossible()) {
                b.setEnPassantPossible(false);
            }
        }
    }
    public static void resetMovingPiece(Zug zug, Piece [][] board, MoveInfo info){
        board[zug.startY][zug.startX] = info.movingPiece;

        if(info.movingPiece instanceof Koenig k){
            k.setKannRochieren(info.wasFirstMove);
        }

        if(info.movingPiece instanceof Turm t){
            t.setKannRochieren(info.wasFirstMove);
        }

        if(info.movingPiece instanceof Bauer b){
            b.setEnPassantPossible(info.wasEnPassantCapturable);
        }
    }
    
    public static void resetSquareMovedOnto(Piece [][] board,Zug zug, MoveInfo info){
        board[zug.endY][zug.endX] = info.squareMovedOnto;
    }
    
    public static void enPassantResetExe(Piece [][] board, Zug zug){
        board[zug.endY][zug.endX] = new Empty();
        board[zug.startY][zug.endX] = new Bauer(!board[zug.startY][zug.startX].isWhite());
        ((Bauer) board[zug.startY][zug.endX]).setEnPassantPossible(true);
    }
    
    public static Koordinaten bauerHasEnPassantFlag(Piece [][] board){
        Piece p;
        for(int y = 3; y <= 4; y++){
            for(int x = 0; x < 8; x++){
                p = board[y][x];
                if(p instanceof Bauer b){
                    if(b.isEnPassantPossible()) {
                        return new Koordinaten(x, y);
                    }
                }
            }
        }
        return null;
    }
    
    public static boolean isLegalMove(Zug zug, Piece[][] board, boolean isWhite) {
        MoveInfo info = MoveFinder.saveMoveInfo(zug, board);

        if (rochade(zug, board)) {
            if (kurze(zug)) {
                if (!kurzePossible(zug, board)) {
                    return false;
                }
            } else if (!kurze(zug)) {
                if (!langePossible(zug, board)) {
                    return false;
                }
            } else {
                return false;
            }
        }

        MoveFinder.doMoveNoHash(zug, board, info);

        boolean kingInCheck = inCheck(board, isWhite);

        MoveFinder.undoMove(zug, board, info);

        return !kingInCheck;
    }
}
