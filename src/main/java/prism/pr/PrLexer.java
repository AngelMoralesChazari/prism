package prism.pr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PrLexer {

    private static final Map<String, PrTokenType> KEYWORDS = Map.ofEntries(
            Map.entry("module", PrTokenType.MODULE),
            Map.entry("use", PrTokenType.USE),
            Map.entry("atom", PrTokenType.ATOM),
            Map.entry("formula", PrTokenType.FORMULA),
            Map.entry("nl", PrTokenType.NL),
            Map.entry("let", PrTokenType.LET),
            Map.entry("translate", PrTokenType.TRANSLATE),
            Map.entry("vertauto", PrTokenType.VERTAUTO),
            Map.entry("not", PrTokenType.NOT),
            Map.entry("and", PrTokenType.AND),
            Map.entry("or", PrTokenType.OR),
            Map.entry("impl", PrTokenType.IMPL),
            Map.entry("iff", PrTokenType.IFF),
            Map.entry("steps", PrTokenType.STEPS),
            Map.entry("table", PrTokenType.TABLE),
            Map.entry("verdict", PrTokenType.VERDICT)
    );

    private final String input;
    private int pos;
    private int line;

    public PrLexer(String input) {
        this.input = input == null ? "" : input;
        this.pos = 0;
        this.line = 1;
    }

    public List<PrToken> tokenize() {
        List<PrToken> tokens = new ArrayList<>();
        while (true) {
            PrToken token = nextToken();
            tokens.add(token);
            if (token.type() == PrTokenType.EOF) {
                break;
            }
        }
        return tokens;
    }

    private PrToken nextToken() {
        skipWhitespaceAndComments();
        int startLine = line;
        if (pos >= input.length()) {
            return new PrToken(PrTokenType.EOF, "", startLine);
        }
        char c = input.charAt(pos);
        return switch (c) {
            case '=' -> single(PrTokenType.EQ, startLine);
            case ':' -> single(PrTokenType.COLON, startLine);
            case ',' -> single(PrTokenType.COMMA, startLine);
            case '(' -> single(PrTokenType.LPAREN, startLine);
            case ')' -> single(PrTokenType.RPAREN, startLine);
            case '{' -> single(PrTokenType.LBRACE, startLine);
            case '}' -> single(PrTokenType.RBRACE, startLine);
            case '"' -> readString(startLine);
            default -> {
                if (Character.isLetter(c) || c == '_') {
                    yield readIdentOrKeyword(startLine);
                }
                throw new PrParseException("Carácter no reconocido '" + c + "' en línea " + startLine);
            }
        };
    }

    private PrToken single(PrTokenType type, int startLine) {
        String lex = String.valueOf(input.charAt(pos++));
        return new PrToken(type, lex, startLine);
    }

    private void skipWhitespaceAndComments() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '#') {
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    pos++;
                }
                continue;
            }
            if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
                continue;
            }
            if (c == '\n') {
                line++;
                pos++;
                continue;
            }
            break;
        }
    }

    private PrToken readString(int startLine) {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos++);
            if (c == '"') {
                return new PrToken(PrTokenType.STRING, sb.toString(), startLine);
            }
            if (c == '\\' && pos < input.length()) {
                char esc = input.charAt(pos++);
                sb.append(switch (esc) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> esc;
                });
            } else {
                sb.append(c);
            }
        }
        throw new PrParseException("Cadena sin cerrar en línea " + startLine);
    }

    private PrToken readIdentOrKeyword(int startLine) {
        int start = pos;
        pos++;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') {
                pos++;
            } else {
                break;
            }
        }
        String text = input.substring(start, pos);
        PrTokenType type = KEYWORDS.getOrDefault(text, PrTokenType.IDENT);
        return new PrToken(type, text, startLine);
    }
}
