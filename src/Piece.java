import java.util.List;

public abstract class Piece {
    private final String type;
    private final boolean white;
    private final int value;

    public Piece(String name, boolean istWeiss, int value) {
        this.type = name;
        this.white = istWeiss;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public boolean isWhite() {
        return white;
    }
    public int getValue(){
        return value;
    }

    public abstract List<Koordinaten> bedrohteFelder(int x, int y, Piece[][] board);
    public abstract List<Zug> moeglicheZuege(int x, int y, Piece[][] board);
}
