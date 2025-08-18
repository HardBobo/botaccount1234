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

    public boolean istZugMoeglich(int startX, int startY, int zielX, int zielY, Piece[][] board) {
        int dx = zielX - startX;
        int dy = zielY - startY;

        //normal
        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
            return true;
        }

        //kann er noch rochieren
        Piece king = board[startY][startX];
        if (!(king instanceof Koenig) || !((Koenig) king).kannRochieren()) {
            return false;
        }

        //horizontal 2 links oder rechts
        if (dy != 0 || (dx != 2 && dx != -2)) {
            return false;
        }

        //wohin und welcher turm
        int richtung = dx > 0 ? 1 : -1;
        int turmX = dx > 0 ? 7 : 0;

        Piece rook = board[startY][turmX];
        if (!(rook instanceof Turm) || !((Turm) rook).kannRochieren()) {
            return false;
        }

        //ist was dazwischen
        for (int x = startX + richtung; x != turmX; x += richtung) {
            if (!(board[startY][x] instanceof Empty)) {
                return false;
            }
        }
        Set<Koordinaten> alleBedrohten = Spiel.allSeenSquares(!king.isWhite(), board);
        //koenig bedroht
        if (alleBedrohten.contains(new Koordinaten(startX, startY)))
            return false;
        //feld dazwischen bedroht
        if(alleBedrohten.contains(new Koordinaten(startX + richtung, startY))){
            return false;
        }
        //zielfeld bedroht
        if(alleBedrohten.contains(new Koordinaten(startX + (richtung*2), startY))){
            return false;
        }
        return true;
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

        for (int[] r : richtungen) {
            int zielX = x + r[0];
            int zielY = y + r[1];

            zug = new Zug(x, y, zielX, zielY);

            if (Spiel.isValid(zug, this.isWhite(), board)) {
                moegliche.add(zug);
            }
        }

        if(this.isWhite()){
            zug = new Zug("e1g1");
            if (Spiel.isValid(zug, this.isWhite(), board)) {
                moegliche.add(zug);
            }
            zug = new Zug("e1c1");
            if (Spiel.isValid(zug, this.isWhite(), board)) {
                moegliche.add(zug);
            }
        } else {
            zug = new Zug("e8g8");
            if (Spiel.isValid(zug, this.isWhite(), board)) {
                moegliche.add(zug);
            }
            zug = new Zug("e8c8");
            if (Spiel.isValid(zug, this.isWhite(), board)) {
                moegliche.add(zug);
            }
        }

        return moegliche;
    }
}