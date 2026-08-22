# BYOW Technical Design Document (Phiên bản v11 - Tích hợp Random Room Placement, L-shaped Hallways & Nghiên cứu Va chạm Hành lang)
**Tác giả:** Anh Ha  
**Phiên bản:** v11 (Tích hợp Phương án Sinh phòng Ngẫu nhiên và Hành lang chữ L, Nghiên cứu sâu về Va chạm Hành lang)  
**Khóa học:** CS 61B - UC Berkeley  

---

## 1. Kiến trúc Hệ thống & Cấu trúc Dữ liệu (Classes and Data Structures)

Để quản lý độ phức tạp ngày càng tăng của thế giới game trong BYOW và tránh lỗi thiết kế **Primitive Obsession** (Ám ảnh kiểu nguyên thủy), chúng ta xây dựng hệ thống dựa trên sự phân cấp rõ rệt giữa **Thực thể logic vĩ mô (Logical Entities)** và **Khối cấu trúc vật lý vi mô (Physical Tiles)** [28.1, 28.2].

Chúng ta xóa bỏ hoàn toàn sự phân tách phức tạp giữa lớp `Hallway` độc lập [28.2]. Về mặt vật lý rời rạc trên lưới, hành lang thực chất chỉ là một căn phòng siêu hẹp có chiều rộng hoặc chiều cao là 3 ô gạch (để đảm bảo lòng trong là 1 ô sàn đi được và bọc 2 ô tường hai bên) [v3]. Sự đồng nhất này cho phép chúng ta đơn giản hóa cấu trúc dữ liệu thế giới thành một danh sách duy nhất các đối tượng `Room`, tận dụng tối đa khả năng tái sử dụng mã nguồn [27.5, 28.3].

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

## 2. Phương án A: Thuật toán Cây mọc nhánh hữu cơ (Growing Tree Generation)

Thuật toán này chuyển dịch hoàn toàn từ tư duy đệ quy tuyến tính (vốn dễ gây lỗi Stack Overflow) sang thuật toán **Duyệt biên lặp kiểm soát bằng danh sách Fringe (`activeRooms`)** [v6]. Bản đồ game sẽ phát triển một cách hữu cơ, mọc các nhánh hành lang và phòng con từ các thực thể đã tồn tại một cách cực kỳ an toàn và mạch lạc [v6].

### Các bước thuật toán chi tiết và hiện thực mã nguồn Java:

### Bước 1: Khởi tạo Phòng mầm (Seed Room / Root Node)
1.  **Tính toán kích thước & căn lề:** Để thế giới phát triển cân đối và đẹp mắt nhất, căn phòng mầm `seedRoom` được khởi tạo có kích thước lớn ngẫu nhiên trong khoảng $[6, 12]$ cho cả chiều rộng và chiều cao [v6].
2.  **Định vị tâm bản đồ:** Tọa độ của phòng mầm được tính toán sao cho nó nằm chính xác ở trung tâm lưới $WIDTH 	imes HEIGHT$ (80x60) nhằm tránh bế tắc không gian mọc nhánh quá sớm ở các cạnh biên [v6]:
    $$	ext{seedPos.x} = rac{WIDTH}{2} - rac{seedW}{2}$$
    $$	ext{seedPos.y} = rac{HEIGHT}{2} - rac{seedH}{2}$$
3.  **Khởi tạo Fringe:** Thêm `seedRoom` vào danh sách cấu thành thế giới `allComponents` và danh sách tiền tuyến `activeRooms` (Frontier/Fringe) để bắt đầu chu kỳ mọc nhánh [v6].

### Bước 2: Vòng lặp duyệt Frontier kiểm soát va chạm chủ động
Vòng lặp `while` hoạt động trên danh sách Fringe `activeRooms`. Ở mỗi bước lặp, hệ thống bốc ngẫu nhiên một phòng mầm (parent) từ Frontier, sau đó thử rẽ nhánh theo cả 4 hướng ngẫu nhiên đã shuffle [v6]. Nếu mọc nhánh thành công, cặp (Hành lang + Phòng con) mới sẽ được thêm vào thế giới, đồng thời phòng con trở thành mầm rẽ tiếp theo. 

