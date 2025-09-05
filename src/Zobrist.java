import java.util.Random;

public class Zobrist {
    private static long[][] pieceSquareKeys;
    private static long sideToMoveKey;
    private static long[] castleKeys;
    private static long[] enPassantKeys;

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
        int enPassantFile = -1;

        // Nur relevante rank: weiße Bauern auf 4, schwarze auf 3
        int targetRank = whiteToMove ? 3 : 4;

        for (int x = 0; x < 8; x++) {
            Piece p = board[targetRank][x];
            if (p instanceof Bauer b && b.isEnPassantPossible()) {
                // Prüfe, ob dieser Bauer gerade zwei Felder gezogen sein könnte
                // z.B. weißer Bauer auf rank 5 = y=3 für Hashberechnung
                // Prüfe, ob Gegner auf adjacent file vorhanden ist
                int adjLeft = x - 1;
                int adjRight = x + 1;
                boolean capturePossible = (adjLeft >= 0 && board[targetRank][adjLeft] instanceof Bauer &&
                        board[targetRank][adjLeft].isWhite() != p.isWhite()) ||
                        (adjRight <= 7 && board[targetRank][adjRight] instanceof Bauer &&
                                board[targetRank][adjRight].isWhite() != p.isWhite());
                if (capturePossible) {
                    enPassantFile = x;
                    break; // nur ein en passant File kann gleichzeitig gelten
                }
            }
        }

        if (enPassantFile >= 0) {
            hash ^= Zobrist.getEnPassantKey(enPassantFile);
        }

        return hash;
    }
    private static int getPieceIndex(Piece p) {
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
}
