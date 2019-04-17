package event;

import card.UnoCard;
import display.UnoDisplay;
import manager.HandManager;

public class PlayEvent implements Event {
    private HandManager target;
    private int c;
    private UnoCard card;

    public PlayEvent(HandManager target, int c) {
        this.target = target;
        this.c = c;
    }

    @Override
    public void start() {
        card = target.takeCard(c);
        UnoDisplay.playCard(card);
        card.startAnimating();
    }

    @Override
    public boolean isDone() {
        return card.doneAnimating();
    }
}
