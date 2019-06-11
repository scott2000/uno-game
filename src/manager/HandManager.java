package manager;

import card.CardObject;

import java.util.ArrayList;

public class HandManager {
    public ArrayList<CardObject> hand = new ArrayList<>();
    public boolean isTurn = false;

    public void reset() {
        hand.clear();
        isTurn = false;
    }

    protected void onTurnStart(int opponentHandSize) {}

    protected void onAddCard(CardObject cardObject, int c) {}

    protected void onRemoveCard(CardObject cardObject, int c) {}

    public final void addCard(CardObject card) {
        for (CardObject handCard : hand) {
            handCard.startAnimating();
        }
        int c = hand.size();
        hand.add(card);
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
