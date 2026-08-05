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

    public static final int WIDTH = 60;
    public static final int HEIGHT = 30;

    private final TETile[][] tiles;

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

    public static void main(String[] args) {
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        HexWorld hexWorld = new HexWorld();
        hexWorld.addHexagon(0, 0, 2, Tileset.GRASS);

        ter.renderFrame(hexWorld.getTiles());
    }
}
