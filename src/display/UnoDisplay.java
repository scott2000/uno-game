package display;

import card.NormalCard;
import card.UnoCard;
import card.WildCard;
import event.*;
import event.Event;
import manager.ComputerManager;
import manager.HandManager;
import manager.OpponentManager;
import manager.PlayerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class UnoDisplay extends JPanel implements MouseListener {
    public static int width;
    public static int height;
    public static Random random = new Random();

    private static ArrayList<UnoCard> deck;
    private static ArrayList<UnoCard> discard;

    private static HandManager playerManager;
    private static OpponentManager opponentManager;
    private static UnoObject menu;

    private static UnoCard topOfDeck;
    private static boolean isInitialized = false;

    private static boolean isPlayerTurn;
    private static long gameOverTimer;

    private static Point drawPileLocation;
    private static Point topOfDeckLocation;

    private static ArrayDeque<Event> eventQueue;
    private static Event currentEvent;

    UnoDisplay() {
        addMouseListener(this);
    }

    private void setup() {
        reset();
        new Thread(() -> {
            try {
                long lastTime = System.currentTimeMillis();
                while (true) {
                    synchronized (this) {
                        long currentTime = System.currentTimeMillis();
                        long time = currentTime-lastTime;
                        lastTime = currentTime;

                        if (gameOverTimer == 0) {
                            updateWHD(false);
                            playerManager.update(time);
                            opponentManager.update(time);
                            topOfDeck.update(topOfDeckLocation.x, topOfDeckLocation.y, true, time);
                            if (currentEvent != null && currentEvent.isDone()) {
                                currentEvent = null;
                            }
                            if (currentEvent == null && !eventQueue.isEmpty()) {
                                currentEvent = eventQueue.removeFirst();
                                currentEvent.start();
                            }
                        } else if (currentTime >= gameOverTimer) {
                            reset();
                        }

                        repaint();
                    }
                    Thread.sleep(16);
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void reset() {
        deck = new ArrayList<>();
        for (int color = UnoCard.RED; color <= UnoCard.BLUE; color++) {
            deck.add(new NormalCard(color, 0));
            for (int number = 1; number <= UnoCard.DRAW_2; number++) {
                deck.add(new NormalCard(color, number));
                deck.add(new NormalCard(color, number));
            }
        }
        for (int i = 0; i < 4; i++) {
            deck.add(new WildCard(false));
            deck.add(new WildCard(true));
        }
        Collections.shuffle(deck, random);
        discard = new ArrayList<>();
        eventQueue = new ArrayDeque<>();
        updateWHD(false);
        opponentManager = new ComputerManager();
        playerManager = new PlayerManager();
        for (int i = 0; i < 7; i++) {
            opponentManager.addCard(drawCard());
            playerManager.addCard(drawCard());
        }
        topOfDeck = null;
        for (int i = deck.size()-1; ; i--) {
            if (deck.get(i).isNumeric()) {
                topOfDeck = deck.remove(i);
                topOfDeck.setPosition(drawPileLocation.x, drawPileLocation.y);
                topOfDeck.setFlipped(false);
                break;
            }
        }
        gameOverTimer = 0;
        if (opponentManager.claimsStart()) {
            isPlayerTurn = false;
            opponentManager.startTurn();
        } else {
            isPlayerTurn = true;
            playerManager.startTurn();
        }
    }

    public static void playCard(UnoCard card) {
        discard.add(topOfDeck);
        topOfDeck = card;
        if (getCurrentManager().count() == 0) {
            pushEvent(new GameOverEvent());
        }
        if (card.isSkip()) {
            HandManager other = getOtherManager();
            int draws = card.cardDraws();
            for (int i = 0; i < draws; i++) {
                pushEvent(new DrawEvent(other, drawCard()));
            }
            pushEvent(new RestartTurnEvent());
        } else {
            pushEvent(new EndTurnEvent());
        }
    }

    public static void finishTurn() {
        if (isPlayerTurn) {
            playerManager.endTurn();
            isPlayerTurn = false;
            opponentManager.startTurn();
        } else {
            opponentManager.endTurn();
            isPlayerTurn = true;
            playerManager.startTurn();
        }
    }

    public static void endGame() {
        gameOverTimer = System.currentTimeMillis()+5000;
    }

    public static Point getDrawPileLocation() {
        return drawPileLocation;
    }

    private static UnoCard getDrawCard() {
        int size = deck.size();
        // If the deck is empty, use discarded cards
        if (size == 0) {
            size = discard.size();
            // If there are no discarded cards, make some new (boring) cards
            if (size == 0) {
                return new NormalCard(random.nextInt(4), random.nextInt(9)+1);
            }
            deck = discard;
            discard = new ArrayList<>();
            for (UnoCard card : deck) {
                if (card instanceof WildCard) {
                    ((WildCard) card).setColor(-1);
                }
            }
            Collections.shuffle(deck, random);
        }
        return deck.remove(size - 1);
    }

    public static UnoCard drawCard() {
        UnoCard card = getDrawCard();
        card.setPosition(drawPileLocation.x, drawPileLocation.y);
        card.setFlipped(false);
        return card;
    }

    public static boolean canPlay(UnoCard card) {
        return card.canPlay(topOfDeck.getColorCode(), topOfDeck.getNumberCode());
    }

    public static void setMenu(UnoObject menu) {
        UnoDisplay.menu = menu;
    }

    public static UnoCard getTopOfDeck() {
        return topOfDeck;
    }

    public static HandManager getCurrentManager() {
        return isPlayerTurn ? playerManager : opponentManager;
    }

    public static HandManager getOtherManager() {
        return isPlayerTurn ? opponentManager : playerManager;
    }

    public static void pushEvent(Event event) {
        eventQueue.addLast(event);
    }

    public static boolean hasEvent() {
        return !eventQueue.isEmpty();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        displayUno((Graphics2D) g);
    }

    private void updateWHD(boolean willDisplay) {
        width = getWidth();
        height = getHeight();
        int dy = (height-UnoCard.HEIGHT)/2+2;
        drawPileLocation = new Point((width-2*UnoCard.WIDTH)/2-5, dy);
        topOfDeckLocation = new Point(width/2+5, dy);
        if (!willDisplay) {
            drawPileLocation.x += 4;
            drawPileLocation.y -= 4;
            int s = Math.min(discard.size(), 4);
            topOfDeckLocation.x += s;
            topOfDeckLocation.y -= s;
        }
    }

    private synchronized void displayUno(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (!isInitialized) {
            setup();
            isInitialized = true;
        }

        updateWHD(true);

        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, width+32, height+32);

        int dy = drawPileLocation.y;
        for (int i = 0; i < 4; i++) {
            UnoCard.paintBlank(g, drawPileLocation.x, drawPileLocation.y);
            drawPileLocation.x += 1;
            drawPileLocation.y -= 1;
        }
        UnoCard.paintBlank(g, drawPileLocation.x, drawPileLocation.y);

        int dx = width/2+5;
        for (int i = Math.max(0, discard.size()-4); i < discard.size(); i++) {
            discard.get(i).paint(g, false, dx, dy, 1.0f);
            dx += 1;
            dy -= 1;
        }
        topOfDeck.paint(g, false);

        opponentManager.paint(g);
        playerManager.paint(g);

        if (menu != null) {
            menu.paint(g);
        }

        if (gameOverTimer != 0) {
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.75f));
            g.fillRect(width/2-200, height/2-100, 400, 200);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            UnoCard.shadowTextCenter(g, isPlayerTurn ? "YOU WIN!" : "YOU LOSE", width/2, height/2);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        synchronized (this) {
            if (currentEvent == null) {
                if (menu != null) {
                    menu.click(e.getX(), e.getY());
                } else if (isPlayerTurn) {
                    playerManager.click(e.getX(), e.getY());
                } else {
                    opponentManager.click(e.getX(), e.getY());
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}
