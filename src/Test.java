import java.util.ArrayList;

public class Test {
    static Piece [][] temp;

    public static void main(String [] args){
        temp = new Piece[8][8];
        temp = Board.fenToBoard("r1bq1rk1/ppp1bpp1/2n1p2p/3p4/2PPN2P/4P1B1/PP3PP1/R2QKBNR b KQ - 0 9");

        test1();
    }
    public static void test1(){
        // Startzeit für Messung
        long startTime = System.currentTimeMillis();

        ArrayList<Zug> pM = MoveFinder.possibleMoves(false, temp);
        pM.removeIf(zug -> !MoveFinder.isLegalMove(zug, temp, false));
        MoveOrdering.orderMoves(pM, temp, false);

        System.out.println(MoveFinder.findBestMoves(temp, 1, false, pM).getFirst().processZug());
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
