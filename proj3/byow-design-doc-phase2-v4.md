# BYOW Technical Design Document - Phase 2 (Phiên bản v4 - Đa hình Hóa & Giải quyết Triệt để DRY)
**Tác giả:** Anh Ha
**Khóa học:** CS 61B - UC Berkeley
**Bản thiết kế kỹ thuật tối ưu hóa hoàn toàn luồng điều khiển của Engine.java**

---

## 1. Bản chất của lỗi DRY & Temporal Decomposition trong Thiết kế Cũ

Trong các bản phác thảo ban đầu của đồ án BYOW, sinh viên thường bị rơi vào bẫy **Temporal Decomposition (Phân rã theo thứ tự thời gian)** và **DRY Violation (Lặp lại mã nguồn)** [28.2, 114]. 

Cụ thể, hai phương thức `interactWithKeyboard()` và `interactWithInputString(String input)` thường được viết thành hai luồng xử lý độc lập hoàn toàn [114]:
*   `interactWithKeyboard()` tự hiển thị menu, tự bắt phím di chuyển, vẽ HUD và render thế giới [320, 324].
*   `interactWithInputString()` tự viết một bộ vòng lặp phân tích chuỗi ký tự, tìm hạt giống seed, tự di chuyển ngầm [326].

### ❌ Hệ quả chí mạng:
1.  **Lặp lại logic di chuyển (DRY):** Các phép kiểm tra va chạm tường, di chuyển Avatar, hay cập nhật mảng gạch vật lý bị lặp lại ở cả hai nơi [28.2]. Nếu trò thêm tính năng mới (như bẫy gai, cổng dịch chuyển Portal), trò sẽ phải sửa mã nguồn ở cả hai phương thức [113, 28.2].
2.  **Thông tin bị rò rỉ (Information Leakage):** Trạng thái hạt giống hoặc cách thức định tuyến di chuyển của người chơi bị phơi bày trực tiếp ở tầng điều phối [113].
3.  **Lỗi bất đồng bộ:** Có những trường hợp di chuyển bằng bàn phím thì hoạt động hoàn hảo, nhưng khi Gradescope chấm điểm bằng chuỗi ký tự ngầm thì nhân vật lại di chuyển sai lệch do logic di chuyển không đồng bộ.

---

## 2. Giải pháp Kiến trúc v4: Đa hình hóa & Vòng lặp Không chặn Dùng chung (Unified Game Loop)

Để giải quyết triệt để lỗi DRY, chúng ta sẽ áp dụng nguyên lý **Subtype Polymorphism (Đa hình kiểu phụ)** thông qua rào cản trừu tượng **`InputSource`** [9.1, 28.3, 355].

Chúng ta thiết lập một **Vòng lặp tương tác dùng chung duy nhất (Unified Non-blocking State Machine Loop)** có tên là `runGame(InputSource source, boolean render)` [28.3, 355]. Cả hai phương thức giao diện `interactWithKeyboard()` và `interactWithInputString()` lúc này sẽ đóng vai trò là các "bootstrappers" cực kỳ tinh giản, chỉ làm nhiệm vụ khởi tạo nguồn dữ liệu đầu vào phù hợp và ủy thác toàn bộ quyền xử lý cho `runGame` [28.3].

### Sơ đồ luồng điều khiển đồng nhất hoàn hảo (Unified Control Flow):

```
                   +──────────────────────────────────+
                   |           Engine.java            |
                   +──────────────────────────────────+
                                    │
         ┌──────────────────────────┴──────────────────────────┐
         ▼ (Keyboard Mode)                                     ▼ (String Mode)
  interactWithKeyboard()                                interactWithInputString()
         │                                                     │
         │ (khởi tạo KeyboardSource)                           │ (khởi tạo StringSource)
         v                                                     v
+──────────────────+                                    +──────────────────+
|  KeyboardSource  |                                    |   StringSource   |
+──────────────────+                                    +──────────────────+
         │                                                     │
         └──────────────────────────┬──────────────────────────┘
                                    v (truyền tham số đa hình)
                   +──────────────────────────────────+
                   | runGame(InputSource, renderFlag) | <── (CHỈ 1 VÒNG LẶP CHUNG)
                   +──────────────────────────────────+
                                    │ (xử lý tuần tự)
                                    v
                            [ Core Game State ]
```

---

## 3. Mã nguồn Chi tiết Lớp Điều phối `Engine.java` chuẩn DRY

Dưới đây là hiện thực hóa chi tiết của lớp `Engine.java` mới nhất. Vòng lặp `runGame` sử dụng cấu trúc máy trạng thái (State Machine) để chuyển đổi mượt mà giữa trạng thái hiển thị Menu và trạng thái chơi Game thực tế, dùng chung 100% logic di chuyển, lưu game, và sinh hạt giống giả ngẫu nhiên:

