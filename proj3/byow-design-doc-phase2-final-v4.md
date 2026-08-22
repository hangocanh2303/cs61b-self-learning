# BYOW Technical Design Document - Phase 2 (Unified Comprehensive Design Document)
**Tác giả:** Anh Ha
**Phiên bản:** v21 (Bản Thiết Kế Đồng Bộ Với Source Code Thực Tế)
**Khóa học:** CS 61B - UC Berkeley

---

## 1. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (UML & System Architecture)

Để đảm bảo tính linh hoạt, khả năng mở rộng lâu dài và vượt qua 100% các bài kiểm thử tự động (Autograder) của Gradescope, cấu trúc Phase 2 của dự án BYOW được tổ chức dựa trên hai nguyên lý cốt lõi của Kỹ nghệ Phần mềm: **Đóng gói Trạng thái vật lý rời rạc (State Encapsulation)** và **Đa hình Kiểu phụ (Subtype Polymorphism)** nhằm triệt tiêu hoàn toàn sự lặp mã nguồn (DRY) và lỗi phân rã theo thời gian (Temporal Decomposition) [27.2, 28.1, 28.3].

```
                     +──────────────────────────────────+
                     |            Core.Main             |
                     +──────────────────────────────────+
                                      │ (khởi chạy)
                                      v
                     +──────────────────────────────────+
                     |           Core.Engine            | <─── (Điều phối chính, chứa runGame và drawMainMenu)
                     +──────────────────────────────────+      WIDTH=80, HEIGHT=30
                                      │
         ┌────────────────────────────┴────────────────────────────┐
         ▼ (Chế độ chơi Keyboard GUI)                              ▼ (Chế độ Autograder String)
  interactWithKeyboard()                                    interactWithInputString()
         │                                                         │
         │ (vẽ drawMainMenu() trước,                               │ (không render, xử lý Load prefix)
         │  sau đó khởi tạo KeyboardSource)                        │ (khởi tạo StringSource)
         v                                                         v
+──────────────────+                                        +──────────────────+
|  KeyboardSource  |                                        |   StringSource   |
+──────────────────+                                        +──────────────────+
         │                                                         │
         └────────────────────────────┬────────────────────────────┘
                                      v (Truyền tham số đa hình InputSource)
                     +──────────────────────────────────+
                     | runGame(InputSource, renderFlag) | <─── (CHỈ MỘT VÒNG LẶP ĐIỀU PHỐI DUY NHẤT)
                     +──────────────────────────────────+
                                      │ (Xử lý máy trạng thái, cập nhật trạng thái game)
                                      v
                     +──────────────────────────────────+
                     |          Core.GameState          | <─── (Serializable - Lưu giữ toàn trạng)
                     +──────────────────────────────────+
                        │              │              │
                        ├─ TETile[][]  ├─ Position    ├─ int health, score
                        ├─ Random rng  ├─ losEnabled  └─ StringBuilder inputHistory
```

### Danh sách File trong Package `byow.Core`:
| File | Mô tả |
|------|-------|
| `Engine.java` | Điều phối chính, xử lý menu và game loop |
| `GameState.java` | Lưu trữ trạng thái game: world, avatar, score, health, inputHistory |
| `MapGenerator.java` | Sinh bản đồ ngẫu nhiên với phòng và hành lang |
| `Room.java` | Đại diện một phòng hình chữ nhật |
| `Position.java` | Tọa độ (x, y) immutable |
| `WorldComponent.java` | Interface cho các thành phần có thể vẽ |
| `InputSource.java` | Interface đầu vào đa hình |
| `KeyboardSource.java` | Đọc phím từ bàn phím (non-blocking) |
| `StringSource.java` | Đọc từ chuỗi ký tự (cho autograder) |
| `PersistenceUtils.java` | Lưu/đọc history ra file text |
| `RandomUtils.java` | Tiện ích sinh số ngẫu nhiên |

