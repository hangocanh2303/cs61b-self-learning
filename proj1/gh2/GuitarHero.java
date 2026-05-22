package gh2;

import edu.princeton.cs.algs4.StdAudio;
import edu.princeton.cs.algs4.StdDraw;

/**
 * A client that uses the synthesizer package to replicate a plucked guitar string sound
 */
public class GuitarHero {

    private static final int KEYBOARD_SIZE = 37;

    private static final String ALL_CAPS = "q2we4r5ty7u8i9op-[=zxdcfvgbnjmk,.;/' ";

    public static void main(String[] args) {

        GuitarString[] guitarStrings = new GuitarString[KEYBOARD_SIZE];

        for (int i = 0; i < KEYBOARD_SIZE; i += 1) {
            double concertFrequency = 440 * Math.pow(2, (double) (i - 24) / 12);
            GuitarString guitarString = new GuitarString(concertFrequency);
            guitarStrings[i] = guitarString;
        }

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                int indexKey = ALL_CAPS.indexOf(key);
                if (indexKey < 0) {
                    continue;
                }
                guitarStrings[indexKey].pluck();
            }

            double sample = 0.0;
            for (int i = 0; i < KEYBOARD_SIZE; i += 1) {
                sample += guitarStrings[i].sample();
            }

            StdAudio.play(sample);

            for (int i = 0; i < KEYBOARD_SIZE; i += 1) {
                guitarStrings[i].tic();
            }
        }
    }
}

