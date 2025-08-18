import java.util.ArrayList;
import java.util.List;

public class Springer extends Piece {
    public Springer(boolean istWeiss) {
        super("Springer", istWeiss, 300);
    }
    public boolean istZugMoeglich(int startX, int startY, int zielX, int zielY, Piece[][] board) {
        int dx = Math.abs(zielX - startX);
        int dy = Math.abs(zielY - startY);

        return (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
    }
    @Override
    public List<Koordinaten> bedrohteFelder(int x, int y, Piece[][] board) {
        List<Koordinaten> bedrohte = new ArrayList<>();

        int[][] zuege = {
                { 1, 2 }, { 2, 1 }, { -1, 2 }, { -2, 1 },
                { 1, -2 }, { 2, -1 }, { -1, -2 }, { -2, -1 }
        };

        for (int[] zug : zuege) {
            int zielX = x + zug[0];
            int zielY = y + zug[1];

            if (Spiel.imBrett(zielX, zielY)) {
                bedrohte.add(new Koordinaten(zielX, zielY));
            }
        }

        return bedrohte;
    }

    @Override
    public List<Zug> moeglicheZuege(int x, int y, Piece[][] board) {
        List<Zug> moegliche = new ArrayList<>();

        int[][] zuege = {
                { 1, 2 }, { 2, 1 }, { -1, 2 }, { -2, 1 },
                { 1, -2 }, { 2, -1 }, { -1, -2 }, { -2, -1 }
        };

        Zug zug;

        for (int[] zug1 : zuege) {
            zug = new Zug(x, y, x + zug1[0], y + zug1[1]);

            if (Spiel.isValid(zug, this.isWhite(), board)) {
                moegliche.add(zug);
            }
        }

        return moegliche;
    }
}