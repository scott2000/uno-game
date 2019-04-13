package card;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public abstract class UnoCard {
    private static final float MOVE_SPEED = 1.0f;
    private static final float FLIP_SPEED = 1/200.0f;

    public static final int WIDTH = 75;
    public static final int HEIGHT = 105;
    public static final int ARC = 7;
    public static final int SEP_X = WIDTH + 5;
    public static final int SEP_Y_COMPUTER = HEIGHT/3;
    public static final int SEP_Y_PLAYER = HEIGHT*2/3;

    public static final int RED = 0;
    public static final int YELLOW = 1;
    public static final int GREEN = 2;
    public static final int BLUE = 3;

    public static final int NO_NUMBER = -1;
    public static final int SKIP = 10;
    public static final int REVERSE = 11;
    public static final int DRAW_2 = 12;

    private float x;
    private float y;

    private float flipAnimate = -1.0f;

    private boolean isAnimating = false;

    public abstract int getColorCode();
    public abstract int getNumberCode();

    public abstract boolean canPlay(int color, int number);
    public abstract boolean isNumeric();
    public abstract boolean isSkip();
    public abstract int cardDraws();

    public abstract Color getColor();
    public abstract String getText();
    public abstract String getShortText();

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setFlipped(boolean flipped) {
        flipAnimate = flipped ? 1 : -1;
    }

    public void update(float targetX, float targetY, boolean flipped, long time) {
        if (x == 0 && y == 0) {
            setPosition(targetX, targetY);
            setFlipped(flipped);
            return;
        }
        isAnimating = false;
        if (x != targetX || y != targetY) {
            isAnimating = true;
            double xDiff = targetX - x;
            double yDiff = targetY - y;
            double magnitude = Math.sqrt(xDiff*xDiff + yDiff*yDiff);
            x += (float) (xDiff*MOVE_SPEED*time/magnitude);
            y += (float) (yDiff*MOVE_SPEED*time/magnitude);
            if (Math.signum(xDiff) != Math.signum(targetX - x)) {
                x = targetX;
            }
            if (Math.signum(yDiff) != Math.signum(targetY - y)) {
                y = targetY;
            }
        }
        if (flipped && flipAnimate != 1.0f) {
            isAnimating = true;
            flipAnimate = Math.min(flipAnimate + FLIP_SPEED*time, 1.0f);
        } else if (!flipped && flipAnimate != -1.0f) {
            isAnimating = true;
            flipAnimate = Math.max(flipAnimate - FLIP_SPEED*time, -1.0f);
        }
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public boolean inBounds(int x, int y) {
        return x >= this.x && x <= this.x+WIDTH && y >= this.y && y <= this.y+HEIGHT;
    }

    public static boolean inBounds(Point p, int x, int y) {
        return x >= p.x && x <= p.x+WIDTH && y >= p.y && y <= p.y+HEIGHT;
    }

    public void paint(Graphics2D g, boolean darken) {
        paint(g, darken, x, y, flipAnimate);
    }

    public void paint(Graphics2D g, boolean darken, float x, float y, float flipAnimate) {
        if (flipAnimate == 0) return;

        float centerX = x+WIDTH/2;
        float centerY = y+HEIGHT/2;

        AffineTransform transform = g.getTransform();

        g.translate(centerX, 0);
        g.scale(Math.abs(flipAnimate), 1.0);
        g.translate(-centerX, 0);

        if (flipAnimate < 0) {
            paintBlank(g, x, y);
            g.setTransform(transform);
            return;
        }

        AffineTransform transformAfterSquash = g.getTransform();

        Color cardColor = getColor();
        Color darkerColor = cardColor.darker().darker();
        String cardText = getText();
        String cardShortText = getShortText();

        // Card background
        g.setColor(cardColor);
        g.fill(new RoundRectangle2D.Float(x, y, WIDTH, HEIGHT, ARC, ARC));
        g.setClip(new Rectangle2D.Float(x, y, WIDTH, HEIGHT));

        // Center oval
        g.rotate(Math.PI/2.75, centerX, centerY);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(x+6, y+5, WIDTH-12, HEIGHT-10));
        g.setColor(darkerColor);
        g.draw(new Ellipse2D.Float(x+6, y+5, WIDTH-12, HEIGHT-10));
        g.setTransform(transformAfterSquash);
        g.setClip(null);

        // Corner text
        int fontSize = 11;
        if (isNumeric()) {
            fontSize = 18;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        g.setColor(Color.WHITE);
        shadowText(g, cardText, x+5, y+fontSize+2);
        g.rotate(Math.PI, centerX, centerY);
        shadowText(g, cardText, x+5, y+fontSize+2);
        g.setTransform(transformAfterSquash);

        // Big center text
        int bigFontSize = 42;
        if (cardShortText.length() > 2) {
            bigFontSize = 28;
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, bigFontSize));
        g.setColor(cardColor);
        shadowTextCenter(g, cardShortText, centerX, centerY);

        if (darken) {
            g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.15f));
            g.fill(new RoundRectangle2D.Float(x, y, WIDTH, HEIGHT, ARC, ARC));
        }

        // Card border
        g.setColor(darkerColor);
        g.draw(new RoundRectangle2D.Float(x, y, WIDTH, HEIGHT, ARC, ARC));
        g.setTransform(transform);
    }

    public static void paintBlank(Graphics2D g, float x, float y) {
        AffineTransform transform = g.getTransform();
        float centerX = x+UnoCard.WIDTH/2;
        float centerY = y+UnoCard.HEIGHT/2;

        // Card background
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Float(x, y, UnoCard.WIDTH+1, UnoCard.HEIGHT+1, UnoCard.ARC, UnoCard.ARC));
        g.setClip(new Rectangle2D.Float(x, y, UnoCard.WIDTH+1, UnoCard.HEIGHT+1));

        // Center oval
        g.rotate(Math.PI/2.75, centerX, centerY);
        g.setColor(Color.RED);
        g.fill(new Ellipse2D.Float(x+6, y+5, UnoCard.WIDTH-12, UnoCard.HEIGHT-10));
        g.setColor(Color.DARK_GRAY);
        g.draw(new Ellipse2D.Float(x+6, y+5, UnoCard.WIDTH-12, UnoCard.HEIGHT-10));
        g.setTransform(transform);
        g.setClip(null);

        // Uno text
        g.setFont(new Font("SansSerif", Font.BOLD|Font.ITALIC, 28));
        g.setColor(Color.WHITE);
        shadowTextCenter(g, "UNO", centerX, centerY);

        // Card border
        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
        g.draw(new RoundRectangle2D.Float(x, y, UnoCard.WIDTH, UnoCard.HEIGHT, UnoCard.ARC, UnoCard.ARC));
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

    public static Color getColor(int color) {
        switch (color) {
        case RED:
            return Color.RED;
        case YELLOW:
            return Color.ORANGE;
        case GREEN:
            return Color.GREEN.darker();
        case BLUE:
            return new Color(0, 127, 255);
        default:
            throw new IllegalStateException("error: invalid color code");
        }
    }

    public static String getColorText(int color) {
        switch (color) {
        case RED:
            return "Red";
        case YELLOW:
            return "Yellow";
        case GREEN:
            return "Green";
        case BLUE:
            return "Blue";
        default:
            throw new IllegalStateException("error: invalid color code");
        }
    }

    public static String getNumberText(int number) {
        switch (number) {
        case SKIP:
            return "Skip";
        case REVERSE:
            return "Reverse";
        case DRAW_2:
            return "Draw two";
        default:
            return Integer.toString(number);
        }
    }
}
