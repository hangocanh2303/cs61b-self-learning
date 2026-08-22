package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import java.awt.Font;
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
        // TODO: Fill out this method so that it run the engine using the input
        // passed in as an argument, and return a 2D tile representation of the
        // world that would have been drawn if the same inputs had been given
        // to interactWithKeyboard().
        //
        // See proj3.byow.InputDemo for a demo of how you can make a nice clean interface
        // that works for many different input types.

//        if (input == null || input.isEmpty()) {
//            return null;
//        }
//
//        input = input.toLowerCase();
//
//        if (input.charAt(0) != 'n') {
//            return null;
//        }
//
//        int index = 1;
//
//        StringBuilder seedString = new StringBuilder();
//
//        while (index < input.length()
//                && Character.isDigit(input.charAt(index))) {
//            seedString.append(input.charAt(index));
//            index++;
//        }
//
//        if (seedString.length() == 0) {
//            return null;
//        }
//
//        if (index >= input.length() || input.charAt(index) != 's') {
//            return null;
//        }
//
//        long seed = Long.parseLong(seedString.toString());
//
//        MapGenerator mapGenerator = new MapGenerator(seed);
//        return mapGenerator.generate();

        InputSource source = new StringSource(input);
        GameState finalState = runGame(source, false);
        return finalState != null ? finalState.getWorld() : null;
    }

    private GameState runGame(InputSource source, boolean render) {
        GameState gameState = null;
        boolean preparingQuit = false;
        boolean inMenu = true;
        StringBuilder seedBuilder = new StringBuilder();

        // Vòng lặp chính chạy dựa trên possibleNextInput() - Không bao giờ sập luồng khi đứng im!
        while (source.possibleNextInput()) {

            // Chỉ đọc và xử lý phím bấm khi thực sự có phím đang nằm đợi trong buffer (Không chặn!)
            if (source.hasNextKey()) {
                char key = Character.toUpperCase(source.getNextKey());

                // --- TRẠNG THÁI 1: KHỞI TẠO Ở MENU CHÍNH ---
                if (inMenu) {
                    if (key == 'N') {
                        seedBuilder.setLength(0); // Làm trống bộ đệm hạt giống
                        if (render) {
                            drawSeedMenu(""); // Vẽ màn hình yêu cầu nhập SEED lên GUI
                        }
                    } else if (Character.isDigit(key)) {
                        seedBuilder.append(key);
                        if (render) {
                            drawSeedMenu(seedBuilder.toString()); // Hiển thị số đang nhập theo thời gian thực
                        }
                    } else if (key == 'S' && seedBuilder.length() > 0) {
                        // Chốt hạt giống thành công -> Kích hoạt sinh địa hình v12
                        long seed = Long.parseLong(seedBuilder.toString());
                        MapGenerator generator = new MapGenerator(seed);
                        TETile[][] world = generator.generate();

                        // Tìm kiếm ô sàn trống đầu tiên để định vị Avatar một cách an toàn
                        Position startPos = findEmptyFloor(world);
                        world[startPos.getX()][startPos.getY()] = Tileset.AVATAR;

                        // Đóng gói trạng thái thế giới
                        gameState = new GameState(world, startPos, new java.util.Random(seed));
                        inMenu = false; // Thoát khỏi Menu, chuyển sang trạng thái di chuyển!
                    } else if (key == 'L') {
                        // Tải lại game cũ từ file savefile.txt
                        gameState = PersistenceUtils.loadGame();
                        if (gameState == null) {
                            if (render) {
                                System.exit(0); // Nếu chơi GUI mà không có file, đóng chương trình an toàn [310]
                            } else {
                                return null;    // Chế độ chấm điểm trả về null phòng ngự [310]
                            }
                        }
                        inMenu = false; // Vào game trực tiếp bằng trạng thái đã khôi phục!
                    } else if (key == 'Q') {
                        if (render) {
                            System.exit(0);
                        } else {
                            return null;
                        }
                    }

                    // --- TRẠNG THÁI 2: ĐANG TRONG TRẬN ĐẤU (GAMEPLAY STATE) ---
                } else {
                    if (preparingQuit) {
                        if (key == 'Q') {
                            PersistenceUtils.saveGame(gameState); // Ghi tuần tự đối tượng GameState [309, 310]
                            if (render) {
                                System.exit(0);
                            } else {
                                return gameState; // Chế độ String trả về trạng thái để autograder so khớp
                            }
                        }
                        preparingQuit = false; // Nhấn phím bất kỳ khác -> Hủy trạng thái chờ thoát [309]
                    } else if (key == ':') {
                        preparingQuit = true; // Bật cờ chờ nhấn phím Q kế tiếp để thoát [309]
                    } else if (key == 'V') {
                        gameState.toggleLOS(); // Bật/tắt Line of Sight (270 điểm Ambition) [315]
                    } else {
                        gameState.moveAvatar(key); // Di chuyển nhân vật phòng ngự (W, A, S, D) [307]
                    }
                }
            }

            // --- TRẠNG THÁI 3: KẾT XUẤT ĐỒ HỌA HOVER CHUỘT (CHỈ CHẠY TRONG KEYBOARD MODE) ---
            // Đặt ngoài khối hasNextKey() để di chuột luôn mượt mà khi đứng im di chuyển!
            if (render && !inMenu && gameState != null) {
                // Đọc tọa độ pixel của chuột và dịch sang tọa độ ô gạch vật lý [326]
                int mouseX = (int) StdDraw.mouseX();
                int mouseY = (int) StdDraw.mouseY();
                String hudMessage = "Void";

                if (mouseX >= 0 && mouseX < WIDTH && mouseY >= 0 && mouseY < HEIGHT) {
                    hudMessage = gameState.getWorld()[mouseX][mouseY].description();
                }

                // Render mảng gạch đã được lọc qua bóng tối sương mù Line of Sight [315]
                TETile[][] frameToRender = gameState.getRenderFrame();
                ter.renderFrame(frameToRender);

                // Vẽ đè thanh HUD chứa máu, điểm và mô tả hover gạch
                drawHUD(hudMessage, gameState);

                StdDraw.show();
                StdDraw.pause(10); // Khống chế luồng tránh CPU chạy quá công suất
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
        StdDraw.line(0, HEIGHT + 0.5, WIDTH, HEIGHT + 0.5); // Vẽ ranh giới phân tách HUD và bản đồ
    }
}
