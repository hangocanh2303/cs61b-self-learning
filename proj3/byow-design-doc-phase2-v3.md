# BYOW Technical Design Document - Phase 2 (Comprehensive Full Solution)
**Tác giả:** Anh Ha
**Phiên bản:** v15 (Giải pháp thiết kế toàn vẹn & Hiện thực hóa 360 điểm Ambition)
**Khóa học:** CS 61B - UC Berkeley

---

## 1. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (UML & System Architecture)

Để đảm bảo tính linh hoạt, khả năng bảo trì và vượt qua các bài kiểm thử tự động (Autograder) của Gradescope, cấu trúc Phase 2 của dự án BYOW được tổ chức dựa trên hai nguyên lý cốt lõi của Kỹ nghệ Phần mềm: **Đóng gói Trạng thái (State Encapsulation)** và **Đa hình Kiểu phụ (Subtype Polymorphism)** [28.3].

```
                     +──────────────────────+
                     |      Core.Main       |
                     +──────────────────────+
                                │ (khởi chạy)
                                v
                     +──────────────────────+
                     |     Core.Engine      | <─── (Điều phối luồng GUI/Headless)
                     +──────────────────────+
                                │
         ┌──────────────────────┴──────────────────────┐
         ▼ (Keyboard Mode)                             ▼ (String Mode / Autograder)
  interactWithKeyboard()                        interactWithInputString()
         │                                             │
         │ (sử dụng)                                   │ (sử dụng)
         v                                             v
+──────────────────+                            +──────────────────+
|  KeyboardSource  |                            |   StringSource   |
+──────────────────+                            +──────────────────+
         │                                             │
         └──────────────────────┬──────────────────────┘
                                v (đọc kí tự: W, A, S, D, L, :Q)
                     +──────────────────────+
                     |    Core.GameState    | <─── (Serializable - Lưu giữ toàn trạng)
                     +──────────────────────+
                        │              │
                        ├─ TETile[][]  ├─ Position avatarPos
                        ├─ Random rng  ├─ int score, health
                        └─ StringBuilder inputHistory
```

### Các lớp Thực thể & Vai trò Kiến trúc:
1.  **`Engine.java`**: Đóng vai trò là cổng giao tiếp ngoài cùng. Nó chịu trách nhiệm phân tích tham số dòng lệnh, điều hướng giữa chế độ GUI trực quan (`interactWithKeyboard`) và chế độ kiểm thử ngầm định (`interactWithInputString`) [299].
2.  **`GameState.java`**: Mô-đun sâu nhất (Deep Module) chịu trách nhiệm vận hành mọi quy tắc vật lý và cơ chế tương tác [28.3]. Lớp này đóng gói mảng gạch thế giới, tọa độ người chơi, trạng thái chỉ số (máu, điểm số) và bộ sinh số giả ngẫu nhiên `Random` [314, 327].
3.  **`InputSource.java` (Interface)**: Trừu tượng hóa cách thức thu thập các lệnh bấm phím, giúp che giấu nguồn gốc dữ liệu (từ bàn phím vật lý hoặc từ một chuỗi chuỗi tĩnh) [340].

---

## 2. Interface Đa hình Đầu vào (Polymorphic Input System)

Để bảo đảm **Tính tất định (Determinism)** của thế giới game—nơi mà cùng một chuỗi hành động bấm phím phải tạo ra kết quả giống hệt nhau trên mọi môi trường chạy—chúng ta triển khai interface đa hình `InputSource` [314]:

```java
package byow.Core;

public interface InputSource {
    char getNextKey();
    boolean hasNextKey();
}
```

### 2.1 Lớp `KeyboardSource` (Hiện thực không chặn - Non-blocking HUD)
Trong chế độ GUI trực quan, chúng ta tuyệt đối không được sử dụng cơ chế đọc chặn (blocking) vì nó sẽ làm đóng băng toàn bộ luồng cập nhật HUD khi người chơi đứng im di chuột [340]. Chúng ta sử dụng kiểm tra không chặn qua `hasNextKeyTyped()`:

```java
package byow.Core;

import edu.princeton.cs.introcs.StdDraw;

public class KeyboardSource implements InputSource {
    @Override
    public char getNextKey() {
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                return Character.toUpperCase(StdDraw.nextKeyTyped());
            }
            StdDraw.pause(10); // Giảm tải CPU khi chờ đợi phím
        }
    }

    @Override
    public boolean hasNextKey() {
        return StdDraw.hasNextKeyTyped();
    }
}
```

