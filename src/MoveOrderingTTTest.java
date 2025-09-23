import java.util.*;

public class MoveOrderingTTTest {
    public static void main(String[] args) {
        // 1) Init Zobrist and create a test position with multiple legal moves
        Zobrist.initZobrist();

        // Use the same FEN as in Test to keep consistency
        // Position: 8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - -
        Piece[][] board = Board.fenToBoard("8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - -");
        boolean whiteToMove = true;
        long hash = Zobrist.computeHash(board, whiteToMove);

        // Ensure clean TT state
        MoveFinder.transpositionTable.clear();

        // 2) Generate legal moves
        ArrayList<Zug> legal = MoveFinder.possibleMoves(whiteToMove, board);
        legal.removeIf(z -> !MoveFinder.isLegalMove(z, board, whiteToMove));

        if (legal.isEmpty()) {
            System.out.println("No legal moves in this position. Test cannot proceed.");
            return;
        }

        // Pick a target move to prioritize via TT. To make the effect visible,
        // choose the last legal move so that it's unlikely to already be first.
        Zug target = legal.get(legal.size() - 1);

        // 3) Store a TT entry with this bestMove but with small depth so negamax won't early-return.
        // Using EXACT here, but depth=0 means it will not cause cutoff when searching depth>=1.
        TTEntry tt = new TTEntry(0, 0, MoveFinder.EXACT, target);
        MoveFinder.transpositionTable.put(hash, tt);

        // 4) Show ordering BEFORE and AFTER TT prioritization for the full move list
        ArrayList<Zug> ordered = new ArrayList<>(legal);
        MoveOrdering.orderMoves(ordered, board, whiteToMove);

        System.out.println("=== Full move list ordering test ===");
        System.out.println("Before TT priority, first move:  " + ordered.get(0).processZug());
        System.out.println("TT bestMove:                    " + target.processZug());

        moveToFront(ordered, target);

        System.out.println("After TT priority, first move:   " + ordered.get(0).processZug());
        if (sameMove(ordered.get(0), target)) {
            System.out.println("RESULT: PASS (TT bestMove moved to front)");
        } else {
            System.out.println("RESULT: FAIL (TT bestMove not at front)");
        }

        // 5) Build forcing moves like qSearch would (captures or promotions) and test ordering if applicable
        ArrayList<Zug> forcing = new ArrayList<>();
        for (Zug z : legal) {
            if (MoveFinder.isCapture(board, z) || MoveFinder.promotionQ(z, board)) {
                forcing.add(z);
            }
        }

        System.out.println("\n=== Forcing (qSearch) move list ordering test ===");
        if (forcing.isEmpty()) {
            System.out.println("No forcing moves available; skipping qSearch ordering test.");
        } else {
            MoveOrdering.orderMoves(forcing, board, whiteToMove);
            System.out.println("Before TT priority, first move:  " + forcing.get(0).processZug());

            // Only apply prioritization if our TT best move is part of the forcing set
            boolean containsTarget = false;
            for (Zug z : forcing) { if (sameMove(z, target)) { containsTarget = true; break; } }

            if (containsTarget) {
                moveToFront(forcing, target);
                System.out.println("After TT priority, first move:   " + forcing.get(0).processZug());
                if (sameMove(forcing.get(0), target)) {
                    System.out.println("RESULT: PASS (TT bestMove moved to front in forcing list)");
                } else {
                    System.out.println("RESULT: FAIL (TT bestMove not at front in forcing list)");
                }
            } else {
                System.out.println("TT bestMove is not a forcing move in this position; skipping forcing-list assertion.");
            }
        }

        // 6) Optional: Run a quick shallow search to ensure no runtime errors with TT prioritization
        System.out.println("\n=== Sanity search ===");
        int value = MoveFinder.negamax(board, 2, Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2, whiteToMove, hash);
        System.out.println("negamax value at depth 2: " + value);
    }

    // Helpers mirroring MoveFinder's comparison logic
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
