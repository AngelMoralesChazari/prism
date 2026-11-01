package prism.pr;

import prism.pr.ast.PrProgram;
import java.util.List;

public final class PrCheckHarness {

    private PrCheckHarness() {
    }

    public static void main(String[] args) {
        int fallos = 0;
        fallos += probarOk("examples/plantilla_ejercicio.pr");
        fallos += probarOk("examples/ejercicio_lluvia.pr");
        fallos += probarOk("examples/tautologia_estudio.pr");
        fallos += probarErrorEnFuente(
                """
                module m
                let x = translate(falta)
                """,
                "Texto nl no definido"
        );
        fallos += probarErrorEnFuente(
                """
                module m
                formula f = p or q
                vertauto f
                """,
                "Átomo no declarado"
        );
        fallos += probarErrorEnFuente(
                """
                module m
                atom p = "a"
                atom p = "b"
                """,
                "Redeclaración"
        );
        fallos += probarErrorEnFuente(
                """
                module m
                vertauto desconocida
                """,
                "Fórmula no definida"
        );
        if (fallos > 0) {
            System.err.println("PrCheckHarness: " + fallos + " fallo(s)");
            System.exit(1);
        }
        System.out.println("PrCheckHarness: OK (7 casos)");
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
}
