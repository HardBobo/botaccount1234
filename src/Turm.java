import java.util.ArrayList;
import java.util.List;

public class Turm extends Piece {
    private boolean kannRochieren;
    public Turm(boolean istWeiss) {
        super("Turm", istWeiss, 500);
        this.kannRochieren = true;
    }
    public boolean kannRochieren() {
        return kannRochieren;
    }
    public void setKannRochieren(boolean b) {
        this.kannRochieren = b;
    }
    public boolean istZugMoeglich(int startX, int startY, int zielX, int zielY, Piece[][] board) {
        // nur eine richtung
        if (startX != zielX && startY != zielY) {
            //System.out.println("Turmzug unmöglich");
            return false;
        }

        int dx = Integer.compare(zielX, startX); // Richtung in X (0, 1 oder -1)
        int dy = Integer.compare(zielY, startY); // Richtung in Y (0, 1 oder -1)

        int x = startX + dx;
        int y = startY + dy;

        //
        while (x != zielX || y != zielY) {
            if (!(board[y][x] instanceof Empty)) {
                //System.out.println("Turmzug unmöglich");
                return false; //was im weg
            }
            x += dx;
            y += dy;
        }
        return true;
    }

    @Override
    public List<Koordinaten> bedrohteFelder(int x, int y, Piece[][] board) {
        List<Koordinaten> bedrohte = new ArrayList<>();

        int[][] richtungen = {
                { 0, -1 }, // oben
                { 0,  1 }, // unten
                { -1, 0 }, // links
                { 1,  0 }  // rechts
        };

        for (int[] r : richtungen) {
            int dx = r[0];
            int dy = r[1];
            int cx = x + dx;
            int cy = y + dy;

            while (Spiel.imBrett(cx, cy)) {
                bedrohte.add(new Koordinaten(cx, cy));

                if (!(board[cy][cx] instanceof Empty)) {
                    break;
                }//falls geblockt

                cx += dx;
                cy += dy;
            }
        }
        return bedrohte;
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

                if(Spiel.isValid(zug, this.isWhite(), board))
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