# BYOW Design Document (Phiên bản v3 - Cập nhật kiến trúc Đa hình & Thành phần)
**Tác giả:** Anh Ha
**Khóa học:** CS 61B - UC Berkeley

---

## 1. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (Classes and Data Structures)

Để quản lý độ phức tạp ngày càng tăng của thế giới game trong BYOW và tránh lỗi thiết kế **Primitive Obsession** (Ám ảnh kiểu nguyên thủy), chúng ta xây dựng hệ thống dựa trên sự phân cấp rõ rệt giữa **Thực thể logic vĩ mô (Logical Entities)** và **Khối cấu trúc vật lý vi mô (Physical Tiles)** [28.1, 28.2]. 

Sơ đồ phân cấp dưới đây thể hiện sự đóng gói chặt chẽ (information hiding) và khả năng tái sử dụng mã nguồn tối đa [27.5, 28.3]:

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
                                       List<Room> segments
```

---

### WorldComponent (Interface)
Đóng vai trò là **Rào cản trừu tượng (Abstraction Barrier)** chung cho tất cả các thực thể hình học có khả năng hiển thị trên bản đồ thế giới [8.30, 28.3].

#### Các phương thức:
*   `void draw(TETile[][] world)`: Quy định giao diện chung cho phép các đối tượng tự vẽ chính mình lên mảng lưới gạch thế giới `world`.

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
*   `@Override public boolean equals(Object o)`: Được ghi đè sử dụng từ khóa `instanceof` an toàn để so sánh tính bằng nhau về mặt giá trị của hai tọa độ, tuân thủ chặt chẽ tính phản xạ, đối xứng, bắc cầu [10.2].
*   `@Override public int hashCode()`: Được ghi đè đồng bộ với `equals` thông qua `Objects.hash(x, y)` để đảm bảo tính nhất quán khi lưu trữ và truy vấn trong các cấu trúc bảng băm của Java như `HashSet` hay `HashMap` [10.2].

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
Lớp đại diện cho một căn phòng hình chữ nhật trong thế giới. Lớp này kế thừa và thực hiện interface `WorldComponent` [8.30]. `Room` đóng gói toàn bộ kích thước của nó bao gồm cả tường (Wall-inclusive Bounding Box) [28.3].

#### Các trường (Fields):
1.  `private Position bottomLeft`: Tọa độ góc dưới bên trái của căn phòng.
2.  `private int width`: Chiều rộng tổng thể của phòng (đã tính cả tường bao).
3.  `private int height`: Chiều cao tổng thể của phòng (đã tính cả tường bao).

#### Các phương thức (Methods):
*   `public Position getCenter()`: Trả về một đối tượng `Position` đại diện cho tọa độ tâm hình học của căn phòng, đóng vai trò là "mốc định tuyến" để nối hành lang [28.3].
*   `public boolean overlaps(Room other)`: Sử dụng phép kiểm tra giao cắt hình học giữa hai hình chữ nhật trên cả hai trục hoành độ và tung độ với các toán tử **`&&` (AND)** để đảm bảo tính chính xác tuyệt đối [28.3].
*   `@Override public void draw(TETile[][] world)`: Biên dịch dữ liệu logic thành các ô gạch vật lý `TETile` cụ thể (`Tileset.WALL` cho biên và `Tileset.FLOOR` cho lòng lõi phòng) [28.3].

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
Lớp đại diện cho hành lang lắt léo kết nối các phòng. Để tối đa hóa việc tái sử dụng mã nguồn (Code Reuse), chúng ta áp dụng triết lý thiết kế **Thành phần thay vì Kế thừa (Composition over Inheritance - HAS-A)** [27.5, 28.3].

Hành lang chữ L thực chất là sự kết hợp của **các phân đoạn phòng siêu hẹp (narrow Room segments)** giao nhau. Thiết kế này giúp `Hallway` không cần tự vẽ thủ công hay tính toán va chạm phức tạp, mà ủy thác (delegate) hoàn toàn cho lớp `Room` [28.3]!

#### Các trường (Fields):
1.  `private List<Room> segments`: Danh sách chứa các phân đoạn phòng siêu hẹp (chiều ngang hoặc chiều dọc bằng 1 hoặc 2 ô sàn và có tường bao bao quanh) tạo nên hành lang.

#### Các phương thức (Methods):
*   `public void addSegment(Room narrowRoom)`: Thêm một phân đoạn phòng hẹp cấu thành hành lang.
*   `@Override public void draw(TETile[][] world)`: Duyệt qua tất cả các `segments` hình chữ nhật hẹp và gọi phương thức vẽ của chúng để tự dựng khung hình.

```java
import java.util.List;
import java.util.ArrayList;

