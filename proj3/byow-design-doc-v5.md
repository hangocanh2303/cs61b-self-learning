# BYOW Design Document (Phiên bản v6 - Kiến trúc Mọc nhánh Hữu cơ & Đồng nhất hóa Cấu trúc)
**Tác giả:** Anh Ha
**Khóa học:** CS 61B - UC Berkeley

---

## 1. Triết lý Thiết kế: Lập trình Chiến lược (Strategic Programming)

Trong thiết kế phần mềm, đặc biệt là các hệ thống có độ phức tạp cao như đồ án sinh thế giới ngẫu nhiên **BYOW (Build Your Own World)** [242], ranh giới giữa một hệ thống thanh thoát và một hệ thống hỗn loạn nằm ở **tư duy kiến trúc vĩ mô** [257]. 

Ở các phiên bản trước, chúng ta đã đi qua nhiều mô hình:
- *v1/v2:* Sinh phòng ngẫu nhiên rồi tìm cách dò đường nối chúng (gặp lỗi hành lang chạy chồng chéo chi chiết "lung tung" như tệp `result1.PNG` cũ).
- *v3/v4:* Nâng cấp thuật toán kết nối tuyến tính (Linear Sorting) và biểu diễn hành lang dưới dạng một chuỗi điểm `List<Position> path`. Tuy nhiên, mô hình này vẫn gặp phải rào cản phức tạp khi tính toán va chạm động và dò tìm điểm nối.

Trong phiên bản **v6** này, chúng ta thực hiện một bước đột phá lớn: áp dụng thuật toán **Cây phát triển sinh nhánh hữu cơ (Growing Tree / Branching Room Algorithm)** [244] phối hợp với triết lý **Đồng nhất hóa cấu trúc hình học (Geometric Unification)**. Chúng ta xóa bỏ hoàn toàn ranh giới phức tạp giữa `Room` và `Hallway`, quy tất cả về một lớp biểu diễn duy nhất: **`Room`** [256]. Một hành lang (Hallway) thực chất chỉ là một căn phòng siêu hẹp có chiều rộng hoặc chiều cao bằng `3` ô gạch (đảm bảo lòng sàn rộng `1` và có `2` ô tường bao bọc bên ngoài) [260].

Sự cải tiến này mang lại các giá trị kỹ nghệ cốt lõi:
1.  **Che giấu thông tin triệt để (Information Hiding):** Toàn bộ các phép toán va chạm phức tạp được đóng gói bên trong lớp `Room`, giải phóng lớp `MapGenerator` khỏi các chi tiết hình học cấp thấp [13].
2.  **Liên thông tuyệt đối (Guaranteed Connectivity):** Vì mọi thực thể mới (phòng hoặc hành lang) bắt buộc phải được "mọc" ra trực tiếp từ biên của một thực thể đã tồn tại trước đó, thế giới game của chúng ta mặc định là một Đồ thị liên thông hoàn hảo (Single Connected Component) [244]. Chúng ta hoàn toàn triệt tiêu được bài toán kiểm tra liên thông đồ thị bằng DFS/BFS hay Disjoint-Sets phức tạp [244]!
3.  **Thiết kế phòng thủ (Defensive Design):** Không còn tình trạng sinh hành lang chạy đè lên phòng có sẵn rồi mới sửa lỗi. Chúng ta chủ động kiểm tra va chạm của "ứng viên" (candidate) trước khi đưa vào bản đồ. Nếu va chạm, ứng viên bị loại bỏ ngay lập tức [258].

---

## 2. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (Classes and Data Structures)

```
                      +-------------------+
                      |  WorldComponent   | <--- Interface
                      +-------------------+
                                ^
                                | (implements)
                         +------+------+
                         |             |
                 +---------------+     |
                 |     Room      | ----+ (Hallway được đồng nhất hóa thành Room hẹp)
                 +---------------+
```

### WorldComponent (Interface)
Giao diện chung cho mọi thực thể logic có khả năng tự kết xuất đồ họa lên bản đồ [40].

