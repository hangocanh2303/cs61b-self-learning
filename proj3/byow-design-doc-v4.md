# BYOW Design Document (Phiên bản v4 - Chuyển dịch sang kiến trúc Điểm rời rạc tối giản)
**Tác giả:** Anh Ha
**Khóa học:** CS 61B - UC Berkeley

---

## 1. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (Classes and Data Structures)

Để quản lý độ phức tạp ngày càng tăng của thế giới game trong BYOW và tránh lỗi thiết kế **Primitive Obsession** (Ám ảnh kiểu nguyên thủy), chúng ta xây dựng hệ thống dựa trên sự phân cấp rõ rệt giữa **Thực thể logic vĩ mô (Logical Entities)** và **Khối cấu trúc vật lý vi mô (Physical Tiles)** [10]. 

Trong phiên bản v4 này, chúng ta quyết định từ bỏ mô hình dùng `List<Room>` làm hành lang (vốn gây phức tạp hóa tọa độ và xung đột tường bao) để quay lại mô hình tự nhiên và thanh thoát nhất: **Biểu diễn hành lang dưới dạng một đường đi gồm danh sách các tọa độ điểm** [10].

Sơ đồ phân cấp dưới đây thể hiện cấu trúc mô-đun sâu (Deep Modules) và che giấu thông tin (Information Hiding) cực kỳ vững chắc [11]:

```
                      +-------------------+
                      |  WorldComponent   | <--- Interface
                      +-------------------+
                                ^
                                | (implements)
                 +--------------+--------------+
                 |                             |
         +---------------+             +---------------+
         |     Room      |             |    Hallway    |
         +---------------+             +---------------+
                                               | (has-a)
                                               v
                                       List<Position> path
```

---

### WorldComponent (Interface)
Đóng vai trò là **Rào cản trừu tượng (Abstraction Barrier)** chung cho tất cả các thực thể hình học có khả năng hiển thị trên bản đồ thế giới [10, 25].

#### Các phương thức:
*   `void draw(TETile[][] world)`: Quy định giao diện chung cho phép các đối tượng tự vẽ chính mình lên mảng lưới gạch thế giới `world` [10].

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
Lớp đại diện cho tọa độ của một ô gạch `(x, y)` trên lưới tọa độ hai chiều của thế giới game. Lớp này bảo vệ tính đóng gói và an toàn dữ liệu bằng các trường `private final`.

#### Các trường (Fields):
1.  `private final int x`: Tọa độ trục hoành (cột).
2.  `private final int y`: Tọa độ trục tung (hàng).

#### Các phương thức (Methods):
*   `public int getX()` và `public int getY()`: Các hàm getter chuẩn mực để truy xuất dữ liệu an toàn.
*   `@Override public boolean equals(Object o)`: Được ghi đè sử dụng từ khóa `instanceof` an toàn để so sánh tính bằng nhau về mặt giá trị của hai tọa độ, tuân thủ chặt chẽ tính phản xạ, đối xứng, bắc cầu [1].
*   `@Override public int hashCode()`: Được ghi đè đồng bộ với `equals` thông qua `Objects.hash(x, y)` để đảm bảo tính nhất quán khi lưu trữ và truy vấn trong các cấu trúc bảng băm của Java như `HashSet` hay `HashMap` [2].

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

### Room
Lớp đại diện cho một căn phòng hình chữ nhật trong thế giới. Lớp này kế thừa và thực hiện interface `WorldComponent` [25]. `Room` đóng gói toàn bộ kích thước của nó bao gồm cả tường (Wall-inclusive Bounding Box) [11].

#### Các trường (Fields):
1.  `private Position bottomLeft`: Tọa độ góc dưới bên trái của căn phòng.
2.  `private int width`: Chiều rộng tổng thể của phòng (đã tính cả tường bao).
3.  `private int height`: Chiều cao tổng thể của phòng (đã tính cả tường bao).

#### Các phương thức (Methods):
*   `public Position getCenter()`: Trả về một đối tượng `Position` đại diện cho tọa độ tâm hình học của căn phòng, đóng vai trò là "mốc định tuyến" để nối hành lang [10].
*   `public boolean contains(Position p)`: Kiểm tra xem một tọa độ điểm có nằm lọt vào bên trong căn phòng này hay không (bao gồm cả phần biên hoặc chỉ phần lòng sàn tùy mục đích).
*   `public boolean overlaps(Room other)`: Sử dụng phép kiểm tra giao cắt hình học giữa hai hình chữ nhật trên cả hai trục hoành độ và tung độ với các toán tử **`&&` (AND)** để đảm bảo tính chính xác tuyệt đối [10].
*   `@Override public void draw(TETile[][] world)`: Biên dịch dữ liệu logic thành các ô gạch vật lý `TETile` cụ thể (`Tileset.WALL` cho biên và `Tileset.FLOOR` cho lòng lõi phòng) [10].

