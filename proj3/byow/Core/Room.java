package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;


public class Room implements WorldComponent{
    private final Position bottomLeft;
    private final int width;
    private final int height;

    public Room(Position bottomLeft, int width, int height) {
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
    }

    public Position getCenter() {
        return new Position(bottomLeft.getX() + width / 2,
                bottomLeft.getY() + height / 2);
    }

    public boolean overlaps(Room other) {
        int x1 = bottomLeft.getX();
        int y1 = bottomLeft.getY();
        Position otherRoomBottomLeft = other.getBottomLeft();
        int x2 = otherRoomBottomLeft.getX();
        int y2 = otherRoomBottomLeft.getY();
        int w2 = other.getWidth();
        int h2 = other.getHeight();
        return (x1 < x2 + w2) &&
                (x1 + width > x2) &&
                (y1 < y2 + h2) &&
                (y1 + height > y2);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Position getBottomLeft() {
        return bottomLeft;
    }

    @Override
    public void draw(TETile[][] world) {
        int x = bottomLeft.getX();
        int y = bottomLeft.getY();
        int topY = y + height;
        int topX = x + width;
        for (int row = y; row < topY; row += 1) {
            for (int col = x; col < topX; col += 1) {
                if (row == y || row == topY - 1
                || col == x || col == topX - 1) {
                    if (world[col][row].equals(Tileset.NOTHING)) {
                        world[col][row] = Tileset.WALL;
                    }
                } else {
                    world[col][row] = Tileset.FLOOR;
                }
            }
        }
    }
}
