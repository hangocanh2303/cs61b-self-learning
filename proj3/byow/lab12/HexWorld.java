package byow.lab12;
import org.junit.Test;
import static org.junit.Assert.*;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.util.Random;

/**
 * Draws a world consisting of hexagonal regions.
 */
public class HexWorld {

    public static final int WIDTH = 80;
    public static final int HEIGHT = 60;

    private final TETile[][] tiles;

    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random(SEED);


    public HexWorld() {
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

    public void addHexagon(int x, int y, int s, TETile tile) {
        Hexagon hexagon = new Hexagon(x, y, s, tile);
        hexagon.place(this);
    }

    public void setTile(int x, int y, TETile tile) {
        tiles[x][y] = tile;
    }

    public TETile[][] getTiles() {
        return tiles;
    }


    private static TETile randomTile() {
        int tileNum = RANDOM.nextInt(5);
        switch (tileNum) {
            case 0: return Tileset.TREE;
            case 1: return Tileset.FLOWER;
            case 2: return Tileset.MOUNTAIN;
            case 3: return Tileset.GRASS;
            case 4: return Tileset.SAND;
            default: return Tileset.NOTHING;
        }
    }

    private void addHexagonCols(int postX, int postY, int size, int numCols) {
       for (int i = 0; i < numCols; i += 1) {
           addHexagon(postX, postY, size, randomTile());
           postY += 2 * size;
       }


    }

    private void addExampleHexWorld(int hexSize) {
        int maxLen = hexSize + 2 * (hexSize - 1);
        int middle = (WIDTH - maxLen) / 2;

        int midColSize = 5;

        int colRange = 2 * hexSize - 1;
        // add middle col
        addHexagonCols(middle, 0, hexSize, midColSize);

        for (int i = 1; i < 3; i += 1) {
            // add left col
            addHexagonCols(middle - (i * colRange), hexSize * i, hexSize, midColSize - i);
            // add right col
            addHexagonCols(middle + (i * colRange), hexSize * i, hexSize, midColSize - i);
        }

    }

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        HexWorld hexWorld = new HexWorld();
        hexWorld.addExampleHexWorld(4);
//        int startX = 0;
//        int startY = 0;
//        for (int i = 0; i < 19; i += 1) {
//            hexWorld.addHexagon(startX, startY, 3, randomTile());
//            startX += 1;
//            startY += 1;
//        }
        ter.renderFrame(hexWorld.getTiles());
    }
}
