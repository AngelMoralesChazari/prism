package nlp.lexicon;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private final NormalizadorMorfologico normalizadorMorfologico;

    public BaseConocimiento() {
        this(LgsCargador.cargarDesdeClasspath(RECURSO_LEMAS));
    }

    /**
     * Usa el mismo {@link ContenidoLgs} que puede incluir patrones y {@code lexrule}; aquí se consumen lemas y morfología.
     */
    public BaseConocimiento(ContenidoLgs contenido) {
        Objects.requireNonNull(contenido, "contenido");
        Map<String, String> desdeArchivo = contenido.lemas();
        if (!desdeArchivo.isEmpty()) {
            lemas.putAll(desdeArchivo);
        } else {
            registrarLemasIniciales();
        }
        normalizadorMorfologico = new NormalizadorMorfologico(contenido.morfologia());
    }

    public String canonicalizarFragmento(String fragmento) {
        if (fragmento == null) {
            return "";
        }
        String limpio = fragmento.trim().toLowerCase();
        if (limpio.isEmpty()) {
            return "";
        }

        limpio = limpio.replaceAll("^(el|la|los|las|un|una|unos|unas)\\s+", "");
        limpio = limpio.replaceAll("\\s+", " ").trim();

        String[] palabras = limpio.split(" ");
        for (int i = 0; i < palabras.length; i++) {
            palabras[i] = canonicalizarPalabra(palabras[i]);
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
    }
}
