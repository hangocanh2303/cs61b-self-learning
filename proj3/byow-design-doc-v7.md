# BYOW Technical Design Document (Phiên bản v7 - Kiến trúc Cây mọc nhánh hữu cơ & Đồng nhất hóa Thực thể)
**Tác giả:** Anh Ha  
**Khóa học:** CS 61B - UC Berkeley  

---

## 1. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (Classes and Data Structures)

Để giải quyết triệt để sự phức tạp về mặt hình học và lập trình tọa độ của các phiên bản trước, phiên bản v7 áp dụng triết lý **Đồng nhất hóa Thực thể (Homogeneous Representation)** và **Kiến trúc Mô-đun Sâu (Deep Modules)** [28.3]. 

Chúng ta xóa bỏ hoàn toàn sự phân tách phức tạp giữa lớp `Hallway` và lớp `Room` [28.2]. Về mặt vật lý rời rạc trên lưới, hành lang thực chất chỉ là một căn phòng siêu hẹp có chiều rộng hoặc chiều cao là 3 ô gạch (để đảm bảo lòng trong là 1 ô sàn đi được và bọc 2 ô tường hai bên) [v3]. Sự đồng nhất này cho phép chúng ta đơn giản hóa cấu trúc dữ liệu thế giới thành một danh sách duy nhất các đối tượng `Room`, tận dụng tối đa khả năng tái sử dụng mã nguồn [27.5, 28.3].

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

Thuật toán v7 chuyển dịch hoàn toàn từ tư duy đệ quy tuyến tính (vốn dễ gây lỗi Stack Overflow) sang thuật toán **Duyệt biên lặp kiểm soát bằng danh sách Fringe (`activeRooms`)** [v6]. Bản đồ game sẽ phát triển một cách hữu cơ, mọc các nhánh hành lang và phòng con từ các thực thể đã tồn tại một cách cực kỳ an toàn và mạch lạc [v6].

```
                     [ Khởi tạo Seed Room ]
                              │
                              ▼
                     [ Đưa vào activeRooms ] <────────────────┐
                              │                              │
                              ▼                              │
                  Có rẽ nhánh được tiếp? ────(Không)──► [Xóa khỏi Fringe]
                              │
                            (Có)
                              │
                              ▼
                [ Tháo ngẫu nhiên 1 hướng ]
                    (UP, DOWN, LEFT, RIGHT)
                              │
                              ▼
                [ Thử tạo hành lang candidate ]
                              │
                              ▼
               [ Thử tạo phòng con ở đầu hành lang ]
                              │
                              ▼
                     overlapsAny() chồng lấn?
                              │
                   ┌──────────┴──────────┐
                (Không)                 (Có)
                   │                      │
                   ▼                      ▼
           [ Đưa cả 2 vào ]           [ Bỏ qua ]
          [ activeRooms.add ]
```

---

### Các bước thuật toán chi tiết:

### Bước 1: Khởi tạo Phòng mầm (Seed Room)
1.  Sinh một căn phòng mầm lớn `seedRoom` ở chính giữa thế giới để làm gốc rễ phát triển [v6].
2.  Thêm `seedRoom` vào danh sách cấu thành thế giới `allComponents` và danh sách tiền tuyến `activeRooms` (Fringe) [v6].

### Bước 2: Vòng lặp duyệt Frontier kiểm soát va chạm chủ động
Chúng ta chạy vòng lặp lặp `while` trên cấu trúc danh sách Fringe. Vòng lặp sẽ tiếp tục sinh nhánh cho đến khi đạt đủ số phòng mong muốn, hoặc Fringe cạn kiệt, hoặc vượt quá số lần thử tối đa để bảo vệ hệ thống tránh vòng lặp vô hạn [v6]:

```java
while (allComponents.size() < targetComponentsCount && !activeRooms.isEmpty() && attempts < MAX_ATTEMPTS) {
    attempts++;
    
    // 1. Chọn một căn phòng mầm ngẫu nhiên từ tiền tuyến activeRooms (Fringe)
    Room parent = chooseActiveRoom(activeRooms);
    
    // 2. Thử mọc một cặp (Hành lang + Phòng con) từ parent
    BranchResult result = tryCreateBranch(parent, allComponents);
    
    if (result != null) {
        // ĐỦ ĐIỀU KIỆN AN TOÀN: Nối nhánh thành công!
        allComponents.add(result.hallway);
        allComponents.add(result.childRoom);
        activeRooms.add(result.childRoom); // Phòng con mới trở thành mầm rẽ tiếp theo
    } else {
        // Căn phòng parent này đã bị bủa vây hoàn toàn (thử mọi hướng rẽ đều va chạm)
        // Ta loại nó khỏi tiền tuyến activeRooms để chuyển sang nhánh khác thoáng hơn
        activeRooms.remove(parent);
    }
}
```

---

