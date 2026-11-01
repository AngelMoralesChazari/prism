package nlp.lexicon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Normalización morfológica heurística del español (Fase A/C del léxico Prism).
 * <p>
 * Fase A: reglas embebidas en Java. Fase C: reglas declarativas en {@code core.lgs} vía {@code lexrule};
 * si el archivo no declara ninguna, se usan las reglas predeterminadas embebidas.
 * <p>
 * Documentación: {@code docs/Prism/Lexrule.md}
 */
public final class NormalizadorMorfologico {

    private final List<ReglaMorfologicaLgs> reglas;
    private final Set<String> noVerbo;

    public NormalizadorMorfologico() {
        this(ConfiguracionMorfologiaLgs.vacia());
    }

    public NormalizadorMorfologico(ConfiguracionMorfologiaLgs configuracion) {
        if (configuracion == null || configuracion.estaVacia()) {
            this.reglas = reglasPredeterminadas();
            this.noVerbo = exclusionesPredeterminadas();
        } else {
            this.reglas = configuracion.reglas().isEmpty()
                    ? reglasPredeterminadas()
                    : configuracion.reglas();
            this.noVerbo = configuracion.exclusiones().isEmpty()
                    ? exclusionesPredeterminadas()
                    : configuracion.exclusiones();
        }
    }

    /**
     * @param palabra token en minúsculas sin acentos (salida de {@link nlp.normalizacion.NormalizadorTexto})
     * @return infinitivo aproximado o la palabra original si no hay regla aplicable
     */
    public String normalizar(String palabra) {
        if (palabra == null || palabra.isBlank()) {
            return "";
        }
        String w = palabra.trim().toLowerCase(Locale.ROOT);
        if (w.length() < 3) {
            return w;
        }
        if (esInfinitivo(w)) {
            return w;
        }
        if (noVerbo.contains(w)) {
            return w;
        }

        String porSufijo = aplicarReglas(w);
        if (porSufijo != null) {
            return porSufijo;
        }
        return w;
    }

    private static boolean esInfinitivo(String w) {
        if ("ir".equals(w)) {
            return true;
        }
        return w.length() >= 4 && (w.endsWith("ar") || w.endsWith("er") || w.endsWith("ir"));
    }

    private String aplicarReglas(String w) {
        for (ReglaMorfologicaLgs regla : reglas) {
            String resultado = aplicarRegla(w, regla);
            if (resultado != null) {
                return resultado;
            }
        }
        return null;
    }

    private static String aplicarRegla(String w, ReglaMorfologicaLgs regla) {
        String sufijo = regla.sufijo();
        if (!w.endsWith(sufijo) || w.length() <= sufijo.length() + 1) {
            return null;
        }
        String raiz = w.substring(0, w.length() - sufijo.length());
        if (raiz.length() < 2) {
            return null;
        }

        return switch (regla.tipo()) {
            case HEURISTICA_PRIMERA_PERSONA -> infinitivoDesdePrimeraPersona(raiz);
            case SUFIJO -> {
                if (!cumpleCondicion(raiz, regla.condicion(), regla.patronesRaiz())) {
                    yield null;
                }
                yield raiz + regla.vocalInfinitivo() + "r";
            }
        };
    }

    private static boolean cumpleCondicion(
            String raiz, ReglaMorfologicaLgs.Condicion condicion, List<String> patronesRaiz) {
        return switch (condicion) {
            case NINGUNA -> true;
            case RAIZ_VOCAL -> !raiz.isEmpty() && esVocal(raiz.charAt(raiz.length() - 1));
            case RAIZ_TERMINA -> {
                boolean coincide = false;
                for (String patron : patronesRaiz) {
                    if (raiz.endsWith(patron)) {
                        coincide = true;
                        break;
                    }
                }
                yield coincide;
            }
            case RAIZ_CONSONANTE -> !raiz.isEmpty() && !esVocal(raiz.charAt(raiz.length() - 1));
        };
    }

    /**
     * Presente 1.ª persona {@code -o}: elige infinitivo según la raíz.
     * Usado por {@code lexrule sufijo o heuristica primera_persona}.
     */
    private static String infinitivoDesdePrimeraPersona(String raiz) {
        if (raiz.isEmpty()) {
            return null;
        }
        char ultima = raiz.charAt(raiz.length() - 1);
        if (esVocal(ultima)) {
            return raiz + "ar";
        }
        if (raiz.endsWith("rm") || raiz.endsWith("mm") || raiz.endsWith("rc") || raiz.endsWith("rt")) {
            return raiz + "ir";
        }
        String candidatoAr = raiz + "ar";
        if (candidatoAr.length() >= 4) {
            return candidatoAr;
        }
        return raiz + "ir";
    }

    private static boolean esVocal(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static Set<String> exclusionesPredeterminadas() {
        return Set.of(
                "gorra",
                "sombrero",
                "paraguas",
                "calor",
                "frio",
                "sol",
                "cielo",
                "nube",
                "nubes",
                "lluvia",
                "examen",
                "clase");
    }

    private static List<ReglaMorfologicaLgs> reglasPredeterminadas() {
        List<ReglaMorfologicaLgs> reglas = new ArrayList<>();
        reglas.add(ReglaMorfologicaLgs.sufijo("iaria", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("aria", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("iaba", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("abas", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("aban", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("amos", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("ais", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("aron", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("aran", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("are", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("aba", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("ado", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("ada", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("emos", 'e'));
        reglas.add(ReglaMorfologicaLgs.sufijo("eis", 'e'));
        reglas.add(ReglaMorfologicaLgs.sufijo("imos", 'i'));
        reglas.add(ReglaMorfologicaLgs.sufijo("an", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("as", 'a'));
        reglas.add(ReglaMorfologicaLgs.sufijo("a", 'a'));
        reglas.add(ReglaMorfologicaLgs.heuristicaPrimeraPersona("o"));
        return reglas;
    }
}
