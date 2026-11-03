package prism.pr;

import prism.RegistroProposiciones;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registro de átomos declarados con {@code atom p = "etiqueta"} en un programa {@code .ls}.
 * Enlaza símbolos proposicionales con etiquetas NL para emisión de fórmulas y salida de {@code vertauto}.
 */
public final class PrAtomRegistry {

    private final Map<String, String> simboloAEtiqueta;

    public PrAtomRegistry(Map<String, String> simboloAEtiqueta) {
        this.simboloAEtiqueta = Collections.unmodifiableMap(new LinkedHashMap<>(simboloAEtiqueta));
    }

    public static PrAtomRegistry vacio() {
        return new PrAtomRegistry(Map.of());
    }

    public RegistroProposiciones toRegistroProposiciones() {
        return toRegistroProposiciones(null);
    }

    public RegistroProposiciones toRegistroProposiciones(nlp.lexicon.BaseConocimiento baseConocimiento) {
        RegistroProposiciones registro = new RegistroProposiciones();
        for (Map.Entry<String, String> entry : simboloAEtiqueta.entrySet()) {
            String canonica = baseConocimiento != null ? baseConocimiento.canonicalizarFragmento(entry.getValue()) : null;
            registro.registrarAtomoDeclarado(entry.getKey(), entry.getValue(), canonica);
        }
        return registro;
    }

    /**
     * Sustituye fragmentos NL del {@code translate} por las etiquetas declaradas en {@code atom}
     * cuando el símbolo coincide (p. ej. {@code llover=p} → {@code llueve=p} si {@code atom p = "llueve"}).
     */
    public Map<String, String> proposicionesDesdeTraduccion(Map<String, String> desdeTranslate) {
        if (desdeTranslate == null || desdeTranslate.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> resultado = new LinkedHashMap<>();
        java.util.Set<String> simbolosVistos = new java.util.HashSet<>();
        for (Map.Entry<String, String> entry : desdeTranslate.entrySet()) {
            String simbolo = entry.getValue();
            String etiqueta = simboloAEtiqueta.getOrDefault(simbolo, entry.getKey());
            if (simbolosVistos.add(simbolo)) {
                resultado.put(etiqueta, simbolo);
            }
        }
        return Collections.unmodifiableMap(resultado);
    }

    /** Proposiciones para fórmulas simbólicas: etiqueta declarada → símbolo. */
    public Map<String, String> proposicionesParaSimbolos(Collection<String> simbolos) {
        LinkedHashMap<String, String> resultado = new LinkedHashMap<>();
        for (String simbolo : simbolos) {
            String etiqueta = simboloAEtiqueta.get(simbolo);
            if (etiqueta != null) {
                resultado.put(etiqueta, simbolo);
            } else {
                resultado.put(simbolo, simbolo);
            }
        }
        return Collections.unmodifiableMap(resultado);
    }

    public String etiquetaDe(String simbolo) {
        return simboloAEtiqueta.get(simbolo);
    }

    public Map<String, String> mapaSimboloAEtiqueta() {
        return simboloAEtiqueta;
    }

    public boolean contieneSimbolo(String simbolo) {
        return simboloAEtiqueta.containsKey(simbolo);
    }
}
