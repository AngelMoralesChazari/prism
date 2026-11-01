package nlp.lexicon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import nlp.lexer.TipoTokenNatural;

/**
 * Carga archivos {@code .lgs} (v0.2): lemas, patrones semánticos y metadatos ignorables.
 * Formato: {@code docs/Prism/FormatoLGS.md} y {@code docs/Prism/Pattern.md}.
 */
public final class LgsCargador {

    private LgsCargador() {
    }

    /**
     * Carga lemas y patrones desde el classpath con diagnóstico explícito.
     * Preferir este método frente a {@link #cargarDesdeClasspath(String)} en el motor Prism.
     */
    public static ResultadoCargaLgs cargarConDiagnostico(String rutaRecurso) {
        Objects.requireNonNull(rutaRecurso, "rutaRecurso");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = LgsCargador.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(rutaRecurso)) {
            if (in == null) {
                return new ResultadoCargaLgs(
                        ContenidoLgs.vacio(),
                        EstadoCargaLgs.RECURSO_NO_ENCONTRADO,
                        "No se encontró el recurso en el classpath. Copie src/main/resources a out al compilar.",
                        rutaRecurso);
            }
            return cargarConDiagnostico(in, rutaRecurso);
        } catch (IOException e) {
            return new ResultadoCargaLgs(
                    ContenidoLgs.vacio(),
                    EstadoCargaLgs.ERROR_LECTURA,
                    e.getMessage(),
                    rutaRecurso);
        }
    }

    /**
     * Parsea un stream ya abierto (útil en pruebas y herramientas).
     */
    public static ResultadoCargaLgs cargarConDiagnostico(InputStream entrada, String rutaEtiqueta) {
        Objects.requireNonNull(entrada, "entrada");
        try {
            ContenidoLgs contenido = cargar(entrada);
            return new ResultadoCargaLgs(contenido, EstadoCargaLgs.EXITO, "", rutaEtiqueta);
        } catch (IOException e) {
            EstadoCargaLgs estado = mensajePareceSintaxis(e)
                    ? EstadoCargaLgs.ERROR_SINTAXIS
                    : EstadoCargaLgs.ERROR_LECTURA;
            return new ResultadoCargaLgs(ContenidoLgs.vacio(), estado, e.getMessage(), rutaEtiqueta);
        }
    }

    /**
     * Carga lemas y patrones desde el classpath, p. ej. {@code prism/core.pr}.
     * Si el recurso no existe o hay error, devuelve {@link ContenidoLgs#vacio()} sin mensaje
     * (uso legacy; el motor usa {@link #cargarConDiagnostico(String)}).
     */
    public static ContenidoLgs cargarDesdeClasspath(String rutaRecurso) {
        ResultadoCargaLgs r = cargarConDiagnostico(rutaRecurso);
        if (r.bloqueaTraduccion() || r.estado() == EstadoCargaLgs.RECURSO_NO_ENCONTRADO) {
            return ContenidoLgs.vacio();
        }
        return r.contenido();
    }

    private static boolean mensajePareceSintaxis(IOException e) {
        String m = e.getMessage();
        return m != null && m.contains("Línea ");
    }

    /**
     * Compatibilidad: solo el mapa de lemas del recurso indicado.
     */
    public static Map<String, String> cargarLemasDesdeClasspath(String rutaRecurso) {
        return cargarDesdeClasspath(rutaRecurso).lemas();
    }

    public static ContenidoLgs cargar(InputStream entrada) throws IOException {
        Map<String, String> lemas = new LinkedHashMap<>();
        Map<String, String> antonimos = new LinkedHashMap<>();
        List<PatronSemanticoLgs> patrones = new ArrayList<>();
        List<ReglaMorfologicaLgs> reglasMorfologicas = new ArrayList<>();
        Set<String> exclusionesMorfologicas = new LinkedHashSet<>();
        Set<String> prefijosPrivativos = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String linea;
            int num = 0;
            while ((linea = br.readLine()) != null) {
                num++;
                String t = linea.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                String tl = t.toLowerCase(Locale.ROOT);
                if (tl.startsWith("version")) {
                    continue;
                }
                if (tl.startsWith("lemma ")) {
                    parsearLemma(num, t, lemas);
                    continue;
                }
                if (tl.startsWith("antonym ")) {
                    parsearAntonym(num, t, antonimos);
                    continue;
                }
                if (tl.startsWith("lexrule ")) {
                    parsearLexrule(num, t, reglasMorfologicas, exclusionesMorfologicas, prefijosPrivativos);
                    continue;
                }
                if (tl.startsWith("pattern ")) {
                    patrones.add(parsearPattern(num, t));
                    continue;
                }
                throw new IOException("Línea " + num + ": directiva no reconocida: " + linea);
            }
        }
        ConfiguracionMorfologiaLgs morfologia =
                new ConfiguracionMorfologiaLgs(reglasMorfologicas, exclusionesMorfologicas, prefijosPrivativos);
        return new ContenidoLgs(lemas, patrones, morfologia, antonimos);
    }

    public static Map<String, String> cargarLemas(InputStream entrada) throws IOException {
        return cargar(entrada).lemas();
    }

    private static void parsearLemma(int numLinea, String linea, Map<String, String> lemas) throws IOException {
        String resto = linea.substring(6).trim();
        int flecha = resto.indexOf("->");
        if (flecha < 0) {
            throw new IOException("Línea " + numLinea + ": falta '->' en lemma: " + linea);
        }
        String forma = resto.substring(0, flecha).trim().toLowerCase(Locale.ROOT);
        String lema = resto.substring(flecha + 2).trim().toLowerCase(Locale.ROOT);
        if (forma.isEmpty() || lema.isEmpty()) {
            throw new IOException("Línea " + numLinea + ": forma o lema vacío: " + linea);
        }
        lemas.put(forma, lema);
    }

    private static void parsearAntonym(int numLinea, String linea, Map<String, String> antonimos) throws IOException {
        String resto = linea.substring(8).trim();
        int flecha = resto.indexOf("->");
        if (flecha < 0) {
            throw new IOException("Línea " + numLinea + ": falta '->' en antonym: " + linea);
        }
        String negada = resto.substring(0, flecha).trim().toLowerCase(Locale.ROOT);
        String positiva = resto.substring(flecha + 2).trim().toLowerCase(Locale.ROOT);
        if (negada.isEmpty() || positiva.isEmpty()) {
            throw new IOException("Línea " + numLinea + ": forma negada o positiva vacía en antonym: " + linea);
        }
        antonimos.put(negada, positiva);
    }

    private static void parsearLexrule(
            int numLinea,
            String linea,
            List<ReglaMorfologicaLgs> reglas,
            Set<String> exclusiones,
            Set<String> prefijosPrivativos) throws IOException {
        String resto = linea.substring(8).trim();
        if (resto.isEmpty()) {
            throw new IOException("Línea " + numLinea + ": lexrule sin cuerpo: " + linea);
        }
        String[] partes = resto.split("\\s+");
        String tipo = partes[0].toLowerCase(Locale.ROOT);
        if ("excluir".equals(tipo)) {
            if (partes.length < 2) {
                throw new IOException("Línea " + numLinea + ": lexrule excluir requiere al menos una palabra: " + linea);
            }
            for (int i = 1; i < partes.length; i++) {
                String palabra = partes[i].trim().toLowerCase(Locale.ROOT);
                if (!palabra.isEmpty()) {
                    exclusiones.add(palabra);
                }
            }
            return;
        }
        if ("prefijo".equals(tipo)) {
            if (partes.length < 2) {
                throw new IOException("Línea " + numLinea + ": lexrule prefijo requiere al menos un prefijo: " + linea);
            }
            for (int i = 1; i < partes.length; i++) {
                String p = partes[i].trim().toLowerCase(Locale.ROOT);
                if (!p.isEmpty() && !"privativo".equals(p)) {
                    prefijosPrivativos.add(p);
                }
            }
            return;
        }
        if (!"sufijo".equals(tipo)) {
            throw new IOException("Línea " + numLinea + ": lexrule debe empezar por excluir, prefijo o sufijo: " + linea);
        }
        if (partes.length < 3) {
            throw new IOException("Línea " + numLinea + ": lexrule sufijo incompleta: " + linea);
        }
        String sufijo = partes[1].toLowerCase(Locale.ROOT);
        if ("heuristica".equals(partes[2].toLowerCase(Locale.ROOT))) {
            if (partes.length < 4 || !"primera_persona".equals(partes[3].toLowerCase(Locale.ROOT))) {
                throw new IOException(
                        "Línea " + numLinea + ": use 'lexrule sufijo <s> heuristica primera_persona': " + linea);
            }
            try {
                reglas.add(ReglaMorfologicaLgs.heuristicaPrimeraPersona(sufijo));
            } catch (IllegalArgumentException ex) {
                throw new IOException("Línea " + numLinea + ": " + ex.getMessage() + " — " + linea, ex);
            }
            return;
        }
        if (!"infinitivo".equals(partes[2].toLowerCase(Locale.ROOT))) {
            throw new IOException("Línea " + numLinea + ": tras sufijo se espera infinitivo o heuristica: " + linea);
        }
        if (partes.length < 4) {
            throw new IOException("Línea " + numLinea + ": falta ar|er|ir tras infinitivo: " + linea);
        }
        char vocal = parsearVocalInfinitivo(numLinea, partes[3]);
        ReglaMorfologicaLgs.Condicion condicion = ReglaMorfologicaLgs.Condicion.NINGUNA;
        List<String> patronesRaiz = List.of();
        if (partes.length >= 6 && "si".equals(partes[4].toLowerCase(Locale.ROOT))) {
            String cond = partes[5].toLowerCase(Locale.ROOT);
            if ("raiz_vocal".equals(cond)) {
                condicion = ReglaMorfologicaLgs.Condicion.RAIZ_VOCAL;
            } else if ("raiz_consonante".equals(cond)) {
                condicion = ReglaMorfologicaLgs.Condicion.RAIZ_CONSONANTE;
            } else if ("raiz_termina".equals(cond)) {
                condicion = ReglaMorfologicaLgs.Condicion.RAIZ_TERMINA;
                if (partes.length < 7) {
                    throw new IOException("Línea " + numLinea + ": raiz_termina requiere patrones: " + linea);
                }
                patronesRaiz = List.of(java.util.Arrays.copyOfRange(partes, 6, partes.length));
            } else {
                throw new IOException("Línea " + numLinea + ": condicion desconocida en lexrule: " + linea);
            }
        }
        try {
            reglas.add(ReglaMorfologicaLgs.sufijo(sufijo, vocal, condicion, patronesRaiz));
        } catch (IllegalArgumentException ex) {
            throw new IOException("Línea " + numLinea + ": " + ex.getMessage() + " — " + linea, ex);
        }
    }

    private static char parsearVocalInfinitivo(int numLinea, String token) throws IOException {
        String t = token.toLowerCase(Locale.ROOT);
        return switch (t) {
            case "ar" -> 'a';
            case "er" -> 'e';
            case "ir" -> 'i';
            default -> throw new IOException("Línea " + numLinea + ": infinitivo debe ser ar, er o ir: " + token);
        };
    }

    private static PatronSemanticoLgs parsearPattern(int numLinea, String linea) throws IOException {
        int flechaDoble = linea.indexOf("=>");
        if (flechaDoble < 0) {
            throw new IOException("Línea " + numLinea + ": falta '=>' en pattern: " + linea);
        }
        String izquierda = linea.substring(0, flechaDoble).trim();
        String derecha = linea.substring(flechaDoble + 2).trim();
        if (!izquierda.toLowerCase(Locale.ROOT).startsWith("pattern ")) {
            throw new IOException("Línea " + numLinea + ": se esperaba 'pattern': " + linea);
        }
        String tokensPatron = izquierda.substring(8).trim();
        String[] partesForma = tokensPatron.split("\\s+");
        if (partesForma.length < 2) {
            throw new IOException("Línea " + numLinea + ": pattern necesita nombre y al menos un token: " + linea);
        }
        String nombre = partesForma[0];
        List<TipoTokenNatural> forma = new ArrayList<>();
        for (int i = 1; i < partesForma.length; i++) {
            forma.add(parsearTipoToken(numLinea, partesForma[i]));
        }
        String[] partesSalida = derecha.split("\\s+");
        if (partesSalida.length < 3) {
            throw new IOException("Línea " + numLinea + ": tras '=>' se espera p. ej. 'imp left=0 right=2': " + linea);
        }
        TipoSalidaIrPatron tipoIr = parsearTipoIr(numLinea, partesSalida[0]);
        int left = -1;
        int right = -1;
        int mid = -1;
        int fourth = -1;
        for (int i = 1; i < partesSalida.length; i++) {
            String p = partesSalida[i].toLowerCase(Locale.ROOT);
            if (p.startsWith("left=")) {
                left = parsearIndice(numLinea, p.substring(5));
            } else if (p.startsWith("right=")) {
                right = parsearIndice(numLinea, p.substring(6));
            } else if (p.startsWith("mid=")) {
                mid = parsearIndice(numLinea, p.substring(4));
            } else if (p.startsWith("fourth=") || p.startsWith("extra=")) {
                fourth = parsearIndice(numLinea, p.substring(p.indexOf('=') + 1));
            }
        }
        if (left < 0 || (right < 0 && tipoIr != TipoSalidaIrPatron.NOT)) {
            throw new IOException("Línea " + numLinea + ": faltan left= e right= en pattern: " + linea);
        }
        if ((tipoIr == TipoSalidaIrPatron.IMP_AND
                || tipoIr == TipoSalidaIrPatron.IMP_OR
                || tipoIr == TipoSalidaIrPatron.IMP_OR_ANT
                || tipoIr == TipoSalidaIrPatron.IMP_AND_CONS
                || tipoIr == TipoSalidaIrPatron.IMP_AND_AND
                || tipoIr == TipoSalidaIrPatron.IMP_OR_OR
                || tipoIr == TipoSalidaIrPatron.IMP_AND_OR
                || tipoIr == TipoSalidaIrPatron.IMP_OR_AND) && mid < 0) {
            throw new IOException("Línea " + numLinea + ": este tipo IR requiere mid=: " + linea);
        }
        if ((tipoIr == TipoSalidaIrPatron.IMP_AND_AND
                || tipoIr == TipoSalidaIrPatron.IMP_OR_OR
                || tipoIr == TipoSalidaIrPatron.IMP_AND_OR
                || tipoIr == TipoSalidaIrPatron.IMP_OR_AND) && fourth < 0) {
            throw new IOException("Línea " + numLinea + ": este tipo IR requiere fourth=: " + linea);
        }
        try {
            return new PatronSemanticoLgs(nombre, forma, tipoIr, left, right, mid, fourth);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Línea " + numLinea + ": " + ex.getMessage() + " — " + linea, ex);
        }
    }

    private static int parsearIndice(int numLinea, String s) throws IOException {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IOException("Línea " + numLinea + ": índice inválido: " + s);
        }
    }

    private static TipoSalidaIrPatron parsearTipoIr(int numLinea, String token) throws IOException {
        String t = token.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "imp", "implica", "implies" -> TipoSalidaIrPatron.IMP;
            case "and" -> TipoSalidaIrPatron.AND;
            case "or" -> TipoSalidaIrPatron.OR;
            case "equiv", "equivalente", "iff", "bicondicional" -> TipoSalidaIrPatron.EQUIV;
            case "imp_and" -> TipoSalidaIrPatron.IMP_AND;
            case "imp_or" -> TipoSalidaIrPatron.IMP_OR;
            case "imp_or_ant" -> TipoSalidaIrPatron.IMP_OR_ANT;
            case "imp_and_cons" -> TipoSalidaIrPatron.IMP_AND_CONS;
            case "imp_unless" -> TipoSalidaIrPatron.IMP_UNLESS;
            case "and_neg", "nini" -> TipoSalidaIrPatron.AND_NEG;
            case "not", "neg" -> TipoSalidaIrPatron.NOT;
            case "imp_and_and" -> TipoSalidaIrPatron.IMP_AND_AND;
            case "imp_or_or" -> TipoSalidaIrPatron.IMP_OR_OR;
            case "imp_and_or" -> TipoSalidaIrPatron.IMP_AND_OR;
            case "imp_or_and" -> TipoSalidaIrPatron.IMP_OR_AND;
            default -> throw new IOException("Línea " + numLinea + ": tipo IR desconocido: " + token);
        };
    }

    private static TipoTokenNatural parsearTipoToken(int numLinea, String token) throws IOException {
        String t = token.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (t) {
            case "si" -> TipoTokenNatural.SI;
            case "entonces" -> TipoTokenNatural.ENTONCES;
            case "en_caso_de_que" -> TipoTokenNatural.EN_CASO_DE_QUE;
            case "si_y_solo_si" -> TipoTokenNatural.SI_Y_SOLO_SI;
            case "siempre_que" -> TipoTokenNatural.SIEMPRE_QUE;
            case "cuando" -> TipoTokenNatural.CUANDO;
            case "solo_si" -> TipoTokenNatural.SOLO_SI;
            case "a_menos_que" -> TipoTokenNatural.A_MENOS_QUE;
            case "y" -> TipoTokenNatural.Y;
            case "o" -> TipoTokenNatural.O;
            case "ni" -> TipoTokenNatural.NI;
            case "dado_que" -> TipoTokenNatural.DADO_QUE;
            case "puesto_que" -> TipoTokenNatural.PUESTO_QUE;
            case "ya_que" -> TipoTokenNatural.YA_QUE;
            case "o_bien" -> TipoTokenNatural.O_BIEN;
            case "no_es_cierto_que", "no_es_cierto" -> TipoTokenNatural.NO_ES_CIERTO_QUE;
            case "literal" -> TipoTokenNatural.LITERAL;
            default -> throw new IOException("Línea " + numLinea + ": token de forma desconocido: " + token);
        };
    }
}
