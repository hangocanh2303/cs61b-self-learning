# BYOW Design Document
**Author:** Anh Ha  
**Course:** CS 61B - UC Berkeley

---

## 1. Classes and Data Structures

### Position
Lớp này đại diện cho tọa độ của một ô gạch (tile) trên lưới tọa độ hai chiều của thế giới game. Sử dụng lớp này giúp đóng gói các tọa độ nguyên thủy, tăng mức độ trừu tượng hóa và tránh lỗi "Primitive Obsession" (ám ảnh kiểu nguyên thủy).

#### Fields
1. `public final int x`: Tọa độ hoành độ.
2. `public final int y`: Tọa độ tung độ.

#### Methods
*   `@Override public boolean equals(Object o)`: Được ghi đè để so sánh tính bằng nhau về giá trị tọa độ `(x, y)` thay vì so sánh địa chỉ bộ nhớ của đối tượng. Đảm bảo tuân thủ tính phản xạ, đối xứng và bắc cầu.
*   `@Override public int hashCode()`: Được ghi đè nhất quán với `equals` để đảm bảo hai đối tượng `Position` có cùng tọa độ sẽ sinh ra cùng một mã băm, cho phép sử dụng hiệu quả trong các cấu trúc dữ liệu băm như `HashSet` và `HashMap`.

---

### Room
Lớp đại diện cho một căn phòng hình chữ nhật trong thế giới. Đây là một mô-đun logic ở cấp độ vĩ mô, nắm giữ thông số hình học và vị trí của căn phòng.

#### Fields
1. `public final Position bottomLeft`: Tọa độ góc dưới bên trái của căn phòng (dùng đối tượng `Position` để quản lý vị trí).
2. `public final int width`: Chiều rộng của phòng (bao gồm cả tường).
3. `public final int height`: Chiều cao của phòng (bao gồm cả tường).

#### Methods
*   `public Position getCenter()`: Tính toán và trả về tọa độ trung tâm (tâm phòng) dưới dạng đối tượng `Position`. Phương thức này cực kỳ hữu ích khi làm mốc để vạch đường đi cho hành lang nối các phòng.
*   `public boolean overlaps(Room other)`: Kiểm tra xem căn phòng hiện tại có bị chồng lấn (overlap) với một căn phòng khác hay không. Công thức kiểm tra:
    $$\text{overlaps} = (x_1 < x_2 + w_2) \land (x_1 + w_1 > x_2) \land (y_1 < y_2 + h_2) \land (y_1 + h_1 > y_2)$$
*   `public void draw(TETile[][] world)`: Biên dịch dữ liệu hình học thành gạch vật lý. Phương thức này sẽ duyệt qua vùng không gian phòng trên mảng `world` và gán `Tileset.WALL` cho các ô biên, và `Tileset.FLOOR` cho các ô bên trong.

---

### Hallway
Lớp đại diện cho các hành lang kết nối giữa các căn phòng. Hành lang thực chất là một chuỗi các ô sàn (`FLOOR`) liên tục nối từ điểm xuất phát tới điểm đích và được bao bọc bởi tường (`WALL`).

#### Fields
1. `public final Room room1`: Căn phòng xuất phát.
2. `public final Room room2`: Căn phòng đích cần kết nối.
3. `public final List<Position> path`: Danh sách các đối tượng `Position` đại diện cho các ô sàn (`FLOOR`) của hành lang. Lưu trữ đường đi dưới dạng danh sách tọa độ giúp dễ dàng bo tường bao quanh sau này.

#### Methods
*   `public void draw(TETile[][] world)`: Thực hiện vẽ hành lang lên thế giới game theo thuật toán 2 bước:
    1.  Duyệt qua danh sách `path`, đặt các ô gạch tại tọa độ tương ứng thành `Tileset.FLOOR`.
    2.  Với mỗi ô gạch thuộc `path`, kiểm tra 8 ô láng giềng xung quanh. Nếu ô nào đang là không gian trống (`Tileset.NOTHING`), biến ô đó thành `Tileset.WALL` để tự động xây tường bảo vệ quanh hành lang mà không đè lên phần sàn của phòng hay hành lang khác.

---

### MapGenerator (World Generator Coordinator)
Lớp điều phối toàn bộ vòng đời sinh thế giới ngẫu nhiên. Nó chịu trách nhiệm nhận một giá trị hạt giống (`seed`), tính toán phân phối phòng và hành lang dưới dạng đối tượng logic, sau đó vẽ chúng lên mảng `TETile[][]`.

