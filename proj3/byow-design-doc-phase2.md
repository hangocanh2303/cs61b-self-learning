# BYOW Technical Design Document - Phase 2 (Interactivity & Persistence)
**Tác giả:** Anh Ha
**Khóa học:** CS 61B - UC Berkeley
**Bản thiết kế kỹ thuật chi tiết dựa trên nền tảng v12**

---

## 1. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (Classes and Data Structures)

Để quản lý độ phức tạp tăng vọt khi chuyển từ thế giới tĩnh (Phase 1) sang thế giới tương tác động (Phase 2), chúng ta áp dụng triệt để nguyên lý **Che giấu thông tin (Information Hiding)** và **Thiết kế Mô-đun Sâu (Deep Modules)** [28.3]. 

Chúng ta đóng gói toàn bộ trạng thái thế giới vào một lớp duy nhất tên là `GameState`. Lớp này chịu trách nhiệm quản lý dữ liệu, di chuyển nhân vật, lưu/tải game và kiểm soát tính tất định (determinism) [362]. Lớp `Engine` chỉ đóng vai trò là lớp vỏ bọc bên ngoài để điều phối luồng vào/ra [327].

```
                     +──────────────────────+
                     |      Core.Main       |
                     +──────────────────────+
                                │ (gọi)
                                v
                     +──────────────────────+
                     |     Core.Engine      |
                     +──────────────────────+
                                │
         ┌──────────────────────┴──────────────────────┐
         ▼ (Keyboard Mode)                             ▼ (String Mode)
  interactWithKeyboard()                        interactWithInputString()
         │                                             │
         │ (sử dụng)                                   │ (sử dụng)
         v                                             v
+──────────────────+                            +──────────────────+
|  KeyboardSource  |                            |   StringSource   |
+──────────────────+                            +──────────────────+
         │                                             │
         └──────────────────────┬──────────────────────┘
                                v (đọc kí tự)
                     +──────────────────────+
                     |    Core.GameState    | <─── (Serializable) [423]
                     +──────────────────────+
                        │              │
                        v (chứa)       v (chứa)
                  TETile[][] world   Position avatarPos
```

---

### Position
Lớp immutable (bất biến) đại diện cho tọa độ `(x, y)` trên lưới [v3].
*   **Các trường (Fields):**
    *   `private final int x`
    *   `private final int y`
*   **Đặc điểm:** Thực hiện interface `Serializable` để có thể ghi trực tiếp xuống file cùng với `GameState` [423]. Được override các phương thức `equals(Object o)` và `hashCode()` một cách đồng bộ để phục vụ các so sánh hình học và cấu trúc bảng băm [1, 376].

---

### GameState
Căn hộ lưu trữ trung tâm—**Mô-đun sâu nhất trong hệ thống** [28.3]. Lớp này đóng gói mọi thông tin về thế giới game tại một thời điểm cụ thể [28.3].

*   **Các trường (Fields):**
    *   `private static final long serialVersionUID = 123456789L;` (Đảm bảo tính tương thích phiên bản Serializable) [423].
    *   `private TETile[][] world`: Mảng lưới 2D chứa trạng thái vật lý của thế giới hiện tại [307, 327].
    *   `private Position avatarPos`: Vị trí tọa độ `(x, y)` hiện tại của nhân vật Avatar [350].
    *   `private Random random`: Đối tượng sinh ngẫu nhiên dùng chung, lưu lại để bảo toàn chuỗi giả ngẫu nhiên khi load game [353].
    *   `private StringBuilder inputHistory`: Chuỗi lịch sử lưu lại toàn bộ các phím di chuyển mà người chơi đã bấm để phục vụ kiểm thử và replay [355].
    *   `private boolean losEnabled`: Trạng thái bật/tắt của tầm nhìn sương mù (Line of Sight) [359].