```java
public class Room implements WorldComponent {
    private Position bottomLeft;
    private int width;
    private int height;

    public Room(Position pos, int w, int h) {
        this.bottomLeft = pos;
        this.width = w;
        this.height = h;
    }

    public Position getCenter() {
        return new Position(bottomLeft.getX() + width / 2,
                            bottomLeft.getY() + height / 2);
    }

    public boolean contains(Position p) {
        int px = p.getX();
        int py = p.getY();
        int rx = bottomLeft.getX();
        int ry = bottomLeft.getY();
        return (px >= rx && px < rx + width && py >= ry && py < ry + height);
    }

    public boolean overlaps(Room other) {
        int x1 = bottomLeft.getX();
        int y1 = bottomLeft.getY();
        Position otherRoomBottomLeft = other.getBottomLeft();
        int x2 = otherRoomBottomLeft.getX();
        int y2 = otherRoomBottomLeft.getY();
        int w2 = other.getWidth();
        int h2 = other.getHeight();
        
        // Hai phòng chỉ chồng lấn khi và chỉ khi đồng thời giao nhau trên cả 2 trục
        return (x1 < x2 + w2) &&
               (x1 + width > x2) &&
               (y1 < y2 + h2) &&
               (y1 + height > y2);
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

        // Vòng lặp sử dụng toán tử so sánh nghiêm ngặt "<" để triệt tiêu lỗi Off-by-one
        for (int row = y; row < topY; row += 1) {
            for (int col = x; col < topX; col += 1) {
                // Xác định đường biên của phòng hình chữ nhật
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

### Hallway
Lớp đại diện cho hành lang lắt léo kết nối các phòng. Đại diện bằng danh sách các điểm tọa độ `List<Position> path` thay vì các khối hình chữ nhật gò ép [10]. Hành lang chữ L giờ đây được biểu diễn đúng nghĩa vật lý của nó là một **đường dẫn liên tục** [10].

#### Các trường (Fields):
1.  `private List<Position> path`: Danh sách các đối tượng `Position` đại diện cho các ô sàn (`FLOOR`) của hành lang. Lưu trữ dưới dạng chuỗi các điểm giúp dễ dàng kiểm tra va chạm điểm rời rạc và tự động bo tường bao quanh láng giềng [10].

#### Các phương thức (Methods):
*   `public void addPosition(Position p)`: Thêm một điểm tọa độ sàn vào đường đi hành lang.
*   `public List<Position> getPath()`: Trả về danh sách điểm tọa độ sàn để phục vụ kiểm tra va chạm động hoặc lưu trữ.
*   `@Override public void draw(TETile[][] world)`: Khớp nối 2 hành vi then chốt: **Trải gạch sàn** lên toàn bộ đường đi (tự động đè gạch tường phòng lớn để mở cửa) và **Quét lân cận 8 hướng (Local 8-Neighbor Scan)** để tự dựng tường bao bảo vệ trên những vùng khoảng không trống rỗng (`Tileset.NOTHING`) [11].

```java
import java.util.List;
import java.util.ArrayList;

public class Hallway implements WorldComponent {
    private List<Position> path;

    public Hallway() {
        this.path = new ArrayList<>();
    }

    public void addPosition(Position p) {
        this.path.add(p);
    }

    public List<Position> getPath() {
        return path;
    }

