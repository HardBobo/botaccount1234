import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LichessBotStream {
    private static final Config config = Config.getInstance();
    private static boolean isWhite = true; //sind wir weiß?
    private static final HttpClient client = HttpClient.newHttpClient(); //http request
    static String [] moveList; //alle biherigen Züge
    static String moves; //alle bisherigen züge aber in einem string
    static int moveCount; //um herauszufinden, wer dran ist
    private static int lastProcessedMoveCount = 0; //für gui update und game state update
    public static long startHash;

    // Zeitkontrolle (Basiszeit in Sekunden und Inkrement in Sekunden)
    private static int baseTimeSeconds = -1;
    private static int incrementSeconds = 0;
    // Zwischenspeicher aus Challenge-Event, falls Game-Stream keine Clock liefert
    private static Integer pendingBaseTimeSeconds = null;
    private static Integer pendingIncrementSeconds = null;

    // Verbleibende Zeiten (Millisekunden) – aktualisiert pro Zug aus dem Game-Stream
    private static long whiteTimeMs = -1;
    private static long blackTimeMs = -1;
    // Inkremente in Millisekunden, falls vom Game-Stream geliefert (winc/binc); fallback: incrementSeconds*1000
    private static long whiteIncMs = 0;
    private static long blackIncMs = 0;

    public static void main(String[] args) {
        // Validate configuration before starting
        config.validateConfiguration();
        
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lichess.org/api/stream/event"))
                .header("Authorization", "Bearer " + config.getLichessToken())
                .build();

        CompletableFuture<Void> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    response.body().forEach(line -> {
                        System.out.println("Raw line: " + line);
                        String json = null;
                        JSONObject obj = null;
                        String type = null;
                        if (line != null) { // erhaltene updates nach challenges überprüfen
                            json = line;
                            if (!json.isEmpty()) {
                                try {
                                    obj = new JSONObject(json);
                                    System.out.println("Game-Event: " + obj.toString(2));
                                    type = obj.getString("type");
                                } catch (Exception e) {
                                    System.err.println("Ungültiges JSON: " + json);
                                    e.printStackTrace();
                                }
                            }
                        }
                        System.out.println("Gefundener Typ: " + type);
if ("challenge".equals(type)) { //challenge gefunden → automatisch annehmen
                            try {
                                JSONObject challenge = obj.getJSONObject("challenge"); // challengeid auslesen
                                String challengeId = challenge.getString("id");

                                // Lichess tells us if the challenge is compatible with bots.
                                // If compat.bot is false, the API will reject accept requests.
                                boolean botCompat = true;
                                try {
                                    if (obj.has("compat")) {
                                        botCompat = obj.getJSONObject("compat").optBoolean("bot", true);
                                    }
                                } catch (Exception ignored) {
                                }
                                if (!botCompat) {
                                    System.out.println("[Challenge] Not bot-compatible; declining challenge " + challengeId);
                                    declineChallenge(challengeId);
                                    return;
                                }

                                // Zeitkontrolle aus Challenge extrahieren (falls vorhanden)
                                if (challenge.has("timeControl")) {
                                    JSONObject tc = challenge.getJSONObject("timeControl");
                                    // limit ist Basiszeit in Sekunden, increment in Sekunden
                                    pendingBaseTimeSeconds = tc.optInt("limit", -1);
                                    pendingIncrementSeconds = tc.optInt("increment", 0);
                                    System.out.println("[TC] Challenge TimeControl: base=" + pendingBaseTimeSeconds + "s, inc=" + pendingIncrementSeconds + "s");
                                }

                                acceptChallenge(challengeId);
                            } catch (Exception e) {
                                System.err.println("Fehler beim Lesen/Annehmen der Challenge: " + e.getMessage());
                            }
                        }
if("gameStart".equals(type)) { //wenn spielstart in gamestream übergehen
                            isWhite = obj.getJSONObject("game").getString("color").equals("white"); // welche farbe bin ich
                            String gameId = obj.getJSONObject("game").getString("gameId");// gameid auslesen
                            // Falls vorher aus Challenge bekannt, übernehme Zeitkontrolle schon jetzt
                            if (pendingBaseTimeSeconds != null) {
                                baseTimeSeconds = pendingBaseTimeSeconds;
                                incrementSeconds = pendingIncrementSeconds != null ? pendingIncrementSeconds : 0;
                                whiteIncMs = (long) incrementSeconds * 1000L;
                                blackIncMs = (long) incrementSeconds * 1000L;
                                System.out.println("[TC] Set from pending (challenge): base=" + baseTimeSeconds + "s, inc=" + incrementSeconds + "s");
                            } else {
                                // Korrenspondenz/Unbekannt -> erstmal -1/0, wird ggf. im Game-Stream (gameFull) überschrieben
                                baseTimeSeconds = -1;
                                incrementSeconds = 0;
                            }
                            // secondsLeft im gameStart (in Sekunden) für unsere Seite, wenn vorhanden
                            try {
                                if (obj.getJSONObject("game").has("secondsLeft")) {
                                    int secLeft = obj.getJSONObject("game").getInt("secondsLeft");
                                    if (isWhite) whiteTimeMs = secLeft * 1000L; else blackTimeMs = secLeft * 1000L;
                                    System.out.println("[TC] secondsLeft on start: " + secLeft + "s for " + (isWhite ? "white" : "black"));
                                }
                            } catch (Exception e) {
                                // ignore
                            }
                            startGameStream(gameId);
                        }
                    });
                });
        future.join();
    }
    private static void acceptChallenge(String challengeId) { // sendet http zum Akzeptieren der challenge
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lichess.org/api/challenge/" + challengeId + "/accept"))
                .header("Authorization", "Bearer " + config.getLichessToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() == 200) {
                        System.out.println("Challenge angenommen: " + challengeId);
                    } else {
                        System.err.println("Challenge accept failed (" + resp.statusCode() + ") for " + challengeId);
                        System.err.println(resp.body());
                    }
                });
    }

    private static void declineChallenge(String challengeId) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lichess.org/api/challenge/" + challengeId + "/decline"))
                .header("Authorization", "Bearer " + config.getLichessToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() == 200) {
                        System.out.println("Challenge declined: " + challengeId);
                    } else {
                        System.err.println("Challenge decline failed (" + resp.statusCode() + ") for " + challengeId);
                        System.err.println(resp.body());
                    }
                });
    }
    public static void startGameStream(String gameId) { // gamestream (wichtig)
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lichess.org/api/bot/game/stream/" + gameId))
                .header("Authorization", "Bearer " + config.getLichessToken())
                .GET()
                .build();

        CompletableFuture<Void> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    response.body().forEach(line -> {
                        if (line != null && line.startsWith("data: ")) {
                            line = line.substring(6).trim();
                        }
                        if (!line.isEmpty()) {
                            try {
                                JSONObject event = new JSONObject(line);
                                //System.out.println("Game-Event: " + event.toString(2));

                                String type = event.getString("type");
                                //System.out.println("Gefundener Typ: " + type);
                                if ("gameFinish".equals(type)) { // gamefinish ist kein echter state der letzte echte state ist gamestate
                                    // mit status mate stalemate resign draw oder outoftime | angebot zu unentschieden wird immer ignoriert
                                }
else if ("gameFull".equals(type)) { // erster state nach gamestart
                                    // Zeitkontrolle aus gameFull lesen, falls vorhanden
                                    try {
                                        Nnue.tryAutoLoad();
                                    } catch (Throwable t) {
                                        System.err.println("NNUE autoload failed: " + t.getMessage());
                                    }

                                    try {
                                        if (event.has("clock")) {
                                            JSONObject clock = event.getJSONObject("clock");
                                            // Lichess liefert hier i.d.R. Sekundenwerte (initial/increment)
                                            baseTimeSeconds = clock.optInt("initial", baseTimeSeconds);
                                            incrementSeconds = clock.optInt("increment", incrementSeconds);
                                            // Falls state-incs nicht kommen, nutze diese als Fallback
                                            whiteIncMs = (long) incrementSeconds * 1000L;
                                            blackIncMs = (long) incrementSeconds * 1000L;
                                            System.out.println("[TC] GameFull TimeControl: base=" + baseTimeSeconds + "s, inc=" + incrementSeconds + "s");
                                        }
                                        // Aktuelle Zeiten aus dem state; laut API in Millisekunden
                                        if (event.has("state")) {
                                            JSONObject state = event.getJSONObject("state");
                                            long wt = state.optLong("wtime", whiteTimeMs);
                                            long bt = state.optLong("btime", blackTimeMs);
                                            // Lichess liefert wtime/btime in Millisekunden. Keine weitere Normalisierung!
                                            whiteTimeMs = wt;
                                            blackTimeMs = bt;
                                            // Inkremente ggf. aus state (Millisekunden)
                                            whiteIncMs = state.optLong("winc", whiteIncMs);
                                            blackIncMs = state.optLong("binc", blackIncMs);
                                            System.out.println("[TC] GameFull State Times: wtime=" + whiteTimeMs + "ms, btime=" + blackTimeMs + "ms, winc=" + whiteIncMs + "ms, binc=" + blackIncMs + "ms");
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Fehler beim Lesen der Zeitkontrolle aus gameFull: " + e.getMessage());
                                    }
                                    Spiel.newGame();
                                    if(isWhite) {
                                        doFirstMove(gameId);
                                    }
                                } else if ("gameState".equals(type)) { // normaler gamestate
                                    // Zeit pro Zug (Millisekunden) aus gameState (wtime/btime), ggf. winc/binc
                                    try {
                                        if (event.has("wtime")) whiteTimeMs = event.getLong("wtime");
                                        if (event.has("btime")) blackTimeMs = event.getLong("btime");
                                        if (event.has("winc")) whiteIncMs = event.getLong("winc");
                                        if (event.has("binc")) blackIncMs = event.getLong("binc");
                                    } catch (Exception ignored) {}

                                    if(event.getString("status").equals("mate")
                                            || event.getString("status").equals("stalemate")
                                            || event.getString("status").equals("resign")
                                            || event.getString("status").equals("draw")
                                            || event.getString("status").equals("outoftime")
                                            || event.getString("status").equals("aborted")){

                                        resetTimeControl();
                                        Spiel.newGame();
                                    }
                                    else { // normale züge

                                        syncLichessToBoard(event);

                                        if (isWhite) {
                                            if (isMyTurn(moves, "white")) {
                                                if(playOpeningMove(gameId)){

                                                }
                                                else {
                                                    long timeLeft = whiteTimeMs; // tracked from stream in ms
                                                    long incMs = whiteIncMs > 0 ? whiteIncMs : Math.max(0, incrementSeconds) * 1000L;

                                                    Zug best;

                                                    long thinkMs = TimeManager.computeThinkTimeMs(timeLeft, incMs);
                                                    best = MoveFinder.iterativeDeepening(true, startHash, thinkMs);

                                                    playMove(gameId, best.processZug());
                                                }
                                            }
                                        } else {
                                            if (isMyTurn(moves, "black")) {
                                                if(playOpeningMove(gameId)){

                                                }
                                                else {
                                                    long timeLeft = blackTimeMs; // tracked from stream in ms
                                                    long incMs = blackIncMs > 0 ? blackIncMs : Math.max(0, incrementSeconds) * 1000L;

                                                    Zug best;

                                                    long thinkMs = TimeManager.computeThinkTimeMs(timeLeft, incMs);
                                                    best = MoveFinder.iterativeDeepening(false, startHash, thinkMs);

                                                    playMove(gameId, best.processZug());
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Fehler beim Parsen des Spielstreams: " + line);
                                e.printStackTrace();
                            }
                        }
                    });
                });
        future.join();
    }
    public static void playMove(String gameId, String move) { // http methode zum senden des geplanten zugs
        String url = "https://lichess.org/api/bot/game/" + gameId + "/move/" + move;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + config.getLichessToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        System.out.println("Zug gesendet: " + move);
                    } else {
                        System.err.println("Fehler beim Zug: " + response.statusCode());
                        System.err.println(response.body());
                    }
                });
    }

    public static boolean isMyTurn(String moves, String myColor) { // selbsterklärend
        if (moves == null || moves.isEmpty()) {
            return "white".equals(myColor);
        }
        int moveCount = moves.trim().split("\\s+").length;
        return ("white".equals(myColor) && moveCount % 2 == 0)
                || ("black".equals(myColor) && moveCount % 2 == 1);
    }

    private static void doFirstMove(String gameId) {
        Zug zug = null;
        try {
            zug = OpeningDictionary.getNextOpeningMove("");
        } catch (Exception e) {
            System.err.println("[Book] Failed to read opening book for first move: " + e.getMessage());
        }

        if (zug == null) {
            // Fallback to a quick search if the opening book is unavailable.
            long timeLeft = (whiteTimeMs > 0) ? whiteTimeMs : (long) Math.max(0, baseTimeSeconds) * 1000L;
            long incMs = (whiteIncMs > 0) ? whiteIncMs : Math.max(0, incrementSeconds) * 1000L;
            long thinkMs = TimeManager.computeThinkTimeMs(timeLeft, incMs);
            zug = MoveFinder.iterativeDeepening(true, startHash, thinkMs);
        }

        if (zug == null) {
            System.err.println("No move found for first move (no legal moves?)");
            return;
        }

        playMove(gameId, zug.processZug());
        MoveInfo info = MoveFinder.saveMoveInfo(zug);
        startHash = MoveFinder.doMoveUpdateHash(zug, info, startHash);
        lastProcessedMoveCount++;
    }

    private static void resetTimeControl(){
        lastProcessedMoveCount = 0;
        moves = "";
        moveList = new String [0];
        baseTimeSeconds = -1;
        incrementSeconds = 0;
        pendingBaseTimeSeconds = null;
        pendingIncrementSeconds = null;
        whiteTimeMs = -1;
        blackTimeMs = -1;
    }

    private static void syncLichessToBoard(JSONObject event){
        moves = event.getString("moves"); // bisherige Züge geuptdated
        moveList = moves.isEmpty() ? new String[0] : moves.trim().split("\\s+"); // array auch
        moveCount = moveList.length; // auch anzahl
        if (moveCount > lastProcessedMoveCount) { // stellungsupdate
            Zug zug;
            for (int i = lastProcessedMoveCount; i < moveList.length; i++) {
                String raw = moveList[i];
                if (raw == null || raw.isEmpty()) continue;
                String norm = normalizeCastleUci(raw);
                if (!raw.equals(norm)) {
                    System.out.println("[Sync] Normalized castling move: " + raw + " -> " + norm);
                }
                zug = new Zug(norm);
                MoveInfo info = MoveFinder.saveMoveInfo(zug);
                startHash = MoveFinder.doMoveUpdateHash(new Zug(norm), info, startHash);
            }
            lastProcessedMoveCount = moveList.length;
        }
    }

    // Normalize non-standard castling like e8h8/e1a1 into e8g8/e1c1,
    // but ONLY if the starting square currently holds a king.
    private static String normalizeCastleUci(String lan) {
        if (lan == null || lan.length() < 4) return lan;
        char sFile = lan.charAt(0);
        char sRank = lan.charAt(1);
        char eFile = lan.charAt(2);
        char eRank = lan.charAt(3);
        // Only consider potential castle if: starts from e-file, same rank, ends on rook file, and piece is actually a king
        if (sFile == 'e' && (sRank == '1' || sRank == '8') && eRank == sRank && (eFile == 'h' || eFile == 'a')) {
            int startX = sFile - 'a'; // 0..7
            int startY = 8 - Character.getNumericValue(sRank); // rank '1' -> 7, '8' -> 0
            int sq = startY * 8 + startX;
            boolean isWhite = (Board.bitboards.occW & (1L << sq)) != 0L;
            int type = Board.bitboards.pieceTypeAt(sq, isWhite);
            if (type == 5) {
                char newFile = (eFile == 'h') ? 'g' : 'c';
                String prefix = "" + sFile + sRank + newFile + eRank;
                return (lan.length() > 4) ? prefix + lan.substring(4) : prefix;
            }

        }
        return lan;
    }

    private static boolean playOpeningMove(String gameId) {
        try {
            Zug zug = OpeningDictionary.getNextOpeningMove(moves);
            if (zug != null) {
                playMove(gameId, zug.processZug());
                return true;
            }
        } catch (Exception e) {
            System.err.println("[Book] Failed to pick opening move: " + e.getMessage());
        }
        return false;
    }

    private static Zug panicBest(boolean whiteToMove){
        return MoveFinder.searchToDepth(whiteToMove, startHash, 2);
    }
    private static Zug ultraPanicBest(boolean whiteToMove){
        return MoveFinder.searchToDepth(whiteToMove, startHash, 1);
    }
}
