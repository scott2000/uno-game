package manager.local;

import card.CardGraphics;
import card.CardObject;
import card.UnoCard;
import card.WildCard;
import display.UnoPanel;
import manager.HandManager;
import menu.ColorSelectMenu;

import java.awt.*;
import java.util.ArrayList;

public class PlayerManager extends HandManager {
    private int columns;
    private int rows;
    private ArrayList<Integer> sortedIndices = new ArrayList<>();

    private void updateCR() {
        columns = Math.max((UnoPanel.width - 2*MARGIN)/SEP_X, 3);
        rows = (hand.size()-1)/columns;
    }

    public boolean click(int x, int y, Point drawPileLocation) {
        for (int cc = sortedIndices.size() - 1; cc >= 0; cc--) {
            int c = sortedIndices.get(cc);
            CardObject cardObject = hand.get(c);
            if (cardObject.inBounds(x, y)) {
                UnoCard card = cardObject.getCard();
                if (card.canPlayOn(UnoPanel.getTopOfDeck())) {
                    if (card instanceof WildCard) {
                        UnoPanel.setMenu(new ColorSelectMenu((WildCard) card, c));
                    } else {
                        UnoPanel.playCard(c);
                    }
                }
                return true;
            }
        }
        if (UnoPanel.canDraw() && CardObject.pointInBounds(drawPileLocation, x, y)) {
            UnoPanel.drawCard();
            return true;
        }
        return false;
    }

    public boolean shouldPlayDrawnCard() {
        CardObject cardObject = hand.get(hand.size()-1);
        UnoCard card = cardObject.getCard();
        if (card.canPlayOn(UnoPanel.getTopOfDeck())) {
            cardObject.setHighlighted(true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void update(long time) {
        updateCR();
        int indent = (UnoPanel.width - Math.min(columns, hand.size())*SEP_X + 5)/2;

        for (int cc = 0; cc < sortedIndices.size(); cc++) {
            int row = cc/columns;
            int column = cc%columns;
            float x = indent + SEP_X*column;
            float y = UnoPanel.height - SEP_Y *(rows-row) - CardGraphics.HEIGHT - MARGIN;
            hand.get(sortedIndices.get(cc)).update(x, y, true, time);
        }

//        if (isTurn) {
//            int y = UnoPanel.height - SEP_Y*rows - CardGraphics.HEIGHT - 50;
//            Uno.PANEL.repaint(0,
//                    UnoPanel.width/4,
//                    y,
//                    UnoPanel.width/2,
//                    100);
//        }
    }

    @Override
    public int paint(Graphics2D g) {
        int seconds = UnoPanel.getTimeToNewGame();
        if (seconds == 0) {
            showText(g, "Waiting for Opponent");
        } else if (seconds != -1) {
            showText(g, "New Game in " + seconds);
        } else if (isTurn) {
            showText(g, "Your Turn");
        }

        for (int c : sortedIndices) {
            CardObject cardObject = hand.get(c);
            cardObject.paint(g, UnoPanel.hasEventInQueue() || !isTurn || !cardObject.getCard().canPlayOn(UnoPanel.getTopOfDeck()));
        }

        return UnoPanel.height - SEP_Y*rows - CardGraphics.HEIGHT - MARGIN;
    }

    private void showText(Graphics2D g, String text) {
        updateCR();
        g.setColor(UnoPanel.getTopOfDeck().getColor());
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        UnoPanel.shadowTextCenter(g, text, UnoPanel.width/2, UnoPanel.height - SEP_Y*rows - CardGraphics.HEIGHT - MARGIN - 20);
    }

    @Override
    public void reset() {
        super.reset();
        sortedIndices.clear();
    }

    @Override
    protected void onAddCard(CardObject cardObject, int c) {
        UnoCard card = cardObject.getCard();
        int order = card.getOrderCode();
        for (int cc = 0; cc < sortedIndices.size(); cc++) {
            if (order < hand.get(sortedIndices.get(cc)).getCard().getOrderCode()) {
                sortedIndices.add(cc, c);
                return;
            }
        }
        sortedIndices.add(c);
    }

    @Override
    protected void onRemoveCard(CardObject cardObject, int c) {
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
}