### 2.2 Lớp `StringSource` (Phân tích chuỗi lệnh tĩnh học)
Sử dụng cho bộ chấm điểm autograder của Gradescope để chạy các chuỗi kiểm thử ngầm (headless) có dạng `"N999SDDD:Q"` hoặc `"LDDDD"` mà không vẽ màn hình [318]:

```java
package byow.Core;

public class StringSource implements InputSource {
    private final String input;
    private int index;

    public StringSource(String input) {
        this.input = input.toUpperCase();
        this.index = 0;
    }

    @Override
    public char getNextKey() {
        char c = input.charAt(index);
        index++;
        return c;
    }

    @Override
    public boolean hasNextKey() {
        return index < input.length();
    }
}
```

### 2.3 Lớp `RandomInputSource` (Mô phỏng Fuzz Testing tự động)
Dành cho việc chạy các bài kiểm thử độ chịu tải (Stress Test) nhằm phát hiện lỗi tràn biên hoặc rò rỉ bộ nhớ một cách tất định:

```java
package byow.Core;

import java.util.Random;

public class RandomInputSource implements InputSource {
    private final Random rng;

    public RandomInputSource(long seed) {
        this.rng = new Random(seed);
    }

    @Override
    public char getNextKey() {
        char[] moves = {'W', 'A', 'S', 'D'};
        return moves[rng.nextInt(moves.length)];
    }

    @Override
    public boolean hasNextKey() {
        return true;
    }
}
```

---

## 3. Hiện thực hóa GameState & Va chạm Vật lý Phòng ngự

Lớp `GameState` kế thừa `Serializable` để có thể đóng băng toàn bộ trạng thái vào ổ đĩa. Khi di chuyển Avatar, chúng ta áp dụng tư duy **Lập trình phòng thủ (Defensive Programming)** để ngăn chặn việc đi xuyên tường `Tileset.WALL` [1, 28.1]:

```java
package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import java.io.Serializable;
import java.util.Random;

public class GameState implements Serializable {
    private static final long serialVersionUID = 42L;

    private final TETile[][] world;
    private Position avatarPos;
    private final Random random;
    private final StringBuilder inputHistory;
    
    // Chỉ số người chơi (Phục vụ 90 điểm Ambition)
    private int score;
    private int health;
    private boolean losEnabled;

    public GameState(TETile[][] world, Position avatarPos, Random random) {
        this.world = world;
        this.avatarPos = avatarPos;
        this.random = random;
        this.inputHistory = new StringBuilder();
        this.score = 0;
        this.health = 100;
        this.losEnabled = true;
    }

    public TETile[][] getWorld() { return world; }
    public Position getAvatarPos() { return avatarPos; }
    public Random getRandom() { return random; }
    public String getInputHistory() { return inputHistory.toString(); }
    public int getScore() { return score; }
    public int getHealth() { return health; }
    public boolean isLosEnabled() { return losEnabled; }
    public void toggleLOS() { this.losEnabled = !this.losEnabled; }

    /**
     * Di chuyển nhân vật Avatar một cách phòng ngự.
     * Đảm bảo không ném ArrayIndexOutOfBoundsException và không đi qua tường.
     */
    public void moveAvatar(char key) {
        int dx = 0;
        int dy = 0;
        char direction = Character.toUpperCase(key);

        switch (direction) {
            case 'W': dy = 1; break;
            case 'S': dy = -1; break;
            case 'A': dx = -1; break;
            case 'D': dx = 1; break;
            default: return; // Bỏ qua phím lạ
        }

        int targetX = avatarPos.getX() + dx;
        int targetY = avatarPos.getY() + dy;

        // CHỐT CHẶN PHÒNG NGỰ: Kiểm tra ranh giới lưới mảng trước
        if (targetX >= 0 && targetX < MapGenerator.WIDTH && targetY >= 0 && targetY < MapGenerator.HEIGHT) {
            TETile targetTile = world[targetX][targetY];

            // Chỉ cho phép đi vào ô FLOOR hoặc các thực thể tương tác (Vật phẩm, Cổng)
            if (targetTile.equals(Tileset.FLOOR) || targetTile.character() == '🍎' || targetTile.character() == '$') {
                
                // Ghi nhận lịch sử di chuyển phục vụ Replay
                inputHistory.append(direction);

                // Xử lý ăn vật phẩm (Secondary Ambition Feature)
                if (targetTile.character() == '🍎') {
                    health = Math.min(100, health + 20); // Ăn táo hồi máu
                } else if (targetTile.character() == '$') {
                    score += 100; // Nhặt vàng tăng điểm
                }

                // Cập nhật vị trí trên lưới vật lý
                world[avatarPos.getX()][avatarPos.getY()] = Tileset.FLOOR; // Khôi phục sàn ở vị trí cũ
                avatarPos = new Position(targetX, targetY);
                world[targetX][targetY] = Tileset.AVATAR; // Ghi đè kí tự @ lên vị trí mới
            }
        }
    }
}
```

