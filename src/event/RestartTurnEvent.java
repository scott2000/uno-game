package event;

import display.UnoDisplay;
import manager.HandManager;

public class RestartTurnEvent implements Event {
    @Override
    public void start() {
        HandManager current = UnoDisplay.getCurrentManager();
        current.endTurn();
        current.startTurn();
    }

    @Override
    public boolean isDone() {
        return true;
    }
}