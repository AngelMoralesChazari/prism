package prism.pr;

public class PrParseException extends RuntimeException {
    private final int line;
    private final int col;
    private final int length;
    private final String suggestion;

    public PrParseException(String message) {
        this(message, 1, 1, 1, null);
    }

    public PrParseException(String message, int line, int col) {
        this(message, line, col, 1, null);
    }

    public PrParseException(String message, int line, int col, int length, String suggestion) {
        super(message);
        this.line = line;
        this.col = col;
        this.length = length;
        this.suggestion = suggestion;
    }

    public int line() {
        return line;
    }

    public int col() {
        return col;
    }

    public int length() {
        return length;
    }

    public String suggestion() {
        return suggestion;
    }
}
