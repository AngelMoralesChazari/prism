package prism.ir;

/** Equivalencia (↔): ambos miembros con el mismo valor de verdad. */
public record EquivExpr(LogicExpr izquierda, LogicExpr derecha) implements LogicExpr {
    public EquivExpr {
        if (izquierda == null || derecha == null) {
            throw new IllegalArgumentException("Miembros de equivalencia no pueden ser nulos.");
        }
    }
}
