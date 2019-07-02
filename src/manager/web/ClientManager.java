package manager.web;

import card.UnoCard;
import display.UnoPanel;
import manager.DeckManager;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public final class ClientManager extends WebManager {
    private String host;
    private int port;

    private int cardCount;

    public ClientManager(String host, int port) {
        this.host = host;
        this.port = port;
        System.out.printf("client(%s, %d)\n", host, port);
        enableDynamicFor("start");
        enableDynamicFor("clientCanStart");
        enableDynamicFor("card");
        initialize();
    }

    @Override
    protected Socket start() {
        for (int retry = 0; retry < 10; retry++) {
            try {
                Thread.sleep(retry*500);
                if (retry != 0) {
                    System.out.flush();
                    System.err.printf("Failed. Trying again... (attempt %d)\n", retry+1);
                    System.err.flush();
                }
                try {
                    return new Socket(host, port);
                } catch (IOException ignored) {}
            } catch (InterruptedException ignored) {}
        }
        return null;
    }

    @Override
    public void reset() {
        super.reset();
        String[] resetParams = waitFor("start").split(Character.toString(MESSAGE_SEPARATOR));
        String[] cardStrings = resetParams[1].split(" ");
        UnoCard topOfDeck = UnoCard.decode(cardStrings[0]);
        UnoCard[] playerHand = new UnoCard[cardStrings.length-1];
        for (int i = 1; i < cardStrings.length; i++) {
            playerHand[i-1] = UnoCard.decode(cardStrings[i]);
        }
        switch (resetParams[0]) {
        case "new":
            cardCount = DeckManager.INITIAL_DECK_COUNT;
            UnoPanel.newGame(topOfDeck, playerHand, new UnoCard[DeckManager.CARDS_PER_HAND]);
            break;
        case "restore":
            String[] discardStrings = resetParams[2].split(" ");
            int discardSize = discardStrings.length-1;
            int opponentSize = Integer.parseInt(discardStrings[0]);
            ArrayList<UnoCard> discard = new ArrayList<>(discardSize);
            for (int i = 1; i < discardStrings.length; i++) {
                discard.add(UnoCard.decode(discardStrings[i]));
            }
            boolean playerTurn = resetParams[3].equals("1");
            boolean canDraw = resetParams[4].equals("1");
            cardCount = DeckManager.CARDS_PER_DECK - discardSize - playerHand.length - opponentSize - 1;
            UnoPanel.restore(topOfDeck, discard, playerHand, new UnoCard[opponentSize], playerTurn, canDraw);
            break;
        }
    }

    @Override
    public boolean playerCanStart() {
        return waitFor("clientCanStart").equals("1");
    }

    @Override
    public int cardsInDeck() {
        return cardCount;
    }

    private void willDraw() {
        if (cardCount == 0) {
            cardCount = UnoPanel.takeDiscardPile().size();
            if (cardCount == 0) {
                cardCount = DeckManager.CARDS_PER_DECK;
            }
        }
        cardCount--;
    }

    @Override
    public UnoCard drawVisibleCard() {
        willDraw();
        return UnoCard.decode(waitFor("card"));
    }

    @Override
    public UnoCard drawHiddenCard() {
        willDraw();
        return null;
    }
}
