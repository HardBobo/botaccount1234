import java.util.*;

public class MoveFinder {

    // Time control for search
    private static volatile long searchEndTimeMs = Long.MAX_VALUE;
    private static volatile boolean timeUp = false;
    // Set to true if an iteration was aborted due to timeout inside the search
    private static volatile boolean depthAborted = false;

    // Node counting for debug output
    private static long nodeCount = 0;
    private static long searchStartTime = 0;

    static final int EXACT = 0;
    static final int LOWERBOUND = 1;
    static final int UPPERBOUND = 2;

    static final int NMP_MIN_DEPTH = 3;
    static final int REDUCTION_NMP = 2;

    static Map<Long, TTEntry> transpositionTable = new HashMap<>();

    public static ArrayList<Zug> possibleMoves(boolean white, Piece[][] board) {
        return Board.pieceTracker.generateMoves(white, board);
    }

    public static SearchResult findBestMovesWithAspirationWindow(Piece[][] board, int depth, boolean isWhite, ArrayList<Zug> orderedMoves, long hash, int alpha, int beta) {
        if (System.currentTimeMillis() >= searchEndTimeMs) { timeUp = true; return new SearchResult(orderedMoves, 0, false); }

        // Remove illegal moves
        orderedMoves.removeIf(zug -> !Spiel.isLegalMove(zug, board, isWhite));
        if (orderedMoves.isEmpty()) return new SearchResult(new ArrayList<>(), 0, false);

        // Prefer TT best move at root if available
        TTEntry rootTT = transpositionTable.get(hash);
        if (rootTT != null && rootTT.isValid && rootTT.bestMove != null) {
            moveToFront(orderedMoves, rootTT.bestMove);
        }

        // List to hold moves with their scores (only fully searched moves)
        ArrayList<ZugScore> scoredMoves = new ArrayList<>();
        HashSet<Zug> completed = new HashSet<>();

        int bestScore = Integer.MIN_VALUE;
        boolean failedLow = false;
        boolean failedHigh = false;

        for (Zug zug : orderedMoves) {
            if (System.currentTimeMillis() >= searchEndTimeMs) { timeUp = true; break; }

            MoveInfo info = saveMoveInfo(zug, board);
            long oldHash = hash;

            hash = doMoveUpdateHash(zug, board, info, hash);

            // Search with aspiration window
            int score = -negamax(board, depth-1, -beta, -alpha, !isWhite, hash, true);

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

            // Track best score and check for aspiration window failures
            if (score > bestScore) {
                bestScore = score;
            }

            // Check for fail-high (score >= beta)
            if (score >= beta) {
                failedHigh = true;
                break; // This move is too good, opponent won't allow it
            }

            // Update alpha for next move
            if (score > alpha) {
                alpha = score;
            }
        }

        // Check for fail-low (bestScore <= original alpha)
        if (bestScore <= alpha && !scoredMoves.isEmpty()) {
            failedLow = true;
        }

        // If nothing was fully evaluated at this depth, fall back to previous ordering
        if (scoredMoves.isEmpty()) {
            return new SearchResult(orderedMoves, 0, false);
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

        // Return the best score from the sorted moves
        int actualBestScore = scoredMoves.getFirst().score;
        return new SearchResult(newOrder, actualBestScore, true);
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

    // Result class to return both moves and best score
    static class SearchResult {
        ArrayList<Zug> moves;
        int bestScore;
        boolean hasScore; // true if we have a valid score

        SearchResult(ArrayList<Zug> moves, int bestScore, boolean hasScore) {
            this.moves = moves;
            this.bestScore = bestScore;
            this.hasScore = hasScore;
        }
    }


    // Backward-compatible wrapper: allow null moves by default
    public static int negamax(Piece [][] board, int depth, int alpha, int beta, boolean isWhite, long hash) {
        return negamax(board, depth, alpha, beta, isWhite, hash, true);
    }

    public static int negamax(Piece [][] board, int depth, int alpha, int beta, boolean isWhite, long hash, boolean canNull) {
        nodeCount++; // Count this node

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
        }

        // Futility context
        boolean inCheckNow = Spiel.inCheck(board, isWhite);
        boolean nearMateBounds = (alpha <= -(100000 - 200)) || (beta >= (100000 - 200));
        int staticEval = 0;
        boolean haveStaticEval = false;
        if (!inCheckNow && !nearMateBounds && depth <= 2) {
            staticEval = Evaluation.evaluation(board, isWhite);
            haveStaticEval = true;
        }

        ArrayList<Zug> pseudoLegalMoves = possibleMoves(isWhite, board);

        pseudoLegalMoves.removeIf(zug -> !Spiel.isLegalMove(zug, board, isWhite));

        if (pseudoLegalMoves.isEmpty()) {
            if (inCheckNow) {
                return -(100000 + depth);
            } else {
                return 0;
            }
        }

        // Node-level futility pruning (frontier and extended)
        if (!inCheckNow && !nearMateBounds && haveStaticEval) {
            // Depth 1: frontier futility pruning
            if (depth == 1) {
                int margin1 = 300; // ~ minor piece
                if (staticEval + margin1 <= alpha) {
                    return alpha; // fail-low hard as per CPW/TR
                }
            }
            // Depth 2: extended futility pruning
            if (depth == 2) {
                int margin2 = 500; // ~ rook
                if (staticEval + margin2 <= alpha) {
                    return alpha; // fail-low hard
                }
            }
        }
        
        boolean nonPV = (beta - alpha == 1);

        // Null Move Pruning (guard against consecutive null, pawn-only, in-check, and near-mate bounds)
        if (canNull && nonPV && !inCheckNow && !nearMateBounds && !PieceTracker.onlyHasPawns(isWhite) && depth >= NMP_MIN_DEPTH) {

            long oldHash = hash;
            NullState ns = new  NullState();
            hash = doNullMoveUpdateHash(board, hash, ns);
            int nullMoveScore = -negamax(board, depth - REDUCTION_NMP, -beta, -beta + 1, !isWhite, hash, false);
            undoNullMove(board, ns);
            hash = oldHash;

            if(nullMoveScore >= beta) {
                transpositionTable.put(hash, new TTEntry(nullMoveScore, depth, LOWERBOUND));
                return beta;
            }
        }

        MoveOrdering.orderMoves(pseudoLegalMoves, board, isWhite);

        // If we have a TT entry for this node, try its best move first
        if (entry != null && entry.isValid && entry.bestMove != null) {
            moveToFront(pseudoLegalMoves, entry.bestMove);
        }

        int value = Integer.MIN_VALUE;
        Zug bestMove = null;
        int moveIndex = 0;
        boolean firstMove = true;
        for (Zug zug : pseudoLegalMoves){

            // Precompute quietness once for this move (before making it)
            boolean isQuietMove = !Spiel.isCapture(board, zug) && !Spiel.willPromote(zug, board);

            MoveInfo info = saveMoveInfo(zug, board);
            long oldHash = hash;

            hash = doMoveUpdateHash(zug, board, info, hash);

            // Determine if gives check after making the move
            boolean givesCheck = Spiel.inCheck(board, !isWhite);

            // Move-level futility pruning at frontier (depth 1), after we know if it gives check:
            if (!inCheckNow && !nearMateBounds && depth == 1 && isQuietMove && !givesCheck) {
                // ensure staticEval available
                if (!haveStaticEval) { staticEval = Evaluation.evaluation(board, isWhite); haveStaticEval = true; }
                int moveMargin = 150; // safety margin
                if (staticEval + moveMargin <= alpha) {
                    // prune this quiet move
                    undoMove(zug, board, info);
                    hash = oldHash;
                    moveIndex++;
                    continue;
                }
            }

            // Late Move Reductions (LMR):
            // reduce late, quiet, non-check moves at non-PV nodes when depth >= 3 and not in check


            int child;
            if (firstMove) {
                // Principal variation move: full-window search
                child = -negamax(board, depth - 1, -beta, -alpha, !isWhite, hash);
            } else {
                // PVS for later moves: start with null-window, possibly reduced by LMR
                boolean applyLMR = nonPV && !inCheckNow && depth >= 3 && moveIndex >= 3 && isQuietMove && !givesCheck;
                int r = applyLMR ? 1 : 0;
                int searchDepth = depth - 1 - r;
                if (searchDepth < 1) searchDepth = depth - 1; // safety

                // Null-window probe
                child = -negamax(board, searchDepth, -alpha - 1, -alpha, !isWhite, hash);

                // If raised alpha in reduced probe, re-search
                if (child > alpha) {
                    // If reduced, re-search at full depth null-window first
                    if (r > 0 && (depth - 1) >= 1) {
                        child = -negamax(board, depth - 1, -alpha - 1, -alpha, !isWhite, hash);
                    }
                    // If still raises alpha and not fail-high, re-search full window
                    if (child > alpha && child < beta) {
                        child = -negamax(board, depth - 1, -beta, -alpha, !isWhite, hash);
                    }
                }
            }

            if (child > value) {
                value = child;
                bestMove = zug;
            }

            undoMove(zug, board, info);
            hash = oldHash;

            alpha = Math.max(alpha, value);

            if(alpha >= beta)
                break; //alpha beta cutoff

            firstMove = false;
            moveIndex++;
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
        nodeCount++; // Count this node

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

    public static long doNullMoveUpdateHash(Piece [][] board, long hash, NullState ns) {
        ns.epPawn = Spiel.bauerHasEnPassantFlag(board);
        Spiel.resetPawnEnPassant(board);
        return Zobrist.nullMoveHashUpdate(hash, ns.epPawn);
    }

    public static void undoNullMove(Piece [][] board, NullState ns) {
        if (ns.epPawn != null) {
            ((Bauer) board[ns.epPawn.y][ns.epPawn.x]).setEnPassantPossible(true);
        }
    }

    // --- Null Move helpers ---
    private static class NullState {
        Koordinaten epPawn; // pawn that had en passant flag before null move
    }

    public static MoveInfo saveMoveInfo(Zug zug, Piece[][] board) {
        MoveInfo info = new MoveInfo();
        Piece movingPiece = board[zug.startY][zug.startX];
        Piece targetPiece = board[zug.endY][zug.endX];
        boolean whiteToMove = movingPiece.isWhite();

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

        searchStartTime = System.currentTimeMillis();
        setSearchDeadline(searchStartTime + Math.max(1, timeLimitMs));
        nodeCount = 0; // Reset node counter

        ArrayList<Zug> order = possibleMoves(isWhite, board);
        if (order.isEmpty()) return null;

        MoveOrdering.orderMoves(order, board, isWhite);

        Zug bestSoFar = order.getFirst();
        int previousScore = 0;
        boolean hasPreviousScore = false;

        System.out.println("Suche gestartet - Tiefe | Zeit(ms) | Nodes/Sek | Zug     | Eval");
        System.out.println("---------------------------------------------------------------");

        int depth = 1;
        while (depth <= 64) {
            if (System.currentTimeMillis() >= searchEndTimeMs) break;

            // Prepare depth
            timeUp = false;
            depthAborted = false;
            long depthStartTime = System.currentTimeMillis();
            long nodesAtDepthStart = nodeCount;

            SearchResult result;

            if (depth == 1 || !hasPreviousScore) {
                // First depth or no previous score - use full window
                result = findBestMovesWithAspirationWindow(board, depth, isWhite, order, hash, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
            } else {
                // Use aspiration window based on previous score
                final int ASPIRATION_WINDOW = 50;
                int alpha = previousScore - ASPIRATION_WINDOW;
                int beta = previousScore + ASPIRATION_WINDOW;

                result = searchWithAspirationWindowRetries(board, depth, isWhite, order, hash, alpha, beta, previousScore);
            }

            // Calculate stats for this depth
            long depthEndTime = System.currentTimeMillis();
            long depthTime = Math.max(1, depthEndTime - depthStartTime);
            long nodesThisDepth = nodeCount - nodesAtDepthStart;
            long nps = (nodesThisDepth * 1000L) / depthTime;
            long totalTime = depthEndTime - searchStartTime;

            // Adopt improvements found so far at this depth
            // Only update best move when we have a fully evaluated result at this depth
            if (result.hasScore && !result.moves.isEmpty()) {
                bestSoFar = result.moves.getFirst();
                previousScore = result.bestScore;
                hasPreviousScore = true;
            }
            // Always refresh ordering to keep completed moves first
            if (!result.moves.isEmpty()) {
                order = result.moves;
            }
            // Debug output for completed depth
            if (!depthAborted && !timeUp) {
                String moveStr = String.format("%-7s", bestSoFar.processZug());
                String evalStr = result.hasScore ? String.format("%+4d", result.bestScore) : "  ?";
                System.out.printf("   %2d    | %7d | %9d | %s | %s%n",
                        depth, totalTime, nps, moveStr, evalStr);
            } else {
                // Partial depth completed
                String moveStr = String.format("%-7s", bestSoFar.processZug());
                String evalStr = result.hasScore ? String.format("%+4d", result.bestScore) : "  ?";
                System.out.printf("   %2d*   | %7d | %9d | %s | %s (teilweise)%n",
                        depth, totalTime, nps, moveStr, evalStr);
            }

            // If depth aborted due to timeout, stop after adopting partial improvements
            if (depthAborted || timeUp || System.currentTimeMillis() >= searchEndTimeMs) {
                break;
            }

            depth++;
        }

        // Final summary
        long totalTime = System.currentTimeMillis() - searchStartTime;
        long totalNps = totalTime > 0 ? (nodeCount * 1000L) / totalTime : 0;
        System.out.println("---------------------------------------------------------------");
        System.out.printf("Gesamt: %d Tiefe, %dms, %d Knoten, %d NPS%n",
                depth - 1, totalTime, nodeCount, totalNps);
        System.out.printf("Bester Zug: %s (Eval: %s)%n",
                bestSoFar.processZug(), hasPreviousScore ? String.format("%+d", previousScore) : "?");
        System.out.println();

        return bestSoFar;
    }

    // Helper method to handle aspiration window retries on fail-high/fail-low
    private static SearchResult searchWithAspirationWindowRetries(Piece[][] board, int depth, boolean isWhite, ArrayList<Zug> order, long hash, int alpha, int beta, int expectedScore) {
        SearchResult result = findBestMovesWithAspirationWindow(board, depth, isWhite, order, hash, alpha, beta);

        // If we have a score and it's outside our aspiration window, we need to re-search with wider window
        if (result.hasScore) {
            if (result.bestScore <= alpha) {
                // Fail low - research with lowered alpha
                return findBestMovesWithAspirationWindow(board, depth, isWhite, order, hash, Integer.MIN_VALUE + 1, beta);
            } else if (result.bestScore >= beta) {
                // Fail high - research with raised beta
                return findBestMovesWithAspirationWindow(board, depth, isWhite, order, hash, alpha, Integer.MAX_VALUE - 1);
            }
        }

        return result;
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

        int prevScore = 0;
        boolean hasPreviousScore = false;

        Zug bestSoFar = order.getFirst();

        for(int i = 0; i < depth; i++){

            SearchResult result;

            if (depth == 1 || !hasPreviousScore) {
                // First depth or no previous score - use full window
                result = findBestMovesWithAspirationWindow(board, depth, isWhite, order, hash, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
            } else {
                // Use aspiration window based on previous score
                final int ASPIRATION_WINDOW = 50;
                int alpha = prevScore - ASPIRATION_WINDOW;
                int beta = prevScore + ASPIRATION_WINDOW;

                result = searchWithAspirationWindowRetries(board, depth, isWhite, order, hash, alpha, beta, prevScore);
            }

            // Adopt improvements found so far at this depth
            if (!result.moves.isEmpty()) {
                bestSoFar = result.moves.getFirst();
                order = result.moves;

                // Update previous score for next iteration if we have a valid score
                if (result.hasScore) {
                    prevScore = result.bestScore;
                    hasPreviousScore = true;
                }
            }
        }
        return bestSoFar;
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