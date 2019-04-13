package manager;

import card.UnoCard;
import display.UnoDisplay;

import java.awt.*;

public abstract class OpponentManager extends HandManager {
    public abstract boolean claimsStart();

    @Override
    public void update(long time) {
        int width = UnoDisplay.width;

        int columns = Math.max((width-15)/UnoCard.SEP_X, 3);
        int indent = (width - Math.min(columns, hand.size())*UnoCard.SEP_X + 5)/2;

        for (int c = 0; c < hand.size(); c++) {
            int row = c/columns;
            int column = c%columns;
            float x = indent + UnoCard.SEP_X*column;
            float y = 10 + UnoCard.SEP_Y_COMPUTER*row;
            UnoCard card = hand.get(c);
            card.update(x, y, false, time);
        }
    }

    @Override
    public void paint(Graphics2D g) {
        for (UnoCard card : hand) {
            card.paint(g, false);
        }
    }

    @Override
    public void click(int x, int y) {};
}
