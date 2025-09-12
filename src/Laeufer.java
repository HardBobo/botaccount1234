import java.util.ArrayList;
import java.util.List;

public class Laeufer extends Piece {
    public Laeufer(boolean istWeiss) {
        super(2, istWeiss);
    }

    @Override
    public List<Zug> moeglicheZuege(int x, int y, Piece[][] board) {
        List<Zug> moegliche = new ArrayList<>();

        int[][] richtungen = {
                { -1, -1 }, // oben links
                { 1, -1 },  // oben rechts
                { -1, 1 },  // unten links
                { 1, 1 }    // unten rechts
        };

        Zug zug;

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