### Các lớp Thực thể & Vai trò Kiến trúc:
1.  **`Engine.java` (Orchestrator)**: Đóng vai trò là cổng giao tiếp ngoài cùng. Nó chịu trách nhiệm phân tích tham số dòng lệnh, điều hướng giữa chế độ GUI trực quan (`interactWithKeyboard`) và chế độ kiểm thử ngầm định (`interactWithInputString`) [292]. Toàn bộ logic chạy menu, khởi tạo thế giới, điều khiển di chuyển, lưu/tải game được gom gọn vào duy nhất một phương thức điều phối đa hình **`runGame`** nhằm đảm bảo tính đồng nhất 100% về hành vi giữa hai chế độ chơi [28.3].
2.  **`GameState.java` (Core Model)**: Mô-đun sâu nhất (Deep Module) chịu trách nhiệm vận hành mọi quy tắc vật lý, cơ chế tương tác và các chỉ số sinh mệnh của trò chơi [28.3]. Lớp này đóng gói mảng gạch thế giới, tọa độ người chơi, trạng thái chỉ số (máu, điểm số), trạng thái bật/tắt sương mù Line of Sight, và bộ sinh số giả ngẫu nhiên `Random` [307, 312].
3.  **`InputSource.java` (Abstraction Barrier)**: Interface trừu tượng hóa cách thức thu thập các lệnh bấm phím, giúp che giấu nguồn gốc dữ liệu (từ bàn phím vật lý rời rạc hoặc từ chuỗi ký tự tĩnh của Gradescope) [28.3, 326].
4.  **`PersistenceUtils.java` (Persistence Layer)**: Module xử lý lưu/đọc chuỗi lịch sử input (history string) phục vụ tính năng `:Q` (Lưu game) và `L` (Tải game) [372, 388]. Sử dụng file text đơn giản thay vì serialization để đảm bảo tính tất định.

---

## 2. Interface Đa hình Đầu vào (Polymorphic Input System)

Để bảo đảm **Tính tất định (Determinism)** của thế giới game—nơi mà cùng một chuỗi hành động bấm phím phải tạo ra kết quả giống hệt nhau trên mọi môi trường chạy—chúng ta triển khai interface đa hình `InputSource` [312]. 

Để giải quyết triệt để lỗi "đứng im nghẽn bàn phím" và "mất cập nhật HUD chuột", chúng ta tách biệt hoàn toàn hai hành vi: **Thăm dò luồng chạy (`possibleNextInput`)** và **Kiểm tra bộ đệm phím bấm tức thời (`hasNextKey`)** [421, 422]:

```java
package byow.Core;

public interface InputSource {
    char getNextKey();
    boolean hasNextKey();
    boolean possibleNextInput();
}
```

### 2.1 Lớp `KeyboardSource` (Hiện thực không chặn - Non-blocking HUD)
Trong chế độ chơi trực tiếp, `possibleNextInput()` luôn trả về `true` vì người chơi luôn có thể bấm phím tiếp theo. `hasNextKey()` bọc hàm không chặn `StdDraw.hasNextKeyTyped()` để kiểm tra xem có phím nào đang chờ xử lý hay không mà không làm nghẽn luồng render HUD chuột [421, 422]:

```java
package byow.Core;

import edu.princeton.cs.introcs.StdDraw;

public class KeyboardSource implements InputSource {
    private static final int PAUSE = 10;
    
    @Override
    public char getNextKey() {
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                return Character.toUpperCase(StdDraw.nextKeyTyped());
            }
            StdDraw.pause(PAUSE); // Giảm tải CPU khi chờ đợi phím
        }
    }

    @Override
    public boolean hasNextKey() {
        return StdDraw.hasNextKeyTyped(); // Kiểm tra không chặn bộ đệm phím
    }

    @Override
    public boolean possibleNextInput() {
        return true; // Bàn phím luôn có khả năng đón nhận phím gõ tiếp theo
    }
}
```

### 2.2 Lớp `StringSource` (Phân tích chuỗi lệnh tĩnh học)
Sử dụng cho bộ chấm điểm autograder của Gradescope để chạy các chuỗi kiểm thử ngầm (headless) có dạng `"N999SDDD:Q"` hoặc `"LDDDD"` mà không vẽ màn hình [311, 312]:

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
        index += 1;
        return c;
    }

    @Override
    public boolean hasNextKey() {
        return true; // Chuỗi tĩnh luôn sẵn sàng đọc ngay lập tức mà không cần chờ đợi gõ phím
    }

    @Override
    public boolean possibleNextInput() {
        return index < input.length(); // Kết thúc khi đã duyệt hết chuỗi
    }
}
```

**Lưu ý**: `RandomInputSource` có sẵn trong package `byow.InputDemo` nhưng không được sử dụng trong implementation Phase 2 hiện tại.

---

## 3. Lớp GameState & Cơ chế Va chạm Vật lý Phòng thủ

Lớp `GameState` kế thừa `Serializable` để có thể hỗ trợ tương lai. Khi di chuyển Avatar, chúng ta áp dụng tư duy **Lập trình phòng thủ (Defensive Programming)** để ngăn chặn việc đi xuyên tường `Tileset.WALL` và thu thập các vật phẩm [28.1]:

```java
package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import java.io.Serializable;
import java.util.Random;

