package prism.pr;

public final class PrcCli {

    private static final String VERSION = "0.2.0";

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
        } else if ("check".equals(comando)) {
            if (args.length < 2) {
                System.err.println("Uso: prc check <archivo.pr>");
                System.exit(1);
            }
            try {
                if (!PrChecker.checkArchivo(args[1])) {
                    System.exit(1);
                }
            } catch (PrParseException e) {
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
        System.out.println("Prism compiler / intérprete (prc) " + VERSION);
        System.out.println();
        System.out.println("Uso:");
        System.out.println("  prc run <archivo.pr>    Ejecuta un programa .pr");
        System.out.println("  prc check <archivo.pr>  Valida sintaxis y referencias (sin ejecutar)");
        System.out.println("  prc --version           Muestra la versión");
        System.out.println("  prc help                Muestra esta ayuda");
    }
}
