package nlp.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexer incremental sobre cadena normalizada: reconoce conectores de uso frecuente
 * y agrupa el resto como literales. Orden de palabras clave: más largas primero.
 */
public final class NaturalLexer {

    private static final String PALABRAS_CLAVE[] = {
            "no es cierto que ",
            "no es verdad que ",
            "en caso de que ",
            "si y solo si ",
            "a menos que ",
            "siempre que ",
            "puesto que ",
            "dado que ",
            "solo si ",
            "ya que ",
            " o bien ",
            "o bien ",
            "entonces ",
            "cuando ",
            "si ",
            " y ",
            " o ",
            " ni ",
            "ni ",
    };

    private static final TipoTokenNatural TIPOS[] = {
            TipoTokenNatural.NO_ES_CIERTO_QUE,
            TipoTokenNatural.NO_ES_CIERTO_QUE,
            TipoTokenNatural.EN_CASO_DE_QUE,
            TipoTokenNatural.SI_Y_SOLO_SI,
            TipoTokenNatural.A_MENOS_QUE,
            TipoTokenNatural.SIEMPRE_QUE,
            TipoTokenNatural.PUESTO_QUE,
            TipoTokenNatural.DADO_QUE,
            TipoTokenNatural.SOLO_SI,
            TipoTokenNatural.YA_QUE,
            TipoTokenNatural.O_BIEN,
            TipoTokenNatural.O_BIEN,
            TipoTokenNatural.ENTONCES,
            TipoTokenNatural.CUANDO,
            TipoTokenNatural.SI,
            TipoTokenNatural.Y,
            TipoTokenNatural.O,
            TipoTokenNatural.NI,
            TipoTokenNatural.NI,
    };

    public List<TokenNatural> tokenizar(String normalizado) {
        List<TokenNatural> salida = new ArrayList<>();
        if (normalizado == null || normalizado.isEmpty()) {
            return salida;
        }
        String s = normalizado;
        int i = 0;
        int n = s.length();
        while (i < n) {
            // Palabra clave antes de consumir espacios: patrones como " y " empiezan en espacio;
            // si saltamos espacios primero, nunca coinciden tras un literal (p. ej. "a y b").
            int[] kw = encontrarPalabraClave(s, i);
            if (kw != null) {
                int fin = kw[0];
                int indiceTipo = kw[1];
                salida.add(new TokenNatural(TIPOS[indiceTipo], PALABRAS_CLAVE[indiceTipo].trim()));
                i = fin;
                continue;
            }
            char c = s.charAt(i);
            if (c == ' ' || c == ',') {
                i++;
                continue;
            }
            int j = finLiteral(s, i);
            String lit = s.substring(i, j).trim();
            if (!lit.isEmpty()) {
                boolean delimitadoExplicito = (j < n && s.charAt(j) == ',') || hayEntoncesSiguiente(s, j);
                agregarLiterales(salida, lit, delimitadoExplicito);
            }
            i = j;
        }
        return salida;
    }

    /**
     * Tras {@code si}, frases elípticas (*si estudio apruebo*) se parten en un literal por palabra
     * solo cuando no hay delimitación explícita (coma o {@code entonces}); en condicionales explícitos,
     * el antecedente multi-palabra se preserva íntegro.
     */
    private static void agregarLiterales(List<TokenNatural> salida, String lit, boolean delimitadoExplicito) {
        if (!delimitadoExplicito
                && (ultimoTokenPermiteElipsis(salida) || ultimoTokenParteLiteralEnPalabras(salida))
                && literalSinConectoresInternos(lit)) {
            List<String> palabras = fusionarPrefijosNo(lit.split("\\s+"));
            if (ultimoTokenPermiteElipsis(salida) && palabras.size() >= 3) {
                salida.add(new TokenNatural(TipoTokenNatural.LITERAL, palabras.get(0)));
                salida.add(new TokenNatural(
                        TipoTokenNatural.LITERAL,
                        String.join(" ", palabras.subList(1, palabras.size()))));
                return;
            }
            for (String palabra : palabras) {
                if (!palabra.isEmpty()) {
                    salida.add(new TokenNatural(TipoTokenNatural.LITERAL, palabra));
                }
            }
            return;
        }
        salida.add(new TokenNatural(TipoTokenNatural.LITERAL, lit));
    }

