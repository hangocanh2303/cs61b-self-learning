# BYOW Design Document
**Author:** Anh Ha
**Course:** CS 61B - UC Berkeley

---

## 1. Classes and Data Structures

### Position
This class represents the coordinates of a tile on the two-dimensional coordinate grid of the game world. Using this class encapsulates primitive coordinates, increases the level of abstraction, and avoids the "Primitive Obsession" code smell.

#### Fields
1. `public final int x`: The x-coordinate.
2. `public final int y`: The y-coordinate.

#### Methods
*   `@Override public boolean equals(Object o)`: Overridden to compare equality by the coordinate values `(x, y)` rather than by the object's memory address. Ensures compliance with reflexivity, symmetry, and transitivity.
*   `@Override public int hashCode()`: Overridden consistently with `equals` to ensure that two `Position` objects with the same coordinates produce the same hash code, allowing efficient use in hash-based data structures such as `HashSet` and `HashMap`.

---

### Room
This class represents a rectangular room in the world. It is a high-level logical module that holds the room's geometric parameters and position.

#### Fields
1. `public final Position bottomLeft`: The coordinates of the room's bottom-left corner (using a `Position` object to manage the location).
2. `public final int width`: The width of the room (including the walls).
3. `public final int height`: The height of the room (including the walls).

#### Methods
*   `public Position getCenter()`: Calculates and returns the center coordinates (room center) as a `Position` object. This method is extremely useful as a reference point for routing hallways that connect rooms.
*   `public boolean overlaps(Room other)`: Checks whether the current room overlaps with another room. The checking formula is:
    $$\text{overlaps} = (x_1 < x_2 + w_2) \land (x_1 + w_1 > x_2) \land (y_1 < y_2 + h_2) \land (y_1 + h_1 > y_2)$$
*   `public void draw(TETile[][] world)`: Compiles the geometric data into physical tiles. This method traverses the room's spatial region in the `world` array and assigns `Tileset.WALL` to boundary tiles and `Tileset.FLOOR` to interior tiles.

---

### Hallway
This class represents hallways connecting rooms. A hallway is essentially a continuous sequence of floor (`FLOOR`) tiles connecting the start point to the destination and surrounded by walls (`WALL`).

#### Fields
1. `public final Room room1`: The starting room.
2. `public final Room room2`: The destination room to connect.
3. `public final List<Position> path`: A list of `Position` objects representing the hallway's floor (`FLOOR`) tiles. Storing the path as a list of coordinates makes it easy to build the surrounding walls later.

#### Methods
*   `public void draw(TETile[][] world)`: Draws the hallway in the game world using a 2-step algorithm:
    1.  Iterate through the `path` list, setting the tiles at the corresponding coordinates to `Tileset.FLOOR`.
    2.  For each tile in `path`, check the 8 surrounding neighboring tiles. If any tile is currently empty space (`Tileset.NOTHING`), turn it into `Tileset.WALL` to automatically build protective walls around the hallway without overwriting the floor of a room or another hallway.

---

### MapGenerator (World Generator Coordinator)
This class coordinates the entire lifecycle of random world generation. It is responsible for receiving a seed value (`seed`), calculating the distribution of rooms and hallways as logical objects, and then rendering them onto the `TETile[][]` array.

#### Fields
1. `private final int width`: The width of the world (typically 80 tiles).
2. `private final int height`: The height of the world (typically 30 tiles).
3. `private final Random random`: The pseudorandom number generator initialized with a `long`-type `seed` to ensure determinism.

#### Methods
*   `public TETile[][] generate()`: The main method that returns the completed `TETile[][]` array after sequentially executing the 3 world-generation steps.

---

## 2. Algorithms

The world-generation algorithm is designed according to the principles of **Divide and Conquer** and **Hierarchical Abstraction**, dividing the process into 3 independent, clearly separated steps:

### Step 1: Random Room Generation
1.  Initialize an empty list `List<Room> rooms = new ArrayList<>()`.
2.  Run a fixed number of iterations (for example, 100 attempts) to generate random rooms:
    *   Randomly generate the width `w` and height `h` within a bounded range (for example, from 4 to 10 tiles).
    *   Randomly generate the bottom-left corner coordinate `bottomLeft` so that the room lies completely within the world boundaries `(width, height)`.
    *   Create the object `Room potentialRoom = new Room(bottomLeft, w, h)`.
    *   Collision check: Iterate through all accepted rooms in `rooms`. If `potentialRoom.overlaps(existingRoom)` returns `true` for any room, discard this room.
    *   If there is no collision, add `potentialRoom` to `rooms`.

