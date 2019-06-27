package card;

import java.awt.*;

public class NormalCard extends UnoCard {
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
    public int getOrderCode() {
        return 10+color*13+number;
    }

    @Override
    public int getPowerCode() {
        switch (number) {
        case 0:
            return 1;
        case SKIP:
        case REVERSE:
            return 5;
        case DRAW_2:
            return 10;
        default:
            return 0;
        }
    }

    @Override
    public String encode() {
        char n = UnoCard.encodeColor(color);
        switch (number) {
        case SKIP:
            return "s"+n;
        case REVERSE:
            return "r"+n;
        case DRAW_2:
            return "d"+n;
        default:
            return Integer.toString(number)+n;
        }
    }

    @Override
    public boolean canBecome(UnoCard card) {
        if (card instanceof NormalCard) {
            NormalCard normalCard = (NormalCard) card;
            return normalCard.color == color && normalCard.number == number;
        }
        return false;
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
    public Color getTextColor() {
        return UnoCard.getTextColor(color);
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
