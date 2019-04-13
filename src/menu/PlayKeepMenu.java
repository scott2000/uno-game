package menu;

import card.UnoCard;
import card.WildCard;
import display.UnoDisplay;
import display.UnoObject;
import event.EndTurnEvent;
import manager.PlayerManager;

import java.awt.*;

public class PlayKeepMenu extends UnoObject {
    private static final int WIDTH = 75;
    private static final int HEIGHT = 50;

    PlayerManager target;
    UnoCard card;
    int c;

    Point[] buttonLocations;

    public PlayKeepMenu(PlayerManager target, UnoCard card, int c) {
        this.target = target;
        this.card = card;
        this.c = c;
    }

    @Override
    public void paint(Graphics2D g) {
        int width = UnoDisplay.width;
        int height = UnoDisplay.height;

        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.75f));
        g.fillRect(width/2-200, height/2-100, 400, 200);
        card.paint(g, false, width/2-175, height/2-UnoCard.HEIGHT/2, 1.0f);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        UnoCard.shadowTextCenter(g, "Do you want to play or keep this card?", width/2, height/2-80);
        buttonLocations = new Point[] {
                new Point(width/2-WIDTH/2, height/2-HEIGHT/2),
                new Point(width/2+10+WIDTH, height/2-HEIGHT/2),
        };
        for (Point p : buttonLocations) {
            g.setColor(Color.WHITE);
            g.fillRect(p.x-1, p.y-1, WIDTH+2, HEIGHT+2);
            g.setColor(Color.BLACK);
            g.fillRect(p.x, p.y, WIDTH, HEIGHT);
        }
        g.setColor(Color.WHITE);
        UnoCard.shadowTextCenter(g, "Play",buttonLocations[0].x+WIDTH/2, buttonLocations[0].y+HEIGHT/2);
        UnoCard.shadowTextCenter(g, "Keep",buttonLocations[1].x+WIDTH/2, buttonLocations[0].y+HEIGHT/2);
    }

    private boolean inBounds(Point p, int x, int y) {
        return x >= p.x && x < p.x+WIDTH && y >= p.y && y < p.y+HEIGHT;
    }

    @Override
    public void click(int x, int y) {
        if (buttonLocations != null) {
            if (inBounds(buttonLocations[0], x, y)) {
                if (card instanceof WildCard) {
                    UnoDisplay.setMenu(new ColorSelectMenu(target, (WildCard) card, c));
                } else {
                    UnoDisplay.setMenu(null);
                    target.selectCard(c);
                }
            } else if (inBounds(buttonLocations[1], x, y)) {
                UnoDisplay.setMenu(null);
                UnoDisplay.pushEvent(new EndTurnEvent());
            }
        }
    }
}
