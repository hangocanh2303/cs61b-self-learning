# BYOW Design Document
**Author:** Anh Ha  
**Course:** CS 61B - UC Berkeley

---

## 1. Classes and Data Structures

### Position
Lớp này đại diện cho tọa độ của một ô gạch (tile) trên lưới tọa độ hai chiều của thế giới game. Sử dụng lớp này giúp đóng gói các tọa độ nguyên thủy, tăng mức độ trừu tượng hóa và tránh lỗi "Primitive Obsession" (ám ảnh kiểu nguyên thủy).

#### Fields
1. `private final int x`: Tọa độ hoành độ.
2. `private final int y`: Tọa độ tung độ.

#### Methods
*   `@Override public boolean equals(Object o)`: Được ghi đè để so sánh tính bằng nhau về giá trị tọa độ `(x, y)` thay vì so sánh địa chỉ bộ nhớ của đối tượng. Đảm bảo tham số truyền vào là kiểu `Object` để thực hiện ghi đè chính xác (tránh lỗi nạp chồng - overloading).
*   `@Override public int hashCode()`: Được ghi đè nhất quán với `equals` để đảm bảo hai đối tượng `Position` có cùng tọa độ sẽ sinh ra cùng một mã băm, cho phép sử dụng hiệu quả trong các cấu trúc dữ liệu băm như `HashSet` và `HashMap`.

---

### WorldComponent (Interface)
Interface đóng vai trò là một "bản hợp đồng thiết kế", xác định hành vi chung cho mọi thành phần hình học vĩ mô cấu thành nên thế giới game. Điều này cho phép áp dụng **Đa hình kiểu con (Subtype Polymorphism)** trong hệ thống.

#### Methods
*   `void draw(TETile[][] world)`: Phương thức bắt buộc mọi lớp con phải hiện thực hóa để tự vẽ chính mình lên lưới thế giới vật lý.

---

### Room (Implements WorldComponent)
Lớp đại diện cho một căn phòng hình chữ nhật trong thế giới. Đây là một mô-đun logic ở cấp độ vĩ mô, nắm giữ thông số hình học và ranh giới bao của căn phòng. Chúng ta áp dụng thiết kế **Tường nằm trong biên (Wall-inclusive Bounding Box)**, nghĩa là chiều rộng và chiều cao của đối tượng `Room` bao trọn cả phần tường ngoài cùng.

#### Fields
1. `public final Position bottomLeft`: Tọa độ góc dưới bên trái của căn phòng (sử dụng đối tượng `Position`).
2. `public final int width`: Chiều rộng tổng thể của phòng (bao gồm cả tường, kích thước tối thiểu là 3x3 để có ít nhất 1 ô sàn đi được ở giữa).
3. `public final int height`: Chiều cao tổng thể của phòng (bao gồm cả tường).

#### Methods
*   `public Position getCenter()`: Tính toán và trả về tọa độ trung tâm (tâm phòng) dưới dạng đối tượng `Position`, làm mốc để kết nối hành lang.
*   `public boolean overlaps(Room other)`: Kiểm tra va chạm cực kỳ đơn giản và thuần khiết về mặt hình học nhờ thiết kế biên đã bao trọn cả tường. Công thức so sánh hai hình chữ nhật logic trực tiếp:
    $$\text{overlaps} = (x_1 < x_2 + w_2) \land (x_1 + w_1 > x_2) \land (y_1 < y_2 + h_2) \land (y_1 + h_1 > y_2)$$
*   `@Override public void draw(TETile[][] world)`: Duyệt qua vùng không gian phòng trên lưới mảng và gán `Tileset.WALL` cho các ô nằm ở biên hình chữ nhật, và gán `Tileset.FLOOR` cho các ô trống vùng lõi bên trong.

---

### Hallway (Implements WorldComponent)
Lớp đại diện cho các hành lang kết nối giữa các căn phòng. Để giải quyết bài toán chống trùng lặp mã nguồn (code duplication), lớp này được thiết kế theo hướng **Thành phần (Composition over Inheritance)**. Thay vì viết lại logic vẽ sàn và tường lặp đi lặp lại, hành lang chữ L hoặc lắt léo được coi là tập hợp các phân đoạn thẳng giao nhau. Mỗi phân đoạn thẳng này thực chất là một căn phòng siêu hẹp (ví dụ: `width = 2` hoặc `height = 2`).

#### Fields
1. `private final List<Room> segments`: Danh sách các phân đoạn, mỗi phân đoạn là một đối tượng `Room` siêu hẹp đóng vai trò là đường đi thẳng ngang hoặc dọc.

#### Methods
*   `public void addSegment(Room narrowRoom)`: Thêm một phân đoạn phòng hẹp vào hành lang.
*   `@Override public void draw(TETile[][] world)`: Tái sử dụng 100% logic vẽ của lớp `Room`. Duyệt qua danh sách `segments` và gọi:
    ```java
    for (Room segment : segments) {
        segment.draw(world);
    }
    ```

