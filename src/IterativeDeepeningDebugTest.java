public class IterativeDeepeningDebugTest {
    
    public static void main(String[] args) {
        System.out.println("=== Iterative Deepening Debug Test ===\n");
        
        // Initialize zobrist hashing
        Zobrist.initZobrist();
        
        // Test 1: Starting position
        System.out.println("Test 1: Eröffnungsposition");
        System.out.println("=========================");
        
        Board.brett = new Piece[8][8];
        Board.setupBoard(Board.brett);
        long hash = Zobrist.computeHash(Board.brett, true);
        
        Zug bestMove = MoveFinder.iterativeDeepening(Board.brett, true, hash, 2000); // 2 seconds
        
        // Test 2: Complex middlegame position
        System.out.println("\nTest 2: Komplexe Mittelspielposition");
        System.out.println("====================================");
        
        Board.brett = Board.fenToBoard("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -");
        hash = Zobrist.computeHash(Board.brett, true);
        
        bestMove = MoveFinder.iterativeDeepening(Board.brett, true, hash, 1500); // 1.5 seconds
        
        // Test 3: Tactical position
        System.out.println("\nTest 3: Taktische Position");
        System.out.println("==========================");
        
        Board.brett = Board.fenToBoard("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4");
        hash = Zobrist.computeHash(Board.brett, true);
        
        bestMove = MoveFinder.iterativeDeepening(Board.brett, true, hash, 1000); // 1 second
        
        System.out.println("=== Test beendet ===");
    }
}