Nếu tất cả hướng rẽ đều va chạm, phòng mẹ bị loại bỏ hoàn toàn khỏi Fringe (Backtracking hữu cơ) [v6].

#### Hiện thực Java chuẩn mực cho phương thức điều phối `generate()` trong Phương án A:

```java
public TETile[][] generateOptionA() {
    List<Room> allComponents = new ArrayList<>();
    List<Room> activeRooms = new ArrayList<>(); 

    int seedW = RandomUtils.uniform(RANDOM, 6, 12);
    int seedH = RandomUtils.uniform(RANDOM, 6, 12);
    Position seedPos = new Position(WIDTH / 2 - seedW / 2, HEIGHT / 2 - seedH / 2);
    Room seedRoom = new Room(seedPos, seedW, seedH);

    allComponents.add(seedRoom);
    activeRooms.add(seedRoom);

    int attempts = 0;
    int targetComponentsCount = TARGET_ROOM_COUNT * 2 - 1;

    while (allComponents.size() < targetComponentsCount 
           && !activeRooms.isEmpty() 
           && attempts < MAX_ATTEMPTS) {
        attempts++;

        int parentIdx = RandomUtils.uniform(RANDOM, 0, activeRooms.size());
        Room parent = activeRooms.get(parentIdx);

        BranchResult result = tryCreateBranch(parent, allComponents);

        if (result != null) {
            allComponents.add(result.hallway);
            allComponents.add(result.childRoom);
            activeRooms.add(result.childRoom); 
        } else {
            activeRooms.remove(parent);
        }
    }

    for (Room comp : allComponents) {
        comp.draw(tiles);
    }

    return tiles;
}
```

---

## 3. Phương án B: Đặt phòng ngẫu nhiên kết hợp với Hành lang chữ L (Random Room Placement & L-shaped Hallways)

Đây là phương án tiếp cận **từ trên xuống (Top-down)** mang tính cổ điển của thể loại Roguelike. Quy trình của phương án này bao gồm việc thả các phòng một cách ngẫu nhiên lên lưới bản đồ, sau đó nối chúng lại bằng các hành lang gấp khúc dạng chữ L [294, 297].

```
                  [ Sinh ngẫu nhiên Room ứng viên ]
                                 │
                                 ▼
                      overlapsAny() chồng lấn?
                                 │
                     ┌───────────┴───────────┐
                  (Không)                   (Có)
                     │                        │
                     ▼                        ▼
               [ Thêm phòng ]             [ Bỏ qua ]
              [ Vào rooms list ]
                                 │
                     (Lặp đến khi đủ phòng hoặc hết lượt thử)
                                 │
                                 ▼
               [ Sắp xếp phòng theo Hoành độ X ]
                                 │
                                 ▼
                [ Nối tâm phòng i với phòng i+1 ]
                    [ Bằng hành lang chữ L ]
                                 │
                                 ▼
                  [ Kết xuất và Vẽ đè lớp ]
```

### Các bước thuật toán chi tiết:

### Bước 1: Sinh phòng ngẫu nhiên và lọc chồng lấn (overlaps)
1. Khởi tạo danh sách phòng trống `List<Room> rooms = new ArrayList<>()`.
2. Chạy một vòng lặp thử sinh ngẫu nhiên tối đa `MAX_ATTEMPTS` (ví dụ: 1000 lượt):
    * Sinh ngẫu nhiên chiều rộng `w` và chiều cao `h` trong giới hạn $[4, 10]$ ô gạch.
    * Sinh ngẫu nhiên vị trí `bottomLeft` nằm trọn vẹn trong lưới mảng, chừa biên tối thiểu 1 ô gạch để đảm bảo an toàn [v4].
    * Kiểm tra va chạm chồng lấn với tất cả các phòng đã được chấp nhận trước đó bằng hàm `overlaps`.
    * Nếu không va chạm, thêm phòng vào danh sách `rooms`. Khi số lượng phòng đạt `TARGET_ROOM_COUNT` (ví dụ: 15 phòng), dừng sinh phòng.

