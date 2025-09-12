import java.util.List;

public class Empty extends Piece{

    public Empty(){
        super(-1, true);
    }

    @Override
    public List<Zug> moeglicheZuege(int x, int y, Piece[][] board) {
        return null;
    }
}