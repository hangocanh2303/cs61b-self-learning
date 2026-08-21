package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.util.*;

public class MapGenerator {
    public static final int WIDTH = 60;
    public static final int HEIGHT = 40;

    private final TETile[][] tiles;

    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random(SEED);

    private static final int TARGET_ROOM_COUNT = 50;
    private static final int MAX_ATTEMPTS = 6000;



    public MapGenerator() {
        tiles = new TETile[WIDTH][HEIGHT];
        fillWithEmptyTiles();
    }

    private void fillWithEmptyTiles() {
        for (int i = 0; i < WIDTH; i += 1) {
            for (int j = 0; j < HEIGHT; j += 1) {
                tiles[i][j] = Tileset.NOTHING;
            }
        }
    }

    public TETile[][] generate() {
        // step 1: generate random room with no overlaps
//        List<Room> rooms = generateRandomRoom();
//
        List<Room> allComponents = new ArrayList<>();
        List<Room> activeRooms = new ArrayList<>();

        int seedW = RandomUtils.uniform(RANDOM, 6, 12);
        int seedH = RandomUtils.uniform(RANDOM, 6, 12);
        Position seedPos = new Position(WIDTH / 2 - seedW / 2, HEIGHT / 2 - seedH / 2);
        Room seedRoom = new Room(seedPos, seedW, seedH);

        allComponents.add(seedRoom);
        activeRooms.add(seedRoom);

        int targetComponentsCount = TARGET_ROOM_COUNT * 2 - 1;
        int attempts = 0;

        while (allComponents.size() < targetComponentsCount
        && !activeRooms.isEmpty()
        && attempts < MAX_ATTEMPTS) {
            attempts += 1;
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

        // Pass 1: Vẽ tất cả FLOOR trước
        for (Room comp: allComponents) {
            comp.draw(tiles);
        }
        
        // Pass 2: Vẽ WALL ở những ô còn là NOTHING
        for (Room comp: allComponents) {
            comp.drawWalls(tiles);
        }
        return tiles;
    }


    private static class BranchResult {
        final Room hallway;
        final Room childRoom;

        BranchResult(Room hallway, Room childRoom) {
            this.hallway = hallway;
            this.childRoom = childRoom;
        }
    }

    private BranchResult tryCreateBranch(Room parent, List<Room> allComponents) {
        List<Integer> directions = new ArrayList<>(Arrays.asList(0, 1, 2, 3));

        Collections.shuffle(directions, RANDOM);

        for (int direction: directions) {
            int hallwayLength = RandomUtils.uniform(RANDOM, 4, 10);
            int childW = RandomUtils.uniform(RANDOM, 4, 10);
            int childH = RandomUtils.uniform(RANDOM, 4, 10);

            Room candidateHallway = createHallway(parent, direction, hallwayLength);
            Room candidateChild = createNeighborRoom(candidateHallway, direction, childW, childH);

            if (inBounds(candidateHallway) && inBounds(candidateChild)
                    && !overlapsAny(allComponents, candidateHallway, parent)
                    && !overlapsAny(allComponents, candidateChild, null)) {
                return new BranchResult(candidateHallway, candidateChild);
            }
        }
        return null;
    }

    private boolean inBounds(Room r) {
        Position bl = r.getBottomLeft();
        int rx = bl.getX();
        int ry = bl.getY();
        int rw = r.getWidth();
        int rh = r.getHeight();

        return (rx > 0 && (rx + rw) < WIDTH - 1 && ry > 0 && (ry + rh) < HEIGHT - 1);
    }

    private boolean overlapsAny(List<Room> existing, Room candidate, Room excludeParent) {
        for (Room r : existing) {
            if (r == excludeParent) {
                continue; // Bỏ qua phòng mẹ vì hành lang cố tình gối biên với nó
            }
            if (r.overlaps(candidate)) {
                return true;
            }
        }
        return false;
    }

    private Room createHallway(Room parent, int direction, int length) {
        Position pPos = parent.getBottomLeft();
        int px = pPos.getX();
        int py = pPos.getY();
        int pw = parent.getWidth();
        int ph = parent.getHeight();
        
        // Phần FLOOR của phòng: cột [px+1, px+pw-2], hàng [py+1, py+ph-2]
        // Hành lang 3 ô: biên-FLOOR-biên
        // Cần ô FLOOR của hành lang nằm TRONG vùng FLOOR của phòng

        switch (direction) {
            case 0: // UP - hành lang dọc đi lên
                // Chọn cột FLOOR của hành lang trong vùng FLOOR phòng
                int floorColUp = px + pw / 2; // mặc định tâm
                if (pw >= 5) {
                    floorColUp = RandomUtils.uniform(RANDOM, px + 2, px + pw - 2);
                }
                // Hành lang: bottomLeft tại (floorCol-1, py+ph-2)
                // -> hàng py+ph-2 là biên dưới hành lang (WALL)
                // -> hàng py+ph-1 là FLOOR của hành lang, trùng với WALL phòng
                // Sai! Cần hàng FLOOR của hành lang trùng với FLOOR phòng
                // -> bottomLeft.y = py + ph - 3 để hàng py+ph-2 (FLOOR hành lang) = row FLOOR phòng
                return new Room(new Position(floorColUp - 1, py + ph - 3), 3, length);
                
            case 1: // DOWN
                int floorColDown = px + pw / 2;
                if (pw >= 5) {
                    floorColDown = RandomUtils.uniform(RANDOM, px + 2, px + pw - 2);
                }
                // Hành lang đi xuống: hàng top của hành lang = py+1 (FLOOR phòng)
                // bottomLeft.y + length - 2 = py + 1 -> bottomLeft.y = py + 3 - length
                return new Room(new Position(floorColDown - 1, py + 3 - length), 3, length);
                
            case 2: // LEFT
                int floorRowLeft = py + ph / 2;
                if (ph >= 5) {
                    floorRowLeft = RandomUtils.uniform(RANDOM, py + 2, py + ph - 2);
                }
                // Hành lang ngang: cột right của hành lang = px+1 (FLOOR phòng)
                // bottomLeft.x + length - 2 = px + 1 -> bottomLeft.x = px + 3 - length
                return new Room(new Position(px + 3 - length, floorRowLeft - 1), length, 3);
                
            case 3: // RIGHT
                int floorRowRight = py + ph / 2;
                if (ph >= 5) {
                    floorRowRight = RandomUtils.uniform(RANDOM, py + 2, py + ph - 2);
                }
                // Hành lang ngang: cột left+1 (FLOOR) của hành lang = px+pw-2 (FLOOR phòng)
                // bottomLeft.x + 1 = px + pw - 2 -> bottomLeft.x = px + pw - 3
                return new Room(new Position(px + pw - 3, floorRowRight - 1), length, 3);
                
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
        int hCenterX = hx + hw / 2;  // Cột FLOOR của hành lang dọc (width=3)
        int hCenterY = hy + hh / 2;  // Hàng FLOOR của hành lang ngang (height=3)

        // Phòng con cần có FLOOR bao phủ ô FLOOR của đầu hành lang
        switch (direction) {
            case 0: // UP - hành lang đi lên, phòng con ở trên
                // Hàng FLOOR cuối của hành lang = hy + hh - 2
                // Phòng con cần có FLOOR tại hàng đó và cột hCenterX
                // -> py + 1 <= hy + hh - 2 -> py <= hy + hh - 3
                // Căn để hàng FLOOR đầu của phòng con = hàng FLOOR cuối của hành lang
                // py + 1 = hy + hh - 2 -> py = hy + hh - 3
                return new Room(new Position(hCenterX - cw / 2, hy + hh - 3), cw, ch);
                
            case 1: // DOWN - hành lang đi xuống, phòng con ở dưới
                // Hàng FLOOR đầu của hành lang = hy + 1
                // Phòng con cần có FLOOR tại hàng đó
                // py + ch - 2 = hy + 1 -> py = hy + 3 - ch
                return new Room(new Position(hCenterX - cw / 2, hy + 3 - ch), cw, ch);
                
            case 2: // LEFT - hành lang đi trái, phòng con ở trái
                // Cột FLOOR đầu của hành lang = hx + 1
                // Phòng con cần có FLOOR tại cột đó
                // px + cw - 2 = hx + 1 -> px = hx + 3 - cw
                return new Room(new Position(hx + 3 - cw, hCenterY - ch / 2), cw, ch);
                
            case 3: // RIGHT - hành lang đi phải, phòng con ở phải
                // Cột FLOOR cuối của hành lang = hx + hw - 2
                // Phòng con cần có FLOOR tại cột đó
                // px + 1 = hx + hw - 2 -> px = hx + hw - 3
                return new Room(new Position(hx + hw - 3, hCenterY - ch / 2), cw, ch);
                
            default:
                throw new IllegalArgumentException("Direction không hợp lệ!");
        }
    }

//    private List<Hallway> connectRoomsWithHallways(List<Room> rooms) {
//        List<Hallway> hallways = new ArrayList<>();
//
//        for (int i = 0; i <= rooms.size() - 2; i += 1) {
//            Room r1 = rooms.get(i);
//            Room r2 = rooms.get(i + 1);
//            Position start = r1.getCenter();
//            Position end = r2.getCenter();
//            Hallway hallway = new Hallway();
//
//            int hallSize = RandomUtils.uniform(RANDOM, 3, 5);
//
//            int horX = Math.min(start.getX(), end.getX());
//            int horY = start.getY();
//            int horWidth = Math.abs(start.getX() - end.getX()) + hallSize;
//            Position horBottomLeft = new Position(horX, horY);
//            Room horizontalSegment = new Room(horBottomLeft, horWidth, hallSize);
//
//            int vertX = end.getX();
//            int vertY = Math.min(start.getY(), end.getY());
//            int vertHeight = Math.abs(start.getY() - end.getY()) + hallSize;
//
//            Position vertBottomLeft = new Position(vertX, vertY);
//            Room verticalSegment = new Room(vertBottomLeft, hallSize, vertHeight);
//
//            hallway.addSegment(horizontalSegment);
//            hallway.addSegment(verticalSegment);
//
//            hallways.add(hallway);
//        }
//        return hallways;
//    }

//    private List<Room> generateRandomRoom() {
//        List<Room> rooms = new ArrayList<>();
//        for (int i = 0; i < 2; i += 1) {
//            int width = RandomUtils.uniform(RANDOM, 4, 10);
//            int height = RandomUtils.uniform(RANDOM, 4, 10);
//
//            int roomX = RandomUtils.uniform(RANDOM, 1, WIDTH - width);
//            int roomY = RandomUtils.uniform(RANDOM, 1, HEIGHT - height);
//
//            Position bottomLeft = new Position(roomX, roomY);
//            Room potentialRoom = new Room(bottomLeft, width, height);
//
//            boolean hasOverlaps = false;
//
//            for (Room exitsRoom : rooms) {
//                if (exitsRoom.overlaps(potentialRoom)) {
//                    hasOverlaps = true;
//                    break;
//                }
//            }
//
//            if (!hasOverlaps) {
//                rooms.add(potentialRoom);
//            }
//        }
//        return rooms;
//    }

    public TETile[][] getTiles() {
        return tiles;
    }

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        MapGenerator map = new MapGenerator();
        TETile[][] tiles = map.generate();

        ter.renderFrame(tiles);
    }
}