---

### MapGenerator (World Generator Coordinator)
Lớp điều phối trung tâm nhận hạt giống (`seed`), tính toán dữ liệu logic của các `WorldComponent` và "biên dịch" xuống mảng gạch vật lý tĩnh `TETile[][]`.

#### Fields
1. `private final int width`: Chiều rộng thế giới (80 ô).
2. `private final int height`: Chiều cao thế giới (30 ô).
3. `private final Random random`: Bộ sinh số giả ngẫu nhiên được khởi tạo bằng `seed`.

#### Methods
*   `public TETile[][] generate()`: Phương thức chính trả về mảng `TETile[][]` hoàn chỉnh sau khi chạy tuần tự 3 bước sinh thế giới logic và vật lý.

---

## 2. Algorithms

Thuật toán sinh thế giới được thiết kế theo nguyên lý **Chia để trị** và **Tách biệt mối quan tâm (Separation of Concerns)**, phân rã quy trình thành 3 bước rõ ràng:

### Bước 1: Sinh phòng ngẫu nhiên (Random Room Generation)
1.  Khởi tạo một danh sách rỗng lưu trữ các phòng hợp lệ: `List<Room> rooms = new ArrayList<>()`.
2.  Chạy một vòng lặp thử nghiệm cố định (ví dụ: thử 100 lần) để phân bổ phòng ngẫu nhiên:
    *   Sinh ngẫu nhiên chiều rộng `w` và chiều cao `h` (khoảng từ 4 đến 10 ô).
    *   Sinh ngẫu nhiên tọa độ `bottomLeft` sao cho căn phòng luôn nằm trọn vẹn và an toàn bên trong biên thế giới (chừa lùi lại 1 ô biên làm tường bao):
        $$bottomLeft.x \in [1, WIDTH - w - 1]$$
        $$bottomLeft.y \in [1, HEIGHT - h - 1]$$
    *   Khởi tạo đối tượng phòng logic: `Room potentialRoom = new Room(bottomLeft, w, h)`.
    *   Kiểm tra va chạm logic: Duyệt qua danh sách `rooms` đã được chấp nhận. Nếu `potentialRoom.overlaps(existingRoom)` trả về `true` đối với bất kỳ phòng nào, căn phòng ngẫu nhiên này bị hủy bỏ.
    *   Nếu không chồng lấn, thêm `potentialRoom` vào `rooms`.

### Bước 2: Sinh hành lang liên kết các phòng (Connected Hallway Generation)
Để đảm bảo thế giới luôn liên thông hoàn toàn (không có phòng nào bị cô lập như yêu cầu nghiêm ngặt của spec), ta kết nối các phòng logic theo một chuỗi tuyến tính không đứt đoạn (Room $i$ nối với Room $i+1$):
1.  Duyệt danh sách các phòng từ `0` đến `rooms.size() - 2`.
2.  Với mỗi cặp phòng kề nhau `r1 = rooms.get(i)` và `r2 = rooms.get(i+1)`:
    *   Lấy hai điểm tâm phòng đại diện: `start = r1.getCenter()` và `end = r2.getCenter()`.
    *   Tạo ra một đối tượng `Hallway hallway = new Hallway()`.
    *   Tạo đường đi chữ L nối `start` và `end` bằng 2 phân đoạn phòng siêu hẹp (`Room` có chiều rộng hoặc chiều cao là 2 ô):
        *   **Phân đoạn ngang (Horizontal Segment):** Khởi tạo một `Room` hẹp chạy dọc theo trục X từ `start.x` đến `end.x` tại vị trí `start.y`. Thêm phân đoạn này vào `hallway` qua `addSegment()`.
        *   **Phân đoạn dọc (Vertical Segment):** Khởi tạo một `Room` hẹp chạy dọc theo trục Y từ `start.y` đến `end.y` tại vị trí `end.x`. Thêm phân đoạn này vào `hallway` qua `addSegment()`.
    *   Thêm `hallway` vào danh sách `List<Hallway> hallways`.

*Chứng minh toán học về tính liên thông:* Vì mọi căn phòng đều kết nối với phòng kế tiếp tạo thành một chuỗi liên tục ($0 \\leftrightarrow 1 \\dots \\leftrightarrow N$), đồ thị của thế giới luôn là một thành phần liên thông duy nhất.

### Bước 3: Biên dịch thế giới và Tự động đục lỗ cửa (World Compilation & Drawing Order)
Ở bước cuối cùng này, mảng `TETile[][] world` được khởi tạo và phủ kín bằng `Tileset.NOTHING`. Chúng ta gộp chung tất cả các phòng và hành lang vào danh sách các `WorldComponent` và tiến hành vẽ theo **quy tắc thứ tự lớp (Drawing Order / Overwriting)** để tự động mở cửa phòng mà không cần các tính toán giao điểm phức tạp:

