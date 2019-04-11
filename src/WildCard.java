import java.awt.*;

class WildCard implements UnoCard {
    private boolean isDrawFour;
    private int color = -1;

    WildCard(boolean isDrawFour) {
        this.isDrawFour = isDrawFour;
    }

    void setColor(int color) {
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
        return NO_NUMBER;
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
