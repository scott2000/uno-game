package menu;

import card.UnoCard;
import card.WildCard;
import display.UnoPanel;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class ColorSelectMenu implements UnoMenu {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private static final int BUTTON_SIZE = 75;

    private WildCard card;
    private int c;

    private Point boxLocation;
    private Point[] buttonLocations;

    public ColorSelectMenu(WildCard card, int c) {
        this.card = card;
        this.c = c;
    }

    @Override
    public void paint(Graphics2D g) {
        int width = UnoPanel.width;
        int height = UnoPanel.height;

        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.75f));
        boxLocation = new Point(width/2 - WIDTH/2, height/2 - HEIGHT/2);
        g.fillRect(boxLocation.x, boxLocation.y, WIDTH, HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        UnoPanel.shadowTextCenter(g, "Select a color for the wild card:", width/2, height/2-80);
        int xl = width/2-BUTTON_SIZE;
        int xr = width/2+1;
        int yt = height/2-BUTTON_SIZE+15;
        int yb = height/2+15+1;
        buttonLocations = new Point[] {
                new Point(xl, yt),
                new Point(xl, yb),
                new Point(xr, yb),
                new Point(xr, yt),
        };
        g.clip(new Ellipse2D.Double(xl, yt,BUTTON_SIZE*2, BUTTON_SIZE*2));
        for (int color = UnoCard.COLOR_MIN; color <= UnoCard.COLOR_MAX; color++) {
            Point p = buttonLocations[color];
            g.setColor(UnoCard.getColor(color));
            g.fillRect(p.x, p.y, BUTTON_SIZE, BUTTON_SIZE);
        }
        g.setColor(Color.BLACK);
        g.drawLine(width/2, yt, width/2, yb+BUTTON_SIZE);
        g.drawLine(xl, height/2+15, xr+BUTTON_SIZE, height/2+15);
        g.setClip(null);
        g.drawOval(xl, yt, BUTTON_SIZE*2, BUTTON_SIZE*2);
    }

    @Override
    public boolean click(int x, int y) {
        if (boxLocation != null) {
            for (int color = 0; color < 4; color++) {
                Point p = buttonLocations[color];
                if (x >= p.x && x < p.x+BUTTON_SIZE && y >= p.y && y < p.y+BUTTON_SIZE) {
                    card.setColor(color);
                    UnoPanel.setMenu(null);
                    UnoPanel.playCard(c);
                    return true;
                }
            }
            return x >= boxLocation.x && x <= boxLocation.x+WIDTH && y >= boxLocation.y && y <= boxLocation.y+HEIGHT;
        }
        return false;
    }
}
