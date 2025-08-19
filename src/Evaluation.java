public class Evaluation {
    public Evaluation(){

    }

//middlegame heatmaps wo die jeweiligen Figuren sein sollten
    static int[][] mg_pawn = {
        {0, 0, 0, 0, 0, 0, 0, 0},
        {98, 134, 61, 95, 68, 126, 34, -11},
        {-6, 7, 26, 31, 65, 56, 25, -20},
        {-14, 13, 6, 21, 23, 12, 17, -23},
        {-27, -2, -5, 12, 17, 6, 10, -25},
        {-26, -4, -4, -10, 3, 3, 33, -12},
        {-35, -1, -20, -23, -15, 24, 38, -22},
        {0, 0, 0, 0, 0, 0, 0, 0}
    };
    static int[][] mg_rook = {
            {32,  42,  32,  51, 63,  9,  31,  43},
            {27,  32,  58,  62, 80, 67,  26,  44},
            {-5,  19,  26,  36, 17, 45,  61,  16},
            {-24, -11,   7,  26, 24, 35,  -8, -20},
            {-36, -26, -12,  -1,  9, -7,   6, -23},
            {-45, -25, -16, -17,  3,  0,  -5, -33},
            {-44, -16, -20,  -9, -1, 11,  -6, -71},
            {-19, -13,   1,  2, 16,  -12, -37, -26}};
    // heat map für turm auch angepasst damit er weniger gerne einfach ohne zu rochieren auf den rochade platz geht
    // original: {-19, -13,   1,  17, 16,  7, -37, -26}};
    static int[][] mg_queen = {
            {-28,   0,  29,  12,  59,  44,  43,  45},
            {-24, -39,  -5,   1, -16,  57,  28,  54},
            {-13, -17,   7,   8,  29,  56,  47,  57},
            {-27, -27, -16, -16,  -1,  17,  -2,   1},
            {-9, -26,  -9, -10,  -2,  -4,   3,  -3},
            {-14,   2, -11,  -2,  -5,   2,  14,   5},
            {-35,  -8,  11,   2,   8,  15,  -3,   1},
            {-1, -18,  -9,  10, -15, -25, -31, -50}};
    static int[][] mg_king = {
            {-65,  23,  16, -15, -56, -34,   2,  13},
            { 29,  -1, -20,  -7,  -8,  -4, -38, -29},
            {-9,  24,   2, -16, -20,   6,  22, -22},
            {-17, -20, -12, -27, -30, -25, -14, -36},
            {-49,  -1, -27, -39, -46, -44, -33, -51},
            {-14, -14, -22, -46, -44, -30, -15, -27},
            {1,   7,  -8, -64, -43, -16,   9,   8},
            {-15,  55,  30, -54,   8, -28,  66,  14}};
    // damit rochade attraktiver fuer den bot ist wurde die heatmap verändert unten ist orginal
    //      {-15,  36,  12, -54,   8, -28,  24,  14}};
    static int[][] mg_knight = {
            {-167, -89, -34, -49, 61, -97, -15, -107},
            {-73, -41, 72, 36, 23, 62, 7, -17,},
            {-47, 60, 37, 65, 84, 129, 73, 44,},
            {-9, 17, 19, 53, 37, 69, 18, 22},
            {-13, 4, 16, 13, 28, 19, 21, -8},
            {-23, -9, 12, 10, 19, 17, 25, -16},
            {-29, -53, -12, -3, -1, 18,- 14, -19},
            {-105, -21, -58, -33, -17, -28, -19, -23}};

    static int[][] mg_bishop = {
            {-29,   4, -82, -37, -25, -42,   7,  -8},
            {-26,  16, -18, -13,  30,  59,  18, -47},
            {-16,  37,  43,  40,  35,  50,  37,  -2},
            {-4,   5,  19,  50,  37,  37,   7,  -2},
            {-6,  13,  13,  26,  34,  12,  10,   4},
            {0,  15,  15,  15,  14,  27,  18,  10},
            {4,  15,  16,   0,   7,  21,  33,   1},
            {-33,  -3, -14, -21, -13, -12, -39, -21}};

    //endgame heatmaps wo die jeweiligen Figuren sein sollten

    static int [][] eg_pawn = {
            {0,   0,   0,   0,   0,   0,   0,   0},
            {178, 173, 158, 134, 147, 132, 165, 187},
            {94, 100,  85,  67,  56,  53,  82,  84},
            {32,  24,  13,   5,  -2,   4,  17,  17},
            {13,   9,  -3,  -7,  -7,  -8,   3,  -1},
            {4,   7,  -6,   1,   0,  -5,  -1,  -8},
            {13,   8,   8,  10,  13,   0,   2,  -7},
            {0,   0,   0,   0,   0,   0,   0,   0}
    };
    static int[][] eg_rook = {
            {13, 10, 18, 15, 12,  12,   8,   5},
            {11, 13, 13, 11, -3,   3,   8,   3},
            {7,  7,  7,  5,  4,  -3,  -5,  -3},
            {4,  3, 13,  1,  2,   1,  -1,   2},
            {3,  5,  8,  4, -5,  -6,  -8, -11},
            {-4,  0, -5, -1, -7, -12,  -8, -16},
            {-6, -6,  0,  2, -9,  -9, -11,  -3},
            {-9,  2,  3, -1, -5, -13,   4, -20}
    };
    static int[][] eg_queen = {
            {-9,  22,  22,  27,  27,  19,  10,  20},
            {-17,  20,  32,  41,  58,  25,  30,   0},
            {-20,   6,   9,  49,  47,  35,  19,   9},
            {3,  22,  24,  45,  57,  40,  57,  36},
            {-18,  28,  19,  47,  31,  34,  39,  23},
            {-16, -27,  15,   6,   9,  17,  10,   5},
            {-22, -23, -30, -16, -16, -23, -36, -32},
            {-33, -28, -22, -43,  -5, -32, -20, -41}
    };
    static int[][] eg_king = {
            {-74, -35, -18, -18, -11,  15,   4, -17},
            {-12,  17,  14,  17,  17,  38,  23,  11},
            {10,  17,  23,  15,  20,  45,  44,  13},
            {-8,  22,  24,  27,  26,  33,  26,   3},
            {-18,  -4,  21,  24,  27,  23,   9, -11},
            {-19,  -3,  11,  21,  23,  16,   7,  -9},
            {-27, -11,   4,  13,  14,   4,  -5, -17},
            {-53, -34, -21, -11, -28, -14, -24, -43}
    };
    static int[][] eg_knight = {
            {-58, -38, -13, -28, -31, -27, -63, -99},
            {-25,  -8, -25,  -2,  -9, -25, -24, -52},
            {-24, -20,  10,   9,  -1,  -9, -19, -41},
            {-17,   3,  22,  22,  22,  11,   8, -18},
            {-18,  -6,  16,  25,  16,  17,   4, -18},
            {-23,  -3,  -1,  15,  10,  -3, -20, -22},
            {-42, -20, -10,  -5,  -2, -20, -23, -44},
            {-29, -51, -23, -15, -22, -18, -50, -64}
    };
    static int[][] eg_bishop = {
            {-14, -21, -11,  -8, -7,  -9, -17, -24},
            {-8,  -4,   7, -12, -3, -13,  -4, -14},
            {2,  -8,   0,  -1, -2,   6,   0,   4},
            {-3,   9,  12,   9, 14,  10,   3,   2},
            {-6,   3,  13,  19,  7,  10,  -3,  -9},
            {-12,  -3,   8,  10, 13,   3,  -7, -15},
            {-14, -18,  -7,  -1,  4,  -9, -15, -27},
            {-23,  -9, -23,  -5, -9, -16,  -5, -17}
    };
    public static int evalWithPosition (Koordinaten where, Piece[][] brett, boolean iswhite)
    {
        //Methode die für die übergebene Figur einen Wert abhängig von der Position und dem Gamestate(End- oder MIddlegame) ausgibt
        int value = 0;
        Piece piece = brett[where.y][where.x];
        boolean endgame = endgame(brett);

        if (!endgame) {
            switch (piece) {
                case Bauer bauer -> {
                    if (iswhite) {
                        value = mg_pawn[where.y][where.x];
                    } else{
                        value = brettFlipper(bauer, false)[where.y][where.x];
                    }
                }
                case Turm turm -> {
                    if (iswhite) {
                        value = mg_rook[where.y][where.x];
                    } else {
                        value = brettFlipper(turm, false)[where.y][where.x];
                    }
                }
                case Dame dame -> {
                    if (iswhite) {
                        value = mg_queen[where.y][where.x];
                    } else {
                        value = brettFlipper(dame, false)[where.y][where.x];
                    }
                }
                case Springer springer -> {
                    if (iswhite) {
                        value = mg_knight[where.y][where.x];
                    } else {
                        value = brettFlipper(springer, false)[where.y][where.x];
                    }
                }
                case Laeufer laeufer -> {
                    if (iswhite) {
                        value = mg_bishop[where.y][where.x];
                    } else {
                        value = brettFlipper(laeufer, false)[where.y][where.x];
                    }
                }
                case Koenig koenig -> {
                    if (iswhite) {
                        value = mg_king[where.y][where.x];
                    } else {
                        value = brettFlipper(koenig, false)[where.y][where.x];
                    }
                }
                case Empty empty -> value = 0;
                case null, default -> System.err.println("Fehler bei eval middlegame");
            }
        }
        else{
            switch (piece) {
                case Bauer bauer -> {
                    if (iswhite) {
                        value = eg_pawn[where.y][where.x];
                    } else{
                        value = brettFlipper(bauer, true)[where.y][where.x];
                    }
                }
                case Turm turm -> {
                    if (iswhite) {
                        value = eg_rook[where.y][where.x];
                    } else {
                        value = brettFlipper(turm, true)[where.y][where.x];
                    }
                }
                case Dame dame -> {
                    if (iswhite) {
                        value = eg_queen[where.y][where.x];
                    } else {
                        value = brettFlipper(dame, true)[where.y][where.x];
                    }
                }
                case Springer springer -> {
                    if (iswhite) {
                        value = eg_knight[where.y][where.x];
                    } else {
                        value = brettFlipper(springer, true)[where.y][where.x];
                    }
                }
                case Laeufer laeufer -> {
                    if (iswhite) {
                        value = eg_bishop[where.y][where.x];
                    } else {
                        value = brettFlipper(laeufer, true)[where.y][where.x];
                    }
                }
                case Koenig koenig -> {
                    if (iswhite) {
                        value = eg_king[where.y][where.x];
                    } else {
                        value = brettFlipper(koenig, true)[where.y][where.x];
                    }
                }
                case Empty empty -> value = 0;
                case null, default -> System.err.println("Fehler bei eval middlegame");
            }
        }

        return value;
    }
    public static boolean endgame(Piece [][] board){
        //Methode die Abhängig von dem noch auf dem Feld vorhandenen Material sagt, ob es ein End- oder Middelgame ist
        Piece p;
        int sumValueW = 0;
        int sumValueB = 0;
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                p = board[i][j];
                if(!(p instanceof Empty)){
                    if(p.isWhite()){
                        sumValueW += p.getValue();
                    } else {
                        sumValueB += p.getValue();
                    }
                }
            }
        }
        return sumValueW <= 1500 && sumValueB <= 1500;
    }

    public static int [][] brettFlipper(Piece p, boolean eg)
    {
        //Methode spiegelt die Belohnungstabellen damit sie auch für Schwarz stimmen
        //Es wird ein Array zurückgegeben spezifisch für die jeweilige Figur und Spielstatus (End- oder Middelgame)
        int[][]tempA = new int[8][8];
        int temp = 0;

                switch (p) {
                    case Bauer bauer -> {
                        if(!eg) {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = mg_pawn[7 - i][j];
                                    tempA[7 - i][j] = mg_pawn[i][j];
                                }
                            }
                        }
                        else {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = eg_pawn[7 - i][j];
                                    tempA[7 - i][j] = eg_pawn[i][j];
                                }
                            }
                        }
                    }
                    case Turm turm -> {

                        if(!eg) {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = mg_rook[7 - i][j];
                                    tempA[7 - i][j] = mg_rook[i][j];
                                }
                            }
                        }
                        else {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = eg_rook[7 - i][j];
                                    tempA[7 - i][j] = eg_rook[i][j];
                                }
                            }
                        }
                    }
                    case Springer springer -> {
                        if(!eg) {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = mg_knight[7 - i][j];
                                    tempA[7 - i][j] = mg_knight[i][j];
                                }
                            }
                        }
                        else {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = eg_knight[7 - i][j];
                                    tempA[7 - i][j] = eg_knight[i][j];
                                }
                            }
                        }
                    }
                    case Laeufer laeufer -> {
                        if(!eg) {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = mg_bishop[7 - i][j];
                                    tempA[7 - i][j] = mg_bishop[i][j];
                                }
                            }
                        }
                        else {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = eg_bishop[7 - i][j];
                                    tempA[7 - i][j] = eg_bishop[i][j];
                                }
                            }
                        }
                    }
                    case Dame dame -> {
                        if(!eg) {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = mg_queen[7 - i][j];
                                    tempA[7 - i][j] = mg_queen[i][j];
                                }
                            }
                        }
                        else {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = eg_queen[7 - i][j];
                                    tempA[7 - i][j] = eg_queen[i][j];
                                }
                            }
                        }
                    }
                    case Koenig koenig -> {
                        if(!eg) {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = mg_king[7 - i][j];
                                    tempA[7 - i][j] = mg_king[i][j];
                                }
                            }
                        }
                        else {
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 4; i++) {
                                    tempA[i][j] = eg_king[7 - i][j];
                                    tempA[7 - i][j] = eg_king[i][j];
                                }
                            }
                        }
                    }
                    case Empty empty -> {
                        for (int j = 0; j < 8; j++) {
                            for (int i = 0; i < 4; i++) {
                                tempA[i][j] = 0;
                                tempA[7-i][j] = 0;
                            }
                        }
                    }
                    case null, default -> System.err.println("Fehler bei Brettflip");
                }
          return tempA;
    }
}
