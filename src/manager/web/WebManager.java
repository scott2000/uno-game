package manager.web;

import card.CardObject;
import card.UnoCard;
import display.UnoMain;
import display.UnoPanel;
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
    static final char MESSAGE_SEPARATOR = ':';

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private HashMap<String, MessageHandler> handlers = new HashMap<>();
    private boolean hasConnected = false;
    private boolean isClosed = false;

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
                        UnoMain.desynchronized();
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

    private synchronized MessageHandler handlerFor(String kind) {
        MessageHandler handler = handlers.get(kind);
        if (handler == null) {
            handler = new MessageHandler();
            handlers.put(kind, handler);
        }
        return handler;
    }

    abstract Socket start();

    // opponent actions must be handled as events due to possible timing desynchronization
    public void handleMessage(Message message) {
        switch (message.kind) {
        case "drawCard":
            UnoPanel.pushEvent(() -> {
                requireTurn();
                UnoPanel.drawCard();
            });
            return;
        case "playCard":
            UnoPanel.pushEvent(() -> {
                requireTurn();
                String[] parts = message.contents.split(" ");
                int c = Integer.parseInt(parts[0]);
                CardObject cardObject = hand.get(c);
                UnoCard oldCard = cardObject.getCard();
                UnoCard newCard = UnoCard.decode(parts[1]);
                if (oldCard == null || oldCard.canBecome(newCard)) {
                    cardObject.setCard(newCard);
                    UnoPanel.playCard(c);
                } else {
                    invalid("Opponent tried to play "+oldCard+" as "+newCard+".");
                }
            });
            return;
        case "finishTurnEarly":
            UnoPanel.pushEvent(() -> {
                requireTurn();
                UnoPanel.finishTurnEarly();
            });
            return;
        case "invalid":
            error("Opponent believes move was invalid.");
            return;
        case "close":
            synchronized (this) {
                isClosed = true;
            }
            UnoPanel.pushEvent(UnoMain::opponentClosed);
            return;
        default:
            handlerFor(message.kind).addMessage(message.contents);
        }
    }

    final void initialize() {
        connect();
        Thread webLoop = new Thread(() -> {
            String next;
            while ((next = read()) != null) {
                int firstSpace = next.indexOf(MESSAGE_SEPARATOR);
                if (firstSpace == -1) {
                    handleMessage(new Message(next));
                } else {
                    handleMessage(new Message(next.substring(0, firstSpace), next.substring(firstSpace+1)));
                }
            }
        });
        webLoop.setName("webLoop");
        webLoop.start();
    }

    private void connect() {
        JDialog message;
        if (hasConnected) {
            System.out.println("Attempting to reconnect...");
            message = UnoMain.reconnect();
        } else {
            System.out.println("Connecting... ");
            message = UnoMain.connect();
        }
        message.setModalityType(JDialog.ModalityType.MODELESS);
        message.setVisible(true);
        try {
            if (socket != null) socket.close();
            socket = start();

            if (socket != null) {
                System.out.println("Connected!");
                hasConnected = true;
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), false);
                return;
            }
        } catch (IOException ignored) {} finally {
            message.dispose();
        }
        System.out.flush();
        System.err.println("Failed.");
        if (hasConnected) {
            UnoMain.disconnect();
        } else {
            UnoMain.failConnect();
        }
    }

    private void requireTurn() {
        if (!isTurn) {
            invalid("Opponent moved out of turn.");
        }
    }

    private void error(String msg) {
        System.out.flush();
        System.err.println("[!] "+msg);
        UnoMain.desynchronized();
    }

    private void invalid(String msg) {
        write("invalid");
        error(msg);
    }

    private String read() {
        while (true) {
            String result = null;
            try {
                result = input.readLine();
            } catch (IOException ignored) {}
            synchronized (this) {
                if (result == null) {
                    if (isClosed) return null;
                    connect();
                } else {
                    System.out.println("> "+result);
                    return result;
                }
            }
        }
    }

    final void write(String text) {
        while (true) {
            output.println(text);
            synchronized (this) {
                if (output.checkError() || socket == null || socket.isClosed()) {
                    if (isClosed) return;
                    connect();
                } else {
                    return;
                }
            }
        }
    }

    final void write(String kind, String contents) {
        if (contents == null || contents.isEmpty()) {
            write(kind);
        } else {
            write(kind+MESSAGE_SEPARATOR+contents);
        }
    }

    final String waitFor(String kind) {
        return handlerFor(kind).next();
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
    public void onClose() {
        write("close");
    }
}