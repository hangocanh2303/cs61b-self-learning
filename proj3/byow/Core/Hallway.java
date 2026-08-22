package byow.Core;

import byow.TileEngine.TETile;

import java.util.ArrayList;
import java.util.List;

public class Hallway implements WorldComponent {

    private final List<Room> segments;

    public Hallway() {
        this.segments = new ArrayList<>();
    }

    public void addSegment(Room narrowRoom) {
        segments.add(narrowRoom);
    }
    @Override
    public void draw(TETile[][] world) {
        for (Room segment: segments) {
            segment.draw(world);
        }
    }
}
