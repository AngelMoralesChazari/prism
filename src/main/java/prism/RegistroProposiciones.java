package prism;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asigna símbolos {@code p, q, r, …} a fragmentos de texto normalizados de forma estable en una sola traducción.
 */
public final class RegistroProposiciones {
    private static final char SIMBOLOS[] = "pqrstuvwxyzabcdefghijklmno".toCharArray();

    private final LinkedHashMap<String, String> claveASimbolo = new LinkedHashMap<>();

    public String simboloPara(String fragmentoNormalizado, List<String> pasosDeAnalisis) {
        String clave = fragmentoNormalizado.trim();
        String existente = claveASimbolo.get(clave);
        if (existente != null) {
            return existente;
        }
        String simbolo = siguienteSimbolo(claveASimbolo.size());
        claveASimbolo.put(clave, simbolo);
        pasosDeAnalisis.add("Nueva proposición: " + simbolo + " = \"" + clave + "\"");
        return simbolo;
    }

    public Map<String, String> mapaParaResultado() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(claveASimbolo));
    }

    /**
     * Pre-registra un átomo declarado en fuente {@code .ls}: el símbolo {@code p} y su etiqueta NL.
     */
    public void registrarAtomoDeclarado(String simbolo, String etiquetaNl) {
        String sym = simbolo.trim();
        claveASimbolo.put(sym, sym);
        if (etiquetaNl != null && !etiquetaNl.isBlank()) {
            claveASimbolo.put(etiquetaNl.trim(), sym);
        }
    }

    private static String siguienteSimbolo(int indice) {
        if (indice < SIMBOLOS.length) {
            return String.valueOf(SIMBOLOS[indice]);
        }
        return "p" + indice;
    }
}
