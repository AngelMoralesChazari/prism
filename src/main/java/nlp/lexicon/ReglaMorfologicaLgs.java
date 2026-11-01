package nlp.lexicon;

import java.util.List;
import java.util.Objects;

/**
 * Regla morfológica declarativa en {@code .lgs} (directiva {@code lexrule}, Fase C).
 * El orden en el archivo define la prioridad (primera coincidencia gana).
 */
public final class ReglaMorfologicaLgs {

    public enum Tipo {
        /** {@code lexrule sufijo <s> infinitivo ar|er|ir [si condicion ...]} */
        SUFIJO,
        /** {@code lexrule sufijo o heuristica primera_persona} */
        HEURISTICA_PRIMERA_PERSONA
    }

    public enum Condicion {
        NINGUNA,
        RAIZ_VOCAL,
        RAIZ_TERMINA,
        RAIZ_CONSONANTE
    }

    private final Tipo tipo;
    private final String sufijo;
    private final char vocalInfinitivo;
    private final Condicion condicion;
    private final List<String> patronesRaiz;

    private ReglaMorfologicaLgs(
            Tipo tipo,
            String sufijo,
            char vocalInfinitivo,
            Condicion condicion,
            List<String> patronesRaiz) {
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.sufijo = sufijo;
        this.vocalInfinitivo = vocalInfinitivo;
        this.condicion = condicion == null ? Condicion.NINGUNA : condicion;
        this.patronesRaiz = patronesRaiz == null ? List.of() : List.copyOf(patronesRaiz);
    }

    public static ReglaMorfologicaLgs sufijo(String sufijo, char vocalInfinitivo) {
        return sufijo(sufijo, vocalInfinitivo, Condicion.NINGUNA, List.of());
    }

    public static ReglaMorfologicaLgs sufijo(
            String sufijo, char vocalInfinitivo, Condicion condicion, List<String> patronesRaiz) {
        if (sufijo == null || sufijo.isBlank()) {
            throw new IllegalArgumentException("sufijo vacío");
        }
        if (vocalInfinitivo != 'a' && vocalInfinitivo != 'e' && vocalInfinitivo != 'i') {
            throw new IllegalArgumentException("vocal infinitivo debe ser a, e o i (ar/er/ir)");
        }
        return new ReglaMorfologicaLgs(Tipo.SUFIJO, sufijo.trim(), vocalInfinitivo, condicion, patronesRaiz);
    }

    public static ReglaMorfologicaLgs heuristicaPrimeraPersona(String sufijo) {
        if (sufijo == null || sufijo.isBlank()) {
            throw new IllegalArgumentException("sufijo vacío");
        }
        return new ReglaMorfologicaLgs(
                Tipo.HEURISTICA_PRIMERA_PERSONA, sufijo.trim(), 'a', Condicion.NINGUNA, List.of());
    }

    public Tipo tipo() {
        return tipo;
    }

    public String sufijo() {
        return sufijo;
    }

    public char vocalInfinitivo() {
        return vocalInfinitivo;
    }

    public Condicion condicion() {
        return condicion;
    }

    public List<String> patronesRaiz() {
        return patronesRaiz;
    }
}
