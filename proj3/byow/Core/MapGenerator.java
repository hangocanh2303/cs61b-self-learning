package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MapGenerator {

    public static final int WIDTH = Engine.WIDTH;
    public static final int HEIGHT = Engine.HEIGHT;
    private final TETile[][] world;
    private final Random random;

    private static final int MAX_ROOMS = 50;
    private static final int MIN_ROOM_SIZE = 4;
    private static final int MAX_ROOM_SIZE = 10;
    private static final int MAX_ATTEMPTS = 1000;
    private static final int DEMO_SEED = 124;
    private static final double FLOWER_CHANCE = 0.0015;
    private static final double GOLD_CHANCE = 0.0025;

    public MapGenerator(long seed) {
        this.world = new TETile[WIDTH][HEIGHT];
        this.random = new Random(seed);
        initializeWorld();
    }

    private void initializeWorld() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                world[x][y] = Tileset.NOTHING;
            }
        }
    }


    public TETile[][] generate() {
        // step 1: create random rooms
        List<Room> rooms = generateRooms();

        // step 2: connect rooms by hallway
        connectRooms(rooms);

        // step 3: create wall
        generateWalls();

        populateItems(world);

        return world;
    }

    private List<Room> generateRooms() {
        List<Room> rooms = new ArrayList<>();
        int attempts = 0;

        int targetRoomCount =
                RandomUtils.uniform(random, 10, MAX_ROOMS + 1);

        while (rooms.size() < targetRoomCount && attempts < MAX_ATTEMPTS) {
            attempts++;
            int w = RandomUtils.uniform(random, MIN_ROOM_SIZE, MAX_ROOM_SIZE + 1);
            int h = RandomUtils.uniform(random, MIN_ROOM_SIZE, MAX_ROOM_SIZE + 1);

            int x = RandomUtils.uniform(random, 2, WIDTH - w - 1);
            int y = RandomUtils.uniform(random, 2, HEIGHT - h - 1);

            Room candidate = new Room(new Position(x, y), w, h);

            boolean overlaps = false;
            for (Room existing : rooms) {
                if (candidate.overlaps(existing)) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) {
                rooms.add(candidate);
                candidate.draw(world);
            }
        }
        return rooms;
    }

    private void connectRooms(List<Room> rooms) {
        if (rooms.size() < 2) {
            return;
        }
        rooms.sort(Comparator.comparingInt(r -> r.getCenter().getX()));
        for (int i = 0; i < rooms.size() - 1; i++) {
            Position p1 = rooms.get(i).getCenter();
            Position p2 = rooms.get(i + 1).getCenter();
            drawLHallway(p1, p2);
        }
    }

    private void drawLHallway(Position start, Position end) {
        int x1 = start.getX();
        int y1 = start.getY();
        int x2 = end.getX();
        int y2 = end.getY();

        int startX = Math.min(x1, x2);
        int endX = Math.max(x1, x2);
        for (int x = startX; x <= endX; x++) {
            world[x][y1] = Tileset.FLOOR;
        }

        int startY = Math.min(y1, y2);
        int endY = Math.max(y1, y2);
        for (int y = startY; y <= endY; y++) {
            world[x2][y] = Tileset.FLOOR;
        }
    }

    private void generateWalls() {
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (world[x][y].equals(Tileset.NOTHING)) {
                    boolean hasFloorNeighbor = false;
                    for (int i = 0; i < 8; i++) {
                        int nx = x + dx[i];
                        int ny = y + dy[i];

                        if (nx >= 0 && nx < WIDTH && ny >= 0 && ny < HEIGHT) {
                            if (world[nx][ny].equals(Tileset.FLOOR)) {
                                hasFloorNeighbor = true;
                                break;
                            }
                        }
                    }
                    if (hasFloorNeighbor) {
                        world[x][y] = Tileset.WALL;
                    }
                }
            }
        }
    }

    private void populateItems(TETile[][] tiles) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (tiles[x][y].equals(Tileset.FLOOR)) {
                    double roll = random.nextDouble();
                    if (roll < FLOWER_CHANCE) {
                        tiles[x][y] = Tileset.FLOWER;
                    } else if (roll < FLOWER_CHANCE + GOLD_CHANCE) {
                        tiles[x][y] = Tileset.GOLD;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);
        MapGenerator map = new MapGenerator(DEMO_SEED);
        TETile[][] tiles = map.generate();
        ter.renderFrame(tiles);
    }
}
