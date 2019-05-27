package display;

@FunctionalInterface
public interface Event {
    void start();
    default boolean isDone() {
        return true;
    }
}
