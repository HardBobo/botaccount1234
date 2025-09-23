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

    public static void main(String[] args) throws IOException, InterruptedException {
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
                            // Zeitkontrolle aus Challenge extrahieren (falls vorhanden)
                            try {
                                JSONObject challenge = obj.getJSONObject("challenge"); // challengeid auslesen
                                if (challenge.has("timeControl")) {
                                    JSONObject tc = challenge.getJSONObject("timeControl");
                                    // limit ist Basiszeit in Sekunden, increment in Sekunden
                                    pendingBaseTimeSeconds = tc.optInt("limit", -1);
                                    pendingIncrementSeconds = tc.optInt("increment", 0);
                                    System.out.println("[TC] Challenge TimeControl: base=" + pendingBaseTimeSeconds + "s, inc=" + pendingIncrementSeconds + "s");
                                }
                                String challengeId = challenge.getString("id");
                                acceptChallenge(challengeId);
                            } catch (Exception e) {
                                System.err.println("Fehler beim Lesen der Zeitkontrolle aus Challenge: " + e.getMessage());
                            }
                        }
if("gameStart".equals(type)) { //wenn spielstart in gamestream übergehen
                            isWhite = obj.getJSONObject("game").getString("color").equals("white"); // welche farbe bin ich
                            String gameId = obj.getJSONObject("game").getString("gameId");// gameid auslesen
                            // Falls vorher aus Challenge bekannt, übernehme Zeitkontrolle schon jetzt
                            if (pendingBaseTimeSeconds != null) {
                                baseTimeSeconds = pendingBaseTimeSeconds;
                                incrementSeconds = pendingIncrementSeconds != null ? pendingIncrementSeconds : 0;
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

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(resp -> System.out.println("Challenge angenommen: " + challengeId));
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
                                        if (event.has("clock")) {
                                            JSONObject clock = event.getJSONObject("clock");
                                            // Lichess liefert hier i.d.R. Sekundenwerte
                                            baseTimeSeconds = clock.optInt("initial", baseTimeSeconds);
                                            incrementSeconds = clock.optInt("increment", incrementSeconds);
                                            System.out.println("[TC] GameFull TimeControl: base=" + baseTimeSeconds + "s, inc=" + incrementSeconds + "s");
                                        }
                                        // Aktuelle Zeiten aus dem state (Millisekunden)
                                        if (event.has("state")) {
                                            JSONObject state = event.getJSONObject("state");
                                            whiteTimeMs = state.optLong("wtime", whiteTimeMs);
                                            blackTimeMs = state.optLong("btime", blackTimeMs);
                                            System.out.println("[TC] GameFull State Times: wtime=" + whiteTimeMs + "ms, btime=" + blackTimeMs + "ms");
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Fehler beim Lesen der Zeitkontrolle aus gameFull: " + e.getMessage());
                                    }

                                    Board.setupBoard(Board.brett);
                                    MoveFinder.transpositionTable.clear();
                                    Zobrist.initZobrist();
                                    startHash = Zobrist.computeHash(Board.brett, true);
                                    if(isWhite) {
                                        Zug zug = Objects.requireNonNull(OpeningDictionary.getNextOpeningMove(""));
                                        playMove(gameId, zug.processZug());
                                        MoveInfo info = MoveFinder.saveMoveInfo(zug, Board.brett);
                                        startHash = MoveFinder.doMove(zug, Board.brett, info, startHash);
                                        lastProcessedMoveCount++;
                                    }
                                } else if ("gameState".equals(type)) { // normaler gamestate
                                    // Zeit pro Zug (Millisekunden) aus gameState
                                    try {
                                        if (event.has("wtime")) whiteTimeMs = event.getLong("wtime");
                                        if (event.has("btime")) blackTimeMs = event.getLong("btime");
                                    } catch (Exception ignored) {}

                                    if(event.getString("status").equals("mate")
                                            || event.getString("status").equals("stalemate")
                                            || event.getString("status").equals("resign")
                                            || event.getString("status").equals("draw")
                                            || event.getString("status").equals("outoftime")
                                            || event.getString("status").equals("aborted")){
                                        // finish statement statt gamefinish
                                        //System.out.println("SCHACHMATT");
                                        lastProcessedMoveCount = 0;
                                        moves = "";
                                        moveList = new String [0];
                                        // Zeitkontrolle & Zeiten zurücksetzen
                                        baseTimeSeconds = -1;
                                        incrementSeconds = 0;
                                        pendingBaseTimeSeconds = null;
                                        pendingIncrementSeconds = null;
                                        whiteTimeMs = -1;
                                        blackTimeMs = -1;

                                        Board.setupBoard(Board.brett);
                                        MoveFinder.transpositionTable.clear();
                                        Zobrist.initZobrist();
                                        startHash = Zobrist.computeHash(Board.brett, true);
                                    }
                                    else { // normale züge
                                        moves = event.getString("moves"); // bisherige Züge geuptdated
                                        moveList = moves.isEmpty() ? new String[0] : moves.split(" "); // array auch
                                        moveCount = moveList.length; // auch anzahl
                                        if (moveCount > lastProcessedMoveCount) { // stellungsupdate
                                            Zug zug;
                                            for (int i = lastProcessedMoveCount; i < moveList.length; i++) {
                                                //System.out.println("Neuer Zug erkannt: " + moveList[i]);
                                                zug = new Zug(moveList[i]);
                                                MoveInfo info = MoveFinder.saveMoveInfo(zug, Board.brett);
                                                startHash = MoveFinder.doMove(new Zug(moveList[i]), Board.brett, info, startHash);
                                            }
                                            lastProcessedMoveCount = moveList.length;
                                        }
                                        if (isWhite) {
                                            if (isMyTurn(moves, "white")) {
                                                Zug zug = OpeningDictionary.getNextOpeningMove(moves);
                                                if(zug != null){
                                                    playMove(gameId, zug.processZug());
                                                }
                                                else {
                                                    playMove(gameId, MoveFinder.iterativeDeepening(Board.brett,true, startHash).processZug());
                                                }
                                            }
                                        } else {
                                            if (isMyTurn(moves, "black")) {
                                                Zug zug = OpeningDictionary.getNextOpeningMove(moves);
                                                if(zug != null){
                                                    playMove(gameId, zug.processZug());
                                                }
                                                else {
                                                    playMove(gameId, MoveFinder.iterativeDeepening(Board.brett, false, startHash).processZug());
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
        int moveCount = moves.split(" ").length;
        return ("white".equals(myColor) && moveCount % 2 == 0)
                || ("black".equals(myColor) && moveCount % 2 == 1);
    }

    // Getter für Zeitkontrolle
    public static int getBaseTimeSeconds() {
        return baseTimeSeconds;
    }

    public static int getIncrementSeconds() {
        return incrementSeconds;
    }

    // Getter für verbleibende Zeiten in Millisekunden
    public static long getWhiteTimeMs() {
        return whiteTimeMs;
    }
    public static long getBlackTimeMs() {
        return blackTimeMs;
    }
}
