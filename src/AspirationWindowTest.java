import java.util.ArrayList;

public class AspirationWindowTest {
    
    private static int nodeCount = 0;
    private static long startTime;
    
    public static void main(String[] args) {
        System.out.println("=== Aspiration Window Test Suite ===\n");
        
        // Initialize zobrist hashing
        Zobrist.initZobrist();
        
        // Test 1: Basic functionality test
        testBasicFunctionality();
        
        // Test 2: Performance comparison
        testPerformanceComparison();
        
        // Test 3: Fail-high/fail-low scenarios
        testFailHighFailLow();
        
        // Test 4: Different position types
        testDifferentPositions();
        
        System.out.println("\n=== All Aspiration Window Tests Complete ===");
    }
    
    private static void testBasicFunctionality() {
        System.out.println("Test 1: Basic Functionality");
        System.out.println("-----------------------------");
        
        // Set up a standard starting position
        Board.brett = new Piece[8][8];
        Board.setupBoard(Board.brett);
        
        long hash = Zobrist.computeHash(Board.brett, true);
        ArrayList<Zug> moves = MoveFinder.possibleMoves(true, Board.brett);
        MoveOrdering.orderMoves(moves, Board.brett, true);
        
        // Test with full window
        MoveFinder.SearchResult fullResult = MoveFinder.findBestMovesWithAspirationWindow(
            Board.brett, 3, true, moves, hash, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
        
        System.out.println("Full window search:");
        System.out.println("  Best move: " + (fullResult.moves.isEmpty() ? "none" : fullResult.moves.getFirst().processZug()));
        System.out.println("  Best score: " + fullResult.bestScore);
        System.out.println("  Has score: " + fullResult.hasScore);
        
        if (fullResult.hasScore) {
            // Test with aspiration window around the score
            int windowSize = 50;
            MoveFinder.SearchResult aspirationResult = MoveFinder.findBestMovesWithAspirationWindow(
                Board.brett, 3, true, moves, hash, 
                fullResult.bestScore - windowSize, fullResult.bestScore + windowSize);
            
            System.out.println("\nAspiration window search (±" + windowSize + "):");
            System.out.println("  Best move: " + (aspirationResult.moves.isEmpty() ? "none" : aspirationResult.moves.getFirst().processZug()));
            System.out.println("  Best score: " + aspirationResult.bestScore);
            System.out.println("  Has score: " + aspirationResult.hasScore);
            
            // Check if results are consistent
            boolean consistent = fullResult.moves.getFirst().processZug().equals(aspirationResult.moves.getFirst().processZug());
            System.out.println("  Results consistent: " + consistent);
            if (!consistent) {
                System.out.println("  WARNING: Aspiration window gave different result!");
            }
        }
        
        System.out.println();
    }
    
    private static void testPerformanceComparison() {
        System.out.println("Test 2: Performance Comparison");
        System.out.println("-------------------------------");
        
        // Use a more complex position for better testing
        Board.brett = Board.fenToBoard("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
        long hash = Zobrist.computeHash(Board.brett, true);
        
        ArrayList<Zug> moves = MoveFinder.possibleMoves(true, Board.brett);
        MoveOrdering.orderMoves(moves, Board.brett, true);
        
        int depth = 4;
        int trials = 3;
        
        // Test full window search multiple times
        long fullWindowTime = 0;
        MoveFinder.SearchResult baseResult = null;
        
        for (int i = 0; i < trials; i++) {
            MoveFinder.transpositionTable.clear(); // Clear TT for fair comparison
            long start = System.currentTimeMillis();
            baseResult = MoveFinder.findBestMovesWithAspirationWindow(
                Board.brett, depth, true, new ArrayList<>(moves), hash, 
                Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
            long end = System.currentTimeMillis();
            fullWindowTime += (end - start);
        }
        fullWindowTime /= trials;
        
        System.out.println("Full window (depth " + depth + "):");
        System.out.println("  Average time: " + fullWindowTime + "ms");
        System.out.println("  Best score: " + baseResult.bestScore);
        
        if (baseResult.hasScore) {
            // Test aspiration window search
            long aspirationTime = 0;
            int windowSize = 50;
            
            for (int i = 0; i < trials; i++) {
                MoveFinder.transpositionTable.clear(); // Clear TT for fair comparison
                long start = System.currentTimeMillis();
                MoveFinder.SearchResult aspirationResult = MoveFinder.findBestMovesWithAspirationWindow(
                    Board.brett, depth, true, new ArrayList<>(moves), hash,
                    baseResult.bestScore - windowSize, baseResult.bestScore + windowSize);
                long end = System.currentTimeMillis();
                aspirationTime += (end - start);
            }
            aspirationTime /= trials;
            
            System.out.println("\nAspiration window (±" + windowSize + "):");
            System.out.println("  Average time: " + aspirationTime + "ms");
            
            double improvement = ((double)(fullWindowTime - aspirationTime) / fullWindowTime) * 100;
            System.out.println("  Performance improvement: " + String.format("%.1f", improvement) + "%");
            
            if (improvement > 0) {
                System.out.println("  ✓ Aspiration windows are faster!");
            } else if (improvement < -10) {
                System.out.println("  ⚠ Aspiration windows are significantly slower - possible fail-high/low?");
            } else {
                System.out.println("  → Similar performance (expected for narrow windows)");
            }
        }
        
        System.out.println();
    }
    
    private static void testFailHighFailLow() {
        System.out.println("Test 3: Fail-High/Fail-Low Scenarios");
        System.out.println("------------------------------------");
        
        // Use a tactical position where there are clear good/bad moves
        Board.brett = Board.fenToBoard("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4");
        long hash = Zobrist.computeHash(Board.brett, true);
        
        ArrayList<Zug> moves = MoveFinder.possibleMoves(true, Board.brett);
        if (moves.isEmpty()) {
            System.out.println("No legal moves available in test position\n");
            return;
        }
        
        MoveOrdering.orderMoves(moves, Board.brett, true);
        
        // First get the actual score with full window
        MoveFinder.SearchResult actualResult = MoveFinder.findBestMovesWithAspirationWindow(
            Board.brett, 3, true, new ArrayList<>(moves), hash, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
        
        if (actualResult.hasScore) {
            System.out.println("Actual best score: " + actualResult.bestScore);
            System.out.println("Actual best move: " + actualResult.moves.getFirst().processZug());
            
            // Test with narrow window around the score (should work normally)
            int windowSize = 25;
            MoveFinder.SearchResult normalTest = MoveFinder.findBestMovesWithAspirationWindow(
                Board.brett, 3, true, new ArrayList<>(moves), hash,
                actualResult.bestScore - windowSize, actualResult.bestScore + windowSize);
            
            System.out.println("\nNormal aspiration window (±" + windowSize + "):");
            System.out.println("  Result score: " + normalTest.bestScore);
            System.out.println("  Best move: " + normalTest.moves.getFirst().processZug());
            System.out.println("  Consistent result: " + normalTest.moves.getFirst().processZug().equals(actualResult.moves.getFirst().processZug()));
            
            // Test with window that might be too low (potential fail high)
            int lowBeta = actualResult.bestScore - 50;
            if (lowBeta > Integer.MIN_VALUE + 1) {
                MoveFinder.SearchResult failHighTest = MoveFinder.findBestMovesWithAspirationWindow(
                    Board.brett, 3, true, new ArrayList<>(moves), hash, 
                    Integer.MIN_VALUE + 1, lowBeta);
                
                System.out.println("\nPotential fail-high test (beta=" + lowBeta + "):");
                System.out.println("  Result score: " + failHighTest.bestScore);
                if (failHighTest.bestScore >= lowBeta) {
                    System.out.println("  ✓ Failed high as expected (score >= beta)");
                } else {
                    System.out.println("  → Score within window");
                }
            }
            
            // Test with window that might be too high (potential fail low)
            int highAlpha = actualResult.bestScore + 50;
            if (highAlpha < Integer.MAX_VALUE - 1) {
                MoveFinder.SearchResult failLowTest = MoveFinder.findBestMovesWithAspirationWindow(
                    Board.brett, 3, true, new ArrayList<>(moves), hash,
                    highAlpha, Integer.MAX_VALUE - 1);
                
                System.out.println("\nPotential fail-low test (alpha=" + highAlpha + "):");
                System.out.println("  Result score: " + failLowTest.bestScore);
                if (failLowTest.bestScore <= highAlpha) {
                    System.out.println("  ✓ Failed low as expected (score <= alpha)");
                } else {
                    System.out.println("  → Score above window");
                }
            }
        } else {
            System.out.println("Could not get a valid score from the test position");
        }
        
        System.out.println();
    }
    
    private static void testDifferentPositions() {
        System.out.println("Test 4: Different Position Types");
        System.out.println("--------------------------------");
        
        String[] testPositions = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // Starting position
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -", // Complex middle game
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -", // Endgame
            "rnbqkb1r/pppp1ppp/5n2/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR w KQkq e6 0 3" // Opening
        };
        
        String[] positionNames = {"Opening", "Complex Middlegame", "Endgame", "Early Opening"};
        
        for (int i = 0; i < testPositions.length; i++) {
            System.out.println(positionNames[i] + ":");
            
            Board.brett = Board.fenToBoard(testPositions[i]);
            long hash = Zobrist.computeHash(Board.brett, true);
            
            ArrayList<Zug> moves = MoveFinder.possibleMoves(true, Board.brett);
            if (moves.isEmpty()) {
                System.out.println("  No legal moves available\n");
                continue;
            }
            
            MoveOrdering.orderMoves(moves, Board.brett, true);
            
            MoveFinder.SearchResult result = MoveFinder.findBestMovesWithAspirationWindow(
                Board.brett, 3, true, moves, hash, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1);
            
            System.out.println("  Best move: " + (result.moves.isEmpty() ? "none" : result.moves.getFirst().processZug()));
            System.out.println("  Score: " + (result.hasScore ? result.bestScore : "N/A"));
            System.out.println("  Legal moves: " + moves.size());
            System.out.println();
        }
    }
}