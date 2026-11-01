package prism;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PrismResult {
    private final boolean exito;
    private final String formula;
    private final String mensaje;
    private final List<String> pasosDeAnalisis;
    private final Map<String, String> proposiciones;

    private PrismResult(
            boolean exito,
            String formula,
            String mensaje,
            List<String> pasosDeAnalisis,
            Map<String, String> proposiciones
    ) {
        this.exito = exito;
        this.formula = formula;
        this.mensaje = mensaje;
        this.pasosDeAnalisis = pasosDeAnalisis;
        this.proposiciones = proposiciones;
    }

    public static PrismResult exito(
            String formula,
            String mensaje,
            List<String> pasosDeAnalisis,
            Map<String, String> proposiciones
    ) {
        return new PrismResult(
                true,
                formula,
                mensaje,
                Collections.unmodifiableList(pasosDeAnalisis),
                Collections.unmodifiableMap(proposiciones)
        );
    }

    public static PrismResult error(String mensaje, List<String> pasosDeAnalisis) {
        return new PrismResult(
                false,
                "",
                mensaje,
                Collections.unmodifiableList(pasosDeAnalisis),
                Collections.emptyMap()
        );
    }

    public boolean isExito() {
        return exito;
    }

    public String getFormula() {
        return formula;
    }

    public String getMensaje() {
        return mensaje;
    }

    public List<String> getPasosDeAnalisis() {
        return pasosDeAnalisis;
    }

    public Map<String, String> getProposiciones() {
        return proposiciones;
    }
}
