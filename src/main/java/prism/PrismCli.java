package prism;

public final class PrismCli {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java -cp <out> prism.PrismCli \"<enunciado en lenguaje natural>\"");
            System.exit(1);
        }
        String enunciado = String.join(" ", args).trim();
        if (enunciado.isEmpty()) {
            System.err.println("Enunciado vacío.");
            System.exit(1);
        }

        PrismService servicio = new PrismService();
        PrismResult resultado = servicio.traducir(enunciado);

        System.out.println("=== Prism ===");
        System.out.println("Entrada: " + enunciado);
        System.out.println();

        if (!resultado.isExito()) {
            System.out.println("Estado: ERROR");
            System.out.println(resultado.getMensaje());
            for (String paso : resultado.getPasosDeAnalisis()) {
                System.out.println("  - " + paso);
            }
            System.exit(2);
        }

        String formula = resultado.getFormula();
        System.out.println("Estado: OK");
        System.out.println("Fórmula: " + formula);
        System.out.println("Proposiciones: " + resultado.getProposiciones());
        System.out.println("Pasos de análisis:");
        for (String paso : resultado.getPasosDeAnalisis()) {
            System.out.println("  - " + paso);
        }
    }
}
