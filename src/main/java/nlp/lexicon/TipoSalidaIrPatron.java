package nlp.lexicon;

/**
 * Conectiva de la IR que produce un patrón semántico cargado desde {@code .lgs}.
 */
public enum TipoSalidaIrPatron {
    /** Implicación ({@code ImpExpr}): antecedente → consecuente. */
    IMP,
    /** Conjunción ({@code AndExpr}). */
    AND,
    /** Disyunción ({@code OrExpr}). */
    OR,
    /** Equivalencia ({@code EquivExpr}). */
    EQUIV,
    /** Implicación con antecedente conjunción: ({@code AndExpr}) → consecuente. */
    IMP_AND,
    /** Implicación con consecuente disyunción: antecedente → ({@code OrExpr}). */
    IMP_OR,
    /** Implicación con antecedente disyunción: ({@code OrExpr}) → consecuente. */
    IMP_OR_ANT,
    /** Implicación con consecuente conjunción: antecedente → ({@code AndExpr}). */
    IMP_AND_CONS,
    /** Excepción (*a menos que*): ¬right → left. */
    IMP_UNLESS
}
