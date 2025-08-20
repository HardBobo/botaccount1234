public class Board {
    public static Piece [][] brett = new Piece[8][8];
    public static void setupBoard(Piece [][] board){
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
}
