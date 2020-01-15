package display;

import card.CardGraphics;
import card.CardObject;
import card.UnoCard;
import card.WildCard;
import manager.DeckManager;
import manager.HandManager;
import manager.OpponentManager;
import manager.local.PlayerManager;
import menu.UnoMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

public final class UnoPanel extends JPanel implements MouseListener, KeyListener {
    private interface Event {
        void start();
        boolean isDone();
    }

    private static final long REFRESH_FREQUENCY = 8;
    private static final long CIRCLE_EXPAND_TIME = 250;

    private static final int CIRCLE_INITIAL_RADIUS = CardGraphics.WIDTH;
    private static final int CIRCLE_EXPAND_RADIUS = 100;

    private static final int END_TURN_WIDTH = CardGraphics.WIDTH;
    private static final int END_TURN_HEIGHT = 40;

    public static final Random RANDOM = new Random();

    public static int width;
    public static int height;

    private static List<UnoCard> discard = new ArrayList<>();

    private static UnoMenu menu = null;
    private static Chat chat = null;

    private static CardObject topOfDeck = null;
    private static long gameOverTimer = 0;

    private static Point drawPileLocation;
    private static Point topOfDeckLocation;
    private static Point endTurnButton;

    private static ArrayDeque<Event> eventQueue = new ArrayDeque<>();
    private static Event currentEvent = null;

    private static PlayerManager player = new PlayerManager();
    private static OpponentManager opponent;

    private static float circleOpacityRoot = 0.0f;
    private static Color circleColor;

    private static boolean hasDrawn;
    private static long endRecommendedTime = 0;
    private static int peekStage = 0;

    private static boolean repaintAll;

    private boolean isInitialized = false;

    UnoPanel() {
        addMouseListener(this);
        setBackground(Color.WHITE);
    }

    void onClose() {
        if (opponent != null) {
            opponent.onClose();
        }
    }