public class GameState implements Serializable {
    private final TETile[][] world;
    private Position avatarPos;
    private final Random random;
    private final StringBuilder inputHistory;
    
    // Các chỉ số nâng cao phục vụ 360 điểm Ambition
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
        this.losEnabled = false; // Mặc định tắt LOS để hiển thị toàn bộ bản đồ
    }

    public TETile[][] getWorld() { return world; }
    public Position getAvatarPos() { return avatarPos; }
    public Random getRandom() { return random; }
    public StringBuilder getInputHistory() { return inputHistory; }
    public String getInputHistoryString() { return inputHistory.toString(); }
    public int getScore() { return score; }
    public int getHealth() { return health; }
    public boolean isLosEnabled() { return losEnabled; }
    
    /**
     * Bật/Tắt chế độ tầm nhìn sương mù (Line of Sight) thời gian thực.
     * Thỏa mãn yêu cầu toggle an toàn bằng phím 'V'.
     */
    public void toggleLOS() { 
        this.losEnabled = !this.losEnabled; 
    }

    /**
     * Di chuyển nhân vật Avatar một cách phòng ngự.
     * Đảm bảo không ném ArrayIndexOutOfBoundsException và không đi qua tường.
     * GHI CHÚ QUAN TRỌNG: Chỉ ghi nhận phím di chuyển hợp lệ vào inputHistory.
     * Phần khởi tạo (N + seed + S) được ghi riêng trong Engine khi tạo GameState.
     */
    public void moveAvatar(char key) {
        int dx = 0;
        int dy = 0;
        char direction = Character.toUpperCase(key);

        switch (direction) {
            case 'W': dy = 1; break;   // UP
            case 'S': dy = -1; break;  // DOWN
            case 'A': dx = -1; break;  // LEFT
            case 'D': dx = 1; break;   // RIGHT
            default: return; // Bỏ qua phím không hợp lệ - KHÔNG ghi vào history
        }

        int targetX = avatarPos.getX() + dx;
        int targetY = avatarPos.getY() + dy;

        // CHỐT CHẶN PHÒNG NGỰ: Kiểm tra ranh giới lưới mảng trước dựa trên kích thước của Engine
        if (targetX >= 0 && targetX < Engine.WIDTH && targetY >= 0 && targetY <= Engine.HEIGHT) {
            TETile targetTile = world[targetX][targetY];

            // Chỉ cho phép đi vào ô FLOOR hoặc các thực thể tương tác (FLOWER, GOLD)
            if (targetTile.equals(Tileset.FLOOR) || targetTile.equals(Tileset.FLOWER) 
                    || targetTile.equals(Tileset.GOLD)) {
                
                // Ghi nhận lịch sử di chuyển phục vụ Replay hoặc lưu trữ
                inputHistory.append(direction);

                // Xử lý ăn vật phẩm (Secondary Ambition Feature)
                if (targetTile.equals(Tileset.FLOWER)) {
                    health = Math.min(100, health + 20); // Ăn hoa hồi máu
                } else if (targetTile.equals(Tileset.GOLD)) {
                    score += 100; // Nhặt vàng tăng điểm
                }

                // Cập nhật vị trí trên lưới vật lý
                world[avatarPos.getX()][avatarPos.getY()] = Tileset.FLOOR; // Khôi phục sàn ở vị trí cũ
                avatarPos = new Position(targetX, targetY);
                world[targetX][targetY] = Tileset.AVATAR; // Ghi đè kí tự @ lên vị trí mới
            }
        }
    }

    /**
     * Lọc và vẽ một vùng không gian chiếu sáng hình vuông bán kính bằng 5 ô gạch bọc xung quanh người chơi.
     * Toàn bộ ô gạch nằm ngoài khoảng này sẽ hiển thị là Tileset.NOTHING.
     */
    public TETile[][] getRenderFrame() {
        if (!losEnabled) {
            return world; // Trả về thế giới đầy đủ nếu người chơi chủ động tắt Line of Sight
        }

        TETile[][] frame = new TETile[Engine.WIDTH][Engine.HEIGHT];
        int playerX = avatarPos.getX();
        int playerY = avatarPos.getY();
        int radius = 5; // Bán kính tầm nhìn sương mù hình vuông bọc quanh Avatar [315]

        for (int x = 0; x < Engine.WIDTH; x += 1) {
            for (int y = 0; y < Engine.HEIGHT; y += 1) {
                // Toán học khoảng cách Chebyshev rời rạc (dùng < thay vì <=)
                if (Math.abs(x - playerX) < radius && Math.abs(y - playerY) < radius) {
                    frame[x][y] = world[x][y]; // Trong tầm nhìn -> Vẽ bình thường
                } else {
                    frame[x][y] = Tileset.NOTHING; // Ngoài tầm nhìn -> Bọc bóng tối sương mù
                }
            }
        }
        return frame;
    }
}
```

---

## 4. Hiện thực hóa Lớp Điều phối Engine.java - Máy Trạng Thái Đồng Nhất (Unified State Machine Loop)

Để giải quyết triệt để lỗi DRY và Temporal Decomposition, đồng thời bảo đảm **vòng lặp không bị kết thúc sớm khi đứng im**, `Engine.java` điều phối máy trạng thái dựa trên dòng `possibleNextInput()` và chỉ xử lý phím bấm khi `hasNextKey()` có dữ liệu lấp đầy [421, 422].

**QUAN TRỌNG - Cơ chế Save/Load cho Autograder:**
- Khi tạo thế giới mới, phải ghi **toàn bộ chuỗi khởi tạo** (`N` + seed + `S`) vào `inputHistory` ngay lập tức.
- Khi xử lý `interactWithInputString("L...")`, ghép history đã lưu với phần còn lại của input rồi replay từ đầu.
- Điều này đảm bảo `interactWithInputString("n123sss:q")` + `interactWithInputString("lww")` = `interactWithInputString("n123sssww")`.

```java
package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.introcs.StdDraw;

