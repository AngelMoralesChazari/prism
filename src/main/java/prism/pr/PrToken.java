package prism.pr;

public record PrToken(PrTokenType type, String lexeme, int line, int col) {
    public PrToken(PrTokenType type, String lexeme, int line) {
        this(type, lexeme, line, 1);
    }
}
