/**
 * Smoke test for the fixed-size transposition table.
 *
 * Runs a few short searches from the start position, plays the chosen move,
 * and repeats. This exercises:
 *  - TT allocation at configured size
 *  - generation aging via MoveFinder.iterativeDeepening() (tt.newSearch())
 *  - store/probe and best-move ordering from TT
 */
public final class TtSmokeTest {
    public static void main(String[] args) {
        // Ensure hashing is initialized
        Zobrist.initZobrist();

        // Start position
        Board.setupStartPosition();

        // Touch TT so we can print configured size/capacity
        int configuredMb = Config.getInstance().getTtSizeMB();
        System.out.println("TT configured MB: " + configuredMb);
        System.out.println("TT capacity entries: " + MoveFinder.tt.capacityEntries());

        long hash = Zobrist.computeHash(Board.bitboards, true);
        boolean whiteToMove = true;

        // Play a few plies with short time limits
        int plies = 12;
        long thinkMs = 80;

        for (int ply = 1; ply <= plies; ply++) {
            Zug best = MoveFinder.iterativeDeepening(whiteToMove, hash, thinkMs);
            if (best == null) {
                System.out.println("No move found at ply " + ply + " (game over?)");
                break;
            }

            System.out.println("ply " + ply + " " + (whiteToMove ? "w" : "b") + " plays " + best.processZug());

            MoveInfo info = MoveFinder.saveMoveInfo(best);
            hash = MoveFinder.doMoveUpdateHash(best, info, hash);

            whiteToMove = !whiteToMove;
            Board.whiteToMove = whiteToMove;
        }

        System.out.println("Smoke test finished.");
    }
}
