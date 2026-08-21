# BYOW Technical Design Document (Phiên bản v10 - Hiện thực chi tiết thuật toán Cây mọc nhánh Bước 1 & Bước 2)
**Tác giả:** Anh Ha  
**Phiên bản:** v10 (Chi tiết hiện thực hóa Bước 1 & Bước 2 trong Cây mọc nhánh hữu cơ)  
**Khóa học:** CS 61B - UC Berkeley  

---

## 1. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (Classes and Data Structures)

Để quản lý độ phức tạp ngày càng tăng của thế giới game trong BYOW và tránh lỗi thiết kế **Primitive Obsession** (Ám ảnh kiểu nguyên thủy), chúng ta xây dựng hệ thống dựa trên sự phân cấp rõ rệt giữa **Thực thể logic vĩ mô (Logical Entities)** và **Khối cấu trúc vật lý vi mô (Physical Tiles)** [28.1, 28.2].

Trong phiên bản v10 này, chúng ta tiếp tục củng cố mô hình tối giản tuyệt đối: **Đồng nhất hóa Thực thể (Homogeneous Representation)** [28.3]. Chúng ta xóa bỏ hoàn toàn lớp `Hallway` độc lập [28.2]. Về mặt vật lý rời rạc trên lưới, hành lang thực chất chỉ là một căn phòng siêu hẹp có chiều rộng hoặc chiều cao là 3 ô gạch (để đảm bảo lòng trong là 1 ô sàn đi được và bọc 2 ô tường hai bên) [v3]. Sự đồng nhất này cho phép chúng ta đơn giản hóa cấu trúc dữ liệu thế giới thành một danh sách duy nhất các đối tượng `Room`, tận dụng tối đa khả năng tái sử dụng mã nguồn [27.5, 28.3].

### Sơ đồ phân cấp cấu trúc hệ thống:
```
                      +-------------------+
                      |  WorldComponent   | <--- Interface [8.30]
                      +-------------------+
                                ^
                                | (implements)
                        +---------------+
                        |     Room      | <--- (Gánh vác cả Room và Hallway)
                        +---------------+
                                ^
                                | (được quản lý bởi)
                        +---------------+
                        |  MapGenerator | <--- (Chứa allComponents & activeRooms)
                        +---------------+
```

---

### Position
Lớp immutable (bất biến) đại diện cho tọa độ `(x, y)` của một ô gạch trên bản đồ thế giới [v3].

#### Các trường (Fields):
1.  `private final int x`: Tọa độ trục hoành (cột).
2.  `private final int y`: Tọa độ trục tung (hàng).

#### Các phương thức (Methods):
*   `public int getX()` và `public int getY()`: Hàm getter chuẩn mực bảo vệ dữ liệu.
*   `@Override public boolean equals(Object o)`: So sánh giá trị tọa độ sử dụng `instanceof` an toàn [10.2].
*   `@Override public int hashCode()`: Được ghi đè đồng bộ bằng `Objects.hash(x, y)` để hỗ trợ lưu trữ trong các bảng băm như `HashSet` phục vụ tìm kiếm va chạm và xử lý di chuyển của nhân vật với độ phức tạp $O(1)$ [10.2].

```java
import java.util.Objects;

public class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position position = (Position) o;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
```

---

### Room (Thừa hành cả vai trò Căn phòng và Hành lang)
Lớp đại diện cho một khối hình chữ nhật vật lý trên lưới bản đồ. `Room` đóng gói toàn bộ kích thước của nó bao gồm cả tường biên (Wall-inclusive Bounding Box) [28.3].

#### Các trường (Fields):
1.  `private final Position bottomLeft`: Tọa độ góc dưới bên trái của khối.
2.  `private final int width`: Chiều rộng tổng thể (gồm cả tường).
3.  `private final int height`: Chiều cao tổng thể (gồm cả tường).

#### Các phương thức (Methods):
*   `public Position getCenter()`: Trả về tọa độ tâm hình học để định tuyến rẽ nhánh [28.3].
*   `public boolean overlaps(Room other)`: Kiểm tra chồng lấn hình học giữa hai khối chữ nhật trên cả 2 trục hoành và tung bằng toán tử logic `&&` [28.3].
*   `public boolean contains(Position p)`: Kiểm tra xem một tọa độ điểm có nằm lọt vào lòng khối này hay không.
*   `@Override public void draw(TETile[][] world)`: Duyệt lưới cục bộ để rải gạch sàn `TETile` ở lõi và gạch tường ở đường biên [28.3].

