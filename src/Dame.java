import java.util.ArrayList;
import java.util.List;

public class Dame extends Piece {
    public Dame(boolean istWeiss) {
        super(4, istWeiss);
    }


    @Override
    public List<Zug> moeglicheZuege(int x, int y, Piece[][] board) {
        List<Zug> moegliche = new ArrayList<>();

        Zug zug;

        int[][] richtungen = {
                { 0, -1 }, { 0, 1 },  // vertikal
                { -1, 0 }, { 1, 0 },  // horizontal
                { -1, -1 }, { 1, -1 },  // diagonal oben
                { -1, 1 }, { 1, 1 }     // diagonal unten
        };

        for (int[] r : richtungen) {
            int dx = r[0];
            int dy = r[1];
            int cx = x + dx;
            int cy = y + dy;

            while (Spiel.imBrett(cx, cy)) {

                zug = new Zug(x, y, cx, cy);

                if(Spiel.isPseudoLegal(zug, this.isWhite(), board))
                    moegliche.add(zug);

                if (!(board[cy][cx] instanceof Empty)) {
                    //wenn figur im weg
                    break;
                }

                cx += dx;
                cy += dy;
            }
        }

        return moegliche;
    }
}