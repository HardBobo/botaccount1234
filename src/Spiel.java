public class Spiel {

    public static void newGame(){
        Board.setupStartPosition();
        MoveFinder.tt.clear();
        Zobrist.initZobrist();
        LichessBotStream.startHash = Zobrist.computeHash(Board.bitboards, true);
    }
}
