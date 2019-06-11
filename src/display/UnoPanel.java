package display;

import card.CardGraphics;
import card.CardObject;
import card.UnoCard;
import card.WildCard;
import manager.DeckManager;
import manager.HandManager;
import manager.OpponentManager;
import manager.local.ComputerManager;
import manager.local.PlayerManager;
import menu.ColorSelectMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;

public class UnoPanel extends JPanel implements MouseListener {
    private static final long FPS = 200;
    private static final long REFRESH_DELAY = 1000/FPS;

    public static final Random random = new Random();

    public static int width;
    public static int height;

    private static ArrayList<UnoCard> discard = new ArrayList<>();

    private static UnoMenu menu;

    private static CardObject topOfDeck;
    private static boolean isInitialized = false;

    private static long gameOverTimer;

    private static Point drawPileLocation;
    private static Point topOfDeckLocation;

    private static ArrayDeque<Event> eventQueue = new ArrayDeque<>();
    private static Event currentEvent;

    private static PlayerManager player;
    private static OpponentManager remote;
    private static boolean isTurn;

    private static int playerColumns;
    private static int playerRows;
    private static ArrayList<Integer> playerSortedIndices = new ArrayList<>();

    UnoPanel() {
        addMouseListener(this);
        setBackground(Color.WHITE);
    }

