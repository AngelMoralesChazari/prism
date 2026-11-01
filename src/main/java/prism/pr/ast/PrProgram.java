package prism.pr.ast;

import java.util.List;

public record PrProgram(String moduleName, List<String> uses, List<PrStmt> statements) {
}
