package manager.local;

import card.CardObject;
import card.UnoCard;
import display.UnoPanel;
import manager.HandManager;
import menu.PlayKeepMenu;

public class PlayerManager extends HandManager {
    @Override
    protected void onAddCard(CardObject cardObject, int c) {
        UnoPanel.playerAddSortedCard(cardObject, c);
        UnoCard card = cardObject.getCard();
        if (isTurn) {
            if (card.canPlayOn(UnoPanel.getTopOfDeck().getCard())) {
                UnoPanel.setMenu(new PlayKeepMenu(card, c));
            } else {
                UnoPanel.finishTurnEarly();
            }
        }
    }

    @Override
    protected void onRemoveCard(CardObject cardObject, int c) {
        UnoPanel.playerRemoveSortedCard(c);
    }
}
