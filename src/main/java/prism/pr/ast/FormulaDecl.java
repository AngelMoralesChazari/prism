package prism.pr.ast;

public record FormulaDecl(String name, FormulaExpr expression) implements PrStmt {
}
