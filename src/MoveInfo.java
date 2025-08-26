public class MoveInfo {
    public MoveInfo(){

    }
    public Piece squareMovedOnto;
    public boolean wasFirstMove;
    public boolean rookMoved;
    public int rookStartX, rookEndX;
    public Piece movingPiece;
    public Koordinaten enPassantBauerCoords;
    public boolean wasEnPassantCapturable;
}