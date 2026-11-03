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
    private int lineStartPos;

    public PrLexer(String input) {
        this.input = input == null ? "" : input;
        this.pos = 0;
        this.line = 1;
        this.lineStartPos = 0;
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
        int col = pos - lineStartPos + 1;
        if (pos >= input.length()) {
            return new PrToken(PrTokenType.EOF, "", startLine, col);
        }
        char c = input.charAt(pos);
        return switch (c) {
            case '=' -> single(PrTokenType.EQ, startLine, col);
            case ':' -> single(PrTokenType.COLON, startLine, col);
            case ',' -> single(PrTokenType.COMMA, startLine, col);
            case '(' -> single(PrTokenType.LPAREN, startLine, col);
            case ')' -> single(PrTokenType.RPAREN, startLine, col);
            case '{' -> single(PrTokenType.LBRACE, startLine, col);
            case '}' -> single(PrTokenType.RBRACE, startLine, col);
            case '"' -> readString(startLine, col);
            default -> {
                if (Character.isLetter(c) || c == '_') {
                    yield readIdentOrKeyword(startLine, col);
                }
                throw new PrParseException("Carácter no reconocido '" + c + "' en línea " + startLine + ":" + col, startLine, col);
            }
        };
    }

    private PrToken single(PrTokenType type, int startLine, int col) {
        String lex = String.valueOf(input.charAt(pos++));
        return new PrToken(type, lex, startLine, col);
    }

    private void skipWhitespaceAndComments() {
        while (pos < input.length()) {
            char c = input.charAt(pos);

            // 1. Comentario de bloque /* ... */
            if (c == '/' && pos + 1 < input.length() && input.charAt(pos + 1) == '*') {
                int startLine = line;
                int col = pos - lineStartPos + 1;
                pos += 2;
                boolean cerrado = false;
                while (pos < input.length()) {
                    if (input.charAt(pos) == '*' && pos + 1 < input.length() && input.charAt(pos + 1) == '/') {
                        pos += 2;
                        cerrado = true;
                        break;
                    }
                    if (input.charAt(pos) == '\n') {
                        line++;
                        pos++;
                        lineStartPos = pos;
                        continue;
                    }
                    pos++;
                }
                if (!cerrado) {
                    throw new PrParseException("Comentario multilínea '/*' sin cerrar en línea " + startLine + ":" + col, startLine, col);
                }
                continue;
            }

            // 2. Comentario de bloque ### ... ###
            if (c == '#' && pos + 2 < input.length() && input.charAt(pos + 1) == '#' && input.charAt(pos + 2) == '#') {
                int startLine = line;
                int col = pos - lineStartPos + 1;
                pos += 3;
                boolean cerrado = false;
                while (pos < input.length()) {
                    if (input.charAt(pos) == '#' && pos + 2 < input.length() && input.charAt(pos + 1) == '#' && input.charAt(pos + 2) == '#') {
                        pos += 3;
                        cerrado = true;
                        break;
                    }
                    if (input.charAt(pos) == '\n') {
                        line++;
                        pos++;
                        lineStartPos = pos;
                        continue;
                    }
                    pos++;
                }
                if (!cerrado) {
                    throw new PrParseException("Comentario multilínea '###' sin cerrar en línea " + startLine + ":" + col, startLine, col);
                }
                continue;
            }

            // 3. Comentario de línea # ...
            if (c == '#') {
                while (pos < input.length() && input.charAt(pos) != '\n') {
                    pos++;
                }
                continue;
            }

            // 4. Espacios en blanco y saltos de línea
            if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
                continue;
            }
            if (c == '\n') {
                line++;
                pos++;
                lineStartPos = pos;
                continue;
            }
            break;
        }
    }

    private PrToken readString(int startLine, int col) {
        // Bloque con triple comilla: """ ... """
        if (pos + 2 < input.length() && input.charAt(pos + 1) == '"' && input.charAt(pos + 2) == '"') {
            pos += 3;
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                if (input.charAt(pos) == '"' && pos + 2 < input.length() && input.charAt(pos + 1) == '"' && input.charAt(pos + 2) == '"') {
                    pos += 3;
                    return new PrToken(PrTokenType.STRING, limpiarCadenaMultilinea(sb.toString()), startLine, col);
                }
                char c = input.charAt(pos++);
                if (c == '\n') {
                    line++;
                    lineStartPos = pos;
                    sb.append('\n');
                    continue;
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
            throw new PrParseException("Cadena multilínea '\"\"\"' sin cerrar en línea " + startLine + ":" + col, startLine, col);
        }

        // Cadena estándar con comillas simples: " ... " (permite saltos de línea con ENTER)
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos++);
            if (c == '"') {
                return new PrToken(PrTokenType.STRING, limpiarCadenaMultilinea(sb.toString()), startLine, col);
            }
            if (c == '\n') {
                line++;
                lineStartPos = pos;
                sb.append('\n');
                continue;
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
        throw new PrParseException("Cadena sin cerrar en línea " + startLine + ":" + col, startLine, col);
    }

    private String limpiarCadenaMultilinea(String raw) {
        if (!raw.contains("\n") && !raw.contains("\r")) {
            return raw;
        }
        return raw.replaceAll("[\\r\\n]+[\\t ]*", " ").trim();
    }

    private PrToken readIdentOrKeyword(int startLine, int col) {
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
        return new PrToken(type, text, startLine, col);
    }
}
