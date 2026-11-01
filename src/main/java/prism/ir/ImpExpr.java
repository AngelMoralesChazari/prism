package prism.ir;

/** Implicación (→): antecedente → consecuente. */
public record ImpExpr(LogicExpr antecedente, LogicExpr consecuente) implements LogicExpr {
    public ImpExpr {
        if (antecedente == null || consecuente == null) {
            throw new IllegalArgumentException("Antecedente y consecuente no pueden ser nulos.");
        }
    }
}
