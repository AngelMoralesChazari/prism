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
    /** Fragmento de texto (candidato a proposición) */
    LITERAL
}
