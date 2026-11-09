package nlp.normalizacion;

import java.text.Normalizer;

/**
 * Normalización previa al análisis LN: minúsculas, sin acentos diacríticos
 * combinados,
 * puntuación básica y espacios. Punto único de verdad para el pipeline Prism /
 * NLP.
 */
public final class NormalizadorTexto {

    public String normalizar(String textoOriginal) {
        if (textoOriginal == null) {
            return "";
        }
        String t = textoOriginal.trim().toLowerCase();
        if (t.isEmpty()) {
            return "";
        }
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        t = t.replaceAll(":+", ",");
        t = t.replaceAll("[.!?]+$", "");
        t = t.replaceAll("\\s*,\\s*", ", ");
        t = t.replaceAll("\\s*;+\\s*", "; ");
        t = t.replaceAll("(?<![0-9])\\s*\\.+\\s*(?![0-9])", ". ");
        t = t.replaceAll("[.!?,;]+$", "");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }
}