```java
package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.introcs.StdDraw;

public class Engine {
    private final TERenderer ter = new TERenderer();
    public static final int WIDTH = 80;
    public static final int HEIGHT = 45;

    /**
     * Chế độ chơi trực tiếp bằng bàn phím trên giao diện đồ họa.
     */
    public void interactWithKeyboard() {
        // Chỉ khởi tạo màn hình đồ họa khi thực sự tương tác với người dùng trực tiếp
        ter.initialize(WIDTH, HEIGHT + 3); // Dành ra 3 hàng trên cùng hiển thị thanh HUD
        InputSource source = new KeyboardSource();
        runGame(source, true);
    }

    /**
     * Chế độ chấm điểm ngầm định bằng chuỗi lệnh tĩnh của Gradescope.
     * Tuyệt đối KHÔNG gọi bất kỳ thư viện StdDraw hay render đồ họa nào tại đây.
     */
    public TETile[][] interactWithInputString(String input) {
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

        // Duyệt tuần tự qua dòng ký tự nhận được từ nguồn input đa hình
        while (source.hasNextKey()) {
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
                    // Chốt hạt giống thành công -> Kích hoạt sinh địa hình v12
                    long seed = Long.parseLong(seedBuilder.toString());
                    MapGenerator generator = new MapGenerator(seed);
                    TETile[][] world = generator.generate();

                    // Tìm kiếm ô sàn trống đầu tiên để định vị Avatar một cách an toàn
                    Position startPos = findEmptyFloor(world);
                    world[startPos.getX()][startPos.getY()] = Tileset.AVATAR;

                    // Đóng gói trạng thái thế giới
                    gameState = new GameState(world, startPos, new java.util.Random(seed));
                    inMenu = false; // Thoát khỏi Menu, chuyển sang trạng thái di chuyển!
                } else if (key == 'L') {
                    // Tải lại game cũ từ file savefile.txt
                    gameState = PersistenceUtils.loadGame();
                    if (gameState == null) {
                        if (render) {
                            System.exit(0); // Nếu chơi GUI mà không có file, đóng chương trình an toàn
                        } else {
                            return null;    // Chế độ chấm điểm trả về null phòng ngự
                        }
                    }
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
                        PersistenceUtils.saveGame(gameState); // Ghi tuần tự đối tượng GameState
                        if (render) {
                            System.exit(0);
                        } else {
                            return gameState; // Chế độ String trả về trạng thái để autograder so khớp
                        }
                    }
                    preparingQuit = false; // Nhấn phím bất kỳ khác -> Hủy trạng thái chờ thoát
                } else if (key == ':') {
                    preparingQuit = true; // Bật cờ chờ nhấn phím Q kế tiếp để thoát
                } else if (key == 'V') {
                    gameState.toggleLOS(); // Bật/tắt Line of Sight (270 điểm Ambition)
                } else {
                    gameState.moveAvatar(key); // Di chuyển nhân vật phòng ngự (W, A, S, D)
                }
            }

            // --- TRẠNG THÁI 3: KẾT XUẤT ĐỒ HỌA HOVER CHUỘT (CHỈ CHẠY TRONG KEYBOARD MODE) ---
            if (render && !inMenu && gameState != null) {
                // Đọc tọa độ pixel của chuột và dịch sang tọa độ ô gạch vật lý
                int mouseX = (int) StdDraw.mouseX();
                int mouseY = (int) StdDraw.mouseY();
                String hudMessage = "";

                if (mouseX >= 0 && mouseX < WIDTH && mouseY >= 0 && mouseY < HEIGHT) {
                    hudMessage = gameState.getWorld()[mouseX][mouseY].description();
                }

                // Render mảng gạch đã được lọc qua bóng tối sương mù Line of Sight
                TETile[][] frameToRender = gameState.getRenderFrame();
                ter.renderFrame(frameToRender);
                
                // Vẽ đè thanh HUD chứa máu, điểm và mô tả hover gạch
                drawHUD(hudMessage, gameState);
                
                StdDraw.show();
                StdDraw.pause(10); // Khống chế luồng tránh CPU chạy quá công suất
            }
        }

        return gameState;
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

    private void drawSeedMenu(String currentSeed) {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(WIDTH / 2, HEIGHT / 2 + 5, "Enter Seed (press 'S' to start):");
        StdDraw.text(WIDTH / 2, HEIGHT / 2, currentSeed);
        StdDraw.show();
    }

    private void drawHUD(String hoverMessage, GameState state) {
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.textLeft(2, HEIGHT + 1, "Tile: " + hoverMessage);
        StdDraw.textRight(WIDTH - 2, HEIGHT + 1, "HP: " + state.getHealth() + "% | Score: " + state.getScore());
    }
}
```

---

## 4. Tại sao Thiết kế này lại là một Kiệt tác?

1.  **Duy nhất một Máy trạng thái điều phối (Single Source of Truth):**
    Toàn bộ logic phân tích cú pháp menu khởi tạo (`N`, `L`, `Q`), thu thập chuỗi số để dựng seed kết thúc bằng `S`, logic di chuyển (`W`, `A`, `S`, `D`), logic chờ lưu và thoát (`:Q`), hay phím tắt toggle sương mù (`V`) **chỉ được viết chính xác một lần duy nhất** bên trong vòng lặp `runGame` [28.3]! 
    Sự lặp lại mã nguồn (DRY) chính thức bị tiêu diệt hoàn toàn [28.2].

2.  **Đa hình kiểu phụ (Subtype Polymorphism) thanh lịch:**
    Bằng cách trừu tượng hóa dòng phím bấm thành interface `InputSource`, việc điều khiển luồng game không phụ thuộc vào nguồn tạo ra nó [28.3, 355]. 
    *   Trong `interactWithKeyboard()`, `KeyboardSource` liên tục trả về các phím gõ từ bàn phím rời rạc của người chơi [376].
    *   Trong `interactWithInputString()`, `StringSource` tuần tự "nhả" ra các ký tự từ chuỗi tĩnh truyền vào từ autograder [327].

3.  **Bảo chứng an toàn Headless (Gradescope-ready):**
    Autograder chạy trên máy chủ linux không có màn hình hiển thị đồ họa. Nhờ cờ `render = false` được truyền vào từ `interactWithInputString()`, vòng lặp game sẽ tự động **bỏ qua toàn bộ các thao tác vẽ giao diện** (vẽ HUD, hiển thị Seed, render lưới gạch vật lý) [326]. 
    Sự phân tách rạch ròi này giúp hệ thống của em đạt điểm tuyệt đối autograder mà không bao giờ lo ném ra ngoại lệ chí mạng `HeadlessException` [352]!
