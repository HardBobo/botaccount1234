import java.util.ArrayList;
import java.util.List;

public class Bauer extends Piece {
    private boolean enPassantPossible;

    public Bauer(boolean istWeiss) {
        super("Bauer", istWeiss, 100);
        this.enPassantPossible = false;
    }
    public boolean isEnPassantPossible(){
        return enPassantPossible;
    }
    public void setEnPassantPossible(boolean wert){
        this.enPassantPossible = wert;
    }
    public boolean istZugMoeglich(int startX, int startY, int zielX, int zielY, char promoteTo, Piece[][] board) {
        int richtung = isWhite() ? -1 : 1; //weiß nach oben schwarz nach unten

        int dy = zielY - startY;
        int dx = zielX - startX;

        Piece ziel = board[zielY][zielX];

        // normal
        if (dx == 0 && dy == richtung && ziel instanceof Empty) {
            if(zielY == 7 || zielY == 0){
                return promoteTo == 'q' || promoteTo == 'n' || promoteTo == 'b' || promoteTo == 'r';
            }
            return true;
        }

        // doppelter
        if (dx == 0 && dy == 2 * richtung && (startY == 1 || startY == 6)) {
            int zwischenschrittY = startY + richtung;
            if (board[zwischenschrittY][startX] instanceof Empty && ziel instanceof Empty) {
                return true;
            }
        }

        // schlagen
        if (Math.abs(dx) == 1 && dy == richtung && !(ziel instanceof Empty) && ziel.isWhite() != this.isWhite()) {
            if(zielY == 7 || zielY == 0){
                return promoteTo == 'q' || promoteTo == 'n' || promoteTo == 'b' || promoteTo == 'r';
            }
            return true;
        }

        // en passant
        if (Math.abs(dx) == 1 && dy == richtung && board[startY][zielX] instanceof Bauer gegnerBauer) {
            return gegnerBauer.isWhite() != this.isWhite() && gegnerBauer.isEnPassantPossible();
        }
        return false;
    }
    @Override
    public List<Koordinaten> bedrohteFelder(int x, int y, Piece[][] board) {
        List<Koordinaten> bedrohte = new ArrayList<>();

        int richtung = this.isWhite() ? -1 : 1; //richtung

        int zielY = y + richtung;

        // links vorne
        if (Spiel.imBrett(x - 1, zielY)) {
            bedrohte.add(new Koordinaten(x - 1, zielY));
        }

        // rechts vorne
        if (Spiel.imBrett(x + 1, zielY)) {
            bedrohte.add(new Koordinaten(x + 1, zielY));
        }

        return bedrohte;
    }

    @Override
    public List<Zug> moeglicheZuege(int x, int y, Piece[][] board) {
        List<Zug> moegliche = new ArrayList<>();

        int richtung = this.isWhite() ? -1 : 1;

        Zug [] zuege = new Zug[4];
        Zug [] promotions = new Zug[12];
        if(y + richtung == 7 || y + richtung == 0) {
            promotions[0] = new Zug(x, y, x, y + richtung, 'q');
            promotions[1] = new Zug(x, y, x, y + richtung, 'n');
            promotions[2] = new Zug(x, y, x, y + richtung, 'r');
            promotions[3] = new Zug(x, y, x, y + richtung, 'b');
            promotions[4] = new Zug(x, y, x + 1, y + richtung, 'q');
            promotions[5] = new Zug(x, y, x + 1, y + richtung, 'n');
            promotions[6] = new Zug(x, y, x + 1, y + richtung, 'r');
            promotions[7] = new Zug(x, y, x + 1, y + richtung, 'b');
            promotions[8] = new Zug(x, y, x - 1, y + richtung, 'q');
            promotions[9] = new Zug(x, y, x - 1, y + richtung, 'n');
            promotions[10] = new Zug(x, y, x - 1, y + richtung, 'r');
            promotions[11] = new Zug(x, y, x - 1, y + richtung, 'b');
        } else {
            zuege[0] = new Zug(x, y, x, y + richtung);
            zuege[1] = new Zug(x, y, x, y + (2 * richtung));
            zuege[2] = new Zug(x, y, x + 1, y + richtung);
            zuege[3] = new Zug(x, y, x - 1, y + richtung);
        }
        if(y + richtung == 7 || y + richtung == 0) {
            for (Zug zug : promotions) {
                if (Spiel.isValid(zug, this.isWhite(), board)) {
                    moegliche.add(zug);
                }
            }
        } else {
            for (Zug zug : zuege) {
                if (Spiel.isValid(zug, this.isWhite(), board)) {
                    moegliche.add(zug);
                }
            }
        }
        return moegliche;
    }
}