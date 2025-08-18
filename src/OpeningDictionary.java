import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.*;
public class OpeningDictionary {
    private static final File openingData = new File("C:\\Users\\hardb\\Desktop\\Schach2\\openingdatabank\\gm_games5moves.txt");
    public static List<String> openings;
    public static void readData() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(openingData));
        String line;
        openings = new ArrayList<>();
        while((line = reader.readLine()) != null){
            openings.add(line);
        }
    }
    public static Zug getNextOpeningMove(String playedMoves) throws IOException {
        readData();
        List<String> candidates = new ArrayList<>();

        for(String opening : openings){
            if(opening.startsWith(playedMoves))
                candidates.add(opening);
        }

        if (candidates.isEmpty()) {
            return null; //opening zuende oder kein passendes gefunden
        }

        Random rand = new Random();
        String chosen = candidates.get(rand.nextInt(candidates.size()));
        String[] moves = chosen.split(" ");
        int playedCount = playedMoves.isEmpty() ? 0 : playedMoves.split(" ").length;

        if (moves.length > playedCount) {
            return new Zug(moves[playedCount]); // nächster Zug
        } else {
            return null; // opening zu Ende
        }
    }
}
