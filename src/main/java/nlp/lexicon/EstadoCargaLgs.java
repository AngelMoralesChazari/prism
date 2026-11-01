package nlp.lexicon;

/**
 * Resultado de intentar cargar un archivo {@code .lgs} desde el classpath.
 */
public enum EstadoCargaLgs {
    /** Archivo leído y parseado sin errores. */
    EXITO,
    /** El recurso no está en el classpath (p. ej. {@code out} sin copiar {@code resources}). */
    RECURSO_NO_ENCONTRADO,
    /** Sintaxis inválida en una línea útil (mensaje incluye número de línea). */
    ERROR_SINTAXIS,
    /** Fallo de lectura del stream distinto al parseo. */
    ERROR_LECTURA
}
