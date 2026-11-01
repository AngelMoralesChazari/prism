package prism.pr.ast;

public record LetDecl(String name, TranslateCall translate) implements PrStmt {
}
