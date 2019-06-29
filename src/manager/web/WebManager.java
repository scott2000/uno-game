package manager.web;

import card.CardObject;
import card.UnoCard;
import display.Uno;
import display.UnoPanel;
import manager.DeckManager;
import manager.OpponentManager;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;

public abstract class WebManager extends OpponentManager {
    public static final int DEFAULT_PORT = 3108;

    static final char OPTIONAL = '?';
    static final char MESSAGE_SEPARATOR = ':';

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private HashMap<String, MessageHandler> handlers = new HashMap<>();
    private boolean isClosed = false;

    private boolean hasEstablishedVersion = false;

    private static class MessageHandler {
        private ArrayDeque<String> messages = new ArrayDeque<>();
        private ArrayDeque<Mailbox> waitingMailboxes = new ArrayDeque<>();

        static class Mailbox {
            String message;
            boolean received = false;

            synchronized void send(String msg) {
                message = msg;
                received = true;
                notify();
            }

            synchronized String take() {
                while (!received) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Uno.desynchronized();
                    }
                }
                return message;
            }
        }

        synchronized void addMessage(String msg) {
            if (waitingMailboxes.isEmpty()) {
                messages.addLast(msg == null ? "" : msg);
            } else {
                waitingMailboxes.removeFirst().send(msg);
            }
        }

        String next() {
            Mailbox mailbox;
            synchronized (this) {
                if (messages.isEmpty()) {
                    mailbox = new Mailbox();
                    waitingMailboxes.addLast(mailbox);
                } else {
                    return messages.removeFirst();
                }
            }
            return mailbox.take();
        }
    }

    private synchronized MessageHandler handlerFor(String kind, boolean optional) {
        MessageHandler handler = handlers.get(kind);
        if (handler == null) {
            // it wasn't declared, so it will never be read from and can be safely created and then discarded
            handler = new MessageHandler();
            if (!optional) {
                write("invalid", MESSAGE_SEPARATOR+kind);
                if (close()) Uno.unknownMessage(kind);
            }
        }
        return handler;
    }

    final void enableDynamicFor(String kind) {
        handlers.put(kind, new MessageHandler());
    }

    final void initialize() {
        enableDynamicFor("reset");
        connect();
        write("version", Integer.toString(Uno.VERSION));
        write(OPTIONAL+"setName", UnoPanel.getChat().getName());
        Thread webLoop = new Thread(() -> {
            String next;
            while ((next = read()) != null) {
                int separatorIndex = next.indexOf(MESSAGE_SEPARATOR);
                Message message;
                if (separatorIndex == -1) {
                    message = new Message(next);
                } else {
                    message = new Message(next.substring(0, separatorIndex), next.substring(separatorIndex+1));
                }
                try {
                    handleMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                    write("invalid", message.toString());
                    Uno.invalidMessage(message);
                }
            }
        });
        webLoop.setName("webLoop");
        webLoop.start();
    }

    private void connect() {
        System.out.println("Connecting... ");
        JDialog message = Uno.connect();
        message.setModalityType(JDialog.ModalityType.MODELESS);
        message.setVisible(true);
        try {
            if ((socket = start()) != null) {
                System.out.println("Connected!");
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), false);
                return;
            }
        } catch (IOException ignored) {} finally {
            message.dispose();
        }
        System.out.flush();
        System.err.println("Failed.");
        isClosed = true;
        Uno.failConnect();
    }

    private synchronized boolean close() {
        if (isClosed) {
            return false;
        }
        isClosed = true;
        try {
            socket.close();
        } catch (IOException ignored) {}
        return true;
    }

    abstract Socket start();

    // opponent actions should usually be handled as events due to possible timing desynchronization
    private void handleMessage(Message message) {
        switch (message.kind) {
        case "drawCard":
            UnoPanel.pushEvent("drawCard", () -> {
                requireTurn();
                if (UnoPanel.canDraw()) {
                    UnoPanel.drawCard();
                } else {
                    invalid("Opponent tried to draw a second card.");
                }
            });
            return;
        case "playCard":
            UnoPanel.pushEvent("playCard", () -> {
                requireTurn();
                try {
                    String[] parts = message.contents.split(" ");
                    int c = Integer.parseInt(parts[0]);
                    UnoCard newCard = UnoCard.decode(parts[1]);
                    if (c < 0 || c >= hand.size()) {
                        invalid("Opponent believes their hand is a different size.");
                        return;
                    }
                    CardObject cardObject = hand.get(c);
                    UnoCard oldCard = cardObject.getCard();
                    if (oldCard == null || oldCard.canBecome(newCard)) {
                        cardObject.setCard(newCard);
                        UnoPanel.playCard(c);
                    } else {
                        invalid("Opponent tried to play "+oldCard+" as "+newCard+".");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    write("invalid", message.toString());
                    Uno.invalidMessage(message);
                }
            });
            return;
        case "finishTurnEarly":
            UnoPanel.pushEvent("finishTurnEarly", () -> {
                requireTurn();
                if (UnoPanel.canDraw()) {
                    invalid("Opponent tried to end turn without drawing a card.");
                } else {
                    UnoPanel.finishTurnEarly();
                }
            });
            return;
        case "reveal":
            synchronized (Uno.PANEL) {
                UnoCard[] newHand = DeckManager.loadCards(message.contents);
                if (newHand.length == hand.size()) {
                    for (int i = 0; i < newHand.length; i++) {
                        CardObject cardObject = hand.get(i);
                        UnoCard oldCard = cardObject.getCard();
                        UnoCard newCard = newHand[i];
                        if (oldCard == null || oldCard.canBecome(newCard)) {
                            cardObject.setCard(newCard);
                        } else {
                            invalid("Opponent tried to reveal "+oldCard+" as "+newCard+".");
                            return;
                        }
                    }
                    sortHandAndAnimateForReveal();
                } else {
                    invalid("Opponent tried to reveal hand of different size.");
                }
            }
            return;
        case "chat":
            if (message.contents != null) {
                synchronized (Uno.PANEL) {
                    UnoPanel.getChat().receive(message.contents);
                }
            }
            return;
        case "setName":
            if (message.contents != null) {
                synchronized (Uno.PANEL) {
                    UnoPanel.getChat().setOpponentName(message.contents);
                }
            }
            return;
        case "version":
            synchronized (this) {
                hasEstablishedVersion = true;
            }
            if (!Uno.isCompatible(message.contents)) {
                write("incompatible", Integer.toString(Uno.BACK_COMPAT_VERSION));
                if (close()) Uno.opponentIncompatible(message.contents);
            }
            return;
        case "incompatible":
            if (close()) Uno.playerIncompatible(message.contents);
            return;
        case "invalid":
            if (close()) Uno.desynchronized();
            return;
        case "close":
            if (close()) Uno.opponentClosed();
            return;
        default:
            handlerFor(message.kind, message.optional).addMessage(message.contents);
        }
    }

    private void requireTurn() {
        if (!isTurn) {
            invalid("Opponent moved out of turn.");
        }
    }

    private void error(String msg) {
        System.out.flush();
        System.err.println("! "+msg);
        if (close()) Uno.desynchronized();
    }

    private void invalid(String msg) {
        write("invalid");
        error(msg);
    }

    private String read() {
        String result = null;
        try {
            if (input != null) {
                result = input.readLine();
            }
        } catch (IOException ignored) {}
        if (result == null) {
            if (close()) Uno.disconnect();
        } else {
            System.out.println("> "+result);
        }
        return result;
    }

    private void write(String text) {
        synchronized (this) {
            if (output != null) {
                output.println(text);
                if (!output.checkError() && !socket.isClosed()) return;
            }
        }
        if (close()) Uno.disconnect();
    }

    final void write(String kind, String contents) {
        if (contents == null || contents.isEmpty()) {
            write(kind);
        } else {
            write(kind+MESSAGE_SEPARATOR+contents);
        }
    }

    final String waitFor(String kind) {
        return handlerFor(kind, false).next();
    }

    @Override
    public void playerDrawCard() {
        write("drawCard");
    }

    @Override
    public void playerPlayCard(int c, UnoCard card) {
        write("playCard", Integer.toString(c)+' '+card.encode());
    }

    @Override
    public void playerFinishTurnEarly() {
        write("finishTurnEarly");
    }

    @Override
    public void chat(String message) {
        write(OPTIONAL+"chat", message);
    }

    @Override
    public void willReset() {
        write("reset");
        waitFor("reset");
        synchronized (this) {
            if (!hasEstablishedVersion) {
                invalid("Opponent did not send version information before reset.");
            }
        }
    }

    @Override
    public void onClose() {
        write("close");
    }
}