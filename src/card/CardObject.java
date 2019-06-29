package card;

import java.awt.*;

public final class CardObject {
    private static final long FLIP_TIME = 200;

    private static final double MOVE_SPEED = 1.0;
    private static final double FLIP_SPEED = 1.0/FLIP_TIME;

    private UnoCard card;

    private double x;
    private double y;
    private double flipAnimate = -1.0;
    private boolean isAnimating = false;
    private long highlightTime = 0;

    public UnoCard getCard() {
        return card;
    }

    public void setCard(UnoCard card) {
        this.card = card;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setFlipped(boolean flipped) {
        flipAnimate = card != null && flipped ? 1 : -1;
    }

    public void setHighlighted(boolean highlighted) {
        highlightTime = highlighted ? System.currentTimeMillis() - (long) ((1-flipAnimate)*FLIP_TIME) : 0;
    }

    public void update(double targetX, double targetY, boolean flipped, long time) {
        if (isAnimating) {
            isAnimating = false;
            if (x != targetX || y != targetY) {
                double xDiff = targetX - x;
                double yDiff = targetY - y;
                double magnitude = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
                double stm = MOVE_SPEED * time / magnitude;
                x += xDiff * stm;
                y += yDiff * stm;
                if (Math.signum(xDiff) != Math.signum(targetX - x)) {
                    x = targetX;
                } else {
                    isAnimating = true;
                }
                if (Math.signum(yDiff) != Math.signum(targetY - y)) {
                    y = targetY;
                } else {
                    isAnimating = true;
                }
            }
            if (card == null) {
                flipAnimate = -1.0;
            } else if (flipped && flipAnimate != 1.0) {
                isAnimating = true;
                flipAnimate = Math.min(flipAnimate + FLIP_SPEED * time, 1.0);
            } else if (!flipped && flipAnimate != -1.0) {
                isAnimating = true;
                flipAnimate = Math.max(flipAnimate - FLIP_SPEED * time, -1.0);
            }
        } else {
            setPosition(targetX, targetY);
            setFlipped(flipped);
        }
    }

    public void startAnimating() {
        isAnimating = true;
        highlightTime = 0;
    }

    public boolean doneAnimating() {
        return !isAnimating;
    }

    public boolean inBounds(int x, int y) {
        return inBounds(this.x, this.y, x, y);
    }

    public static boolean pointInBounds(Point p, int x, int y) {
        return inBounds(p.x, p.y, x, y);
    }

    private static boolean inBounds(double cardX, double cardY, int x, int y) {
        return x >= cardX && x <= cardX+CardGraphics.WIDTH && y >= cardY && y <= cardY+CardGraphics.HEIGHT;
    }

    public void paint(Graphics2D g, boolean darken) {
        CardGraphics.paint(g, card, darken, highlightTime, x, y, flipAnimate);
    }
}
