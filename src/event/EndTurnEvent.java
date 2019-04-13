package event;

import display.UnoDisplay;

public class EndTurnEvent implements Event {
    @Override
    public void start() {
        UnoDisplay.finishTurn();
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
