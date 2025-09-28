import java.util.Random;

public class Zobrist {
    public static long[][] pieceSquareKeys;
    public static long sideToMoveKey;
    public static long[] castleKeys;
    public static long[] enPassantKeys;

    public static void initZobrist() {
        Random random = new Random(239847502); // fester Seed, damit gleiche Zufallszahlen bei jedem Start
        pieceSquareKeys = new long[12][64];
        castleKeys = new long[4];
        enPassantKeys = new long[8];

        // Figuren/Felder
        for (int piece = 0; piece < 12; piece++) {
            for (int square = 0; square < 64; square++) {
                pieceSquareKeys[piece][square] = random.nextLong();
            }
        }

        // Rochaderechte
        for (int i = 0; i < 4; i++) {
            castleKeys[i] = random.nextLong();
        }

        // En passant
        for (int i = 0; i < 8; i++) {
            enPassantKeys[i] = random.nextLong();
        }

        // Seite am Zug
        sideToMoveKey = random.nextLong();
    }

    public static long getPieceSquareKey(int piece, int square) {
        return pieceSquareKeys[piece][square];
    }

    public static long getCastleKey(int index) {
        return castleKeys[index];
    }

    public static long getEnPassantKey(int file) {
        return enPassantKeys[file];
    }

    public static long getSideToMoveKey() {
        return sideToMoveKey;
    }
    // computeHash(Piece[][]) removed in bitboard-only refactor

    public static long computeHash(Bitboards bb, boolean whiteToMove) {
        long hash = 0L;
        // Pieces: white indices 0..5, black 6..11 matching getPieceIndex mapping
        for (int type = 0; type < 6; type++) {
            long wbb = bb.w[type];
            while (wbb != 0) {
                int sq = Long.numberOfTrailingZeros(wbb);
                wbb &= wbb - 1;
                int pieceIndex = type; // white base 0
                hash ^= Zobrist.getPieceSquareKey(pieceIndex, sq);
            }
            long bbb = bb.b[type];
            while (bbb != 0) {
                int sq = Long.numberOfTrailingZeros(bbb);
                bbb &= bbb - 1;
                int pieceIndex = 6 + type; // black base 6
                hash ^= Zobrist.getPieceSquareKey(pieceIndex, sq);
            }
        }

        if (whiteToMove) {
            hash ^= Zobrist.getSideToMoveKey();
        }
        // Castling rights in order [whiteShort, whiteLong, blackShort, blackLong]
        if (bb.wK) hash ^= Zobrist.getCastleKey(0);
        if (bb.wQ) hash ^= Zobrist.getCastleKey(1);
        if (bb.bK) hash ^= Zobrist.getCastleKey(2);
        if (bb.bQ) hash ^= Zobrist.getCastleKey(3);

        int epFile = getEnPassantFileFromBB(bb, whiteToMove);
        if (epFile >= 0) {
            hash ^= Zobrist.getEnPassantKey(epFile);
        }
        return hash;
    }

    public static long updateHash(
            long hash,
            Zug zug,
            MoveInfo info,
            boolean[] castleRightsBefore,
            boolean[] castleRightsAfter,
            int enPassantFileBefore,
            int enPassantFileAfter
    ) {
        int fromSq = zug.startY * 8 + zug.startX;
        int toSq   = zug.endY * 8 + zug.endX;

        int movingIndex = (info.movingPieceWhite ? 0 : 6) + info.movingPieceType;

        if (!info.wasPromotion) {
            hash ^= getPieceSquareKey(movingIndex, fromSq);
            hash ^= getPieceSquareKey(movingIndex, toSq);
        } else {
            hash ^= getPieceSquareKey(movingIndex, fromSq);
            int promotionPieceIndex = (info.movingPieceWhite ? 0 : 6) + info.promotionType;
            hash ^= getPieceSquareKey(promotionPieceIndex, toSq);
        }

        if (info.rookMoved) {
            // rook color equals king's color (moving piece)
            int rookIndex = info.movingPieceWhite ? 3 : 9; // base 0 or 6 + 3 for rook
            int rToSq = zug.endY * 8 + info.rookEndX;
            int rFromSq = zug.endY * 8 + info.rookStartX;
            hash ^=  Zobrist.getPieceSquareKey(rookIndex, rFromSq);
            hash ^=  Zobrist.getPieceSquareKey(rookIndex, rToSq);
        }

//        System.out.println("Hash nach Rochade: " + hash);

        // geschlagene Figur (falls vorhanden)
        if (!info.squareMovedOntoWasEmpty && !info.wasEnPassant) {
            int capturedIndex = (info.capturedPieceWhite ? 0 : 6) + info.capturedPieceType;
            hash ^= getPieceSquareKey(capturedIndex, toSq);
        } else if (info.wasEnPassant) {
            int capturedIndex = (info.capturedPieceWhite ? 0 : 6) + 0; // pawn
            int capSq = info.capEnPassantBauerCoords.y * 8 + info.capEnPassantBauerCoords.x;
            hash ^= getPieceSquareKey(capturedIndex, capSq);
        }


//        System.out.println("Hash nach geschlagener Figur: " + hash);

        // Rochaderechte vor/nach
        for (int i = 0; i < 4; i++) {
            if (castleRightsBefore[i] != castleRightsAfter[i]) {
                hash ^= castleKeys[i];
            }
        }

//        System.out.println("Hash nach RochaderechteUpdate Figur: " + hash);

        // En-Passant vor/nach
        if (enPassantFileBefore != -1)
            hash ^= enPassantKeys[enPassantFileBefore];
        if (enPassantFileAfter != -1)
            hash ^= enPassantKeys[enPassantFileAfter];

//        System.out.println("Hash nach EnPassantUpdate: " + hash);

        // Side to move
        hash ^= sideToMoveKey;

//        System.out.println("Hash nach SideToMoveKey: " + hash);

        return hash;
    }

    // getPieceIndex(Piece) removed in bitboard-only refactor


    public static int getEnPassantFileFromBB(Bitboards bb, boolean whiteToMove) {
        if (bb.epSquare == -1) return -1;
        int colorIndex = whiteToMove ? 0 : 1;
        long pawns = whiteToMove ? bb.w[0] : bb.b[0];
        long attackers = Attacks.pawnAttack[colorIndex][bb.epSquare] & pawns;
        if (attackers != 0L) {
            return Bitboards.xOf(bb.epSquare);
        }
        return -1;
    }

    public static long nullMoveHashUpdate(long hash, int epFile) {
        if (epFile >= 0 && epFile < 8) {
            hash ^= enPassantKeys[epFile];
        }
        hash ^= sideToMoveKey;
        return hash;
    }
}
