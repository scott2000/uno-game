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
    private static final long FPS = 200;
    private static final long REFRESH_DELAY = 1000/FPS;

    public static final Random random = new Random();

    public static int width;
    public static int height;

    private static ArrayList<UnoCard> deck;
    private static ArrayList<UnoCard> discard;

    private static HandManager playerManager;
    private static OpponentManager opponentManager;
    private static UnoObject menu;

    private static UnoCard topOfDeck;
    private static boolean isInitialized = false;

    private static boolean isPlayerTurn;
    private static boolean isGameStarted;
    private static long gameOverTimer;

    private static Point drawPileLocation;
    private static Point topOfDeckLocation;

    private static ArrayDeque<Event> eventQueue;
    private static Event currentEvent;

    /*
     * Ideas:
     * - Maybe replace the play/keep menu with an end turn button after drawing?
     * - Online play (server/client opponent)
     * - Possibly allow computer vs computer play by making board side and card visibility configurable?
     */

    UnoDisplay() {
        addMouseListener(this);
        setBackground(Color.WHITE);
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

                            if (topOfDeck != null) {
                                topOfDeck.update(topOfDeckLocation.x, topOfDeckLocation.y, true, time);
                            }

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
                    Thread.sleep(REFRESH_DELAY);
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
            pushEvent(new DrawEvent(opponentManager, drawCard()));
            pushEvent(new DrawEvent(playerManager, drawCard()));
        }
        topOfDeck = null;
        for (int i = deck.size()-1; ; i--) {
            if (deck.get(i).isNumeric()) {
                UnoCard card = deck.remove(i);
                card.setPosition(drawPileLocation.x, drawPileLocation.y);
                card.setFlipped(false);
                pushEvent(new FlipOverCardEvent(card));
                break;
            }
        }
        gameOverTimer = 0;
        isPlayerTurn = !opponentManager.claimsStart();
        isGameStarted = false;
        pushEvent(new StartGameEvent());
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

    public static void startGame() {
        if (isGameStarted) {
            throw new IllegalStateException("error: game already started");
        }
        isGameStarted = true;
        if (isPlayerTurn) {
            playerManager.startTurn();
        } else {
            opponentManager.startTurn();
        }
    }

    public static void flipOverCard(UnoCard card) {
        if (topOfDeck == null) {
            topOfDeck = card;
        } else {
            throw new IllegalStateException("error: card already flipped over");
        }
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
        setHints(g);
        if (!isInitialized) {
            setup();
            isInitialized = true;
        }

        updateWHD(true);

        int dy = drawPileLocation.y;
        for (int i = 0; i < 4; i++) {
            UnoCard.paintBlank(g, drawPileLocation.x, drawPileLocation.y);
            drawPileLocation.x += 1;
            drawPileLocation.y -= 1;
        }
        UnoCard.paintBlank(g, drawPileLocation.x, drawPileLocation.y);

        int dx = width/2+5;
        for (int i = Math.max(0, discard.size()-4); i < discard.size(); i++) {
            discard.get(i).paint(g, false, dx, dy, 1.0);
            dx += 1;
            dy -= 1;
        }

        if (topOfDeck != null) {
            topOfDeck.paint(g, false);
        }

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
            shadowTextCenter(g, isPlayerTurn ? "YOU WIN!" : "YOU LOSE", width/2, height/2);
        }
    }

    public static void setHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static void shadowTextCenter(Graphics2D g, String text, float x, float y) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        float x2 = x - metrics.stringWidth(text)/2.0f;
        float y2 = y - metrics.getHeight()/2.0f + metrics.getAscent();
        shadowText(g, text, x2, y2);
    }

    public static void shadowText(Graphics2D g, String text, float x, float y) {
        Color c = g.getColor();
        g.setColor(Color.DARK_GRAY);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                g.drawString(text, x + a, y + b);
            }
        }
        g.setColor(c);
        g.drawString(text, x, y);
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
