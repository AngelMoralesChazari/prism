package prism.pr;

import prism.pr.ast.PrProgram;

public final class PrInterpreterHarness {

    private PrInterpreterHarness() {
    }

    public static void main(String[] args) {
        int fallos = 0;
        fallos += probarParser();
        fallos += probarEjecucion("examples/ejercicio_lluvia.pr", "contingency", "antecedente=p", "consecuente=q");
        fallos += probarEjecucion("examples/prueba_nl.pr", "contingency", "el procesador se suspende=p", "la tarea se reintenta=q");
        fallos += probarEjecucion("examples/translate_literal.pr", "contingency", "estudio=p", "apruebo=q");
        fallos += probarEjecucion("examples/reglas_inferencia.pr", "tautology", "llueve=p", "la calle se moja=q");
        fallos += probarEjecucion("examples/deteccion_falacias.pr", "contingency", "llueve=p", "la calle se moja=q");
        fallos += probarEjecucionModular();
        if (fallos > 0) {
            System.err.println("PrInterpreterHarness: " + fallos + " fallo(s)");
            System.exit(1);
        }
        System.out.println("PrInterpreterHarness: OK (7 casos)");
    }

    private static int probarParser() {
        String src = """
                module prueba
                use core
                nl t = "si p q"
                let f = translate(t)
                vertauto f
                """;
        try {
            PrProgram p = PrParser.parse(src);
            if (!"prueba".equals(p.moduleName()) || p.statements().size() != 3) {
                throw new AssertionError("AST inesperado");
            }
            return 0;
        } catch (RuntimeException e) {
            System.err.println("Parser: " + e.getMessage());
            return 1;
        }
    }

    private static int probarEjecucion(String archivo, String... fragmentosEsperados) {
        try {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            var originalOut = System.out;
            try {
                System.setOut(new java.io.PrintStream(buffer, true, java.nio.charset.StandardCharsets.UTF_8));
                new PrInterpreter().ejecutarArchivo(archivo);
            } finally {
                System.setOut(originalOut);
            }
            String salida = buffer.toString(java.nio.charset.StandardCharsets.UTF_8);
            for (String fragmento : fragmentosEsperados) {
                if (!salida.contains(fragmento)) {
                    System.err.println(archivo + ": se esperaba '" + fragmento + "' en la salida");
                    System.err.println(salida);
                    return 1;
                }
            }
            return 0;
        } catch (RuntimeException e) {
            System.err.println(archivo + ": " + e.getMessage());
            return 1;
        }
    }

    private static int probarEjecucionModular() {
        String src = """
                module modular_inline
                use core
                formula mp = (regla and p) impl q
                vertauto mp { verdict }
                atom p = "llueve"
                atom q = "la calle se moja"
                let regla = translate("Si llueve entonces la calle se moja.")
                let auto1 = translate("Si hace frio entonces uso abrigo.")
                let auto2 = translate("Uso abrigo.")
                let auto3 = translate("Hace frio.")
                vertauto ((auto1 and auto2) impl auto3) { verdict }
                """;
        try {
            PrProgram program = PrParser.parse(src);
            PrValidator.validarOError(program);
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            var originalOut = System.out;
            try {
                System.setOut(new java.io.PrintStream(buffer, true, java.nio.charset.StandardCharsets.UTF_8));
                new PrInterpreter().ejecutar(program);
            } finally {
                System.setOut(originalOut);
            }
            String salida = buffer.toString(java.nio.charset.StandardCharsets.UTF_8);
            if (!salida.contains("(((p → q) ∧ p) → q)  tautology")) {
                System.err.println("modular_inline: se esperaba MP tautology en:\n" + salida);
                return 1;
            }
            if (!salida.contains("contingency")) {
                System.err.println("modular_inline: se esperaba falacia contingency en:\n" + salida);
                return 1;
            }
            return 0;
        } catch (RuntimeException e) {
            System.err.println("modular_inline: " + e.getMessage());
            return 1;
        }
    }
}
