import java.util.Scanner;
import java.io.IOException;

public class UCIWrapper {

    static String[] moveList;
    static String moves;
    static int moveCount;
    public static long startHash;
    private static boolean baseWhiteToMove = true; // side to move at start of current position

    // UCI time controls (milliseconds)
    private static long wtimeMs = -1;
    private static long btimeMs = -1;
    private static long wincMs = 0;
    private static long bincMs = 0;

    public static void main(String[] args) throws IOException {
        // Initial engine state similar to LichessBotStream game start
        Spiel.newGame();
        resetVariables();

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
                // Reset full engine state and TT like Lichess game start
                Spiel.newGame();
                resetVariables();
            }
            else if (command.startsWith("position")) {

                // UCI 'position' provides a full position description. Reset incremental state.
                resetVariablesNoWhiteToMove();

                if (command.contains("startpos")) {
                    // Start position
                    Board.setupBoard(Board.brett);
                    baseWhiteToMove = true;
                    // Do not clear TT here; use ucinewgame for a hard reset
                    startHash = Zobrist.computeHash(Board.brett, true);
                }

                // load FEN if given
                if (command.contains("fen")) {
                    loadPosFromFen(command);
                }

                // process moves appended to the position
                if (command.contains("moves")) {
                    syncGameStateBoard(command);
                }
            }
            else if (command.startsWith("go")) {
                // Parse UCI time parameters (milliseconds)
                parseUciTimeParameters(command);

                // Determine side to move: start side flipped by number of moves applied
                boolean whiteToMove = (moveCount % 2 == 0) == baseWhiteToMove;

                // Try opening move first, else search
                Zug best = OpeningDictionary.getNextOpeningMove(moves == null ? "" : moves);
                if(best == null){
                    if (whiteToMove) {
                        if (wtimeMs >= 0 && wtimeMs <= 4000 && wincMs <= 500) {
                            best = panicBest(true);
                        } else {
                            long thinkMs = TimeManager.computeThinkTimeMs(wtimeMs, wincMs);
                            best = MoveFinder.iterativeDeepening(Board.brett, true, startHash, thinkMs);
                        }
                    } else {
                        if (btimeMs >= 0 && btimeMs <= 4000 && bincMs <= 500) {
                            best = panicBest(false);
                        } else {
                            long thinkMs = TimeManager.computeThinkTimeMs(btimeMs, bincMs);
                            best = MoveFinder.iterativeDeepening(Board.brett, false, startHash, thinkMs);
                        }
                    }
                }
                System.out.println("bestmove " + best.processZug());
            }
            else if (command.equals("quit")) {
                break;
            }
        }
    }
    private static void resetVariables(){
        baseWhiteToMove = true;
        moves = "";
        moveList = new String[0];
        moveCount = 0;
    }
    private static void resetVariablesNoWhiteToMove(){
        moves = "";
        moveList = new String[0];
        moveCount = 0;
    }
    private static void loadPosFromFen(String command){
        String fen = command.substring(command.indexOf("fen") + 4);
        if (fen.contains("moves")) {
            fen = fen.substring(0, fen.indexOf("moves")).trim();
        }
        Board.brett = Board.fenToBoard(fen);
        // detect side to move from FEN
        String[] parts = fen.split(" ");
        if (parts.length > 1) {
            baseWhiteToMove = parts[1].equals("w");
        } else {
            baseWhiteToMove = true;
        }
        startHash = Zobrist.computeHash(Board.brett, baseWhiteToMove);
    }
    private static void syncGameStateBoard(String command){
        moves = command.substring(command.indexOf("moves") + 6).trim();
        moveList = moves.isEmpty() ? new String[0] : moves.split(" ");
        moveCount = moveList.length;

        // apply the moves from the described root
        for (int i = 0; i < moveList.length; i++) {
            Zug zug = new Zug(moveList[i]);
            MoveInfo info = MoveFinder.saveMoveInfo(zug, Board.brett);
            startHash = MoveFinder.doMoveUpdateHash(zug, Board.brett, info, startHash);
        }
    }
    private static void parseUciTimeParameters(String command){
        String[] parts = command.split("\\s+");
        for (int i = 1; i < parts.length; i++) {
            switch (parts[i]) {
                case "wtime":
                    if (i + 1 < parts.length) { try { wtimeMs = Long.parseLong(parts[++i]); } catch (NumberFormatException ignored) {} }
                    break;
                case "btime":
                    if (i + 1 < parts.length) { try { btimeMs = Long.parseLong(parts[++i]); } catch (NumberFormatException ignored) {} }
                    break;
                case "winc":
                    if (i + 1 < parts.length) { try { wincMs = Long.parseLong(parts[++i]); } catch (NumberFormatException ignored) {} }
                    break;
                case "binc":
                    if (i + 1 < parts.length) { try { bincMs = Long.parseLong(parts[++i]); } catch (NumberFormatException ignored) {} }
                    break;
                default:
                    // ignore other go params for now
                    break;
            }
        }
    }
    private static Zug panicBest(boolean whiteToMove){
        return MoveFinder.searchToDepth(Board.brett, whiteToMove, startHash, 2);
    }
}
