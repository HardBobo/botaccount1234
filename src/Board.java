public class Board {
    public static Piece [][] brett = new Piece[8][8];
    public static boolean whiteToMove = true;
    public static PieceTracker pieceTracker = new PieceTracker();


    public static void setupBoard(Piece [][] board){
        whiteToMove = true;
        boolean white = true;
        Piece p = new Empty();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if(white){
                    white = false;
                }
                else{
                    white = true;
                }
                board[i][j] = p;
            }
            white = !white;
        }
        board[7][0] = new Turm(true);
        board[7][7] = new Turm(true);
        board[7][1] = new Springer(true);
        board[7][6] = new Springer(true);
        board[7][2] = new Laeufer(true);
        board[7][5] = new Laeufer(true);
        board[7][3] = new Dame(true);
        board[7][4] = new Koenig(true);
        board[0][7] = new Turm(false);
        board[0][0] = new Turm(false);
        board[0][1] = new Springer(false);
        board[0][6] = new Springer(false);
        board[0][2] = new Laeufer(false);
        board[0][5] = new Laeufer(false);
        board[0][3] = new Dame(false);
        board[0][4] = new Koenig(false);
        for(int i = 0; i < 8; i++){
            board[6][i] = new Bauer(true);
        }
        for(int i = 0; i < 8; i++){
            board[1][i] = new Bauer(false);
        }
        
        // Initialize piece tracker after setting up the board
        pieceTracker.initializeFromBoard(board);
    }
    public static Piece[][] copy(Piece [][] board) {
        Piece[][] kopie = new Piece[8][8];

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece original = board[i][j];

                switch (original) {
                    case Bauer bauer -> {
                        kopie[i][j] = new Bauer(original.isWhite());
                        if(((Bauer) original).isEnPassantPossible()) {
                            ((Bauer) kopie[i][j]).setEnPassantPossible(true);
                        }
                    }
                    case Turm turm -> {
                        kopie[i][j] = new Turm(original.isWhite());
                        if (!((Turm) original).kannRochieren()) {
                            ((Turm) kopie[i][j]).setKannRochieren(false);
                        }
                    }
                    case Springer springer -> kopie[i][j] = new Springer(original.isWhite());
                    case Laeufer laeufer -> kopie[i][j] = new Laeufer(original.isWhite());
                    case Dame dame -> kopie[i][j] = new Dame(original.isWhite());
                    case Koenig koenig -> {
                        kopie[i][j] = new Koenig(original.isWhite());
                        if (!((Koenig) original).kannRochieren()) {
                            ((Koenig) kopie[i][j]).setKannRochieren(false);
                        }
                    }
                    case null, default -> kopie[i][j] = new Empty();
                }
            }
        }
        return kopie;
    }
    public static String boardToString(Piece[][] board, boolean whiteToMove) {
        StringBuilder sb = new StringBuilder();
        for (Piece[] pieces : board) {
            for (Piece p : pieces) {
                switch (p) {
                    case Empty empty -> sb.append(".");
                    case Bauer bauer -> {
                        sb.append(p.isWhite() ? "P" : "p");
                        sb.append(bauer.isEnPassantPossible() ? "y" : "n");
                    }
                    case Turm turm -> {
                        sb.append(p.isWhite() ? "R" : "r");
                        sb.append(turm.kannRochieren() ? "y" : "n");
                    }
                    case Springer springer -> sb.append(p.isWhite() ? "N" : "n");
                    case Laeufer laeufer -> sb.append(p.isWhite() ? "B" : "b");
                    case Dame dame -> sb.append(p.isWhite() ? "Q" : "q");
                    case Koenig koenig -> {
                        sb.append(p.isWhite() ? "K" : "k");
                        sb.append(koenig.kannRochieren() ? "y" : "n");
                    }
                    case null, default -> sb.append("?"); // falls du mal eine neue Figurklasse einbaust
                }
            }
            sb.append("/"); // Trenner zwischen Reihen
        }
        sb.append(whiteToMove ? "w" : "b");
        return sb.toString();
    }

    public static Piece[][] fenToBoard(String fen) {
        Piece[][] board = new Piece[8][8];

        String[] parts = fen.split(" ");
        String boardPart = parts[0];
        String activeColor = parts[1];
        String castlingRights = parts[2];
        String enPassant = parts[3];

        String[] rows = boardPart.split("/");

        for (int y = 0; y < 8; y++) {
            String row = rows[y];
            int x = 0;

            for (char c : row.toCharArray()) {
                if (Character.isDigit(c)) {
                    int empty = c - '0';
                    for (int i = 0; i < empty; i++) {
                        board[y][x] = new Empty();
                        x++;
                    }
                } else {
                    board[y][x] = createPieceFromFenChar(c);
                    x++;
                }
            }
        }

        // --- Whos turn ---
        whiteToMove = activeColor.equals("w");

        // --- Castling rights ---
        if (!castlingRights.equals("-")) {
            for (char c : castlingRights.toCharArray()) {
                if (c == 'K') { // White kingside
                    if (board[7][4] instanceof Koenig k && k.isWhite())
                        k.setKannRochieren(true);
                    if (board[7][7] instanceof Turm r && r.isWhite())
                        r.setKannRochieren(true);
                } else if (c == 'Q') { // White queenside
                    if (board[7][4] instanceof Koenig k2 && k2.isWhite())
                        k2.setKannRochieren(true);
                    if (board[7][0] instanceof Turm r2 && r2.isWhite())
                        r2.setKannRochieren(true);
                } else if (c == 'k') { // Black kingside
                    if (board[0][4] instanceof Koenig k3 && !k3.isWhite())
                        k3.setKannRochieren(true);
                    if (board[0][7] instanceof Turm r3 && !r3.isWhite())
                        r3.setKannRochieren(true);
                } else if (c == 'q') { // Black queenside
                    if (board[0][4] instanceof Koenig k4 && !k4.isWhite())
                        k4.setKannRochieren(true);
                    if (board[0][0] instanceof Turm r4 && !r4.isWhite())
                        r4.setKannRochieren(true);
                }
            }
        }

        // --- En Passant ---
        if (!enPassant.equals("-")) {
            int epX = enPassant.charAt(0) - 'a';
            int epY = 8 - Character.getNumericValue(enPassant.charAt(1));

            if (activeColor.equals("w")) {
                // Black just pushed -> mark black pawn
                if (board[epY + 1][epX] instanceof Bauer b && !b.isWhite())
                    b.setEnPassantPossible(true);
            } else {
                // White just pushed -> mark white pawn
                if (board[epY - 1][epX] instanceof Bauer b && b.isWhite())
                    b.setEnPassantPossible(true);
            }
        }

        // Initialize piece tracker after parsing FEN
        pieceTracker.initializeFromBoard(board);
        
        return board;
    }

    private static Piece createPieceFromFenChar(char c) {
        boolean isWhite = Character.isUpperCase(c);
        char lower = Character.toLowerCase(c);

        return switch (lower) {
            case 'p' -> new Bauer(isWhite);
            case 'r' -> new Turm(isWhite);
            case 'n' -> new Springer(isWhite);
            case 'b' -> new Laeufer(isWhite);
            case 'q' -> new Dame(isWhite);
            case 'k' -> new Koenig(isWhite);
            default -> null;
        };
    }
}
