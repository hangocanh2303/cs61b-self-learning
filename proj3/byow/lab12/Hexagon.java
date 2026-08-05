package byow.lab12;

import byow.TileEngine.TETile;

public class Hexagon {

    private int originX;
    private int originY;

    private int edgeSize;
    private TETile tile;


    public Hexagon(int originX, int originY, int edgeSize, TETile tile) {
        this.originX = originX;
        this.originY = originY;
        this.edgeSize = edgeSize;
        this.tile = tile;
    }

    public void place(HexWorld world) {
        world.setTile(originX, originY, tile);
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

    public int getOriginX() {
        return originX;
    }

    public void setOriginX(int originX) {
        this.originX = originX;
    }

    public int getOriginY() {
        return originY;
    }

    public void setOriginY(int originY) {
        this.originY = originY;
    }
}
