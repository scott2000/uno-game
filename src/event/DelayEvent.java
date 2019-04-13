package event;

public class DelayEvent implements Event {
    private long timer;

    public DelayEvent(long timer) {
        this.timer = timer;
    }

    @Override
    public void start() {
        timer += System.currentTimeMillis();
    }

    @Override
    public boolean isDone() {
        return System.currentTimeMillis() >= timer;
    }
}
