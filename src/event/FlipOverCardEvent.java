package event;

import card.UnoCard;
import display.UnoDisplay;

public class FlipOverCardEvent implements Event {
    private UnoCard card;

    public FlipOverCardEvent(UnoCard card) {
        this.card = card;
    }

    @Override
    public void start() {
        UnoDisplay.flipOverCard(card);
    }

    @Override
    public boolean isDone() {
        return !card.isAnimating();
    }
}
