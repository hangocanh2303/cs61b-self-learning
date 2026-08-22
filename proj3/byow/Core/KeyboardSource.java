package byow.Core;

import edu.princeton.cs.introcs.StdDraw;

public class KeyboardSource implements InputSource {
    private static final int PAUSE = 10;
    @Override
    public char getNextKey() {
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                return Character.toUpperCase(StdDraw.nextKeyTyped());
            }
            StdDraw.pause(PAUSE);
        }
    }

    @Override
    public boolean hasNextKey() {
        return StdDraw.hasNextKeyTyped();
    }

    @Override
    public boolean possibleNextInput() {
        return true;
    }
}
