import java.util.Scanner;

public class UCIWrapper {

    private static final BotEngine engine = new BotEngine();
    static String[] moveList;
    static String moves;
    static int moveCount;
    private static int lastProcessedMoveCount = 0;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        while (in.hasNextLine()) {
            String command = in.nextLine().trim();

            if (command.equals("uci")) {
                System.out.println("id name botaccount1234");
                System.out.println("id author HardBobo, CaroAce");
                System.out.println("uciok");
            }
            else if (command.equals("isready")) {
                System.out.println("readyok");
            }
            else if (command.startsWith("ucinewgame")) {
                engine.newGame();
                lastProcessedMoveCount = 0; // reset history
            }
            else if (command.startsWith("position")) {
                // reset game if startpos
                if (command.contains("startpos")) {
                    engine.newGame();
                    lastProcessedMoveCount = 0;
                }

                // load FEN if given
                if (command.contains("fen")) {
                    String fen = command.substring(command.indexOf("fen") + 4);
                    if (fen.contains("moves")) {
                        fen = fen.substring(0, fen.indexOf("moves")).trim();
                    }
                    engine.loadFEN(fen);
                    lastProcessedMoveCount = 0;

                    // detect side to move from FEN
                    String[] parts = fen.split(" ");
                    if (parts.length > 1) {
                        boolean whiteToMove = parts[1].equals("w");
                        engine.setSideToMove(whiteToMove);
                    }
                }

                // process moves
                if (command.contains("moves")) {
                    moves = command.substring(command.indexOf("moves") + 6);
                    moveList = moves.trim().split(" ");
                    moveCount = moveList.length;

                    // apply any new moves since last processed
                    for (int i = lastProcessedMoveCount; i < moveList.length; i++) {
                        engine.makeMove(moveList[i]);
                    }

                    lastProcessedMoveCount = moveList.length;
                }
            }
            else if (command.startsWith("go")) {
                // determine whose turn it is
                boolean whiteToMove = engine.isWhiteToMove(); // let engine handle it
                String bestMove = engine.bestMove(whiteToMove);
                System.out.println("bestmove " + bestMove);
            }
            else if (command.equals("quit")) {
                break;
            }
        }
    }
}