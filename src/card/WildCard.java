package card;

import java.awt.*;

public final class WildCard extends UnoCard {
    private boolean isDrawFour;
    private int color;

    WildCard(boolean isDrawFour) {
        this(-1, isDrawFour);
    }

    WildCard(int color, boolean isDrawFour) {
        this.isDrawFour = isDrawFour;
        this.color = color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public int getColorCode() {
        if (color == -1) {
            throw new IllegalStateException("error: wild card color not yet set!");
        }
        return color;
    }

    @Override
    public int getNumberCode() {
        return UnoCard.NO_NUMBER;
    }

    @Override
    public int getOrderCode() {
        if (isDrawFour) {
            return 6+color;
        } else {
            return 1+color;
        }
    }

    @Override
    public int getPowerCode() {
        if (isDrawFour) {
            return 16;
        } else {
            return 9;
        }
    }

    @Override
    public String encode() {
        char n = UnoCard.encodeColor(color);
        if (isDrawFour) {
            return "D"+n;
        } else {
            return "W"+n;
        }
    }

    @Override
    public boolean canBecome(UnoCard card) {
        if (card instanceof WildCard) {
            WildCard wildCard = (WildCard) card;
            return wildCard.isDrawFour == isDrawFour && (color == -1 || wildCard.color == color);
        }
        return false;
    }

    @Override
    public boolean canPlay(int color, int number) {
        return true;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isSkip() {
        return isDrawFour;
    }

    @Override
    public int cardDraws() {
        if (isDrawFour) {
            return 4;
        } else {
            return 0;
        }
    }

    @Override
    public Color getColor() {
        if (color == -1) {
            return Color.BLACK;
        } else {
            return UnoCard.getColor(color);
        }
    }

    @Override
    public String getText() {
        if (isDrawFour) {
            return "Draw four";
        } else {
            return "Wild";
        }
    }

    @Override
    public String getShortText() {
        if (isDrawFour) {
            return "+4";
        } else {
            return "Wild";
        }
    }

    @Override
    public String toString() {
        return "("+getText()+")";
    }
}
