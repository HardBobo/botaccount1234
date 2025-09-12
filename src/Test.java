import java.util.ArrayList;

public class Test {
    static Piece [][] temp;

    public static void main(String [] args){
        temp = new Piece[8][8];
        temp = Board.fenToBoard("r2qkb1r/pp2nppp/3p4/2pNN1B1/2BnP3/3P4/PPP2PPP/R2bK2R w KQkq - 1 0\n");
        test1();
    }
    public static void test1(){
        // Startzeit für Messung
        long startTime = System.currentTimeMillis();

        ArrayList<Zug> pM = MoveFinder.possibleMoves(true, temp);
        MoveOrdering.orderMoves(pM, temp, true);

        System.out.println(MoveFinder.findBestMoves(temp, 5, true, pM).getFirst().processZug());

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Time elapsed: " + elapsed + " ms");
    }

}
