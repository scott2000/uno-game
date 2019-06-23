package manager;

import card.CardObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class HandManager {
    public List<CardObject> hand = new ArrayList<>();
    public boolean isTurn = false;

    public abstract void update(long time);
    public abstract void paint(Graphics2D g);

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

    public final CardObject removeCard(int c) {
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
    }

    public final int count() {
        return hand.size();
    }
}
