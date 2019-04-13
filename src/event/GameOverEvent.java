package event;

import display.UnoDisplay;

public class GameOverEvent implements Event {
    @Override
    public void start() {
        UnoDisplay.endGame();
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
