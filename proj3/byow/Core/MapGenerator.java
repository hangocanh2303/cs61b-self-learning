package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import byow.lab12.HexWorld;

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

    public TETile[][] getTiles() {
        return tiles;
    }

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        MapGenerator map = new MapGenerator();


        ter.renderFrame(map.getTiles());
    }
}
