package prism.pr.ast;

public record TextRef(String literal, String variableName) {
    public static TextRef literal(String value) {
        return new TextRef(value, null);
    }

    public static TextRef variable(String name) {
        return new TextRef(null, name);
    }

    public boolean isLiteral() {
        return variableName == null;
    }
}