```java
public class Room implements WorldComponent {
    private final Position bottomLeft;
    private final int width;
    private final int height;

    public Room(Position pos, int w, int h) {
        this.bottomLeft = pos;
        this.width = w;
        this.height = h;
    }

    public Position getCenter() {
        return new Position(bottomLeft.getX() + width / 2,
                            bottomLeft.getY() + height / 2);
    }

    public boolean overlaps(Room other) {
        int x1 = bottomLeft.getX();
        int y1 = bottomLeft.getY();
        Position otherBL = other.getBottomLeft();
        int x2 = otherBL.getX();
        int y2 = otherBL.getY();
        
        return (x1 < x2 + other.getWidth()) &&
               (x1 + width > x2) &&
               (y1 < y2 + other.getHeight()) &&
               (y1 + height > y2);
    }

    public boolean contains(Position p) {
        int px = p.getX();
        int py = p.getY();
        int rx = bottomLeft.getX();
        int ry = bottomLeft.getY();
        return (px >= rx && px < rx + width && py >= ry && py < ry + height);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Position getBottomLeft() { return bottomLeft; }

    @Override
    public void draw(TETile[][] world) {
        int x = bottomLeft.getX();
        int y = bottomLeft.getY();
        int topY = y + height;
        int topX = x + width;

        for (int row = y; row < topY; row += 1) {
            for (int col = x; col < topX; col += 1) {
                // Xác định đường biên của phòng
                if (row == y || row == topY - 1 || col == x || col == topX - 1) {
                    // CHỈ dựng gạch tường WALL nếu ô đó đang là NOTHING trống rỗng
                    if (world[col][row].equals(Tileset.NOTHING)) {
                        world[col][row] = Tileset.WALL;    
                    }
                } else {
                    // Đổ gạch sàn FLOOR ở phần lòng lõi phòng
                    world[col][row] = Tileset.FLOOR;
                }
            }
        }
    }
}
```

---

## 2. Thiết kế Thuật toán Cây mọc nhánh hữu cơ (Growing Tree Generation)

Thuật toán v10 chuyển dịch hoàn toàn từ tư duy đệ quy tuyến tính (vốn dễ gây lỗi Stack Overflow) sang thuật toán **Duyệt biên lặp kiểm soát bằng danh sách Fringe (`activeRooms`)** [v6]. Bản đồ game sẽ phát triển một cách hữu cơ, mọc các nhánh hành lang và phòng con từ các thực thể đã tồn tại một cách cực kỳ an toàn và mạch lạc [v6].

### Các bước thuật toán chi tiết và hiện thực mã nguồn Java:

### Bước 1: Khởi tạo Phòng mầm (Seed Room / Root Node)
1.  **Tính toán kích thước & căn lề:** Để thế giới phát triển cân đối và đẹp mắt nhất, căn phòng mầm `seedRoom` được khởi tạo có kích thước lớn ngẫu nhiên trong khoảng $[6, 12]$ cho cả chiều rộng và chiều cao [v6].
2.  **Định vị tâm bản đồ:** Tọa độ của phòng mầm được tính toán sao cho nó nằm chính xác ở trung tâm lưới $WIDTH \times HEIGHT$ (80x60) nhằm tránh bế tắc không gian mọc nhánh quá sớm ở các cạnh biên [v6]:
    $$\text{seedPos.x} = \frac{WIDTH}{2} - \frac{seedW}{2}$$
    $$\text{seedPos.y} = \frac{HEIGHT}{2} - \frac{seedH}{2}$$
3.  **Khởi tạo Fringe:** Thêm `seedRoom` vào danh sách cấu thành thế giới `allComponents` và danh sách tiền tuyến `activeRooms` (Frontier/Fringe) để bắt đầu chu kỳ mọc nhánh [v6].

### Bước 2: Vòng lặp duyệt Frontier kiểm soát va chạm chủ động
Vòng lặp `while` hoạt động trên danh sách Fringe `activeRooms`. Ở mỗi bước lặp, hệ thống bốc ngẫu nhiên một phòng mầm (parent) từ Frontier, sau đó thử rẽ nhánh theo cả 4 hướng ngẫu nhiên đã shuffle [v6]. Nếu mọc nhánh thành công, cặp (Hành lang + Phòng con) mới sẽ được thêm vào thế giới, đồng thời phòng con trở thành mầm rẽ tiếp theo. 

