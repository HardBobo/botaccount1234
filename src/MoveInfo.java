public class MoveInfo {
    public MoveInfo(){ }

    // Moving piece (type 0..5) and color
    public int movingPieceType;
    public boolean movingPieceWhite;

    // Capture info; if squareMovedOntoWasEmpty is true and wasEnPassant is false, then no capture
    public boolean squareMovedOntoWasEmpty;
    public int capturedPieceType; // valid if capture
    public boolean capturedPieceWhite; // valid if capture

    // Castling rook movement (if any)
    public boolean rookMoved;
    public int rookStartX, rookEndX;

    // En passant flags
    public boolean wasEnPassant;
    public Koordinaten capEnPassantBauerCoords; // square of captured pawn in EP

    // Promotion flags
    public boolean wasPromotion;
    public int promotionType; // 0..5 for promoted piece type

    // Bitboard-related bookkeeping for undo/hash
    public int oldCastlingRightsMask; // bitmask (wK=1,wQ=2,bK=4,bQ=8)
    public int oldEpSquare;           // -1 or 0..63
}