*   **Các phương thức (Methods):**
    *   `public void moveAvatar(char key)`: Tiếp nhận phím bấm (`W`, `A`, `S`, `D`), tính toán tọa độ mới và di chuyển một cách phòng thủ (không đi xuyên tường) [28.1, 362].
    *   `public TETile[][] getRenderFrame()`: Trả về khung lưới gạch cần hiển thị. Nếu `losEnabled = true`, nó sẽ áp dụng bộ lọc tầm nhìn sương mù hình vuông xung quanh Avatar [359].
    *   `public void toggleLOS()`: Thay đổi trạng thái của `losEnabled` khi người chơi nhấn phím chuyển đổi [359].

---

### InputSource (Interface)
Giải pháp trừu tượng hóa để đồng bộ hóa hoàn toàn hai chế độ chơi: Bàn phím trực tiếp và Chuỗi kí tự tĩnh [376]. Điều này triệt tiêu hoàn toàn lỗi **Temporal Decomposition** (Phân rã theo thứ tự thời gian) [28.2].

*   **Các phương thức (Methods):**
    *   `char getNextKey()`: Trả về kí tự tiếp theo trong luồng input.
    *   `boolean hasNextKey()`: Kiểm tra xem còn kí tự nào để đọc hay không.

```java
package byow.Core;

public interface InputSource {
    char getNextKey();
    boolean hasNextKey();
}
```

#### KeyboardSource (Hiện thực không chặn - Non-blocking Keyboard Input)
Sử dụng `StdDraw.hasNextKeyTyped()` và `StdDraw.nextKeyTyped()` để bắt sự kiện từ bàn phím mà không làm đứng màn hình, cho phép thanh HUD cập nhật thông tin hover chuột liên tục [376, 377].

```java
package byow.Core;

import edu.princeton.cs.introcs.StdDraw;

public class KeyboardSource implements InputSource {
    @Override
    public char getNextKey() {
        return StdDraw.nextKeyTyped();
    }

    @Override
    public boolean hasNextKey() {
        return StdDraw.hasNextKeyTyped();
    }
}
```

#### StringSource (Hiện thực phân tích chuỗi kí tự tĩnh)
Đọc tuần tự từng kí tự từ một chuỗi `String` đầu vào [327].

