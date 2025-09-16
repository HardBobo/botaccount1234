import java.util.ArrayList;

public class Test {
    static Piece [][] temp;

    public static void main(String [] args){
        temp = new Piece[8][8];
        temp = Board.fenToBoard("r1bq1rk1/p3bpp1/2p2p1p/3p4/Q2P4/2N1PN2/PP3PPP/R3K2R b KQ - 1 11");

        test1();
    }
    public static void test1(){
//        MoveInfo info = MoveFinder.saveMoveInfo(new Zug("e7b4"), temp);
//        MoveFinder.doMove(new Zug("e7b4"), temp, info);
        System.out.println("Evaluation: " + Evaluation.evaluation(temp, true));


        // Startzeit für Messung
//        long startTime = System.currentTimeMillis();

        boolean whiteToMove = false;

        ArrayList<Zug> pM = MoveFinder.possibleMoves(whiteToMove, temp);
        pM.removeIf(zug -> !MoveFinder.isLegalMove(zug, temp, whiteToMove));
        MoveOrdering.orderMoves(pM, temp, whiteToMove);

        System.out.println(MoveFinder.findBestMoves(temp, 4, whiteToMove, pM).getFirst().processZug());
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

}
