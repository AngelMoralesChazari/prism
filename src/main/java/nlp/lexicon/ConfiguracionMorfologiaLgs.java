package nlp.lexicon;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Conjunto de reglas morfológicas y exclusiones cargadas desde {@code .lgs}.
 */
public final class ConfiguracionMorfologiaLgs {

    private final List<ReglaMorfologicaLgs> reglas;
    private final Set<String> exclusiones;
    private final Set<String> prefijosPrivativos;

    public ConfiguracionMorfologiaLgs(List<ReglaMorfologicaLgs> reglas, Set<String> exclusiones) {
        this(reglas, exclusiones, Set.of("des", "in", "im", "i"));
    }

    public ConfiguracionMorfologiaLgs(
            List<ReglaMorfologicaLgs> reglas,
            Set<String> exclusiones,
            Set<String> prefijosPrivativos) {
        this.reglas = reglas == null || reglas.isEmpty() ? List.of() : List.copyOf(reglas);
        this.exclusiones = exclusiones == null || exclusiones.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(exclusiones));
        this.prefijosPrivativos = prefijosPrivativos == null || prefijosPrivativos.isEmpty()
                ? Set.of("des", "in", "im", "i")
                : Collections.unmodifiableSet(new LinkedHashSet<>(prefijosPrivativos));
    }

    public List<ReglaMorfologicaLgs> reglas() {
        return reglas;
    }

    public Set<String> exclusiones() {
        return exclusiones;
    }

    public Set<String> prefijosPrivativos() {
        return prefijosPrivativos;
    }

    public boolean estaVacia() {
        return reglas.isEmpty() && exclusiones.isEmpty() && prefijosPrivativos.isEmpty();
    }

    public static ConfiguracionMorfologiaLgs vacia() {
        return new ConfiguracionMorfologiaLgs(List.of(), Set.of(), Set.of("des", "in", "im", "i"));
    }
}
