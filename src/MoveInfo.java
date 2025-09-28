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
    public long oldHash;
    public Piece capturedPiece;
    public Koordinaten capEnPassantBauerCoords;
    public boolean wasEnPassant;
    public boolean wasPromotion;
    public Piece promotionPiece;

    // Bitboard-related bookkeeping for undo/hash
    public int oldCastlingRightsMask; // bitmask (wK=1,wQ=2,bK=4,bQ=8)
    public int oldEpSquare;           // -1 or 0..63
}
