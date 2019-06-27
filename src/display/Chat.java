package display;

import card.UnoCard;
import manager.HandManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Chat {
    private static final int TEXT_MARGIN = 5;
    private static final long HIDE_DELAY = 5000;

    private String buffer = null;
    private int cursor = 0;
    private Message[] history = new Message[64];
    private int start = 0;
    private long visibleTime = 0;

    private String name;
    private String opponentName = "Opponent";

    private class Message {
        final String message;
        final boolean isPlayer;

        Message(String message, boolean isPlayer) {
            String sender = isPlayer ? name : opponentName;
            this.message = "["+sender+"] "+message;
            this.isPlayer = isPlayer;
        }

        List<String> getLines(FontMetrics m, int space) {
            ArrayList<String> lines = new ArrayList<>();
            String current = "";
            for (char c : message.toCharArray()) {
                String next = current+c;
                if (c != '\b' && m.stringWidth(next) > space) {
                    if (c == ' ') {
                        lines.add(current);
                        current = "";
                    } else {
                        int lastSpace = current.lastIndexOf(' ');
                        if (lastSpace == -1) {
                            String hyphen = current+'-';
                            if (m.stringWidth(hyphen) > space) {
                                int s = current.length()-1;
                                lines.add(current.substring(0, s)+'-');
                                current = next.substring(s);
                            } else {
                                lines.add(hyphen);
                                current = Character.toString(c);
                            }
                        } else {
                            lines.add(current.substring(0, lastSpace));
                            current = next.substring(lastSpace+1);
                        }
                    }
                } else {
                    current = next;
                }
            }
            lines.add(current);
            return lines;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    Chat(String name) {
        this.name = name;
    }

    public void setOpponentName(String name) {
        opponentName = name;
    }

    public String getName() {
        return name;
    }

    public void receive(String message) {
        add(new Message(message, false));
    }

    void press(int code) {
        if (buffer == null) {
            switch (code) {
            case KeyEvent.VK_ENTER:
                buffer = "";
                cursor = 0;
                break;
            case KeyEvent.VK_ESCAPE:
                visibleTime = 0;
                break;
            }
        } else {
            switch (code) {
            case KeyEvent.VK_ENTER:
                if (buffer.isEmpty()) {
                    visibleTime = System.currentTimeMillis() + HIDE_DELAY;
                } else {
                    add(new Message(buffer, true));
                    UnoPanel.sendMessage(buffer);
                }
                buffer = null;
                break;
            case KeyEvent.VK_ESCAPE:
                visibleTime = System.currentTimeMillis()+1000;
                buffer = null;
                break;
            case KeyEvent.VK_LEFT:
                if (cursor != 0) cursor--;
                break;
            case KeyEvent.VK_RIGHT:
                if (cursor != buffer.length()) cursor++;
                break;
            case KeyEvent.VK_BACK_SPACE:
                if (cursor != 0) {
                    buffer = buffer.substring(0, cursor-1)+buffer.substring(cursor);
                    cursor--;
                }
                break;
            }
        }
    }

    void type(char c) {
        if (buffer == null) {
            buffer = Character.toString(c);
            cursor = 1;
        } else {
            buffer = buffer.substring(0, cursor)+c+buffer.substring(cursor);
            cursor++;
        }
    }

    void paint(Graphics2D g, int maxX, int minY, int maxY) {
        long currentTime = System.currentTimeMillis();
        float opacity;
        if (buffer == null) {
            if (currentTime > visibleTime) {
                return;
            } else {
                opacity = Math.min(visibleTime-currentTime, 1000)/1000f;
            }
        } else {
            opacity = 1.0f;
        }

        int minX = HandManager.MARGIN;
        maxX -= HandManager.MARGIN;
        minY += HandManager.MARGIN;
        maxY -= HandManager.MARGIN;

        g.setColor(new Color(0.0f, 0.0f, 0.0f, opacity*0.5f));
        g.fillRect(minX, minY, maxX-minX, maxY-minY);

        int w = maxX-minX;
        int h = maxY-minY;

        if (opacity == 1.0f) {
            g.setClip(minX, minY, w, h);
            paint(g, minX, maxX, minY, maxY);
            g.setClip(0, 0, UnoPanel.width, UnoPanel.height);
        } else {
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = (Graphics2D) image.getGraphics();
            UnoPanel.setHints(ig);
            paint(ig, 0, w, 0, h);
            ig.dispose();
            Composite composite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g.drawImage(image, minX, minY, null);
            g.setComposite(composite);
        }
    }

    private void paint(Graphics2D g, int minX, int maxX, int minY, int maxY) {
        Font font = new Font("SansSerif", Font.BOLD, 18);
        FontMetrics m = g.getFontMetrics(font);
        g.setFont(font);

        int textMinX = minX + TEXT_MARGIN;
        int textMaxX = maxX - TEXT_MARGIN;
        int textSpaceX = textMaxX-textMinX;

        int textMinY = minY - m.getDescent();
        int textMaxY = maxY - m.getDescent() - TEXT_MARGIN;
        int textSepY = m.getHeight();

        outer: for (int y = textMaxY, i = 0; ; i++) {
            Message message = get(i);
            if (message == null) break;
            if (message.isPlayer) {
                Color color = UnoPanel.getTextColor();
                if (color == null) {
                    g.setColor(UnoCard.getTextColor(0));
                } else {
                    g.setColor(color);
                }
            } else {
                g.setColor(Color.WHITE);
            }

            List<String> lines = message.getLines(m, textSpaceX);
            for (int line = lines.size()-1; line >= 0; line--, y -= textSepY) {
                if (y <= textMinY) break outer;
                String text = lines.get(line);
                int cursorIndex = text.indexOf('\b');
                if (cursorIndex == -1) {
                    UnoPanel.shadowText(g, lines.get(line), textMinX, y, 0.7f);
                } else {
                    String first = text.substring(0, cursorIndex);
                    int offset = m.stringWidth(first);
                    UnoPanel.shadowText(g, first, textMinX, y, 0.7f);
                    if (cursorIndex+1 < text.length()) {
                        String last = text.substring(cursorIndex+1);
                        UnoPanel.shadowText(g, last, textMinX+offset, y, 0.7f);
                    }
                    Color color = g.getColor();
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(textMinX+offset, y-m.getAscent(), 3, m.getAscent()+m.getDescent());
                    g.setColor(color);
                    g.fillRect(textMinX+offset+1, y-m.getAscent()+1, 1, m.getAscent()+m.getDescent()-2);
                }
            }
        }
    }

    private void add(Message message) {
        start = (start+history.length-1) % history.length;
        history[start] = message;
        visibleTime = System.currentTimeMillis()+HIDE_DELAY;
    }

    private Message get(int index) {
        if (buffer != null) {
            if (index == 0) {
                return new Message(buffer.substring(0, cursor)+'\b'+buffer.substring(cursor), true);
            } else {
                index -= 1;
            }
        }
        if (index >= history.length) return null;
        return history[(start+index) % history.length];
    }
}
