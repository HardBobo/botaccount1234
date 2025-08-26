import java.util.ArrayList;

public class Test {
    static Piece [][] temp;

    public static void main(String [] args){
        temp = new Piece[8][8];
        temp = Board.fenToBoard("r2k4/1p2R3/p2p2b1/2B4p/1P5P/1KP4B/Pn6/5R2 b - - 0 41");
        test1();
    }
    public static void test1(){
        // Startzeit für Messung
        long startTime = System.currentTimeMillis();

        ArrayList<Zug> pM = MoveFinder.possibleMoves(false, temp);
        MoveOrdering.orderMoves(pM, temp, false);

        System.out.println(MoveFinder.findBestMoves(temp, 3, false, pM).getFirst().processZug());

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Time elapsed: " + elapsed + " ms");
    }

}
