package nlp.lexicon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Normalización morfológica heurística del español (Fase A/C del léxico Prism).
 * <p>
 * Fase A: reglas embebidas en Java. Fase C: reglas declarativas en {@code core.pr} vía {@code lexrule};
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

    /**
     * Determina si dos palabras son flexiones verbales regulares del mismo verbo en español,
     * atendiendo a la alternancia sistemática indicativo/subjuntivo (e.g. suspende <-> suspenda,
     * reintenta <-> reintente, optimiza <-> optimice) y número (singular/plural).
     */
    public static boolean sonFlexionesEquivalentes(String w1, String w2) {
        if (w1 == null || w2 == null) {
            return false;
        }
        String a = w1.trim().toLowerCase(Locale.ROOT);
        String b = w2.trim().toLowerCase(Locale.ROOT);
        if (a.equals(b)) {
            return true;
        }
        if (a.length() < 3 || b.length() < 3) {
            return false;
        }

        // 1. Alternancia 3.ª persona singular: -a <-> -e
        if (coincidenAlternancia(a, b, "a", "e") || coincidenAlternancia(a, b, "e", "a")) {
            return true;
        }

        // 2. Alternancia 3.ª persona plural: -an <-> -en
        if (coincidenAlternancia(a, b, "an", "en") || coincidenAlternancia(a, b, "en", "an")) {
            return true;
        }

        // 3. Alternancia 1.ª persona presente (-o) <-> subjuntivo (-a / -e)
        if (coincidenAlternancia(a, b, "o", "a") || coincidenAlternancia(a, b, "a", "o")
                || coincidenAlternancia(a, b, "o", "e") || coincidenAlternancia(a, b, "e", "o")) {
            return true;
        }

        // 4. Alternancias entre formas en infinitivo (-ar / -er) y formas conjugadas (-en, -an, -e, -a)
        if (coincidenAlternancia(a, b, "ar", "en") || coincidenAlternancia(a, b, "en", "ar")
                || coincidenAlternancia(a, b, "er", "an") || coincidenAlternancia(a, b, "an", "er")
                || coincidenAlternancia(a, b, "ar", "e") || coincidenAlternancia(a, b, "e", "ar")
                || coincidenAlternancia(a, b, "er", "a") || coincidenAlternancia(a, b, "a", "er")
                || coincidenAlternancia(a, b, "ar", "er") || coincidenAlternancia(a, b, "er", "ar")) {
            return true;
        }

        return false;
    }

    private static boolean coincidenAlternancia(String w1, String w2, String suf1, String suf2) {
        if (!w1.endsWith(suf1) || !w2.endsWith(suf2)) {
            return false;
        }
        String raiz1 = w1.substring(0, w1.length() - suf1.length());
        String raiz2 = w2.substring(0, w2.length() - suf2.length());
        if (raiz1.length() < 2 || raiz2.length() < 2) {
            return false;
        }

        // Raíz idéntica (e.g. suspend- <-> suspend-, reintent- <-> reintent-)
        if (raiz1.equals(raiz2)) {
            return true;
        }

        // Alternancias ortográficas regulares ante vocal e/i vs a/o:
        // c <-> z (optimic-e <-> optimiz-a, organic-e <-> organiz-a)
        if (raiz1.endsWith("z") && raiz2.endsWith("c")) {
            if (raiz1.substring(0, raiz1.length() - 1).equals(raiz2.substring(0, raiz2.length() - 1))) {
                return true;
            }
        }
        if (raiz1.endsWith("c") && raiz2.endsWith("z")) {
            if (raiz1.substring(0, raiz1.length() - 1).equals(raiz2.substring(0, raiz2.length() - 1))) {
                return true;
            }
        }

        // g <-> gu (apag-a <-> apagu-e, lleg-a <-> llegu-e)
        if (raiz1.endsWith("g") && raiz2.endsWith("gu")) {
            if (raiz1.substring(0, raiz1.length() - 1).equals(raiz2.substring(0, raiz2.length() - 2))) {
                return true;
            }
        }
        if (raiz1.endsWith("gu") && raiz2.endsWith("g")) {
            if (raiz1.substring(0, raiz1.length() - 2).equals(raiz2.substring(0, raiz2.length() - 1))) {
                return true;
            }
        }

        // c <-> qu (provoc-a <-> provoqu-e, arranc-a <-> arranqu-e)
        if (raiz1.endsWith("c") && raiz2.endsWith("qu")) {
            if (raiz1.substring(0, raiz1.length() - 1).equals(raiz2.substring(0, raiz2.length() - 2))) {
                return true;
            }
        }
        if (raiz1.endsWith("qu") && raiz2.endsWith("c")) {
            if (raiz1.substring(0, raiz1.length() - 2).equals(raiz2.substring(0, raiz2.length() - 1))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determina si dos cláusulas o proposiciones son semánticamente equivalentes
     * permitiendo que los verbos conjugados alternen entre modo indicativo y subjuntivo,
     * elisiones de sujeto en predicados coordinados o reducción anafórica de complementos del nombre.
     */
    public static boolean sonFrasesEquivalentes(String f1, String f2) {
        if (f1 == null || f2 == null) {
            return false;
        }
        String s1 = f1.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        String s2 = f2.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (s1.equals(s2)) {
            return true;
        }

        s1 = s1.replaceAll("^(el|la|los|las|un|una|unos|unas)\\s+", "");
        s2 = s2.replaceAll("^(el|la|los|las|un|una|unos|unas)\\s+", "");
        if (s1.equals(s2)) {
            return true;
        }

        // Sujeto elíptico compartido en predicados coordinados (p. ej. "permanece estable" <-> "cluster permanece estable")
        if (s1.length() >= 5 && s2.length() >= 5 && (s1.endsWith(" " + s2) || s2.endsWith(" " + s1))) {
            return true;
        }

        String[] p1 = s1.split(" ");
        String[] p2 = s2.split(" ");
        if (sonPalabrasEquivalentes(p1, p2)) {
            return true;
        }

        // Reducción anafórica del complemento del nombre (p. ej. "token de autenticacion expira" <-> "token expire")
        String patronDe = "\\b(?:de|del)\\s+(?:(?:el|la|los|las|un|una|unos|unas)\\s+)?[\\w]+\\b";
        String red1 = s1.replaceAll(patronDe, " ").replaceAll("\\s+", " ").trim();
        String red2 = s2.replaceAll(patronDe, " ").replaceAll("\\s+", " ").trim();
        if (!red1.equals(s1) || !red2.equals(s2)) {
            if (sonPalabrasEquivalentes(red1.split(" "), red2.split(" "))) {
                return true;
            }
        }

        // Reducción de modificadores adverbiales accesorios (p. ej. "responde rapidamente" <-> "responde")
        String adv1 = eliminarAdverbiosAccesorios(s1);
        String adv2 = eliminarAdverbiosAccesorios(s2);
        if (!adv1.equals(s1) || !adv2.equals(s2)) {
            if (adv1.equals(adv2)) {
                return true;
            }
            if (adv1.length() >= 5 && adv2.length() >= 5 && (adv1.endsWith(" " + adv2) || adv2.endsWith(" " + adv1))) {
                return true;
            }
            if (sonPalabrasEquivalentes(adv1.split(" "), adv2.split(" "))) {
                return true;
            }
            // Combinación: reducción de modificadores adverbiales + complemento del nombre (de X)
            String redAdv1 = adv1.replaceAll(patronDe, " ").replaceAll("\\s+", " ").trim();
            String redAdv2 = adv2.replaceAll(patronDe, " ").replaceAll("\\s+", " ").trim();
            if (sonPalabrasEquivalentes(redAdv1.split(" "), redAdv2.split(" "))) {
                return true;
            }
        }

        return false;
    }

    // --- Módulo de Modificadores Adverbiales Accesorios ---

    private static final java.util.regex.Pattern PATRON_LOCUCION_MODAL = java.util.regex.Pattern.compile(
            "\\bde\\s+(?:manera|forma|modo)\\s+(?:(?:muy|tan|bastante)\\s+)?[\\w]+\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    private static final java.util.regex.Pattern PATRON_INTENSIFICADOR_MENTE = java.util.regex.Pattern.compile(
            "\\b(?:muy|tan|bastante)\\s+(?=[\\w]*mente\\b)",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    /**
     * Determina morfológicamente si una palabra corresponde a un modificador adverbial accesorio
     * derivado sobre base adjetival con sufijo regular {@code -mente} (e.g. rápida-mente, exitosa-mente).
     * Discrimina formalmente frente a conjugaciones verbales en subjuntivo de verbos en {@code -mentar}
     * (e.g. experimente, implemente, documente).
     */
    public static boolean esAdverbioAccesorio(String palabra) {
        if (palabra == null || palabra.length() < 7) {
            return false;
        }
        String p = palabra.toLowerCase(Locale.ROOT);
        if (!p.endsWith("mente")) {
            return false;
        }
        // Desinencias morfológicas regulares del adjetivo antepuesto al sufijo -mente
        return p.endsWith("amente")
                || p.endsWith("lmente")
                || p.endsWith("temente")
                || p.endsWith("blemente")
                || p.endsWith("zmente")
                || p.endsWith("rmente")
                || p.endsWith("simamente");
    }

    /**
     * Abstrae modificadores adverbiales de modo o tiempo que actúan como adjuntos verbales
     * accesorios en proposiciones atómicas.
     */
    public static String eliminarAdverbiosAccesorios(String frase) {
        if (frase == null || frase.isBlank()) {
            return frase;
        }
        String limpia = PATRON_LOCUCION_MODAL.matcher(frase).replaceAll(" ");
        limpia = PATRON_INTENSIFICADOR_MENTE.matcher(limpia).replaceAll(" ");
        String[] palabras = limpia.trim().split("\\s+");
        java.util.List<String> filtradas = new java.util.ArrayList<>();
        for (String p : palabras) {
            String pLimpia = p.replaceAll("^[\\p{Punct}]+", "").replaceAll("[\\p{Punct}]+$", "");
            if (!esAdverbioAccesorio(pLimpia)) {
                filtradas.add(p);
            }
        }
        if (filtradas.isEmpty()) {
            return frase;
        }
        return String.join(" ", filtradas).replaceAll("\\s+", " ").trim();
    }

    private static boolean sonPalabrasEquivalentes(String[] p1, String[] p2) {
        if (p1.length != p2.length) {
            return false;
        }
        int diferencias = 0;
        for (int i = 0; i < p1.length; i++) {
            if (p1[i].equals(p2[i])) {
                continue;
            }
            if (sonFlexionesEquivalentes(p1[i], p2[i])) {
                diferencias++;
            } else {
                return false;
            }
        }
        return diferencias <= 2;
    }

    // --- Módulo de Correferencia Pronominal y Anáfora ---

    private static final Set<String> PRONOMBRES_PERSONALES = Set.of(
            "el", "ella", "ellos", "ellas", "ello"
    );

    private static final Set<String> PRONOMBRES_DEMOSTRATIVOS = Set.of(
            "este", "esta", "estos", "estas", "esto",
            "ese", "esa", "esos", "esas", "eso",
            "aquel", "aquella", "aquellos", "aquellas", "aquello"
    );

    private static final Set<String> DETERMINATIVOS_ANAFORICOS = Set.of(
            "dicho", "dicha", "dichos", "dichas", "mismo", "misma", "mismos", "mismas"
    );

    public static boolean esPronombreAnaforico(String palabra) {
        if (palabra == null || palabra.isBlank()) {
            return false;
        }
        String p = palabra.trim().toLowerCase(Locale.ROOT);
        return PRONOMBRES_PERSONALES.contains(p)
                || PRONOMBRES_DEMOSTRATIVOS.contains(p)
                || DETERMINATIVOS_ANAFORICOS.contains(p);
    }

    public enum GeneroGramatical { MASCULINO, FEMENINO, NEUTRO, INDIFERENTE }
    public enum NumeroGramatical { SINGULAR, PLURAL, INDIFERENTE }

    public static GeneroGramatical generoPronombre(String p) {
        String w = p.trim().toLowerCase(Locale.ROOT);
        if (w.equals("ella") || w.equals("ellas") || w.equals("esta") || w.equals("estas")
                || w.equals("esa") || w.equals("esas") || w.equals("aquella") || w.equals("aquellas")
                || w.equals("dicha") || w.equals("dichas") || w.equals("misma") || w.equals("mismas")) {
            return GeneroGramatical.FEMENINO;
        }
        if (w.equals("el") || w.equals("ellos") || w.equals("este") || w.equals("estos")
                || w.equals("ese") || w.equals("esos") || w.equals("aquel") || w.equals("aquellos")
                || w.equals("dicho") || w.equals("dichos") || w.equals("mismo") || w.equals("mismos")) {
            return GeneroGramatical.MASCULINO;
        }
        return GeneroGramatical.NEUTRO;
    }

    public static NumeroGramatical numeroPronombre(String p) {
        String w = p.trim().toLowerCase(Locale.ROOT);
        if (w.endsWith("s")) {
            return NumeroGramatical.PLURAL;
        }
        return NumeroGramatical.SINGULAR;
    }

    public static boolean concuerdaGeneroYNumero(String pronombre, String sujetoAntecedente) {
        return concuerdaGeneroYNumero(pronombre, sujetoAntecedente, false, false);
    }

    public static boolean concuerdaGeneroYNumero(
            String pronombre, String sujetoAntecedente, boolean articuloMasc, boolean articuloFem) {
        GeneroGramatical gP = generoPronombre(pronombre);
        NumeroGramatical nP = numeroPronombre(pronombre);
        if (gP == GeneroGramatical.NEUTRO || gP == GeneroGramatical.INDIFERENTE) {
            return true;
        }

        String s = sujetoAntecedente.trim().toLowerCase(Locale.ROOT);
        boolean esPluralSujeto = s.endsWith("s") || s.contains("los ") || s.contains("las ") || s.contains("unos ") || s.contains("unas ");
        if (nP == NumeroGramatical.PLURAL && !esPluralSujeto) {
            return false;
        }
        if (nP == NumeroGramatical.SINGULAR && esPluralSujeto) {
            return false;
        }

        boolean esFemeninoSujeto;
        if (articuloMasc) {
            esFemeninoSujeto = false;
        } else if (articuloFem) {
            esFemeninoSujeto = true;
        } else {
            // Sin artículo explícito: sustantivos en -ma (de origen griego: el sistema, el programa) son masculinos
            boolean esMasculinoMa = s.endsWith("ma") || s.endsWith("mas");
            if (esMasculinoMa) {
                esFemeninoSujeto = false;
            } else {
                esFemeninoSujeto = s.startsWith("la ") || s.startsWith("una ") || s.startsWith("las ") || s.startsWith("unas ")
                        || s.endsWith("a") || s.endsWith("as") || s.contains(" cion") || s.contains(" dad") || s.contains(" tud");
            }
        }

        if (gP == GeneroGramatical.FEMENINO) {
            return esFemeninoSujeto;
        }
        if (gP == GeneroGramatical.MASCULINO) {
            return !esFemeninoSujeto;
        }
        return true;
    }

    /**
     * Determina si una cláusula con sujeto pronominal anafórico (e.g. "este se suspenda",
     * "ella se reintente", "el libera la memoria") corresponde al átomo antecedente.
     */
    public static boolean coincideAnafora(String fraseConPronombre, String etiquetaAntecedente) {
        if (fraseConPronombre == null || etiquetaAntecedente == null) {
            return false;
        }
        String sPronombre = fraseConPronombre.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        String sAntecedenteRaw = etiquetaAntecedente.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        boolean articuloMasc = sAntecedenteRaw.matches("^(el|un|los|unos)\\s+.*");
        boolean articuloFem = sAntecedenteRaw.matches("^(la|una|las|unas)\\s+.*");

        String sAntecedente = sAntecedenteRaw.replaceAll("^(el|la|los|las|un|una|unos|unas)\\s+", "");

        String[] palabrasPronombre = sPronombre.split(" ");
        if (palabrasPronombre.length < 2) {
            return false;
        }

        String pronombre = palabrasPronombre[0];
        if (!esPronombreAnaforico(pronombre)) {
            return false;
        }

        String predicadoPronombre = String.join(" ", java.util.Arrays.copyOfRange(palabrasPronombre, 1, palabrasPronombre.length));

        String[] palabrasAntecedente = sAntecedente.split(" ");
        if (palabrasAntecedente.length < 2) {
            return false;
        }

        for (int i = 1; i < palabrasAntecedente.length; i++) {
            String subPredicado = String.join(" ", java.util.Arrays.copyOfRange(palabrasAntecedente, i, palabrasAntecedente.length));
            if (sonFrasesEquivalentes(predicadoPronombre, subPredicado)) {
                String sujetoAntecedente = String.join(" ", java.util.Arrays.copyOfRange(palabrasAntecedente, 0, i));
                if (concuerdaGeneroYNumero(pronombre, sujetoAntecedente, articuloMasc, articuloFem)) {
                    return true;
                }
            }
        }

        return false;
    }
}
