/**
 * Test class to verify that PieceTracker is working correctly and efficiently
 */
public class PieceTrackerTest {
    
    public static void main(String[] args) {
        System.out.println("Testing PieceTracker functionality...\n");
        
        // Initialize a new game board
        Board.setupBoard(Board.brett);
        
        // Test initial piece counts
        testInitialPieceCounts();
        
        // Test move generation performance
        testMoveGenerationPerformance();
        
        // Test evaluation performance  
        testEvaluationPerformance();
        
        // Test king lookup
        testKingLookup();
        
        System.out.println("All PieceTracker tests completed successfully!");
    }
    
    private static void testInitialPieceCounts() {
        System.out.println("=== Testing Initial Piece Counts ===");
        PieceTracker tracker = Board.pieceTracker;
        
        // Should have 8 pawns each
        System.out.println("White pawns: " + tracker.getPawns(true).size() + " (expected: 8)");
        System.out.println("Black pawns: " + tracker.getPawns(false).size() + " (expected: 8)");
        
        // Should have 2 rooks each
        System.out.println("White rooks: " + tracker.getRooks(true).size() + " (expected: 2)");
        System.out.println("Black rooks: " + tracker.getRooks(false).size() + " (expected: 2)");
        
        // Should have 2 knights each
        System.out.println("White knights: " + tracker.getKnights(true).size() + " (expected: 2)");
        System.out.println("Black knights: " + tracker.getKnights(false).size() + " (expected: 2)");
        
        // Should have 2 bishops each
        System.out.println("White bishops: " + tracker.getBishops(true).size() + " (expected: 2)");
        System.out.println("Black bishops: " + tracker.getBishops(false).size() + " (expected: 2)");
        
        // Should have 1 queen each
        System.out.println("White queens: " + tracker.getQueens(true).size() + " (expected: 1)");
        System.out.println("Black queens: " + tracker.getQueens(false).size() + " (expected: 1)");
        
        // Should have 16 pieces total each
        System.out.println("Total white pieces: " + tracker.getAllPieces(true).size() + " (expected: 16)");
        System.out.println("Total black pieces: " + tracker.getAllPieces(false).size() + " (expected: 16)");
        
        System.out.println();
    }
    
    private static void testMoveGenerationPerformance() {
        System.out.println("=== Testing Move Generation Performance ===");
        
        // Test old method (direct board scanning)
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            possibleMovesOldMethod(true, Board.brett);
        }
        long oldMethodTime = System.nanoTime() - startTime;
        
        // Test new method (using PieceTracker)
        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            MoveFinder.possibleMoves(true, Board.brett);
        }
        long newMethodTime = System.nanoTime() - startTime;
        
        System.out.println("Old method (1000 iterations): " + oldMethodTime / 1_000_000 + " ms");
        System.out.println("New method (1000 iterations): " + newMethodTime / 1_000_000 + " ms");
        System.out.println("Performance improvement: " + String.format("%.2f", (double)oldMethodTime / newMethodTime) + "x faster");
        System.out.println();
    }
    
    private static void testEvaluationPerformance() {
        System.out.println("=== Testing Evaluation Performance ===");
        
        // Test new evaluation method
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Evaluation.evaluation(Board.brett, true);
        }
        long newMethodTime = System.nanoTime() - startTime;
        
        System.out.println("New evaluation method (1000 iterations): " + newMethodTime / 1_000_000 + " ms");
        System.out.println();
    }
    
    private static void testKingLookup() {
        System.out.println("=== Testing King Lookup ===");
        
        Koordinaten whiteKing = Board.pieceTracker.getKing(true);
        Koordinaten blackKing = Board.pieceTracker.getKing(false);
        
        System.out.println("White king position: (" + whiteKing.x + ", " + whiteKing.y + ") (expected: (4, 7))");
        System.out.println("Black king position: (" + blackKing.x + ", " + blackKing.y + ") (expected: (4, 0))");
        System.out.println();
    }
    
    // Old method for comparison
    private static java.util.ArrayList<Zug> possibleMovesOldMethod(boolean white, Piece[][] board) {
        java.util.ArrayList<Zug> pM = new java.util.ArrayList<>();
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
}