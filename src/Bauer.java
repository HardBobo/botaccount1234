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
        int endreihe = this.isWhite() ? 0 : 7;

        if (Spiel.imBrett(x, y + richtung)
                && board[y + richtung][x] instanceof Empty) {
            if (y + richtung == endreihe) {
                addPromotions(moegliche, x, y, x, y + richtung);
            } else {
                moegliche.add(new Zug(x, y, x, y + richtung));
            }
        }

        if (y == 6 || y == 1) {
            if (Spiel.imBrett(x, y + richtung) && Spiel.imBrett(x, y +2*richtung)) {
                if (board[y + richtung][x] instanceof Empty
                        && board[y + 2 * richtung][x] instanceof Empty) {
                    moegliche.add(new Zug(x, y, x, y + 2 * richtung));
                }
            }
        }

        for (int dx : new int[]{-1, 1}) {
            int zielX = x + dx;
            int zielY = y + richtung;
            if (Spiel.imBrett(zielX, zielY)) {
                if (!(board[zielY][zielX] instanceof Empty)) {
                    if (board[zielY][zielX].isWhite() != this.isWhite()) {
                        if (zielY == endreihe) {
                            addPromotions(moegliche, x, y, zielX, zielY);
                        } else {
                            moegliche.add(new Zug(x, y, zielX, zielY));
                        }
                    }
                } else if (board[y][zielX] instanceof Bauer gegnerBauer
                        && gegnerBauer.isWhite() != this.isWhite()
                        && gegnerBauer.isEnPassantPossible()) {
                    moegliche.add(new Zug(x, y, zielX, zielY));
                }
            }
        }
        return moegliche;
    }
    private void addPromotions(List<Zug> moegliche, int startX, int startY, int zielX, int zielY) {
        char[] figuren = {'q', 'r', 'b', 'n'};
        for (char f : figuren) {
            moegliche.add(new Zug(startX, startY, zielX, zielY, f));
        }
    }
}