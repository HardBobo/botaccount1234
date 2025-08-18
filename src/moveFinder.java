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
    public static int evaluation (Piece[][] board){

        int score = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board[y][x];
                Koordinaten k = new Koordinaten(x,y);
                if (p != null) {
                    if (p.isWhite()) {
                        score += p.getValue();
                        score += Evaluation.evalWithPosition(k, board, true);
                    }
                    else {
                        score -= p.getValue();
                        score -= Evaluation.evalWithPosition(k, board, false);
                    }
                }
            }
        }
        return score;
    }
    public static ArrayList<Zug> findBestMoves(Piece[][] board, int depth, boolean isWhite, ArrayList<Zug> orderedMoves){

//        ArrayList<Zug> moves = possibleMoves(isWhite, board);
//        Set<Zug> set = new LinkedHashSet<>();
        //set.addAll(orderedMoves);
        //set.addAll(moves);

        TreeMap<Integer, Zug> bestMoves = new TreeMap<>();
//        int bestScore = isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Zug bestMove = orderedMoves.getFirst();

        if(orderedMoves.isEmpty()) return null;

        for(Zug zug : orderedMoves){// do move und undo move damit nicht dauerhaft neue teure kopien des boards erstellt werden
            MoveInfo info = saveMoveInfo(zug, board);
            doMove(zug, board);
            ArrayList<Zug> nextMoves = possibleMoves(!isWhite, board);
//            MoveOrdering.orderMoves(nextMoves, board, !isWhite);


            int score = minimax(board, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, !isWhite, nextMoves);

            undoMove(zug, board, info);

            if(isWhite){
                bestMoves.put(-score, zug);
            } else {
                bestMoves.put(score, zug);
            }
        }

        ArrayList<Zug> temp = new ArrayList<>(bestMoves.values());
        return temp;
    }
    private static int minimax(Piece [][] board, int depth, int alpha, int beta, boolean isWhite, ArrayList<Zug> moves) {

        boolean inCheck = Spiel.inCheck(board, isWhite);

        if (moves.isEmpty()) {
            if (inCheck) return isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            else return 0; // Patt
        }

        if (depth == 0)
            return qSearch(board, alpha, beta, isWhite);

        if(isWhite){// do move und undo move damit nicht dauerhaft neue teure kopien des boards erstellt werden
            int maxEval = Integer.MIN_VALUE;
            for (Zug zug : moves) {
                MoveInfo info = saveMoveInfo(zug, board);
                doMove(zug, board);
                ArrayList<Zug> newMoves = possibleMoves(false, board);
                MoveOrdering.orderMoves(newMoves, board, false);
                int eval = 0;
//                if(depth == 1) {
//                    if (Spiel.isCapture(zug, board))
//                        eval = minimax(board, depth, alpha, beta, false, newMoves);
//                    else
//                        eval = minimax(board, depth -1, alpha, beta, false, newMoves);
//                }
//                else
                eval = minimax(board, depth -1, alpha, beta, false, newMoves);
                undoMove(zug, board, info);
                maxEval = Math.max(maxEval, eval);
                if (maxEval >= beta)
                    break; // beta cut-off
                alpha = Math.max(alpha, maxEval);
            }
            return maxEval;
        } else {// do move und undo move damit nicht dauerhaft neue teure kopien des boards erstellt werden
            int minEval = Integer.MAX_VALUE;
            for (Zug zug : moves) {
                MoveInfo info = saveMoveInfo(zug, board);
                doMove(zug, board);
                ArrayList<Zug> newMoves = possibleMoves(true, board);
                MoveOrdering.orderMoves(newMoves, board, true);
                int eval = 0;
//                if(depth == 1) {
//                    if (Spiel.isCapture(zug, board))
//                        eval = minimax(board, depth, alpha, beta, true, newMoves);
//                    else
//                        eval = minimax(board, depth -1, alpha, beta, true, newMoves);
//                }
//                else
                eval = minimax(board, depth -1, alpha, beta, true, newMoves);
                undoMove(zug, board, info);
                minEval = Math.min(minEval, eval);
                if (minEval <= alpha)
                    break; // alpha cut-off
                beta = Math.min(beta, minEval);
            }
            return minEval;
        }
    }
    private static int qSearch(Piece [][] board, int alpha, int beta, boolean isWhite){
//        int currentEval = evaluation(board);
//        //alpha beta für schauen ob wir den besten zug gefunden haben
//        if (isWhite) {
//            if(currentEval >=  beta)
//                return currentEval;
//            if(currentEval > alpha)
//                alpha = currentEval;
//        } else {
//            if(currentEval <= alpha)
//                return currentEval;
//            if(currentEval < beta)
//                beta = currentEval;
//        }

        //nur züge mit schach oder figuren capture
        ArrayList<Zug> moves = possibleMoves(isWhite, board);

        boolean inCheck = Spiel.inCheck(board, isWhite);

        if (moves.isEmpty()) {
            if (inCheck) return isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;// schachmatt
            else return 0; // Patt
        }

        ArrayList<Zug> forcingMoves = new ArrayList<>();
        for(Zug zug : moves){
            //falls zug schach oder schlag ist oder man im schach steht
            if(/*Spiel.inCheckAfterMove(zug, board, isWhite) || Spiel.inCheckAfterMove(zug, board, !isWhite) ||*/ Spiel.isCapture(zug, board)){
                forcingMoves.add(zug);
            }
        }

        if (forcingMoves.isEmpty())
            return evaluation(board);

        //qSearch(board, alpha, beta, isWhite);

        if(isWhite){// do move und undo move damit nicht dauerhaft neue teure kopien des boards erstellt werden
            int maxEval = Integer.MIN_VALUE;
            for (Zug zug : moves) {
                MoveInfo info = saveMoveInfo(zug, board);
                doMove(zug, board);
                MoveOrdering.orderMoves(forcingMoves, board, false);
                int eval = 0;
                eval = qSearch(board, alpha, beta, false);
                undoMove(zug, board, info);
                maxEval = Math.max(maxEval, eval);
                if (maxEval >= beta)
                    break; // beta cut-off
                alpha = Math.max(alpha, maxEval);
            }
            return maxEval;
        } else {// do move und undo move damit nicht dauerhaft neue teure kopien des boards erstellt werden
            int minEval = Integer.MAX_VALUE;
            for (Zug zug : moves) {
                MoveInfo info = saveMoveInfo(zug, board);
                doMove(zug, board);
                MoveOrdering.orderMoves(forcingMoves, board, true);
                int eval = 0;
                eval = qSearch(board, alpha, beta, true);
                undoMove(zug, board, info);
                minEval = Math.min(minEval, eval);
                if (minEval <= alpha)
                    break; // alpha cut-off
                beta = Math.min(beta, minEval);
            }
            return minEval;
        }

//        MoveOrdering.orderMoves(forcingMoves, board, isWhite);
        //int maxEval = Integer.MIN_VALUE;
        //            for (Zug zug : moves) {
        //                MoveInfo info = saveMoveInfo(zug, board);
        //                doMove(zug, board);
        //                ArrayList<Zug> newMoves = possibleMoves(false, board);
        ////                MoveOrdering.orderMoves(newMoves, board, false);
        //                int eval = minimax(board, depth - 1, alpha, beta, false, newMoves);
        //                undoMove(zug, board, info);
        //                maxEval = Math.max(maxEval, eval);
        //                if (maxEval >= beta)
        //                    break; // beta cut-off
        //                alpha = Math.max(alpha, maxEval);
        //            }

        //normal minimax aber nur mit forcingMoves
//        if(isWhite){
//            int maxEval = Integer.MIN_VALUE;
//            for (Zug zug : moves) {
//                MoveInfo info = saveMoveInfo(zug, board);
//                doMove(zug, board);
//                int eval = qSearch(board, alpha, beta,false);
//                undoMove(zug, board, info);
//                maxEval = Math.max(maxEval, eval);
//                if (maxEval >= beta)
//                    break; // beta cut-off
//                alpha = Math.max(alpha, maxEval);
//            }
//            return maxEval;
//        } else {
//            int minEval = Integer.MAX_VALUE;
//            for (Zug zug : moves) {
//                MoveInfo info = saveMoveInfo(zug, board);
//                doMove(zug, board);
//                int eval = qSearch(board, alpha, beta, true);
//                undoMove(zug, board, info);
//                minEval = Math.min(minEval, eval);
//                if (minEval <= alpha)
//                    break; // alpha cut-off
//                beta = Math.min(beta, minEval);
//            }
//            return minEval;
//        }
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
