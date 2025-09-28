public class Bitboards {
    // Piece type indices: 0=P,1=N,2=B,3=R,4=Q,5=K
    public long[] w = new long[6];
    public long[] b = new long[6];

    public long occW, occB, occ;

    // Castling rights
    public boolean wK, wQ, bK, bQ;

    // En passant target square (0..63) or -1 if none
    public int epSquare = -1;

    public Bitboards() { clear(); }

    public void clear() {
        for (int i = 0; i < 6; i++) { w[i] = 0L; b[i] = 0L; }
        occW = occB = occ = 0L;
        wK = wQ = bK = bQ = false;
        epSquare = -1;
    }

    public static int sq(int x, int y) { return y * 8 + x; }
    public static int xOf(int sq) { return sq & 7; }
    public static int yOf(int sq) { return sq >>> 3; }
    public static long bb(int sq) { return 1L << sq; }

    public void updateOcc() {
        occW = 0L; for (long v : w) occW |= v;
        occB = 0L; for (long v : b) occB |= v;
        occ = occW | occB;
    }

    public void initStartPosition() {
        clear();
        // White pieces (rank 1 = y=7, rank 2 = y=6)
        // Explicit setup
        w[0] = 0x00FF_0000_0000_0000L;               // white pawns at y=6 -> bits 48..55
        w[3] = (1L << sq(0,7)) | (1L << sq(7,7));    // a1,h1
        w[1] = (1L << sq(1,7)) | (1L << sq(6,7));
        w[2] = (1L << sq(2,7)) | (1L << sq(5,7));
        w[4] = (1L << sq(3,7));
        w[5] = (1L << sq(4,7));

        // Black pieces (rank 8 = y=0, rank 7 = y=1)
        b[0] = 0x0000_0000_0000_FF00L;               // black pawns at y=1 -> bits 8..15
        b[3] = (1L << sq(0,0)) | (1L << sq(7,0));    // a8,h8
        b[1] = (1L << sq(1,0)) | (1L << sq(6,0));
        b[2] = (1L << sq(2,0)) | (1L << sq(5,0));
        b[4] = (1L << sq(3,0));
        b[5] = (1L << sq(4,0));

        wK = wQ = bK = bQ = true;
        epSquare = -1;
        updateOcc();
    }

    public void syncFromBoard(Piece[][] board) {
        clear();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board[y][x];
                if (p instanceof Empty) continue;
                int t = p.getType();
                long bit = 1L << sq(x, y);
                if (p.isWhite()) w[t] |= bit; else b[t] |= bit;
            }
        }
        updateOcc();
        // Do not attempt to infer castling/EP from board[][]; these are set by FEN parse or move updates.
    }

    public boolean isWhiteAt(int sq) { return (occW & bb(sq)) != 0; }
    public boolean isBlackAt(int sq) { return (occB & bb(sq)) != 0; }
    public boolean isOccupied(int sq) { return (occ & bb(sq)) != 0; }

    public int pieceTypeAt(int sq) {
        long mask = bb(sq);
        for (int t = 0; t < 6; t++) {
            if ((w[t] & mask) != 0) return t;
            if ((b[t] & mask) != 0) return t;
        }
        return -1;
    }

    public int pieceTypeAt(int sq, boolean white) {
        long mask = bb(sq);
        long[] arr = white ? w : b;
        for (int t = 0; t < 6; t++) if ((arr[t] & mask) != 0) return t;
        return -1;
    }

    public long pieces(boolean white, int type) { return white ? w[type] : b[type]; }
    public long allPieces(boolean white) { return white ? occW : occB; }

    public static boolean isPromotionDest(boolean isWhite, int toSq) {
        int y = yOf(toSq);
        return isWhite ? (y == 0) : (y == 7);
    }

    // Simple attack detectors (used by inCheck / isSquareAttacked)
    public boolean isSquareAttackedBy(boolean byWhite, int targetSq) {
        long occLocal = occ;
        // Pawns
        if (byWhite) {
            long wP = w[0];
            long pawnAtt = Attacks.pawnAttack[0][targetSq]; // squares from which a white pawn would attack targetSq
            if ((pawnAtt & wP) != 0) return true;
        } else {
            long bP = b[0];
            long pawnAtt = Attacks.pawnAttack[1][targetSq];
            if ((pawnAtt & bP) != 0) return true;
        }
        // Knights
        long nMask = Attacks.knight[targetSq];
        if (byWhite) { if ((nMask & w[1]) != 0) return true; } else { if ((nMask & b[1]) != 0) return true; }
        // Kings
        long kMask = Attacks.king[targetSq];
        if (byWhite) { if ((kMask & w[5]) != 0) return true; } else { if ((kMask & b[5]) != 0) return true; }
        // Bishops/Queens (diagonals)
        long diag = Attacks.bishopAttacks(targetSq, occLocal);
        if (byWhite) { if (((w[2] | w[4]) & diag) != 0) return true; } else { if (((b[2] | b[4]) & diag) != 0) return true; }
        // Rooks/Queens (orthogonals)
        long ortho = Attacks.rookAttacks(targetSq, occLocal);
        if (byWhite) { if (((w[3] | w[4]) & ortho) != 0) return true; } else { if (((b[3] | b[4]) & ortho) != 0) return true; }
        return false;
    }

    public boolean inCheck(boolean white) {
        long k = white ? w[5] : b[5];
        if (k == 0) return false; // should not happen
        int kingSq = Long.numberOfTrailingZeros(k);
        return isSquareAttackedBy(!white, kingSq);
    }

    public boolean isCapture(Zug z) {
        int from = z.startY * 8 + z.startX;
        int to = z.endY * 8 + z.endX;
        boolean moverWhite = (occW & (1L << from)) != 0L;
        int movingType = pieceTypeAt(from, moverWhite);
        if (((occ >>> to) & 1L) != 0L) return true; // normal capture
        // En passant: pawn moves diagonally to current ep square
        if (movingType == 0 && epSquare == to && (z.endX != z.startX)) return true;
        return false;
    }

    public boolean willPromote(Zug z) {
        int from = z.startY * 8 + z.startX;
        boolean moverWhite = (occW & (1L << from)) != 0L;
        int movingType = pieceTypeAt(from, moverWhite);
        if (movingType != 0) return false;
        int to = z.endY * 8 + z.endX;
        return isPromotionDest(moverWhite, to);
    }

    public boolean isLegalMove(Zug z, boolean isWhite) {
        int from = z.startY * 8 + z.startX;
        int to = z.endY * 8 + z.endX;
        int movingType = pieceTypeAt(from, isWhite);

        // Special castling legality: cannot castle out of, through, or into check
        if (movingType == 5 && Math.abs(z.endX - z.startX) == 2) {
            // Must not be in check at start
            if (inCheck(isWhite)) return false;
            // Intermediate square (f-file for short, d-file for long)
            int midX = (z.endX > z.startX) ? z.startX + 1 : z.startX - 1;
            int midSq = z.startY * 8 + midX;
            if (isSquareAttackedBy(!isWhite, midSq)) return false;
            // Final square checked after applying the move below
        }

        MoveInfo inf = new MoveInfo();
        applyMove(z, inf);
        boolean ok = !inCheck(isWhite);
        undoMove(z, inf);
        return ok;
    }

    public boolean onlyHasPawns(boolean white) {
        if (white) {
            return (w[1] | w[2] | w[3] | w[4]) == 0L;
        } else {
            return (b[1] | b[2] | b[3] | b[4]) == 0L;
        }
    }

    // Apply a move to bitboards and mirror Board.brett, recording info for undo and hashing
    public void applyMove(Zug z, MoveInfo info) {
        int from = z.startY * 8 + z.startX;
        int to = z.endY * 8 + z.endX;

        

        boolean moverWhite = isWhiteAt(from);
        info.movingPieceWhite = moverWhite;
        info.movingPieceType = pieceTypeAt(from, moverWhite);
        info.squareMovedOntoWasEmpty = !isOccupied(to);
        if (!info.squareMovedOntoWasEmpty) {
            boolean capW = isWhiteAt(to);
            info.capturedPieceWhite = capW;
            info.capturedPieceType = pieceTypeAt(to, capW);
        }

        // Save pre-move state to info for undo
        info.oldCastlingRightsMask = rightsMask();
        info.oldEpSquare = epSquare;

        int movingType = info.movingPieceType;
        boolean isCapture = isOccupied(to);

        // En passant detection (target square empty but pawn moves diagonally into epSquare)
        boolean isEnPassant = false;
        int capturedSq = to;
        if (movingType == 0 && !isCapture && epSquare == to) {
            isEnPassant = true;
            capturedSq = moverWhite ? (to + 8) : (to - 8);
            info.wasEnPassant = true;
            info.capturedPieceType = 0;
            info.capturedPieceWhite = !moverWhite;
            info.capEnPassantBauerCoords = new Koordinaten(xOf(capturedSq), yOf(capturedSq));
            info.squareMovedOntoWasEmpty = true;
        }

        // Promotion detection
        boolean willPromote = (movingType == 0) && isPromotionDest(moverWhite, to) && (z.promoteTo == 'q' || z.promoteTo == 'r' || z.promoteTo == 'b' || z.promoteTo == 'n');
        if (willPromote) {
            info.wasPromotion = true;
            info.promotionType = promoTypeFromChar(z.promoteTo);
        }

        // Castling detection
        boolean isCastle = (movingType == 5) && Math.abs(z.endX - z.startX) == 2;
        if (isCastle) {
            info.rookMoved = true;
            if (moverWhite) {
                if (z.endX > z.startX) { info.rookStartX = 7; info.rookEndX = 5; }
                else { info.rookStartX = 0; info.rookEndX = 3; }
            } else {
                if (z.endX > z.startX) { info.rookStartX = 7; info.rookEndX = 5; }
                else { info.rookStartX = 0; info.rookEndX = 3; }
            }
        }

        // Remove captured piece
        if (isCapture) {
            boolean capWhite = isWhiteAt(to);
            int capType = pieceTypeAt(to, capWhite);
            if (capWhite) w[capType] &= ~bb(to); else b[capType] &= ~bb(to);
        } else if (isEnPassant) {
            boolean capWhite = isWhiteAt(capturedSq);
            if (capWhite) w[0] &= ~bb(capturedSq); else b[0] &= ~bb(capturedSq);
        }

        // Move piece
        long fromMask = bb(from), toMask = bb(to);
        if (moverWhite) {
            w[movingType] &= ~fromMask;
            if (!willPromote) w[movingType] |= toMask; else {
                int promoType = promoTypeFromChar(z.promoteTo);
                w[promoType] |= toMask;
            }
        } else {
            b[movingType] &= ~fromMask;
            if (!willPromote) b[movingType] |= toMask; else {
                int promoType = promoTypeFromChar(z.promoteTo);
                b[promoType] |= toMask;
            }
        }

        // Handle rook move in castling
        if (isCastle) {
            if (moverWhite) {
                int rookFrom = (z.endX > z.startX) ? sq(7,7) : sq(0,7);
                int rookTo   = (z.endX > z.startX) ? sq(5,7) : sq(3,7);
                long rf = bb(rookFrom), rt = bb(rookTo);
                w[3] &= ~rf; w[3] |= rt;
            } else {
                int rookFrom = (z.endX > z.startX) ? sq(7,0) : sq(0,0);
                int rookTo   = (z.endX > z.startX) ? sq(5,0) : sq(3,0);
                long rf = bb(rookFrom), rt = bb(rookTo);
                b[3] &= ~rf; b[3] |= rt;
            }
        }

        // Update castling rights
        if (movingType == 5) { // king moved
            if (moverWhite) { wK = false; wQ = false; } else { bK = false; bQ = false; }
        }
        if (movingType == 3) { // rook moved
            if (moverWhite) {
                if (from == sq(0,7)) wQ = false; if (from == sq(7,7)) wK = false;
            } else {
                if (from == sq(0,0)) bQ = false; if (from == sq(7,0)) bK = false;
            }
        }
        // If a rook got captured, clear its side's right as well
        if (isCapture) {
            if (to == sq(0,7)) wQ = false; if (to == sq(7,7)) wK = false;
            if (to == sq(0,0)) bQ = false; if (to == sq(7,0)) bK = false;
        }

        // Update EP square
        epSquare = -1;
        if (movingType == 0) {
            int delta = to - from;
            if (delta == -16) { // white double push
                epSquare = from - 8; // square jumped over
            } else if (delta == 16) { // black double push
                epSquare = from + 8;
            }
        }

        updateOcc();

        // No mirror updates
    }

    public void undoMove(Zug z, MoveInfo info) {
        int from = z.startY * 8 + z.startX;
        int to = z.endY * 8 + z.endX;
        boolean moverWhite = info.movingPieceWhite;
        int movingType = info.movingPieceType;

        // Undo promotion: remove promoted piece and restore pawn at from
        boolean wasPromotion = info.wasPromotion;
        if (wasPromotion) {
            // Remove promoted piece at to (promotionType), restore pawn at from
            int promoType = info.promotionType;
            if (moverWhite) w[promoType] &= ~bb(to); else b[promoType] &= ~bb(to);
            if (moverWhite) w[0] |= bb(from); else b[0] |= bb(from);
        } else {
            // Move piece back from 'to' to 'from'
            if (moverWhite) { w[movingType] &= ~bb(to); w[movingType] |= bb(from); }
            else { b[movingType] &= ~bb(to); b[movingType] |= bb(from); }
        }

        // Restore captured piece (including en-passant)
        if (info.wasEnPassant && info.capEnPassantBauerCoords != null) {
            int capSq = info.capEnPassantBauerCoords.y * 8 + info.capEnPassantBauerCoords.x;
            // Place captured pawn back
            if (moverWhite) b[0] |= bb(capSq); else w[0] |= bb(capSq);
            // Clear destination implicitly by moving piece bits
        } else if (!info.squareMovedOntoWasEmpty) {
            boolean capWhite = info.capturedPieceWhite;
            int capType = info.capturedPieceType;
            if (capWhite) w[capType] |= bb(to); else b[capType] |= bb(to);
        } else {
            // destination restored implicitly
        }

        // Undo castling rook move
        if (info.rookMoved) {
            if (moverWhite) {
                int rookFrom = (z.endX > z.startX) ? sq(7,7) : sq(0,7);
                int rookTo   = (z.endX > z.startX) ? sq(5,7) : sq(3,7);
                w[3] &= ~bb(rookTo); w[3] |= bb(rookFrom);
            } else {
                int rookFrom = (z.endX > z.startX) ? sq(7,0) : sq(0,0);
                int rookTo   = (z.endX > z.startX) ? sq(5,0) : sq(3,0);
                b[3] &= ~bb(rookTo); b[3] |= bb(rookFrom);
            }
        }

        // Restore castling rights and EP square
        setRightsFromMask(info.oldCastlingRightsMask);
        epSquare = info.oldEpSquare;
        updateOcc();

        // No mirror updates
    }

    public int rightsMask() {
        int m = 0;
        if (wK) m |= 1; if (wQ) m |= 2; if (bK) m |= 4; if (bQ) m |= 8;
        return m;
    }
    public void setRightsFromMask(int m) {
        wK = (m & 1) != 0; wQ = (m & 2) != 0; bK = (m & 4) != 0; bQ = (m & 8) != 0;
    }

    private static int promoTypeFromChar(char c) {
        return switch (c) {
            case 'n' -> 1;
            case 'b' -> 2;
            case 'r' -> 3;
            default -> 4; // 'q' or default to queen
        };
    }

    private static Piece pieceObject(int type, boolean white) {
        return switch (type) {
            case 0 -> new Bauer(white);
            case 1 -> new Springer(white);
            case 2 -> new Laeufer(white);
            case 3 -> new Turm(white);
            case 4 -> new Dame(white);
            case 5 -> new Koenig(white);
            default -> new Empty();
        };
    }
}
