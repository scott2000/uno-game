package menu;

import card.UnoCard;
import card.WildCard;
import display.UnoPanel;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class ColorSelectMenu implements UnoMenu {
    private static final int BUTTON_SIZE = 75;

    private WildCard card;
    private int c;

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
        g.fillRect(width/2-200, height/2-100, 400, 200);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        UnoPanel.shadowTextCenter(g, "Select a color for the wild card:", width/2, height/2-80);
        int xl = width/2-BUTTON_SIZE;
        int xr = width/2;
        int yt = height/2-BUTTON_SIZE+15;
        int yb = height/2+15;
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
        g.setClip(null);
        g.setColor(Color.BLACK);
        g.drawOval(xl, yt, BUTTON_SIZE*2, BUTTON_SIZE*2);
    }

    @Override
    public void click(int x, int y) {
        if (buttonLocations != null) {
            for (int color = 0; color < 4; color++) {
                Point p = buttonLocations[color];
                if (x >= p.x && x < p.x+BUTTON_SIZE && y >= p.y && y < p.y+BUTTON_SIZE) {
                    card.setColor(color);
                    UnoPanel.setMenu(null);
                    UnoPanel.playCard(c);
                    return;
                }
            }
        }
    }
}
