import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Koenig extends Piece {
    private boolean kannRochieren;

    public Koenig(boolean istWeiss) {
        super("König", istWeiss, 0);
        this.kannRochieren = true;
    }

    public boolean kannRochieren() {
        return kannRochieren;
    }

    public void setKannRochieren(boolean b) {
        this.kannRochieren = b;
    }

    @Override
    public List<Koordinaten> bedrohteFelder(int x, int y, Piece[][] board) {
        List<Koordinaten> bedrohte = new ArrayList<>();

        // 8 mögliche Richtungen
        int[][] richtungen = {
                { -1, -1 }, { 0, -1 }, { 1, -1 },
                { -1, 0 },            { 1, 0 },
                { -1, 1 },  { 0, 1 },  { 1, 1 }
        };

        for (int[] r : richtungen) {
            int zielX = x + r[0];
            int zielY = y + r[1];

            if (Spiel.imBrett(zielX, zielY)) {
                bedrohte.add(new Koordinaten(zielX, zielY));
            }
        }

        return bedrohte;
    }

    @Override
    public List<Zug> moeglicheZuege(int x, int y, Piece[][] board) {
        List<Zug> moegliche = new ArrayList<>();

        int[][] richtungen = {
                { -1, -1 }, { 0, -1 }, { 1, -1 },
                { -1, 0 },            { 1, 0 },
                { -1, 1 },  { 0, 1 },  { 1, 1 }
        };

        Zug zug;

        boolean isWhite = this.isWhite();

        for (int[] r : richtungen) {
            int zielX = x + r[0];
            int zielY = y + r[1];
            if(Spiel.imBrett(zielX, zielY)) {

                zug = new Zug(x, y, zielX, zielY);

                if (Spiel.isPseudoLegal(zug, isWhite, board)) {
                    moegliche.add(zug);
                }
            }
        }
        if (isWhite) {
            if (board[7][4] instanceof Koenig k && k.isWhite() && k.kannRochieren()) {
                if(board[7][5] instanceof Empty
                        && board[7][6] instanceof Empty
                        && board[7][7] instanceof Turm t
                        && t.kannRochieren()) {
                    zug = new Zug("e1g1");
                    moegliche.add(zug);
                }
                if(board[7][3] instanceof Empty
                        && board[7][2] instanceof Empty
                        && board[7][1] instanceof Empty
                        && board[7][0] instanceof Turm t
                        && t.kannRochieren()){
                    zug = new Zug("e1c1");
                    moegliche.add(zug);
                }
            }
        } else {
            if (board[0][4] instanceof Koenig k && !k.isWhite() && k.kannRochieren()) {
                if(board[0][5] instanceof Empty
                        && board[0][6] instanceof Empty
                        && board[0][7] instanceof Turm t
                        && t.kannRochieren()) {
                    zug = new Zug("e8g8");
                    moegliche.add(zug);
                }
                if(board[0][3] instanceof Empty
                        && board[0][2] instanceof Empty
                        && board[0][1] instanceof Empty
                        && board[0][0] instanceof Turm t
                        && t.kannRochieren()){
                    zug = new Zug("e8c8");
                    moegliche.add(zug);
                }
            }
        }
        return moegliche;
    }
}