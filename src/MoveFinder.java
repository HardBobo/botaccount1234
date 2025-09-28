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

    // Delta pruning constants for qSearch (conservative)
    private static final int[] DELTA_PIECE_VALUES = {100, 300, 310, 500, 1000, 0}; // P, N, B, R, Q, K
    private static final int DELTA_MARGIN = 200; // safety margin in centipawns

    static final int NMP_MIN_DEPTH = 3;
    static final int REDUCTION_NMP = 2;

    static Map<Long, TTEntry> transpositionTable = new HashMap<>();

    public static ArrayList<Zug> possibleMoves(boolean white) {
        // Bitboard-based move generation
        return BitboardMoveGen.generate(white, Board.bitboards);
    }

    public static SearchResult findBestMovesWithAspirationWindow(int depth, boolean isWhite, ArrayList<Zug> orderedMoves, long hash, int alpha, int beta) {
        if (System.currentTimeMillis() >= searchEndTimeMs) { timeUp = true; return new SearchResult(orderedMoves, 0, false); }

        // Remove illegal moves using bitboards
        orderedMoves.removeIf(zug -> !Board.bitboards.isLegalMove(zug, isWhite));
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

            MoveInfo info = saveMoveInfo(zug);
            long oldHash = hash;

            hash = doMoveUpdateHash(zug, info, hash);

            // Search with aspiration window
            int score = -negamax(depth-1, -beta, -alpha, !isWhite, hash, true);

            // Always undo before any potential early return/break
            undoMove(zug, info);
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
    public static int negamax(int depth, int alpha, int beta, boolean isWhite, long hash) {
        return negamax(depth, alpha, beta, isWhite, hash, true);
    }

    public static int negamax(int depth, int alpha, int beta, boolean isWhite, long hash, boolean canNull) {

        if (System.currentTimeMillis() >= searchEndTimeMs) {
            timeUp = true;
            depthAborted = true;
            return Evaluation.evaluation(isWhite);
        }

        int alphaOrig = alpha;

        TTEntry entry = transpositionTable.get(hash);

        if (entry != null && entry.isValid && entry.depth >= depth) {
            if (ttLookup(alpha, beta, entry)) {return entry.value;}
        }

        if (depth == 0){
            return qSearch(alpha, beta, isWhite, hash);
        }

        // Futility context
        boolean inCheckNow = Board.bitboards.inCheck(isWhite);
        boolean nearMateBounds = (alpha <= -(100000 - 200)) || (beta >= (100000 - 200));
        int staticEval = 0;
        boolean haveStaticEval = false;
        if (!inCheckNow && !nearMateBounds && depth <= 2) {
            staticEval = Evaluation.evaluation(isWhite);
            haveStaticEval = true;
        }

        ArrayList<Zug> pseudoLegalMoves = possibleMoves(isWhite);

        pseudoLegalMoves.removeIf(zug -> !Board.bitboards.isLegalMove(zug, isWhite));

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
        // Adaptive reduction: r = 2 normally, r = 3 for deeper nodes
        int nmpR = 2 + (depth >= 7 ? 1 : 0);
        if (canNull && nonPV && !inCheckNow && !nearMateBounds && !Board.bitboards.onlyHasPawns(isWhite) && depth >= (nmpR + 1)) {

            long oldHash = hash;
            NullState ns = new  NullState();
            hash = doNullMoveUpdateHash(hash, ns);
            int nullMoveScore = -negamax(depth - 1 - nmpR, -beta, -beta + 1, !isWhite, hash, false);
            undoNullMove(ns);
            hash = oldHash;

            if(nullMoveScore >= beta) {
                transpositionTable.put(hash, new TTEntry(nullMoveScore, depth, LOWERBOUND));
                return beta;
            }
        }

        MoveOrdering.orderMoves(pseudoLegalMoves, isWhite);

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
            boolean isQuietMove = !Board.bitboards.isCapture(zug) && !Board.bitboards.willPromote(zug);

            MoveInfo info = saveMoveInfo(zug);
            long oldHash = hash;

            hash = doMoveUpdateHash(zug, info, hash);

            // Determine if gives check after making the move
            boolean givesCheck = Board.bitboards.inCheck(!isWhite);

            // Move-level futility pruning at frontier (depth 1), after we know if it gives check:
            if (!inCheckNow && !nearMateBounds && depth == 1 && isQuietMove && !givesCheck) {
                // ensure staticEval available
                if (!haveStaticEval) { staticEval = Evaluation.evaluation(isWhite); haveStaticEval = true; }
                int moveMargin = 150; // safety margin
                if (staticEval + moveMargin <= alpha) {
                    // prune this quiet move
                    undoMove(zug, info);
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
                child = -negamax(depth - 1, -beta, -alpha, !isWhite, hash);
            } else {
                // PVS for later moves: start with null-window, possibly reduced by LMR
                boolean applyLMR = nonPV && !inCheckNow && depth >= 3 && moveIndex >= 3 && isQuietMove && !givesCheck;
                int r = applyLMR ? 1 : 0;
                int searchDepth = depth - 1 - r;
                if (searchDepth < 1) searchDepth = depth - 1; // safety

                // Null-window probe
                child = -negamax(searchDepth, -alpha - 1, -alpha, !isWhite, hash);

                // If raised alpha in reduced probe, re-search
                if (child > alpha) {
                    // If reduced, re-search at full depth null-window first
                    if (r > 0 && (depth - 1) >= 1) {
                        child = -negamax(depth - 1, -alpha - 1, -alpha, !isWhite, hash);
                    }
                    // If still raises alpha and not fail-high, re-search full window
                    if (child > alpha && child < beta) {
                        child = -negamax(depth - 1, -beta, -alpha, !isWhite, hash);
                    }
                }
            }

            if (child > value) {
                value = child;
                bestMove = zug;
            }

            undoMove(zug, info);
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

    public static int qSearch(int alpha, int beta, boolean isWhite, long hash){
        
        if (System.currentTimeMillis() >= searchEndTimeMs) {
            timeUp = true;
            depthAborted = true;
            return Evaluation.evaluation(isWhite);
        }
        
        // Near mate bounds guard for pruning heuristics
        boolean nearMateBounds = (alpha <= -(100000 - 200)) || (beta >= (100000 - 200));
        
        TTEntry entry = transpositionTable.get(hash);
        if (entry != null && entry.isValid) {
            if (ttLookup(alpha, beta, entry)) {return entry.value;}
        }

        int alphaOrig = alpha;

        int best_value = Evaluation.evaluation(isWhite);
        
        if( best_value >= beta ) {
            return best_value;
        }
        
        if( best_value > alpha )
            alpha = best_value;
        
        // Detect if side to move is in check – disable delta pruning if so
        boolean inCheckNow = Board.bitboards.inCheck(isWhite);
        
        ArrayList<Zug> moves = possibleMoves(isWhite);

        moves.removeIf(zug -> !Board.bitboards.isLegalMove(zug, isWhite));

        if (moves.isEmpty()) {
            if (Board.bitboards.inCheck(isWhite)) {
                return -100000;
            } else {
                return 0;
            }
        }

        ArrayList<Zug> forcingMoves = new ArrayList<>();

        for(Zug zug : moves){
            if(Board.bitboards.isCapture(zug) || (Board.bitboards.willPromote(zug) && zug.promoteTo == 'q'))
                forcingMoves.add(zug);
        }

        MoveOrdering.orderMoves(forcingMoves, isWhite);

        // If we have a TT entry for this node, try its best move first
        if (entry != null && entry.isValid && entry.bestMove != null) {
            moveToFront(forcingMoves, entry.bestMove);
        }

        int flag;

        Zug bestMove = null;

        for(Zug zug : forcingMoves)  {
            // Delta pruning (move-level): conservative application only at non-PV nodes,
            // skipping promotions and en passant to avoid tactical misses.
            boolean nonPVq = (beta - alpha == 1);
            if (nonPVq && !nearMateBounds && !inCheckNow) {
                // Skip delta pruning for promotions and en passant
                boolean isPromotion = (zug.promoteTo == 'q');
                int fromSq = zug.startY * 8 + zug.startX;
                int toSq = zug.endY * 8 + zug.endX;
                boolean moverWhite = (Board.bitboards.occW & (1L << fromSq)) != 0L;
                int moverType = Board.bitboards.pieceTypeAt(fromSq, moverWhite);
                boolean isEP = (moverType == 0) && (Board.bitboards.epSquare == toSq) && (zug.endX != zug.startX) && (((Board.bitboards.occ >>> toSq) & 1L) == 0L);
                if (!isPromotion && !isEP) {
                    int capValue;
                    if (((Board.bitboards.occ >>> toSq) & 1L) != 0L) {
                        boolean victimWhite = (Board.bitboards.occW & (1L << toSq)) != 0L;
                        int victimType = Board.bitboards.pieceTypeAt(toSq, victimWhite);
                        capValue = DELTA_PIECE_VALUES[victimType];
                    } else {
                        // Should only happen for en passant, but we excluded EP above, so treat as pawn
                        capValue = DELTA_PIECE_VALUES[0];
                    }
                    if (best_value + capValue + DELTA_MARGIN <= alpha) {
                        continue; // prune futile capture
                    }
                }
            }
        
            MoveInfo info = saveMoveInfo(zug);
        
            long oldHash = hash;
        
            hash = doMoveUpdateHash(zug, info, hash);
        
            int score = -qSearch(-beta, -alpha, !isWhite, hash);
        
            undoMove(zug, info);
        
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


    public static long doMoveUpdateHash(Zug zug, MoveInfo info, long hash) {

        // Use bitboards for castle rights and en-passant file to avoid relying on mirror flags
        boolean[] castleRightsBefore = rightsFromMask(Board.bitboards.rightsMask());
        int fromSqForColor = zug.startY * 8 + zug.startX;
        boolean moverWhiteForHash = (Board.bitboards.occW & (1L << fromSqForColor)) != 0L;
        int epBefore = Zobrist.getEnPassantFileFromBB(Board.bitboards, moverWhiteForHash);

        doMoveNoHash(zug, info);

        boolean[] castleRightsAfter = rightsFromMask(Board.bitboards.rightsMask());
        int epAfter = Zobrist.getEnPassantFileFromBB(Board.bitboards, !moverWhiteForHash);

        hash = Zobrist.updateHash(hash, zug, info, castleRightsBefore, castleRightsAfter, epBefore, epAfter);

        return hash;
    }

    public static long doNullMoveUpdateHash(long hash, NullState ns) {
        // Save old EP file from bitboards and clear EP for null move
        ns.oldEpSquare = Board.bitboards.epSquare;
        Board.bitboards.epSquare = -1;
        return Zobrist.nullMoveHashUpdate(hash, ns.oldEpSquare == -1 ? -1 : Bitboards.xOf(ns.oldEpSquare));
    }

    public static void undoNullMove(NullState ns) {
        // Restore previous EP square in bitboards (no mirror flags needed)
        Board.bitboards.epSquare = ns.oldEpSquare;
    }

    // --- Null Move helpers ---
    private static class NullState {
        int oldEpSquare; // -1 or 0..63 square that was EP target before null move
    }

    public static MoveInfo saveMoveInfo(Zug zug) {
        // Bitboard-driven make/undo will populate MoveInfo fields as needed.
        return new MoveInfo();
    }
    public static void undoMove(Zug zug, MoveInfo info) {
        // Undo on bitboards (also mirrors to Board.brett)
        Board.bitboards.undoMove(zug, info);

        // No PieceTracker updates (bitboards are authoritative now)
    }

    // Time-limited iterative deepening; search runs until deadline and returns best-so-far
    public static Zug iterativeDeepening (boolean isWhite, long hash, long timeLimitMs){
        setSearchDeadline(System.currentTimeMillis() + Math.max(1, timeLimitMs));

        ArrayList<Zug> order = possibleMoves(isWhite);
        if (order.isEmpty()) return null;

        MoveOrdering.orderMoves(order, isWhite);

        Zug bestSoFar = order.getFirst();
        int previousScore = 0;
        boolean hasPreviousScore = false;

        int depth = 1;
        while (depth <= 64) {
            if (System.currentTimeMillis() >= searchEndTimeMs) break;

            timeUp = false;
            depthAborted = false;

            SearchResult result;

            if (depth == 1 || !hasPreviousScore) {
                result = findBestMovesWithAspirationWindow(depth, isWhite, order, hash, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
            } else {
                final int ASPIRATION_WINDOW = 50;
                int alpha = previousScore - ASPIRATION_WINDOW;
                int beta = previousScore + ASPIRATION_WINDOW;

                result = searchWithAspirationWindowRetries(depth, isWhite, order, hash, alpha, beta, previousScore);
            }

            if (result.hasScore && !result.moves.isEmpty()) {
                bestSoFar = result.moves.getFirst();
                previousScore = result.bestScore;
                hasPreviousScore = true;
            }
            if (!result.moves.isEmpty()) {
                order = result.moves;
            }

            if (depthAborted || timeUp || System.currentTimeMillis() >= searchEndTimeMs) {
                break;
            }

            depth++;
        }

        // Emit exactly one minimal UCI info line so GUIs like fastchess don't warn
        int reportedDepth = Math.max(1, depth - 1);
        int reportScore = hasPreviousScore ? previousScore : 0;
        System.out.printf("info depth %d score cp %d%n", reportedDepth, reportScore);

        return bestSoFar;
    }

    // Helper method to handle aspiration window retries on fail-high/fail-low
    private static SearchResult searchWithAspirationWindowRetries(int depth, boolean isWhite, ArrayList<Zug> order, long hash, int alpha, int beta, int expectedScore) {
        SearchResult result = findBestMovesWithAspirationWindow(depth, isWhite, order, hash, alpha, beta);

        // If we have a score and it's outside our aspiration window, we need to re-search with wider window
        if (result.hasScore) {
            if (result.bestScore <= alpha) {
                // Fail low - research with lowered alpha
                return findBestMovesWithAspirationWindow(depth, isWhite, order, hash, Integer.MIN_VALUE + 1, beta);
            } else if (result.bestScore >= beta) {
                // Fail high - research with raised beta
                return findBestMovesWithAspirationWindow(depth, isWhite, order, hash, alpha, Integer.MAX_VALUE - 1);
            }
        }

        return result;
    }
    public static void setSearchDeadline(long deadlineMs) {
        searchEndTimeMs = deadlineMs;
        timeUp = false;
    }

    // Fixed-depth search utility used for low-time situations
    public static Zug searchToDepth(boolean isWhite, long hash, int depth) {
        setSearchDeadline(Long.MAX_VALUE);
        ArrayList<Zug> order = possibleMoves(isWhite);
        if (order.isEmpty()) return null;

        MoveOrdering.orderMoves(order, isWhite);

        int prevScore = 0;
        boolean hasPreviousScore = false;

        Zug bestSoFar = order.getFirst();

        for(int i = 0; i < depth; i++){

            SearchResult result;

            if (depth == 1 || !hasPreviousScore) {
                // First depth or no previous score - use full window
                result = findBestMovesWithAspirationWindow(depth, isWhite, order, hash, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
            } else {
                // Use aspiration window based on previous score
                final int ASPIRATION_WINDOW = 50;
                int alpha = prevScore - ASPIRATION_WINDOW;
                int beta = prevScore + ASPIRATION_WINDOW;

                result = searchWithAspirationWindowRetries(depth, isWhite, order, hash, alpha, beta, prevScore);
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

    public static void doMoveNoHash(Zug zug, MoveInfo info) {

        // Apply move using bitboards (also mirrors to Board.brett)
        Board.bitboards.applyMove(zug, info);

        // No PieceTracker updates (bitboards are authoritative now)
    }

    private static boolean[] rightsFromMask(int m) {
        return new boolean[]{ (m & 1) != 0, (m & 2) != 0, (m & 4) != 0, (m & 8) != 0 };
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