package manager.web;

import card.CardObject;
import card.UnoCard;
import display.Uno;
import display.UnoPanel;
import manager.DeckManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ServerManager extends WebManager {
    private ServerSocket socket;

    public ServerManager(int port) {
        deckManager = new DeckManager("server");
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            Uno.failServerStart();
            return;
        }
        System.out.printf("server(%d)\n", port);
        initialize();
    }

    @Override
    protected Socket start() {
        try {
            return socket.accept();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void reset() {
        super.reset();
        deckManager.startGame();
    }

    @Override
    public boolean playerCanStart() {
        boolean start = UnoPanel.random.nextBoolean();
        write("clientCanStart", start ? "0" : "1");
        return start;
    }

    @Override
    public UnoCard drawVisibleCard() {
        return deckManager.draw();
    }

    @Override
    public UnoCard drawHiddenCard() {
        UnoCard card = deckManager.draw();
        write("card", card.encode());
        return card;
    }

    private void encodeTopHand(StringBuilder message, UnoCard topOfDeck, UnoCard[] hand) {
        message.append(topOfDeck.encode());
        for (UnoCard card : hand) {
            message.append(' ');
            message.append(card.encode());
        }
    }

    @Override
    public void newGame(UnoCard topOfDeck, UnoCard[] hand) {
        StringBuilder message = new StringBuilder("new;");
        encodeTopHand(message, topOfDeck, hand);
        write("start", message.toString());
    }

    @Override
    public void restore(UnoCard topOfDeck, UnoCard[] hand, int playerSize, List<UnoCard> discard, boolean playerWillStart) {
        StringBuilder message = new StringBuilder("restore;");
        encodeTopHand(message, topOfDeck, hand);
        message.append(';');
        message.append(playerSize);
        for (UnoCard card : discard) {
            message.append(' ');
            message.append(card.encode());
        }
        message.append(';');
        message.append(playerWillStart ? '0' : '1');
        write("start", message.toString());
    }

    @Override
    public void reveal(List<CardObject> cardObjects) {
        if (cardObjects.isEmpty()) {
            sortHandAndAnimateForReveal();
        } else {
            write(OPTIONAL+"reveal", DeckManager.saveCardObjects(cardObjects));
        }
    }

    @Override
    public void canSave() {
        deckManager.saveGame();
    }

    @Override
    public void gameOver() {
        deckManager.deleteSave();
    }
}
