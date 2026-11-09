package prism.pr;

import prism.pr.ast.PrProgram;
import java.util.List;

public final class PrCheckHarness {

    private PrCheckHarness() {
    }

    public static void main(String[] args) {
        int fallos = 0;
        fallos += probarOk("examples/ejercicio_lluvia.pr");
        fallos += probarOk("examples/prueba_nl.pr");
        fallos += probarOk("examples/translate_literal.pr");
        fallos += probarOk("examples/reglas_inferencia.pr");
        fallos += probarOk("examples/deteccion_falacias.pr");
        fallos += probarOk("src/main/resources/prism/core.pr");
        fallos += probarErrorEnFuente(
                """
                        module m
                        let x = translate(falta)
                        """,
                "Texto nl no definido");
        fallos += probarErrorEnFuente(
                """
                        module m
                        formula f = p or q
                        vertauto f
                        """,
                "Átomo no declarado");
        fallos += probarErrorEnFuente(
                """
                        module m
                        atom p = "a"
                        atom p = "b"
                        """,
                "Redeclaración");
        fallos += probarErrorEnFuente(
                """
                        module m
                        vertauto desconocida
                        """,
                "Fórmula no definida");
        fallos += probarErrorEnFuente(
                """
                        module m
                        atom p = "a"
                        formula a = b and p
                        formula b = a or p
                        """,
                "Referencia circular");
        fallos += probarErrorEnFuente(
                """
                        module m
                        atom p = "llueve"
                        nl texto = "si llueve salgo"
                        formula f = texto and p
                        """,
                "es una variable nl");
        fallos += probarErrorEnFuente(
                """
                        module m
                        atom p = "llueve"
                        vertauto (p and z)
                        """,
                "Átomo no declarado en 'vertauto inline': z");
        fallos += probarUbicacionDiagnostico();
        if (fallos > 0) {
            System.err.println("PrCheckHarness: " + fallos + " fallo(s)");
            System.exit(1);
        }
        System.out.println("PrCheckHarness: OK (14 casos)");
    }

    private static int probarOk(String archivo) {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            java.io.PrintStream originalOut = System.out;
            try {
                System.setOut(new java.io.PrintStream(out, true, java.nio.charset.StandardCharsets.UTF_8));
                if (!PrChecker.checkArchivo(archivo)) {
                    System.setOut(originalOut);
                    System.err.println(archivo + ": se esperaba OK");
                    return 1;
                }
            } finally {
                System.setOut(originalOut);
            }
            return 0;
        } catch (RuntimeException e) {
            System.err.println(archivo + ": " + e.getMessage());
            return 1;
        }
    }

    private static int probarErrorEnFuente(String fuente, String fragmento) {
        try {
            PrProgram program = PrParser.parse(fuente);
            List<String> errores = PrValidator.validar(program);
            if (errores.isEmpty()) {
                System.err.println("Se esperaba error con: " + fragmento);
                return 1;
            }
            String todas = String.join("\n", errores);
            if (!todas.contains(fragmento)) {
                System.err.println("Se esperaba '" + fragmento + "' en: " + todas);
                return 1;
            }
            return 0;
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains(fragmento)) {
                return 0;
            }
            System.err.println("Error inesperado: " + e.getMessage());
            return 1;
        }
    }

    private static int probarUbicacionDiagnostico() {
        String fuente = """
                module diag_test
                atom p = "uno"
                nl texto = "si llueve llevo paraguas"
                formula a = b and p
                formula b = a impl p
                formula c = texto or p
                vertauto (a and z)
                """;
        List<PrDiagnostic> diags = PrChecker.diagnosticarFuente(fuente, "diag_test.pr");
        boolean vioNlEnLinea6 = diags.stream().anyMatch(d -> d.line() == 6 && d.message().contains("'texto' en 'c'"));
        boolean vioInlineEnLinea7 = diags.stream().anyMatch(d -> d.line() == 7 && d.message().contains("'vertauto inline': z"));
        boolean vioCicloEnLinea4 = diags.stream().anyMatch(d -> d.line() == 4 && d.message().contains("Referencia circular"));
        if (!vioNlEnLinea6 || !vioInlineEnLinea7 || !vioCicloEnLinea4) {
            System.err.println("Ubicaciones inesperadas en diag_test: " + diags);
            return 1;
        }
        return 0;
    }
}
