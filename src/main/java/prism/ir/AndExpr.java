package prism.ir;

/** Conjunción (∧). */
public record AndExpr(LogicExpr izquierda, LogicExpr derecha) implements LogicExpr {
    public AndExpr {
        if (izquierda == null || derecha == null) {
            throw new IllegalArgumentException("Los operandos no pueden ser nulos.");
        }
    }
}
