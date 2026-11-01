package nlp.lexer;

import java.util.Objects;

/** Token emitido por {@link NaturalLexer} sobre texto ya normalizado. */
public final class TokenNatural {
    private final TipoTokenNatural tipo;
    private final String lexema;

    public TokenNatural(TipoTokenNatural tipo, String lexema) {
        this.tipo = Objects.requireNonNull(tipo);
        this.lexema = lexema != null ? lexema : "";
    }

    public TipoTokenNatural getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    @Override
    public String toString() {
        return tipo + "(\"" + lexema + "\")";
    }
}