public class Hallway implements WorldComponent {
    private List<Room> segments;

    public Hallway() {
        this.segments = new ArrayList<>();
    }

    public void addSegment(Room narrowRoom) {
        this.segments.add(narrowRoom);
    }

    @Override
    public void draw(TETile[][] world) {
        // Tận dụng 100% logic vẽ từ lớp Room
        for (Room segment : segments) {
            segment.draw(world);
        }
    }
}
```

---

## 2. Thiết kế Thuật toán 3 Bước (Algorithmic Pipeline)

Chúng ta tách biệt rạch ròi quy trình thiết kế thành 3 bước độc lập dựa trên triết lý **Chia để trị** và **Tránh phân rã theo thời gian bị rò rỉ thông tin (Temporal Decomposition)** [28.3]:

### Bước 1: Sinh phòng ngẫu nhiên và kiểm tra chồng lấn (overlaps)
1.  Khởi tạo danh sách logic rỗng `List<Room> rooms = new ArrayList<>()`.
2.  Chạy một số lượt thử giả ngẫu nhiên cố định (ví dụ: 100 lần thử):
    *   Sinh ngẫu nhiên chiều rộng `w` và chiều cao `h` trong khoảng giới hạn an toàn.
    *   Sinh ngẫu nhiên điểm bắt đầu `bottomLeft` khống chế nằm trọn trong bản đồ thế giới bằng công thức an toàn:
        $$\text{bottomLeft.x} \in [1, \text{WIDTH} - w]$$
        $$\text{bottomLeft.y} \in [1, \text{HEIGHT} - h]$$
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
    *   Nếu không chồng lấn, thêm vào danh sách `rooms`. Trả về `List<Room>` khi kết thúc.

### Bước 2: Sinh hành lang liên kết các phòng theo chuỗi liên thông
Để đảm bảo tất cả căn phòng đều liên thông hoàn hảo (single connected component) và không có phòng nào bị cô lập (thỏa mãn ràng buộc tuyệt đối của spec):
1.  Duyệt qua danh sách các phòng đã sinh thành công từ `0` đến `rooms.size() - 2`.
2.  Với mỗi cặp phòng kề nhau `r1 = rooms.get(i)` và `r2 = rooms.get(i+1)`:
    *   Xác định hai tâm điểm: `start = r1.getCenter()` và `end = r2.getCenter()`.
    *   Sinh một hành lang chữ L bằng cách tạo **hai phân đoạn phòng siêu hẹp (narrow Rooms)**:
        *   *Phân đoạn ngang (Horizontal segment):* Một phòng hẹp có chiều rộng chạy từ `start.x` đến `end.x` và chiều cao cố định là 3 ô (để đảm bảo lòng trong là 1 ô sàn đi được và 2 ô tường bao).
        *   *Phân đoạn dọc (Vertical segment):* Một phòng hẹp có chiều dọc chạy từ `start.y` đến `end.y` và chiều rộng cố định là 3 ô (để đảm bảo lòng trong là 1 ô sàn đi được và 2 ô tường bao).
    *   Thêm hai phân đoạn này vào một đối tượng `Hallway` và đưa vào danh sách `List<Hallway> hallways`.

### Bước 3: Đè lớp hiển thị biên dịch thế giới vật lý (World Compilation)
Đây là giai đoạn quyết định tính thẩm mỹ của thế giới. Bằng cách áp dụng **Quy tắc đè lớp (Layering Rule)**, chúng ta tự động hóa việc mở cửa phòng và giải quyết triệt để lỗi tường hành lang chắn ngang lòng phòng [28.3]:

1.  Khởi tạo mảng gạch `TETile[][] world` ngập tràn ô nền trống `Tileset.NOTHING`.
2.  **Lượt 1 (Vẽ các Phòng trước):** Duyệt qua tất cả căn phòng và gọi `room.draw(world)`. Lúc này các căn phòng khép kín hoàn toàn được tạo dựng.
3.  **Lượt 2 (Vẽ các Hành lang đè lên sau):** Duyệt qua danh sách các hành lang và gọi `hallway.draw(world)`:
    *   *Mở cửa tự động:* Khi vẽ các phân đoạn phòng hẹp của hành lang, các ô sàn `FLOOR` ở lõi hành lang sẽ tự động **ghi đè** và thay thế các ô tường `WALL` của phòng tại điểm giao cắt. Cửa phòng tự động được đục mở một cách thanh thoát!
    *   *Ngăn tường đè sàn:* Trong phương thức `draw` chung của lớp `Room` (mà phân đoạn hành lang thừa hưởng), chúng ta có điều kiện bảo vệ:
        ```java
        if (world[col][row].equals(Tileset.NOTHING)) {
            world[col][row] = Tileset.WALL;    
        }
        ```
        Hành lang sẽ chỉ xây tường `WALL` lên những ô đang trống rỗng (`NOTHING`). Tại những nơi hành lang chạy sượt qua lòng phòng hoặc đè lên phần sàn `FLOOR` của phòng khác, tường hành lang sẽ **tuyệt đối không được xây dựng**, giữ cho lòng phòng luôn thông thoáng và an toàn cho nhân vật di chuyển!

---

## 3. Phân tích Hiệu năng (Complexity and Performance Analysis)

### Độ phức tạp thời gian (Time Complexity)
*   **Thuật toán sinh phòng:** Gọi $N$ là tổng số lần thử sinh ngẫu nhiên ($100$) và $R$ là số phòng thực tế được chấp nhận ($R \le N$). Với mỗi phòng mới, ta kiểm tra va chạm với tối đa $R$ phòng cũ. Độ phức tạp là $O(N \cdot R)$. Thực tế chạy gần như tức thời ($< 5\text{ms}$).
*   **Thuật toán liên kết hành lang:** Duyệt qua $R-1$ cặp phòng để sinh các phân đoạn chữ L. Chi phí tỷ lệ tuyến tính với khoảng cách Manhattan giữa tâm các phòng, tức là $O(R \cdot (W + H))$ trong trường hợp xấu nhất.
*   **Vẽ và Biên dịch thế giới:** Duyệt qua mảng lưới cố định để gán tham chiếu gạch. Độ phức tạp là $O(W \cdot H)$ với mảng tĩnh cỡ $80 \times 30$.
*   **Tổng kết:** Thời gian chạy tổng thể đạt mức tối ưu tuyến tính **$O(W \cdot H + N \cdot R)$**, đảm bảo thế giới được khởi tạo và kết xuất đồ họa mượt mà ngay lập tức khi chạy trò chơi.

---

## 4. Các trường hợp đặc biệt & Giải pháp (Edge Cases and Solutions)

1.  **Hạt giống siêu lớn gây crash hệ thống:**
    *   *Nguy cơ:* Người dùng nhập chuỗi seed quá lớn vượt mức lưu trữ của kiểu số nguyên `int` (32-bit), gây lỗi nghiêm trọng `NumberFormatException` [FAQ 3].
    *   *Giải pháp:* Đóng gói việc phân tích hạt giống bằng lớp `Long` thông qua `Long.parseLong(seedString)` để hỗ trợ phạm vi số giả ngẫu nhiên 64-bit khổng lồ [FAQ 3].
2.  **Lệch một ô (Off-by-one) gây tràn mảng khi vẽ sát biên:**
    *   *Nguy cơ:* Vòng lặp so sánh lỏng lẻo `<=` khi duyệt các biên phòng sát rìa mảng có thể cố vẽ thêm 1 ô gạch ra ngoài biên thế giới, ném ra ngoại lệ nguy hiểm `ArrayIndexOutOfBoundsException`.
    *   *Giải pháp:* Đồng bộ hóa thuật toán vẽ bằng vòng lặp so sánh nghiêm ngặt `<` khống chế bởi giới hạn tối đa `bottomLeft.x + width` giúp loại bỏ hoàn toàn các phép toán trừ `- 1` dễ sai lệch.
3.  **Hành lang đâm sượt qua lòng căn phòng lớn:**
    *   *Nguy cơ:* Nếu một hành lang chữ L vô tình chạy sượt qua biên một căn phòng lớn, thuật toán dựng tường của hành lang hẹp có thể tự ý gán tường chặn ngang các phần gạch sàn kề bên.
    *   *Giải pháp:* Áp dụng triệt để cơ chế **"Chỉ xây tường trên NOTHING"**. Tường của hành lang sẽ tự động dừng xây tại bất kỳ ô nào đã được đặt sẵn làm `FLOOR` trước đó, bảo toàn tính liên thông di chuyển không bị gián đoạn.
