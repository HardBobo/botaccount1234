public class BotEngine {

    private boolean whiteToMove = true;

    public void newGame(){
        Board.setupBoard(Board.brett);
        whiteToMove = true;
    }

    public void loadFEN(String fen){
        Board.fenToBoard(fen);
    }

    public void makeMove(String move){
        Zug zug = new Zug(move);
        MoveInfo info = MoveFinder.saveMoveInfo(zug, Board.brett);
        MoveFinder.doMove(zug, Board.brett, info);
        whiteToMove = !whiteToMove;
    }

    public String bestMove(boolean whiteToMove){
        return MoveFinder.iterativeDeepening(Board.brett, whiteToMove).processZug();
    }

    public void setSideToMove(boolean isWhite){
        this.whiteToMove = isWhite;
    }

    public boolean isWhiteToMove(){
        return whiteToMove;
    }
}
