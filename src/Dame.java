import java.util.ArrayList;
import java.util.List;

public class Dame extends Piece {
    public Dame(boolean istWeiss) {
        super("Dame", istWeiss, 900);
    }
    public boolean istZugMoeglich(int startX, int startY, int zielX, int zielY, Piece[][] board) {
        //läufer + turm
        int dx = zielX - startX;
        int dy = zielY - startY;

        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);

        if (absDx == absDy && absDx != 0) {
            int stepX = Integer.compare(dx, 0);
            int stepY = Integer.compare(dy, 0);

            int x = startX + stepX;
            int y = startY + stepY;

            while (x != zielX || y != zielY) {
                if (!board[y][x].getType().equals("empty")) return false;
                x += stepX;
                y += stepY;
            }
            return true;
        }

        if ((dx == 0 && dy != 0) || (dy == 0 && dx != 0)) {
            int stepX = Integer.compare(dx, 0);
            int stepY = Integer.compare(dy, 0);

            int x = startX + stepX;
            int y = startY + stepY;

            while (x != zielX || y != zielY) {
                if (!board[y][x].getType().equals("empty")) return false;
                x += stepX;
                y += stepY;
            }
            return true;
        }

        return false;
    }
    @Override
    public List<Koordinaten> bedrohteFelder(int x, int y, Piece[][] board) {
        List<Koordinaten> bedrohte = new ArrayList<>();

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
                bedrohte.add(new Koordinaten(cx, cy));

                if (!(board[cy][cx] instanceof Empty)) {
                    // wenn im weg
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