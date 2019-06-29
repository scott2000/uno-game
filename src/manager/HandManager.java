package manager;

import card.CardGraphics;
import card.CardObject;
import card.UnoCard;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class HandManager {
    public static final int MARGIN = 10;

    protected static final int SEP_X = CardGraphics.WIDTH + 5;
    protected static final int SEP_Y = CardGraphics.HEIGHT*2/3;
    static final int SEP_Y_HIDDEN = CardGraphics.HEIGHT/3;

    public List<CardObject> hand = new ArrayList<>();
    public boolean isTurn = false;

    public abstract void update(long time);
    public abstract int paint(Graphics2D g);

    public void reset() {
        hand.clear();
        isTurn = false;
    }

    protected void onTurnStart(int opponentHandSize) {}

    protected void onAddCard(CardObject cardObject, int c) {}

    protected void onRemoveCard(CardObject cardObject, int c) {}

    public final void addCard(CardObject card, boolean animate) {
        int c = hand.size();
        hand.add(card);
        if (animate) {
            for (CardObject handCard : hand) {
                handCard.startAnimating();
            }
        }
        onAddCard(card, c);
    }

    public final CardObject removeCard(int c, boolean animate) {
        if (animate) {
            for (CardObject handCard : hand) {
                handCard.startAnimating();
            }
        }
        CardObject cardObject = hand.remove(c);
        onRemoveCard(cardObject, c);
        return cardObject;
    }

    public final void startTurn(int opponentHandSize) {
        isTurn = true;
        onTurnStart(opponentHandSize);
    }

    public final void endTurn() {
        isTurn = false;
        for (CardObject cardObject : hand) {
            cardObject.setHighlighted(false);
        }
    }

    public final int count() {
        return hand.size();
    }

    protected final void sortHandAndAnimateForReveal() {
        UnoCard[] cards = new UnoCard[hand.size()];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = hand.get(i).getCard();
        }
        Arrays.sort(cards);
        for (int i = 0; i < cards.length; i++) {
            CardObject cardObject = hand.get(i);
            cardObject.setCard(cards[i]);
            cardObject.startAnimating();
        }
    }
}
