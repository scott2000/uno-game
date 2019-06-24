package display;

import card.CardGraphics;
import card.CardObject;
import card.UnoCard;
import card.WildCard;
import manager.DeckManager;
import manager.HandManager;
import manager.OpponentManager;
import manager.local.PlayerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

public class UnoPanel extends JPanel implements MouseListener {
    private static final long REFRESH_DELAY = 5;

    public static final Random random = new Random();

    public static int width;
    public static int height;

    private static List<UnoCard> discard = new ArrayList<>();

    private static UnoMenu menu;

    private static CardObject topOfDeck;
    private static boolean isInitialized = false;

    private static long gameOverTimer;
    private static boolean firstTurnEnded;
    private static boolean gameRunning;

    private static Point drawPileLocation;
    private static Point topOfDeckLocation;

    private static ArrayDeque<Event> eventQueue = new ArrayDeque<>();
    private static Event currentEvent;

    private static PlayerManager player = new PlayerManager();
    private static OpponentManager opponent;

    private static UnoPanel instance;

    UnoPanel(OpponentManager opponent) {
        UnoPanel.opponent = opponent;
        addMouseListener(this);
        setBackground(Color.WHITE);
        instance = this;
    }

    void onClose() {
        if (opponent != null) {
            opponent.onClose();
        }
    }

    private void setup() {
        reset();
        Thread mainLoop = new Thread(() -> {
            try {
                long lastTime = System.currentTimeMillis();
                while (true) {
                    synchronized (this) {
                        long currentTime = System.currentTimeMillis();
                        long time = currentTime-lastTime;
                        lastTime = currentTime;

                        updateWHD(false);

                        if (time != 0) {
                            opponent.update(time);
                            player.update(time);

                            if (topOfDeck != null) {
                                topOfDeck.update(topOfDeckLocation.x, topOfDeckLocation.y, true, time);
                            }
                        }

                        if (gameOverTimer == 0) {
                            if (currentEvent != null && currentEvent.isDone()) {
                                currentEvent = null;
                                if (eventQueue.isEmpty() && menu == null && firstTurnEnded) {
                                    opponent.noEventsInQueue();
                                }
                            }
                            if (currentEvent == null && !eventQueue.isEmpty()) {
                                currentEvent = eventQueue.removeFirst();
                                currentEvent.start();
                            }
                        } else if (currentTime >= gameOverTimer) {
                            reset();
                        }

                        if (time != 0) {
                            repaint(0);
                        }
                    }
                    Thread.sleep(REFRESH_DELAY);
                }
            } catch (InterruptedException ignored) {}
        });
        mainLoop.setName("mainLoop");
        mainLoop.start();
    }

    private void reset() {
        discard.clear();
        eventQueue.clear();
        topOfDeck = null;
        gameOverTimer = 0;
        updateWHD(false);
        player.reset();
        opponent.reset();
    }

    private static HandManager current() {
        return player.isTurn ? player : opponent;
    }

    public static void pushEvent(Event event) {
        eventQueue.addLast(event);
    }

    public static boolean hasEvent() {
        return !eventQueue.isEmpty();
    }

    public static void setMenu(UnoMenu menu) {
        UnoPanel.menu = menu;
    }

    public static boolean isGameRunning() {
        return gameRunning;
    }

    public static boolean isGameOver() {
        return gameOverTimer != 0;
    }

    public static UnoCard getTopOfDeck() {
        return topOfDeck.getCard();
    }

    public static UnoPanel getInstance() {
        return instance;
    }

    public static int getTimeToNewGame() {
        if (gameOverTimer == 0) {
            return 0;
        } else {
            return Math.max(0, (int) (gameOverTimer-System.currentTimeMillis()+999)/1000);
        }
    }

    public static List<UnoCard> takeDiscardPile() {
        List<UnoCard> oldDiscard = discard;
        discard = new ArrayList<>();
        return oldDiscard;
    }

