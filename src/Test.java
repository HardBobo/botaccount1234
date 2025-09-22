import java.util.ArrayList;

public class Test {
    static Piece [][] temp;
    private static long startHash;

    public static void main(String [] args){
        temp = new Piece[8][8];
        temp = Board.fenToBoard("8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - -");
        Zobrist.initZobrist();
        startHash = Zobrist.computeHash(temp, true);
        test1();
    }
    public static void test1(){
//        MoveInfo info = MoveFinder.saveMoveInfo(new Zug("e7b4"), temp);
//        MoveFinder.doMove(new Zug("e7b4"), temp, info);
        System.out.println("Evaluation: " + Evaluation.evaluation(temp, true));


        // Startzeit für Messung
//        long startTime = System.currentTimeMillis();

        boolean whiteToMove = true;

        ArrayList<Zug> pM = MoveFinder.possibleMoves(whiteToMove, temp);
        pM.removeIf(zug -> !MoveFinder.isLegalMove(zug, temp, whiteToMove));
        MoveOrdering.orderMoves(pM, temp, whiteToMove);

        System.out.println(MoveFinder.findBestMoves(temp, 24, whiteToMove, pM, startHash).getFirst().processZug());
//        MoveInfo info = MoveFinder.saveMoveInfo(new Zug("d4c3"), temp);
//        MoveFinder.doMove(new Zug("d4c3"), temp, info);
//
//        System.out.println("Evaluation nach zug: " + Evaluation.evaluation(temp, true));
//
//        ArrayList<Zug> pM2 = MoveFinder.possibleMoves(true, temp);
//        pM.removeIf(zug -> !MoveFinder.isLegalMove(zug, temp, true));
//        MoveOrdering.orderMoves(pM2, temp, true);
//
//        System.out.println(MoveFinder.findBestMoves(temp, 1, true, pM2).getFirst().processZug());
//
//        long elapsed = System.currentTimeMillis() - startTime;
//        System.out.println("Time elapsed: " + elapsed + " ms");
    }

//    public static void test2(){
//        // Startzeit für Messung
//        long startTime = System.currentTimeMillis();
//
//        ArrayList<Zug> pM = MoveFinder.possibleMoves(false, temp);
//        MoveOrdering.orderMoves(pM, temp, false);
//
//        System.out.println(MoveFinder.findBestMoves(temp, 4, false, pM).getFirst().processZug());
//
//        long elapsed = System.currentTimeMillis() - startTime;
//        System.out.println("Time elapsed: " + elapsed + " ms");
//    }

    public static void debug(){
        System.out.println(Zobrist.sideToMoveKey);
        for(int i = 0; i < 8; i++){
            System.out.println(Zobrist.enPassantKeys[i]);
        }
        for(int i = 0; i < 4; i++){
            System.out.println(Zobrist.castleKeys[i]);
        }
        for(int i = 0; i < 12; i++){
            for(int j = 0; j < 64; j++){
                System.out.println(Zobrist.pieceSquareKeys[i][j]);
            }
        }
    }

    public static void debugComputeHash() {
        Board.setupBoard(Board.brett);

        // Test 1: Startposition
        long hash1 = Zobrist.computeHash(Board.brett, true);
        long hash2 = Zobrist.computeHash(Board.brett, true);
        System.out.println("Startposition consistent: " + (hash1 == hash2));
        System.out.println("Hash1: " + hash1);

        // Test 2: Seite wechseln
        long hashWhite = Zobrist.computeHash(Board.brett, true);
        long hashBlack = Zobrist.computeHash(Board.brett, false);
        System.out.println("Different sides give different hash: " + (hashWhite != hashBlack));

        // Test 3: Einzelne Komponenten
        System.out.println("=== Hash Components ===");

        // Nur Figuren
        long figureHash = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = Board.brett[y][x];
                if (!(p instanceof Empty)) {
                    int pieceIndex = Zobrist.getPieceIndex(p);
                    int squareIndex = y * 8 + x;
                    figureHash ^= Zobrist.getPieceSquareKey(pieceIndex, squareIndex);
                    System.out.println("Piece at " + x + "," + y + ": " + p + " -> index " + pieceIndex);
                }
            }
        }

        for(int k = 0; k < 4; k++){
            boolean [] rights = MoveFinder.getCastleRights(Board.brett);
            if (rights[k]) {
                figureHash ^= Zobrist.getCastleKey(k);
            }
        }
        figureHash ^= Zobrist.getSideToMoveKey();

        System.out.println("Figure hash: " + figureHash);

        // Castle rights
        boolean[] rights = MoveFinder.getCastleRights(Board.brett);
        System.out.println("Castle rights: " + java.util.Arrays.toString(rights));

        // En passant
        int epFile = Zobrist.getEnPassantFile(Board.brett, true);
        System.out.println("En passant file: " + epFile);
    }

}