#### Fields
1. `private final int width`: Chiều rộng thế giới (thường là 80 ô).
2. `private final int height`: Chiều cao thế giới (thường là 30 ô).
3. `private final Random random`: Bộ sinh số giả ngẫu nhiên (pseudorandom number generator) được khởi tạo bằng `seed` kiểu `long` để đảm bảo tính tất định (deterministic).

#### Methods
*   `public TETile[][] generate()`: Phương thức chính trả về mảng `TETile[][]` hoàn chỉnh sau khi chạy tuần tự 3 bước sinh thế giới.

---

## 2. Algorithms

Thuật toán sinh thế giới được thiết kế theo nguyên lý **Chia để trị** và **Trừu tượng hóa phân cấp**, chia quy trình thành 3 bước độc lập, rạch ròi:

### Bước 1: Sinh phòng ngẫu nhiên (Random Room Generation)
1.  Khởi tạo một danh sách rỗng `List<Room> rooms = new ArrayList<>()`.
2.  Chạy một vòng lặp cố định (ví dụ: thử 100 lần) để sinh các phòng ngẫu nhiên:
    *   Sinh ngẫu nhiên chiều rộng `w` và chiều cao `h` trong một khoảng giới hạn (ví dụ: từ 4 đến 10 ô).
    *   Sinh ngẫu nhiên một tọa độ góc dưới bên trái `bottomLeft` sao cho căn phòng nằm hoàn toàn bên trong biên thế giới `(width, height)`.
    *   Tạo đối tượng `Room potentialRoom = new Room(bottomLeft, w, h)`.
    *   Kiểm tra va chạm: Duyệt qua tất cả các phòng đã được chấp nhận trong `rooms`. Nếu `potentialRoom.overlaps(existingRoom)` trả về `true` đối với bất kỳ phòng nào, ta loại bỏ căn phòng này.
    *   Nếu không có va chạm, thêm `potentialRoom` vào `rooms`.

### Bước 2: Sinh hành lang liên kết các phòng (Connected Hallway Generation)
Để đảm bảo tất cả các căn phòng đều liên thông với nhau (không có phòng nào bị cô lập như yêu cầu nghiêm ngặt của spec), chúng ta kết nối các phòng theo chuỗi tuyến tính (Room $i$ nối với Room $i+1$):
1.  Duyệt qua danh sách các phòng từ `0` đến `rooms.size() - 2`.
2.  Với mỗi cặp phòng `r1 = rooms.get(i)` và `r2 = rooms.get(i+1)`:
    *   Lấy tâm của hai phòng: `start = r1.getCenter()` và `end = r2.getCenter()`.
    *   Xây dựng một đường đi hình chữ L nối `start` và `end`.
    *   Tạo danh sách `List<Position> path` chứa các tọa độ chuyển tiếp:
        *   Cố định tọa độ $Y$ tại `start.y`, thay đổi $X$ dần dần từ `start.x` đến `end.x`. Thêm mỗi `Position(currentX, start.y)` vào `path`.
        *   Cố định tọa độ $X$ tại `end.x`, thay đổi $Y$ dần dần từ `start.y` đến `end.y`. Thêm mỗi `Position(end.x, currentY)` vào `path`.
    *   Tạo đối tượng `Hallway hallway = new Hallway(r1, r2, path)` và thêm vào danh sách `List<Hallway> hallways`.

*Chứng minh toán học:* Vì mọi căn phòng đều được nối trực tiếp với phòng kế tiếp trong danh sách theo một chuỗi không đứt đoạn ($0 \leftrightarrow 1 \leftrightarrow 2 \leftrightarrow \dots \leftrightarrow N$), đồ thị các phòng sẽ tạo thành một thành phần liên thông duy nhất (single connected component). Do đó, người chơi luôn có thể đi đến bất kỳ căn phòng nào trong thế giới game.

### Bước 3: Vẽ và Biên dịch thế giới (World Rendering & Compilation)
Ở giai đoạn này, chúng ta tiến hành đổ gạch thực tế xuống mảng gạch hai chiều:
1.  Khởi tạo mảng `TETile[][] world` kích thước `[width][height]`.
2.  Phủ kín mảng `world` bằng gạch nền mặc định `Tileset.NOTHING`.
3.  Vẽ toàn bộ các căn phòng bằng cách gọi `r.draw(world)` cho từng `Room r` trong `rooms`. Giai đoạn này tạo ra các ô gạch `FLOOR` và bao quanh chúng bằng tường `WALL`.
4.  Vẽ toàn bộ hành lang bằng cách gọi `h.draw(world)` cho từng `Hallway h` trong `hallways`. Giai đoạn này rải các ô `FLOOR` làm đường đi hành lang và tự động đặt thêm `WALL` ở các hướng xung quanh nếu ô đó đang là `NOTHING` (tránh ghi đè lên gạch sàn `FLOOR` của các phòng kề bên).

