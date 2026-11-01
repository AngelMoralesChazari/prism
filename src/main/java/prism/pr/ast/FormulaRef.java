package prism.pr.ast;

public record FormulaRef(String variableName, FormulaExpr inlineExpression) {
    public static FormulaRef variable(String name) {
        return new FormulaRef(name, null);
    }

    public static FormulaRef inline(FormulaExpr expr) {
        return new FormulaRef(null, expr);
    }

    public boolean isVariable() {
        return variableName != null;
    }
}