Nếu tất cả hướng rẽ đều va chạm, phòng mẹ bị loại bỏ hoàn toàn khỏi Fringe (Backtracking hữu cơ) [v6].

#### Hiện thực Java chuẩn mực cho phương thức điều phối `generate()`:

```java
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class MapGenerator {
    public static final int WIDTH = 80;
    public static final int HEIGHT = 60;
    private final TETile[][] tiles;
    private final Random RANDOM;
    
    // Khống chế số lượng phòng mong muốn để đạt mật độ bản đồ cân đối
    private static final int TARGET_ROOM_COUNT = 15; 
    private static final int MAX_ATTEMPTS = 1000; // Chốt chặn bảo vệ tránh vòng lặp vô hạn

    public MapGenerator(long seed) {
        this.tiles = new TETile[WIDTH][HEIGHT];
        this.RANDOM = new Random(seed);
        fillWithEmptyTiles(); // Lấp đầy mảng bằng Tileset.NOTHING ban đầu
    }

    public TETile[][] generate() {
        // =================================================================
        // --- BƯỚC 1: KHỞI TẠO PHÒNG MẦM (SEED ROOM / ROOT NODE) ---
        // =================================================================
        
        // allComponents lưu trữ mọi Room vật lý (cả phòng lớn và hành lang siêu hẹp)
        List<Room> allComponents = new ArrayList<>();
        
        // activeRooms đóng vai trò là Fringe (Frontier) lưu các phòng có thể rẽ nhánh
        List<Room> activeRooms = new ArrayList<>(); 

        // Sinh căn phòng mầm đầu tiên ở chính giữa bản đồ thế giới
        int seedW = RandomUtils.uniform(RANDOM, 6, 12);
        int seedH = RandomUtils.uniform(RANDOM, 6, 12);
        Position seedPos = new Position(WIDTH / 2 - seedW / 2, HEIGHT / 2 - seedH / 2);
        Room seedRoom = new Room(seedPos, seedW, seedH);

        // Đưa phòng mầm vào danh sách thế giới và đặt nó làm mầm Frontier đầu tiên
        allComponents.add(seedRoom);
        activeRooms.add(seedRoom);

        // =================================================================
        // --- BƯỚC 2: VÒNG LẶP DUYỆT FRONTIER KIỂM SOÁT VA CHẠM CHỦ ĐỘNG ---
        // =================================================================
        int attempts = 0;
        
        // Mỗi lần rẽ nhánh thành công tạo ra 1 hành lang và 1 phòng con.
        // Để có TARGET_ROOM_COUNT phòng lớn, tổng số thực thể (gồm hành lang) sẽ là:
        int targetComponentsCount = TARGET_ROOM_COUNT * 2 - 1;

        while (allComponents.size() < targetComponentsCount 
               && !activeRooms.isEmpty() 
               && attempts < MAX_ATTEMPTS) {
            attempts++;

            // 2a. Chọn ngẫu nhiên một phòng mầm (parent) từ Frontier đang hoạt động
            int parentIdx = RandomUtils.uniform(RANDOM, 0, activeRooms.size());
            Room parent = activeRooms.get(parentIdx);

            // 2b. Thử mọc một nhánh mới (Hành lang + Phòng con) từ phòng mẹ này
            BranchResult result = tryCreateBranch(parent, allComponents);

            if (result != null) {
                // NỐI NHÁNH THÀNH CÔNG: Cả hai ứng viên đều không va chạm
                allComponents.add(result.hallway);
                allComponents.add(result.childRoom);
                
                // Phòng con mới sẽ là tiền tuyến mọc nhánh tiếp theo
                activeRooms.add(result.childRoom); 
            } else {
                // THẤT BẠI PHÒNG THỦ: Phòng mẹ hiện tại đã bị bao vây cô lập ở cả 4 hướng
                // Ta loại bỏ nó khỏi Fringe để giải phóng không gian cho các phòng thoáng hơn
                activeRooms.remove(parent);
            }
        }

        // =================================================================
        // --- BƯỚC 3: KẾT XUẤT ĐỒ HỌA TOÀN CỤC ---
        // =================================================================
        for (Room comp : allComponents) {
            comp.draw(tiles);
        }

        return tiles;
    }
}
```

---

## 3. Hiện thực Chi tiết Chiến lược Shuffling Hướng Phòng thủ