    @Override
    public void draw(TETile[][] world) {
        // BƯỚC 1: Trải gạch sàn FLOOR lên toàn bộ chuỗi tọa độ của hành lang.
        // Quy tắc vẽ đè tự nhiên sẽ ghi đè lên tường của phòng lớn chắn giữa, tự động "đục cửa"!
        for (Position p : path) {
            world[p.getX()][p.getY()] = Tileset.FLOOR;
        }

        // BƯỚC 2: Quét lân cận 8 hướng xung quanh từng ô sàn để tự động dựng tường bao bảo vệ.
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (Position p : path) {
            int x = p.getX();
            int y = p.getY();

            for (int i = 0; i < 8; i++) {
                int targetX = x + dx[i];
                int targetY = y + dy[i];

                // Nếu ô láng giềng nằm trong bản đồ và là NOTHING trống rỗng -> Dựng tường
                if (inBounds(targetX, targetY) && world[targetX][targetY].equals(Tileset.NOTHING)) {
                    world[targetX][targetY] = Tileset.WALL;
                }
            }
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < MapGenerator.WIDTH && y >= 0 && y < MapGenerator.HEIGHT;
    }
}
```

---

## 2. Thiết kế Thuật toán 3 Bước (Algorithmic Pipeline)

Chúng ta tách biệt rạch ròi quy trình thiết kế thành 3 bước độc lập dựa trên triết lý **Chia để trị** và **Tránh phân rã theo thời gian bị rò rỉ thông tin (Temporal Decomposition)** [10, 12]:

### Bước 1: Sinh phòng ngẫu nhiên và kiểm tra chồng lấn (overlaps)
1.  Khởi tạo danh sách logic rỗng `List<Room> rooms = new ArrayList<>()` [10].
2.  Chạy một số lượt thử giả ngẫu nhiên cố định (ví dụ: 100 lần thử):
    *   Sinh ngẫu nhiên chiều rộng `w` và chiều cao `h` trong khoảng giới hạn an toàn.
    *   Sinh ngẫu nhiên điểm bắt đầu `bottomLeft` khống chế nằm trọn trong bản đồ thế giới bằng công thức an toàn:
        $$\text{bottomLeft.x} \\in [1, \\text{WIDTH} - w]$$
        $$\text{bottomLeft.y} \\in [1, \\text{HEIGHT} - h]$$
    *   Sử dụng vòng lặp tối ưu hóa **Thoát sớm (Early Exit)** để kiểm tra chồng lấn:
        ```java
        boolean hasOverlaps = false;
        for (Room existingRoom : rooms) {
            if (existingRoom.overlaps(potentialRoom)) {
                hasOverlaps = true;
                break; // Dừng duyệt ngay khi phát hiện va chạm đầu tiên
            }
        }
        ```
    *   Nếu không chồng lấn, thêm vào danh sách `rooms`. Trả về `List<Room>` khi kết thúc [10].

### Bước 2: Sinh hành lang liên kết các phòng bằng đường đi chữ L rời rạc
Để đảm bảo thế giới luôn liên thông hoàn toàn (không có phòng nào bị cô lập như yêu cầu nghiêm ngặt của spec) [10]:
1.  Duyệt qua danh sách các phòng từ `0` đến `rooms.size() - 2` [10].
2.  Với mỗi cặp phòng kề nhau `r1 = rooms.get(i)` và `r2 = rooms.get(i+1)` [10]:
    *   Xác định hai tâm điểm: `start = r1.getCenter()` và `end = r2.getCenter()` [10].
    *   Tạo đối tượng `Hallway hallway = new Hallway()` [10].
    *   Sinh đường đi chữ L bằng vòng lặp tịnh tiến từng ô một (rẽ ngang trước, rẽ dọc sau). Thuật toán này sử dụng các bước nhảy động `stepX` và `stepY` (`1` hoặc `-1`) nên hoàn toàn miễn nhiễm với lỗi hiệu số tọa độ âm:
        ```java
        int startX = start.getX();
        int startY = start.getY();
        int endX = end.getX();
        int endY = end.getY();

        // 1. Phân đoạn ngang
        int currentX = startX;
        int stepX = (startX < endX) ? 1 : -1;
        while (currentX != endX) {
            hallway.addPosition(new Position(currentX, startY));
            currentX += stepX;
        }
        hallway.addPosition(new Position(endX, startY)); // Điểm góc cua chữ L

