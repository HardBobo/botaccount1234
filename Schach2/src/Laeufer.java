import java.util.ArrayList;
import java.util.List;

public class Laeufer extends Piece {
    public Laeufer(boolean istWeiss) {
        super("Läufer", istWeiss, 310);
    }
    public boolean istZugMoeglich(int startX, int startY, int zielX, int zielY, Piece[][] board) {
        int dx = zielX - startX;
        int dy = zielY - startY;

        // diagonal
        if (Math.abs(dx) != Math.abs(dy)) {
            return false;
        }

        int stepX = Integer.compare(dx, 0); // 1 oder -1
        int stepY = Integer.compare(dy, 0); // 1 oder -1

        int x = startX + stepX;
        int y = startY + stepY;

        while (x != zielX || y != zielY) {
            if (!board[y][x].getType().equals("empty")) {
                return false; // was im weg
            }
            x += stepX;
            y += stepY;
        }
        return true;
    }
    @Override
    public List<Koordinaten> bedrohteFelder(int x, int y, Piece[][] board) {
        List<Koordinaten> bedrohte = new ArrayList<>();
        int[][] richtungen = {
                { -1, -1 }, // oben links
                { 1, -1 },  // oben rechts
                { -1, 1 },  // unten links
                { 1, 1 }    // unten rechts
        };

        for (int[] r : richtungen) {
            int dx = r[0];
            int dy = r[1];
            int cx = x + dx;
            int cy = y + dy;

            while (Spiel.imBrett(cx, cy)) {
                bedrohte.add(new Koordinaten(cx, cy));

                if (!board[cy][cx].getType().equals("empty")) {
                    //wenn figur im weg
                    break;
                }

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