### Bước 2: Sắp xếp tuyến tính các phòng để tối ưu hóa liên thông
Để đảm bảo tất cả các phòng đều liên thông với nhau và tạo nên một đường đi tự nhiên từ góc này sang góc kia của bản đồ, danh sách phòng được sắp xếp theo thứ tự tăng dần của hoành độ tâm X:
```java
rooms.sort((Room r1, Room r2) -> r1.getCenter().getX() - r2.getCenter().getX());
```
Việc sắp xếp này giúp triệt tiêu hoàn toàn nguy cơ hành lang bị kéo dãn quá dài hoặc đan chéo chằng chịt lên nhau một cách mất kiểm soát.

### Bước 3: Kiến tạo hành lang chữ L kết nối tuần tự
Duyệt qua danh sách phòng từ `0` đến `rooms.size() - 2`. Với mỗi cặp phòng liên tiếp `r1` và `r2`:
1. Xác định hai tọa độ tâm: `start = r1.getCenter()` và `end = r2.getCenter()`.
2. Tạo hai phân đoạn hành lang thẳng đứng và nằm ngang cấu thành góc rẽ chữ L:
    * **Phân đoạn ngang (Horizontal segment):** Một căn phòng siêu hẹp có chiều rộng từ `start.x` đến `end.x` và chiều cao cố định là 3 (để đảm bảo có 1 hàng gạch sàn đi được và 2 hàng gạch tường bao bọc trên dưới).
    * **Phân đoạn dọc (Vertical segment):** Một căn phòng siêu hẹp có chiều cao từ `start.y` đến `end.y` và chiều rộng cố định là 3.
3. Đưa hai phân đoạn này vào danh sách thực thể thế giới để vẽ.

#### Hiện thực Java cho phương thức điều phối `generate()` trong Phương án B:

```java
public TETile[][] generateOptionB() {
    List<Room> rooms = new ArrayList<>();
    int attempts = 0;

    // 1. Sinh phòng ngẫu nhiên không chồng lấn
    while (rooms.size() < TARGET_ROOM_COUNT && attempts < MAX_ATTEMPTS) {
        attempts++;
        int w = RandomUtils.uniform(RANDOM, 5, 10);
        int h = RandomUtils.uniform(RANDOM, 5, 10);
        int x = RandomUtils.uniform(RANDOM, 1, WIDTH - w - 1);
        int y = RandomUtils.uniform(RANDOM, 1, HEIGHT - h - 1);
        Room candidate = new Room(new Position(x, y), w, h);

        if (!overlapsAny(rooms, candidate)) {
            rooms.add(candidate);
        }
    }

    // 2. Sắp xếp tuyến tính các phòng theo trục hoành X
    rooms.sort((Room r1, Room r2) -> r1.getCenter().getX() - r2.getCenter().getX());

    // 3. Tạo hành lang chữ L kết nối tuần tự
    List<Room> hallways = new ArrayList<>();
    for (int i = 0; i < rooms.size() - 1; i++) {
        Room r1 = rooms.get(i);
        Room r2 = rooms.get(i + 1);
        Position start = r1.getCenter();
        Position end = r2.getCenter();

        // Tạo phân đoạn ngang chữ L
        int startX = start.getX();
        int startY = start.getY();
        int endX = end.getX();
        int endY = end.getY();

        int horX = Math.min(startX, endX);
        int horWidth = Math.abs(startX - endX) + 1;
        // Bọc tường: rộng là horWidth, cao cố định là 3, bottomLeft lùi xuống 1 ô để sàn căn giữa
        Room horSegment = new Room(new Position(horX, startY - 1), horWidth, 3);
        hallways.add(horSegment);

        // Tạo phân đoạn dọc chữ L
        int vertY = Math.min(startY, endY);
        int vertHeight = Math.abs(startY - endY) + 1;
        // Bọc tường: cao là vertHeight, rộng cố định là 3, bottomLeft lùi sang trái 1 ô để sàn căn giữa
        Room vertSegment = new Room(new Position(endX - 1, vertY), 3, vertHeight);
        hallways.add(vertSegment);
    }

    // 4. Kết xuất và Vẽ đè lớp hiển thị (Vẽ phòng trước, vẽ hành lang sau)
    for (Room r : rooms) {
        r.draw(tiles);
    }
    for (Room h : hallways) {
        h.draw(tiles);
    }

    return tiles;
}
```

