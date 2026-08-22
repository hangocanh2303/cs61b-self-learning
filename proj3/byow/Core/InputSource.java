package byow.Core;

public interface InputSource {
    char getNextKey();
    boolean hasNextKey();
    boolean possibleNextInput();
}
