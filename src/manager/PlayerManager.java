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
import java.util.ArrayList;

public class PlayerManager extends HandManager {
    public void selectCard(int c) {
        UnoDisplay.pushEvent(new PlayEvent(this, c));
    }

    private int columns;
    private int rows;

    private ArrayList<Integer> sortedIndices = new ArrayList<>();

    private void updateCR() {
        columns = Math.max((UnoDisplay.width-15)/UnoCard.SEP_X, 3);
        rows = (hand.size()-1)/columns;
    }

    @Override
    public void update(long time) {
        updateCR();
        int indent = (UnoDisplay.width - Math.min(columns, hand.size())*UnoCard.SEP_X + 5)/2;

        for (int cc = 0; cc < sortedIndices.size(); cc++) {
            int row = cc/columns;
            int column = cc%columns;
            float x = indent + UnoCard.SEP_X*column;
            float y = UnoDisplay.height - UnoCard.SEP_Y_PLAYER*(rows-row) - UnoCard.HEIGHT - 10;
            UnoCard card = hand.get(sortedIndices.get(cc));
            card.update(x, y, true, time);
        }
    }

    @Override
    public void paint(Graphics2D g) {
        for (int c : sortedIndices) {
            UnoCard card = hand.get(c);
            card.paint(g, UnoDisplay.hasEvent() || !isTurn || !UnoDisplay.canPlay(card));
        }
        if (isTurn) {
            updateCR();
            g.setColor(UnoDisplay.getTopOfDeck().getColor());
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            UnoDisplay.shadowTextCenter(g, "Your Turn", UnoDisplay.width/2, UnoDisplay.height - UnoCard.SEP_Y_PLAYER*rows - UnoCard.HEIGHT - 30);
        }
    }

    @Override
    public void click(int x, int y) {
        Point drawPile = UnoDisplay.getDrawPileLocation();
        if (drawPile != null) {
            for (int cc = sortedIndices.size() - 1; cc >= 0; cc--) {
                int c = sortedIndices.get(cc);
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

    private void addSortedCard(int c, UnoCard card) {
        int order = card.getOrderCode();
        for (int cc = 0; cc < sortedIndices.size(); cc++) {
            if (order < hand.get(sortedIndices.get(cc)).getOrderCode()) {
                sortedIndices.add(cc, c);
                return;
            }
        }
        sortedIndices.add(c);
    }

    private void removeSortedCard(int c) {
        int cc = 0;
        while (cc < sortedIndices.size()) {
            int i = sortedIndices.get(cc);
            if (i == c) {
                sortedIndices.remove(cc);
            } else {
                if (i > c) {
                    sortedIndices.set(cc, i-1);
                }
                cc++;
            }
        }
    }

    @Override
    public int addCard(UnoCard card) {
        int c = super.addCard(card);
        addSortedCard(c, card);
        if (isTurn) {
            if (UnoDisplay.canPlay(card)) {
                UnoDisplay.setMenu(new PlayKeepMenu(this, card, c));
            } else {
                UnoDisplay.pushEvent(new EndTurnEvent());
            }
        }
        return c;
    }

    @Override
    public UnoCard takeCard(int c) {
        UnoCard card = super.takeCard(c);
        removeSortedCard(c);
        return card;
    }
}
