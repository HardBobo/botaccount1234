import java.util.ArrayList;
import java.util.List;

public class Springer extends Piece {
    public Springer(boolean istWeiss) {
        super(1, istWeiss);
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
            if(!Spiel.imBrett(x + zug1[0], y + zug1[1]))
                continue;
            zug = new Zug(x, y, x + zug1[0], y + zug1[1]);

            if (Spiel.isPseudoLegal(zug, this.isWhite(), board)) {
                moegliche.add(zug);
            }
        }

        return moegliche;
    }
}