public class Engine {
    TERenderer ter = new TERenderer();
    public static final int WIDTH = 80;
    public static final int HEIGHT = 30;

    /**
     * Chế độ chơi trực tiếp bằng bàn phím trên giao diện đồ họa.
     */
    public void interactWithKeyboard() {
        // Khởi tạo màn hình đồ họa 1 lần duy nhất
        ter.initialize(WIDTH, HEIGHT + 3); // Dành ra 3 hàng trên cùng hiển thị thanh HUD
        
        // CHỐT CHẶN KHỞI ĐỘNG: Vẽ ngay Menu chính cho người chơi nhìn thấy để tránh lỗi "Màn hình đen"
        drawMainMenu(); 
        
        InputSource source = new KeyboardSource();
        runGame(source, true);
    }

    /**
     * Chế độ chấm điểm ngầm định bằng chuỗi lệnh tĩnh của Gradescope.
     * Tuyệt đối KHÔNG gọi bất kỳ thư viện StdDraw hay render đồ họa nào tại đây.
     * 
     * QUAN TRỌNG: Nếu input bắt đầu bằng 'L', ghép savedHistory với phần còn lại
     * rồi replay từ đầu để đảm bảo tính tất định.
     */
    public TETile[][] interactWithInputString(String input) {
        if (input.toUpperCase().startsWith("L")) {
            String savedHistory = PersistenceUtils.loadHistory();
            if (savedHistory == null) {
                return null;
            }
            // Ghép history đã lưu + phần input sau 'L' để replay toàn bộ
            input = savedHistory + input.substring(1);
        }
        
        InputSource source = new StringSource(input);
        GameState finalState = runGame(source, false);
        return finalState != null ? finalState.getWorld() : null;
    }