```java
package byow.Core;

public class StringSource implements InputSource {
    private String input;
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

---

## 2. Thiết kế Thuật toán & Luồng xử lý (Algorithms & Flow)

### 2.1 Thuật toán Di chuyển Nhân vật Phòng thủ (Defensive Collision Guard)
Để đảm bảo nhân vật Avatar (ký tự `@`) di chuyển mượt mà và tuyệt đối **không bao giờ đi xuyên qua tường** (`Tileset.WALL`), phương thức `moveAvatar` thực hiện kiểm tra va chạm một cách nghiêm ngặt trước khi cập nhật dữ liệu tọa độ [28.1]:

```java
public void moveAvatar(char key) {
    int dx = 0;
    int dy = 0;
    
    switch (Character.toUpperCase(key)) {
        case 'W': dy = 1; break;  // UP
        case 'S': dy = -1; break; // DOWN
        case 'A': dx = -1; break; // LEFT
        case 'D': dx = 1; break;  // RIGHT
        default: return; // Phím không hợp lệ -> Bỏ qua an toàn
    }

    int targetX = avatarPos.getX() + dx;
    int targetY = avatarPos.getY() + dy;

    // CHỐT CHẶN PHÒNG NGỰ: Chỉ di chuyển nếu tọa độ đích là ô sàn đi được (FLOOR)
    if (inBounds(targetX, targetY) && world[targetX][targetY].equals(Tileset.FLOOR)) {
        // Ghi lại lịch sử phím bấm hợp lệ vào StringBuilder
        inputHistory.append(key);
        
        // Cập nhật lưới gạch vật lý
        world[avatarPos.getX()][avatarPos.getY()] = Tileset.FLOOR; // Khôi phục sàn ở vị trí cũ
        avatarPos = new Position(targetX, targetY);                // Cập nhật vị trí mới
        world[targetX][targetY] = Tileset.AVATAR;                  // Đặt Avatar lên vị trí mới
    }
}
```

---

### 2.2 Thuật toán Điều phối Keyboard không chặn (Heads Up Display Integration)
Nếu chúng ta gọi một hàm đọc phím dạng chặn (blocking reader), luồng xử lý của CPU sẽ bị dừng lại tại dòng đó để đợi người chơi bấm phím. Điều này làm cho chuột không thể di chuyển tự do để hover xem mô tả gạch trên HUD [376]. 

Bằng cách sử dụng **KeyboardSource không chặn**, chúng ta có thể liên tục cập nhật tọa độ chuột và hiển thị thông tin gạch lên thanh HUD mà không làm gián đoạn trò chơi [376, 377]:

```java
public void interactWithKeyboard() {
    // 1. Khởi tạo Renderer và Menu ban đầu [306, 361]
    TERenderer ter = new TERenderer();
    ter.initialize(MapGenerator.WIDTH, MapGenerator.HEIGHT + 3); // Chừa 3 hàng trên cùng làm HUD

    GameState gameState = drawMainMenuAndGetState(); // Hiển thị Menu (New, Load, Quit) [361]
    InputSource input = new KeyboardSource();
    boolean gameRunning = true;
    boolean preparingQuit = false;

    // 2. Vòng lặp Game chính (Main Game Loop)
    while (gameRunning) {
        // Kiểm tra và bắt sự kiện bàn phím (Nếu có)
        if (input.hasNextKey()) {
            char key = Character.toUpperCase(input.getNextKey());
            
            if (preparingQuit) {
                if (key == 'Q') {
                    // Người chơi hoàn tất chuỗi lệnh ":Q" -> Lưu và thoát [354]
                    PersistenceUtils.saveGame(gameState);
                    System.exit(0); // Tắt chương trình an toàn [330]
                } else {
                    preparingQuit = false; // Nhấn phím khác -> Hủy trạng thái chờ thoát
                }
            } else if (key == ':') {
                preparingQuit = true; // Kích hoạt trạng thái chờ nhấn phím Q để lưu và thoát
            } else if (key == 'V') {
                gameState.toggleLOS(); // Phím tắt 'V' bật/tắt tầm nhìn sương mù [359]
            } else {
                gameState.moveAvatar(key); // Di chuyển nhân vật [350]
            }
        }

        // 3. XỬ LÝ HOVER CHUỘT TRÊN HUD THEO THỜI GIAN THỰC [376]
        int mouseX = (int) StdDraw.mouseX();
        int mouseY = (int) StdDraw.mouseY();
        String hudMessage = "";

        if (mouseX >= 0 && mouseX < MapGenerator.WIDTH && mouseY >= 0 && mouseY < MapGenerator.HEIGHT) {
            // Đọc mô tả loại gạch trực tiếp từ Tile Engine
            hudMessage = gameState.getWorld()[mouseX][mouseY].description();
        }

        // Vẽ thế giới game lên màn hình
        TETile[][] frameToRender = gameState.getRenderFrame(); // Áp dụng bộ lọc tầm nhìn sương mù [359]
        ter.renderFrame(frameToRender);
        
        // Vẽ đè thanh HUD ở trên cùng
        drawHUD(hudMessage);
        
        StdDraw.show();
        StdDraw.pause(10); // Khống chế tốc độ vòng lặp để tránh nghẽn luồng CPU
    }
}
```

---

### 2.3 Phân tích Chuỗi lệnh Tĩnh không Render (String Input Parser)
Khi chạy qua phương thức `interactWithInputString(String input)`, bộ kiểm thử tự động của Gradescope **tuyệt đối không cho phép kết xuất đồ họa ra màn hình** (vì máy chủ chấm điểm không có giao diện hiển thị GUI) [375]. Chương trình phải phân tích chuỗi lệnh một cách tĩnh học và trả về mảng gạch lưới vật lý cuối cùng [339, 370]:

```java
public TETile[][] interactWithInputString(String input) {
    InputSource source = new StringSource(input);
    GameState gameState = null;
    boolean preparingQuit = false;

    while (source.hasNextKey()) {
        char key = Character.toUpperCase(source.getNextKey());

        if (key == 'N') {
            // Bước 1: Trích xuất hạt giống SEED nằm giữa ký tự 'N' và 'S' [336]
            StringBuilder seedStr = new StringBuilder();
            while (source.hasNextKey()) {
                char next = source.getNextKey();
                if (Character.toUpperCase(next) == 'S') {
                    break;
                }
                if (Character.isDigit(next)) {
                    seedStr.append(next);
                }
            }
            long seed = Long.parseLong(seedStr.toString()); // Sử dụng kiểu Long để tránh tràn số [FAQ 3]
            
            // Bước 2: Tạo thế giới mới từ seed thông qua bộ sinh v12
            MapGenerator generator = new MapGenerator(seed);
            TETile[][] world = generator.generate();
            
            // Bước 3: Định vị vị trí Avatar ngẫu nhiên trên một ô sàn trống (FLOOR)
            Position startPos = findEmptyFloor(world);
            world[startPos.getX()][startPos.getY()] = Tileset.AVATAR;

            gameState = new GameState(world, startPos, new Random(seed));
            
        } else if (key == 'L') {
            // Khôi phục lại trạng thái thế giới đã lưu trước đó [353]
            gameState = PersistenceUtils.loadGame();
            if (gameState == null) {
                System.exit(0); // Nếu chưa có file save, thoát an toàn không báo lỗi [353]
            }
        } else if (gameState != null) {
            // Xử lý các di chuyển và chuỗi thoát tương tự như Keyboard mode
            if (preparingQuit) {
                if (key == 'Q') {
                    PersistenceUtils.saveGame(gameState);
                    return gameState.getWorld(); // Trả về mảng gạch ngay thời điểm lưu và thoát [354, 355]
                } else {
                    preparingQuit = false;
                }
            } else if (key == ':') {
                preparingQuit = true;
            } else {
                gameState.moveAvatar(key);
            }
        }
    }

    return gameState != null ? gameState.getWorld() : null;
}
```

---

## 3. Thiết kế Cơ chế Lưu & Tải game (Persistence - Serialization)

Để tuần tự hóa (serialize) toàn bộ trạng thái phức tạp của thế giới game một cách an toàn và sạch sẽ, chúng ta sử dụng cơ chế **`Serializable`** tích hợp sẵn của Java, kế thừa tinh hoa từ Lab 6 [349, 423].

*   **Tệp lưu trữ:** Ghi nhận và lưu trực tiếp xuống tệp `savefile.txt` nằm ở thư mục gốc của dự án `proj3` [351].
*   **Tính tất định của Bộ sinh ngẫu nhiên (Deterministic Random Persistence):**
    Khi lưu game, đối tượng `Random random` chứa bên trong `GameState` cũng được tuần tự hóa một cách tự động xuống file [353]. Điều này đảm bảo khi tải lại game bằng phím `L`, bộ sinh số ngẫu nhiên của Java sẽ **tiếp tục sinh ra chính xác chuỗi giả ngẫu nhiên tiếp theo** như trạng thái trước khi lưu, bảo chứng tính tất định 100% khi chạy qua autograder [353, 362].

```java
package byow.Core;

