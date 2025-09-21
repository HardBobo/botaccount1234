public class EvaluationTest {
    public static void main(String[] args) {
        // Setup the same complex position
        Board.brett = Board.fenToBoard("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - ");
        
        System.out.println("=== Evaluation Performance Test with PieceTracker ===\n");
        
        // Test 1: King lookup (massive improvement expected)
        testKingLookup();
        
        // Test 2: Evaluation function (significant improvement expected)  
        testEvaluation();
        
        // Test 3: Pawn structure analysis (good improvement expected)
        testPawnAnalysis();
    }
    
    private static void testKingLookup() {
        System.out.println("1. King Lookup Performance:");
        
        int iterations = 1_000_000;
        
        // Old method simulation
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            kingLookupOld(true);
        }
        long oldTime = System.nanoTime() - start;
        
        // New method (PieceTracker)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Board.pieceTracker.getKing(true);
        }
        long newTime = System.nanoTime() - start;
        
        System.out.println("  Old method: " + oldTime / 1_000_000 + " ms");
        System.out.println("  New method: " + newTime / 1_000_000 + " ms");
        System.out.println("  Speedup: " + String.format("%.1f", (double)oldTime / newTime) + "x faster\n");
    }
    
    private static void testEvaluation() {
        System.out.println("2. Full Position Evaluation:");
        
        int iterations = 100_000;
        
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Evaluation.evaluation(Board.brett, true);
        }
        long evalTime = System.nanoTime() - start;
        
        System.out.println("  " + iterations + " evaluations completed in " + evalTime / 1_000_000 + " ms");
        System.out.println("  Average per evaluation: " + String.format("%.3f", evalTime / 1_000_000.0 / iterations) + " ms");
        System.out.println("  Evaluations per second: " + String.format("%.0f", iterations * 1000.0 / (evalTime / 1_000_000.0)) + "\n");
    }
    
    private static void testPawnAnalysis() {
        System.out.println("3. Pawn Structure Analysis:");
        
        int iterations = 500_000;
        
        // Old method (board scanning)
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            bauerCounterOld(Board.brett, true);
        }
        long oldTime = System.nanoTime() - start;
        
        // New method (PieceTracker)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Evaluation.bauerCounter(Board.brett, true);
        }
        long newTime = System.nanoTime() - start;
        
        System.out.println("  Old method: " + oldTime / 1_000_000 + " ms");
        System.out.println("  New method: " + newTime / 1_000_000 + " ms");
        System.out.println("  Speedup: " + String.format("%.1f", (double)oldTime / newTime) + "x faster\n");
        
        // Verify correctness
        int oldResult = bauerCounterOld(Board.brett, true);
        int newResult = Evaluation.bauerCounter(Board.brett, true);
        System.out.println("4. Correctness Check:");
        System.out.println("  Old result: " + oldResult + " pawns");
        System.out.println("  New result: " + newResult + " pawns");
        System.out.println("  Results match: " + (oldResult == newResult ? "✅" : "❌"));
    }
    
    // Simulate old methods for comparison
    private static Koordinaten kingLookupOld(boolean white) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (Board.brett[i][j] instanceof Koenig && Board.brett[i][j].isWhite() == white) {
                    return new Koordinaten(j, i);
                }
            }
        }
        return null;
    }
    
    private static int bauerCounterOld(Piece[][] brett, boolean isWhite) {
        int count = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece p = brett[x][y];
                if (p instanceof Bauer) {
                    count++;
                }
            }
        }
        return count;
    }
}