---

## 4. Nghiên cứu Chuyên sâu: Vấn đề Va chạm Hành lang trong Phương án B

Trong phương án "Đặt phòng ngẫu nhiên và hành lang chữ L", **Va chạm Hành lang (Hallway/Corridor Collision)** là một thách thức kỹ thuật cực kỳ lớn về mặt hình học rời rạc. 

### A. Bản chất của vấn đề Va chạm Hành lang
Khi chúng ta thả tự do các phòng lớn ngẫu nhiên trên lưới bản đồ, vị trí của chúng hoàn toàn độc lập. Khi thuật toán vẽ đường chữ L kết nối tâm của phòng $A$ và phòng $B$, đường đi thẳng tắp này rất dễ **đâm xuyên qua một căn phòng trung gian $C$ bất kỳ** đang nằm chắn giữa đường đi của chúng.

```text
       +---------+
       | Phòng A |
       +----+----+
            |
            | <--- Hành lang chữ L đâm xuyên qua lòng phòng C!
      +-----+-----+
      |  Phòng C  | <--- Phòng trung gian bị đâm xuyên qua
      +-----+-----+
            |
       +----+----+
       | Phòng B |
       +---------+
```

Nếu hệ thống không có cơ chế xử lý va chạm thông minh, hai hiện tượng lỗi thẩm mỹ (visual artifacts) và lỗi logic di chuyển nghiêm trọng sau sẽ xảy ra:
1.  **Lỗi "Tường chắn lòng phòng" (Blocked Interiors):** Khi hành lang chữ L tự vẽ chính mình (bao gồm cả hàng gạch sàn `FLOOR` ở giữa và hai hàng gạch tường `WALL` ở rìa), hàng gạch tường biên của hành lang vẽ sau sẽ **đâm thẳng vào giữa lòng sàn** của căn phòng trung gian $C$. Phòng $C$ sẽ bị chia cắt làm hai nửa biệt lập, người chơi đứng ở nửa bên này không thể đi sang nửa bên kia!
2.  **Lỗi "Hở góc chéo sườn tường" (Diagonal Wall Leaks):** Tại điểm giao cắt nơi hành lang đâm vào hoặc đi ra khỏi phòng $C$, nếu chỉ quét tường trực giao 4 hướng, các bức tường bao quanh sẽ bị đứt gãy ở góc 45 độ, làm lộ các ô khoảng không vô định `NOTHING` màu đen ra ngoài lưới di chuyển.

---

### B. Ba giải pháp Công nghệ khắc phục triệt để Va chạm Hành lang

Để giải quyết vấn đề này một cách chuẩn mực và thanh thoát theo tiêu chuẩn phần mềm chuyên nghiệp của UC Berkeley, chúng ta có thể áp dụng một trong ba chiến lược thiết kế sau:

#### Chiến lược 1: Quy tắc Đè lớp Toàn cục & "Chỉ xây tường trên NOTHING" (Global Layering & NOTHING Filter)
Đây là chiến lược **Tối giản mà Hiệu quả nhất (Minimalist & Bulletproof)**, tận dụng tối đa cơ chế vẽ đè lớp để triệt tiêu lỗi va chạm mà không tốn chi phí tính toán hình học phức tạp [v3]:

