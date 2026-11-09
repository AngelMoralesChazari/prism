package prism.vertauto;

/**
 * Pruebas de regresión de {@link VertautoService}.
 * Ejecutar: {@code java -cp out prism.vertauto.VertautoHarness}
 */
public final class VertautoHarness {

        public static void main(String[] args) {
                int fallos = 0;
                VertautoService svc = new VertautoService();

                fallos += verificar(
                                "tautologia_p_or_not_p",
                                () -> {
                                        VertautoResult r = svc.vertautoExpr("p or not p");
                                        return r.getDictamen() == TipoDictamen.TAUTOLOGY;
                                });

                fallos += verificar(
                                "contingencia_p_impl_q",
                                () -> {
                                        VertautoResult r = svc.vertautoClasica("(p → q)");
                                        return r.getDictamen() == TipoDictamen.CONTINGENCY;
                                });

                fallos += verificar(
                                "contradiccion_p_and_not_p",
                                () -> {
                                        VertautoResult r = svc.vertautoExpr("p and not p");
                                        return r.getDictamen() == TipoDictamen.CONTRADICTION;
                                });

                fallos += verificar(
                                "modo_corto_formato",
                                () -> {
                                        VertautoResult r = svc.vertautoExpr("p or not p");
                                        String corto = r.formatoCorto();
                                        return corto.endsWith("tautology");
                                });

                fallos += verificar(
                                "integracion_nl_implicacion",
                                () -> {
                                        VertautoResult r = svc.vertautoDesdeNl("si estudio apruebo");
                                        return r.getDictamen() == TipoDictamen.CONTINGENCY
                                                        && r.getFormula().contains("→");
                                });

                fallos += verificar(
                                "tabla_tiene_filas",
                                () -> {
                                        VertautoResult r = svc.vertautoExpr("p or not p");
                                        return r.getFilasTabla().size() >= 3;
                                });

                fallos += verificar(
                                "nl_incluye_pasos_traduccion",
                                () -> {
                                        VertautoResult r = svc.vertautoDesdeNl("si llueve llevo paraguas");
                                        return !r.getPasosTraduccion().isEmpty()
                                                        && r.getPasosTraduccion().stream()
                                                                        .anyMatch(p -> p.contains("Lexemas"));
                                });

                fallos += verificar(
                                "nl_modus_ponens_tautology",
                                () -> svc.vertautoDesdeNl(
                                                "Si llueve entonces la calle se moja; llueve; por lo tanto la calle se moja.")
                                                .getDictamen() == TipoDictamen.TAUTOLOGY);

                fallos += verificar(
                                "nl_modus_tollens_tautology",
                                () -> svc.vertautoDesdeNl(
                                                "Si llueve entonces la calle se moja; la calle no se moja; por lo tanto no llueve.")
                                                .getDictamen() == TipoDictamen.TAUTOLOGY);

                fallos += verificar(
                                "nl_silogismo_hipotetico_tautology",
                                () -> svc.vertautoDesdeNl(
                                                "Si estudio entonces aprendo; si aprendo entonces apruebo; por lo tanto si estudio entonces apruebo.")
                                                .getDictamen() == TipoDictamen.TAUTOLOGY);

                fallos += verificar(
                                "nl_silogismo_disyuntivo_tautology",
                                () -> svc.vertautoDesdeNl("Estudio o trabajo; no estudio; por lo tanto trabajo.")
                                                .getDictamen() == TipoDictamen.TAUTOLOGY);

                fallos += verificar(
                                "nl_de_morgan_subjuntivo_tautology",
                                () -> svc.vertautoDesdeNl(
                                                "No es verdad que llueva o duerma; por lo tanto no llueve y no duerme.")
                                                .getDictamen() == TipoDictamen.TAUTOLOGY);

                fallos += verificar(
                                "nl_contraposicion_tautology",
                                () -> svc.vertautoDesdeNl(
                                                "Si llueve entonces la calle se moja si y solo si si la calle no se moja entonces no llueve.")
                                                .getDictamen() == TipoDictamen.TAUTOLOGY);

                fallos += verificar(
                                "nl_falacia_afirmacion_consecuente_contingency",
                                () -> svc.vertautoDesdeNl(
                                                "Si llueve entonces la calle se moja; la calle se moja; por lo tanto llueve.")
                                                .getDictamen() == TipoDictamen.CONTINGENCY);

                fallos += verificar(
                                "nl_falacia_negacion_antecedente_contingency",
                                () -> svc.vertautoDesdeNl(
                                                "Si llueve entonces la calle se moja; no llueve; por lo tanto la calle no se moja.")
                                                .getDictamen() == TipoDictamen.CONTINGENCY);

                if (fallos == 0) {
                        System.out.println("VertautoHarness: OK (15 casos).");
                } else {
                        System.err.println("VertautoHarness: " + fallos + " fallo(s).");
                        System.exit(1);
                }
        }

        private static int verificar(String id, Prueba prueba) {
                try {
                        if (prueba.ejecutar()) {
                                return 0;
                        }
                        System.err.println("FALLO: " + id);
                        return 1;
                } catch (Exception ex) {
                        System.err.println("FALLO: " + id + " — " + ex.getMessage());
                        return 1;
                }
        }

        @FunctionalInterface
        private interface Prueba {
                boolean ejecutar();
        }
}
