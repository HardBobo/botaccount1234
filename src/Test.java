import java.util.ArrayList;

public class Test {
    static Piece [][] temp;

    public static void main(String [] args){
        temp = new Piece[8][8];
        temp = Board.fenToBoard("r1b1k2r/pp3ppp/6n1/2b1p3/1PPqp3/6N1/P2N1PPP/R1BK1B1R b kq - 0 17");
        test1();
    }
    public static void test1(){
        // Startzeit für Messung
        long startTime = System.currentTimeMillis();

        ArrayList<Zug> pM = MoveFinder.possibleMoves(false, temp);
        MoveOrdering.orderMoves(pM, temp, false);

        System.out.println(MoveFinder.findBestMoves(temp, 4, false, pM).getFirst().processZug());

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Time elapsed: " + elapsed + " ms");
    }

}
