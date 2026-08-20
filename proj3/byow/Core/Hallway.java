package byow.Core;

import byow.TileEngine.TETile;

import java.util.List;

public class Hallway implements WorldComponent{

    private final List<Room> segments;

    public Hallway(List<Room> segments) {
        this.segments = segments;
    }

    public void addSegment(Room narrowRoom) {

    }
    @Override
    public void draw(TETile[][] world) {
        for (Room segment: segments) {
            segment.draw(world);
        }
    }
}
