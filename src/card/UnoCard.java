package card;

import display.UnoPanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public abstract class UnoCard implements Comparable<UnoCard> {
    private static final int RED = 0;
    private static final int YELLOW = 1;
    private static final int GREEN = 2;
    private static final int BLUE = 3;

    public static final int COLOR_MIN = RED;
    public static final int COLOR_MAX = BLUE;

    public static final Color DEFAULT_TEXT_COLOR = new Color(255, 128, 128);

    static final int NO_NUMBER = -1;
    static final int SKIP = 10;
    static final int REVERSE = 11;
    static final int DRAW_2 = 12;

    private static final int NUMBER_MAX = DRAW_2;

    static final int ORDER_CODE_COUNT = 10 + 4*13;

    public static final int MAX_POWER_DIFF = 20;

    public abstract int getColorCode();
    public abstract int getNumberCode();
    public abstract int getOrderCode();
    public abstract int getPowerCode();

    public abstract String encode();
    public abstract boolean canBecome(UnoCard card);

    public abstract boolean canPlay(int color, int number);
    public abstract boolean isNumeric();
    public abstract boolean isSkip();
    public abstract int cardDraws();

    public abstract Color getColor();
    public abstract String getText();
    public abstract String getShortText();

    public final Color getTextColor() {
        switch (getColorCode()) {
        case RED:
            return DEFAULT_TEXT_COLOR;
        case YELLOW:
            return new Color(255, 227, 128);
        case GREEN:
            return new Color(176, 255, 176);
        case BLUE:
            return new Color(128, 191, 255);
        default:
            throw new IllegalStateException("error: invalid color code");
        }
    }

    public final Color getCircleColor() {
        switch (getColorCode()) {
        case RED:
            return new Color(255, 0, 0, 51);
        case YELLOW:
            return new Color(255, 187, 0, 127);
        case GREEN:
            return new Color(0, 255, 0, 51);
        case BLUE:
            return new Color(0, 132, 255, 51);
        default:
            throw new IllegalStateException("error: invalid color code");
        }
    }

    public final boolean canPlayOn(UnoCard topOfDeck) {
        return canPlay(topOfDeck.getColorCode(), topOfDeck.getNumberCode());
    }

    public static ArrayList<UnoCard> newDeck() {
        ArrayList<UnoCard> deck = new ArrayList<>();
        for (int color = UnoCard.COLOR_MIN; color <= UnoCard.COLOR_MAX; color++) {
            deck.add(new NormalCard(color, 0));
            for (int number = 1; number <= UnoCard.NUMBER_MAX; number++) {
                deck.add(new NormalCard(color, number));
                deck.add(new NormalCard(color, number));
            }
        }
        for (int i = 0; i < 4; i++) {
            deck.add(new WildCard(false));
            deck.add(new WildCard(true));
        }
        Collections.shuffle(deck, UnoPanel.RANDOM);
        return deck;
    }

    public static Color getColor(int color) {
        switch (color) {
        case RED:
            return Color.RED;
        case YELLOW:
            return Color.ORANGE;
        case GREEN:
            return Color.GREEN.darker();
        case BLUE:
            return new Color(0, 127, 255);
        default:
            throw new IllegalStateException("error: invalid color code");
        }
    }

    static String getColorText(int color) {
        switch (color) {
        case RED:
            return "Red";
        case YELLOW:
            return "Yellow";
        case GREEN:
            return "Green";
        case BLUE:
            return "Blue";
        default:
            throw new IllegalStateException("error: invalid color code");
        }
    }

    static String getNumberText(int number) {
        switch (number) {
        case SKIP:
            return "Skip";
        case REVERSE:
            return "Reverse";
        case DRAW_2:
            return "Draw two";
        default:
            return Integer.toString(number);
        }
    }

    public static UnoCard decode(String encoded) {
        char[] encodedChars = encoded.toCharArray();
        if (encodedChars.length != 2) {
            throw new IllegalArgumentException("encoded cards must be 2 characters long (found: "+encoded+")");
        }
        char kind = encodedChars[0];
        int color = decodeColor(encodedChars[1]);
        switch (kind) {
        case 'D':
            return new WildCard(color, true);
        case 'W':
            return new WildCard(color, false);
        case 's':
            return new NormalCard(color, SKIP);
        case 'r':
            return new NormalCard(color, REVERSE);
        case 'd':
            return new NormalCard(color, DRAW_2);
        default:
            return new NormalCard(color, kind-'0');
        }
    }

    static char encodeColor(int color) {
        switch (color) {
        case RED:
            return 'r';
        case YELLOW:
            return 'y';
        case GREEN:
            return 'g';
        case BLUE:
            return 'b';
        default:
            return '?';
        }
    }

    private static int decodeColor(char ch) {
        switch (ch) {
        case 'r':
            return RED;
        case 'y':
            return YELLOW;
        case 'g':
            return GREEN;
        case 'b':
            return BLUE;
        default:
            return -1;
        }
    }

    @Override
    public int compareTo(UnoCard o) {
        return getOrderCode()-o.getOrderCode();
    }

    @Override
    public int hashCode() {
        return getOrderCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UnoCard && compareTo((UnoCard) o) == 0;
    }
}