```java
public interface WorldComponent {
    /**
     * Tự vẽ thành phần lên lưới gạch 2D của thế giới.
     * Thừa hành nguyên tắc: "Đối tượng nào giữ dữ liệu thì tự thực hiện hành vi liên quan".
     */
    void draw(TETile[][] world);
}
```

---

### Position
Lớp đóng gói tọa độ `(x, y)` trên lưới tọa độ, được trang bị hai phương thức `equals` và `hashCode` chuẩn mực để hỗ trợ các phép toán bảng băm độ phức tạp $O(1)$ [6, 290].

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

### Room (Unified Representation)
Đại diện cho cả phòng lớn và các phân đoạn hành lang hẹp (kích thước bao gồm cả tường biên) [260].

#### Các trường (Fields):
1.  `private final Position bottomLeft`: Tọa độ góc dưới bên trái.
2.  `private final int width`: Chiều rộng tổng thể (bao gồm tường).
3.  `private final int height`: Chiều cao tổng thể (bao gồm tường).

#### Các phương thức (Methods):
*   `public boolean overlaps(Room other)`: Kiểm tra chồng lấn bằng phép so sánh bounding box $O(1)$ cực kỳ nhanh [261].
*   `public void drawFloor(TETile[][] world)`: Chỉ rải gạch sàn `Tileset.FLOOR` cho vùng lõi đi lại của thực thể.
    *   *Đối với phòng lớn ($w > 3, h > 3$):* Chỉ vẽ sàn ở lòng lõi `[x+1, x+w-2]` và `[y+1, y+h-2]`.
    *   *Đối với hành lang dọc ($w = 3$):* Trải sàn toàn bộ cột lõi giữa từ hàng đáy `y` đến hàng đỉnh `y+h-1`.
    *   *Đối với hành lang ngang ($h = 3$):* Trải sàn toàn bộ hàng lõi giữa từ cột trái `x` đến cột phải `x+w-1`.

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

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Position getBottomLeft() { return bottomLeft; }

    public Position getCenter() {
        return new Position(bottomLeft.getX() + width / 2,
                            bottomLeft.getY() + height / 2);
    }

    public boolean overlaps(Room other) {
        int x1 = bottomLeft.getX();
        int y1 = bottomLeft.getY();
        Position otherPos = other.getBottomLeft();
        int x2 = otherPos.getX();
        int y2 = otherPos.getY();
        int w2 = other.getWidth();
        int h2 = other.getHeight();

        return (x1 < x2 + w2) && (x1 + width > x2) &&
               (y1 < y2 + h2) && (y1 + height > y2);
    }

    /** Vẽ sàn gạch FLOOR trơn tru cho đối tượng, chuẩn bị cho bước bo tường toàn cục. */
    public void drawFloor(TETile[][] world) {
        int x = bottomLeft.getX();
        int y = bottomLeft.getY();

        if (width == 3) {
            // Hành lang dọc: Sàn chạy dọc suốt chiều cao ở cột giữa
            for (int row = y; row < y + height; row++) {
                world[x + 1][row] = Tileset.FLOOR;
            }
        } else if (height == 3) {
            // Hành lang ngang: Sàn chạy ngang suốt chiều rộng ở hàng giữa
            for (int col = x; col < x + width; col++) {
                world[col][y + 1] = Tileset.FLOOR;
            }
        } else {
            // Phòng lớn: Chỉ vẽ sàn ở lòng trong căn phòng
            for (int col = x + 1; col < x + width - 1; col++) {
                for (int row = y + 1; row < y + height - 1; row++) {
                    world[col][row] = Tileset.FLOOR;
                }
            }
        }
    }

    @Override
    public void draw(TETile[][] world) {
        // Phương thức này sẽ được gọi ở lượt biên dịch thế giới (drawFloor trước, bo tường sau)
        drawFloor(world);
    }
}
```

---

## 3. Thiết kế Thuật toán Sinh Thế giới Mọc Nhánh (The Branching Algorithm)

Chúng ta loại bỏ hoàn toàn việc phân rã theo thời gian (Temporal Decomposition) và lập trình chắp vá [14]. Quy trình sinh thế giới được gói gọn trong thuật toán phát triển hữu cơ thông qua một danh sách hoạt động **`activeRooms` (Fringe)** tương tự như cơ chế của thuật toán BFS/DFS [311].

```
[Phòng mầm (Seed)]
       │
       ├──► (Hướng: PHẢI) ──► [Hành lang candidate] ──► [Phòng con candidate]
       │                            │                           │
       │                      (inBounds &                 (inBounds &
       │                     !overlaps?)                 !overlaps?)
       │                            │                           │
       │                            ▼                           ▼
       └──────────────────── CHẤP NHẬN VÀ LƯU ──────────────────┘