        // 2. Phân đoạn dọc (Bỏ qua điểm cua đã vẽ ở trên để tránh trùng lặp)
        int currentY = startY;
        int stepY = (startY < endY) ? 1 : -1;
        currentY += stepY;
        while (currentY != endY) {
            hallway.addPosition(new Position(endX, currentY));
            currentY += stepY;
        }
        hallway.addPosition(new Position(endX, endY)); // Điểm kết thúc tại tâm phòng r2
        ```
    *   Đưa `hallway` vào danh sách `List<Hallway> hallways` [10].

### Bước 3: Biên dịch thế giới và vẽ đè lớp hiển thị (World Compilation)
Bằng cách áp dụng **Quy tắc đè lớp (Layering Rule)**, chúng ta tự động hóa việc mở cửa phòng và giải quyết triệt để lỗi gạch tường hành lang đè lên sàn gạch phòng [11]:

1.  Khởi tạo mảng gạch `TETile[][] world` ngập tràn ô nền trống `Tileset.NOTHING`.
2.  **Lượt 1 (Vẽ các Phòng trước):** Duyệt qua tất cả căn phòng lớn và gọi `room.draw(world)`. Toàn bộ các phòng khép kín hoàn chỉnh được rải gạch sàn và dựng tường biên bao bọc [10].
3.  **Lượt 2 (Vẽ các Hành lang đè lên sau):** Duyệt qua danh sách các hành lang và gọi `hallway.draw(world)` [10]:
    *   *Mở cửa tự động:* Các ô sàn `Tileset.FLOOR` của hành lang rải đè lên hàng gạch biên `WALL` của phòng, đục thông lối ra vào tại các khớp tâm phòng một cách mượt mà và chính xác tuyệt đối [11]!
    *   *Tránh lỗi tường hành lang chắn lòng phòng:* Nhờ quy trình quét lân cận 8 hướng của `Hallway.draw` chỉ thực hiện gán gạch `WALL` nếu tọa độ đích đang là `Tileset.NOTHING`, bất kỳ lân cận nào rơi vào lòng căn phòng lớn (vốn đã được lát gạch `FLOOR` từ Lượt 1) sẽ **bị bỏ qua và không dựng tường**. Lòng phòng luôn thông thoáng, sạch sẽ và thông suốt [11]!

---

## 3. Phân tích Hiệu năng (Complexity and Performance Analysis)

### Độ phức tạp thời gian (Time Complexity)
*   **Thuật toán sinh phòng:** Gọi $N$ là tổng số lần thử sinh ngẫu nhiên ($100$) và $R$ là số phòng thực tế được chấp nhận ($R \\le N$). Với mỗi phòng mới, ta kiểm tra va chạm với tối đa $R$ phòng cũ. Độ phức tạp là $O(N \\cdot R)$. Thực tế chạy gần như tức thời ($< 5\\text{ms}$).
*   **Thuật toán liên kết hành lang:** Duyệt qua $R-1$ cặp phòng để sinh các phân đoạn chữ L rời rạc. Chi phí tỷ lệ tuyến tính với khoảng cách Manhattan giữa tâm các phòng, tức là $O(R \\cdot (W + H))$ trong trường hợp xấu nhất.
*   **Quét lân cận bo tường hành lang:** Với mỗi điểm tọa độ trong đường đi (độ dài tối đa là khoảng cách Manhattan $W + H$), ta kiểm tra 8 ô lân cận. Với $R-1$ hành lang, tổng thời gian quét bo tường là $O(R \\cdot (W + H))$.
*   **Tổng kết:** Thời gian chạy tổng thể đạt mức tuyến tính tuyệt vời **$O(W \\cdot H + N \\cdot R)$**, đảm bảo thế giới được khởi tạo và kết xuất đồ họa mượt mà ngay lập tức khi chạy trò chơi.

---

## 4. Các trường hợp đặc biệt & Giải pháp (Edge Cases and Solutions)

1.  **Hạt giống siêu lớn gây crash hệ thống:**
    *   *Nguy cơ:* Người dùng nhập chuỗi seed quá lớn vượt mức lưu trữ của kiểu số nguyên `int` (32-bit), gây lỗi nghiêm trọng `NumberFormatException` [313].
    *   *Giải pháp:* Đóng gói việc phân tích hạt giống bằng lớp `Long` thông qua `Long.parseLong(seedString)` để hỗ trợ phạm vi số giả ngẫu nhiên 64-bit khổng lồ [313].
2.  **Lệch một ô (Off-by-one) gây tràn mảng khi vẽ sát biên:**
    *   *Nguy cơ:* Vòng lặp so sánh lỏng lẻo `<=` khi duyệt các biên phòng sát rìa mảng có thể cố vẽ thêm 1 ô gạch ra ngoài biên thế giới, ném ra ngoại lệ nguy hiểm `ArrayIndexOutOfBoundsException` [31].
    *   *Giải pháp:* Đồng bộ hóa thuật toán vẽ bằng vòng lặp so sánh nghiêm ngặt `<` khống chế bởi giới hạn tối đa `bottomLeft.x + width` giúp loại bỏ hoàn toàn các phép toán trừ `- 1` dễ sai lệch.
3.  **Hành lang đâm xuyên qua một căn phòng lớn trung gian:**
    *   *Nguy cơ:* Hành lang nối phòng $i$ và $i+1$ ngẫu nhiên chạy xuyên qua một phòng $k$ nào đó.
    *   *Giải pháp / Tính năng:* Quy tắc vẽ đè (Layering Rule) sẽ cho phép sàn hành lang hòa vào làm một với sàn phòng lớn $k$. Nhờ cơ chế chỉ bo tường bao trên gạch `NOTHING`, không có bất kỳ bức tường hành lang nào mọc lên chắn ngang phòng $k$ cả! Bản đồ tự động có thêm các đường đi tắt (shortcuts) và vòng lặp (loops) thú vị, nâng cao tính trải nghiệm khám phá của trò chơi [257]!
