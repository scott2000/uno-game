package manager;

import card.UnoCard;
import display.UnoObject;

import java.util.ArrayList;

public abstract class HandManager implements UnoObject {
    ArrayList<UnoCard> hand = new ArrayList<>();
    boolean isTurn = false;

    public abstract void update(long time);

    public void startTurn() {
        isTurn = true;
    }

    public void endTurn() {
        isTurn = false;
    }

    public int addCard(UnoCard card) {
        int c = hand.size();
        hand.add(card);
        return c;
    }

    public UnoCard takeCard(int c) {
        return hand.remove(c);
    }

    public final int count() {
        return hand.size();
    }
}
