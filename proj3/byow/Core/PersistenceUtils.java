package byow.Core;

import java.io.*;
import java.nio.file.Paths;

public class PersistenceUtils {
    private static final File SAVE_FILE = Paths.get("savefile.txt").toFile();

    public static void saveGame(GameState state) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static GameState loadGame() {
        if (!SAVE_FILE.exists()) {
            return null; // Nếu chưa có file save, trả về null để Engine xử lý thoát an toàn [310]
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            return (GameState) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}