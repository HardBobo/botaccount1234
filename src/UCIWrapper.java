import java.util.Scanner;
import java.io.IOException;

public class UCIWrapper {

    static String[] moveList;
    static String moves;
    static int moveCount;
    private static int lastProcessedMoveCount = 0;
    public static long startHash;
    private static boolean baseWhiteToMove = true; // side to move at start of current position

    // UCI time controls (milliseconds)
    private static long wtimeMs = -1;
    private static long btimeMs = -1;
    private static long wincMs = 0;
    private static long bincMs = 0;

    public static void main(String[] args) {
        // Initial engine state similar to LichessBotStream game start
        Board.setupBoard(Board.brett);
        MoveFinder.transpositionTable.clear();
        Zobrist.initZobrist();
        startHash = Zobrist.computeHash(Board.brett, true);
        baseWhiteToMove = true;
        moves = "";
        moveList = new String[0];
        moveCount = 0;
        lastProcessedMoveCount = 0;

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
                Board.setupBoard(Board.brett);
                MoveFinder.transpositionTable.clear();
                Zobrist.initZobrist();
                startHash = Zobrist.computeHash(Board.brett, true);
                baseWhiteToMove = true;
                lastProcessedMoveCount = 0;
                moves = "";
                moveList = new String[0];
                moveCount = 0;
            }
            else if (command.startsWith("position")) {
                // UCI 'position' provides a full position description. Reset incremental state.
                lastProcessedMoveCount = 0;
                moves = "";
                moveList = new String[0];
                moveCount = 0;

                if (command.contains("startpos")) {
                    // Start position
                    Board.setupBoard(Board.brett);
                    baseWhiteToMove = true;
                    // Do not clear TT here; use ucinewgame for a hard reset
                    startHash = Zobrist.computeHash(Board.brett, baseWhiteToMove);
                }

                // load FEN if given
                if (command.contains("fen")) {
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

                // process moves appended to the position
                if (command.contains("moves")) {
                    moves = command.substring(command.indexOf("moves") + 6).trim();
                    moveList = moves.isEmpty() ? new String[0] : moves.split(" ");
                    moveCount = moveList.length;

                    // apply the moves from the described root
                    for (int i = 0; i < moveList.length; i++) {
                        Zug zug = new Zug(moveList[i]);
                        MoveInfo info = MoveFinder.saveMoveInfo(zug, Board.brett);
                        startHash = MoveFinder.doMove(zug, Board.brett, info, startHash);
                    }

                    lastProcessedMoveCount = moveList.length;
                }
            }
            else if (command.startsWith("go")) {
                // Parse UCI time parameters (milliseconds)
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

                // Determine side to move: start side flipped by number of moves applied
                boolean whiteToMove = (moveCount % 2 == 0) ? baseWhiteToMove : !baseWhiteToMove;

                // Try opening move first, else search
                try {
                    Zug opening = OpeningDictionary.getNextOpeningMove(moves == null ? "" : moves);
                    if (opening != null) {
                        System.out.println("bestmove " + opening.processZug());
                    } else {
                        long timeLeft = whiteToMove ? wtimeMs : btimeMs;
                        long inc = whiteToMove ? wincMs : bincMs;
                        long thinkMs = TimeManager.computeThinkTimeMs(Board.brett, whiteToMove, timeLeft, inc, moveCount);
                        Zug best = MoveFinder.iterativeDeepening(Board.brett, whiteToMove, startHash, thinkMs);
                        if (best == null) {
                            // Fallback if no move found (should not happen)
                            best = MoveFinder.iterativeDeepening(Board.brett, whiteToMove, startHash);
                        }
                        System.out.println("bestmove " + best.processZug());
                    }
                } catch (IOException e) {
                    // If opening book unavailable, fall back to timed search
                    long timeLeft = whiteToMove ? wtimeMs : btimeMs;
                    long inc = whiteToMove ? wincMs : bincMs;
                    long thinkMs = TimeManager.computeThinkTimeMs(Board.brett, whiteToMove, timeLeft, inc, moveCount);
                    Zug best = MoveFinder.iterativeDeepening(Board.brett, whiteToMove, startHash, thinkMs);
                    if (best == null) {
                        best = MoveFinder.iterativeDeepening(Board.brett, whiteToMove, startHash);
                    }
                    System.out.println("bestmove " + best.processZug());
                }
            }
            else if (command.equals("quit")) {
                break;
            }
        }
    }
}
