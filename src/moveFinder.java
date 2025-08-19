import java.util.*;

public class moveFinder{
    public moveFinder(){

    }
    public static ArrayList<Zug> possibleMoves(boolean white, Piece[][] board) {
        ArrayList<Zug> pM = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];
                if (!(piece instanceof Empty) && piece.isWhite() == white) {
                    pM.addAll(piece.moeglicheZuege(x, y, board));
                }
            }
        }
        return pM;
    }
    public static int evaluation (Piece[][] board, boolean isWhite){
        int score = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board[y][x];
                Koordinaten k = new Koordinaten(x,y);
                if (p != null) {
                    if (isWhite) {
                        if (p.isWhite()) {
                            score += p.getValue();
                            score += Evaluation.evalWithPosition(k, board, true);
                        }
                        else {
                            score -= p.getValue();
                            score -= Evaluation.evalWithPosition(k, board, false);
                        }
                    } else {
                        if (p.isWhite()) {
                            score -= p.getValue();
                            score -= Evaluation.evalWithPosition(k, board, true);
                        }
                        else {
                            score += p.getValue();
                            score += Evaluation.evalWithPosition(k, board, false);
                        }
                    }
                }
            }
        }
        return score;
    }
    public static ArrayList<Zug> findBestMoves(Piece[][] board, int depth, boolean isWhite, ArrayList<Zug> orderedMoves){

        TreeMap<Integer, Zug> bestMoves = new TreeMap<>();

        if(orderedMoves.isEmpty()) return null;

        Zug bestMove = orderedMoves.getFirst();

        for(Zug zug : orderedMoves){// do move und undo move damit nicht dauerhaft neue teure kopien des boards erstellt werden
            MoveInfo info = saveMoveInfo(zug, board);
            doMove(zug, board);

            int score = negamax(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, !isWhite);

            undoMove(zug, board, info);

            if(isWhite){
                bestMoves.put(score, zug);
            } else {
                bestMoves.put(score, zug);
            }
        }

        ArrayList<Zug> temp = new ArrayList<>(bestMoves.values());
        return temp;
    }
    private static int negamax(Piece [][] board, int depth, int alpha, int beta, boolean isWhite) {

        if (depth == 0)
            return qSearch(board, alpha, beta, isWhite);

        ArrayList<Zug> possibleMoves = possibleMoves(isWhite, board);
        MoveOrdering.orderMoves(possibleMoves, board, isWhite);

        if (possibleMoves.isEmpty()) {
            if (Spiel.inCheck(board, isWhite)) {
                return isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            } else {
                return 0;
            }
        }

        int value = Integer.MIN_VALUE;
        for (Zug zug : possibleMoves){
            MoveInfo info = saveMoveInfo(zug, board);
            doMove(zug, board);
            value = Math.max(value, -negamax(board, depth - 1, -beta, -alpha, !isWhite ));
            undoMove(zug, board, info);
            alpha = Math.max(alpha, value);
            if(alpha >= beta)
                break; //alpha beta cutoff
        }
        return value;
    }

    private static int qSearch(Piece [][] board, int alpha, int beta, boolean isWhite){

        int static_eval = evaluation(board, isWhite);

        int best_value = static_eval;

        if( best_value >= beta ) {
            return best_value;
        }

        if( best_value > alpha )
            alpha = best_value;

        ArrayList<Zug> moves = possibleMoves(isWhite, board);
        ArrayList<Zug> forcingMoves = new ArrayList<>();

        for(Zug zug : moves){
            if(Spiel.isCapture(zug, board))
                forcingMoves.add(zug);
        }

        for(Zug zug : forcingMoves)  {

            MoveInfo info = saveMoveInfo(zug, board);
            doMove(zug, board);

            int score = -qSearch(board, -beta, -alpha, !isWhite);

            undoMove(zug, board, info);

            if( score >= beta ) {
                return score;
            }
            if( score > best_value )
                best_value = score;
            if( score > alpha )
                alpha = score;
        }
        return best_value;
    }
    public static void doMove(Zug zug, Piece[][] board) {
        boolean bauerDoppelZug = bauerDoppel(zug, board);

        if (rochade(zug, board)) {
            if (kurze(zug)) { // kurze Rochade
                kurzeRochade(zug, board);
            } else { // lange Rochade
                langeRochade(zug, board);
            }
        }

        // En-Passant
        if (enPassant(zug, board)) {
            enPassantExe(zug, board);
        }

        //Normaler Zug
        normalTurnExe(zug, board);

        //Promotion
        if (promotion(zug, board)) {
            promotionExe(zug, board, board[zug.endY][zug.endX].isWhite());
        }

        // en passant flags resetten und dann die neue setzen
        resetPawnEnPassant(board);
        if (bauerDoppelZug && board[zug.endY][zug.endX] instanceof Bauer b) {
            b.setEnPassantPossible(true);
        }

        // erster zug flags
        if (board[zug.endY][zug.endX] instanceof Turm t) {
            t.setKannRochieren(false);
        }
        if (board[zug.endY][zug.endX] instanceof Koenig k) {
            k.setKannRochieren(false);
        }
    }
    public static MoveInfo saveMoveInfo(Zug zug, Piece[][] board) {
        MoveInfo info = new MoveInfo();
        Piece movingPiece = board[zug.startY][zug.startX];
        Piece targetPiece;
        Koordinaten targetCoords;
        if(enPassant(zug, board)) {
            targetPiece = board[zug.startY][zug.endX];
            targetCoords = new Koordinaten(zug.endX, zug.startY);

        }
        else {
            targetPiece = board[zug.endY][zug.endX];
            targetCoords = new Koordinaten(zug.endX, zug.endY);
        }

        info.targetCoords = targetCoords;
        // sich bewegende figur speichern
        info.movingPiece = movingPiece;

        // geschlagene Figur speichern
        info.capturedPiece = targetPiece;

        // rochade Infos
        if (rochade(zug, board)) {
            info.rookMoved = true;
            if (kurze(zug)) {
                info.rookStartX = 7;
                info.rookEndX = 5;
            } else {
                info.rookStartX = 0;
                info.rookEndX = 3;
            }
        } else {
            info.rookMoved = false;
        }

        return info;
    }
    public static void undoMove(Zug zug, Piece[][] board, MoveInfo info) {
        resetMovingPiece(zug, board, info);

        // geschlagene
        if(wasEnPassant(zug, board, info.capturedPiece, info.targetCoords)){
            board[zug.endY][zug.endX] = new Empty();
        }
        resetSquareMovedOnto(board, info);

        // turm bei rochade zurück
        if (info.rookMoved) {
            Piece rook = board[zug.endY][info.rookEndX];
            board[zug.endY][info.rookStartX] = rook;
            ((Turm) rook).setKannRochieren(true);
            board[zug.endY][info.rookEndX] = new Empty();
        }
    }

    public static Zug iterativeDeepening (Piece[][] board, boolean isWhite){
        ArrayList<Zug> order = possibleMoves(isWhite, board);

        for(int i = 1; i<4; i++) {
            System.out.println("Tiefe: " + i);

            order = (findBestMoves(board, i, isWhite,order));
            System.out.println("Bester Zug bisher: " + order.getFirst().processZug());
        }

        return order.getFirst();
    }
    private static boolean wasEnPassant(Zug zug, Piece[][] board, Piece capturedPiece, Koordinaten targetCoords){
        if(board[zug.startY][zug.startX] instanceof Bauer && capturedPiece.isWhite() != board[zug.startY][zug.startX].isWhite()
                && capturedPiece instanceof Bauer b && b.isEnPassantPossible() && zug.startY == targetCoords.y){
            return true;
        }
        return false;
    }
    private static boolean rochade(Zug zug, Piece [][] board){
        return board[zug.startY][zug.startX] instanceof Koenig && Math.abs(zug.endX - zug.startX) == 2;
    }
    private static boolean kurze(Zug zug){
        return zug.endX > zug.startX;
    }
    public static void kurzeRochade(Zug zug, Piece [][] board){
        ((Koenig) board[zug.startY][zug.startX]).setKannRochieren(false);
        ((Turm) board[zug.startY][7]).setKannRochieren(false);
        board[zug.endY][5] = board[zug.endY][7];
        board[zug.endY][7] = new Empty();
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
    public static void promotionExe(Zug zug, Piece [][] board, boolean isWhite){
        char c = zug.promoteTo;
        switch (c){
            case 'q' -> board[zug.endY][zug.endX] = new Dame(isWhite);
            case 'r' -> board[zug.endY][zug.endX] = new Turm(isWhite);
            case 'b' -> board[zug.endY][zug.endX] = new Laeufer(isWhite);
            case 'n' -> board[zug.endY][zug.endX] = new Springer(isWhite);
        }
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
    public static void resetMovingPiece(Zug zug, Piece [][] board, MoveInfo info){
        board[zug.startY][zug.startX] = info.movingPiece;
    }
    public static void resetSquareMovedOnto(Piece [][] board, MoveInfo info){
        board[info.targetCoords.y][info.targetCoords.x] = info.capturedPiece;
    }
    public static boolean bauerDoppel(Zug zug, Piece [][] board){
        return Math.abs(zug.endY - zug.startY) == 2 && board[zug.startY][zug.startX] instanceof Bauer;
    }
}