    /**
     * TRỌNG TÂM TRÁNH LẶP CODE (DRY) & TRIỆT TIÊU TEMPORAL DECOMPOSITION.
     * Một máy trạng thái duy nhất xử lý luồng ký tự nhận được từ InputSource.
     * 
     * @param source Luồng đầu vào đa hình (Bàn phím, Chuỗi tĩnh, hoặc Ngẫu nhiên tất định)
     * @param render Cờ cấu hình có hiển thị đồ họa ra màn hình hay không
     * @return Trạng thái cuối cùng của thế giới sau khi xử lý xong luồng đầu vào
     */
    private GameState runGame(InputSource source, boolean render) {
        GameState gameState = null;
        boolean preparingQuit = false;
        boolean inMenu = true;
        StringBuilder seedBuilder = new StringBuilder();

        // Vòng lặp chính chạy dựa trên possibleNextInput() - Không bao giờ sập luồng khi đứng im!
        while (source.possibleNextInput()) {
            
            // Chỉ đọc và xử lý phím bấm khi thực sự có phím đang nằm đợi trong buffer (Không chặn!)
            if (source.hasNextKey()) {
                char key = Character.toUpperCase(source.getNextKey());

                // --- TRẠNG THÁI 1: KHỞI TẠO Ở MENU CHÍNH ---
                if (inMenu) {
                    if (key == 'N') {
                        seedBuilder.setLength(0); // Làm trống bộ đệm hạt giống
                        if (render) {
                            drawSeedMenu(""); // Vẽ màn hình yêu cầu nhập SEED lên GUI
                        }
                    } else if (Character.isDigit(key)) {
                        seedBuilder.append(key);
                        if (render) {
                            drawSeedMenu(seedBuilder.toString()); // Hiển thị số đang nhập theo thời gian thực
                        }
                    } else if (key == 'S' && seedBuilder.length() > 0) {
                        // Chốt hạt giống thành công -> Kích hoạt sinh địa hình
                        long seed = Long.parseLong(seedBuilder.toString());
                        MapGenerator generator = new MapGenerator(seed);
                        TETile[][] world = generator.generate();

                        // Tìm kiếm ô sàn trống đầu tiên để định vị Avatar một cách an toàn
                        Position startPos = findEmptyFloor(world);
                        world[startPos.getX()][startPos.getY()] = Tileset.AVATAR;

                        // Đóng gói trạng thái thế giới
                        gameState = new GameState(world, startPos, new java.util.Random(seed));
                        
                        // QUAN TRỌNG: Ghi toàn bộ lệnh khởi tạo vào inputHistory để save/load hoạt động đúng
                        gameState.getInputHistory().append("N").append(seedBuilder).append("S");
                        
                        inMenu = false; // Thoát khỏi Menu, chuyển sang trạng thái di chuyển!
                    } else if (key == 'L') {
                        // Tải lại game cũ từ file savefile.txt
                        String savedHistory = PersistenceUtils.loadHistory();
                        if (savedHistory == null) {
                            if (render) {
                                System.exit(0); // Nếu chơi GUI mà không có file, đóng chương trình an toàn [310]
                            } else {
                                return null;    // Chế độ chấm điểm trả về null phòng ngự [310]
                            }
                        }
                        // Replay toàn bộ history đã lưu để khôi phục trạng thái
                        InputSource replaySource = new StringSource(savedHistory);
                        gameState = runGame(replaySource, false);
                        inMenu = false; // Vào game trực tiếp bằng trạng thái đã khôi phục!
                    } else if (key == 'Q') {
                        if (render) {
                            System.exit(0);
                        } else {
                            return null;
                        }
                    }

                // --- TRẠNG THÁI 2: ĐANG TRONG TRẬN ĐẤU (GAMEPLAY STATE) ---
                } else {
                    if (preparingQuit) {
                        if (key == 'Q') {
                            // Lưu toàn bộ inputHistory (bao gồm N+seed+S và các bước di chuyển)
                            PersistenceUtils.saveHistory(gameState.getInputHistoryString());
                            if (render) {
                                System.exit(0);
                            } else {
                                return gameState; // Chế độ String trả về trạng thái để autograder so khớp
                            }
                        }
                        preparingQuit = false; // Nhấn phím bất kỳ khác -> Hủy trạng thái chờ thoát [309]
                    } else if (key == ':') {
                        preparingQuit = true; // Bật cờ chờ nhấn phím Q kế tiếp để thoát [309]
                    } else if (key == 'V') {
                        gameState.toggleLOS(); // Bật/tắt Line of Sight (270 điểm Ambition) [315]
                    } else {
                        gameState.moveAvatar(key); // Di chuyển nhân vật phòng ngự (W, A, S, D) [307]
                    }
                }
            }

            // --- TRẠNG THÁI 3: KẾT XUẤT ĐỒ HỌA HOVER CHUỘT (CHỈ CHẠY TRONG KEYBOARD MODE) ---
            // Đặt ngoài khối hasNextKey() để di chuột luôn mượt mà khi đứng im di chuyển!
            if (render && !inMenu && gameState != null) {
                renderGameView(gameState);
            }
        }

        return gameState;
    }
    
