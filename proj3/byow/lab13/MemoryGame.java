package byow.lab13;

import byow.Core.RandomUtils;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.Color;
import java.awt.Font;
import java.util.Random;

public class MemoryGame {
    /** The width of the window of this game. */
    private int width;

    /** The height of the window of this game. */
    private int height;

    /** The current round the user is on. */
    private int round;

    /** The Random object used to randomly generate Strings. */
    private Random rand;

    /** Whether or not the game is over. */
    private boolean gameOver;

    /** Whether or not it is the player's turn. Used in the last section of the
     * spec, 'Helpful UI'. */
    private boolean playerTurn;

    /** The characters we generate random Strings from. */
    private static final char[] CHARACTERS =
            "abcdefghijklmnopqrstuvwxyz".toCharArray();

    /** Encouraging phrases. Used in the last section of the spec, 'Helpful UI'. */
    private static final String[] ENCOURAGEMENT = {
            "You can do this!",
            "I believe in you!",
            "You got this!",
            "You're a star!",
            "Go Bears!",
            "Too easy for you!",
            "Wow, so impressive!"
    };

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please enter a seed");
            return;
        }

        long seed = Long.parseLong(args[0]);
        MemoryGame game = new MemoryGame(40, 40, seed);
        game.startGame();
    }

    public MemoryGame(int width, int height, long seed) {
        /* Sets up StdDraw so that it has a width by height grid of 16 by 16 squares as its canvas
         * Also sets up the scale so the top left is (0,0) and the bottom right is (width, height)
         */
        this.width = width;
        this.height = height;

        StdDraw.setCanvasSize(this.width * 16, this.height * 16);
        Font font = new Font("Monaco", Font.BOLD, 30);
        StdDraw.setFont(font);

        StdDraw.setXscale(0, this.width);
        StdDraw.setYscale(0, this.height);

        StdDraw.clear(Color.BLACK);
        StdDraw.enableDoubleBuffering();

        // Initialize random number generator
        rand = new Random(seed);
    }

    public String generateRandomString(int n) {
        // Generate random string of letters of length n
        StringBuilder result = new StringBuilder();

        int size = CHARACTERS.length;

        for (int i = 0; i < n; i += 1) {
            int index = RandomUtils.uniform(rand, size);
            result.append(CHARACTERS[index]);
        }

        return result.toString();
    }

    public void drawFrame(String s) {
        // Clear the screen
        StdDraw.clear(Color.BLACK);

        Font font = new Font("Monaco", Font.BOLD, 30);
        StdDraw.setFont(font);
        StdDraw.setPenColor(Color.WHITE);

        // Display main content in the center
        StdDraw.text(width / 2.0, height / 2.0, s);

        // Display game information when the game is not over
        if (!gameOver) {
            // Round number on the left
            StdDraw.text(5, height - 2, "Round: " + round);

            // Current task in the center
            if (playerTurn) {
                StdDraw.text(width / 2.0, height - 2, "Type!");
            } else {
                StdDraw.text(width / 2.0, height - 2, "Watch!");
            }

            // Encouraging phrase on the right
            int index = RandomUtils.uniform(rand, ENCOURAGEMENT.length);
            StdDraw.text(
                    width - 8,
                    height - 2,
                    ENCOURAGEMENT[index]
            );
        }

        StdDraw.show();
    }

    public void flashSequence(String letters) {
        // Display each character in letters,
        // making sure to blank the screen between letters
        for (int i = 0; i < letters.length(); i += 1) {
            drawFrame(Character.toString(letters.charAt(i)));

            // Display character for 1 second
            StdDraw.pause(1000);

            // Blank the screen
            StdDraw.clear(Color.BLACK);
            StdDraw.show();

            // Blank screen for 0.5 seconds
            StdDraw.pause(500);
        }
    }

    public String solicitNCharsInput(int n) {
        // Read n letters of player input
        StringBuilder result = new StringBuilder();

        while (result.length() < n) {
            if (StdDraw.hasNextKeyTyped()) {
                result.append(StdDraw.nextKeyTyped());

                // Display the input typed so far
                drawFrame(result.toString());
            }
        }

        return result.toString();
    }

    public void startGame() {
        // Set relevant variables before the game starts
        round = 1;
        gameOver = false;

        while (!gameOver) {
            // Player is watching the sequence
            playerTurn = false;

            drawFrame("Round: " + round);

            // Give the player time to see the round number
            StdDraw.pause(1000);

            // Generate target string
            String s = generateRandomString(round);

            // Display target string one character at a time
            flashSequence(s);

            // Player is now typing
            playerTurn = true;

            // Read player's input
            String inputFromUser = solicitNCharsInput(round);

            // Check player's answer
            if (s.equals(inputFromUser)) {
                round += 1;
            } else {
                drawFrame(
                        "Game Over! You made it to round: " + round
                );
                gameOver = true;
            }
        }
    }
}