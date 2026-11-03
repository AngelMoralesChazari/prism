package prism.pr;

public final class PrcCli {

    private static final String VERSION = "1.0.0";

    private PrcCli() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            imprimirAyuda();
            System.exit(1);
        }
        String comando = args[0];
        if ("run".equals(comando)) {
            if (args.length < 2) {
                System.err.println("Uso: prc run <archivo.pr>");
                System.exit(1);
            }
            try {
                new PrInterpreter().ejecutarArchivo(args[1]);
            } catch (PrParseException | PrRuntimeException | PrValidationException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        } else if ("compile".equals(comando)) {
            if (args.length < 2) {
                System.err.println("Uso: prc compile [-d <directorio_salida>] [--keep-java] <archivo.pr>");
                System.exit(1);
            }
            String ruta = null;
            String outDir = null;
            boolean keepJava = false;
            for (int i = 1; i < args.length; i++) {
                if ("--keep-java".equals(args[i])) {
                    keepJava = true;
                } else if ("-d".equals(args[i]) && i + 1 < args.length) {
                    outDir = args[++i];
                } else {
                    ruta = args[i];
                }
            }
            if (ruta == null) {
                System.err.println("Uso: prc compile [-d <directorio_salida>] [--keep-java] <archivo.pr>");
                System.exit(1);
            }
            PrCompiler.CompileResult res = PrCompiler.compile(ruta, outDir, keepJava);
            if (!res.exito()) {
                System.err.println(res.mensaje());
                System.exit(1);
            }
            System.out.println(res.mensaje());
        } else if ("check".equals(comando)) {
            if (args.length < 2) {
                System.err.println("Uso: prc check [--json] <archivo.pr>");
                System.exit(1);
            }
            boolean json = false;
            String ruta = null;
            for (int i = 1; i < args.length; i++) {
                if ("--json".equals(args[i])) {
                    json = true;
                } else {
                    ruta = args[i];
                }
            }
            if (ruta == null) {
                System.err.println("Uso: prc check [--json] <archivo.pr>");
                System.exit(1);
            }
            try {
                if (json) {
                    java.util.List<PrDiagnostic> diags = PrChecker.diagnosticarArchivo(ruta);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < diags.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(diags.get(i).toJson());
                    }
                    sb.append("]");
                    System.out.println(sb);
                    if (diags.stream().anyMatch(d -> "error".equals(d.severity()))) {
                        System.exit(1);
                    }
                } else {
                    if (!PrChecker.checkArchivo(ruta)) {
                        System.exit(1);
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        } else if ("--version".equals(comando) || "-V".equals(comando)) {
            System.out.println("prc " + VERSION);
        } else if ("help".equals(comando) || "--help".equals(comando) || "-h".equals(comando)) {
            imprimirAyuda();
        } else {
            System.err.println("Comando desconocido: " + comando);
            imprimirAyuda();
            System.exit(1);
        }
    }

    private static void imprimirAyuda() {
        System.out.println("Prism compiler & runtime (prc) " + VERSION);
        System.out.println();
        System.out.println("Uso:");
        System.out.println("  prc run <archivo.pr>               Ejecuta un programa .pr (modo interprete)");
        System.out.println("  prc compile <archivo.pr> [-d out]  Compila un programa .pr a bytecode JVM (.class)");
        System.out.println("  prc check <archivo.pr> [--json]    Valida sintaxis y referencias");
        System.out.println("  prc --version                      Muestra la versión oficial");
        System.out.println("  prc help                           Muestra esta ayuda");
    }
}