1.  **Quy trình đè lớp:** Chúng ta vẽ toàn bộ các căn phòng lớn trước để thiết lập sàn `FLOOR` và tường `WALL`. Sau đó mới vẽ hành lang đè lên sau [v3].
2.  **Bộ lọc NOTHING trong lớp Room:** Trong phương thức `draw` của lớp `Room` (mà hành lang hẹp cũng thừa hưởng):
    ```java
    if (world[col][row].equals(Tileset.NOTHING)) {
        world[col][row] = Tileset.WALL;    
    }
    ```
    *   *Cơ chế tự động sửa lỗi:* Khi hành lang đi xuyên qua lòng phòng $C$ (vốn đã được lấp đầy bằng gạch sàn `Tileset.FLOOR` từ trước), gạch sàn của hành lang sẽ hòa làm một với gạch sàn của phòng $C$. 
    *   Đồng thời, tại các ô rìa của hành lang đi qua lòng phòng $C$, điều kiện `world[col][row].equals(Tileset.NOTHING)` trả về `false` (vì ô gạch đang là `FLOOR` chứ không phải `NOTHING`), do đó **không có bất kỳ khối tường `WALL` nào của hành lang được phép mọc lên chắn ngang lòng phòng $C$**! 
    *   Hàng tường bao hành lang sẽ tự động biến mất khi đi vào phòng $C$ và mọc lại bình thường ở rìa đối diện khi đi ra ngoài khoảng trống.

---

#### Chiến lược 2: Kiểm tra Va chạm Điểm rời rạc và Rẽ nhánh dự phòng (Discrete Path Collision Checking)
Nếu chúng ta muốn duy trì một layout hầm ngục "ngăn nắp tuyệt đối", không cho phép hành lang chạy sượt qua phòng khác để giữ tính biệt lập, chúng ta sẽ biểu diễn hành lang dưới dạng một danh sách các điểm tọa độ rời rạc `List<Position> path` [v6] và kiểm tra va chạm trước khi vẽ:

```java
/**
 * Kiểm tra xem hành lang chữ L có đâm xuyên qua bất kỳ căn phòng trung gian nào khác không.
 */
public boolean isHallwayColliding(List<Position> path, Room r1, Room r2, List<Room> allRooms) {
    for (Position p : path) {
        for (Room room : allRooms) {
            // Bỏ qua hai phòng đầu và cuối đang được kết nối trực tiếp
            if (room.equals(r1) || room.equals(r2)) {
                continue;
            }
            // Nếu một điểm tọa độ bất kỳ của hành lang lọt vào bên trong lòng phòng trung gian thứ ba
            if (room.contains(p)) {
                return true; // Phát hiện va chạm đâm xuyên phòng!
            }
        }
    }
    return false;
}
```

*   *Cơ chế hoạt động dự phòng:* Với mỗi cặp phòng kề nhau, chúng ta tính toán cả hai ứng viên đường đi chữ L:
    *   *Ứng viên 1:* Đi ngang trước, rẽ dọc sau.
    *   *Ứng viên 2:* Đi dọc trước, rẽ ngang sau.
*   Nếu Ứng viên 1 bị va chạm (`isHallwayColliding` trả về `true`), hệ thống sẽ tự động chuyển sang kiểm tra và vẽ Ứng viên 2. Nếu cả hai ứng viên đều bị va chạm, ta mới kích hoạt cơ chế sinh lại tọa độ phòng (re-seed) hoặc chấp nhận vẽ đè lớp.

---

#### Chiến lược 3: Tìm đường trên Lưới rời rạc (Grid-based A* / BFS Pathfinding)
Đây là chiến lược **Tối ưu hóa và Cao cấp nhất (Advanced Graph Routing)**. Thay vì vẽ đường chữ L tĩnh cứng nhắc, chúng ta coi mảng gạch `world` như một đồ thị rời rạc, trong đó:
*   Các ô `Tileset.NOTHING` có chi phí di chuyển (cost) thấp.
*   Các ô `Tileset.FLOOR` của các phòng trung gian đã tồn tại có chi phí di chuyển cực cao (hoặc được coi là chướng ngại vật tuyệt đối).

