package prism.vertauto;

import prism.ir.AndExpr;
import prism.ir.AtomExpr;
import prism.ir.EquivExpr;
import prism.ir.ImpExpr;
import prism.ir.LogicExpr;
import prism.ir.NegExpr;
import prism.ir.OrExpr;

/**
 * Parsea expresiones de fórmula en sintaxis Prism {@code .ls}:
 * {@code not and or impl iff} con identificadores {@code p}, {@code q}, …
 */
public final class ParserFormulaPrism {

    private final String input;
    private int pos;

    private ParserFormulaPrism(String input) {
        this.input = input == null ? "" : input.trim();
        this.pos = 0;
    }

    public static LogicExpr parsear(String expresion) {
        if (expresion == null || expresion.isBlank()) {
            throw new IllegalArgumentException("La expresión no puede estar vacía.");
        }
        ParserFormulaPrism p = new ParserFormulaPrism(expresion);
        LogicExpr expr = p.parseIff();
        p.skipSpaces();
        if (p.pos < p.input.length()) {
            throw new IllegalArgumentException("Entrada sobrante en expresión: " + expresion);
        }
        return expr;
    }

    private LogicExpr parseIff() {
        LogicExpr left = parseImpl();
        while (true) {
            skipSpaces();
            if (palabra("iff")) {
                LogicExpr right = parseImpl();
                left = new EquivExpr(left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private LogicExpr parseImpl() {
        LogicExpr left = parseOr();
        while (true) {
            skipSpaces();
            if (palabra("impl")) {
                LogicExpr right = parseImpl();
                left = new ImpExpr(left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private LogicExpr parseOr() {
        LogicExpr left = parseAnd();
        while (true) {
            skipSpaces();
            if (palabra("or")) {
                LogicExpr right = parseAnd();
                left = new OrExpr(left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private LogicExpr parseAnd() {
        LogicExpr left = parseNot();
        while (true) {
            skipSpaces();
            if (palabra("and")) {
                LogicExpr right = parseNot();
                left = new AndExpr(left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private LogicExpr parseNot() {
        skipSpaces();
        if (palabra("not")) {
            return new NegExpr(parseNot());
        }
        return parsePrimary();
    }

    private LogicExpr parsePrimary() {
        skipSpaces();
        if (match('(')) {
            LogicExpr inner = parseIff();
            skipSpaces();
            if (!match(')')) {
                throw new IllegalArgumentException("Falta ')' en expresión.");
            }
            return inner;
        }
        String id = leerIdentificador();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Se esperaba identificador o '(' en posición " + pos);
        }
        return new AtomExpr(id, false);
    }

    private boolean palabra(String kw) {
        skipSpaces();
        if (!input.regionMatches(true, pos, kw, 0, kw.length())) {
            return false;
        }
        int after = pos + kw.length();
        if (after < input.length()) {
            char c = input.charAt(after);
            if (Character.isLetterOrDigit(c) || c == '_') {
                return false;
            }
        }
        pos = after;
        return true;
    }

    private String leerIdentificador() {
        skipSpaces();
        int start = pos;
        if (pos >= input.length() || !Character.isLetter(input.charAt(pos))) {
            return "";
        }
        pos++;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') {
                pos++;
            } else {
                break;
            }
        }
        return input.substring(start, pos);
    }

    private boolean match(char expected) {
        skipSpaces();
        if (pos < input.length() && input.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private void skipSpaces() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }
}
