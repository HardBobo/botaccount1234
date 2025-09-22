public class ZobristTest {

    public static void main(String[] args) {
        System.out.println("Initializing Zobrist keys...");
        Zobrist.initZobrist();

        Piece[][] emptyBoard = createEmptyBoard();

//        testHashConsistency(emptyBoard);
//        testHashChangesAfterMove(emptyBoard);
//        testCastlingMove();
//        testEnPassantHashing(emptyBoard);
//        testSideToMoveToggle(emptyBoard);
        testPromotionHashing(emptyBoard);

        System.out.println("All tests done.");
    }

    private static void testHashConsistency(Piece[][] emptyBoard) {
        System.out.println("\nTest: Hash Consistency");

        Piece[][] board = copyBoard(emptyBoard);
        board[1][4] = new Bauer(true); // White pawn on e2

        long hash1 = Zobrist.computeHash(board, true);
        long hash2 = Zobrist.computeHash(board, true);

        System.out.println("Hash1: " + hash1);
        System.out.println("Hash2: " + hash2);
        System.out.println("Hashes equal? " + (hash1 == hash2));
    }

    private static void testHashChangesAfterMove(Piece[][] emptyBoard) {
        System.out.println("\nTest: Hash Changes After Move");

        Piece[][] boardBefore = copyBoard(emptyBoard);
        boardBefore[1][4] = new Bauer(true); // Pawn on e2

        Piece[][] boardAfter = copyBoard(emptyBoard);
        boardAfter[3][4] = new Bauer(true); // Pawn moved to e4

        long hashBefore = Zobrist.computeHash(boardBefore, true);
        long hashAfter = Zobrist.computeHash(boardAfter, false);

        System.out.println("Hash before move: " + hashBefore);
        System.out.println("Hash after move:  " + hashAfter);

        Zug move = new Zug(4, 1, 4, 3); // from e2 to e4 (x,y)
        MoveInfo info = new MoveInfo();
        info.movingPiece = new Bauer(true);
        info.wasPromotion = false;
        info.squareMovedOnto = new Empty();
        info.rookMoved = false;
        info.wasEnPassant = false;

        boolean[] castleRightsBefore = MoveFinder.getCastleRights(boardBefore);
        boolean[] castleRightsAfter = MoveFinder.getCastleRights(boardAfter);

        int enPassantFileBefore = Zobrist.getEnPassantFile(boardBefore, true);
        int enPassantFileAfter = Zobrist.getEnPassantFile(boardAfter, false);

        long updatedHash = Zobrist.updateHash(
                hashBefore,
                move,
                info,
                boardAfter,
                castleRightsBefore,
                castleRightsAfter,
                enPassantFileBefore,
                enPassantFileAfter
        );

        System.out.println("Updated hash after move: " + updatedHash);
        System.out.println("Updated hash matches recomputed? " + (updatedHash == hashAfter));
    }

    private static void testCastlingMove() {
        System.out.println("\nTest: Castling Move");

        // Setup initial board with white king and rook in starting positions
        Piece[][] boardBefore = createEmptyBoard();
        boardBefore[7][4] = new Koenig(true);  // White king on e1
        boardBefore[7][7] = new Turm(true);    // White rook on h1

        // Castling rights: all true initially
        boolean[] castleRightsBefore = {true, false, false, false};

        // Compute hash before castling
        long hashBefore = Zobrist.computeHash(boardBefore, true);

        // Setup board after white short castling (king to g1, rook to f1)
        Piece[][] boardAfter = createEmptyBoard();
        boardAfter[7][6] = new Koenig(true);  // King on g1
        boardAfter[7][5] = new Turm(true);    // Rook on f1

        // Castling rights after castling: white short castling no longer available
        boolean[] castleRightsAfter = {false, false, false, false};

        // Create Zug representing king move e1->g1
        Zug kingMove = new Zug(4, 7, 6, 7); // x,y: from e1(4,7) to g1(6,7)

        // MoveInfo for castling
        MoveInfo info = new MoveInfo();
        info.movingPiece = new Koenig(true);
        info.wasPromotion = false;
        info.squareMovedOnto = new Empty();
        info.rookMoved = true; // rook also moved during castling
        info.rookStartX = 7;   // rook from h1 (7,7)
        info.rookEndX = 5;     // rook to f1 (5,7)
        info.wasEnPassant = false;

        // Compute en passant files before and after (none)
        int enPassantFileBefore = -1;
        int enPassantFileAfter = -1;

        // Update hash with castling move
        long updatedHash = Zobrist.updateHash(
                hashBefore,
                kingMove,
                info,
                boardAfter,
                castleRightsBefore,
                castleRightsAfter,
                enPassantFileBefore,
                enPassantFileAfter
        );

        // Compute hash from scratch for board after castling
        long hashAfter = Zobrist.computeHash(boardAfter, false); // side to move toggled

        System.out.println("Hash before castling: " + hashBefore);
        System.out.println("Hash after castling (recomputed): " + hashAfter);
        System.out.println("Hash after castling (updated):    " + updatedHash);
        System.out.println("Hashes match? " + (hashAfter == updatedHash));
    }


    private static void testEnPassantHashing(Piece[][] emptyBoard) {
        System.out.println("\nTest: En Passant Hashing");

        Piece[][] board = copyBoard(emptyBoard);
        board[3][4] = new Bauer(true);  // White pawn on e5
        board[3][3] = new Bauer(false); // Black pawn on d5

        // Simulate en passant possible on black pawn
        ((Bauer) board[3][3]).setEnPassantPossible(true);

        long hashBefore = Zobrist.computeHash(board, true);
        int enPassantFile = Zobrist.getEnPassantFile(board, true);

        System.out.println("Hash before en passant: " + hashBefore);
        System.out.println("Detected en passant file: " + enPassantFile);

        Zug move = new Zug(4, 3, 3, 2); // dummy move coords (adjust if needed)
        MoveInfo info = new MoveInfo();
        info.movingPiece = new Bauer(true);
        info.wasPromotion = false;
        info.squareMovedOnto = new Empty();
        info.rookMoved = false;
        info.wasEnPassant = true;
        info.capturedPiece = new Bauer(false);
        info.capEnPassantBauerCoords = new Koordinaten(3, 3);

        boolean[] castleRightsBefore = MoveFinder.getCastleRights(board);
        boolean[] castleRightsAfter = castleRightsBefore;

        int enPassantFileBefore = enPassantFile;
        int enPassantFileAfter = -1;

        long updatedHash = Zobrist.updateHash(
                hashBefore,
                move,
                info,
                board,
                castleRightsBefore,
                castleRightsAfter,
                enPassantFileBefore,
                enPassantFileAfter
        );

        System.out.println("Updated hash after en passant capture: " + updatedHash);
        System.out.println("Hash changed? " + (updatedHash != hashBefore));

        // --- MANUAL SIMULATION ---
        System.out.println("\nManual simulation:");

        // Start with the old hash
        long manualHash = hashBefore;

        // 2. Move the moving piece
        manualHash ^= Zobrist.getPieceSquareKey(info.movingPiece.getType(), move.startY * 8 + move.startX); // remove from old square
        manualHash ^= Zobrist.getPieceSquareKey(info.movingPiece.getType(), move.endY * 8 + move.endX);     // add to new square
        board[move.endY][move.endX] = info.movingPiece;
        board[move.startY][move.startX] = new Empty();
        System.out.println("Hash after moving pawn: " + manualHash);

        // 1. Remove captured piece (en passant pawn)
        manualHash ^= Zobrist.getPieceSquareKey(Zobrist.getPieceIndex(info.capturedPiece), info.capEnPassantBauerCoords.y * 8 + info.capEnPassantBauerCoords.x);
        board[info.capEnPassantBauerCoords.y][info.capEnPassantBauerCoords.x] = new Empty();
        System.out.println("Hash after removing captured pawn: " + manualHash);

        // 4. En-passant file cleared (if applicable)
        if (enPassantFileBefore != -1) {
            manualHash ^= Zobrist.getEnPassantKey(enPassantFileBefore);
            System.out.println("Hash after clearing en-passant file: " + manualHash);
        }

        // 3. Side to move changes
        manualHash ^= Zobrist.getSideToMoveKey();
        System.out.println("Hash after side to move change: " + manualHash);

        System.out.println("Manual hash matches updated hash? " + (manualHash == updatedHash));
    }

    private static void testSideToMoveToggle(Piece[][] emptyBoard) {
        System.out.println("\nTest: Side To Move Toggle");

        Piece[][] board = copyBoard(emptyBoard);
        board[1][0] = new Bauer(true);

        long hashWhite = Zobrist.computeHash(board, true);
        long hashBlack = Zobrist.computeHash(board, false);

        System.out.println("Hash white to move: " + hashWhite);
        System.out.println("Hash black to move: " + hashBlack);

        long diff = hashWhite ^ hashBlack;
        System.out.println("XOR difference: " + diff);
        System.out.println("SideToMoveKey: " + Zobrist.getSideToMoveKey());
        System.out.println("Difference equals SideToMoveKey? " + (diff == Zobrist.getSideToMoveKey()));
    }

    private static void testPromotionHashing(Piece[][] emptyBoard) {
        System.out.println("\nTest: Promotion Hashing");

        Piece[][] boardBefore = copyBoard(emptyBoard);
        boardBefore[6][0] = new Bauer(true); // Pawn on a7

        Piece[][] boardAfter = copyBoard(emptyBoard);
        boardAfter[7][0] = new Dame(true); // Promoted to queen on a8

        long hashBefore = Zobrist.computeHash(boardBefore, true);
        long hashAfter = Zobrist.computeHash(boardAfter, false);

        System.out.println("Hash before promotion: " + hashBefore);
        System.out.println("Hash after promotion:  " + hashAfter);

        Zug move = new Zug(0, 6, 0, 7);
        MoveInfo info = new MoveInfo();
        info.movingPiece = new Bauer(true);
        info.wasPromotion = true;
        info.promotionPiece = new Dame(true);
        info.squareMovedOnto = new Empty();
        info.rookMoved = false;
        info.wasEnPassant = false;

        boolean[] castleRightsBefore = MoveFinder.getCastleRights(boardBefore);
        boolean[] castleRightsAfter = MoveFinder.getCastleRights(boardAfter);

        int enPassantFileBefore = Zobrist.getEnPassantFile(boardBefore, true);
        int enPassantFileAfter = Zobrist.getEnPassantFile(boardAfter, false);

        long updatedHash = Zobrist.updateHash(
                hashBefore,
                move,
                info,
                boardAfter,
                castleRightsBefore,
                castleRightsAfter,
                enPassantFileBefore,
                enPassantFileAfter
        );

        System.out.println("Updated hash after promotion: " + updatedHash);
        System.out.println("Hashes match? " + (updatedHash == hashAfter));
    }

    private static Piece[][] createEmptyBoard() {
        Piece[][] board = new Piece[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                board[y][x] = new Empty();
            }
        }
        return board;
    }

    private static Piece[][] copyBoard(Piece[][] original) {
        Piece[][] copy = new Piece[8][8];
        for (int y = 0; y < 8; y++) {
            System.arraycopy(original[y], 0, copy[y], 0, 8);
        }
        return copy;
    }
}
