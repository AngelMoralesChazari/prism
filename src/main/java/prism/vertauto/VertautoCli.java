package prism.vertauto;

/**
 * CLI de demostración para {@code vertauto} (Fase 2).
 *
 * <pre>
 * java -cp out prism.vertauto.VertautoCli "(p → q)"
 * java -cp out prism.vertauto.VertautoCli --expr "p or not p"
 * java -cp out prism.vertauto.VertautoCli --from-nl "si estudio apruebo"
 * java -cp out prism.vertauto.VertautoCli --expr "p or not p" --extended
 * </pre>
 */
public final class VertautoCli {

    public static void main(String[] args) {
        if (args.length == 0) {
            imprimirUso();
            System.exit(1);
        }

        boolean extended = false;
        String modo = "clasica";
        String entrada = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--extended".equals(a) || "-v".equals(a)) {
                extended = true;
            } else if ("--expr".equals(a) && i + 1 < args.length) {
                modo = "expr";
                entrada = args[++i];
            } else if ("--from-nl".equals(a) && i + 1 < args.length) {
                modo = "nl";
                entrada = args[++i];
            } else if (!a.startsWith("-") && entrada == null) {
                entrada = a;
            }
        }

        if (entrada == null || entrada.isBlank()) {
            imprimirUso();
            System.exit(1);
        }

        VertautoService servicio = new VertautoService();
        try {
            VertautoResult resultado = switch (modo) {
                case "expr" -> servicio.vertautoExpr(entrada);
                case "nl" -> servicio.vertautoDesdeNl(entrada);
                default -> servicio.vertautoClasica(entrada);
            };

            if (extended) {
                System.out.println("=== vertauto (extendido) ===");
                resultado.imprimirExtendido(true, true, true);
            } else {
                resultado.imprimirModoCorto();
            }
        } catch (IllegalArgumentException ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(2);
        }
    }

    private static void imprimirUso() {
        System.err.println("Uso:");
        System.err.println("  VertautoCli \"(p → q)\"");
        System.err.println("  VertautoCli --expr \"p or not p\"");
        System.err.println("  VertautoCli --from-nl \"si llueve llevo paraguas\" --extended");
    }
}
