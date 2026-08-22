package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.introcs.StdDraw;

public class Engine {
    TERenderer ter = new TERenderer();
    /* Feel free to change the width and height. */
    public static final int WIDTH = 80;
    public static final int HEIGHT = 30;

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        ter.initialize(WIDTH, HEIGHT + 3);
        drawMainMenu();
        InputSource source = new KeyboardSource();
        runGame(source, true);
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     *
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     *
     * In other words, both of these calls:
     *   - interactWithInputString("n123sss:q")
     *   - interactWithInputString("lww")
     *
     * should yield the exact same world state as:
     *   - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */
    public TETile[][] interactWithInputString(String input) {
        if (input.toUpperCase().startsWith("L")) {
            String savedHistory = PersistenceUtils.loadHistory();
            if (savedHistory == null) {
                return null;
            }
            input = savedHistory + input.substring(1);
        }

        InputSource source = new StringSource(input);
        GameState finalState = runGame(source, false);
        return finalState != null ? finalState.getWorld() : null;
    }

    private GameState runGame(InputSource source, boolean render) {
        GameState gameState = null;
        boolean preparingQuit = false;
        boolean inMenu = true;
        StringBuilder seedBuilder = new StringBuilder();
        while (source.possibleNextInput()) {
            if (source.hasNextKey()) {
                char key = Character.toUpperCase(source.getNextKey());
                if (inMenu) {
                    if (key == 'N') {
                        seedBuilder.setLength(0);
                        if (render) {
                            drawSeedMenu("");
                        }
                    } else if (Character.isDigit(key)) {
                        seedBuilder.append(key);
                        if (render) {
                            drawSeedMenu(seedBuilder.toString());
                        }
                    } else if (key == 'S' && seedBuilder.length() > 0) {
                        long seed = Long.parseLong(seedBuilder.toString());
                        MapGenerator generator = new MapGenerator(seed);
                        TETile[][] world = generator.generate();
                        Position startPos = findEmptyFloor(world);
                        world[startPos.getX()][startPos.getY()] = Tileset.AVATAR;
                        gameState = new GameState(world, startPos, new java.util.Random(seed));
                        inMenu = false;
                    } else if (key == 'L') {
                        String savedHistory = PersistenceUtils.loadHistory();
                        if (savedHistory == null) {
                            if (render) {
                                System.exit(0);
                            } else {
                                return null;
                            }
                        }
                        InputSource replaySource = new StringSource(savedHistory);
                        gameState = runGame(replaySource, false);
                        inMenu = false;
                    } else if (key == 'Q') {
                        if (render) {
                            System.exit(0);
                        } else {
                            return null;
                        }
                    }
                } else {
                    if (preparingQuit) {
                        if (key == 'Q') {
                            PersistenceUtils.saveHistory(gameState.getInputHistoryString());
                            if (render) {
                                System.exit(0);
                            } else {
                                return gameState;
                            }
                        }
                        preparingQuit = false;
                    } else if (key == ':') {
                        preparingQuit = true;
                    } else if (key == 'V') {
                        gameState.toggleLOS();
                    } else {
                        gameState.moveAvatar(key);
                    }
                }
            }
            if (render && !inMenu && gameState != null) {
                int mouseX = (int) StdDraw.mouseX();
                int mouseY = (int) StdDraw.mouseY();
                String hudMessage = "Void";
                if (mouseX >= 0 && mouseX < WIDTH && mouseY >= 0 && mouseY < HEIGHT) {
                    hudMessage = gameState.getWorld()[mouseX][mouseY].description();
                }
                TETile[][] frameToRender = gameState.getRenderFrame();
                ter.renderFrame(frameToRender);
                drawHUD(hudMessage, gameState);
                StdDraw.show();
                StdDraw.pause(10);
            }
        }
        return gameState;
    }

    private Position findEmptyFloor(TETile[][] world) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (world[x][y].equals(Tileset.FLOOR)) {
                    return new Position(x, y);
                }
            }
        }
        return new Position(WIDTH / 2, HEIGHT / 2);
    }

    private void drawMainMenu() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);

        java.awt.Font fontTitle = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 40);
        StdDraw.setFont(fontTitle);
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0 + 10, "CS61B: THE GAME");

        java.awt.Font fontOptions = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 20);
        StdDraw.setFont(fontOptions);
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0 + 2, "New Game (N)");
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0, "Load Game (L)");
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0 - 2, "Quit (Q)");

        StdDraw.show();
    }

    private void drawSeedMenu(String currentSeed) {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0 + 5, "Enter Seed (press 'S' to start):");
        StdDraw.text(WIDTH / 2.0, HEIGHT / 2.0, currentSeed);
        StdDraw.show();
    }

    private void drawHUD(String hoverMessage, GameState state) {
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.textLeft(2, HEIGHT + 1.5, "Tile: " + hoverMessage);
        StdDraw.textRight(WIDTH - 2, HEIGHT + 1.5, "HP: " + state.getHealth() + "% | Score: " + state.getScore());
        StdDraw.line(0, HEIGHT + 0.5, WIDTH, HEIGHT + 0.5);
    }

    public String getInputHistory() {
        return inputHistory.toString(); // Trả về chuỗi String trơn chứa các phím gõ (e.g., "N999SDDD")
    }
}
