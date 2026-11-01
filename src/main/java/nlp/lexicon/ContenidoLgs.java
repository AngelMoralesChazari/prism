package nlp.lexicon;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Resultado de parsear un archivo {@code .lgs}: lemas, patrones semánticos y reglas morfológicas.
 */
public final class ContenidoLgs {

    private final Map<String, String> lemas;
    private final List<PatronSemanticoLgs> patronesSemanticos;
    private final ConfiguracionMorfologiaLgs morfologia;
    private final Map<String, String> antonimos;

    public ContenidoLgs(Map<String, String> lemas, List<PatronSemanticoLgs> patronesSemanticos) {
        this(lemas, patronesSemanticos, ConfiguracionMorfologiaLgs.vacia(), Map.of());
    }

    public ContenidoLgs(
            Map<String, String> lemas,
            List<PatronSemanticoLgs> patronesSemanticos,
            ConfiguracionMorfologiaLgs morfologia) {
        this(lemas, patronesSemanticos, morfologia, Map.of());
    }

    public ContenidoLgs(
            Map<String, String> lemas,
            List<PatronSemanticoLgs> patronesSemanticos,
            ConfiguracionMorfologiaLgs morfologia,
            Map<String, String> antonimos) {
        this.lemas = lemas == null || lemas.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(Map.copyOf(lemas));
        this.patronesSemanticos = patronesSemanticos == null || patronesSemanticos.isEmpty()
                ? List.of()
                : List.copyOf(patronesSemanticos);
        this.morfologia = morfologia == null ? ConfiguracionMorfologiaLgs.vacia() : morfologia;
        this.antonimos = antonimos == null || antonimos.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(Map.copyOf(antonimos));
    }

    public Map<String, String> lemas() {
        return lemas;
    }

    public List<PatronSemanticoLgs> patronesSemanticos() {
        return patronesSemanticos;
    }

    public ConfiguracionMorfologiaLgs morfologia() {
        return morfologia;
    }

    public Map<String, String> antonimos() {
        return antonimos;
    }

    public static ContenidoLgs vacio() {
        return new ContenidoLgs(Map.of(), List.of(), ConfiguracionMorfologiaLgs.vacia(), Map.of());
    }
}
