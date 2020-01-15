package manager.local;

import card.CardObject;
import card.NormalCard;
import card.UnoCard;
import card.WildCard;
import display.UnoPanel;
import manager.DeckManager;
import manager.OpponentManager;

import java.util.List;

public final class ComputerManager extends OpponentManager {
    private int winningBy = 0;
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
     *   The computer will chose the first possible option in the following list, with a few special cases to make it
     * a bit less predictable or to help the player if they have been losing frequently. Also, the computer attempts
     * to save wild cards until the end instead of wasting them early on: this is the core of its strategy.
     *
     * Play a wild card if the player is about to win       (except for sometimes when on a winning streak)
     * Play a numeric card with the same color              (except for sometimes when on a winning streak)
     * Play any card with the same color                    (except for sometimes when on a winning streak)
     * Play a matching card with the best possible color    (or a random color sometimes if on a winning streak)
     * Play a wild card                                     (or maybe not--more likely when on a winning streak--but
     * Draw a card and play it if possible                     usually tries to save cards until the end of the game)
     */
    @Override
    public void onTurnStart(int opponentHandSize) {
        UnoPanel.delay(500);
        UnoCard topOfDeck = UnoPanel.getTopOfDeck();
        if (UnoPanel.isPeeking()) {
            if (winningBy > 0) {
                winningBy = 0;
            } else if (winningBy > -5) {
                winningBy--;
            }
        }
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
        int alternateColor = -1;
        int maxColor = 0;
        bestColor = colorOffset;
        for (int i = colorOffset; i < 4+colorOffset; i++) {
            int index = i%4;
            int c = colorCounts[index];
            if (index != targetColor) {
                if (c >= maxColor) {
                    maxColor = c;
                    bestColor = index;
                } else if (c != 0) {
                    alternateColor = index;
                }
            }
        }
        if (alternateColor == -1) {
            alternateColor = bestColor;
        }
        int nonWildCount = 0;
        for (int c = hand.size()-1; c >= 0; c--) {
            UnoCard card = hand.get(c).getCard();
            if (card.canPlayOn(topOfDeck)) {
                playable = c;
                if (card instanceof NormalCard) {
                    if (opponentHandSize <= 2 && card.cardDraws() != 0) {
                        matchColor = c;
                        break;
                    }
                    int color = card.getColorCode();
                    int count = colorCounts[color];
                    if (count > nonWildCount) {
                        nonWild = c;
                        nonWildCount = count;
                    }
                    if (color == targetColor) {
                        matchColor = c;
                        if (card.isNumeric() && opponentHandSize > 2) {
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
        // When winning too much, ease off to make the player feel better at the game
        if (winningBy > 1 && hand.size() <= 10 && UnoPanel.RANDOM.nextInt(2*Math.max(1, 6 - winningBy)) == 0) {
            if (matchColor == -1 && nonWild != -1) {
                // Switch colors before using all of that color if possible
                UnoPanel.playCard(nonWild);
                return;
            }
            // Act as though the opponent isn't as close to winning
            opponentHandSize += 2;
            if (hand.size() <= 7 && opponentHandSize > 4) {
                // Otherwise, pick a suboptimal color for wild cards if it's not too risky
                bestColor = alternateColor;
            }
        }
        if (matchColor != -1) {
            UnoPanel.playCard(matchColor);
        } else if (nonWild != -1) {
            UnoPanel.playCard(nonWild);
        } else if (playable != -1 && (hand.size() <= 3 || hand.size() > 10
                || opponentHandSize <= 3 || opponentHandSize > 10
                || UnoPanel.RANDOM.nextInt(Math.max(2, 5 - winningBy)) == 0)) {
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
    public void restore(UnoCard topOfDeck, UnoCard[] hand, int playerSize, List<UnoCard> discard, boolean playerWillStart, boolean hasDrawn) {
        // When restoring, guess how much of an advantage the computer had last time
        winningBy = Math.max(-1, Math.min((playerSize - hand.length + 1) / 5, 3));
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
    public boolean fastReset() {
        return true;
    }

    @Override
    public void gameOver() {
        if (isTurn) {
            winningBy++;
        } else {
            winningBy--;
        }
        deckManager.deleteSave();
    }
}
