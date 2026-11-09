package prism;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nlp.lexicon.NormalizadorMorfologico;

/**
 * Asigna símbolos {@code p, q, r, …} a fragmentos de texto normalizados de
 * forma estable en una sola traducción.
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
        existente = claveASimbolo.get(clave.toLowerCase());
        if (existente != null) {
            return existente;
        }

        String flexivo = buscarCoincidenciaFlexiva(clave);
        if (flexivo != null) {
            claveASimbolo.put(clave, flexivo);
            if (pasosDeAnalisis != null) {
                pasosDeAnalisis.add("Asociación por equivalencia flexiva/anáfora: \""
                        + clave + "\" → " + flexivo);
            }
            return flexivo;
        }

        String simbolo = siguienteSimboloDisponible(claveASimbolo.size());
        claveASimbolo.put(clave, simbolo);
        if (pasosDeAnalisis != null) {
            pasosDeAnalisis.add("Nueva proposición: " + simbolo + " = \"" + clave + "\"");
        }
        return simbolo;
    }

    private String buscarCoincidenciaFlexiva(String clave) {
        for (Map.Entry<String, String> entry : claveASimbolo.entrySet()) {
            String etiqueta = entry.getKey();
            String simbolo = entry.getValue();
            if (etiqueta.length() <= 2) {
                continue;
            }
            if (NormalizadorMorfologico.sonFrasesEquivalentes(clave, etiqueta)) {
                return simbolo;
            }
            if (NormalizadorMorfologico.coincideAnafora(clave, etiqueta)) {
                return simbolo;
            }
        }
        return null;
    }

    public Map<String, String> mapaParaResultado() {
        LinkedHashMap<String, String> resultado = new LinkedHashMap<>();
        Set<String> simbolosVistos = new HashSet<>();
        for (Map.Entry<String, String> entry : claveASimbolo.entrySet()) {
            String clave = entry.getKey();
            String simbolo = entry.getValue();
            if (clave.equals(simbolo)) {
                continue;
            }
            if (simbolosVistos.add(simbolo)) {
                resultado.put(clave, simbolo);
            }
        }
        return Collections.unmodifiableMap(resultado);
    }

    /**
     * Pre-registra un átomo declarado en fuente {@code .pr}: el símbolo {@code p} y
     * su etiqueta NL.
     */
    public void registrarAtomoDeclarado(String simbolo, String etiquetaNl) {
        String sym = simbolo.trim();
        claveASimbolo.put(sym, sym);
        if (etiquetaNl != null && !etiquetaNl.isBlank()) {
            String etiqueta = etiquetaNl.trim();
            claveASimbolo.put(etiqueta, sym);
            claveASimbolo.put(etiqueta.toLowerCase(), sym);
        }
    }

    public void registrarAtomoDeclarado(String simbolo, String etiquetaNl, String formaCanonica) {
        registrarAtomoDeclarado(simbolo, etiquetaNl);
        if (formaCanonica != null && !formaCanonica.isBlank()) {
            claveASimbolo.put(formaCanonica.trim(), simbolo.trim());
            claveASimbolo.put(formaCanonica.trim().toLowerCase(), simbolo.trim());
        }
    }

    private String siguienteSimboloDisponible(int indiceInicial) {
        int indice = indiceInicial;
        while (true) {
            String candidato = siguienteSimbolo(indice);
            if (!claveASimbolo.containsValue(candidato)) {
                return candidato;
            }
            indice++;
        }
    }

    private static String siguienteSimbolo(int indice) {
        if (indice < SIMBOLOS.length) {
            return String.valueOf(SIMBOLOS[indice]);
        }
        return "p" + indice;
    }
}
