public class Test {
    static Piece [][] temp;
    private static long startTime = 0; // Startzeit für Messung
    public static void main(String [] args){
        temp = new Piece[8][8];
        Board.setupBoard(temp);
        test1();
    }
    public static void test1(){
        startTime = System.currentTimeMillis();
        for(int i = 0; i < 100000; i++){
            System.out.println(MoveFinder.possibleMoves(true, temp).getFirst().processZug());
        }
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Time elapsed: " + elapsed + " ms");
    }

}
