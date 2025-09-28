public class Spiel {

    public static void newGame(){
        Board.setupStartPosition();
        MoveFinder.transpositionTable.clear();
        Zobrist.initZobrist();
        LichessBotStream.startHash = Zobrist.computeHash(Board.bitboards, true);
    }
}
