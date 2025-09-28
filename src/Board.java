public class Board {
    public static boolean whiteToMove = true;
    public static Bitboards bitboards = new Bitboards();

    public static void setupStartPosition(){
        whiteToMove = true;
        bitboards.initStartPosition();
    }

    public static void loadFEN(String fen) {
        bitboards.clear();
        String[] parts = fen.trim().split(" ");
        String boardPart = parts[0];
        String activeColor = parts.length > 1 ? parts[1] : "w";
        String castlingRights = parts.length > 2 ? parts[2] : "-";
        String enPassant = parts.length > 3 ? parts[3] : "-";

        String[] rows = boardPart.split("/");
        for (int y = 0; y < 8; y++) {
            String row = rows[y];
            int x = 0;
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (Character.isDigit(c)) {
                    x += (c - '0');
                } else {
                    boolean isWhite = Character.isUpperCase(c);
                    char lower = Character.toLowerCase(c);
                    int t = switch (lower) {
                        case 'p' -> 0;
                        case 'n' -> 1;
                        case 'b' -> 2;
                        case 'r' -> 3;
                        case 'q' -> 4;
                        case 'k' -> 5;
                        default -> -1;
                    };
                    if (t >= 0) {
                        int sq = y * 8 + x;
                        if (isWhite) bitboards.w[t] |= (1L << sq); else bitboards.b[t] |= (1L << sq);
                        x++;
                    }
                }
            }
        }

        whiteToMove = activeColor.equals("w");
        bitboards.wK = castlingRights.contains("K");
        bitboards.wQ = castlingRights.contains("Q");
        bitboards.bK = castlingRights.contains("k");
        bitboards.bQ = castlingRights.contains("q");

        if (!enPassant.equals("-")) {
            int epX = enPassant.charAt(0) - 'a';
            int epY = 8 - Character.getNumericValue(enPassant.charAt(1));
            if (epX >= 0 && epX < 8 && epY >= 0 && epY < 8) {
                bitboards.epSquare = epY * 8 + epX;
            } else {
                bitboards.epSquare = -1;
            }
        } else {
            bitboards.epSquare = -1;
        }

        bitboards.updateOcc();
    }
}
