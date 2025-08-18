import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DatabankReader {
    static File inputFile = new File("C:\\Users\\hardb\\Downloads\\games201412.uci");
    static File outputFile = new File("C:\\Users\\hardb\\Downloads\\gm_games_real2.txt");

    public static void main(String[] args) {
        List<GmGame> spiele = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            int eloW = 0;
            int eloB = 0;
            String moves = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("[WhiteElo")) {
                    eloW = parseElo(line);
                } else if (line.startsWith("[BlackElo")) {
                    eloB = parseElo(line);
                } else if (!line.isEmpty() && !line.startsWith("[")) {
                    // moves line
                    moves = line;

                    // hier haben wir ein komplettes Spiel
                    if (eloW >= 2300 && eloB >= 2300) {
                        spiele.add(new GmGame(eloW, eloB, moves));
                    }

                    // reset für nächstes Spiel
                    eloW = 0;
                    eloB = 0;
                    moves = null;
                }
            }
            reader.close();

            // Spiele ins Ziel schreiben
            for (GmGame g : spiele) {
                writer.write(g.moves);
                writer.newLine();
            }
            writer.flush();
            writer.close();

            System.out.println("Fertig! Anzahl gespeicherter Spiele: " + spiele.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int parseElo(String line) {
        // Beispiel: [WhiteElo "1525"]
        int start = line.indexOf("\"") + 1;
        int end = line.lastIndexOf("\"");
        if(!line.substring(start, end).equals("?"))
            return Integer.parseInt(line.substring(start, end));
        return 0;
    }
}