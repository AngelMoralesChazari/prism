package prism.vertauto;

import java.util.ArrayList;
import java.util.List;
import prism.ir.AndExpr;
import prism.ir.AtomExpr;
import prism.ir.EquivExpr;
import prism.ir.ImpExpr;
import prism.ir.LogicExpr;
import prism.ir.NegExpr;
import prism.ir.OrExpr;

/**
 * Parsea fórmulas en notación clásica emitida por {@code EmitidorFormula}:
 * {@code ¬ ∧ ∨ → ↔} y paréntesis. Átomos: identificadores {@code p}, {@code q}, …
 */
public final class ParserNotacionClasica {

    private final String input;
    private int pos;

    private ParserNotacionClasica(String input) {
        this.input = input == null ? "" : input.trim();
        this.pos = 0;
    }

    public static LogicExpr parsear(String formula) {
        if (formula == null || formula.isBlank()) {
            throw new IllegalArgumentException("La fórmula no puede estar vacía.");
        }
        ParserNotacionClasica p = new ParserNotacionClasica(formula);
        LogicExpr expr = p.parseExpr();
        p.skipSpaces();
        if (p.pos < p.input.length()) {
            throw new IllegalArgumentException("Entrada sobrante en fórmula: " + formula);
        }
        return expr;
    }

    private LogicExpr parseExpr() {
        LogicExpr left = parseImp();
        while (true) {
            skipSpaces();
            if (match('\u2194')) {
                LogicExpr right = parseImp();
                left = new EquivExpr(left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private LogicExpr parseImp() {
        LogicExpr left = parseOr();
        while (true) {
            skipSpaces();
            if (match('\u2192')) {
                LogicExpr right = parseImp();
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
            if (match('\u2228')) {
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
            if (match('\u2227')) {
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
        if (match('\u00AC')) {
            return new NegExpr(parseNot());
        }
        return parsePrimary();
    }

    private LogicExpr parsePrimary() {
        skipSpaces();
        if (match('(')) {
            LogicExpr inner = parseExpr();
            skipSpaces();
            if (!match(')')) {
                throw new IllegalArgumentException("Falta ')' en fórmula.");
            }
            return inner;
        }
        String atom = leerIdentificador();
        if (atom.isEmpty()) {
            throw new IllegalArgumentException("Se esperaba átomo o '(' en posición " + pos);
        }
        return new AtomExpr(atom, false);
    }

    private String leerIdentificador() {
        skipSpaces();
        int start = pos;
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
