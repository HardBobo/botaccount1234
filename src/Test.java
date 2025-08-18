public class Test {
    static Piece [][] temp;
    public static void main(String [] args){
        temp = new Piece[8][8];
        test1();
    }
    public static void test1(){
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                temp[i][j] = new Empty();
            }
        }
        temp[2][5] = new Dame(true);
        temp[2][4] = new Laeufer(true);
        temp[0][0] = new Koenig(true);
        temp[1][5] = new Bauer(true);
        temp[1][7] = new Koenig(false);
    }

}
