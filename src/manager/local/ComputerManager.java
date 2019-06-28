package manager.local;

import card.CardObject;
import card.NormalCard;
import card.UnoCard;
import card.WildCard;
import display.UnoPanel;
import manager.DeckManager;
import manager.OpponentManager;

import java.util.List;

public class ComputerManager extends OpponentManager {
    private int bestColor;

    public ComputerManager() {
        deckManager = new DeckManager("computer");
    }

    @Override
    public void reset() {
        super.reset();
        deckManager.startGame();
    }

    @Override
    public boolean playerCanStart() {
        return true;
    }

    @Override
    public UnoCard drawVisibleCard() {
        return deckManager.draw();
    }

    /*
     * Play a wild card if the player is about to win
     * Play a numeric card with the same color
     * Play any card with the same color
     * Play a matching card with the best possible color
     * Play a wild card (or maybe not)
     * Draw a card and play it if possible
     */
    @Override
    public void onTurnStart(int opponentHandSize) {
        UnoPanel.delay(500);
        UnoCard topOfDeck = UnoPanel.getTopOfDeck();
        int playable = -1;
        int nonWild = -1;
        int matchColor = -1;
        int[] colorCounts = new int[4];
        for (CardObject cardObject : hand) {
            UnoCard card = cardObject.getCard();
            if (card instanceof NormalCard) {
                colorCounts[card.getColorCode()] += 1;
            }
        }
        int colorOffset = UnoPanel.RANDOM.nextInt(4);
        int targetColor = topOfDeck.getColorCode();
        int maxColor = 0;
        bestColor = colorOffset;
        for (int i = colorOffset; i < 4+colorOffset; i++) {
            int index = i%4;
            int c = colorCounts[index];
            if (c > maxColor && index != targetColor) {
                maxColor = c;
                bestColor = index;
            }
        }
        int nonWildCount = 0;
        for (int c = hand.size()-1; c >= 0; c--) {
            UnoCard card = hand.get(c).getCard();
            if (card.canPlayOn(topOfDeck)) {
                playable = c;
                if (card instanceof NormalCard) {
                    int color = card.getColorCode();
                    int count = colorCounts[color];
                    if (count > nonWildCount) {
                        nonWild = c;
                        nonWildCount = count;
                    }
                    if (color == targetColor) {
                        matchColor = c;
                        if (card.isNumeric()) {
                            break;
                        }
                    }
                } else if (opponentHandSize == 1 || opponentHandSize == 2 && UnoPanel.RANDOM.nextBoolean()){
                    ((WildCard) card).setColor(bestColor);
                    matchColor = c;
                    break;
                }
            }
        }
        if (matchColor != -1) {
            UnoPanel.playCard(matchColor);
        } else if (nonWild != -1) {
            UnoPanel.playCard(nonWild);
        } else if (playable != -1 && (hand.size() <= 3 || opponentHandSize <= 3 || UnoPanel.RANDOM.nextInt(4) == 0)) {
            WildCard wild = (WildCard) hand.get(playable).getCard();
            wild.setColor(bestColor);
            UnoPanel.playCard(playable);
        } else {
            UnoPanel.drawCard();
        }
    }

    @Override
    protected void onAddCard(CardObject cardObject, int c) {
        if (isTurn) {
            UnoCard card = cardObject.getCard();
            if (card.canPlayOn(UnoPanel.getTopOfDeck())) {
                if (card instanceof WildCard) {
                    ((WildCard) card).setColor(bestColor);
                }
                UnoPanel.delay(100);
                UnoPanel.playCard(c);
            } else {
                UnoPanel.finishTurnEarly();
            }
        }
    }

    @Override
    public void reveal(List<CardObject> cardObjects) {
        sortHandAndAnimateForReveal();
    }

    @Override
    public void canSave() {
        deckManager.saveGame();
    }

    @Override
    public void gameOver() {
        deckManager.deleteSave();
    }
}
