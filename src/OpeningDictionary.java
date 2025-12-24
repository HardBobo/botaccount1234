import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class OpeningDictionary {
    private static final Config config = Config.getInstance();

    // Cached openings to avoid re-reading the file on every move (and to avoid leaking file handles)
    private static volatile List<String> openings = null;
    private static volatile String loadedPath = null;

    private static synchronized void ensureLoaded() throws IOException {
        String path = config.getOpeningDatabasePath();
        if (openings != null && path != null && path.equals(loadedPath)) return;

        File openingData = new File(path);
        ArrayList<String> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(openingData))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) list.add(line);
            }
        }
        openings = list;
        loadedPath = path;
        System.out.println("[Book] Loaded " + list.size() + " lines from " + path);
    }

    public static Zug getNextOpeningMove(String playedMoves) throws IOException {
        ensureLoaded();
        if (openings == null || openings.isEmpty()) return null;

        String prefix = (playedMoves == null) ? "" : playedMoves.trim();
        String startsWith = prefix.isEmpty() ? "" : (prefix + " ");

        List<String> candidates = new ArrayList<>();
        for (String opening : openings) {
            if (prefix.isEmpty()) {
                candidates.add(opening);
            } else if (opening.equals(prefix) || opening.startsWith(startsWith)) {
                candidates.add(opening);
            }
        }

        if (candidates.isEmpty()) return null;

        String chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        String[] moves = chosen.trim().split("\\s+");
        int playedCount = prefix.isEmpty() ? 0 : prefix.split("\\s+").length;

        if (moves.length > playedCount) {
            return new Zug(moves[playedCount]);
        }
        return null;
    }
}
