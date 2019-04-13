package card;

import display.UnoDisplay;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public abstract class UnoCard implements Comparable<UnoCard> {
    private static final double MOVE_SPEED = 1.0;
    private static final double FLIP_SPEED = 0.005;

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

    private double x;
    private double y;
    private double flipAnimate = -1.0;
    private boolean isAnimating = false;

    private BufferedImage cached;

    public abstract int getColorCode();
    public abstract int getNumberCode();
    public abstract int getOrderCode();

    public abstract boolean canPlay(int color, int number);
    public abstract boolean isNumeric();
    public abstract boolean isSkip();
    public abstract int cardDraws();

    public abstract Color getColor();
    public abstract String getText();
    public abstract String getShortText();

    private static BufferedImage cachedBlank;
    static {
        cachedBlank = new BufferedImage(WIDTH+1, HEIGHT+1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) cachedBlank.getGraphics();
        UnoDisplay.setHints(g);

        AffineTransform transform = g.getTransform();
        double centerX = WIDTH/2;
        double centerY = HEIGHT/2;

        // Card background
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Double(0, 0, WIDTH+1, HEIGHT+1, ARC, ARC));
        g.setClip(new Rectangle2D.Double(0, 0, WIDTH+1, HEIGHT+1));

        // Center oval
        g.rotate(Math.PI/2.75, centerX, centerY);
        g.setColor(Color.RED);
        g.fill(new Ellipse2D.Double(6, 5, WIDTH-12, HEIGHT-10));
        g.setColor(Color.DARK_GRAY);
        g.draw(new Ellipse2D.Double(6, 5, WIDTH-12, HEIGHT-10));
        g.setTransform(transform);
        g.setClip(null);

        // Uno text
        g.setFont(new Font("SansSerif", Font.BOLD|Font.ITALIC, 28));
        g.setColor(Color.WHITE);
        UnoDisplay.shadowTextCenter(g, "UNO", (float) centerX, (float) centerY);

        // Card border
        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
        g.draw(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));

        g.dispose();
    }

    private static BufferedImage shade;
    static {
        shade = new BufferedImage(WIDTH+1, HEIGHT+1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) shade.getGraphics();
        UnoDisplay.setHints(g);

        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.15f));
        g.fill(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));

        g.dispose();
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setFlipped(boolean flipped) {
        flipAnimate = flipped ? 1 : -1;
    }

    public void update(double targetX, double targetY, boolean flipped, long time) {
        isAnimating = false;
        if (x != targetX || y != targetY) {
            isAnimating = true;
            double xDiff = targetX - x;
            double yDiff = targetY - y;
            double magnitude = Math.sqrt(xDiff*xDiff + yDiff*yDiff);
            double stm = MOVE_SPEED*time/magnitude;
            x += xDiff*stm;
            y += yDiff*stm;
            if (Math.signum(xDiff) != Math.signum(targetX - x)) {
                x = targetX;
            }
            if (Math.signum(yDiff) != Math.signum(targetY - y)) {
                y = targetY;
            }
        }
        if (flipped && flipAnimate != 1.0) {
            isAnimating = true;
            flipAnimate = Math.min(flipAnimate + FLIP_SPEED*time, 1.0);
        } else if (!flipped && flipAnimate != -1.0) {
            isAnimating = true;
            flipAnimate = Math.max(flipAnimate - FLIP_SPEED*time, -1.0);
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

    public void paint(Graphics2D g, boolean darken, double x, double y, double flipAnimate) {
        if (flipAnimate == 0) {
            g.setColor(Color.BLACK);
            g.draw(new Line2D.Double(x, y, x, y+HEIGHT));
            return;
        }

        AffineTransform transform = new AffineTransform();

        double centerX = WIDTH/2;
        double scaleFactor = Math.abs(flipAnimate);
        if (scaleFactor != 1) {
            scaleFactor = Math.pow(scaleFactor, 0.75);
            transform.translate(x+centerX, y);
            transform.scale(scaleFactor, 1.0);
            transform.translate(-centerX, 0);
        } else {
            transform.translate(x, y);
        }

        if (flipAnimate < 0) {
            paintBlank(g, transform);

            if (scaleFactor != 1) {
                g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                g.draw(new RoundRectangle2D.Double(x+centerX*(1-scaleFactor), y, WIDTH*scaleFactor, HEIGHT, ARC*scaleFactor, ARC));
            }
            return;
        }

        g.drawImage(getCached(), transform, null);

        if (darken) {
            g.drawImage(shade, transform, null);
        }

        if (scaleFactor != 1) {
            g.setColor(getColor().darker().darker());
            g.draw(new RoundRectangle2D.Double(x+centerX*(1-scaleFactor), y, WIDTH*scaleFactor, HEIGHT, ARC*scaleFactor, ARC));
        }
    }

    public static void paintBlank(Graphics2D g, double x, double y) {
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        g.drawImage(cachedBlank, transform, null);
    }

    private static void paintBlank(Graphics2D g, AffineTransform transform) {
        g.drawImage(cachedBlank, transform, null);
    }

    void invalidateCached() {
        cached = null;
    }

    private BufferedImage getCached() {
        if (cached != null) return cached;

        cached = new BufferedImage(WIDTH+1, HEIGHT+1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) cached.getGraphics();
        UnoDisplay.setHints(g);

        AffineTransform transform = g.getTransform();
        double centerX = WIDTH/2;
        double centerY = HEIGHT/2;

        Color cardColor = getColor();
        Color darkerColor = cardColor.darker().darker();
        String cardText = getText();
        String cardShortText = getShortText();

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
        if (isNumeric()) {
            fontSize = 18;
        }
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        g.setColor(Color.WHITE);
        UnoDisplay.shadowText(g, cardText, 5, fontSize+2);
        g.rotate(Math.PI, centerX, centerY);
        UnoDisplay.shadowText(g, cardText, 5, fontSize+2);
        g.setTransform(transform);

        // Big center text
        int bigFontSize = 42;
        if (cardShortText.length() > 2) {
            bigFontSize = 28;
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, bigFontSize));
        g.setColor(cardColor);
        UnoDisplay.shadowTextCenter(g, cardShortText, (float) centerX, (float) centerY);

        // Card border
        g.setColor(darkerColor);
        g.draw(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, ARC, ARC));

        g.dispose();
        return cached;
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

    @Override
    public int compareTo(UnoCard o) {
        return getOrderCode()-o.getOrderCode();
    }
}
