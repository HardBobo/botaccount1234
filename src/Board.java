import javax.swing.*;
import java.awt.*;
public class Board {
    public static JButton[][] buttons = new JButton[8][8];
    public static Piece [][] brett = new Piece[8][8];
    public static Icon pawnWhite = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\PawnWhite.png");
    public static Icon rookWhite = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\RookWhite.png");
    public static Icon bishopWhite = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\BishopWhite.png");
    public static Icon knightWhite = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\KnightWhite.png");
    public static Icon queenWhite = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\QueenWhite.png");
    public static Icon kingWhite = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\KingWhite.png");
    public static Icon pawnBlack = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\PawnBlack.png");
    public static Icon rookBlack = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\RookBlack.png");
    public static Icon bishopBlack = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\BishopBlack.png");
    public static Icon knightBlack = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\KnightBlack.png");
    public static Icon queenBlack = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\QueenBlack.png");
    public static Icon kingBlack = new ImageIcon("C:\\Users\\hardb\\Desktop\\Schach2\\KingBlack.png");
    private static JFrame frame;
    public static void setupBoard(){
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
                brett[i][j] = p;
            }
            white = !white;
        }
        brett[7][0] = new Turm(true);
        brett[7][7] = new Turm(true);
        brett[7][1] = new Springer(true);
        brett[7][6] = new Springer(true);
        brett[7][2] = new Laeufer(true);
        brett[7][5] = new Laeufer(true);
        brett[7][3] = new Dame(true);
        brett[7][4] = new Koenig(true);
        brett[0][7] = new Turm(false);
        brett[0][0] = new Turm(false);
        brett[0][1] = new Springer(false);
        brett[0][6] = new Springer(false);
        brett[0][2] = new Laeufer(false);
        brett[0][5] = new Laeufer(false);
        brett[0][3] = new Dame(false);
        brett[0][4] = new Koenig(false);
        for(int i = 0; i < 8; i++){
            brett[6][i] = new Bauer(true);
        }
        for(int i = 0; i < 8; i++){
            brett[1][i] = new Bauer(false);
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
