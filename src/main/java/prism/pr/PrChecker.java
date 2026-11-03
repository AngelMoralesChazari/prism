package prism.pr;

import prism.PrismService;
import prism.pr.ast.PrProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class PrChecker {

    private PrChecker() {
    }

    /**
     * Analiza un archivo .pr y devuelve diagnósticos estructurados con línea, columna, severidad y sugerencias.
     */
    public static List<PrDiagnostic> diagnosticarArchivo(String ruta) {
        String source;
        try {
            source = PrSourceLoader.leer(ruta);
        } catch (Exception e) {
            return List.of(new PrDiagnostic(1, 1, 1, 10, "error", "No se pudo leer el archivo: " + e.getMessage(), null));
        }
        return diagnosticarFuente(source, ruta);
    }

    /**
     * Analiza el código fuente de un programa Prism y produce diagnósticos para el editor y CLI.
     */
    public static List<PrDiagnostic> diagnosticarFuente(String source, String ruta) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        // 1. Fase Léxica y Sintáctica (Parser)
        PrProgram program;
        try {
            program = PrParser.parse(source);
        } catch (PrParseException e) {
            int len = Math.max(1, e.length());
            return List.of(new PrDiagnostic(
                    e.line(),
                    e.col(),
                    e.line(),
                    e.col() + len,
                    "error",
                    e.getMessage(),
                    e.suggestion()
            ));
        } catch (Exception e) {
            return List.of(new PrDiagnostic(1, 1, 1, 10, "error", "Error de análisis: " + e.getMessage(), null));
        }

        // 2. Fase de Validación Estática (Tipos, identificadores y convenciones)
        List<String> erroresValidacion = new ArrayList<>(PrValidator.validar(program));
        if (ruta != null && !ruta.isBlank()) {
            PrValidator.validarArchivoConvencion(ruta, program.moduleName(), erroresValidacion);
        }

        if (!erroresValidacion.isEmpty()) {
            List<PrDiagnostic> diags = new ArrayList<>();
            String[] lineas = source.split("\n", -1);
            for (String err : erroresValidacion) {
                int lin = 1;
                int col = 1;
                int endCol = 20;

                int q1 = err.indexOf('\'');
                int q2 = err.indexOf('\'', q1 + 1);
                if (q1 != -1 && q2 != -1) {
                    String ident = err.substring(q1 + 1, q2);
                    for (int i = 0; i < lineas.length; i++) {
                        int idx = lineas[i].indexOf(ident);
                        if (idx != -1) {
                            lin = i + 1;
                            col = idx + 1;
                            endCol = col + ident.length();
                            break;
                        }
                    }
                }
                diags.add(new PrDiagnostic(lin, col, lin, endCol, "error", err, null));
            }
            return Collections.unmodifiableList(diags);
        }

        // 3. Fase de Linter Semántico (Coherencia atom <-> translate)
        List<PrDiagnostic> diags = new ArrayList<>();
        try {
            PrismService service = new PrismService();
            List<String> avisos = PrSemanticLinter.diagnosticarPrograma(program, service);
            String[] lineas = source.split("\n", -1);
            for (String av : avisos) {
                int lin = 1;
                int col = 1;
                int endCol = 30;
                String sugerencia = null;

                int q1 = av.indexOf('\'');
                int q2 = av.indexOf('\'', q1 + 1);
                if (q1 != -1 && q2 != -1) {
                    String atomo = av.substring(q1 + 1, q2);
                    for (int i = 0; i < lineas.length; i++) {
                        if (lineas[i].matches(".*\\batom\\s+" + Pattern.quote(atomo) + "\\b.*")) {
                            lin = i + 1;
                            col = 1;
                            endCol = lineas[i].length() + 1;
                            break;
                        }
                    }
                }

                int idxSug = av.indexOf("¿Quisiste decir: ");
                if (idxSug != -1) {
                    int finSug = av.indexOf('?', idxSug);
                    if (finSug != -1) {
                        sugerencia = av.substring(idxSug + "¿Quisiste decir: ".length(), finSug).trim();
                    }
                }

                diags.add(new PrDiagnostic(lin, col, lin, endCol, "warning", av, sugerencia));
            }
        } catch (Exception ignored) {
        }

        return Collections.unmodifiableList(diags);
    }

    /**
     * Valida un archivo e imprime los resultados en formato legible de consola.
     */
    public static boolean checkArchivo(String ruta) {
        List<PrDiagnostic> diags = diagnosticarArchivo(ruta);
        boolean hayErrores = false;
        for (PrDiagnostic d : diags) {
            if ("error".equals(d.severity())) {
                System.err.println(d.message());
                hayErrores = true;
            } else {
                System.err.println(d.message());
            }
        }
        if (hayErrores) {
            return false;
        }
        System.out.println("OK: " + ruta);
        return true;
    }
}