    public static List<UnoCard> newDeckFromDiscard() {
        if (discard.isEmpty()) {
            return UnoCard.newDeck();
        } else {
            List<UnoCard> deck = takeDiscardPile();
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
        if (!isInitialized) {
            setup();
            isInitialized = true;
        }

        setHints(g);
        updateWHD(true);

        int dy = drawPileLocation.y;
        int dx = width/2+5;
        for (int i = Math.max(0, discard.size()-4); i < discard.size(); i++) {
            CardGraphics.paint(g, discard.get(i), false, dx, dy, 1.0);
            dx += 1;
            dy -= 1;
        }

        int shownCards = Math.min(4, opponent.cardsInDeck());
        for (int i = 0; i < shownCards; i++) {
            CardGraphics.paintBlank(g, drawPileLocation.x, drawPileLocation.y);
            drawPileLocation.x += 1;
            drawPileLocation.y -= 1;
        }
        CardGraphics.paintBlank(g, drawPileLocation.x, drawPileLocation.y);

        if (topOfDeck != null) {
            topOfDeck.paint(g, false);
        }

        opponent.paint(g);
        player.paint(g);

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
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        setAntialias(g);
    }

    public static void setAntialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public static void setNoAntialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
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

    public static void saveState(List<String> saveData) {
        saveData.add(topOfDeck.getCard().encode());
        saveData.add(DeckManager.saveCards(discard));
        saveData.add(DeckManager.saveCardObjects(player.hand));
        saveData.add(DeckManager.saveCardObjects(opponent.hand));
        saveData.add(player.isTurn ? "1" : "0");
    }

    public static void loadState(List<String> loadData) {
        UnoCard topOfDeck = UnoCard.decode(loadData.get(1));
        List<UnoCard> discard = new ArrayList<>(Arrays.asList(DeckManager.loadCards(loadData.get(2))));
        UnoCard[] playerHand = DeckManager.loadCards(loadData.get(3));
        UnoCard[] opponentHand = DeckManager.loadCards(loadData.get(4));
        boolean playerWillStart = loadData.get(5).equals("1");
        restore(topOfDeck, discard, playerHand, opponentHand, playerWillStart);
    }

    public static void delay(long time) {
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
        final UnoCard card;
        if (player.isTurn) {
            opponent.playerDrawCard();
            card = opponent.drawVisibleCard();
        } else {
            card = opponent.drawHiddenCard();
        }
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
        if (player.isTurn) {
            opponent.playerPlayCard(c, player.hand.get(c).getCard());
        }
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
                        if (player.isTurn) {
                            drawCardTo(opponent.drawHiddenCard(), opponent);
                        } else {
                            drawCardTo(opponent.drawVisibleCard(), player);
                        }
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

    public static void newGame(UnoCard topOfDeck, UnoCard[] playerHand, UnoCard[] opponentHand) {
        opponent.newGame(topOfDeck, opponentHand);
        final boolean playerWillStart = opponent.playerCanStart();
        for (int i = 0; i < DeckManager.CARDS_PER_HAND; i++) {
            if (playerWillStart) {
                drawCardTo(opponentHand[i], opponent);
                drawCardTo(playerHand[i], player);
            } else {
                drawCardTo(playerHand[i], player);
                drawCardTo(opponentHand[i], opponent);
            }
        }
        pushEvent(new Event() {
            @Override
            public void start() {
                UnoPanel.topOfDeck = new CardObject();
                UnoPanel.topOfDeck.setCard(topOfDeck);
                UnoPanel.topOfDeck.setPosition(drawPileLocation.x, drawPileLocation.y);
                UnoPanel.topOfDeck.startAnimating();
            }

            @Override
            public boolean isDone() {
                return UnoPanel.topOfDeck.doneAnimating();
            }
        });
        pushEvent(() -> startGame(playerWillStart));
    }

    public static void restore(UnoCard topOfDeck, List<UnoCard> discard, UnoCard[] playerHand, UnoCard[] opponentHand, boolean playerWillStart) {
        opponent.restore(topOfDeck, opponentHand, playerHand.length, discard, playerWillStart);
        UnoPanel.topOfDeck = new CardObject();
        UnoPanel.topOfDeck.setCard(topOfDeck);
        UnoPanel.discard = discard;
        for (UnoCard card : playerHand) {
            CardObject cardObject = new CardObject();
            cardObject.setCard(card);
            cardObject.setFlipped(true);
            player.addCard(cardObject, false);
        }
        for (UnoCard card : opponentHand) {
            CardObject cardObject = new CardObject();
            cardObject.setCard(card);
            opponent.addCard(cardObject, false);
        }
        startGame(playerWillStart);
    }

    public static void finishTurnEarly() {
        if (player.isTurn) {
            opponent.playerFinishTurnEarly();
        }
        pushEvent(UnoPanel::finishTurn);
    }

    public static void requestRestore() {
        pushEvent(() -> {
            int count = opponent.count();
            UnoCard[] opponentCards = new UnoCard[count];
            for (int i = 0; i < count; i++) {
                opponentCards[i] = opponent.hand.get(i).getCard();
            }
            opponent.restore(topOfDeck.getCard(), opponentCards, player.count(), discard, player.isTurn);
        });
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
        target.addCard(cardObject, true);
        return cardObject;
    }

    private static void restartTurn() {
        pushEvent(() -> {
            HandManager current = current();
            current.endTurn();
            current.startTurn(opponent.count());
        });
    }

    private static void endGame() {
        pushEvent(() -> {
            gameOverTimer = System.currentTimeMillis()+10_000;
            gameRunning = false;
            firstTurnEnded = false;
            opponent.gameOver();
            opponent.reveal(player.hand);
        });
    }

    private static void startGame(boolean playerWillStart) {
        gameRunning = true;
        if (playerWillStart) {
            player.startTurn(opponent.count());
        } else {
            opponent.startTurn(player.count());
        }
    }

    private static void finishTurn() {
        firstTurnEnded = true;
        if (player.isTurn) {
            player.endTurn();
            opponent.startTurn(player.count());
        } else {
            opponent.endTurn();
            player.startTurn(opponent.count());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        synchronized (this) {
            if (currentEvent == null) {
                pushEvent(() -> {
                    int x = e.getX();
                    int y = e.getY();
                    if (menu != null) {
                        menu.click(x, y);
                    } else if (drawPileLocation != null && player.isTurn) {
                        player.click(x, y, drawPileLocation);
                    }
                });
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
