package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapGenerator {
    public static final int WIDTH = 80;
    public static final int HEIGHT = 60;

    private final TETile[][] tiles;

    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random(SEED);


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
        List<Room> rooms = generateRandomRoom();

        // step 2: connect room with hallways
        List<Hallway> hallways = connectRoomsWithHallways(rooms);

        // step 3: generate world
        List<WorldComponent> components = new ArrayList<>();
        components.addAll(rooms);
        components.addAll(hallways);

        for (WorldComponent comp : components) {
            comp.draw(tiles);
        }

        return tiles;
    }

    private List<Hallway> connectRoomsWithHallways(List<Room> rooms) {
        List<Hallway> hallways = new ArrayList<>();

        for (int i = 0; i <= rooms.size() - 2; i += 1) {
            Room r1 = rooms.get(i);
            Room r2 = rooms.get(i + 1);
            Position start = r1.getCenter();
            Position end = r2.getCenter();
            Hallway hallway = new Hallway();

            int hallSize = RandomUtils.uniform(RANDOM, 3, 5);

            int horX = Math.min(start.getX(), end.getX());
            int horY = start.getY();
            int horWidth = Math.abs(start.getX() - end.getX()) + hallSize;
            Position horBottomLeft = new Position(horX, horY);
            Room horizontalSegment = new Room(horBottomLeft, horWidth, hallSize);

            int vertX = end.getX();
            int vertY = Math.min(start.getY(), end.getY());
            int vertHeight = Math.abs(start.getY() - end.getY()) + hallSize;

            Position vertBottomLeft = new Position(vertX, vertY);
            Room verticalSegment = new Room(vertBottomLeft, hallSize, vertHeight);

            hallway.addSegment(horizontalSegment);
            hallway.addSegment(verticalSegment);

            hallways.add(hallway);
        }
        return hallways;
    }

    private List<Room> generateRandomRoom() {
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < 10; i += 1) {
            int width = RandomUtils.uniform(RANDOM, 4, 10);
            int height = RandomUtils.uniform(RANDOM, 4, 10);

            int roomX = RandomUtils.uniform(RANDOM, 1, WIDTH - width);
            int roomY = RandomUtils.uniform(RANDOM, 1, HEIGHT - height);

            Position bottomLeft = new Position(roomX, roomY);
            Room potentialRoom = new Room(bottomLeft, width, height);

            boolean hasOverlaps = false;

            for (Room exitsRoom : rooms) {
                if (exitsRoom.overlaps(potentialRoom)) {
                    hasOverlaps = true;
                    break;
                }
            }

            if (!hasOverlaps) {
                rooms.add(potentialRoom);
            }
        }
        return rooms;
    }

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
