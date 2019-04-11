import java.awt.*;

interface UnoCard {
    int RED = 0;
    int YELLOW = 1;
    int GREEN = 2;
    int BLUE = 3;

    int NO_NUMBER = -1;
    int SKIP = 10;
    int REVERSE = 11;
    int DRAW_2 = 12;

    int getColorCode();
    int getNumberCode();

    boolean canPlay(int color, int number);
    boolean isNumeric();
    boolean isSkip();
    int cardDraws();

    Color getColor();
    String getText();
    String getShortText();

    static Color getColor(int color) {
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
}
