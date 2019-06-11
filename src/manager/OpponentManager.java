package manager;

import card.UnoCard;

public abstract class OpponentManager extends HandManager {
    protected DeckManager deckManager = null;

    public abstract boolean claimsStart();

    public UnoCard drawCard() {
        return deckManager.draw();
    }

    @Override
    public void reset() {
        super.reset();
        if (deckManager != null) {
            deckManager.startGame();
        }
    }
}