    private void setup() {
        player = new PlayerManager();
        remote = new ComputerManager();
        reset();
        new Thread(() -> {
            try {
                long lastTime = System.currentTimeMillis();
                while (true) {
                    synchronized (this) {
                        long currentTime = System.currentTimeMillis();
                        long time = currentTime-lastTime;
                        lastTime = currentTime;

                        updateWHD(false);
                        updateCR();
                        int playerIndent = (width - Math.min(playerColumns, player.hand.size())*CardGraphics.SEP_X + 5)/2;

                        for (int cc = 0; cc < playerSortedIndices.size(); cc++) {
                            int row = cc/playerColumns;
                            int column = cc%playerColumns;
                            float x = playerIndent + CardGraphics.SEP_X*column;
                            float y = height - CardGraphics.SEP_Y_PLAYER*(playerRows-row) - CardGraphics.HEIGHT - 10;
                            player.hand.get(playerSortedIndices.get(cc)).update(x, y, true, time);
                        }

                        int remoteColumns = Math.max((width-15)/CardGraphics.SEP_X, 3);
                        int remoteIndent = (UnoPanel.width - Math.min(remoteColumns, remote.hand.size())*CardGraphics.SEP_X + 5)/2;

                        for (int c = 0; c < remote.hand.size(); c++) {
                            int row = c/remoteColumns;
                            int column = c%remoteColumns;
                            float x = remoteIndent + CardGraphics.SEP_X*column;
                            float y = 10 + CardGraphics.SEP_Y_COMPUTER*row;
                            remote.hand.get(c).update(x, y, false, time);
                        }

                        if (topOfDeck != null) {
                            topOfDeck.update(topOfDeckLocation.x, topOfDeckLocation.y, true, time);
                        }

                        if (gameOverTimer == 0) {
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
        discard.clear();
        eventQueue.clear();
        topOfDeck = null;
        gameOverTimer = 0;
        updateWHD(false);
        player.reset();
        playerSortedIndices = new ArrayList<>();
        remote.reset();
        isTurn = !remote.claimsStart();
    }

    private static HandManager current() {
        return isTurn ? player : remote;
    }

    private static HandManager other() {
        return isTurn ? remote : player;
    }

    private static void updateCR() {
        playerColumns = Math.max((UnoPanel.width-15)/CardGraphics.SEP_X, 3);
        playerRows = (player.hand.size()-1)/playerColumns;
    }

    public static void playerAddSortedCard(CardObject cardObject, int c) {
        UnoCard card = cardObject.getCard();
        int order = card.getOrderCode();
        for (int cc = 0; cc < playerSortedIndices.size(); cc++) {
            if (order < player.hand.get(playerSortedIndices.get(cc)).getCard().getOrderCode()) {
                playerSortedIndices.add(cc, c);
                return;
            }
        }
        playerSortedIndices.add(c);
    }

    public static void playerRemoveSortedCard(int c) {
        int cc = 0;
        while (cc < playerSortedIndices.size()) {
            int i = playerSortedIndices.get(cc);
            if (i == c) {
                playerSortedIndices.remove(cc);
            } else {
                if (i > c) {
                    playerSortedIndices.set(cc, i-1);
                }
                cc++;
            }
        }
    }

    private static void pushEvent(Event event) {
        eventQueue.addLast(event);
    }

    private static boolean hasEvent() {
        return !eventQueue.isEmpty();
    }

    public static void setMenu(UnoMenu menu) {
        UnoPanel.menu = menu;
    }

    public static CardObject getTopOfDeck() {
        return topOfDeck;
    }

    public static ArrayList<UnoCard> newDeckFromDiscard() {
        if (discard.size() == 0) {
            return UnoCard.newDeck();
        } else {
            ArrayList<UnoCard> deck = discard;
            discard = new ArrayList<>();
            // TODO: notify opponent of discard pile change
            for (UnoCard card : deck) {
                if (card instanceof WildCard) {
                    ((WildCard) card).setColor(-1);
                }
            }
            Collections.shuffle(deck, random);
            return deck;
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
        int dy = (height-CardGraphics.HEIGHT)/2+2;
        drawPileLocation = new Point((width-2*CardGraphics.WIDTH)/2-5, dy);
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
            CardGraphics.paintBlank(g, drawPileLocation.x, drawPileLocation.y);
            drawPileLocation.x += 1;
            drawPileLocation.y -= 1;
        }
        CardGraphics.paintBlank(g, drawPileLocation.x, drawPileLocation.y);

        int dx = width/2+5;
        for (int i = Math.max(0, discard.size()-4); i < discard.size(); i++) {
            CardGraphics.paint(g, discard.get(i), false, dx, dy, 1.0);
            dx += 1;
            dy -= 1;
        }

        if (topOfDeck != null) {
            if (isTurn) {
                updateCR();
                g.setColor(topOfDeck.getCard().getColor());
                g.setFont(new Font("SansSerif", Font.BOLD, 24));
                shadowTextCenter(g, "Your Turn", width/2, height - CardGraphics.SEP_Y_PLAYER*playerRows - CardGraphics.HEIGHT - 30);
            }
            topOfDeck.paint(g, false);
        }

        for (CardObject card : remote.hand) {
            card.paint(g, false);
        }

        for (int c : playerSortedIndices) {
            CardObject cardObject = player.hand.get(c);
            cardObject.paint(g, hasEvent() || !isTurn || !cardObject.getCard().canPlayOn(topOfDeck.getCard()));
        }

        if (menu != null) {
            menu.paint(g);
        }

        if (gameOverTimer != 0) {
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.75f));
            g.fillRect(width/2-200, height/2-100, 400, 200);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            shadowTextCenter(g, player.isTurn ? "YOU WIN!" : "YOU LOSE", width/2, height/2);
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

    public static void delay(long time) {
        System.out.printf("delay(%d)\n", time);
        pushEvent(new Event() {
            private long timer = time;

            @Override
            public void start() {
                timer += System.currentTimeMillis();
            }

            @Override
            public boolean isDone() {
                return System.currentTimeMillis() >= timer;
            }
        });
    }

    public static void drawCard() {
        System.out.println("drawCard()");
        final UnoCard card = remote.drawCard();
        pushEvent(new Event() {
            private CardObject cardObject;

            @Override
            public void start() {
                cardObject = drawCardDirectlyTo(card, current());
            }

            @Override
            public boolean isDone() {
                return cardObject.doneAnimating();
            }
        });
    }

    public static void playCard(int c) {
        System.out.printf("playCard(%d)\n", c);
        pushEvent(new Event() {
            @Override
            public void start() {
                discard.add(topOfDeck.getCard());
                HandManager target = current();
                topOfDeck = target.removeCard(c);
                UnoCard card = topOfDeck.getCard();
                if (target.count() == 0) {
                    endGame();
                } else if (card.isSkip()) {
                    for (int i = 0; i < card.cardDraws(); i++) {
                        drawCardTo(remote.drawCard(), other());
                    }
                    restartTurn();
                } else {
                    finishTurn();
                }
                topOfDeck.startAnimating();
            }

            @Override
            public boolean isDone() {
                return topOfDeck.doneAnimating();
            }
        });
    }

    public static void newGame(UnoCard topOfDeck, UnoCard[] playerHand, UnoCard[] remoteHand) {
        System.out.printf("newGame(%s, %s, %s)\n", topOfDeck, Arrays.toString(playerHand), Arrays.toString(remoteHand));
        for (int i = 0; i < DeckManager.CARDS_PER_HAND; i++) {
            drawCardTo(remoteHand[i], remote);
            drawCardTo(playerHand[i], player);
        }
        pushEvent(new Event() {
            @Override
            public void start() {
                UnoPanel.topOfDeck = new CardObject();
                UnoPanel.topOfDeck.setCard(topOfDeck);
                UnoPanel.topOfDeck.setPosition(drawPileLocation.x, drawPileLocation.y);
                UnoPanel.topOfDeck.setFlipped(false);
                UnoPanel.topOfDeck.startAnimating();
            }

            @Override
            public boolean isDone() {
                return UnoPanel.topOfDeck.doneAnimating();
            }
        });
        pushEvent(() -> {
            if (isTurn) {
                player.startTurn(remote.count());
            } else {
                remote.startTurn(player.count());
            }
        });
    }

    public static void finishTurnEarly() {
        System.out.println("finishTurnEarly()");
        pushEvent(UnoPanel::finishTurn);
    }

    private static void drawCardTo(UnoCard card, HandManager target) {
        pushEvent(new Event() {
            private CardObject cardObject;

            @Override
            public void start() {
                cardObject = drawCardDirectlyTo(card, target);
            }

            @Override
            public boolean isDone() {
                return cardObject.doneAnimating();
            }
        });
    }

    private static CardObject drawCardDirectlyTo(UnoCard card, HandManager target) {
        CardObject cardObject = new CardObject();
        cardObject.setCard(card);
        cardObject.setPosition(drawPileLocation.x, drawPileLocation.y);
        cardObject.setFlipped(false);
        cardObject.startAnimating();
        target.addCard(cardObject);
        return cardObject;
    }

    private static void restartTurn() {
        System.out.println("restartTurn()");
        pushEvent(() -> {
            HandManager current = current();
            current.endTurn();
            current.startTurn(remote.count());
        });
    }

    private static void endGame() {
        System.out.println("endGame()");
        pushEvent(() -> gameOverTimer = System.currentTimeMillis()+5000);
    }

    private static void finishTurn() {
        if (hasEvent()) {
            throw new IllegalStateException("error: finishing turn before all events handled");
        }
        if (isTurn) {
            player.endTurn();
            isTurn = false;
            remote.startTurn(player.count());
        } else {
            remote.endTurn();
            isTurn = true;
            player.startTurn(remote.count());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        synchronized (this) {
            if (currentEvent == null) {
                int x = e.getX();
                int y = e.getY();
                if (menu != null) {
                    menu.click(x, y);
                } else if (drawPileLocation != null && player.isTurn) {
                    for (int cc = playerSortedIndices.size() - 1; cc >= 0; cc--) {
                        int c = playerSortedIndices.get(cc);
                        CardObject cardObject = player.hand.get(c);
                        if (cardObject.inBounds(x, y)) {
                            UnoCard card = cardObject.getCard();
                            if (card.canPlayOn(topOfDeck.getCard())) {
                                if (card instanceof WildCard) {
                                    setMenu(new ColorSelectMenu((WildCard) card, c));
                                } else {
                                    playCard(c);
                                }
                            }
                            return;
                        }
                    }
                    if (CardObject.pointInBounds(drawPileLocation, x, y)) {
                        drawCard();
                    }
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
