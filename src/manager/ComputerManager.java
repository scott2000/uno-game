package manager;


import card.NormalCard;
import card.UnoCard;
import card.WildCard;
import display.UnoDisplay;
import event.DelayEvent;
import event.DrawEvent;
import event.EndTurnEvent;
import event.PlayEvent;

public class ComputerManager extends OpponentManager {
    int bestColor;

    @Override
    public boolean claimsStart() {
        return false;
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
    public void startTurn() {
        super.startTurn();
        UnoDisplay.pushEvent(new DelayEvent(500));
        int playable = -1;
        int nonWild = -1;
        int matchColor = -1;
        int[] colorCounts = new int[4];
        for (UnoCard card : hand) {
            if (card instanceof NormalCard) {
                colorCounts[card.getColorCode()] += 1;
            }
        }
        int colorOffset = UnoDisplay.random.nextInt(4);
        int maxColor = 0;
        bestColor = colorOffset;
        for (int i = colorOffset; i < 4+colorOffset; i++) {
            int index = i%4;
            int c = colorCounts[index];
            if (c > maxColor) {
                maxColor = c;
                bestColor = index;
            }
        }
        int nonWildCount = 0;
        int targetColor = UnoDisplay.getTopOfDeck().getColorCode();
        int playerCards = UnoDisplay.getOtherManager().count();
        for (int c = hand.size()-1; c >= 0; c--) {
            UnoCard card = hand.get(c);
            if (UnoDisplay.canPlay(card)) {
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
                } else if (playerCards == 1 || playerCards == 2 && UnoDisplay.random.nextBoolean()){
                    ((WildCard) card).setColor(bestColor);
                    matchColor = c;
                    break;
                }
            }
        }
        int c;
        if (matchColor != -1) {
            c = matchColor;
        } else if (nonWild != -1) {
            c = nonWild;
        } else if (playable != -1 && (hand.size() <= 3 || playerCards <= 3 || UnoDisplay.random.nextInt(4) == 0)) {
            WildCard wild = (WildCard) hand.get(playable);
            wild.setColor(bestColor);
            c = playable;
        } else {
            UnoDisplay.pushEvent(new DrawEvent(this, UnoDisplay.drawCard()));
            return;
        }
        UnoDisplay.pushEvent(new PlayEvent(this, c));
    }

    @Override
    public void addCard(UnoCard card) {
        int c = hand.size();
        super.addCard(card);
        if (isTurn) {
            if (UnoDisplay.canPlay(card)) {
                if (card instanceof WildCard) {
                    ((WildCard) card).setColor(bestColor);
                }
                UnoDisplay.pushEvent(new DelayEvent(100));
                UnoDisplay.pushEvent(new PlayEvent(this, c));
            } else {
                UnoDisplay.pushEvent(new EndTurnEvent());
            }
        }
    }
}