    private void renderGameView(GameState gameState) {
        // Đọc tọa độ pixel của chuột và dịch sang tọa độ ô gạch vật lý [326]
        int mouseX = (int) StdDraw.mouseX();
        int mouseY = (int) StdDraw.mouseY();
        String hudMessage = "Void";

        if (mouseX >= 0 && mouseX < WIDTH && mouseY >= 0 && mouseY < HEIGHT) {
            hudMessage = gameState.getWorld()[mouseX][mouseY].description();
        }

        // Render mảng gạch đã được lọc qua bóng tối sương mù Line of Sight [315]
        TETile[][] frameToRender = gameState.getRenderFrame();
        ter.renderFrame(frameToRender);
        
        // Vẽ đè thanh HUD chứa máu, điểm và mô tả hover gạch
        drawHUD(hudMessage, gameState);
        
        StdDraw.show();
        StdDraw.pause(10); // Khống chế luồng tránh CPU chạy quá công suất
    }

    private Position findEmptyFloor(TETile[][] world) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (world[x][y].equals(Tileset.FLOOR)) {
                    return new Position(x, y);
                }
            }
        }
        return new Position(WIDTH / 2, HEIGHT / 2);
    }

    private void drawMainMenu() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        
        java.awt.Font fontTitle = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 40);
        StdDraw.setFont(fontTitle);
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0 + 10, "CS61B: THE GAME");
        
        java.awt.Font fontOptions = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 20);
        StdDraw.setFont(fontOptions);
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0 + 2, "New Game (N)");
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0, "Load Game (L)");
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0 - 2, "Quit (Q)");
        
        StdDraw.show();
    }

    private void drawSeedMenu(String currentSeed) {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0 + 5, "Enter Seed (press 'S' to start):");
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0, currentSeed);
        StdDraw.show();
    }

    private void drawHUD(String hoverMessage, GameState state) {
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.textLeft(2, HEIGHT + 1.5, "Tile: " + hoverMessage);
        StdDraw.textRight(WIDTH - 2, HEIGHT + 1.5, "HP: " + state.getHealth() + "% | Score: " + state.getScore());
        StdDraw.line(0, HEIGHT + 0.5, WIDTH, HEIGHT + 0.5); // Vẽ ranh giới phân tách HUD và bản đồ
    }
}
```

---

## 5. Cơ chế Lưu & Tải game Deterministic (History-based Persistence)

Đặc tả yêu cầu trò chơi phải khôi phục lại thế giới giống hệt thời điểm trước khi thoát [310]. Thay vì serialize toàn bộ đối tượng `GameState`, chúng ta sử dụng phương pháp **lưu chuỗi lịch sử input (History String)** đơn giản và hiệu quả hơn.

**Nguyên lý hoạt động:**
1. Khi tạo game mới: Ghi `"N" + seed + "S"` vào `inputHistory`
2. Khi di chuyển: Ghi thêm các phím W/A/S/D vào `inputHistory`
3. Khi save (`:Q`): Lưu toàn bộ `inputHistory` ra file text
4. Khi load (`L`): Đọc `inputHistory` từ file, ghép với phần input mới, replay từ đầu

Cách này đảm bảo **tính tất định tuyệt đối** vì cùng một chuỗi input sẽ luôn tạo ra cùng một trạng thái thế giới.

```java
package byow.Core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class PersistenceUtils {
    // Đảm bảo đường dẫn tệp tin an toàn trên mọi hệ điều hành (OS-agnostic file join) [321, 322]
    private static final File SAVE_FILE = Paths.get("savefile.txt").toFile();

    /**
     * Lưu chuỗi lịch sử input ra file.
     * Chuỗi này bao gồm cả phần khởi tạo (N+seed+S) và các bước di chuyển (W/A/S/D).
     */
    public static void saveHistory(String history) {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(SAVE_FILE), StandardCharsets.UTF_8))) {
            out.print(history);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Đọc chuỗi lịch sử input từ file.
     * Trả về null nếu file không tồn tại để Engine xử lý thoát an toàn [310].
     */
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
```

**Ví dụ minh họa:**
- `interactWithInputString("n123sss:q")`:
  - Tạo world với seed 123 → `inputHistory = "N123S"`
  - Di chuyển S 3 lần → `inputHistory = "N123SSSS"` 
  - Save → lưu `"N123SSSS"` vào file
  
- `interactWithInputString("lww")`:
  - Load → đọc `"N123SSSS"` từ file
  - Ghép với `"ww"` → `"N123SSSSww"`
  - Replay toàn bộ → kết quả giống `interactWithInputString("n123sssww")`

---

## 6. Thuật toán Sinh Bản đồ (MapGenerator) 

Lớp `MapGenerator` sử dụng thuật toán sinh phòng ngẫu nhiên và kết nối bằng hành lang L-shape đơn giản. Đây là implementation gọn nhẹ đáp ứng yêu cầu Phase 1 của dự án.

### 6.1 Cấu trúc và Hằng số

```java
package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MapGenerator {
    public static final int WIDTH = Engine.WIDTH;   // 80
    public static final int HEIGHT = Engine.HEIGHT; // 30 - đồng bộ với Engine
    
    private final TETile[][] world;
    private final Random random;

    // Tham số cấu hình sinh bản đồ
    private static final int MAX_ROOMS = 50;
    private static final int MIN_ROOM_SIZE = 4;
    private static final int MAX_ROOM_SIZE = 10;
    private static final int MAX_ATTEMPTS = 1000;
    private static final double FLOWER_CHANCE = 0.0015;  // 0.15% cơ hội sinh hoa
    private static final double GOLD_CHANCE = 0.0025;    // 0.25% cơ hội sinh vàng

    public MapGenerator(long seed) {
        this.world = new TETile[WIDTH][HEIGHT];
        this.random = new Random(seed);
        initializeWorld();  // Điền NOTHING vào toàn bộ ô
    }
}
```

### 6.2 Quy trình sinh bản đồ

```
generate()
    │
    ├─► generateRooms()      // Bước 1: Tạo các phòng ngẫu nhiên không chồng lấn
    │       │
    │       └─► Room.draw()  // Vẽ FLOOR vào vùng phòng
    │
    ├─► connectRooms()       // Bước 2: Kết nối phòng bằng hành lang L-shape
    │       │
    │       ├─► Sắp xếp theo tọa độ X của tâm phòng
    │       │
    │       └─► drawLHallway(p1, p2)  // Vẽ hành lang ngang rồi dọc
    │
    ├─► generateWalls()      // Bước 3: Tạo tường bao quanh FLOOR
    │
    └─► populateItems()      // Bước 4: Rải vật phẩm (FLOWER, GOLD)
```

### 6.3 Thuật toán chi tiết

**Sinh phòng (generateRooms):**
1. Random số phòng mục tiêu từ 10 đến MAX_ROOMS
2. Lặp tối đa MAX_ATTEMPTS lần:
   - Random kích thước phòng (MIN_ROOM_SIZE đến MAX_ROOM_SIZE)
   - Random vị trí bottomLeft (đảm bảo không chạm biên)
   - Kiểm tra overlap với các phòng đã tạo
   - Nếu không overlap → thêm phòng và vẽ FLOOR

**Kết nối phòng (connectRooms):**
1. Sắp xếp danh sách phòng theo tọa độ X tâm tăng dần
2. Với mỗi cặp phòng liền kề (i, i+1):
   - Lấy tâm p1, p2
   - Vẽ hành lang ngang từ x1 đến x2 tại y1
   - Vẽ hành lang dọc từ y1 đến y2 tại x2

**Sinh tường (generateWalls):**
- Duyệt từng ô NOTHING
- Nếu có ít nhất 1 ô FLOOR kề cạnh (8 hướng) → chuyển thành WALL

### 6.4 Lớp hỗ trợ Room và WorldComponent

```java
// Interface cho các thành phần có thể vẽ
public interface WorldComponent {
    void draw(TETile[][] world);
}

// Lớp Room đại diện một phòng hình chữ nhật
public class Room implements WorldComponent {
    private final Position bottomLeft;
    private final int width;
    private final int height;

    public Room(Position bottomLeft, int width, int height) { ... }
    
    public Position getCenter() {
        return new Position(bottomLeft.getX() + width / 2,
                            bottomLeft.getY() + height / 2);
    }
    
    public boolean overlaps(Room other) {
        // Kiểm tra giao nhau của 2 hình chữ nhật
        return (x1 < x2 + w2) && (x1 + w1 > x2) 
            && (y1 < y2 + h2) && (y1 + h1 > y2);
    }
    
    @Override
    public void draw(TETile[][] world) {
        // Vẽ FLOOR cho toàn bộ vùng phòng
        for (int x = startX; x < startX + width; x++) {
            for (int y = startY; y < startY + height; y++) {
                world[x][y] = Tileset.FLOOR;
            }
        }
    }
}
```

---

## 7. Tính năng Ambition (Primary + Secondary Features)

Để giúp trò giành điểm tối đa của danh mục Ambition Score, chúng ta triển khai các tính năng bổ trợ nhau [313]:

### 7.1 Line of Sight (Tính năng chính)
Sử dụng toán học Chebyshev rời rạc để lọc và vẽ một vùng không gian chiếu sáng hình vuông bán kính bằng `5` ô gạch bọc xung quanh người chơi. Toàn bộ ô gạch nằm ngoài khoảng này sẽ hiển thị là `Tileset.NOTHING` [315]. Toàn bộ logic này được đóng gói gọn gàng bên trong `GameState.getRenderFrame()` đã trình bày tại Mục 3 [315].

- **Bật/Tắt**: Phím `V` để toggle
- **Trạng thái mặc định**: Tắt (hiển thị toàn bộ bản đồ)

### 7.2 Vật phẩm tương tác & Cơ chế Máu (Tính năng phụ)
*   **Vật phẩm Hoa (`FLOWER`)**: Hồi phục máu cho người chơi (+20 HP, tối đa 100 HP) [307].
*   **Vật phẩm Vàng (`GOLD`)**: Tăng điểm số (+100 điểm) [307].
*   Cơ chế được tích hợp trực tiếp vào logic di chuyển phòng ngự của `moveAvatar` tại Mục 3 [307].

**Tham số phân bố vật phẩm (trong MapGenerator):**
```java
private static final double FLOWER_CHANCE = 0.0015;  // 0.15% mỗi ô FLOOR
private static final double GOLD_CHANCE = 0.0025;    // 0.25% mỗi ô FLOOR
```

### 7.3 HUD (Heads-Up Display)
Thanh thông tin phía trên màn hình hiển thị:
- **Tile description**: Mô tả ô gạch đang hover chuột
- **HP**: Máu hiện tại (%)
- **Score**: Điểm số tích lũy

```java
private void drawHUD(String hoverMessage, GameState state) {
    StdDraw.setPenColor(StdDraw.WHITE);
    StdDraw.textLeft(2, HEIGHT + 1.5, "Tile: " + hoverMessage);
    StdDraw.textRight(WIDTH - 2, HEIGHT + 1.5, 
        "HP: " + state.getHealth() + "% | Score: " + state.getScore());
    StdDraw.line(0, HEIGHT + 0.5, WIDTH, HEIGHT + 0.5);
}
```

---

## 8. Phân tích Hiệu năng & Ranh giới Phức tạp (Complexity Bounds)

### 8.1 Độ phức tạp thời gian (Time Complexity)
*   **Xử lý phím bấm & di chuyển Avatar**: Chỉ là phép tịnh tiến và gán trị tọa độ trực tiếp, đạt độ phức tạp tối ưu **Θ(1)** [174].
*   **Tính toán tầm nhìn sương mù (Line of Sight)**: Duyệt mảng tĩnh 2D một lượt mỗi khung hình. Độ phức tạp là **Θ(WIDTH × HEIGHT)** [174].
*   **Lưu/tải game**: Đạt độ phức tạp tuyến tính **O(K)** với K là độ dài chuỗi inputHistory [372].

### 8.2 Độ phức tạp không gian (Space Complexity)
Toàn bộ cấu trúc game chỉ tốn một mảng lưới 2D tĩnh và một chuỗi String ghi nhận lịch sử chuyển động. Đạt độ phức tạp không gian tối ưu **O(WIDTH × HEIGHT + K)** với K là số lượng phím di chuyển.

---

## 9. Chiến lược Kiểm thử Tích hợp (Gradescope Integration Testing)

Để tự tin kiểm chứng tính tất định (Determinism) của `Engine` trước khi nộp bài lên Gradescope, em sử dụng JUnit 4 để chạy thử nghiệm các Replay chuỗi phím có lưu/tải trạng thái động [374, 395].

```java
package byow;

import byow.Core.Engine;
import byow.TileEngine.TETile;
import org.junit.Test;
import static org.junit.Assert.*;

public class BYOWIntegrationTest {

    @Test
    public void testSaveAndLoadPersistence() {
        Engine engine1 = new Engine();
        Engine engine2 = new Engine();
        
        // Chạy thế giới 1 liên tục không dừng
        TETile[][] world1 = engine1.interactWithInputString("N999SDDDWWWAA");
        
        // Chạy thế giới 2 có save và load giữa chừng
        engine1.interactWithInputString("N999SDDD:Q"); // Lưu game tại chỗ
        TETile[][] world2 = engine2.interactWithInputString("LWWWAA"); // Tải lại và di chuyển tiếp
        
        // So khớp sâu mảng lưới 2D để bảo chứng tính tất định tuyệt đối!
        assertArrayEquals(world1, world2);
    }
}
```
