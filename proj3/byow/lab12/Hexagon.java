package byow.lab12;

import byow.TileEngine.TETile;

public class Hexagon {

    private int bottomLeftX;
    private int bottomLeftY;

    private int edgeSize;
    private TETile tile;

    private static final int UP = 1;
    private static final int DOWN = -1;


    public Hexagon(int bottomLeftX, int bottomLeftY, int edgeSize, TETile tile) {
        this.bottomLeftX = bottomLeftX;
        this.bottomLeftY = bottomLeftY;
        this.edgeSize = edgeSize;
        this.tile = tile;
    }

    /**
     *
     *      aa        aaa         aaaa             aaaaa
     *     aaaa      aaaaa       aaaaaa           aaaaaaa
     *     aaaa     aaaaaaa     aaaaaaaa         aaaaaaaaa
     *      aa      aaaaaaa    aaaaaaaaaa       aaaaaaaaaaa
     *               aaaaa     aaaaaaaaaa      aaaaaaaaaaaaa
     *                aaa       aaaaaaaa       aaaaaaaaaaaaa
     *                           aaaaaa         aaaaaaaaaaa
     *                            aaaa           aaaaaaaaa
     *                                            aaaaaaa
     *                                             aaaaa
     */
    public void place(HexWorld world) {
        int maxLen = getMaxRowLen();
        // place upper half
        placeHalf(bottomLeftX, bottomLeftY + edgeSize, maxLen, world, UP);
        // place lower half
        placeHalf(bottomLeftX, bottomLeftY + edgeSize - 1, maxLen, world, DOWN);
    }

    private void placeHalf(int firstX, int firstY, int maxLen, HexWorld hexWorld, int yStep) {
        for (int i = 0; i < edgeSize; i += 1) {
            placeRow(firstX, firstY, maxLen, hexWorld);
            firstX += 1;
            maxLen -= 2;
            firstY += yStep;
        }
    }

    private int getMaxRowLen() {
        return edgeSize + 2 * (edgeSize - 1);
//        int len = edgeSize;
//        for (int i = 1; i < edgeSize; i += 1) {
//            len += 2;
//        }
//        return len;
    }

    private void placeRow(int startX, int startY, int len, HexWorld world) {
        for (int i = 0; i < len; i += 1) {
            world.setTile(startX, startY, tile);
            startX += 1;
        }
    }

    public int getEdgeSize() {
        return edgeSize;
    }

    public void setEdgeSize(int edgeSize) {
        this.edgeSize = edgeSize;
    }

    public TETile getTile() {
        return tile;
    }

    public void setTile(TETile tile) {
        this.tile = tile;
    }
}
