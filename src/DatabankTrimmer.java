import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DatabankTrimmer {
    static File inputFile = new File("C:\\Users\\hardb\\Downloads\\gm_games_real2.txt");
    static File outputFile = new File("C:\\Users\\hardb\\Downloads\\gm_games5moves2.txt");
    public static void main(String[] args) {

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            String [] moves;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                moves = line.split(" ");
                if(moves.length > 9){
                    for(int i = 0; i < 10; i++){
                        writer.write(moves[i]+ " ");
                    }
                    writer.newLine();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