```

### Chi tiết các bước thuật toán:

1.  **Khởi tạo:**
    *   Đặt một phòng mầm `seedRoom` ở chính giữa bản đồ thế giới [239].
    *   Thêm `seedRoom` vào hai danh sách: `allComponents` (lưu trữ vật lý) và `activeRooms` (danh sách Fringe để rẽ nhánh) [311].
2.  **Vòng lặp sinh nhánh (Growing Tree Loop):**
    Chừng nào số lượng phòng chưa đạt yêu cầu và danh sách `activeRooms` vẫn còn phần tử:
    *   Chọn ngẫu nhiên một phòng `parent` từ `activeRooms` làm gốc rẽ nhánh.
    *   Chọn ngẫu nhiên một trong 4 hướng di chuyển: `0 = UP, 1 = DOWN, 2 = LEFT, 3 = RIGHT`.
    *   Sinh ngẫu nhiên độ dài hành lang `hallwayLength` và kích thước phòng con mới `childW, childH`.
    *   **Tính toán tọa độ ứng viên (Candidates Computation):**
        Sử dụng công thức tịnh tiến gối biên `1-tile overlap` để hành lang bám sát vào tường biên của phòng parent, đồng thời tự động đục cửa khi rải gạch sàn:
        *   **UP (0):**
            *   `hallway` xuất phát tại: `(centerX - 1, py + ph - 1)`, kích thước: `3 x length`.
            *   `child` xuất phát tại: `(hCenterX - cw / 2, hy + hh - 1)`.
        *   **DOWN (1):**
            *   `hallway` xuất phát tại: `(centerX - 1, py - length + 1)`, kích thước: `3 x length`.
            *   `child` xuất phát tại: `(hCenterX - cw / 2, hy - ch + 1)`.
        *   **LEFT (2):**
            *   `hallway` xuất phát tại: `(px - length + 1, centerY - 1)`, kích thước: `length x 3`.
            *   `child` xuất phát tại: `(hx - cw + 1, hCenterY - ch / 2)`.
        *   **RIGHT (3):**
            *   `hallway` xuất phát tại: `(px + pw - 1, centerY - 1)`, kích thước: `length x 3`.
            *   `child` xuất phát tại: `(hx + hw - 1, hCenterY - ch / 2)`.
    *   **Kiểm định Không gian (Collision & Boundary Check):**
        *   Kiểm tra xem cả `candidateHallway` và `candidateChild` có nằm trọn vẹn trong biên của bản đồ thế giới hay không (`inBounds`).
        *   Kiểm tra xem hai ứng viên này có chồng lấn (`overlaps`) với bất kỳ thực thể nào đã được chấp nhận trong `allComponents` trước đó hay không.
    *   **Chấp nhận hoặc Đào thải (Acceptance or Backtracking):**
        *   *Nếu Hợp lệ:* Thêm cả hai ứng viên vào `allComponents`. Thêm `candidateChild` vào `activeRooms` để tiếp tục làm mầm phát triển thế giới.
        *   *Nếu Thất bại:* Tăng biến đếm thử nghiệm. Nếu một phòng `parent` đã thử rẽ nhánh nhiều lần (ví dụ: sau 30 lượt thử toàn cục) mà vẫn thất bại do không gian xung quanh đã bị lấp đầy, tiến hành loại bỏ `parent` khỏi `activeRooms` để nhường không gian cho các phòng thoáng hơn, ngăn chặn tuyệt đối lỗi nghẽn bản đồ hoặc vòng lặp vô tận.

---

## 4. Biên dịch Thế giới và Vẽ Tường Toàn Cục (Global Wall Generation)

Để giải quyết triệt để lỗi "tường hành lang đâm xuyên lòng phòng" và lỗi "tường chặn lối vào phòng" của các phiên bản trước [297], thuật toán v6 áp dụng cơ chế **Vẽ sàn trước, Dựng tường sau (Global Decoupled Rendering)** [14].

1.  **Bước 1: Rải mảng NOTHING**
    Khởi tạo toàn bộ mảng `world` là `Tileset.NOTHING` [239].
2.  **Bước 2: Trải SÀN toàn bộ thực thể**
    Duyệt qua danh sách `allComponents` và gọi `comp.drawFloor(world)`. Do các hành lang và phòng lớn gối biên `1` ô lên nhau tại các lối ra vào, toàn bộ thế giới lúc này sẽ là một **mạng lưới sàn `FLOOR` liên thông mượt mà, thông suốt**, hoàn toàn không có bất kỳ một bức tường nào chắn lối di chuyển của người chơi.
3.  **Bước 3: Bo TƯỜNG toàn cục (Global 8-Neighbor Scan)**
    Duyệt qua từng tọa độ `(col, row)` trên mảng `world`:
    *   Nếu ô gạch hiện tại đang là **`Tileset.NOTHING`**:
        *   Kiểm tra lân cận 8 hướng (trên, dưới, trái, phải, và 4 hướng chéo) của ô này [286].
        *   Nếu phát hiện **ít nhất một ô lân cận là `Tileset.FLOOR`**:
            *   Gán ô gạch hiện tại thành **`Tileset.WALL`**.
    *   *Tại sao phương pháp này hoàn hảo?*
        *   Nó đảm bảo toàn bộ đường biên của thế giới game (bao gồm cả các góc cua chéo 45 độ của hành lang chữ L) được bọc kín hoàn toàn bằng tường gạch, triệt tiêu lỗi rò rỉ đồ họa chéo [286].
        *   Nó tuyệt đối không dựng bất kỳ bức tường thừa thãi nào bên trong lòng phòng lớn hay lòng hành lang, vì bộ lọc chỉ dựng tường trên ô `NOTHING`.

---

## 5. Phân tích Hiệu năng (Complexity and Performance Analysis)

### Độ phức tạp thời gian (Time Complexity)
*   **Vòng lặp sinh nhánh:** Với $R$ căn phòng mục tiêu, thuật toán sinh chính xác $R$ căn phòng và $R-1$ hành lang. Số lần kiểm tra va chạm tối đa cho mỗi ứng viên là $O(R)$. Tổng chi phí kiểm tra va chạm toàn cục là $O(R^2)$. Với $R \le 20$, phép toán này chạy trong chưa đầy $1\text{ms}$.
*   **Trải sàn thế giới:** Duyệt qua danh sách thực thể để rải sàn, chi phí tỷ lệ thuận với tổng diện tích các phòng, tức là $O(R \cdot (W_{avg} \cdot H_{avg}))$.
*   **Quét tường toàn cục:** Duyệt qua ma trận tĩnh $80 \times 60$ để kiểm tra lân cận 8 hướng. Chi phí là tuyến tính tuyệt đối **$O(\text{WIDTH} \cdot \text{HEIGHT})$**.
*   **Tổng kết:** Thời gian chạy tổng thể đạt mức tối ưu **$O(W \cdot H + R^2)$**, đảm bảo bản đồ thế giới được tạo lập hoàn hảo, không vết nứt đồ họa chỉ trong chớp mắt khi khởi động game!
