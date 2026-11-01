package prism.vertauto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resultado de {@link VertautoService}: fórmula evaluada, dictamen y datos opcionales de tabla/pasos.
 * Si la entrada fue lenguaje natural, {@link #getPasosTraduccion()} incluye lexemas, patrones, etc.
 */
public final class VertautoResult {

    private final String formula;
    private final TipoDictamen dictamen;
    private final List<String> atomos;
    private final List<String> filasTabla;
    private final List<String> pasosEvaluacion;
    private final String textoNatural;
    private final List<String> pasosTraduccion;
    private final Map<String, String> proposiciones;

    public VertautoResult(
            String formula,
            TipoDictamen dictamen,
            List<String> atomos,
            List<String> filasTabla,
            List<String> pasosEvaluacion
    ) {
        this(formula, dictamen, atomos, filasTabla, pasosEvaluacion, null, List.of(), Map.of());
    }

    public VertautoResult(
            String formula,
            TipoDictamen dictamen,
            List<String> atomos,
            List<String> filasTabla,
            List<String> pasosEvaluacion,
            String textoNatural,
            List<String> pasosTraduccion,
            Map<String, String> proposiciones
    ) {
        this.formula = Objects.requireNonNull(formula);
        this.dictamen = Objects.requireNonNull(dictamen);
        this.atomos = List.copyOf(atomos);
        this.filasTabla = List.copyOf(filasTabla);
        this.pasosEvaluacion = List.copyOf(pasosEvaluacion);
        this.textoNatural = textoNatural;
        this.pasosTraduccion = pasosTraduccion == null ? List.of() : List.copyOf(pasosTraduccion);
        this.proposiciones = proposiciones == null ? Map.of() : Collections.unmodifiableMap(proposiciones);
    }

    public String getFormula() {
        return formula;
    }

    public TipoDictamen getDictamen() {
        return dictamen;
    }

    public List<String> getAtomos() {
        return atomos;
    }

    public List<String> getFilasTabla() {
        return filasTabla;
    }

    /** Pasos de tabla de verdad y dictamen. */
    public List<String> getPasosEvaluacion() {
        return pasosEvaluacion;
    }

    /** Pasos del pipeline {@code translate} (lexemas, patrones, lemas). Vacío si la entrada no fue NL. */
    public List<String> getPasosTraduccion() {
        return pasosTraduccion;
    }

    public String getTextoNatural() {
        return textoNatural;
    }

    public Map<String, String> getProposiciones() {
        return proposiciones;
    }

    /** Salida modo corto: {@code (p → q)  contingency} */
    public String formatoCorto() {
        return formula + "  " + dictamen.etiquetaCorta();
    }

    public void imprimirModoCorto() {
        System.out.println(formatoCorto());
    }

    public void imprimirExtendido(boolean steps, boolean table, boolean verdict) {
        if (steps) {
            if (!pasosTraduccion.isEmpty()) {
                System.out.println("=== Traducción (NL → fórmula) ===");
                if (textoNatural != null && !textoNatural.isBlank()) {
                    System.out.println("Entrada: " + textoNatural);
                }
                for (String paso : pasosTraduccion) {
                    System.out.println("  - " + paso);
                }
                if (!proposiciones.isEmpty()) {
                    System.out.println("Proposiciones: " + proposiciones);
                }
                System.out.println("Fórmula emitida: " + formula);
                System.out.println();
            } else if (!proposiciones.isEmpty()) {
                System.out.println("=== Átomos declarados ===");
                System.out.println("Proposiciones: " + proposiciones);
                System.out.println("Fórmula: " + formula);
                System.out.println();
            }
            if (!pasosEvaluacion.isEmpty()) {
                System.out.println("=== Evaluación lógica ===");
                for (String paso : pasosEvaluacion) {
                    System.out.println("  - " + paso);
                }
                System.out.println();
            }
        }
        if (table && !filasTabla.isEmpty()) {
            System.out.println("=== Tabla de verdad ===");
            for (String fila : filasTabla) {
                System.out.println(fila);
            }
            System.out.println();
        }
        if (verdict) {
            System.out.println("=== Dictamen ===");
            System.out.println(formatoCorto());
            System.out.println("Clasificación: " + dictamen.etiquetaCorta());
        }
    }
}
