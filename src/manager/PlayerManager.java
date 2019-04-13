package manager;

import card.UnoCard;
import card.WildCard;
import display.UnoDisplay;
import event.DrawEvent;
import event.EndTurnEvent;
import event.PlayEvent;
import menu.ColorSelectMenu;
import menu.PlayKeepMenu;

import java.awt.*;

public class PlayerManager extends HandManager {
    public void selectCard(int c) {
        UnoDisplay.pushEvent(new PlayEvent(this, c));
    }

    private int columns;
    private int rows;

    private void updateCR() {
        columns = Math.max((UnoDisplay.width-15)/UnoCard.SEP_X, 3);
        rows = (hand.size()-1)/columns;
    }

    @Override
    public void update(long time) {
        updateCR();
        int indent = (UnoDisplay.width - Math.min(columns, hand.size())*UnoCard.SEP_X + 5)/2;

        for (int c = 0; c < hand.size(); c++) {
            int row = c/columns;
            int column = c%columns;
            float x = indent + UnoCard.SEP_X*column;
            float y = UnoDisplay.height - UnoCard.SEP_Y_PLAYER*(rows-row) - UnoCard.HEIGHT - 10;
            UnoCard card = hand.get(c);
            card.update(x, y, true, time);
        }
    }

    @Override
    public void paint(Graphics2D g) {
        for (UnoCard card : hand) {
            card.paint(g, UnoDisplay.hasEvent() || !isTurn || !UnoDisplay.canPlay(card));
        }
        if (isTurn) {
            updateCR();
            g.setColor(UnoDisplay.getTopOfDeck().getColor());
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            UnoCard.shadowTextCenter(g, "Your Turn", UnoDisplay.width/2, UnoDisplay.height - UnoCard.SEP_Y_PLAYER*rows - UnoCard.HEIGHT - 30);
        }
    }

    @Override
    public void click(int x, int y) {
        Point drawPile = UnoDisplay.getDrawPileLocation();
        if (drawPile != null) {
            for (int c = hand.size() - 1; c >= 0; c--) {
                UnoCard card = hand.get(c);
                if (card.inBounds(x, y)) {
                    if (UnoDisplay.canPlay(card)) {
                        if (card instanceof WildCard) {
                            UnoDisplay.setMenu(new ColorSelectMenu(this, (WildCard) card, c));
                        } else {
                            selectCard(c);
                        }
                    }
                    return;
                }
            }
            if (UnoCard.inBounds(drawPile, x, y)) {
                UnoDisplay.pushEvent(new DrawEvent(this, UnoDisplay.drawCard()));
            }
        }
    }

    @Override
    public void addCard(UnoCard card) {
        int c = hand.size();
        super.addCard(card);
        if (isTurn) {
            if (UnoDisplay.canPlay(card)) {
                UnoDisplay.setMenu(new PlayKeepMenu(this, card, c));
            } else {
                UnoDisplay.pushEvent(new EndTurnEvent());
            }
        }
    }
}
