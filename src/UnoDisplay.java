import sun.plugin.dom.exception.InvalidStateException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

class UnoDisplay extends JPanel implements MouseListener {
    private final int STATE_PLAYER_MOVE = 0;
    private final int STATE_SELECT_COLOR = 1;
    private final int STATE_PLAY_OR_KEEP = 2;
    private final int STATE_COMPUTER_MOVE = 3;
    private final int STATE_WIN = 4;
    private final int STATE_LOSE = 5;

    private final int CARD_WIDTH = 75;
    private final int CARD_HEIGHT = 105;
    private final int CARD_ARC = 7;
    private final int CARD_SEP_X = CARD_WIDTH + 5;
    private final int CARD_SEP_Y_COMPUTER = CARD_HEIGHT/3;
    private final int CARD_SEP_Y_PLAYER = CARD_HEIGHT*2/3;

    private final int COLOR_SELECT_BUTTON_SIZE = 75;
    private final int PLAY_KEEP_WIDTH = 75;
    private final int PLAY_KEEP_HEIGHT = 50;

    private ArrayList<UnoCard> deck;
    private ArrayList<UnoCard> computerHand;
    private ArrayList<UnoCard> playerHand;

    private ArrayList<UnoCard> discard;

    private UnoCard topOfDeck;

    private Random random = new Random();

    private int state = STATE_PLAYER_MOVE;
    private long stateTimer = 0;

    private UnoCard intermediateStateCard;
    Point[] intermediateButtons;

    int width;
    int height;
    int cardColumns;
    int playerRows;
    int cardIndentComputer;
    int cardIndentPlayer;

    Point drawPile;

