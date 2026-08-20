package byow.Core;

import byow.TileEngine.TETile;

public class Room implements WorldComponent{
    private Position bottomLeft;
    private int width;
    private int height;

    public Position getCenter() {
        return null;
    }

    public boolean overlaps(Room other) {
        return false;
    }

    @Override
    public void draw(TETile[][] world) {

    }
}