1.  **VẼ PHÒNG TRƯỚC:** Duyệt qua `rooms` và gọi `r.draw(world)`. Tất cả các căn phòng được vẽ hoàn chỉnh với ranh giới tường bao đóng kín xung quanh sàn.
2.  **VẼ HÀNH LANG SAU:** Duyệt qua `hallways` và gọi `h.draw(world)`. Do hành lang được xây dựng kết nối trực tiếp các tâm phòng, đường đi của hành lang chắc chắn sẽ cắt ngang qua tường bao đóng kín của phòng. 
    *   *Cơ chế tự động đục tường:* Khi vẽ hành lang, các ô sàn `Tileset.FLOOR` của phân đoạn hành lang sẽ được ghi đè trực tiếp lên mảng `world`. Tại điểm giao cắt hình học giữa hành lang và phòng, các ô `Tileset.WALL` của phòng **tự động bị thay thế** thành `Tileset.FLOOR`. Cửa phòng tự động được đục mở một cách hoàn hảo mà không hề phát sinh lỗi "off-by-one"!
    *   *Logic xây tường hành lang an toàn:* Trong phương thức vẽ của các phân đoạn phòng hẹp thuộc hành lang, khi sinh các ô gạch tường bao quanh `WALL` lân cận, ta chỉ đặt gạch `Tileset.WALL` tại những tọa độ đang là `Tileset.NOTHING`. Nếu tọa độ lân cận đó đã là gạch sàn `Tileset.FLOOR` của phòng hoặc hành lang khác, ta tuyệt đối giữ nguyên để tránh việc xây tường chắn ngay lối đi của người chơi.

Sử dụng cơ chế đa hình kiểu con giúp quy trình vẽ thế giới trong `MapGenerator` cực kỳ tinh gọn:
```java
TETile[][] world = new TETile[width][height];
// Khởi tạo world bằng Tileset.NOTHING ...

List<WorldComponent> components = new ArrayList<>();
components.addAll(rooms);      // Vẽ phòng kín trước
components.addAll(hallways);   // Vẽ hành lang sau để đè và tự đục cửa

for (WorldComponent comp : components) {
    component.draw(world);
}
```

---

## 3. Complexity & Performance Analysis

### Time Complexity (Độ phức tạp thời gian)
*   **Sinh phòng logic:** Chạy vòng lặp thử $N$ lần, kiểm tra va chạm với danh sách tối đa $R$ phòng thành công. Độ phức tạp là $O(N \\cdot R)$, chạy trong vài mili-giây.
*   **Sinh hành lang logic:** Duyệt qua $R-1$ cặp phòng để sinh các phân đoạn phòng hẹp chữ L. Độ phức tạp là $O(R)$, cực kỳ nhanh chóng.
*   **Vẽ thế giới vật lý:** Gọi các phương thức `draw()` để gán tham chiếu ô gạch tĩnh trên mảng mốc. Vì số ô gạch của mảng là cố định, độ phức tạp thời gian vẽ là $O(WIDTH \\times HEIGHT)$.
*   **Tổng kết:** Độ phức tạp thời gian tổng thể đạt mức tối ưu tuyến tính **$O(W \\cdot H + N \\cdot R)$**, giúp sinh và dựng bản đồ ngay lập tức mà không gây độ trễ cảm nhận được.

### Space Complexity (Độ phức tạp không gian)
*   Bộ nhớ động sử dụng để lưu các đối tượng logic hình học trong quá trình tính toán là $O(R)$ cho phòng và $O(R)$ cho hành lang.
*   Lưới mảng thế giới cần dung lượng cố định là $O(W \\cdot H)$ để lưu trữ các tham chiếu `TETile`.
*   **Tổng kết:** Độ phức tạp không gian cực kỳ nhỏ, chỉ chiếm dụng vài kilobytes tài nguyên RAM.

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
        Khoảng cách lùi lại 1 ô gạch giúp đảm bảo phần tường biên của phòng luôn nằm trọn vẹn trong lưới bản đồ mà không bị tràn ra ngoài biên.

3.  **Tường hành lang đè lên sàn phòng (Information Hiding & Tile Overwriting):**
    *   *Sự cố:* Khi hành lang đi sát qua một căn phòng hoặc một hành lang khác, bước xây tường bao xung quanh hành lang có thể vô tình đè gạch `WALL` lên các ô gạch `FLOOR` (sàn đi được) đã vẽ trước đó.
    *   *Giải pháp:* Trong phương thức `draw()` của hành lang, khi quét 8 hướng xung quanh các ô sàn hành lang để đặt gạch tường, ta chỉ đặt gạch `Tileset.WALL` tại những ô đang có giá trị là `Tileset.NOTHING`. Nếu ô đó đã là `Tileset.FLOOR`, ta tuyệt đối giữ nguyên để bảo toàn khả năng di chuyển liên tục cho người chơi.
