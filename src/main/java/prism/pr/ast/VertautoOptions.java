package prism.pr.ast;

public record VertautoOptions(boolean steps, boolean table, boolean verdict) {
    public static VertautoOptions corto() {
        return new VertautoOptions(false, false, false);
    }
}