### Bước 3: Thuật toán Shuffling Hướng và sinh Nhánh Phòng thủ (tryCreateBranch)
Để tránh việc một phòng bị **trục xuất quá sớm (Premature Eviction)** khỏi Fringe khi chỉ mới thử 1 hướng rẽ và gặp va chạm, phương thức `tryCreateBranch` phải thực hiện kiểm tra đầy đủ cả 4 hướng theo thứ tự xáo trộn ngẫu nhiên [v6]:

1.  Khởi tạo danh sách hướng: `List<Integer> directions = Arrays.asList(0, 1, 2, 3)` (0: UP, 1: DOWN, 2: LEFT, 3: RIGHT).
2.  Sử dụng `Collections.shuffle(directions, RANDOM)` để xáo trộn hướng, tạo tính ngẫu nhiên bất ngờ cho hang động.
3.  Duyệt qua từng hướng trong danh sách đã shuffle:
    *   Tính toán tọa độ ứng viên hành lang `candidateHallway` dựa trên hướng và khoảng cách Manhattan ngẫu nhiên [v6].
    *   Tính toán tọa độ ứng viên phòng con `candidateChild` nối tiếp vào đầu kia của hành lang [v6].
    *   Kiểm tra xem cả hai ứng viên có nằm trọn trong ranh giới bản đồ thế giới hay không (`inBounds`) [v6].
    *   Kiểm tra xem cả hai ứng viên có chồng lấn lên **bất kỳ thực thể nào đã vẽ từ trước** hay không (`overlapsAny`) [v6].
    *   Nếu cả hai điều kiện trên đều thỏa mãn: Trả về cặp thực thể hợp lệ ngay lập tức và dừng duyệt hướng (Early Exit) [v6].
4.  Nếu duyệt qua cả 4 hướng đều không tìm được lối mọc nhánh trống trải, trả về `null` (lúc này việc trục xuất phòng parent khỏi Fringe mới hoàn toàn chính xác về mặt hình học) [v6].

---

### Bước 4: Đè lớp hiển thị tự động đục cửa (Overlapping Stitching)
Để kết nối thông suốt giữa các căn phòng, chúng ta áp dụng quy tắc **chồng lấn biên 1 ô gạch (1-tile border overlap)** khi tính toán tọa độ dịch chuyển của hành lang và phòng con:
*   Đầu hành lang sẽ đè khít lên 1 hàng tường biên của phòng `parent` [v3].
*   Cuối hành lang sẽ đè khít lên 1 hàng tường biên của phòng `child` [v3].
*   Khi kết xuất bản đồ, mảng thế giới `world` được lấp đầy bằng `Tileset.NOTHING` trước [246]. Sau đó, ta tuần tự vẽ các phòng và hành lang trong `allComponents`.
*   Vì lòng của hành lang chứa sàn `Tileset.FLOOR`, khi vẽ đè lên sau, nó sẽ tự động ghi đè và "đục thủng" hàng tường biên của phòng lớn tại điểm khớp nối, mở ra cửa ra vào cực kỳ thông suốt và hoàn toàn tự nhiên mà không cần viết thuật toán đục lỗ thủ công phức tạp [v3]!

---

## 3. Công thức tính toán tọa độ dịch chuyển (Branching Coordinates)

Giả sử phòng mẹ (`parent`) có tọa độ góc dưới bên trái là `(px, py)`, kích thước `(pw, ph)`.  
Tâm hình học của phòng mẹ là: `centerX = px + pw / 2`, `centerY = py + ph / 2`.  
Hành lang có chiều dài sinh ngẫu nhiên là `L`. Phòng con (`child`) có kích thước ngẫu nhiên là `(cw, ch)`.

### ➡️ Hướng 3: RIGHT (Rẽ sang Phải)
*   **Hành lang Candidate (Ngang):**
    *   `bottomLeft = (px + pw - 1, centerY - 1)` (Gối biên phải phòng mẹ, lùi tung độ xuống 1 ô để sàn hành lang trùng khớp với cao độ `centerY` của tâm phòng mẹ).
    *   `width = L`, `height = 3`.
*   **Phòng con Candidate:**
    *   `bottomLeft = (px + pw + L - 2, centerY - ch / 2)` (Gối biên phải hành lang, căn giữa phòng con theo hành lang).
    *   `width = cw`, `height = ch`.

### ⬅️ Hướng 2: LEFT (Rẽ sang Trái)
*   **Hành lang Candidate (Ngang):**
    *   `bottomLeft = (px - L + 1, centerY - 1)`.
    *   `width = L`, `height = 3`.
*   **Phòng con Candidate:**
    *   `bottomLeft = (px - L - cw + 2, centerY - ch / 2)`.
    *   `width = cw`, `height = ch`.

### ⬆️ Hướng 0: UP (Rẽ lên Trên)
*   **Hành lang Candidate (Dọc):**
    *   `bottomLeft = (centerX - 1, py + ph - 1)` (Sàn hành lang trùng khớp hoành độ `centerX`).
    *   `width = 3`, `height = L`.