### Step 2: Connected Hallway Generation
To ensure that all rooms are connected to one another (with no isolated rooms as strictly required by the spec), we connect the rooms in a linear chain (Room $i$ connects to Room $i+1$):
1.  Iterate through the list of rooms from `0` to `rooms.size() - 2`.
2.  For each pair of rooms `r1 = rooms.get(i)` and `r2 = rooms.get(i+1)`:
    *   Get the centers of the two rooms: `start = r1.getCenter()` and `end = r2.getCenter()`.
    *   Construct an L-shaped path connecting `start` and `end`.
    *   Create a `List<Position> path` containing the intermediate coordinates:
        *   Fix the $Y$ coordinate at `start.y`, gradually changing $X$ from `start.x` to `end.x`. Add each `Position(currentX, start.y)` to `path`.
        *   Fix the $X$ coordinate at `end.x`, gradually changing $Y$ from `start.y` to `end.y`. Add each `Position(end.x, currentY)` to `path`.
    *   Create the object `Hallway hallway = new Hallway(r1, r2, path)` and add it to the `List<Hallway> hallways`.

*Mathematical proof:* Because every room is directly connected to the next room in the list in an unbroken chain ($0 \leftrightarrow 1 \leftrightarrow 2 \leftrightarrow \dots \leftrightarrow N$), the graph of rooms forms a single connected component. Therefore, the player can always reach any room in the game world.

### Step 3: World Rendering & Compilation
At this stage, we actually populate the two-dimensional tile array:
1.  Initialize the `TETile[][] world` array with dimensions `[width][height]`.
2.  Fill the entire `world` array with the default background tile `Tileset.NOTHING`.
3.  Draw all rooms by calling `r.draw(world)` for each `Room r` in `rooms`. This stage creates `FLOOR` tiles and surrounds them with `WALL` tiles.
4.  Draw all hallways by calling `h.draw(world)` for each `Hallway h` in `hallways`. This stage lays down `FLOOR` tiles as hallway paths and automatically adds `WALL` tiles in the surrounding directions if the tile is currently `NOTHING` (avoiding overwriting `FLOOR` tiles of adjacent rooms).

---

## 3. Complexity & Performance Analysis

### Time Complexity
*   **Room generation:** Let $N$ be the number of room-generation attempts (for example, $100$) and $R$ be the number of successfully accepted rooms. For each new room, we iterate through up to $R$ existing rooms to check for collisions. Therefore, the complexity is $O(N \cdot R)$. Since $R \le N$, the actual runtime is extremely small and nearly instantaneous.
*   **Hallway generation:** We iterate through $R-1$ pairs of rooms to construct L-shaped paths. The complexity is linear in the Manhattan distance between rooms, i.e. $O(R \cdot (W + H))$ in the worst case.
*   **World rendering:** We render $R$ rooms and $H$ hallways. This operation is equivalent to assigning values to tiles in the two-dimensional array. The time complexity is $O(W \cdot H)$ because the world array has a fixed number of tiles ($80 \times 30$).
*   **Summary:** The algorithm runs in the optimal time **$O(W \cdot H + N \cdot R)$**, ensuring that the world is generated immediately after pressing the `S` key without causing any delay for the player.

### Space Complexity
*   To store the logical data, we need $O(R)$ for the room list and $O(R \cdot (W+H))$ for the hallway coordinate lists.
*   The world array requires a fixed amount of space of $O(W \cdot H)$ `TETile` tiles.
*   **Summary:** The space complexity is extremely efficient, requiring only a few kilobytes of RAM to manage.

---

## 4. Edge Cases & Solutions

1.  **Avoiding `NumberFormatException` when parsing the seed:**
    *   *Issue:* The seed entered by the user can be very large and exceed the storage limit of the standard `int` integer type (32-bit). If `Integer.parseInt()` is used, the program will crash.
    *   *Solution:* Always use the `Long` wrapper class and the `Long.parseLong(seedString)` method to support large seeds up to $9,223,372,036,854,775,807$ (64-bit integer), as required by the assignment specification.

2.  **Handling boundary collisions (Out of Bounds):**
    *   *Issue:* When generating random rooms near the edge of the map, if the width `w` or height `h` combined with the `bottomLeft` coordinate exceeds the map boundaries, the program will report an `ArrayIndexOutOfBoundsException`.
    *   *Solution:* When generating random rooms, we constrain the coordinates of the `bottomLeft` corner to always satisfy:
        $$bottomLeft.x \in [1, WIDTH - w - 1]$$
        $$bottomLeft.y \in [1, HEIGHT - h - 1]$$
        The 1-tile margin ensures that the room's boundary walls remain completely within the map grid without going out of bounds.

3.  **Hallway walls overwriting room floors (Information Hiding & Tile Overwriting):**
    *   *Issue:* When a hallway passes close to a room or another hallway, the step that builds walls around the hallway may inadvertently place `WALL` tiles over previously rendered `FLOOR` tiles (walkable floors).
    *   *Solution:* In the hallway's `draw()` method, when scanning the 8 directions around hallway floor tiles to place wall tiles, we only place a `WALL` tile on cells whose current value is `Tileset.NOTHING`. If the cell is already `Tileset.FLOOR`, we leave it unchanged to preserve continuous player movement.