Phương thức `tryCreateBranch` đóng vai trò là một **Mô-đun sâu (Deep Module)**, tự đóng gói toàn bộ quy trình shuffling và rẽ nhánh nhằm đảm bảo một căn phòng mẹ chỉ bị trục xuất khỏi danh sách tiền tuyến `activeRooms` khi và chỉ khi **tất cả 4 hướng mọc nhánh đều bị bế tắc**.

Dưới đây là mã nguồn Java hoàn chỉnh và chuẩn mực cho phương thức này:

```java
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MapGenerator {
    // ... Các trường dữ liệu logic khác (WIDTH, HEIGHT, RANDOM...) ...

    // Lớp helper đóng gói cặp thực thể rẽ nhánh thành công
    private static class BranchResult {
        final Room hallway;
        final Room childRoom;

        BranchResult(Room hallway, Room childRoom) {
            this.hallway = hallway;
            this.childRoom = childRoom;
        }
    }

    /**
     * Thử mọc nhánh từ phòng parent. Duyệt ngẫu nhiên cả 4 hướng.
     * Chỉ trục xuất parent ra khỏi Fringe nếu TẤT CẢ các hướng đều bế tắc.
     */
    private BranchResult tryCreateBranch(Room parent, List<Room> allComponents) {
        // 1. Khởi tạo danh sách hướng: 0: UP, 1: DOWN, 2: LEFT, 3: RIGHT
        List<Integer> directions = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        
        // 2. XÁO TRỘN NGẪU NHIÊN: Tạo tính ngẫu nhiên bất ngờ cho bố cục hầm ngục
        Collections.shuffle(directions, RANDOM);

        // 3. DUYỆT TUẦN TỰ QUA CÁC HƯỚNG ĐÃ SHUFFLE
        for (int direction : directions) {
            int hallwayLength = RandomUtils.uniform(RANDOM, 4, 10);
            int childW = RandomUtils.uniform(RANDOM, 5, 10);
            int childH = RandomUtils.uniform(RANDOM, 5, 10);

            // Sinh ứng viên hành lang và phòng con dựa trên công thức tịnh tiến gối biên
            Room candidateHallway = createHallway(parent, direction, hallwayLength);
            Room candidateChild = createNeighborRoom(candidateHallway, direction, childW, childH);

            // 4. KIỂM TRA ĐIỀU KIỆN BIÊN & VA CHẠM KHÔNG GIAN (Early Return)
            if (inBounds(candidateHallway) && inBounds(candidateChild)
                    && !overlapsAny(allComponents, candidateHallway)
                    && !overlapsAny(allComponents, candidateChild)) {
                
                // HỢP LỆ: Trả về kết quả và dừng duyệt hướng ngay lập tức (Early Exit)
                return new BranchResult(candidateHallway, candidateChild);
            }
        }

        // 5. BẾ TẮC HOÀN TOÀN: Thử hết cả 4 hướng đều không thể mọc nhánh
        return null;
    }

    private boolean overlapsAny(List<Room> existing, Room candidate) {
        for (Room r : existing) {
            if (r.overlaps(candidate)) {
                return true;
            }
        }
        return false;
    }
}
```

---

## 4. Công thức Hình học Tọa độ của `createHallway` & `createNeighborRoom`

Để hai phân đoạn kết nối thông suốt, chúng ta thiết lập nguyên tắc **chồng lấn biên (1-tile border overlap)**: Tường biên của hành lang sẽ đè khít lên tường biên của phòng [v3].

Giả sử phòng mẹ (`parent`) có tọa độ góc dưới bên trái là `(px, py)`, kích thước `(pw, ph)`.  
Tâm hình học của phòng mẹ là:  
$$	ext{centerX} = px + pw / 2$$  
$$	ext{centerY} = py + ph / 2$$  

Hành lang có chiều dài sinh ngẫu nhiên là `L`. Phòng con (`child`) có kích thước ngẫu nhiên là `(cw, ch)`.

### ➡️ Hướng 3: RIGHT (Rẽ sang Phải)
*   **Hành lang Candidate (Ngang):**
    *   `bottomLeft.x = px + pw - 1` (Gối biên phải phòng mẹ).
    *   `bottomLeft.y = centerY - 1` (Dịch chuyển tung độ xuống 1 ô để ô sàn trung tâm của hành lang—hàng giữa của 3 hàng—nằm chính xác tại cao độ `centerY` của tâm phòng mẹ).
    *   `width = L`, `height = 3`.
