import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class LichessBotStream {
    private static final String TOKEN = "lip_rB8r5GZFsYlOr3sBcXcc"; //api token
    private static boolean isWhite = true; //sind wir weiß?
    private static final HttpClient client = HttpClient.newHttpClient(); //http request
    static String [] moveList; //alle biherigen Züge
    static String moves; //alle bisherigen züge aber in einem string
    static int moveCount; //um herauszufinden, wer dran ist
    private static int lastProcessedMoveCount = 0; //für gui update und game state update
    static Spiel spiel = new Spiel();
    private static Piece [][] temp;

    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lichess.org/api/stream/event"))
                .header("Authorization", "Bearer " + TOKEN)
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
                            //System.out.println("Challenge erhalten");
                            JSONObject challenge = obj.getJSONObject("challenge"); // challengeid auslesen
                            String challengeId = challenge.getString("id");
                            //System.out.println("Challenge-ID: " + challengeId);
                            acceptChallenge(challengeId);
                        }
                        if("gameStart".equals(type)) { //wenn spielstart in gamestream übergehen
                            isWhite = obj.getJSONObject("game").getString("color").equals("white"); // welche farbe bin ich
                            String gameId = obj.getJSONObject("game").getString("gameId");// gameid auslesen
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
                .header("Authorization", "Bearer " + TOKEN)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(resp -> System.out.println("Challenge angenommen: " + challengeId));
    }
    public static void startGameStream(String gameId) { // gamestream (wichtig)
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://lichess.org/api/bot/game/stream/" + gameId))
                .header("Authorization", "Bearer " + TOKEN)
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
                                    if(isWhite)
                                        playMove(gameId, Objects.requireNonNull(OpeningDictionary.getNextOpeningMove("")).processZug());
                                } else if ("gameState".equals(type)) { // normaler gamestate
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
                                        spiel = new Spiel();
                                    }
                                    else { // normale züge
                                        moves = event.getString("moves"); // bisherige Züge geuptdated
                                        moveList = moves.isEmpty() ? new String[0] : moves.split(" "); // array auch
                                        moveCount = moveList.length; // auch anzahl
                                        if (moveCount > lastProcessedMoveCount) { // stellungsupdate
                                            for (int i = lastProcessedMoveCount; i < moveList.length; i++) {
                                                //System.out.println("Neuer Zug erkannt: " + moveList[i]);
                                                spiel.playMove(new Zug(moveList[i]));
                                            }
                                            lastProcessedMoveCount = moveList.length;
                                        }
                                        if (isWhite) {
                                            if (isMyTurn(moves, "white")) {
                                                Zug zug = OpeningDictionary.getNextOpeningMove(moves);
                                                if(zug != null)
                                                    playMove(gameId, Objects.requireNonNull(OpeningDictionary.getNextOpeningMove(moves)).processZug());
                                                else {
                                                    temp = Board.copy(Board.brett);
                                                    playMove(gameId, MoveFinder.iterativeDeepening(temp,true).processZug());
                                                }
                                            }
                                        } else {
                                            if (isMyTurn(moves, "black")) {
                                                Zug zug = OpeningDictionary.getNextOpeningMove(moves);
                                                if(zug != null)
                                                    playMove(gameId, Objects.requireNonNull(OpeningDictionary.getNextOpeningMove(moves)).processZug());
                                                else {
                                                    temp = Board.copy(Board.brett);
                                                    playMove(gameId, MoveFinder.iterativeDeepening(temp, false).processZug());
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
                .header("Authorization", "Bearer " + TOKEN)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        //System.out.println("Zug gesendet: " + move);
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
}
