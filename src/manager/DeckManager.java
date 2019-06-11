package manager;

import card.UnoCard;
import display.UnoPanel;

import java.util.ArrayList;

public class DeckManager {
    public static final int CARDS_PER_HAND = 7;

    private ArrayList<UnoCard> deck;

    UnoCard draw() {
        // If the deck is empty, use discarded cards
        if (deck.isEmpty()) {
            deck = UnoPanel.newDeckFromDiscard();
        }
        return deck.remove(deck.size() - 1);
    }

    UnoCard[] startGame() {
        deck = UnoCard.newDeck();
        UnoCard[] playerHand = new UnoCard[CARDS_PER_HAND];
        UnoCard[] remoteHand = new UnoCard[CARDS_PER_HAND];
        for (int i = 0; i < CARDS_PER_HAND; i++) {
            playerHand[i] = draw();
            remoteHand[i] = draw();
        }
        for (int i = deck.size()-1; ; i--) {
            if (deck.get(i).isNumeric()) {
                UnoPanel.newGame(deck.remove(i), playerHand, remoteHand);
                return remoteHand;
            }
        }
    }


}
