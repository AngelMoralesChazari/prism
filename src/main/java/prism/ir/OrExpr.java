package prism.ir;

/** Disyunción (∨). */
public record OrExpr(LogicExpr izquierda, LogicExpr derecha) implements LogicExpr {
    public OrExpr {
        if (izquierda == null || derecha == null) {
            throw new IllegalArgumentException("Los operandos no pueden ser nulos.");
        }
    }
}
