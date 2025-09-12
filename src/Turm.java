import java.util.ArrayList;
import java.util.List;

public class Turm extends Piece {
    private boolean kannRochieren;
    public Turm(boolean istWeiss) {
        super(3, istWeiss);
        this.kannRochieren = true;
    }
    public boolean kannRochieren() {
        return kannRochieren;
    }
    public void setKannRochieren(boolean b) {
        this.kannRochieren = b;
    }

    @Override
    public List<Zug> moeglicheZuege(int x, int y, Piece[][] board) {
        List<Zug> moegliche = new ArrayList<>();

        int[][] richtungen = {
                { 0, -1 }, // oben
                { 0,  1 }, // unten
                { -1, 0 }, // links
                { 1,  0 }  // rechts
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