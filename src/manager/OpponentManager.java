package manager;

import card.CardObject;
import card.UnoCard;
import display.UnoPanel;

import java.awt.*;
import java.util.List;

public abstract class OpponentManager extends HandManager {
    protected DeckManager deckManager = null;

    public abstract boolean playerCanStart();

    public abstract UnoCard drawVisibleCard();

    @Override
    public void update(long time) {
        int columns = Math.max((UnoPanel.width-15)/SEP_X, 3);
        int indent = (UnoPanel.width - Math.min(columns, hand.size())*SEP_X + 5)/2;

        boolean gameOver = UnoPanel.isGameOver();
        int sepY = gameOver && !hand.isEmpty() && hand.get(0).getCard() != null ? SEP_Y : SEP_Y_HIDDEN;

        for (int c = 0; c < hand.size(); c++) {
            int row = c/columns;
            int column = c%columns;
            float x = indent + SEP_X*column;
            float y = 10 + sepY*row;
            hand.get(c).update(x, y, gameOver, time);
        }
    }

    @Override
    public void paint(Graphics2D g) {
        for (CardObject card : hand) {
            card.paint(g, false);
        }
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

    public void restore(UnoCard topOfDeck, UnoCard[] hand, int playerSize, List<UnoCard> discard, boolean playerWillStart) {}

    public void reveal(List<CardObject> cardObjects) {}

    public void noEventsInQueue() {}

    public void gameOver() {}

    public void onClose() {}
}
