package manager;

import card.CardGraphics;
import card.CardObject;
import card.UnoCard;
import display.UnoPanel;

import java.awt.*;
import java.util.List;

public abstract class OpponentManager extends HandManager {
    private int columns;
    private int sepY;

    protected DeckManager deckManager = null;

    private void updateCS() {
        columns = Math.max((UnoPanel.width - 2*MARGIN)/SEP_X, 3);
        sepY = UnoPanel.isGameOver() && !hand.isEmpty() && hand.get(0).getCard() != null ? SEP_Y : SEP_Y_HIDDEN;
    }

    public abstract boolean playerCanStart();

    public abstract UnoCard drawVisibleCard();

    @Override
    public void update(long time) {
        updateCS();

        int indent = (UnoPanel.width - Math.min(columns, hand.size())*SEP_X + 5)/2;
        boolean gameOver = UnoPanel.isGameOver();

        for (int c = 0; c < hand.size(); c++) {
            int row = c/columns;
            int column = c%columns;
            float x = indent + SEP_X*column;
            float y = MARGIN + sepY*row;
            hand.get(c).update(x, y, gameOver || UnoPanel.isPeeking(), time);
        }
    }

    @Override
    public int paint(Graphics2D g) {
        for (CardObject card : hand) {
            card.paint(g, true);
        }

        updateCS();
        int rows = (hand.size()-1)/columns;
        return MARGIN + sepY*rows + CardGraphics.HEIGHT;
    }

    public UnoCard drawHiddenCard() {
        return drawVisibleCard();
    }

    public int cardsInDeck() {
        return deckManager.count();
    }

    public void playerDrawCard() {}

    public void playerPlayCard(int c, UnoCard card) {}

    public void playerFinishTurnEarly() {}

    public void newGame(UnoCard topOfDeck, UnoCard[] hand) {}

    public void restore(UnoCard topOfDeck, UnoCard[] hand, int playerSize, List<UnoCard> discard, boolean playerWillStart, boolean hasDrawn) {}

    public void reveal(List<CardObject> cardObjects) {}

    public void chat(String message) {}

    public void canSave() {}

    public boolean fastReset() {
        return false;
    }

    public void willReset() {}

    public void gameOver() {}

    public void onClose() {}
}