*   **Phòng con Candidate:**
    *   `bottomLeft.x = (px + pw - 1) + L - 1` (Gối biên phải hành lang).
    *   `bottomLeft.y = centerY - ch / 2` (Căn giữa phòng con theo trục ngang của hành lang).
    *   `width = cw`, `height = ch`.

---

### ⬅️ Hướng 2: LEFT (Rẽ sang Trái)
*   **Hành lang Candidate (Ngang):**
    *   `bottomLeft.x = px - L + 1` (Lùi sang trái một khoảng `L` trừ đi 1 ô gối biên).
    *   `bottomLeft.y = centerY - 1` (Đảm bảo sàn hành lang thẳng hàng tâm phòng mẹ).
    *   `width = L`, `height = 3`.
*   **Phòng con Candidate:**
    *   `bottomLeft.x = (px - L + 1) - cw + 1` (Lùi tiếp một khoảng bằng chiều rộng phòng con gối biên).
    *   `bottomLeft.y = centerY - ch / 2` (Căn giữa phòng con).
    *   `width = cw`, `height = ch`.

---

### ⬆️ Hướng 0: UP (Rẽ lên Trên)
*   **Hành lang Candidate (Dọc):**
    *   `bottomLeft.x = centerX - 1` (Dịch chuyển hoành độ sang trái 1 ô để ô sàn dọc trung tâm của hành lang—cột giữa của 3 cột—nằm chính xác tại cột `centerX` của tâm phòng mẹ).
    *   `bottomLeft.y = py + ph - 1` (Gối biên trên phòng mẹ).
    *   `width = 3`, `height = L`.
*   **Phòng con Candidate:**
    *   `bottomLeft.x = centerX - cw / 2` (Căn giữa phòng con).
    *   `bottomLeft.y = (py + ph - 1) + L - 1` (Gối biên trên hành lang).
    *   `width = cw`, `height = ch`.

---

### ⬇️ Hướng 1: DOWN (Rẽ xuống Dưới)
*   **Hành lang Candidate (Dọc):**
    *   `bottomLeft.x = centerX - 1` (Sàn dọc hành lang trùng khớp hoành độ `centerX`).
    *   `bottomLeft.y = py - L + 1` (Lùi xuống dưới một khoảng `L` trừ đi 1 ô gối biên).
    *   `width = 3`, `height = L`.
*   **Phòng con Candidate:**
    *   `bottomLeft.x = centerX - cw / 2` (Căn giữa phòng con).
    *   `bottomLeft.y = (py - L + 1) - ch + 1` (Lùi tiếp một khoảng bằng chiều cao phòng con gối biên).
    *   `width = cw`, `height = ch`.

---

#### Hiện thực hóa hai phương thức trong Java:

```java
private Room createHallway(Room parent, int direction, int length) {
    Position pPos = parent.getBottomLeft();
    int px = pPos.getX();
    int py = pPos.getY();
    int pw = parent.getWidth();
    int ph = parent.getHeight();
    int centerX = px + pw / 2;
    int centerY = py + ph / 2;

    switch (direction) {
        case 0: // UP
            return new Room(new Position(centerX - 1, py + ph - 1), 3, length);
        case 1: // DOWN
            return new Room(new Position(centerX - 1, py - length + 1), 3, length);
        case 2: // LEFT
            return new Room(new Position(px - length + 1, centerY - 1), length, 3);
        case 3: // RIGHT
            return new Room(new Position(px + pw - 1, centerY - 1), length, 3);
        default:
            throw new IllegalArgumentException("Direction không hợp lệ!");
    }
}

private Room createNeighborRoom(Room hallway, int direction, int cw, int ch) {
    Position hPos = hallway.getBottomLeft();
    int hx = hPos.getX();
    int hy = hPos.getY();
    int hw = hallway.getWidth();
    int hh = hallway.getHeight();
    int hCenterX = hx + hw / 2;
    int hCenterY = hy + hh / 2;

    switch (direction) {
        case 0: // UP
            return new Room(new Position(hCenterX - cw / 2, hy + hh - 1), cw, ch);
        case 1: // DOWN
            return new Room(new Position(hCenterX - cw / 2, hy - ch + 1), cw, ch);
        case 2: // LEFT
            return new Room(new Position(hx - cw + 1, hCenterY - ch / 2), cw, ch);
        case 3: // RIGHT
            return new Room(new Position(hx + hw - 1, hCenterY - ch / 2), cw, ch);
        default:
            throw new IllegalArgumentException("Direction không hợp lệ!");
    }
}
```

