package prism.ir;

/** Negación lógica de una subexpresión (p. ej. para futuros patrones compuestos). */
public record NegExpr(LogicExpr interior) implements LogicExpr {
    public NegExpr {
        if (interior == null) {
            throw new IllegalArgumentException("La subexpresión no puede ser nula.");
        }
    }
}
