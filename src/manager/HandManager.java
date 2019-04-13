package manager;

import card.UnoCard;
import display.UnoObject;

import java.util.ArrayList;

public abstract class HandManager extends UnoObject {
    ArrayList<UnoCard> hand = new ArrayList<>();
    boolean isTurn = false;

    public abstract void update(long time);

    public void startTurn() {
        isTurn = true;
    };

    public void endTurn() {
        isTurn = false;
    };

    public void addCard(UnoCard card) {
        hand.add(card);
    }

    public UnoCard take(int c) {
        return hand.remove(c);
    }

    public final int count() {
        return hand.size();
    }
}
