import java.util.*;

/**
 * PieceTracker maintains efficient lists of piece positions to avoid scanning the entire board
 * for move generation and evaluation. This significantly improves performance.
 */
public class PieceTracker {
    // Lists of piece positions for each piece type and color
    private final List<Koordinaten> whitePawns = new ArrayList<>();
    private final List<Koordinaten> blackPawns = new ArrayList<>();
    private final List<Koordinaten> whiteKnights = new ArrayList<>();
    private final List<Koordinaten> blackKnights = new ArrayList<>();
    private final List<Koordinaten> whiteBishops = new ArrayList<>();
    private final List<Koordinaten> blackBishops = new ArrayList<>();
    private final List<Koordinaten> whiteRooks = new ArrayList<>();
    private final List<Koordinaten> blackRooks = new ArrayList<>();
    private final List<Koordinaten> whiteQueens = new ArrayList<>();
    private final List<Koordinaten> blackQueens = new ArrayList<>();
    private Koordinaten whiteKing = null;
    private Koordinaten blackKing = null;
    
    // All pieces by color for quick access
    private final List<Koordinaten> whitePieces = new ArrayList<>();
    private final List<Koordinaten> blackPieces = new ArrayList<>();
    
    public PieceTracker() {
    }
    
    /**
     * Initialize the piece tracker by scanning the board once
     */
    public void initializeFromBoard(Piece[][] board) {
        clearAllLists();
        
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];
                if (!(piece instanceof Empty)) {
                    addPiece(piece, x, y);
                }
            }
        }
    }
    
    /**
     * Update piece positions when a move is made
     */
    public void updateMove(Zug zug, MoveInfo info, Piece[][] board) {
        // Remove piece from start position
        removePiece(info.movingPiece, zug.startX, zug.startY);
        
        // Handle captured piece (regular capture, not en passant)
        if (info.squareMovedOnto != null && !(info.squareMovedOnto instanceof Empty) && !info.wasEnPassant) {
            removePiece(info.squareMovedOnto, zug.endX, zug.endY);
        }
        
        // Handle en passant capture
        if (info.wasEnPassant && info.capturedPiece != null) {
            removePiece(info.capturedPiece, info.capEnPassantBauerCoords.x, info.capEnPassantBauerCoords.y);
        }
        
        // Handle promotion: add the promoted piece, not the original pawn
        if (info.wasPromotion) {
            // The board now contains the promoted piece, add it to tracker
            Piece promotedPiece = board[zug.endY][zug.endX];
            addPiece(promotedPiece, zug.endX, zug.endY);
        } else {
            // Normal move: add the moved piece to new position
            addPiece(info.movingPiece, zug.endX, zug.endY);
        }
        
        // Handle castling rook movement
        if (info.rookMoved) {
            Piece rook = board[zug.endY][info.rookEndX];
            removePiece(rook, info.rookStartX, zug.endY);
            addPiece(rook, info.rookEndX, zug.endY);
        }
    }
    
    /**
     * Undo move changes to piece positions
     */
    public void undoMove(Zug zug, MoveInfo info, Piece[][] board) {
        // Handle promotion undo: remove the promoted piece, restore the original pawn
        if (info.wasPromotion) {
            // Remove the promoted piece (queen, rook, bishop, knight)
            removePiece(info.promotionPiece, zug.endX, zug.endY);
        } else {
            // Normal move: remove the piece that was moved
            removePiece(info.movingPiece, zug.endX, zug.endY);
        }
        
        // Add original piece back to start position (always the original moving piece)
        addPiece(info.movingPiece, zug.startX, zug.startY);
        
        // Restore captured piece (regular capture, not en passant)
        if (info.squareMovedOnto != null && !(info.squareMovedOnto instanceof Empty) && !info.wasEnPassant) {
            addPiece(info.squareMovedOnto, zug.endX, zug.endY);
        }
        
        // Handle en passant restore
        if (info.wasEnPassant && info.capturedPiece != null) {
            addPiece(info.capturedPiece, info.capEnPassantBauerCoords.x, info.capEnPassantBauerCoords.y);
        }
        
        // Handle castling rook undo
        if (info.rookMoved) {
            Piece rook = board[zug.endY][info.rookStartX];
            removePiece(rook, info.rookEndX, zug.endY);
            addPiece(rook, info.rookStartX, zug.endY);
        }
    }
    
    /**
     * Add a piece to the tracking lists
     */
    private void addPiece(Piece piece, int x, int y) {
        if (piece instanceof Empty) return;
        
        Koordinaten pos = new Koordinaten(x, y);
        boolean isWhite = piece.isWhite();
        
        // Add to color-specific list
        if (isWhite) {
            whitePieces.add(pos);
        } else {
            blackPieces.add(pos);
        }
        
        // Add to piece-specific list
        switch (piece) {
            case Bauer bauer -> {
                if (isWhite) whitePawns.add(pos);
                else blackPawns.add(pos);
            }
            case Springer springer -> {
                if (isWhite) whiteKnights.add(pos);
                else blackKnights.add(pos);
            }
            case Laeufer laeufer -> {
                if (isWhite) whiteBishops.add(pos);
                else blackBishops.add(pos);
            }
            case Turm turm -> {
                if (isWhite) whiteRooks.add(pos);
                else blackRooks.add(pos);
            }
            case Dame dame -> {
                if (isWhite) whiteQueens.add(pos);
                else blackQueens.add(pos);
            }
            case Koenig koenig -> {
                if (isWhite) whiteKing = pos;
                else blackKing = pos;
            }
            default -> {}
        }
    }
    
    /**
     * Remove a piece from the tracking lists
     */
    private void removePiece(Piece piece, int x, int y) {
        if (piece instanceof Empty) return;
        
        Koordinaten pos = new Koordinaten(x, y);
        boolean isWhite = piece.isWhite();
        
        // Remove from color-specific list
        if (isWhite) {
            whitePieces.removeIf(p -> p.x == x && p.y == y);
        } else {
            blackPieces.removeIf(p -> p.x == x && p.y == y);
        }
        
        // Remove from piece-specific list
        switch (piece) {
            case Bauer bauer -> {
                if (isWhite) whitePawns.removeIf(p -> p.x == x && p.y == y);
                else blackPawns.removeIf(p -> p.x == x && p.y == y);
            }
            case Springer springer -> {
                if (isWhite) whiteKnights.removeIf(p -> p.x == x && p.y == y);
                else blackKnights.removeIf(p -> p.x == x && p.y == y);
            }
            case Laeufer laeufer -> {
                if (isWhite) whiteBishops.removeIf(p -> p.x == x && p.y == y);
                else blackBishops.removeIf(p -> p.x == x && p.y == y);
            }
            case Turm turm -> {
                if (isWhite) whiteRooks.removeIf(p -> p.x == x && p.y == y);
                else blackRooks.removeIf(p -> p.x == x && p.y == y);
            }
            case Dame dame -> {
                if (isWhite) whiteQueens.removeIf(p -> p.x == x && p.y == y);
                else blackQueens.removeIf(p -> p.x == x && p.y == y);
            }
            case Koenig koenig -> {
                if (isWhite) whiteKing = null;
                else blackKing = null;
            }
            default -> {}
        }
    }
    
    private void clearAllLists() {
        whitePawns.clear();
        blackPawns.clear();
        whiteKnights.clear();
        blackKnights.clear();
        whiteBishops.clear();
        blackBishops.clear();
        whiteRooks.clear();
        blackRooks.clear();
        whiteQueens.clear();
        blackQueens.clear();
        whitePieces.clear();
        blackPieces.clear();
        whiteKing = null;
        blackKing = null;
    }
    
    // Getter methods for efficient access
    public List<Koordinaten> getPawns(boolean white) {
        return white ? whitePawns : blackPawns;
    }
    
    public List<Koordinaten> getKnights(boolean white) {
        return white ? whiteKnights : blackKnights;
    }
    
    public List<Koordinaten> getBishops(boolean white) {
        return white ? whiteBishops : blackBishops;
    }
    
    public List<Koordinaten> getRooks(boolean white) {
        return white ? whiteRooks : blackRooks;
    }
    
    public List<Koordinaten> getQueens(boolean white) {
        return white ? whiteQueens : blackQueens;
    }
    
    public Koordinaten getKing(boolean white) {
        return white ? whiteKing : blackKing;
    }
    
    public List<Koordinaten> getAllPieces(boolean white) {
        return white ? whitePieces : blackPieces;
    }
    
    /**
     * Generate all possible moves for a color using piece lists instead of board scanning
     */
    public ArrayList<Zug> generateMoves(boolean white, Piece[][] board) {
        ArrayList<Zug> moves = new ArrayList<>();
        
        List<Koordinaten> pieces = getAllPieces(white);
        for (Koordinaten pos : pieces) {
            Piece piece = board[pos.y][pos.x];
            moves.addAll(piece.moeglicheZuege(pos.x, pos.y, board));
        }
        
        return moves;
    }
    
    /**
     * Get piece count for game phase calculation
     */
    public int getPieceCount(boolean white) {
        return getAllPieces(white).size();
    }
    
    /**
     * Check if position has specific piece type (for quick evaluation)
     */
    public boolean hasPieceAt(int x, int y, boolean white, Class<? extends Piece> pieceType) {
        List<Koordinaten> pieces = getAllPieces(white);
        for (Koordinaten pos : pieces) {
            if (pos.x == x && pos.y == y) {
                return true; // Found piece at position, would need board to check type
            }
        }
        return false;
    }
}