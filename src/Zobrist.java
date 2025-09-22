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
    public static long computeHash(Piece[][] board, boolean whiteToMove) {
        long hash = 0L;

        // Figuren auf dem Brett
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board[y][x];
                if (!(p instanceof Empty)) {
                    int pieceIndex = getPieceIndex(p); // eigene Methode unten
                    int squareIndex = y * 8 + x;       // 0..63
                    hash ^= Zobrist.getPieceSquareKey(pieceIndex, squareIndex);
                }
            }
        }

        // Seite am Zug
        if (whiteToMove) {
            hash ^= Zobrist.getSideToMoveKey();
        }

        // Rochaderechte (castleRights = [whiteShort, whiteLong, blackShort, blackLong])
        boolean [] castleRights = MoveFinder.getCastleRights(board);
        for (int i = 0; i < 4; i++) {
            if (castleRights[i]) {
                hash ^= Zobrist.getCastleKey(i);
            }
        }

        // En passant (falls vorhanden, -1 wenn keiner)
        int enPassantFile = getEnPassantFile(board, whiteToMove);

        if (enPassantFile >= 0) {
            hash ^= Zobrist.getEnPassantKey(enPassantFile);
        }

        return hash;
    }

    public static long updateHash(
            long hash,
            Zug zug,
            MoveInfo info,
            Piece [][] board,
            boolean[] castleRightsBefore,
            boolean[] castleRightsAfter,
            int enPassantFileBefore,
            int enPassantFileAfter
    ) {
        int fromSq = zug.startY * 8 + zug.startX;
        int toSq   = zug.endY * 8 + zug.endX;

        Piece moving = info.movingPiece;
        int movingIndex = getPieceIndex(moving);

        if (!info.wasPromotion) {
            // Figur vom Startfeld entfernen
            hash ^= getPieceSquareKey(movingIndex, fromSq);
            // Figur auf Zielfeld hinzufügen
            hash ^= getPieceSquareKey(movingIndex, toSq);
        } else {
            hash ^= getPieceSquareKey(movingIndex, fromSq);
            int promotionPieceIndex = getPieceIndex(info.promotionPiece);
            hash ^= getPieceSquareKey(promotionPieceIndex, toSq);
        }

//        System.out.println("Hash nach bewegen der Figur: " + hash);

        if (info.rookMoved) {
            Piece rook = board[zug.endY][info.rookEndX];
            int rookIndex = getPieceIndex(rook);
            int rToSq = zug.endY * 8 + info.rookEndX;
            int rFromSq = zug.endY * 8 + info.rookStartX;
            hash ^=  Zobrist.getPieceSquareKey(rookIndex, rFromSq);
            hash ^=  Zobrist.getPieceSquareKey(rookIndex, rToSq);
        }

//        System.out.println("Hash nach Rochade: " + hash);

        // geschlagene Figur (falls vorhanden)
        if (!(info.squareMovedOnto instanceof Empty)) {
            Piece captured = info.squareMovedOnto;
            int capturedIndex = getPieceIndex(captured);
            hash ^= getPieceSquareKey(capturedIndex, toSq);
        } else {
            if(info.wasEnPassant) {
                Piece captured = info.capturedPiece;
                int capturedIndex = getPieceIndex(captured);
                int capSq = info.capEnPassantBauerCoords.y * 8 + info.capEnPassantBauerCoords.x;
                hash ^= getPieceSquareKey(capturedIndex, capSq);
            }
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

    public static int getPieceIndex(Piece p) {
        int base = p.isWhite() ? 0 : 6;
        return switch (p) {
            case Bauer bauer -> base;
            case Springer springer -> base + 1;
            case Laeufer laeufer -> base + 2;
            case Turm turm -> base + 3;
            case Dame dame -> base + 4;
            case Koenig koenig -> base + 5;
            default -> throw new IllegalArgumentException("Unbekannte Figur: " + p);
        };
    }

    public static int getEnPassantFile(Piece[][] board, boolean whiteToMove) {
        // -1 = kein En Passant möglich
        int enPassantFile = -1;

        // Nur relevanter Rank: weiße Bauern auf 4, schwarze auf 3
        int targetRank = whiteToMove ? 3 : 4;

        for (int x = 0; x < 8; x++) {
            Piece p = board[targetRank][x];

            if (p instanceof Bauer b && b.isEnPassantPossible()) {
                // Prüfen, ob Gegner auf Nachbarfile steht
                int adjLeft = x - 1;
                int adjRight = x + 1;

                boolean capturePossible =
                        (adjLeft >= 0 && board[targetRank][adjLeft] instanceof Bauer b2 &&
                                b2.isWhite() != p.isWhite())
                                || (adjRight <= 7 && board[targetRank][adjRight] instanceof Bauer b3 &&
                                b3.isWhite() != p.isWhite());

                if (capturePossible) {
                    enPassantFile = x;
                    break; // nur ein En-Passant-File gleichzeitig erlaubt
                }
            }
        }

        return enPassantFile;
    }
}
