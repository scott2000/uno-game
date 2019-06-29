package card;

import display.UnoPanel;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public final class CardGraphics {
    private CardGraphics() {}

    public static final int WIDTH = 75;
    public static final int HEIGHT = 105;

    public static final int ARC = 7;
    public static final int HIGHLIGHT_ARC = ARC+3;

    private static final BufferedImage BACK_IMAGE;
    private static final BufferedImage SHADE_IMAGE;

    private static BufferedImage[] cardImages = new BufferedImage[UnoCard.ORDER_CODE_COUNT];

    public static void paint(Graphics2D g, UnoCard card, boolean darken, long highlightTime, double x, double y, double flipAnimate) {
        if (flipAnimate == 0) {
            g.setColor(Color.BLACK);
            g.draw(new Line2D.Double(x, y, x, y+HEIGHT));
            return;
        }

        AffineTransform transform = new AffineTransform();

        double centerX = WIDTH/2;
        double scaleFactor = Math.abs(flipAnimate);

        if (highlightTime != 0 && flipAnimate > 0) {
            float dist = UnoPanel.getHighlightDist(highlightTime);
            g.setColor(UnoPanel.withAlpha(card.getColor(), 0.5f));
            g.fill(new RoundRectangle2D.Double(x+centerX*(1-scaleFactor)-dist, y-dist, (WIDTH+dist*2)*scaleFactor+1, HEIGHT+dist*2+1, HIGHLIGHT_ARC*scaleFactor, HIGHLIGHT_ARC));
        }

        boolean noAntialias = Math.round(x) == x && Math.round(y) == y && scaleFactor == 1;
        if (noAntialias) UnoPanel.setNoAntialias(g);

        if (scaleFactor != 1) {
            scaleFactor = Math.pow(scaleFactor, 0.75);
            transform.translate(x+centerX, y);
            transform.scale(scaleFactor, 1.0);
            transform.translate(-centerX, 0);
        } else {
            transform.translate(x, y);
        }

        if (flipAnimate < 0 || card == null) {
            paintBlank(g, transform);

            if (scaleFactor != 1) {
                g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                g.draw(new RoundRectangle2D.Double(x+centerX*(1-scaleFactor), y, WIDTH*scaleFactor, HEIGHT, ARC*scaleFactor, ARC));
            }
        } else {
            g.drawImage(getCardImage(card), transform, null);

            if (darken) {
                g.drawImage(SHADE_IMAGE, transform, null);
            }

            if (scaleFactor != 1) {
                g.setColor(card.getColor().darker().darker());
                g.draw(new RoundRectangle2D.Double(x+centerX*(1-scaleFactor), y, WIDTH*scaleFactor, HEIGHT, ARC*scaleFactor, ARC));
            }
        }
        if (noAntialias) UnoPanel.setAntialias(g);
    }

    public static void paintBlank(Graphics2D g, double x, double y) {
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        g.drawImage(BACK_IMAGE, transform, null);
    }

    private static void paintBlank(Graphics2D g, AffineTransform transform) {
        g.drawImage(BACK_IMAGE, transform, null);
    }

    private static BufferedImage getCardImage(UnoCard card) {
        int orderCode = card.getOrderCode();

        if (cardImages[orderCode] != null) {
            return cardImages[orderCode];
        }

        cardImages[orderCode] = new BufferedImage(WIDTH+1, HEIGHT+1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) cardImages[orderCode].getGraphics();
        UnoPanel.setHints(g);

        AffineTransform transform = g.getTransform();
        double centerX = WIDTH/2;
        double centerY = HEIGHT/2;

        Color cardColor = card.getColor();
        Color darkerColor = cardColor.darker().darker();
        String cardText = card.getText();
        String cardShortText = card.getShortText();

        // Card background
        g.setColor(cardColor);
        g.fill(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));
        g.setClip(new Rectangle2D.Double(0, 0, WIDTH, HEIGHT));

        // Center oval
        g.rotate(Math.PI/2.75, centerX, centerY);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(6, 5, WIDTH-12, HEIGHT-10));
        g.setColor(darkerColor);
        g.draw(new Ellipse2D.Double(6, 5, WIDTH-12, HEIGHT-10));
        g.setTransform(transform);
        g.setClip(null);

        // Corner text
        int fontSize = 11;
        if (card.isNumeric()) {
            fontSize = 18;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        g.setColor(Color.WHITE);
        UnoPanel.shadowText(g, cardText, 5, fontSize+2);
        g.rotate(Math.PI, centerX, centerY);
        UnoPanel.shadowText(g, cardText, 5, fontSize+2);
        g.setTransform(transform);

        // Big center text
        int bigFontSize = 42;
        if (cardShortText.length() > 2) {
            bigFontSize = 28;
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, bigFontSize));
        g.setColor(cardColor);
        UnoPanel.shadowTextCenter(g, cardShortText, (float) centerX, (float) centerY);

        // Card border
        g.setColor(darkerColor);
        g.draw(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));

        g.dispose();
        return cardImages[orderCode];
    }

    static {
        BACK_IMAGE = new BufferedImage(WIDTH + 1, HEIGHT + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) BACK_IMAGE.getGraphics();
        UnoPanel.setHints(g);

        AffineTransform transform = g.getTransform();
        double centerX = WIDTH / 2;
        double centerY = HEIGHT / 2;

        // Card background
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Double(0, 0, WIDTH + 1, HEIGHT + 1, ARC, ARC));
        g.setClip(new Rectangle2D.Double(0, 0, WIDTH + 1, HEIGHT + 1));

        // Center oval
        g.rotate(Math.PI / 2.75, centerX, centerY);
        g.setColor(Color.RED);
        g.fill(new Ellipse2D.Double(6, 5, WIDTH - 12, HEIGHT - 10));
        g.setColor(Color.DARK_GRAY);
        g.draw(new Ellipse2D.Double(6, 5, WIDTH - 12, HEIGHT - 10));
        g.setTransform(transform);
        g.setClip(null);

        // Uno text
        g.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 28));
        g.setColor(Color.WHITE);
        UnoPanel.shadowTextCenter(g, "UNO", (float) centerX, (float) centerY);

        // Card border
        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
        g.draw(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));

        g.dispose();
    }

    static {
        SHADE_IMAGE = new BufferedImage(WIDTH+1, HEIGHT+1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) SHADE_IMAGE.getGraphics();
        UnoPanel.setHints(g);

        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.15f));
        g.fill(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));

        g.dispose();
    }
}