---

## 4. Quản lý Vòng lặp Game chính & Hiển thị HUD Không chặn

Để đảm bảo tính năng di chuột hover xem mô tả gạch luôn hoạt động mượt mà ngay cả khi người chơi không nhấn phím di chuyển, chúng ta tổ chức vòng lặp game chính theo cơ chế không chặn [340]:

```java
public void play() {
    TERenderer ter = new TERenderer();
    // Chừa ra 3 hàng phía trên làm thanh HUD hiển thị máu, điểm và mô tả ô gạch
    ter.initialize(MapGenerator.WIDTH, MapGenerator.HEIGHT + 3);

    GameState gameState = drawMainMenuAndGetState();
    InputSource input = new KeyboardSource();
    boolean preparingQuit = false;

    while (true) {
        // 1. KIỂM TRA PHÍM BẤM KHÔNG CHẶN
        if (input.hasNextKey()) {
            char key = input.getNextKey();

            if (preparingQuit) {
                if (key == 'Q') {
                    PersistenceUtils.saveGame(gameState);
                    System.exit(0); // Thoát game và lưu an toàn
                } else {
                    preparingQuit = false; // Hủy chuỗi thoát nếu bấm phím khác
                }
            } else if (key == ':') {
                preparingQuit = true; // Bật cờ chờ lưu game
            } else if (key == 'V') {
                gameState.toggleLOS(); // Phím tắt bật/tắt Line of Sight
            } else {
                gameState.moveAvatar(key);
            }
        }

        // 2. CẬP NHẬT HUD KHI DI CHUỘT
        int mouseX = (int) StdDraw.mouseX();
        int mouseY = (int) StdDraw.mouseY();
        String tileDescription = "Void";

        // Chỉ đọc mô tả nếu chuột nằm trong vùng ranh giới bản đồ thế giới
        if (mouseX >= 0 && mouseX < MapGenerator.WIDTH && mouseY >= 0 && mouseY < MapGenerator.HEIGHT) {
            tileDescription = gameState.getWorld()[mouseX][mouseY].description();
        }

        // Vẽ mảng gạch đã được lọc qua tầm nhìn sương mù (Line of Sight Frame)
        TETile[][] frameToRender = getRenderFrame(gameState);
        ter.renderFrame(frameToRender);

        // Vẽ thanh HUD đè lên hàng trống phía trên
        drawHUD(tileDescription, gameState);

        StdDraw.show();
        StdDraw.pause(10); // Khống chế FPS ổn định, tránh treo CPU
    }
}

private void drawHUD(String tileDesc, GameState state) {
    StdDraw.setPenColor(StdDraw.WHITE);
    StdDraw.textLeft(2, MapGenerator.HEIGHT + 1.5, "Health: " + state.getHealth() + "%");
    StdDraw.textLeft(20, MapGenerator.HEIGHT + 1.5, "Score: " + state.getScore());
    StdDraw.textRight(MapGenerator.WIDTH - 2, MapGenerator.HEIGHT + 1.5, "Tile: " + tileDesc);
    StdDraw.line(0, MapGenerator.HEIGHT + 0.5, MapGenerator.WIDTH, MapGenerator.HEIGHT + 0.5);
}
```

---

## 5. Cơ chế Lưu & Tải game Deterministic (RNG State Persistence)

Đặc tả yêu cầu trò chơi phải khôi phục lại thế giới giống hệt thời điểm trước khi thoát, bao gồm cả trạng thái của bộ sinh số ngẫu nhiên `Random` [317]. 

Do lớp `Random` trong thư viện chuẩn Java đã kế thừa `Serializable`, khi em serialize toàn bộ đối tượng `GameState` chứa trường `private final Random random`, **toàn bộ trạng thái hạt giống (internal seed state) của bộ phát sinh ngẫu nhiên sẽ được đóng băng nguyên vẹn** [317]. Khi tải lại, các phép gọi số ngẫu nhiên tiếp theo sẽ trả về chuỗi giả ngẫu nhiên tiếp tục cực kỳ đồng bộ [317]!