*   **Phòng con Candidate:**
    *   `bottomLeft = (centerX - cw / 2, py + ph + L - 2)`.
    *   `width = cw`, `height = ch`.

### ⬇️ Hướng 1: DOWN (Rẽ xuống Dưới)
*   **Hành lang Candidate (Dọc):**
    *   `bottomLeft = (centerX - 1, py - L + 1)`.
    *   `width = 3`, `height = L`.
*   **Phòng con Candidate:**
    *   `bottomLeft = (centerX - cw / 2, py - L - ch + 2)`.
    *   `width = cw`, `height = ch`.

---

## 4. Phân tích Hiệu năng (Complexity and Performance Analysis)

### Độ phức tạp thời gian (Time Complexity):
*   **Thuật toán sinh phòng & rẽ nhánh chủ động:** Ở mỗi lượt thử trong số tối đa $M$ (`MAX_ATTEMPTS = 1000`) lượt, chúng ta kiểm tra va chạm chồng lấn (`overlapsAny`) cho hai ứng viên với danh sách các thực thể đã chấp nhận (tối đa $K$ thực thể, $K \le \text{targetComponentsCount} \times 2 = 30$). Phép toán `overlaps()` có độ phức tạp là $O(1)$ [v3]. Do đó, chi phí rẽ nhánh chỉ là $O(M \cdot K)$ trong trường hợp xấu nhất, chạy hoàn toàn tức thời ($<2\text{ms}$ trên máy ảo của Gradescope) [174].
*   **Vẽ và hiển thị:** Duyệt qua mảng lưới cố định kích thước thế giới để biên dịch và kết xuất đồ họa. Độ phức tạp là tuyến tính tĩnh $O(WIDTH \cdot HEIGHT)$ ($80 \times 60$ ô gạch) [174].
*   **Tổng kết:** Đạt độ phức tạp tối ưu **$O(WIDTH \cdot HEIGHT + M \cdot K)$**, đảm bảo thế giới được sinh ra trơn tru ngay lập tức khi khởi động trò chơi mà không gây giật lag đồ họa [174].

### Độ phức tạp không gian (Space Complexity):
*   Chúng ta không lưu trữ đồ thị liên kết hay chạy đệ quy. Ngăn xếp bộ nhớ đệm Stack được giải phóng hoàn toàn, an toàn tuyệt đối trước nguy cơ `StackOverflowError` [v6].
*   Toàn bộ cấu trúc thế giới được lưu trữ trong danh sách tuyến tính `allComponents` có dung lượng cực nhỏ, đạt mức tối ưu không gian **$O(K)$** với $K$ là số lượng thực thể hình học chữ nhật trong game.

---

## 5. Các trường hợp đặc biệt & Giải pháp phòng ngự (Edge Cases and Solutions)

1.  **Sự Fringe bị cạn kiệt quá sớm (Premature Fringe Depletion):**
    *   *Nguy cơ:* Nếu bốc ngẫu nhiên một hướng rẽ và gặp va chạm rồi xóa phòng parent ngay lập tức, Fringe sẽ cạn kiệt rất nhanh, khiến bản đồ chỉ sinh được 2-3 phòng rồi tắc nghẽn [v6].
    *   *Giải pháp:* Triển khai thuật toán xáo trộn hướng (Shuffle Directions) và duyệt tuần tự cả 4 hướng trong `tryCreateBranch`. Chỉ khi căn phòng mẹ thực sự bị bao vây cô lập ở cả 4 hướng, chúng ta mới loại bỏ nó khỏi `activeRooms` [v6].
2.  **Lỗi tràn ranh giới bản đồ (Out of Bounds Exception):**
    *   *Nguy cơ:* Các căn phòng con hoặc hành lang ở sát rìa thế giới có thể cố tình vẽ vượt ra ngoài giới hạn kích thước mảng gạch $80 \times 60$, ném ra lỗi crash sập game `ArrayIndexOutOfBoundsException` [v4].
    *   *Giải pháp:* Thiết lập phương thức kiểm tra ranh giới nghiêm ngặt `inBounds(Room r)` bảo vệ biên bản đồ tối thiểu 1 ô gạch (chừa khoảng không bao quanh):
        $$\text{left.x} > 0 \quad \text{and} \quad \text{right.x} < \text{WIDTH} - 1$$
        $$\text{bottom.y} > 0 \quad \text{and} \quad \text{top.y} < \text{HEIGHT} - 1$$
3.  **Hạt giống ngẫu nhiên cực lớn gây lỗi phân tích cú pháp:**
    *   *Nguy cơ:* Người dùng nhập chuỗi seed khổng lồ vượt quá giới hạn lưu trữ của kiểu số nguyên `int` gây lỗi `NumberFormatException` [FAQ 3].
    *   *Giải pháp:* Đóng gói hạt giống bằng kiểu dữ liệu 64-bit `Long` và phân tích cú pháp thông qua `Long.parseLong(seedString)` để tương thích hoàn hảo với mọi hạt giống ngẫu nhiên khổng lồ của Gradescope [FAQ 3].