Sử dụng thuật toán **BFS (Breadth-First Search)** để tìm đường đi ngắn nhất từ tâm phòng $A$ đến tâm phòng $B$ [359]:
```java
// Breadth-First Search for Hallway Routing
Queue<Position> fringe = new ArrayDeque<>();
fringe.add(start);
marked.add(start);

while (!fringe.isEmpty()) {
    Position curr = fringe.removeFirst();
    if (curr.equals(end)) {
        break; 
    }
    for (Position neighbor : get4Neighbors(curr)) {
        // Coi sàn các phòng khác là chướng ngại vật để hành lang uốn lượn né tránh
        if (inBounds(neighbor) && !marked.contains(neighbor) && !isInsideOtherRoom(neighbor, r1, r2)) {
            marked.add(neighbor);
            edgeTo.put(neighbor, curr);
            fringe.addLast(neighbor);
        }
    }
}
```
*   *Kết quả:* Hành lang sinh ra sẽ tự động uốn lượn, luồn lách né tránh tất cả các căn phòng trung gian để kết nối hai phòng $A$ và $B$ một cách sạch sẽ, tạo nên những đường đi vô cùng quanh co và thú vị cho hầm ngục!

---

## 5. Phân tích Hiệu năng So sánh (Comparative Performance Analysis)

| Tiêu chí | Phương án A (Cây mọc nhánh Frontier) | Phương án B (L-shaped Hallways truyền thống) |
| :--- | :--- | :--- |
| **Độ tự nhiên của bản đồ** | **Rất cao:** Bản đồ mọc hữu cơ giống hang động / hầm ngục tự nhiên [v6]. | **Trung bình:** Các phòng phân bổ ngẫu nhiên độc lập, kết nối tuần tự theo trục ngang [v5]. |
| **Xác suất bị Reject phòng** | **0%:** Vì chỉ mọc nhánh ở những không gian còn trống trải. | **Cao:** Các lượt thử ngẫu nhiên cuối cùng sẽ bị Reject liên tục do lưới bị lấp đầy. |
| **Độ phức tạp thuật toán** | $O(M \cdot K)$ với $M$ là lần thử rẽ nhánh, $K$ là số phòng [v7]. | $O(M \cdot K + R \log R)$ với $R \log R$ là chi phí sắp xếp các phòng theo trục X. |
| **Độ phức tạp mã nguồn** | **Trung bình:** Cần quản lý cấu trúc Frontier danh sách tiền tuyến `activeRooms` [v6]. | **Thấp nhất:** Chỉ cần vòng lặp sinh ngẫu nhiên và sắp xếp tuần tự mảng. |
| **Xử lý Va chạm Hành lang** | **Tự động tránh:** Do hành lang mọc từ biên phòng mẹ ra khoảng không trống trải. | **Bắt buộc xử lý:** Phải dùng bộ lọc NOTHING hoặc thuật toán kiểm tra va chạm đường đi. |

---

## 6. Các trường hợp đặc biệt & Giải pháp phòng ngự (Edge Cases and Solutions)

1.  **Hành lang chữ L thẳng hàng ngang/dọc gây lỗi độ rộng:**
    *   *Nguy cơ:* Nếu hai phòng $A$ và $B$ xếp thẳng hàng ngang (`startY == endY`), hành lang dọc chữ L sẽ có chiều cao bằng $1$, khiến lòng gạch sàn bị bít kín bởi gạch tường bao quanh [v5].
    *   *Giải pháp:* Tích hợp chốt chặn điều kiện `if (startX != endX)` và `if (startY != endY)` để chỉ vẽ phân đoạn tương ứng khi thực sự có khoảng cách chênh lệch tọa độ, tránh sinh các phân đoạn rỗng hoặc chồng lấn không mong muốn.
2.  **Tràn mảng khi vẽ tường hành lang chữ L sát rìa bản đồ:**
    *   *Nguy cơ:* Khi tịnh tiến bottomLeft của hành lang để bọc tường (lùi 1 ô), nếu hành lang nằm sát rìa $0$ của trục tọa độ, phép trừ `-1` sẽ làm tọa độ bị âm, ném ra lỗi nổ mảng `ArrayIndexOutOfBoundsException` [v4].
    *   *Giải pháp:* Khống chế nghiêm ngặt điều kiện sinh phòng ngẫu nhiên chừa biên tối thiểu 1 ô gạch thông qua hàm `inBounds(Room r)` bảo vệ biên bản đồ [v4].