```java
package byow.Core;

import java.io.*;
import java.nio.file.Paths;

public class PersistenceUtils {
    // Đảm bảo đường dẫn tệp tin an toàn trên mọi hệ điều hành (OS-agnostic file join) [334, 335]
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
            System.exit(0); // Nếu chưa có file save, thoát an toàn không báo lỗi [317]
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            return (GameState) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
```

---

## 6. Hiện thực hóa trọn vẹn 360 điểm Ambition (Primary + Secondary)

Để giúp trò giành trọn vẹn **360 điểm tối đa của danh mục Ambition Score** một cách an toàn và nhẹ nhàng nhất, chúng ta kết hợp hai tính năng bổ trợ nhau:

### 6.1 Line of Sight (270 Điểm - Tính năng chính)
Sử dụng toán học Chebyshev rời rạc để vẽ một vùng không gian chiếu sáng hình vuông bán kính bằng `5` ô gạch bọc xung quanh người chơi [322]:

```java
private TETile[][] getRenderFrame(GameState state) {
    TETile[][] world = state.getWorld();
    if (!state.isLosEnabled()) {
        return world; // Trả về thế giới đầy đủ nếu tắt sương mù
    }

    TETile[][] frame = new TETile[MapGenerator.WIDTH][MapGenerator.HEIGHT];
    Position player = state.getAvatarPos();
    int radius = 5; // Bán kính tầm nhìn sương mù

    for (int x = 0; x < MapGenerator.WIDTH; x++) {
        for (int y = 0; y < MapGenerator.HEIGHT; y++) {
            // Tính toán khoảng cách Chebyshev rời rạc
            if (Math.abs(x - player.getX()) <= radius && Math.abs(y - player.getY()) <= radius) {
                frame[x][y] = world[x][y]; // Trong tầm nhìn -> Hiển thị bình thường
            } else {
                frame[x][y] = Tileset.NOTHING; // Ngoài tầm nhìn -> Bọc bóng tối
            }
        }
    }
    return frame;
}
```

### 6.2 Vật phẩm điểm số & Cơ chế Máu (90 Điểm - Tính năng phụ)
*   **Vật phẩm Táo (`🍎`)**: Hồi phục máu cho người chơi [327].
*   **Vật phẩm Vàng (`$`)**: Tăng điểm số hiển thị trên HUD [327].
*   Cơ chế được tích hợp trực tiếp vào logic di chuyển phòng ngự của `moveAvatar` tại Mục 3. Các vật phẩm được rải ngẫu nhiên trên các ô sàn trống `Tileset.FLOOR` sau khi sinh bản đồ thành công.

---

## 7. Phân tích Hiệu năng & Ranh giới Phức tạp (Complexity Bounds)

### 7.1 Độ phức tạp thời gian (Time Complexity)
*   **Xử lý phím bấm & di chuyển Avatar**: Chỉ là phép tịnh tiến và gán trị tọa độ trực tiếp, đạt độ phức tạp tối ưu **$\Theta(1)$** [174].
*   **Tính toán tầm nhìn sương mù (Line of Sight)**: Duyệt mảng tĩnh 2D một lượt mỗi khung hình. Độ phức tạp là **$\Theta(	ext{WIDTH} \cdot 	ext{HEIGHT})$**. Với mảng kích thước cố định $80 	imes 45 = 3600$ phép tính, thao tác này chạy tốn chưa tới $0.5	ext{ms}$ [174].
*   **Tuần tự hóa lưu/tải game**: Độc lập hoàn toàn với tổng dung lượng hay lịch sử lưu trữ của Gradescope, đạt độ phức tạp tuyến tính **$O(	ext{WIDTH} \cdot 	ext{HEIGHT})$** phù hợp hoàn hảo với yêu cầu chạy thời gian thực [407].

### 7.2 Độ phức tạp không gian (Space Complexity)
Toàn bộ cấu trúc game chỉ tốn một mảng lưới 2D tĩnh và một chuỗi String ghi nhận lịch sử chuyển động. Đạt độ phức tạp không gian tối ưu **$O(	ext{WIDTH} \cdot 	ext{HEIGHT} + K)$** với $K$ là số lượng phím di chuyển, bảo chứng bộ nhớ luôn rảnh rang và tuyệt đối không bao giờ làm tràn bộ nhớ Stack [v10].
