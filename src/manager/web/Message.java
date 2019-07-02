package manager.web;

public final class Message {
    final String kind;
    final String contents;
    final boolean optional;

    Message(String kind) {
        this(kind, null);
    }

    Message(String kind, String contents) {
        if (kind.startsWith(Character.toString(WebManager.OPTIONAL))) {
            this.kind = kind.substring(1);
            optional = true;
        } else {
            this.kind = kind;
            optional = false;
        }
        this.contents = contents;
    }

    @Override
    public String toString() {
        String header = optional ? WebManager.OPTIONAL + kind : kind;
        return contents == null ? header : header+WebManager.MESSAGE_SEPARATOR+contents;
    }
}
