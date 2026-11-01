package prism.pr;

import prism.pr.ast.AtomDecl;
import prism.pr.ast.AtomRef;
import prism.pr.ast.BinaryExpr;
import prism.pr.ast.BinaryOp;
import prism.pr.ast.FormulaDecl;
import prism.pr.ast.FormulaExpr;
import prism.pr.ast.FormulaRef;
import prism.pr.ast.LetDecl;
import prism.pr.ast.PrProgram;
import prism.pr.ast.PrStmt;
import prism.pr.ast.NlDecl;
import prism.pr.ast.NotExpr;
import prism.pr.ast.TextRef;
import prism.pr.ast.TranslateCall;
import prism.pr.ast.VertautoOptions;
import prism.pr.ast.VertautoStmt;
import java.util.ArrayList;
import java.util.List;

public final class PrParser {

    private final List<PrToken> tokens;
    private int pos;

    public PrParser(List<PrToken> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    public static PrProgram parse(String source) {
        PrLexer lexer = new PrLexer(source);
        return new PrParser(lexer.tokenize()).parseProgram();
    }

    private PrProgram parseProgram() {
        expect(PrTokenType.MODULE);
        String moduleName = expectIdent("nombre de módulo");
        List<String> uses = new ArrayList<>();
        List<PrStmt> statements = new ArrayList<>();

        while (!check(PrTokenType.EOF)) {
            if (match(PrTokenType.USE)) {
                uses.add(expectIdent("módulo en use"));
                continue;
            }
            statements.add(parseStatement());
        }
        return new PrProgram(moduleName, List.copyOf(uses), List.copyOf(statements));
    }

    private PrStmt parseStatement() {
        if (match(PrTokenType.ATOM)) {
            String name = expectIdent("átomo");
            expect(PrTokenType.EQ);
            String label = expectString("etiqueta de átomo");
            return new AtomDecl(name, label);
        }
        if (match(PrTokenType.NL)) {
            String name = expectIdent("variable nl");
            expect(PrTokenType.EQ);
            String text = expectString("texto nl");
            return new NlDecl(name, text);
        }
        if (match(PrTokenType.LET)) {
            String name = expectIdent("variable let");
            expect(PrTokenType.EQ);
            TranslateCall call = parseTranslateCall();
            return new LetDecl(name, call);
        }
        if (match(PrTokenType.FORMULA)) {
            String name = expectIdent("fórmula");
            expect(PrTokenType.EQ);
            FormulaExpr expr = parseFormulaExpr();
            return new FormulaDecl(name, expr);
        }
        if (match(PrTokenType.VERTAUTO)) {
            FormulaRef target = parseFormulaRef();
            VertautoOptions options = parseVertautoOptions();
            return new VertautoStmt(target, options);
        }
        throw error("se esperaba atom, nl, let, formula o vertauto");
    }

    private TranslateCall parseTranslateCall() {
        expect(PrTokenType.TRANSLATE);
        expect(PrTokenType.LPAREN);
        TextRef text = parseTextRef();
        String moduleOverride = null;
        if (match(PrTokenType.COMMA)) {
            moduleOverride = expectIdent("módulo en translate");
        }
        expect(PrTokenType.RPAREN);
        return new TranslateCall(text, moduleOverride);
    }

    private TextRef parseTextRef() {
        if (match(PrTokenType.STRING)) {
            return TextRef.literal(previous().lexeme());
        }
        if (check(PrTokenType.IDENT)) {
            return TextRef.variable(advance().lexeme());
        }
        throw error("se esperaba cadena o identificador en translate");
    }

    private FormulaRef parseFormulaRef() {
        if (match(PrTokenType.LPAREN)) {
            FormulaExpr expr = parseFormulaExpr();
            expect(PrTokenType.RPAREN);
            return FormulaRef.inline(expr);
        }
        return FormulaRef.variable(expectIdent("referencia de fórmula"));
    }

    private VertautoOptions parseVertautoOptions() {
        if (!match(PrTokenType.LBRACE)) {
            return VertautoOptions.corto();
        }
        boolean steps = false;
        boolean table = false;
        boolean verdict = false;
        while (!check(PrTokenType.RBRACE) && !check(PrTokenType.EOF)) {
            if (match(PrTokenType.STEPS)) {
                steps = true;
            } else if (match(PrTokenType.TABLE)) {
                table = true;
            } else if (match(PrTokenType.VERDICT)) {
                verdict = true;
            } else {
                throw error("opción vertauto desconocida (steps, table, verdict)");
            }
        }
        expect(PrTokenType.RBRACE);
        return new VertautoOptions(steps, table, verdict);
    }

    private FormulaExpr parseFormulaExpr() {
        return parseIff();
    }

    private FormulaExpr parseIff() {
        FormulaExpr left = parseImpl();
        while (match(PrTokenType.IFF)) {
            left = new BinaryExpr(BinaryOp.IFF, left, parseImpl());
        }
        return left;
    }

    private FormulaExpr parseImpl() {
        FormulaExpr left = parseOr();
        while (match(PrTokenType.IMPL)) {
            left = new BinaryExpr(BinaryOp.IMPL, left, parseImpl());
        }
        return left;
    }

    private FormulaExpr parseOr() {
        FormulaExpr left = parseAnd();
        while (match(PrTokenType.OR)) {
            left = new BinaryExpr(BinaryOp.OR, left, parseAnd());
        }
        return left;
    }

    private FormulaExpr parseAnd() {
        FormulaExpr left = parseNot();
        while (match(PrTokenType.AND)) {
            left = new BinaryExpr(BinaryOp.AND, left, parseNot());
        }
        return left;
    }

    private FormulaExpr parseNot() {
        if (match(PrTokenType.NOT)) {
            return new NotExpr(parseNot());
        }
        return parsePrimary();
    }

    private FormulaExpr parsePrimary() {
        if (match(PrTokenType.LPAREN)) {
            FormulaExpr inner = parseFormulaExpr();
            expect(PrTokenType.RPAREN);
            return inner;
        }
        if (check(PrTokenType.IDENT)) {
            return new AtomRef(advance().lexeme());
        }
        throw error("se esperaba identificador o '(' en expresión de fórmula");
    }

    private boolean match(PrTokenType type) {
        if (check(type)) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean check(PrTokenType type) {
        if (isAtEnd()) {
            return type == PrTokenType.EOF;
        }
        return peek().type() == type;
    }

    private PrToken advance() {
        if (!isAtEnd()) {
            pos++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == PrTokenType.EOF;
    }

    private PrToken peek() {
        return tokens.get(pos);
    }

    private PrToken previous() {
        return tokens.get(pos - 1);
    }

    private void expect(PrTokenType type) {
        if (!match(type)) {
            throw error("se esperaba " + type);
        }
    }

    private String expectIdent(String contexto) {
        if (check(PrTokenType.IDENT)) {
            return advance().lexeme();
        }
        throw error("se esperaba identificador (" + contexto + ")");
    }

    private String expectString(String contexto) {
        if (match(PrTokenType.STRING)) {
            return previous().lexeme();
        }
        throw error("se esperaba cadena (" + contexto + ")");
    }

    private PrParseException error(String message) {
        PrToken token = peek();
        return new PrParseException("Error de sintaxis en línea " + token.line() + ": " + message);
    }
}
