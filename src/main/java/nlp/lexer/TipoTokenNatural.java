package nlp.lexer;

/**
 * Clasificación mínima de unidades léxicas para diagnóstico y futuros parsers;
 * el motor de patrones sigue operando sobre la cadena normalizada.
 */
public enum TipoTokenNatural {
    /** Conector condicional inicial */
    SI,
    /** Conector de consecuencia */
    ENTONCES,
    /** Locución de condición alternativa */
    EN_CASO_DE_QUE,
    /** Condición equivalente (*si y solo si*) */
    SI_Y_SOLO_SI,
    /** Condición suficiente (*siempre que*) */
    SIEMPRE_QUE,
    /** Condición temporal (*cuando*), lectura ≈ implicación */
    CUANDO,
    /** Condición necesaria (*solo si*): *P solo si Q* → *P → Q* */
    SOLO_SI,
    /** Excepción (*a menos que*): *P a menos que Q* → *¬Q → P* */
    A_MENOS_QUE,
    /** Conjunción coordinante */
    Y,
    /** Disyunción coordinante */
    O,
    /** Conjunción negativa (*ni P ni Q*) */
    NI,
    /** Conector condicional/causal (*dado que*) */
    DADO_QUE,
    /** Conector condicional/causal (*puesto que*) */
    PUESTO_QUE,
    /** Conector condicional/causal (*ya que*) */
    YA_QUE,
    /** Disyunción (*o bien*) */
    O_BIEN,
    /** Negación sentencial (*no es cierto que* / *no es verdad que*) */
    NO_ES_CIERTO_QUE,
    /** Fragmento de texto (candidato a proposición) */
    LITERAL
}
