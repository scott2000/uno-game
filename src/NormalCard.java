import java.awt.*;

class NormalCard implements UnoCard {
    private int color;
    private int number;

    NormalCard(int color, int number) {
        this.color = color;
        this.number = number;
    }

    @Override
    public int getColorCode() {
        return color;
    }

    @Override
    public int getNumberCode() {
        return number;
    }

    @Override
    public boolean canPlay(int color, int number) {
        return this.color == color || this.number == number;
    }

    @Override
    public boolean isNumeric() {
        return number >= 0 && number <= 9;
    }

    @Override
    public boolean isSkip() {
        return number > 9;
    }

    @Override
    public int cardDraws() {
        if (number == DRAW_2) {
            return 2;
        } else {
            return 0;
        }
    }

    @Override
    public Color getColor() {
        return UnoCard.getColor(color);
    }

    @Override
    public String getText() {
        return UnoCard.getNumberText(number);
    }

    @Override
    public String getShortText() {
        switch (number) {
        case SKIP:
            return "S";
        case REVERSE:
            return "R";
        case DRAW_2:
            return "+2";
        default:
            return Integer.toString(number);
        }
    }

    @Override
    public String toString() {
        return "("+UnoCard.getColorText(color)+" "+getText()+")";
    }
}
