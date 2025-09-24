import java.util.*;

public class MoveFinder {

    // Time control for search
    private static volatile long searchEndTimeMs = Long.MAX_VALUE;
    private static volatile boolean timeUp = false;
    // Set to true if an iteration was aborted due to timeout inside the search
    private static volatile boolean depthAborted = false;

    static final int EXACT = 0;
    static final int LOWERBOUND = 1;
    static final int UPPERBOUND = 2;

    static Map<Long, TTEntry> transpositionTable = new HashMap<>();

    public static ArrayList<Zug> possibleMoves(boolean white, Piece[][] board) {
        return Board.pieceTracker.generateMoves(white, board);
    }

    public static ArrayList<Zug> findBestMoves(Piece[][] board, int depth, boolean isWhite, ArrayList<Zug> orderedMoves, long hash) {
        if (System.currentTimeMillis() >= searchEndTimeMs) { timeUp = true; return orderedMoves; }

        // Remove illegal moves
        orderedMoves.removeIf(zug -> !Spiel.isLegalMove(zug, board, isWhite));
        if (orderedMoves.isEmpty()) return new ArrayList<>();

        // Prefer TT best move at root if available
        TTEntry rootTT = transpositionTable.get(hash);
        if (rootTT != null && rootTT.isValid && rootTT.bestMove != null) {
            moveToFront(orderedMoves, rootTT.bestMove);
        }

        // List to hold moves with their scores (only fully searched moves)
        ArrayList<ZugScore> scoredMoves = new ArrayList<>();
        HashSet<Zug> completed = new HashSet<>();

        for (Zug zug : orderedMoves) {
            if (System.currentTimeMillis() >= searchEndTimeMs) { timeUp = true; break; }

            MoveInfo info = saveMoveInfo(zug, board);
            long oldHash = hash;

            hash = doMoveUpdateHash(zug, board, info, hash);

            // Negate score to get perspective of current player
            int score = -negamax(board, depth-1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, !isWhite, hash);

            // Always undo before any potential early return/break
            undoMove(zug, board, info);
            hash = oldHash;

            // If time ran out or depth aborted during this move's search, do not
            // accept this (potentially misleading) score; stop here and keep
            // previously completed results only.
            if (timeUp || depthAborted || System.currentTimeMillis() >= searchEndTimeMs) {
                timeUp = true;
                depthAborted = true;
                break;
            }

            scoredMoves.add(new ZugScore(zug, score));
            completed.add(zug);
        }

        // If nothing was fully evaluated at this depth, fall back to previous ordering
        if (scoredMoves.isEmpty()) {
            return orderedMoves;
        }

        // Sort completed moves descending by score (best moves first)
        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));

        // Build new order: completed scored moves first, then remaining moves in prior order
        ArrayList<Zug> newOrder = new ArrayList<>();
        for (ZugScore zs : scoredMoves) {
            newOrder.add(zs.zug);
        }
        for (Zug z : orderedMoves) {
            if (!completed.contains(z)) newOrder.add(z);
        }

        return newOrder;
    }

    // helfer klasse um züge zug sortieren und mit score zu versehen
    static class ZugScore {
        Zug zug;
        int score;

        ZugScore(Zug zug, int score) {
            this.zug = zug;
            this.score = score;
        }
    }


    public static int negamax(Piece [][] board, int depth, int alpha, int beta, boolean isWhite, long hash) {

        if (System.currentTimeMillis() >= searchEndTimeMs) {
            timeUp = true;
            depthAborted = true;
            return Evaluation.evaluation(board, isWhite);
        }

        int alphaOrig = alpha;

        TTEntry entry = transpositionTable.get(hash);

        if (entry != null && entry.isValid && entry.depth >= depth) {
            if (ttLookup(alpha, beta, entry)) {return entry.value;}
        }

        if (depth == 0){
            return qSearch(board, alpha, beta, isWhite, hash);
            //Evaluation.evaluation(board, isWhite);
//            return ;
        }


        ArrayList<Zug> pseudoLegalMoves = possibleMoves(isWhite, board);

        pseudoLegalMoves.removeIf(zug -> !Spiel.isLegalMove(zug, board, isWhite));

        if (pseudoLegalMoves.isEmpty()) {
            if (Spiel.inCheck(board, isWhite)) {
                return -(100000 + depth);
            } else {
                return 0;
            }
        }

        MoveOrdering.orderMoves(pseudoLegalMoves, board, isWhite);

        // If we have a TT entry for this node, try its best move first
        if (entry != null && entry.isValid && entry.bestMove != null) {
            moveToFront(pseudoLegalMoves, entry.bestMove);
        }

        int value = Integer.MIN_VALUE;
        Zug bestMove = null;
        for (Zug zug : pseudoLegalMoves){

            MoveInfo info = saveMoveInfo(zug, board);
            long oldHash = hash;

            hash = doMoveUpdateHash(zug, board, info, hash);

            int child = -negamax(board, depth - 1, -beta, -alpha, !isWhite, hash);
            if (child > value) {
                value = child;
                bestMove = zug;
            }

            undoMove(zug, board, info);
            hash = oldHash;

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

        if (!timeUp) {
            transpositionTable.put(hash, new TTEntry(value, depth, flag, bestMove));
        }

        return value;
    }

    public static int qSearch(Piece [][] board, int alpha, int beta, boolean isWhite, long hash){
        if (System.currentTimeMillis() >= searchEndTimeMs) {
            timeUp = true;
            depthAborted = true;
            return Evaluation.evaluation(board, isWhite);
        }


        TTEntry entry = transpositionTable.get(hash);
        if (entry != null && entry.isValid) {
            if (ttLookup(alpha, beta, entry)) {return entry.value;}
        }

        int alphaOrig = alpha;

        int best_value = Evaluation.evaluation(board, isWhite);

        if( best_value >= beta ) {
            return best_value;
        }

        if( best_value > alpha )
            alpha = best_value;

        ArrayList<Zug> moves = possibleMoves(isWhite, board);

        moves.removeIf(zug -> !Spiel.isLegalMove(zug, board, isWhite));

        if (moves.isEmpty()) {
            if (Spiel.inCheck(board, isWhite)) {
                return -100000;
            } else {
                return 0;
            }
        }

        ArrayList<Zug> forcingMoves = new ArrayList<>();

        for(Zug zug : moves){
            if(Spiel.isCapture(board, zug) || Spiel.promotionQ(zug, board))
                forcingMoves.add(zug);
        }

        MoveOrdering.orderMoves(forcingMoves, board, isWhite);

        // If we have a TT entry for this node, try its best move first
        if (entry != null && entry.isValid && entry.bestMove != null) {
            moveToFront(forcingMoves, entry.bestMove);
        }

        int flag;

        Zug bestMove = null;

        for(Zug zug : forcingMoves)  {

            MoveInfo info = saveMoveInfo(zug, board);

            long oldHash = hash;

            hash = doMoveUpdateHash(zug, board, info, hash);

            int score = -qSearch(board, -beta, -alpha, !isWhite, hash);

            undoMove(zug, board, info);

            hash = oldHash;

            if( score >= beta ) {

                flag = LOWERBOUND;
                if (!timeUp) {
                    transpositionTable.put(hash, new TTEntry(score, 0, flag, zug));
                }
                return score;
            }
            if( score > best_value ) {
                best_value = score;
                bestMove = zug;
            }
            if( score > alpha )
                alpha = score;
        }
        if (best_value <= alphaOrig) flag = UPPERBOUND;
        else flag = EXACT;

        if (!timeUp) {
            transpositionTable.put(hash, new TTEntry(best_value, 0, flag, bestMove));
        }
        return best_value;
    }

    private static boolean ttLookup(int alpha, int beta, TTEntry entry) {
        if (entry.flag == EXACT) {
            return true;
        } else if (entry.flag == LOWERBOUND && entry.value >= beta) {
            return true;
        } else if (entry.flag == UPPERBOUND && entry.value <= alpha) {
            return true;
        }
        return false;
    }


    public static long doMoveUpdateHash(Zug zug, Piece[][] board, MoveInfo info, long hash) {

        boolean [] castleRightsBefore = Spiel.getCastleRights(board);
        int epBefore = Zobrist.getEnPassantFile(board, info.movingPiece.isWhite());

        doMoveNoHash(zug, board, info);

        boolean [] castleRightsAfter = Spiel.getCastleRights(board);
        int epAfter = Zobrist.getEnPassantFile(board, !info.movingPiece.isWhite());;

        hash = Zobrist.updateHash(hash, zug, info, board, castleRightsBefore, castleRightsAfter, epBefore, epAfter);
        
        return hash;
    }
    public static MoveInfo saveMoveInfo(Zug zug, Piece[][] board) {
        MoveInfo info = new MoveInfo();
        Piece movingPiece = board[zug.startY][zug.startX];
        Piece targetPiece = board[zug.endY][zug.endX];
        boolean whiteToMove = movingPiece.isWhite();

//        info.oldHash = currentHash;

        info.enPassantBauerCoords = Spiel.bauerHasEnPassantFlag(board);

        if(movingPiece instanceof Bauer){
            info.wasEnPassantCapturable = ((Bauer) movingPiece).isEnPassantPossible();
            if(Spiel.wasEnPassant(zug, board, targetPiece)){
                info.wasEnPassant = true;
                info.capturedPiece = board[zug.startY][zug.endX];
                info.capEnPassantBauerCoords = new Koordinaten(zug.endX, zug.startY);
            }
            if(Spiel.willPromote(zug, board)) {
                info.wasPromotion = true;
                char c = zug.promoteTo;
                info.promotionPiece = switch (c){
                    case 'q' -> new Dame(whiteToMove);
                    case 'b' -> new Laeufer(whiteToMove);
                    case 'n' -> new Springer(whiteToMove);
                    case 'r' -> new Turm(whiteToMove);
                    default -> null;
                };
            }
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
        if (Spiel.rochade(zug, board)) {
            if (Spiel.kurze(zug) && Spiel.kurzePossible(zug, board)) {
                info.rookMoved = true;
                info.rookStartX = 7;
                info.rookEndX = 5;
            } else if (!Spiel.kurze(zug) && Spiel.langePossible(zug, board)) {
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

        Spiel.resetMovingPiece(zug, board, info);

        // geschlagene
        if(Spiel.wasEnPassant(zug, board, info.squareMovedOnto)){
            Spiel.enPassantResetExe(board, zug);
        } else {
            Spiel.resetSquareMovedOnto(board, zug, info);
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

//        currentHash = info.oldHash;
        
        // Update piece tracker for undo
        Board.pieceTracker.undoMove(zug, info, board);
    }

    // Time-limited iterative deepening; search runs until deadline and returns best-so-far
    public static Zug iterativeDeepening (Piece[][] board, boolean isWhite, long hash, long timeLimitMs){

        long now = System.currentTimeMillis();
        setSearchDeadline(now + Math.max(1, timeLimitMs));


        ArrayList<Zug> order = possibleMoves(isWhite, board);
        if (order.isEmpty()) return null;

        MoveOrdering.orderMoves(order, board, isWhite);

        Zug bestSoFar = order.getFirst();

        int depth = 1;
        while (depth <= 64) {
            if (System.currentTimeMillis() >= searchEndTimeMs) break;
            // Prepare depth
            timeUp = false;
            depthAborted = false;
            // System.out.println("Tiefe: " + depth);
            ArrayList<Zug> newOrder = findBestMoves(board, depth, isWhite, order, hash);

            // Adopt improvements found so far at this depth
            if (!newOrder.isEmpty()) {
                bestSoFar = newOrder.getFirst();
                order = newOrder;
            }

            // If depth aborted due to timeout, stop after adopting partial improvements
            if (depthAborted || timeUp || System.currentTimeMillis() >= searchEndTimeMs) {
                break;
            }

            depth++;
        }
        return bestSoFar;
    }
    public static void setSearchDeadline(long deadlineMs) {
        searchEndTimeMs = deadlineMs;
        timeUp = false;
    }

    // Fixed-depth search utility used for low-time situations
    public static Zug searchToDepth(Piece[][] board, boolean isWhite, long hash, int depth) {
        setSearchDeadline(Long.MAX_VALUE);
        ArrayList<Zug> order = possibleMoves(isWhite, board);
        if (order.isEmpty()) return null;
	
	    MoveOrdering.orderMoves(order, board, isWhite);

        ArrayList<Zug> sorted = findBestMoves(board, depth, isWhite, order, hash);
        if (sorted.isEmpty()) return null;
        return sorted.getFirst();
    }
    public static void doMoveNoHash(Zug zug, Piece[][] board, MoveInfo info) {

        boolean bauerDoppelZug = Spiel.bauerDoppel(zug, board);

        if (Spiel.rochade(zug, board)) {
            if(Spiel.kurze(zug)) { // kurze Rochade
                Spiel.kurzeRochade(zug, board);
            } else{ // lange Rochade
                Spiel.langeRochade(zug, board);
            }
        }

        // En-Passant
        if (Spiel.enPassant(zug, board)) {
            Spiel.enPassantExe(zug, board);
        }

        //Normaler Zug
        Spiel.normalTurnExe(zug, board);

        //Promotion
        if (Spiel.promotion(zug, board)) {
            Spiel.promotionExe(zug, board, board[zug.endY][zug.endX].isWhite());
        }

        // en passant flags resetten und dann die neue setzen
        Spiel.resetPawnEnPassant(board);
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
        // Update piece tracker
        Board.pieceTracker.updateMove(zug, info, board);
    }

    // Helpers to prioritize TT best move in ordering
    private static boolean sameMove(Zug a, Zug b) {
        if (a == null || b == null) return false;
        if (a.startX != b.startX || a.startY != b.startY || a.endX != b.endX || a.endY != b.endY) return false;
        return a.promoteTo == b.promoteTo;
    }

    private static void moveToFront(List<Zug> moves, Zug target) {
        if (moves == null || moves.isEmpty() || target == null) return;
        for (int i = 0; i < moves.size(); i++) {
            if (sameMove(moves.get(i), target)) {
                if (i > 0) {
                    Zug z = moves.remove(i);
                    moves.add(0, z);
                }
                break;
            }
        }
    }
}
