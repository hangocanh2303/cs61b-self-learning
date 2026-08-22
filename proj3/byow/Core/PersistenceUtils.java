package byow.Core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class PersistenceUtils {
    private static final File SAVE_FILE = Paths.get("savefile.txt").toFile();

    public static void saveHistory(String history) {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(SAVE_FILE), StandardCharsets.UTF_8))) {
            out.print(history);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadHistory() {
        if (!SAVE_FILE.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(SAVE_FILE), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