    private void startMainLoop() {
        Thread mainLoop = new Thread(() -> {
            long lastTime = System.currentTimeMillis();
            while (true) {
                try {
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

                            if (circleColor != null) {
                                circleOpacityRoot -= (float) time / CIRCLE_EXPAND_TIME;
                                if (circleOpacityRoot <= 0.0f) {
                                    circleColor = null;
                                }
                                int radius = CIRCLE_EXPAND_RADIUS + HandManager.MARGIN;
                                repaint( 0, topOfDeckLocation.x-radius, topOfDeckLocation.y-radius, 2*radius, 2*radius);
                            }

                            if (chat != null && chat.needsRepaint()) {
                                repaint(0, 0, HandManager.MARGIN, drawPileLocation.x, height-HandManager.MARGIN*2);
                            }
                        }

                        if (gameOverTimer == 0) {
                            while (true) {
                                if (currentEvent != null) {
                                    if (currentEvent.isDone()) {
                                        currentEvent = null;
                                        shouldRepaintAll();
                                    } else {
                                        break;
                                    }
                                }
                                if (eventQueue.isEmpty()) {
                                    break;
                                }
                                currentEvent = eventQueue.removeFirst();
                                ArrayDeque<Event> oldQueue = eventQueue;
                                eventQueue = new ArrayDeque<>();
                                currentEvent.start();
                                if (eventQueue.isEmpty()) {
                                    eventQueue = oldQueue;
                                } else {
                                    for (Event event : oldQueue) {
                                        eventQueue.addLast(event);
                                    }
                                }
                                shouldRepaintAll();
                            }
                            // if the event took a really long time (more than 10 times the normal frequency), ignore it
                            currentTime = System.currentTimeMillis();
                            if (currentTime-lastTime > 10*REFRESH_FREQUENCY) {
                                lastTime = currentTime;
                            }
                        } else if (currentTime >= gameOverTimer && gameOverTimer != -1) {
                            if (opponent.fastReset()) {
                                reset();
                            } else {
                                gameOverTimer = -1;
                                Thread resetThread = new Thread(() -> {
                                    opponent.willReset();
                                    synchronized (this) {
                                        reset();
                                    }
                                });
                                resetThread.setName("resetThread");
                                resetThread.start();
                            }
                        }

                        if (gameOverTimer != 0) {
                            repaint(0);
                        } else if (repaintAll) {
                            repaintAll = false;
                            repaint(0);
                        } else if (endTurnButton != null) {
                            repaint(0,
                                    endTurnButton.x - HandManager.MARGIN,
                                    endTurnButton.y - HandManager.MARGIN,
                                    END_TURN_WIDTH + HandManager.MARGIN*2,
                                    END_TURN_HEIGHT + HandManager.MARGIN*2);
                        }
                        repaint(0);
                    }
                    Thread.sleep(REFRESH_FREQUENCY);
                } catch (InterruptedException ignored) {}
            }
        });
        mainLoop.setName("mainLoop");
        mainLoop.start();
    }

    private void reset() {
        discard.clear();
        eventQueue.clear();
        topOfDeck = null;
        gameOverTimer = 0;
        hasDrawn = false;
        player.reset();
        opponent.reset();
        shouldRepaintAll();
    }

    private static void shouldRepaintAll() {
        repaintAll = true;
    }

    static void setChatName(String name) {
        chat = new Chat(name);
    }

    static void sendMessage(String message) {
        opponent.chat(message);
    }

    public static Chat getChat() {
        return chat;
    }

    static void setOpponent(OpponentManager opponent) {
        UnoPanel.opponent = opponent;
        opponent.willReset();
        opponent.reset();
    }

    private static HandManager current() {
        return player.isTurn ? player : opponent;
    }

    private static HandManager other() {
        return player.isTurn ? opponent : player;
    }

    private static void pushEvent(Event event) {
        eventQueue.addLast(event);
    }

    public static void pushEvent(String kind, Runnable event) {
        pushEvent(new Event() {
            @Override
            public void start() {
                event.run();
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public String toString() {
                return kind;
            }
        });
    }

    public static boolean hasEventInQueue() {
        return !eventQueue.isEmpty();
    }

    public static void setMenu(UnoMenu menu) {
        UnoPanel.menu = menu;
        shouldRepaintAll();
    }

    public static boolean isPeeking() {
        return peekStage == 5;
    }

    public static boolean isGameOver() {
        return gameOverTimer != 0;
    }

    public static boolean canDraw() {
        return !hasDrawn;
    }

    public static UnoCard getTopOfDeck() {
        return topOfDeck.getCard();
    }

    public static int getTimeToNewGame() {
        if (gameOverTimer == 0) {
            return -1;
        } else {
            return Math.max(0, (int) ((gameOverTimer-System.currentTimeMillis()+999)/1000));
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
            Collections.shuffle(deck, RANDOM);
            return deck;
        }
    }

    public static float getHighlightDist(long time) {
        if (time == 0) {
            return 0.0f;
        } else {
            return (float) (2 - 1.5f*Math.cos((System.currentTimeMillis()-time)%2000/1000.0*Math.PI));
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        displayUno((Graphics2D) g);
    }

    private void updateWHD(boolean willDisplay) {
        int newWidth = getWidth();
        int newHeight = getHeight();
        if (newWidth != width || newHeight != height) {
            width = newWidth;
            height = newHeight;
            shouldRepaintAll();
        }
        int dy = (height-CardGraphics.HEIGHT)/2+2;
        drawPileLocation = new Point((width-2*CardGraphics.WIDTH)/2-5, dy);
        topOfDeckLocation = new Point(width/2+5, dy);
        if (!willDisplay) {
            int s = Math.min(4, opponent.cardsInDeck());
            drawPileLocation.x += s;
            drawPileLocation.y -= s;
            s = Math.min(4, discard.size());
            topOfDeckLocation.x += s;
            topOfDeckLocation.y -= s;
        }
    }

    private synchronized void displayUno(Graphics2D g) {
        if (!isInitialized) {
            isInitialized = true;
            startMainLoop();
        }

        setHints(g);
        updateWHD(true);

        for (int i = Math.max(0, discard.size()-4); i < discard.size(); i++) {
            CardGraphics.paint(g, discard.get(i), false, 0, topOfDeckLocation.x, topOfDeckLocation.y, 1.0);
            topOfDeckLocation.x += 1;
            topOfDeckLocation.y -= 1;
        }

        int drawPileStartX = drawPileLocation.x;
        for (int i = 0, shownCards = Math.min(4, opponent.cardsInDeck()); i < shownCards; i++) {
            CardGraphics.paintBlank(g, drawPileLocation.x, drawPileLocation.y);
            drawPileLocation.x += 1;
            drawPileLocation.y -= 1;
        }
        CardGraphics.paintBlank(g, drawPileLocation.x, drawPileLocation.y);

        if (player.isTurn && hasDrawn && !hasEventInQueue()) {
            Color color = topOfDeck.getCard().getColor();
            endTurnButton = new Point(topOfDeckLocation.x + CardGraphics.WIDTH + HandManager.MARGIN, (height - END_TURN_HEIGHT)/2);
            if (endRecommendedTime != 0) {
                float dist = getHighlightDist(endRecommendedTime);
                RoundRectangle2D rect = new RoundRectangle2D.Double(
                        endTurnButton.x-dist,
                        endTurnButton.y-dist,
                        END_TURN_WIDTH+dist*2+1,
                        END_TURN_HEIGHT+dist*2+1,
                        CardGraphics.HIGHLIGHT_ARC,
                        CardGraphics.HIGHLIGHT_ARC);
                g.setColor(withAlpha(color, 0.5f));
                g.fill(rect);
            }
            g.setColor(color);
            g.fillRoundRect(endTurnButton.x, endTurnButton.y, END_TURN_WIDTH, END_TURN_HEIGHT, CardGraphics.ARC, CardGraphics.ARC);
            g.setColor(Color.BLACK);
            Stroke stroke = g.getStroke();
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(endTurnButton.x, endTurnButton.y, END_TURN_WIDTH, END_TURN_HEIGHT, CardGraphics.ARC, CardGraphics.ARC);
            g.setStroke(stroke);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            shadowTextCenter(g, "End Turn", endTurnButton.x + END_TURN_WIDTH/2.0f, endTurnButton.y + END_TURN_HEIGHT/2.0f);
        }

        if (circleColor != null) {
            float opacity = circleOpacityRoot*circleOpacityRoot;
            g.setColor(withAlpha(circleColor, opacity));
            float centerX = topOfDeckLocation.x + CardGraphics.WIDTH/2f;
            float centerY = topOfDeckLocation.y + CardGraphics.HEIGHT/2f;
            float radius = CIRCLE_EXPAND_RADIUS + opacity*(CIRCLE_INITIAL_RADIUS - CIRCLE_EXPAND_RADIUS);
            float diameter = 2*radius;
            g.fill(new Ellipse2D.Float(
                    centerX-radius,
                    centerY-radius,
                    diameter,
                    diameter));
        }

        if (topOfDeck != null) {
            topOfDeck.paint(g, false);
        }

        int minY = opponent.paint(g);
        int maxY = player.paint(g);

        if (chat != null) {
            chat.paint(g, drawPileStartX, minY, maxY);
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
        shadowText(g, text, x2, y2, 1.0f);
    }

    public static void shadowText(Graphics2D g, String text, float x, float y) {
        shadowText(g, text, x, y, 1.0f);
    }

    static void shadowText(Graphics2D g, String text, float x, float y, float diff) {
        Color c = g.getColor();
        g.setColor(Color.DARK_GRAY);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                g.drawString(text, x + a*diff, y + b*diff);
            }
        }
        g.setColor(c);
        g.drawString(text, x, y);
    }

    public static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha*color.getAlpha()));
    }

    private static boolean playerShouldEndTurn() {
        return !player.shouldPlayDrawnCard();
    }

    public static void saveState(List<String> saveData) {
        saveData.add(topOfDeck.getCard().encode());
        saveData.add(DeckManager.saveCards(discard));
        saveData.add(DeckManager.saveCardObjects(player.hand));
        saveData.add(DeckManager.saveCardObjects(opponent.hand));
        saveData.add(player.isTurn ? "1" : "0");
        saveData.add(hasDrawn ? "1" : "0");
    }

    public static void loadState(List<String> loadData) {
        UnoCard topOfDeck = UnoCard.decode(loadData.get(2));
        List<UnoCard> discard = new ArrayList<>(Arrays.asList(DeckManager.loadCards(loadData.get(3))));
        UnoCard[] playerHand = DeckManager.loadCards(loadData.get(4));
        UnoCard[] opponentHand = DeckManager.loadCards(loadData.get(5));
        boolean playerWillStart = loadData.get(6).equals("1");
        boolean hasDrawn = loadData.get(7).equals("1");
        restore(topOfDeck, discard, playerHand, opponentHand, playerWillStart, hasDrawn);
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

            @Override
            public String toString() {
                return "delay("+time+")";
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
                hasDrawn = true;
                cardObject = drawCardDirectlyTo(card, current());
                endRecommendedTime = player.isTurn && playerShouldEndTurn() ? System.currentTimeMillis() : 0;
                opponent.canSave();
                shouldRepaintAll();
            }

            @Override
            public boolean isDone() {
                return cardObject.doneAnimating();
            }

            @Override
            public String toString() {
                return "drawCard";
            }
        });
    }

    public static void playCard(int c) {
        if (player.isTurn) {
            opponent.playerPlayCard(c, player.hand.get(c).getCard());
        }
        pushEvent(waitForCircle());
        pushEvent(new Event() {
            @Override
            public void start() {
                UnoCard oldTopOfDeck = topOfDeck.getCard();
                discard.add(oldTopOfDeck);
                HandManager target = current();
                topOfDeck = target.removeCard(c, true);
                UnoCard card = topOfDeck.getCard();
                if (target.count() == 0) {
                    startCircle();
                    changeChatColor();
                    endGame();
                    return;
                } else if (card.getColorCode() != oldTopOfDeck.getColorCode()) {
                    startCircle();
                    changeChatColor();
                }
                if (card.isSkip()) {
                    for (int i = 0, cardDraws = card.cardDraws(); i < cardDraws; i++) {
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
            }

            @Override
            public boolean isDone() {
                return topOfDeck.doneAnimating();
            }

            @Override
            public String toString() {
                return "playCard("+c+")";
            }
        });
    }

    public static void newGame(UnoCard topOfDeck, UnoCard[] playerHand, UnoCard[] opponentHand) {
        opponent.newGame(topOfDeck, opponentHand);
        final boolean playerWillStart = opponent.playerCanStart();
        delay(250);
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

            @Override
            public String toString() {
                return "flipOverCard("+topOfDeck+")";
            }
        });
        pushEvent("startGame", () -> {
            startCircle();
            changeChatColor();
            startGame(playerWillStart);
        });
    }

    public static void restore(UnoCard topOfDeck, List<UnoCard> discard, UnoCard[] playerHand, UnoCard[] opponentHand, boolean playerWillStart, boolean hasDrawn) {
        opponent.restore(topOfDeck, opponentHand, playerHand.length, discard, playerWillStart, hasDrawn);
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
        if (chat != null) {
            chat.setColor(topOfDeck.getTextColor());
        }
        endRecommendedTime = playerWillStart && hasDrawn && playerShouldEndTurn() ? System.currentTimeMillis() : 0;
        UnoPanel.hasDrawn = hasDrawn;
        startGame(playerWillStart);
        shouldRepaintAll();
    }

    public static void finishTurnEarly() {
        if (player.isTurn) {
            opponent.playerFinishTurnEarly();
        }
        pushEvent("finishTurn", UnoPanel::finishTurn);
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

            @Override
            public String toString() {
                return (target instanceof PlayerManager ? "player" : "opponent")+".drawCard"+(card == null ? "" : card);
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

    private static Event waitForCircle() {
        return new Event() {
            @Override
            public void start() {}

            @Override
            public boolean isDone() {
                return circleOpacityRoot < 0.5f;
            }

            @Override
            public String toString() {
                return "waitForCircle";
            }
        };
    }

    private static void startCircle() {
        pushEvent("startCircle", () -> {
            circleOpacityRoot = 1.0f;
            circleColor = topOfDeck.getCard().getCircleColor();
        });
    }

    private static void changeChatColor() {
        if (chat != null) {
            pushEvent("changeChatColor", () -> chat.changeColor(topOfDeck.getCard().getTextColor()));
        }
    }

    private static void restartTurn() {
        pushEvent("restartTurn", () -> {
            HandManager current = current();
            current.endTurn();
            hasDrawn = false;
            current.startTurn(other().count());
            opponent.canSave();
        });
    }

    private static void endGame() {
        pushEvent("endGame", () -> {
            gameOverTimer = System.currentTimeMillis()+10_000;
            opponent.gameOver();
            opponent.reveal(player.hand);
        });
    }

    private static void startGame(boolean playerWillStart) {
        if (playerWillStart) {
            player.startTurn(opponent.count());
        } else {
            opponent.startTurn(player.count());
        }
        shouldRepaintAll();
    }

    private static void finishTurn() {
        if (player.isTurn) {
            if (peekStage != 0) {
                peekStage = 0;
                for (CardObject card : opponent.hand) {
                    card.startAnimating();
                }
            }
            player.endTurn();
            hasDrawn = false;
            opponent.startTurn(player.count());
        } else {
            opponent.endTurn();
            hasDrawn = false;
            player.startTurn(opponent.count());
        }
        opponent.canSave();
        shouldRepaintAll();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        synchronized (this) {
            if (!hasEventInQueue()) {
                int x = e.getX();
                int y = e.getY();
                pushEvent("click("+x+", "+y+")", () -> {
                    if (menu != null) {
                        if (!menu.click(x, y)) {
                            menu = null;
                        }
                    } else if (drawPileLocation != null && player.isTurn) {
                        if (!player.click(x, y, drawPileLocation) && hasDrawn && endTurnButton != null
                                && x >= endTurnButton.x && x <= endTurnButton.x+END_TURN_WIDTH
                                && y >= endTurnButton.y && y <= endTurnButton.y+END_TURN_HEIGHT) {
                            finishTurnEarly();
                        }
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

    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (!Character.isISOControl(c)) {
            if (chat == null) {
                if (!player.isTurn || hasEventInQueue()) {
                    return;
                }
                char required;
                switch (peekStage) {
                    case 0:
                        required = 'P';
                        break;
                    case 1:
                    case 2:
                        required = 'E';
                        break;
                    case 3:
                        required = 'K';
                        break;
                    case 4:
                        required = '!';
                        break;
                    default:
                        return;
                }
                if (c == required) {
                    peekStage++;
                    if (isPeeking()) {
                        for (CardObject card : opponent.hand) {
                            card.startAnimating();
                        }
                        drawCard();
                        drawCard();
                    }
                } else {
                    peekStage = 0;
                }
            } else {
                chat.type(c);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && menu != null) {
            menu = null;
        } else if (chat != null) {
            chat.press(e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
