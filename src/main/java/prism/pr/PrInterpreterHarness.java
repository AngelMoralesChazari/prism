package prism.pr;

import prism.pr.ast.PrProgram;

public final class PrInterpreterHarness {

    private PrInterpreterHarness() {
    }

    public static void main(String[] args) {
        int fallos = 0;
        fallos += probarParser();
        fallos += probarEjecucion("examples/ejercicio_lluvia.pr", "contingency", "llueve=p", "paraguas=q");
        fallos += probarEjecucion("examples/tautologia_estudio.pr", "tautology", "estudio=p");
        fallos += probarEjecucion("examples/translate_literal.pr", "contingency", "estudio=p", "apruebo=q");
        if (fallos > 0) {
            System.err.println("PrInterpreterHarness: " + fallos + " fallo(s)");
            System.exit(1);
        }
        System.out.println("PrInterpreterHarness: OK (4 casos)");
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
}
