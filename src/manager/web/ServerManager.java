package manager.web;

import card.CardObject;
import card.UnoCard;
import display.UnoMain;
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
            UnoMain.failConnect();
            return;
        }
        System.out.printf("server(%d)\n", port);
        initialize();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.kind) {
        case "reset":
            if (UnoPanel.isGameRunning()) {
                UnoPanel.requestRestore();
                return;
            } // otherwise fallthrough
        default:
            super.handleMessage(message);
        }
    }

    @Override
    protected Socket start() {
        try {
            Socket newSocket = socket.accept();
            socket.setSoTimeout(30_000);
            return newSocket;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void reset() {
        super.reset();
        waitFor("reset");
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
            write("reveal", DeckManager.saveCardObjects(cardObjects));
        }
    }

    @Override
    public void noEventsInQueue() {
        deckManager.saveGame();
    }

    @Override
    public void gameOver() {
        deckManager.deleteSave();
    }
}
