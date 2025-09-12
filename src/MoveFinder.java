import java.util.*;

public class MoveFinder {
    private static long nodes = 0;     // Knoten-Zähler
    private static long startTime = 0; // Startzeit für Messung

    static final int EXACT = 0;
    static final int LOWERBOUND = 1;
    static final int UPPERBOUND = 2;

    static Map<Long, TTEntry> transpositionTable = new HashMap<>();
    public MoveFinder(){

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
                            score += Evaluation.evalForRelativScore(k, board, true);
                        }
                        else {
                            score -= p.getValue();
                            score -= Evaluation.evalWithPosition(k, board, false);
                            score -= Evaluation.evalForRelativScore(k, board, false);
                        }
                    } else {
                        if (p.isWhite()) {
                            score -= p.getValue();
                            score -= Evaluation.evalWithPosition(k, board, true);
                            score -= Evaluation.evalForRelativScore(k, board, true);
                        }
                        else {
                            score += p.getValue();
                            score += Evaluation.evalWithPosition(k, board, false);
                            score += Evaluation.evalForRelativScore(k, board, false);
                        }
                    }
                }
            }
        }
        return score;
    }
    public static ArrayList<Zug> findBestMoves(Piece[][] board, int depth, boolean isWhite, ArrayList<Zug> orderedMoves){
        nodes = 0;
        startTime = System.currentTimeMillis();

        TreeMap<Integer, Zug> bestMoves = new TreeMap<>();

        orderedMoves.removeIf(zug -> !isLegalMove(zug, board, isWhite));

        if(orderedMoves.isEmpty()) return null;

        for(Zug zug : orderedMoves){// do move und undo move damit nicht dauerhaft neue teure kopien des boards erstellt werden

            MoveInfo info = saveMoveInfo(zug, board);

            boolean success = doMove(zug, board);

            if(!success)
                continue;

            int score = negamax(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, !isWhite);

            undoMove(zug, board, info);

            if(isWhite){
                bestMoves.put(score, zug);
            } else {
                bestMoves.put(score, zug);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        double nps = (nodes * 1000.0) / (elapsed + 1);
        System.out.println("Nodes: " + nodes);
        System.out.println("Time elapsed: " + elapsed + " ms");
        System.out.println("Speed: " + (long)nps + " nodes/s");

        return new ArrayList<>(bestMoves.values());
    }
    private static int negamax(Piece [][] board, int depth, int alpha, int beta, boolean isWhite) {

        nodes++;

        int alphaOrig = alpha;
        long hash = Zobrist.computeHash(board, isWhite);

        TTEntry entry = transpositionTable.get(hash);

        if (entry != null && entry.isValid && entry.depth >= depth) {
            if (entry.flag == EXACT) {
                return entry.value;
            } else if (entry.flag == LOWERBOUND && entry.value >= beta) {
                return entry.value;
            } else if (entry.flag == UPPERBOUND && entry.value <= alpha) {
                return entry.value;
            }
        }

        if (depth == 0)
            return evaluation(board, isWhite);

        ArrayList<Zug> pseudoLegalMoves = possibleMoves(isWhite, board);

        pseudoLegalMoves.removeIf(zug -> !isLegalMove(zug, board, isWhite));

        if (pseudoLegalMoves.isEmpty()) {
            if (Spiel.inCheck(board, isWhite)) {
                return -(100000 + depth);
            } else {
                return 0;
            }
        }

        MoveOrdering.orderMoves(pseudoLegalMoves, board, isWhite);

        int value = Integer.MIN_VALUE;
        for (Zug zug : pseudoLegalMoves){

            MoveInfo info = saveMoveInfo(zug, board);

            boolean success = doMove(zug, board);

            if(!success)
                continue;

            value = Math.max(value, -negamax(board, depth - 1, -beta, -alpha, !isWhite ));


            undoMove(zug, board, info);

            alpha = Math.max(alpha, value);

            if(alpha >= beta)
                break; //alpha beta cutoff
        }

        int flag;
        if (value <= alphaOrig) {
            flag = UPPERBOUND;
        } else if (value >= beta) {
            flag = LOWERBOUND;
        } else {
            flag = EXACT;
        }

        transpositionTable.put(hash, new TTEntry(value, depth, flag));

        return value;
    }

    private static int qSearch(Piece [][] board, int alpha, int beta, boolean isWhite){

        ArrayList<Zug> moves = possibleMoves(isWhite, board);

        moves.removeIf(zug -> !isLegalMove(zug, board, isWhite));

        if (moves.isEmpty()) {
            if (Spiel.inCheck(board, isWhite)) {
                return -100000;
            } else {
                return 0;
            }
        }

        int static_eval = evaluation(board, isWhite);

        int best_value = static_eval;

        if( best_value >= beta ) {
            return best_value;
        }

        if( best_value > alpha )
            alpha = best_value;


        ArrayList<Zug> forcingMoves = new ArrayList<>();

        for(Zug zug : moves){
            if(Spiel.isCapture(zug, board))
                forcingMoves.add(zug);
        }

        MoveOrdering.orderMoves(forcingMoves, board, isWhite);

        for(Zug zug : forcingMoves)  {

            MoveInfo info = saveMoveInfo(zug, board);
            boolean success = doMove(zug, board);

            if(!success)
                continue;

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
    public static boolean doMove(Zug zug, Piece[][] board) {
        boolean moveExecuted = false;
        boolean bauerDoppelZug = bauerDoppel(zug, board);

        if (rochade(zug, board)) {
            if(kurze(zug) && kurzePossible(zug, board)) { // kurze Rochade
                kurzeRochade(zug, board);
                moveExecuted = true;
            } else if(!kurze(zug) && langePossible(zug, board)){ // lange Rochade
                langeRochade(zug, board);
                moveExecuted = true;
            } else {
                return false;
            }
        }

        // En-Passant
        if (enPassant(zug, board)) {
            enPassantExe(zug, board);
            moveExecuted = true;
        }

        //Normaler Zug
        normalTurnExe(zug, board);
        moveExecuted = true;

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
        return moveExecuted;
    }
    public static MoveInfo saveMoveInfo(Zug zug, Piece[][] board) {
        MoveInfo info = new MoveInfo();
        Piece movingPiece = board[zug.startY][zug.startX];
        Piece targetPiece = board[zug.endY][zug.endX];

        info.enPassantBauerCoords = bauerHasEnPassantFlag(board);

        if(movingPiece instanceof Bauer){
            info.wasEnPassantCapturable = ((Bauer) movingPiece).isEnPassantPossible();
        }

        if(movingPiece instanceof Koenig){
            info.wasFirstMove = ((Koenig) movingPiece).kannRochieren();
        }

        if(movingPiece instanceof Turm){
            info.wasFirstMove = ((Turm) movingPiece).kannRochieren();
        }

        // sich bewegende figur speichern
        info.movingPiece = movingPiece;

        // geschlagene Figur speichern
        info.squareMovedOnto = targetPiece;

        // rochade Infos
        if (rochade(zug, board)) {
            if (kurze(zug) && kurzePossible(zug, board)) {
                info.rookMoved = true;
                info.rookStartX = 7;
                info.rookEndX = 5;
            } else if (!kurze(zug) && langePossible(zug, board)) {
                info.rookMoved = true;
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
        if(wasEnPassant(zug, board, info.squareMovedOnto)){
            enPassantResetExe(board, zug);
        } else {
            resetSquareMovedOnto(board, zug, info);
        }

        if(info.enPassantBauerCoords != null){
            ((Bauer) board[info.enPassantBauerCoords.y][info.enPassantBauerCoords.x]).setEnPassantPossible(true);
        }

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

        for(int i = 1; i<5; i++) {
            System.out.println("Tiefe: " + i);

            order = (findBestMoves(board, i, isWhite,order));
            System.out.println("Bester Zug bisher: " + order.getFirst().processZug());
        }

        return order.getFirst();
    }
    private static boolean wasEnPassant(Zug zug, Piece[][] board, Piece squareMovedOnto){
        if(board[zug.startY][zug.startX] instanceof Bauer && squareMovedOnto instanceof Empty && Math.abs(zug.endX - zug.startX) == 1){
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
    private static boolean kurzePossible(Zug zug, Piece [][] board){
        int startX = zug.startX;
        int startY = zug.startY;
        boolean gegner = !board[startY][startX].isWhite();
        if(!Spiel.isSquareAttacked(board, startX, startY, gegner)
                && !Spiel.isSquareAttacked(board, startX + 1, startY, gegner)
                && !Spiel.isSquareAttacked(board, startX + 2, startY, gegner)){
            return true;
        }
        return false;
    }
    public static void kurzeRochade(Zug zug, Piece [][] board){
        ((Koenig) board[zug.startY][zug.startX]).setKannRochieren(false);
        ((Turm) board[zug.startY][7]).setKannRochieren(false);
        board[zug.endY][5] = board[zug.endY][7];
        board[zug.endY][7] = new Empty();
    }
    private static boolean langePossible(Zug zug, Piece [][] board){
        int startX = zug.startX;
        int startY = zug.startY;
        boolean gegner = !board[startY][startX].isWhite();
        if(!Spiel.isSquareAttacked(board, startX, startY, gegner)
                && !Spiel.isSquareAttacked(board, startX - 1, startY, gegner)
                && !Spiel.isSquareAttacked(board, startX - 2, startY, gegner)){
            return true;
        }
        return false;
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
    public static boolean bauerDoppel(Zug zug, Piece [][] board){
        return Math.abs(zug.endY - zug.startY) == 2 && board[zug.startY][zug.startX] instanceof Bauer;
    }
    private static void enPassantResetExe(Piece [][] board, Zug zug){
        board[zug.endY][zug.endX] = new Empty();
        board[zug.startY][zug.endX] = new Bauer(!board[zug.startY][zug.startX].isWhite());
        ((Bauer) board[zug.startY][zug.endX]).setEnPassantPossible(true);
    }
    private static Koordinaten bauerHasEnPassantFlag(Piece [][] board){
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
    static boolean isLegalMove(Zug zug, Piece[][] board, boolean isWhite) {
        MoveInfo info = saveMoveInfo(zug, board);
        boolean success = doMove(zug, board);

        if (!success) {
            undoMove(zug, board, info);
            return false;
        }

        boolean kingInCheck = Spiel.inCheck(board, isWhite);

        undoMove(zug, board, info);

        return !kingInCheck;
    }
    public static boolean[] getCastleRights(Piece[][] board) {
        // [whiteShort, whiteLong, blackShort, blackLong]
        boolean[] rights = new boolean[4];

        // Weiß kurze Rochade (König e1, Turm h1)
        if (board[7][4] instanceof Koenig k && board[7][4].isWhite() &&
                board[7][7] instanceof Turm t && board[7][7].isWhite()
                && k.kannRochieren()
                && t.kannRochieren()) {
            rights[0] = true;
        }

        // Weiß lange Rochade (König e1, Turm a1)
        if (board[7][4] instanceof Koenig k && board[7][4].isWhite() &&
                board[7][0] instanceof Turm t && board[7][0].isWhite()
                && k.kannRochieren()
                && t.kannRochieren()) {
            rights[1] = true;
        }

        // Schwarz kurze Rochade (König e8, Turm h8)
        if (board[0][4] instanceof Koenig k && !board[0][4].isWhite() &&
                board[0][7] instanceof Turm t && !board[0][7].isWhite()
                && k.kannRochieren()
                && t.kannRochieren()) {
            rights[2] = true;
        }

        // Schwarz lange Rochade (König e8, Turm a8)
        if (board[0][4] instanceof Koenig k
                && !board[0][4].isWhite()
                && board[0][0] instanceof Turm t
                && !board[0][0].isWhite()
                && k.kannRochieren()
                && t.kannRochieren()) {
            rights[3] = true;
        }

        return rights;
    }

}
