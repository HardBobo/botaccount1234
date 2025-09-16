import java.util.List;

public abstract class Piece {
    private final int type;
    private final boolean white;

    public Piece(int type, boolean istWeiss) {
        this.type = type;
        this.white = istWeiss;
    }

    public int getType() {
        return type;
    }

    public boolean isWhite() {
        return white;
    }
    public abstract List<Zug> moeglicheZuege(int x, int y, Piece[][] board);
}
