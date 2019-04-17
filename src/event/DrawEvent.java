package event;

import card.UnoCard;
import manager.HandManager;

public class DrawEvent implements Event {
    private HandManager target;
    private UnoCard card;

    public DrawEvent(HandManager target, UnoCard card) {
        this.target = target;
        this.card = card;
    }

    @Override
    public void start() {
        target.addCard(card);
        card.startAnimating();
    }

    @Override
    public boolean isDone() {
        return card.doneAnimating();
    }
}