    UnoDisplay() {
        addMouseListener(this);
        reset();
        new Thread(() -> {
            while (true) {
                long currentTime = System.currentTimeMillis();
                if (stateTimer > 0 && currentTime >= stateTimer) {
                    synchronized (this) {
                        stateTimer = 0;
                        switch (state) {
                            case STATE_COMPUTER_MOVE:
                                computerMove();
                                break;
                            case STATE_WIN:
                            case STATE_LOSE:
                                reset();
                                state = STATE_PLAYER_MOVE;
                                break;
                            default:
                                throw new InvalidStateException("error: unexpected state with timer");
                        }
                        repaint();
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
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
        computerHand = new ArrayList<>();
        playerHand = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            computerHand.add(drawCard());
            playerHand.add(drawCard());
        }
        for (int i = deck.size()-1; ; i--) {
            if (deck.get(i).isNumeric()) {
                topOfDeck = deck.remove(i);
                break;
            }
        }
        discard = new ArrayList<>();
    }

    private void playCard(UnoCard card) {
        discard.add(topOfDeck);
        topOfDeck = card;
    }

    private UnoCard drawCard() {
        int size = deck.size();
        // If the deck is empty, use discarded cards
        if (size == 0) {
            size = discard.size();
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

    private boolean canPlay(UnoCard card) {
        return card.canPlay(topOfDeck.getColorCode(), topOfDeck.getNumberCode());
    }

    /*
     * Play a numeric card with the same color
     * Play any card with the same color
     * Play a matching card with the best possible color
     * Play a wild card (or maybe not)
     * Draw a card and play it if possible
     */
    private void computerMove() {
        int playable = -1;
        int nonWild = -1;
        int matchColor = -1;
        int[] colorCounts = new int[4];
        for (UnoCard card : computerHand) {
            if (card instanceof NormalCard) {
                colorCounts[card.getColorCode()] += 1;
            }
        }
        int colorOffset = random.nextInt(4);
        int maxColor = 0;
        int bestColor = colorOffset;
        for (int i = colorOffset; i < 4+colorOffset; i++) {
            int index = i%4;
            int c = colorCounts[index];
            if (c > maxColor) {
                maxColor = c;
                bestColor = index;
            }
        }
        int nonWildCount = 0;
        int targetColor = topOfDeck.getColorCode();
        for (int c = computerHand.size()-1; c >= 0; c--) {
            UnoCard card = computerHand.get(c);
            if (canPlay(card)) {
                playable = c;
                if (card instanceof NormalCard) {
                    int color = card.getColorCode();
                    int count = colorCounts[color];
                    if (count > nonWildCount) {
                        nonWild = c;
                        nonWildCount = count;
                    }
                    if (color == targetColor) {
                        matchColor = c;
                        if (card.isNumeric()) {
                            break;
                        }
                    }
                }
            }
        }
        UnoCard card;
        if (matchColor != -1) {
            card = computerHand.remove(matchColor);
        } else if (nonWild != -1) {
            card = computerHand.remove(nonWild);
        } else if (playable != -1 && (computerHand.size() <= 3 || playerHand.size() <= 3 || random.nextInt(4) == 0)) {
            WildCard wild = (WildCard) computerHand.remove(playable);
            wild.setColor(bestColor);
            card = wild;
        } else {
            UnoCard drawnCard = drawCard();
            if (canPlay(drawnCard)) {
                if (drawnCard instanceof WildCard) {
                    ((WildCard) drawnCard).setColor(bestColor);
                }
                if (drawnCard.isSkip()) {
                    int cardsDrawn = drawnCard.cardDraws();
                    for (int i = 0; i < cardsDrawn; i++) {
                        playerHand.add(drawCard());
                    }
                    stateTimer = System.currentTimeMillis()+1000;
                } else {
                    playCard(drawnCard);
                    state = STATE_PLAYER_MOVE;
                }
            } else {
                computerHand.add(drawnCard);
                state = STATE_PLAYER_MOVE;
            }
            return;
        }
        playCard(card);
        if (card.isSkip()) {
            stateTimer = System.currentTimeMillis()+1000;
            int cardsDrawn = card.cardDraws();
            for (int i = 0; i < cardsDrawn; i++) {
                playerHand.add(drawCard());
            }
        } else {
            state = STATE_PLAYER_MOVE;
        }
        if (computerHand.isEmpty()) {
            state = STATE_LOSE;
            stateTimer = System.currentTimeMillis()+5000;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        displayUno((Graphics2D) g);
    }

    private synchronized void displayUno(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Dimension size = getSize();
        width = (int) size.getWidth();
        height = (int) size.getHeight();
        cardColumns = Math.max((width-15)/CARD_SEP_X, 3);
        playerRows = (playerHand.size()-1)/cardColumns;
        cardIndentComputer = (width - Math.min(cardColumns, computerHand.size())*CARD_SEP_X + 5)/2;
        cardIndentPlayer = (width - Math.min(cardColumns, playerHand.size())*CARD_SEP_X + 5)/2;

        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, width, height);

        for (int card = 0; card < computerHand.size(); card++) {
            paintCard(g, getComputerCardPoint(card));
        }

        int dy = (height-CARD_HEIGHT)/2+2;
        drawPile = new Point((width-2*CARD_WIDTH)/2-5, dy);
        for (int i = 0; i < deck.size()*4/107; i++) {
            paintCard(g, drawPile);
            drawPile.x += 1;
            drawPile.y -= 1;
        }
        paintCard(g, drawPile);

        int dx = width/2+5;
        for (int i = Math.max(0, discard.size()-4); i < discard.size(); i++) {
            paintCard(g, new Point(dx, dy), discard.get(i));
            dx += 1;
            dy -= 1;
        }
        paintCard(g, new Point(dx, dy), topOfDeck);

        if (state == STATE_PLAYER_MOVE || state == STATE_SELECT_COLOR || state == STATE_PLAY_OR_KEEP) {
            g.setColor(topOfDeck.getColor());
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            drawShadowTextCenter(g, "Your Turn", width/2, height - CARD_SEP_Y_PLAYER*playerRows - CARD_HEIGHT - 30);
        }

        for (int card = 0; card < playerHand.size(); card++) {
            paintCard(g, getPlayerCardPoint(card), playerHand.get(card));
        }

        switch (state) {
        case STATE_SELECT_COLOR:
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.75f));
            g.fillRect(width/2-200, height/2-100, 400, 200);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            drawShadowTextCenter(g, "Select a color for the wild card:", width/2, height/2-80);
            int xl = width/2-COLOR_SELECT_BUTTON_SIZE;
            int xr = width/2;
            int yt = height/2-COLOR_SELECT_BUTTON_SIZE+15;
            int yb = height/2+15;
            intermediateButtons = new Point[] {
                    new Point(xl, yt),
                    new Point(xl, yb),
                    new Point(xr, yb),
                    new Point(xr, yt),
            };
            g.clip(new Ellipse2D.Double(xl, yt,COLOR_SELECT_BUTTON_SIZE*2, COLOR_SELECT_BUTTON_SIZE*2));
            for (int color = 0; color < 4; color++) {
                paintColorSelectButton(g, intermediateButtons[color], color);
            }
            g.setClip(null);
            g.setColor(Color.BLACK);
            g.drawOval(xl, yt, COLOR_SELECT_BUTTON_SIZE*2, COLOR_SELECT_BUTTON_SIZE*2);
            break;
        case STATE_PLAY_OR_KEEP:
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.75f));
            g.fillRect(width/2-200, height/2-100, 400, 200);
            paintCard(g, new Point(width/2-175, height/2-CARD_HEIGHT/2), intermediateStateCard);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            drawShadowTextCenter(g, "Do you want to play or keep this card?", width/2, height/2-80);
            intermediateButtons = new Point[] {
                    new Point(width/2-PLAY_KEEP_WIDTH/2, height/2-PLAY_KEEP_HEIGHT/2),
                    new Point(width/2+10+PLAY_KEEP_WIDTH, height/2-PLAY_KEEP_HEIGHT/2),
            };
            for (Point p : intermediateButtons) {
                g.setColor(Color.WHITE);
                g.fillRect(p.x-1, p.y-1, PLAY_KEEP_WIDTH+2, PLAY_KEEP_HEIGHT+2);
                g.setColor(Color.BLACK);
                g.fillRect(p.x, p.y, PLAY_KEEP_WIDTH, PLAY_KEEP_HEIGHT);
            }
            g.setColor(Color.WHITE);
            drawShadowTextCenter(g, "Play",intermediateButtons[0].x+PLAY_KEEP_WIDTH/2, intermediateButtons[0].y+PLAY_KEEP_HEIGHT/2);
            drawShadowTextCenter(g, "Keep",intermediateButtons[1].x+PLAY_KEEP_WIDTH/2, intermediateButtons[0].y+PLAY_KEEP_HEIGHT/2);
            break;
        case STATE_WIN:
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.75f));
            g.fillRect(width/2-200, height/2-100, 400, 200);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            drawShadowTextCenter(g, "YOU WIN!", width/2, height/2);
            break;
        case STATE_LOSE:
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.75f));
            g.fillRect(width/2-200, height/2-100, 400, 200);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            drawShadowTextCenter(g, "YOU LOSE", width/2, height/2);
            break;
        }
    }

    private Point getComputerCardPoint(int card) {
        int row = card/cardColumns;
        int column = card%cardColumns;
        return new Point(cardIndentComputer + CARD_SEP_X*column, 10 + CARD_SEP_Y_COMPUTER*row);
    }

    private Point getPlayerCardPoint(int card) {
        int row = card/cardColumns;
        int column = card%cardColumns;
        return new Point(cardIndentPlayer + CARD_SEP_X*column, height - CARD_SEP_Y_PLAYER*(playerRows-row) - CARD_HEIGHT - 10);
    }

    private void paintColorSelectButton(Graphics2D g, Point p, int color) {
        g.setColor(UnoCard.getColor(color));
        g.fillRect(p.x, p.y, COLOR_SELECT_BUTTON_SIZE, COLOR_SELECT_BUTTON_SIZE);
    }

    private void paintCard(Graphics2D g, Point p) {
        int x = p.x;
        int y = p.y;

        AffineTransform transform = g.getTransform();
        int centerX = x+CARD_WIDTH/2;
        int centerY = y+CARD_HEIGHT/2;

        // Card background
        g.setColor(Color.BLACK);
        g.fillRoundRect(x, y, CARD_WIDTH+1, CARD_HEIGHT+1, CARD_ARC, CARD_ARC);
        g.setClip(x, y, CARD_WIDTH+1, CARD_HEIGHT+1);

        // Center oval
        g.rotate(Math.PI/2.75, centerX, centerY);
        g.setColor(Color.RED);
        g.fillOval(x+6, y+5, CARD_WIDTH-12, CARD_HEIGHT-10);
        g.setColor(Color.DARK_GRAY);
        g.drawOval(x+6, y+5, CARD_WIDTH-12, CARD_HEIGHT-10);
        g.setTransform(transform);
        g.setClip(null);

        // Uno text
        g.setFont(new Font("SansSerif", Font.BOLD|Font.ITALIC, 28));
        g.setColor(Color.WHITE);
        drawShadowTextCenter(g, "UNO", centerX, centerY);

        // Card border
        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
        g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CARD_ARC, CARD_ARC);
    }

    private void paintCard(Graphics2D g, Point p, UnoCard card) {
        int x = p.x;
        int y = p.y;

        Color cardColor = card.getColor();
        Color darkerColor = cardColor.darker().darker();
        String cardText = card.getText();
        String cardShortText = card.getShortText();
        AffineTransform transform = g.getTransform();

        int centerX = x+CARD_WIDTH/2;
        int centerY = y+CARD_HEIGHT/2;

        // Card background
        g.setColor(cardColor);
        g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CARD_ARC, CARD_ARC);
        g.setClip(x, y, CARD_WIDTH, CARD_HEIGHT);

        // Center oval
        g.rotate(Math.PI/2.75, centerX, centerY);
        g.setColor(Color.WHITE);
        g.fillOval(x+6, y+5, CARD_WIDTH-12, CARD_HEIGHT-10);
        g.setColor(darkerColor);
        g.drawOval(x+6, y+5, CARD_WIDTH-12, CARD_HEIGHT-10);
        g.setTransform(transform);
        g.setClip(null);

        // Corner text
        int fontSize = 11;
        if (card.isNumeric()) {
            fontSize = 18;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        g.setColor(Color.WHITE);
        drawShadowText(g, cardText, x+5, y+fontSize+2);
        g.rotate(Math.PI, centerX, centerY);
        drawShadowText(g, cardText, x+5, y+fontSize+2);
        g.setTransform(transform);

        // Big center text
        int bigFontSize = 42;
        if (cardShortText.length() > 2) {
            bigFontSize = 28;
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, bigFontSize));
        g.setColor(cardColor);
        drawShadowTextCenter(g, cardShortText, centerX, centerY);

        // Darken card if it can't be played
        if (card != topOfDeck && (state == STATE_COMPUTER_MOVE || !canPlay(card))) {
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.2f));
            g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CARD_ARC, CARD_ARC);
        }

        // Card border
        g.setColor(darkerColor);
        g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CARD_ARC, CARD_ARC);
    }

    private void drawShadowTextCenter(Graphics2D g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x2 = x - metrics.stringWidth(text)/2;
        int y2 = y - metrics.getHeight()/2 + metrics.getAscent();
        drawShadowText(g, text, x2, y2);
    }

    private void drawShadowText(Graphics2D g, String text, int x, int y) {
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

    private boolean inCardBounds(MouseEvent e, Point p) {
        int x = e.getX();
        int y = e.getY();
        return x >= p.x && x <= p.x+CARD_WIDTH && y >= p.y && y <= p.y+CARD_HEIGHT;
    }

    private boolean inColorSelectBounds(MouseEvent e, Point p) {
        int x = e.getX();
        int y = e.getY();
        return x >= p.x && x < p.x+COLOR_SELECT_BUTTON_SIZE && y >= p.y && y < p.y+COLOR_SELECT_BUTTON_SIZE;
    }

    private boolean inPlayKeepBounds(MouseEvent e, Point p) {
        int x = e.getX();
        int y = e.getY();
        return x >= p.x && x < p.x+PLAY_KEEP_WIDTH && y >= p.y && y < p.y+PLAY_KEEP_HEIGHT;
    }

    private void playerPlayCard(UnoCard card) {
        playCard(card);
        if (card.isSkip()) {
            int cardsDrawn = card.cardDraws();
            for (int i = 0; i < cardsDrawn; i++) {
                computerHand.add(drawCard());
            }
            state = STATE_PLAYER_MOVE;
        } else {
            state = STATE_COMPUTER_MOVE;
            stateTimer = System.currentTimeMillis()+1500;
        }
        if (playerHand.isEmpty()) {
            state = STATE_WIN;
            stateTimer = System.currentTimeMillis()+5000;
        }
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        synchronized (this) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                switch (state) {
                case STATE_PLAYER_MOVE:
                    if (drawPile != null) {
                        for (int c = playerHand.size() - 1; c >= 0; c--) {
                            if (inCardBounds(e, getPlayerCardPoint(c))) {
                                if (canPlay(playerHand.get(c))) {
                                    UnoCard card = playerHand.remove(c);
                                    if (card instanceof WildCard) {
                                        state = STATE_SELECT_COLOR;
                                        intermediateStateCard = card;
                                        repaint();
                                    } else {
                                        playerPlayCard(card);
                                    }
                                }
                                return;
                            }
                        }
                        if (inCardBounds(e, drawPile)) {
                            UnoCard card = drawCard();
                            if (canPlay(card)) {
                                state = STATE_PLAY_OR_KEEP;
                                intermediateStateCard = card;
                            } else {
                                playerHand.add(card);
                                state = STATE_COMPUTER_MOVE;
                                stateTimer = System.currentTimeMillis()+1000;
                            }
                            repaint();
                        }
                    }
                    break;
                case STATE_SELECT_COLOR:
                    if (intermediateButtons != null) {
                        for (int color = 0; color < 4; color++) {
                            if (inColorSelectBounds(e, intermediateButtons[color])) {
                                intermediateButtons = null;
                                ((WildCard) intermediateStateCard).setColor(color);
                                playerPlayCard(intermediateStateCard);
                                return;
                            }
                        }
                    }
                    break;
                case STATE_PLAY_OR_KEEP:
                    if (intermediateButtons != null) {
                        if (inPlayKeepBounds(e, intermediateButtons[0])) {
                            intermediateButtons = null;
                            if (intermediateStateCard instanceof WildCard) {
                                state = STATE_SELECT_COLOR;
                                repaint();
                            } else {
                                playerPlayCard(intermediateStateCard);
                            }
                        } else if (inPlayKeepBounds(e, intermediateButtons[1])) {
                            intermediateButtons = null;
                            playerHand.add(intermediateStateCard);
                            state = STATE_COMPUTER_MOVE;
                            stateTimer = System.currentTimeMillis() + 1000;
                            repaint();
                        }
                    }
                    break;
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
