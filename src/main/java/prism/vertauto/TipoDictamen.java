package prism.vertauto;

/**
 * Clasificación de una fórmula proposicional según su tabla de verdad.
 */
public enum TipoDictamen {
    TAUTOLOGY("tautology"),
    CONTINGENCY("contingency"),
    CONTRADICTION("contradiction");

    private final String etiquetaCorta;

    TipoDictamen(String etiquetaCorta) {
        this.etiquetaCorta = etiquetaCorta;
    }

    public String etiquetaCorta() {
        return etiquetaCorta;
    }
}