---

## 5. Phân tích Hiệu năng (Complexity and Performance Analysis)

### Độ phức tạp thời gian (Time Complexity):
*   **Thuật toán sinh phòng & rẽ nhánh chủ động:** Ở mỗi lượt thử trong số tối đa $M$ (`MAX_ATTEMPTS = 1000`) lượt, chúng ta kiểm tra va chạm chồng lấn (`overlapsAny`) cho hai ứng viên với danh sách các thực thể đã chấp nhận (tối đa $K$ thực thể, $K \le 30$). Phép toán `overlaps()` có độ phức tạp là $O(1)$ [v3]. Do đó, chi phí rẽ nhánh chỉ là $O(M \cdot K)$ trong trường hợp xấu nhất, chạy hoàn toàn tức thời ($<2	ext{ms}$).
*   **Vẽ và hiển thị:** Duyệt qua mảng lưới cố định kích thước thế giới để biên dịch và kết xuất đồ họa. Độ phức tạp là tuyến tính tĩnh $O(WIDTH \cdot HEIGHT)$ ($80 	imes 60$ ô gạch) [174].
*   **Tổng kết:** Đạt độ phức tạp tối ưu **$O(WIDTH \cdot HEIGHT + M \cdot K)$**, đảm bảo thế giới được sinh ra trơn tru ngay lập tức khi khởi động trò chơi mà không gây giật lag đồ họa [174].

### Độ phức tạp không gian (Space Complexity):
*   Chúng ta không lưu trữ đồ thị liên kết hay chạy đệ quy. Ngăn xếp bộ nhớ đệm Stack được giải phóng hoàn toàn, an toàn tuyệt đối trước nguy cơ `StackOverflowError` [v6].
*   Toàn bộ cấu trúc thế giới được lưu trữ trong danh sách tuyến tính `allComponents` có dung lượng cực nhỏ, đạt mức tối ưu không gian **$O(K)$** với $K$ là số lượng thực thể hình học chữ nhật trong game.

---

## 6. Các trường hợp đặc biệt & Giải pháp phòng ngự (Edge Cases and Solutions)

1.  **Sự Fringe bị cạn kiệt quá sớm (Premature Fringe Depletion):**
    *   *Nguy cơ:* Nếu bốc ngẫu nhiên một hướng rẽ và gặp va chạm rồi xóa phòng parent ngay lập tức, Fringe sẽ cạn kiệt rất nhanh, khiến bản đồ chỉ sinh được 2-3 phòng rồi tắc nghẽn [v6].
    *   *Giải pháp:* Triển khai thuật toán xáo trộn hướng (Shuffle Directions) và duyệt tuần tự cả 4 hướng trong `tryCreateBranch`. Chỉ khi căn phòng mẹ thực sự bị bao vây cô lập ở cả 4 hướng, chúng ta mới loại bỏ nó khỏi `activeRooms` [v6].
2.  **Lỗi tràn ranh giới bản đồ (Out of Bounds Exception):**
    *   *Nguy cơ:* Các căn phòng con hoặc hành lang ở sát rìa thế giới có thể cố tình vẽ vượt ra ngoài giới hạn kích thước mảng gạch $80 	imes 60$, ném ra lỗi crash sập game `ArrayIndexOutOfBoundsException` [v4].
    *   *Giải pháp:* Thiết lập phương thức kiểm tra ranh giới nghiêm ngặt `inBounds(Room r)` bảo vệ biên bản đồ tối thiểu 1 ô gạch (chừa khoảng không bao quanh):
        $$	ext{left.x} > 0 \quad 	ext{and} \quad 	ext{right.x} < 	ext{WIDTH} - 1$$
        $$	ext{bottom.y} > 0 \quad 	ext{and} \quad 	ext{top.y} < 	ext{HEIGHT} - 1$$
3.  **Hạt giống ngẫu nhiên cực lớn gây lỗi phân tích cú pháp:**
    *   *Nguy cơ:* Người dùng nhập chuỗi seed khổng lồ vượt quá giới hạn lưu trữ của kiểu số nguyên `int` gây lỗi `NumberFormatException` [FAQ 3].
    *   *Giải pháp:* Đóng gói hạt giống bằng kiểu dữ liệu 64-bit `Long` và phân tích cú pháp thông qua `Long.parseLong(seedString)` để tương thích hoàn hảo với mọi hạt giống ngẫu nhiên khổng lồ của Gradescope [FAQ 3].
