package manager.web;

public class Message {
    final String kind;
    final String contents;

    Message(String kind) {
        this(kind, null);
    }

    Message(String kind, String contents) {
        this.kind = kind;
        this.contents = contents;
    }

    @Override
    public String toString() {
        return kind+WebManager.MESSAGE_SEPARATOR+contents;
    }
}
