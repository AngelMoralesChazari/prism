package nlp.lexicon;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Base de conocimiento léxica para Prism:
 * <ol>
 *   <li>{@code lemma} explícitos en {@code .lgs} (prioridad máxima, irregulares y excepciones)</li>
 *   <li>{@link NormalizadorMorfologico} (reglas {@code lexrule} en {@code .lgs} o respaldo embebido)</li>
 *   <li>forma literal sin cambio (fallback)</li>
 * </ol>
 * Los patrones semánticos ({@code pattern}) no sustituyen esta capa: definen la estructura,
 * no la forma de las palabras.
 */

public class BaseConocimiento {
    private static final String RECURSO_LEMAS = "prism/core.pr";

    private final Map<String, String> lemas = new HashMap<>();
    private final Set<String> prefijosPrivativos = new LinkedHashSet<>();
    private final Set<String> exclusionesMorfologicas = new LinkedHashSet<>();
    private final NormalizadorMorfologico normalizadorMorfologico;

    public record ResultadoAntonimo(
            boolean esAntonimo,
            String textoPositivo,
            String formaNegada,
            String formaPositiva
    ) {
        public static ResultadoAntonimo ninguno(String texto) {
            return new ResultadoAntonimo(false, texto, "", "");
        }
    }

    public BaseConocimiento() {
        this(LgsCargador.cargarDesdeClasspath(RECURSO_LEMAS));
    }

    /**
     * Usa el mismo {@link ContenidoLgs} que puede incluir patrones y {@code lexrule}.
     */
    public BaseConocimiento(ContenidoLgs contenido) {
        Objects.requireNonNull(contenido, "contenido");
        Map<String, String> desdeArchivo = contenido.lemas();
        if (!desdeArchivo.isEmpty()) {
            lemas.putAll(desdeArchivo);
        } else {
            registrarLemasIniciales();
        }

        prefijosPrivativos.addAll(contenido.morfologia().prefijosPrivativos());
        exclusionesMorfologicas.addAll(contenido.morfologia().exclusiones());
        normalizadorMorfologico = new NormalizadorMorfologico(contenido.morfologia());
    }

    public Set<String> getPrefijosPrivativos() {
        return Collections.unmodifiableSet(prefijosPrivativos);
    }

    /**
     * Detecta mediante patrones morfológicos de prefijación privativa (des-, in-, im-, i-)
     * si alguna palabra corresponde a un estado de polaridad negativa y deriva su base positiva afirmativa.
     */
    public ResultadoAntonimo detectarYReemplazarAntonimo(String texto) {
        if (texto == null || texto.isBlank()) {
            return ResultadoAntonimo.ninguno(texto);
        }
        String[] tokens = texto.split("\\s+");
        for (String tok : tokens) {
            String limpio = tok.replaceAll("[^\\p{L}\\p{Nd}]", "").toLowerCase();
            if (limpio.length() < 5 || exclusionesMorfologicas.contains(limpio)) {
                continue;
            }
            for (String pref : prefijosPrivativos) {
                if (limpio.startsWith(pref)) {
                    String raiz = limpio.substring(pref.length());
                    if (raiz.length() >= 3 && esRaizPrivativaValida(pref, raiz)) {
                        String nuevo = texto.replaceFirst("\\b" + java.util.regex.Pattern.quote(tok) + "\\b", raiz);
                        return new ResultadoAntonimo(true, nuevo, limpio, raiz);
                    }
                }
            }
        }
        return ResultadoAntonimo.ninguno(texto);
    }

    private static boolean esRaizPrivativaValida(String prefijo, String raiz) {
        // Fonotáctica y restricciones alomórficas del español
        if ("im".equals(prefijo) && !(raiz.startsWith("p") || raiz.startsWith("b"))) {
            return false;
        }
        if ("i".equals(prefijo) && !(raiz.startsWith("l") || raiz.startsWith("r"))) {
            return false;
        }
        if ("in".equals(prefijo) && (raiz.startsWith("p") || raiz.startsWith("b"))) {
            return false;
        }
        // Desinencias morfológicas regulares de adjetivos, participios y estados en español
        return raiz.endsWith("ivo") || raiz.endsWith("iva")
                || raiz.endsWith("ivos") || raiz.endsWith("ivas")
                || raiz.endsWith("ado") || raiz.endsWith("ada")
                || raiz.endsWith("ados") || raiz.endsWith("adas")
                || raiz.endsWith("ido") || raiz.endsWith("ida")
                || raiz.endsWith("idos") || raiz.endsWith("idas")
                || raiz.endsWith("ble") || raiz.endsWith("bles")
                || raiz.endsWith("al") || raiz.endsWith("ales");
    }

    public String canonicalizarFragmento(String fragmento) {
        if (fragmento == null) {
            return "";
        }
        String limpio = fragmento.trim().toLowerCase();
        if (limpio.isEmpty()) {
            return "";
        }

        limpio = java.text.Normalizer.normalize(limpio, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        limpio = limpio.replaceAll("^[\\p{Punct}\\s]+", "").replaceAll("[\\p{Punct}\\s]+$", "");
        limpio = limpio.replaceAll("^(el|la|los|las|un|una|unos|unas)\\s+", "");
        limpio = limpio.replaceAll("[\\p{Punct}\\s]+$", "");
        limpio = limpio.replaceAll("\\s+", " ").trim();

        String[] palabras = limpio.split(" ");
        for (int i = 0; i < palabras.length; i++) {
            String p = palabras[i].replaceAll("^[\\p{Punct}]+", "").replaceAll("[\\p{Punct}]+$", "");
            palabras[i] = canonicalizarPalabra(p);
        }
        return String.join(" ", palabras).trim();
    }

    /**
     * Orden: lemma manual → morfología heurística → identidad.
     */
    String canonicalizarPalabra(String palabra) {
        String lema = lemas.get(palabra);
        if (lema != null) {
            return lema;
        }
        return normalizadorMorfologico.normalizar(palabra);
    }

    /** Respaldo si {@code core.pr} no está en el classpath (p. ej. compilación sin copiar {@code resources}). */
    private void registrarLemasIniciales() {
        lemas.put("llueve", "llover");
        lemas.put("llueva", "llover");
        lemas.put("llevo", "llevar");
        lemas.put("lleva", "llevar");
        lemas.put("apruebo", "aprobar");
        lemas.put("aprueba", "aprobar");
        lemas.put("solea", "hacer_sol");
        lemas.put("hace", "hacer");
        lemas.put("salgo", "salir");
        lemas.put("voy", "ir");
        lemas.put("es", "ser");
        lemas.put("sea", "ser");
        lemas.put("son", "ser");
        lemas.put("sean", "ser");
        lemas.put("era", "ser");
        lemas.put("fue", "ser");
        lemas.put("esta", "estar");
        lemas.put("este", "estar");
        lemas.put("estan", "estar");
        lemas.put("esten", "estar");
    }
}
