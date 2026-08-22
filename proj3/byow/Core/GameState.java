package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.io.Serializable;
import java.util.Random;

public class GameState implements Serializable {

    private final TETile[][] world;
    private Position avatarPos;
    private final Random random;
    private final StringBuilder inputHistory;

    private int score;
    private int health;
    private boolean losEnabled;

    public GameState(TETile[][] world, Position avatarPos, Random random) {
        this.world = world;
        this.avatarPos = avatarPos;
        this.random = random;
        this.inputHistory = new StringBuilder();
        this.score = 0;
        this.health = 100;
        this.losEnabled = false;
    }

    public TETile[][] getWorld() {
        return world;
    }

    public Position getAvatarPos() {
        return avatarPos;
    }

    public Random getRandom() {
        return random;
    }

    public StringBuilder getInputHistory() {
        return inputHistory;
    }

    public String getInputHistoryString() {
        return inputHistory.toString();
    }

    public int getScore() {
        return score;
    }

    public int getHealth() {
        return health;
    }

    public boolean isLosEnabled() {
        return losEnabled;
    }

    public void moveAvatar(char key) {
        int dx = 0;
        int dy = 0;
        char direction = Character.toUpperCase(key);

        switch (direction) {
            case 'W': dy = 1;
                break;   // UP
            case 'S': dy = -1;
                break;  // DOWN
            case 'A': dx = -1;
                break;  // LEFT
            case 'D': dx = 1;
                break;   // RIGHT
            default:
        }

        int targetX = avatarPos.getX() + dx;
        int targetY = avatarPos.getY() + dy;

        if (targetX >= 0 && targetX < Engine.WIDTH
            && targetY >= 0 && targetY <= Engine.HEIGHT) {
            TETile targetTile = world[targetX][targetY];
            if (targetTile.equals(Tileset.FLOOR) || targetTile.equals(Tileset.FLOWER)
                || targetTile.equals(Tileset.GOLD)) {
                inputHistory.append(direction);
                if (targetTile.equals(Tileset.FLOWER)) {
                    health = Math.min(100, health + 20);
                } else if (targetTile.equals(Tileset.GOLD)) {
                    score += 100;
                }

                world[avatarPos.getX()][avatarPos.getY()] = Tileset.FLOOR;
                avatarPos = new Position(targetX, targetY);
                world[targetX][targetY] = Tileset.AVATAR;
            }
        }
    }

    public TETile[][] getRenderFrame() {
        if (!losEnabled) {
            return world;
        }

        TETile[][] frame = new TETile[Engine.WIDTH][Engine.HEIGHT];
        int playerX = avatarPos.getX();
        int playerY = avatarPos.getY();

        int radius = 5;

        for (int x = 0; x < Engine.WIDTH; x += 1) {
            for (int y = 0; y < Engine.HEIGHT; y += 1) {
                if (Math.abs(x - playerX) < radius && Math.abs(y - playerY) < radius) {
                    frame[x][y] = world[x][y];
                } else {
                    frame[x][y] = Tileset.NOTHING;
                }
            }
        }
        return frame;
    }

    public void toggleLOS() {
        losEnabled = !losEnabled;
    }
}