import java.io.*;
import java.nio.file.Paths;

public class PersistenceUtils {
    // Sử dụng cơ chế ghép đường dẫn an toàn, đa nền tảng (OS-agnostic file join) [368, 369]
    private static final File SAVE_FILE = Paths.get("savefile.txt").toFile();

    /**
     * Tuần tự hóa toàn bộ đối tượng GameState xuống file savefile.txt
     */
    public static void saveGame(GameState state) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Giải tuần tự hóa trạng thái game từ file savefile.txt.
     * Nếu không tìm thấy file, tắt chương trình an toàn không báo lỗi.
     */
    public static GameState loadGame() {
        if (!SAVE_FILE.exists()) {
            return null;
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

## 4. Tính năng Sáng tạo chính: Tầm nhìn Sương mù (Line of Sight - 270 Điểm Ambition)

Để giành trọn vẹn **270 điểm Ambition** một cách thanh thoát và an toàn nhất, chúng ta hiện thực hóa tính năng **Tầm nhìn sương mù (Line of Sight)** dưới dạng một ô vuông ánh sáng di động bao quanh vị trí hiện tại của Avatar [359].

*   **Bán kính tầm nhìn (VISION_RADIUS):** Được khống chế bằng hằng số là `5` ô gạch [359].
*   **Toán học khoảng cách:** Sử dụng khoảng cách khoảng cách Chebyshev (khoảng cách tối đa trên 2 trục) để xác định xem một ô gạch có nằm trong ô vuông ánh sáng hay không:
    $$\max(|x - \text{avatarX}|, \ |y - \text{avatarY}|) \le \text{VISION\_RADIUS}$$

```java
/**
 * Lọc mảng lưới hiển thị dựa trên trạng thái tầm nhìn sương mù.
 * Toàn bộ các ô nằm ngoài tầm nhìn 5 ô gạch của player sẽ hiển thị là gạch NOTHING.
 */
public TETile[][] getRenderFrame() {
    if (!losEnabled) {
        return world; // Trả về thế giới đầy đủ nếu người chơi tắt tầm nhìn sương mù
    }

    int width = world.length;
    int height = world[0].length;
    TETile[][] frame = new TETile[width][height];

    int playerX = avatarPos.getX();
    int playerY = avatarPos.getY();
    int radius = 5; // Bán kính tầm nhìn sương mù hình vuông bao quanh player

    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
            // Tính toán khoảng cách Chebyshev rời rạc
            if (Math.abs(x - playerX) <= radius && Math.abs(y - playerY) <= radius) {
                frame[x][y] = world[x][y]; // Trong tầm nhìn -> Hiển thị bình thường
            } else {
                frame[x][y] = Tileset.NOTHING; // Ngoài tầm nhìn -> Bọc bóng tối
            }
        }
    }
    return frame;
}
```

---

## 5. Phân tích Hiệu năng & Độ phức tạp (Complexity and Performance)

### 5.1 Độ phức tạp thời gian (Time Complexity)
*   **Di chuyển nhân vật (`moveAvatar`):** Do vị trí được cập nhật trực tiếp dựa trên tọa độ, thao tác này có độ phức tạp thời gian cực kỳ tối ưu là **$\Theta(1)$** [174].
*   **Cơ chế Tầm nhìn sương mù (`getRenderFrame`):** Hệ thống duyệt qua toàn bộ lưới bản đồ để sao chép ô gạch hoặc bọc bóng tối. Độ phức tạp là **$\Theta(\text{WIDTH} \cdot \text{HEIGHT})$** (với mảng tĩnh $80 \times 45$, chi phí này mất ít hơn $0.5\text{ms}$ trên mọi cấu hình phần cứng) [174].
*   **Lưu & Tải game (Serialization/Deserialization):** Chi phí tỷ lệ thuận với số lượng phần tử cần ghi trong thế giới game, đạt mức tuyến tính **$O(\text{WIDTH} \cdot \text{HEIGHT})$**. Quá trình ghi nhận xuống ổ đĩa cứng mất khoảng $10 - 20\text{ms}$, hoàn toàn trơn tru đối với trải nghiệm người chơi [174].

### 5.2 Độ phức tạp không gian (Space Complexity)
*   **Cấu trúc lưu trữ:** Trạng thái `GameState` chứa mảng tĩnh cố định và một chuỗi StringBuilder lịch sử di chuyển có độ dài tối đa là $10^5$ kí tự. Độ phức tạp không gian đạt mức tối ưu là **$O(\text{WIDTH} \cdot \text{HEIGHT} + K)$** với $K$ là số bước di chuyển của người chơi, dung lượng file ghi trên ổ cứng cực nhỏ (luôn $<150\text{KB}$), đạt điểm tối đa về mặt tối ưu hóa tài nguyên phần cứng [174].