---

## 3. Complexity & Performance Analysis

### Time Complexity (Độ phức tạp thời gian)
*   **Sinh phòng:** Gọi $N$ là số lần thử sinh phòng (ví dụ: $100$) và $R$ là số phòng được chấp nhận thành công. Với mỗi phòng mới, ta duyệt qua tối đa $R$ phòng cũ để kiểm tra va chạm. Do đó, độ phức tạp là $O(N \cdot R)$. Vì $R \le N$, thời gian chạy thực tế cực kỳ nhỏ và gần như tức thời.
*   **Sinh hành lang:** Ta duyệt qua $R-1$ cặp phòng để xây dựng đường đi chữ L. Độ phức tạp tỉ lệ tuyến tính với khoảng cách Manhattan giữa các phòng, tức là $O(R \cdot (W + H))$ trong trường hợp xấu nhất.
*   **Vẽ thế giới:** Ta thực hiện vẽ $R$ phòng và $H$ hành lang. Thao tác này tương đương với việc gán giá trị cho các ô gạch trên mảng hai chiều. Độ phức tạp thời gian là $O(W \cdot H)$ vì mảng thế giới có số ô cố định ($80 \times 30$).
*   **Tổng kết:** Thuật toán chạy trong thời gian tối ưu **$O(W \cdot H + N \cdot R)$**, đảm bảo thế giới được sinh ra ngay lập tức sau khi nhấn phím `S` mà không gây ra bất kỳ độ trễ nào cho người chơi.

### Space Complexity (Độ phức tạp không gian)
*   Để lưu trữ dữ liệu logic, chúng ta cần $O(R)$ cho danh sách phòng và $O(R \cdot (W+H))$ cho danh sách tọa độ của hành lang.
*   Mảng thế giới cần dung lượng cố định là $O(W \cdot H)$ ô gạch `TETile`.
*   **Tổng kết:** Độ phức tạp không gian là cực kỳ tối ưu, chỉ tốn khoảng vài kilobytes bộ nhớ RAM để quản lý.

---

## 4. Edge Cases & Solutions (Các trường hợp đặc biệt)

1.  **Tránh lỗi `NumberFormatException` khi phân tích hạt giống (seed):**
    *   *Sự cố:* Hạt giống người dùng nhập vào có thể rất lớn và vượt quá giới hạn lưu trữ của kiểu số nguyên `int` thông thường (32-bit). Nếu sử dụng `Integer.parseInt()`, chương trình sẽ bị sụp đổ (crash).
    *   *Giải pháp:* Luôn sử dụng lớp wrapper `Long` và phương thức `Long.parseLong(seedString)` để hỗ trợ các hạt giống lớn lên tới $9,223,372,036,854,775,807$ (số nguyên 64-bit) theo đúng yêu cầu đặc tả của đề bài.

2.  **Xử lý va chạm biên đồ thị (Out of Bounds):**
    *   *Sự cố:* Khi sinh các căn phòng ngẫu nhiên ở gần viền bản đồ, nếu chiều rộng `w` hoặc chiều cao `h` cộng với tọa độ `bottomLeft` vượt quá giới hạn bản đồ, chương trình sẽ báo lỗi `ArrayIndexOutOfBoundsException`.
    *   *Giải pháp:* Khi sinh phòng ngẫu nhiên, ta giới hạn tọa độ của góc `bottomLeft` luôn thỏa mãn:
        $$bottomLeft.x \in [1, WIDTH - w - 1]$$
        $$bottomLeft.y \in [1, HEIGHT - h - 1]$$
        Khoảng cách lùi lại 1 ô gạch giúp đảm bảo phần tường biên của phòng luôn nằm trọn vẹn trong lưới bản đồ mà không bị tràn ra ngoài.

3.  **Tường hành lang đè lên sàn phòng (Information Hiding & Tile Overwriting):**
    *   *Sự cố:* Khi hành lang đi sát qua một căn phòng hoặc một hành lang khác, bước xây tường bao xung quanh hành lang có thể vô tình đè gạch `WALL` lên các ô gạch `FLOOR` (sàn đi được) đã vẽ trước đó.
    *   *Giải pháp:* Trong phương thức `draw()` của hành lang, khi quét 8 hướng xung quanh các ô sàn hành lang để đặt gạch tường, ta chỉ đặt gạch `WALL` tại những ô đang có giá trị là `Tileset.NOTHING`. Nếu ô đó đã là `Tileset.FLOOR`, ta tuyệt đối giữ nguyên để bảo toàn khả năng di chuyển liên tục cho người chơi.
