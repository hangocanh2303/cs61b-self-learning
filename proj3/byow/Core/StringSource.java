package byow.Core;

public class StringSource implements InputSource{

    private final String input;
    private int index;

    public StringSource(String input) {
        this.input = input.toUpperCase();
        this.index = 0;
    }

    @Override
    public char getNextKey() {
        char c = input.charAt(index);
        index += 1;
        return c;
    }

    @Override
    public boolean hasNextKey() {
        return true;
    }

    @Override
    public boolean possibleNextInput() {
        return index < input.length();
    }
}
