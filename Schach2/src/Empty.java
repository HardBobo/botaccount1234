import java.util.List;

public class Empty extends Piece{

    public Empty(){
        super("empty", true, 0);
    }
    @Override
    public List<Koordinaten> bedrohteFelder(int x, int y, Piece[][] board) {
        return null;
    }

    @Override
    public List<Zug> moeglicheZuege(int x, int y, Piece[][] board) {
        return null;
    }
}