    private static boolean hayEntoncesSiguiente(String s, int j) {
        int n = s.length();
        int k = j;
        while (k < n) {
            int[] kw = encontrarPalabraClave(s, k);
            if (kw != null) {
                TipoTokenNatural tipo = TIPOS[kw[1]];
                if (tipo == TipoTokenNatural.ENTONCES) {
                    return true;
                }
                if (tipo == TipoTokenNatural.SI || tipo == TipoTokenNatural.SI_Y_SOLO_SI
                        || tipo == TipoTokenNatural.SIEMPRE_QUE || tipo == TipoTokenNatural.CUANDO
                        || tipo == TipoTokenNatural.DADO_QUE || tipo == TipoTokenNatural.YA_QUE
                        || tipo == TipoTokenNatural.PUESTO_QUE) {
                    return false;
                }
                k = kw[0];
            } else {
                k++;
            }
        }
        return false;
    }

    /** Tras {@code y}/{@code o} dentro de un bloque {@code si …} sin {@code entonces}, cada palabra es un literal. */
    private static boolean ultimoTokenParteLiteralEnPalabras(List<TokenNatural> salida) {
        if (salida.isEmpty()) {
            return false;
        }
        TipoTokenNatural ultimo = salida.get(salida.size() - 1).getTipo();
        if (ultimo != TipoTokenNatural.Y && ultimo != TipoTokenNatural.O) {
            return false;
        }
        boolean vistoCondicional = false;
        for (TokenNatural t : salida) {
            if (t.getTipo() == TipoTokenNatural.ENTONCES) {
                return false;
            }
            if (esInicioBloqueCondicional(t.getTipo())) {
                vistoCondicional = true;
            }
        }
        return vistoCondicional;
    }

    private static boolean esInicioBloqueCondicional(TipoTokenNatural tipo) {
        return tipo == TipoTokenNatural.SI
                || tipo == TipoTokenNatural.SIEMPRE_QUE
                || tipo == TipoTokenNatural.CUANDO
                || tipo == TipoTokenNatural.DADO_QUE
                || tipo == TipoTokenNatural.YA_QUE
                || tipo == TipoTokenNatural.PUESTO_QUE;
    }

    private static boolean ultimoTokenPermiteElipsis(List<TokenNatural> salida) {
        if (salida.isEmpty()) {
            return false;
        }
        TipoTokenNatural t = salida.get(salida.size() - 1).getTipo();
        return t == TipoTokenNatural.SI
                || t == TipoTokenNatural.SIEMPRE_QUE
                || t == TipoTokenNatural.CUANDO
                || t == TipoTokenNatural.DADO_QUE
                || t == TipoTokenNatural.YA_QUE
                || t == TipoTokenNatural.PUESTO_QUE;
    }

    private static boolean literalSinConectoresInternos(String lit) {
        if (!lit.contains(" ")) {
            return false;
        }
        for (String p : PALABRAS_CLAVE) {
            String palabra = p.trim();
            if (palabra.isEmpty()) {
                continue;
            }
            if (contieneComoPalabra(lit, palabra)) {
                return false;
            }
        }
        return true;
    }

    private static boolean contieneComoPalabra(String lit, String palabra) {
        if (lit.contains(palabra) && palabra.contains(" ")) {
            return true;
        }
        for (String w : lit.split("\\s+")) {
            if (w.equals(palabra)) {
                return true;
            }
        }
        return false;
    }

    /** Une {@code no} con la palabra siguiente (*no estudio*) para negación en el mapper. */
    private static List<String> fusionarPrefijosNo(String[] palabras) {
        List<String> fusionadas = new ArrayList<>();
        for (int i = 0; i < palabras.length; i++) {
            String p = palabras[i];
            if (p.isEmpty()) {
                continue;
            }
            if ("no".equals(p) && i + 1 < palabras.length && !palabras[i + 1].isEmpty()) {
                fusionadas.add("no " + palabras[i + 1]);
                i++;
            } else {
                fusionadas.add(p);
            }
        }
        return fusionadas;
    }

    private static int[] encontrarPalabraClave(String s, int i) {
        for (int k = 0; k < PALABRAS_CLAVE.length; k++) {
            String p = PALABRAS_CLAVE[k];
            if (s.regionMatches(i, p, 0, p.length())) {
                return new int[] { i + p.length(), k };
            }
        }
        return null;
    }

    /** Avanza hasta el inicio de la siguiente palabra clave o el final. */
    private static int finLiteral(String s, int i) {
        int n = s.length();
        int j = i;
        while (j < n) {
            if (s.charAt(j) == ',') {
                break;
            }
            if (encontrarPalabraClave(s, j) != null) {
                break;
            }
            j++;
        }
        return j;
    }
}
