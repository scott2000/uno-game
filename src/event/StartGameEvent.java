package event;

import display.UnoDisplay;

public class StartGameEvent implements Event {
    @Override
    public void start() {
        UnoDisplay.startGame();
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
