package prism.pr;

/**
 * Representa un diagnóstico estructurado (error de sintaxis, validación o aviso semántico)
 * consumible por herramientas CLI y extensiones de editores (como VS Code / Cursor).
 */
public record PrDiagnostic(
        int line,
        int col,
        int endLine,
        int endCol,
        String severity,
        String message,
        String suggestion
) {
    public String toJson() {
        return "{"
                + "\"line\":" + line + ","
                + "\"col\":" + col + ","
                + "\"endLine\":" + endLine + ","
                + "\"endCol\":" + endCol + ","
                + "\"severity\":\"" + severity + "\","
                + "\"message\":\"" + escapeJson(message) + "\","
                + "\"suggestion\":" + (suggestion == null ? "null" : "\"" + escapeJson(suggestion) + "\"")
                + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

