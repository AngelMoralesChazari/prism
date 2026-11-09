package prism.pr;

import nlp.lexicon.LgsCargador;
import nlp.lexicon.ResultadoCargaLgs;
import prism.PrismService;
import prism.pr.ast.PrProgram;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PrChecker {

    private PrChecker() {
    }

    /**
     * Analiza un archivo .pr y devuelve diagnósticos estructurados con línea,
     * columna, severidad y sugerencias.
     */
    public static List<PrDiagnostic> diagnosticarArchivo(String ruta) {
        String source;
        try {
            source = PrSourceLoader.leer(ruta);
        } catch (Exception e) {
            return List
                    .of(new PrDiagnostic(1, 1, 1, 10, "error", "No se pudo leer el archivo: " + e.getMessage(), null));
        }
        return diagnosticarFuente(source, ruta);
    }

    /**
     * Analiza el código fuente de un programa Prism y produce diagnósticos para el
     * editor y CLI.
     */
    public static List<PrDiagnostic> diagnosticarFuente(String source, String ruta) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        if (esModuloLexicoPrism(source)) {
            return diagnosticarFuenteLexico(source, ruta);
        }

        // 1. Fase Léxica y Sintáctica (Parser)
        PrProgram program;
        try {
            program = PrParser.parse(source);
        } catch (PrParseException e) {
            int len = Math.max(1, e.length());
            return List.of(new PrDiagnostic(
                    e.line(),
                    e.col(),
                    e.line(),
                    e.col() + len,
                    "error",
                    e.getMessage(),
                    e.suggestion()));
        } catch (Exception e) {
            return List.of(new PrDiagnostic(1, 1, 1, 10, "error", "Error de análisis: " + e.getMessage(), null));
        }

        // 2. Fase de Validación Estática (Tipos, identificadores y convenciones)
        List<String> erroresValidacion = new ArrayList<>(PrValidator.validar(program));
        if (ruta != null && !ruta.isBlank()) {
            PrValidator.validarArchivoConvencion(ruta, program.moduleName(), erroresValidacion);
        }

        if (!erroresValidacion.isEmpty()) {
            List<PrDiagnostic> diags = new ArrayList<>();
            String[] lineas = source.split("\n", -1);
            for (String err : erroresValidacion) {
                int[] pos = ubicarErrorValidacion(err, lineas);
                diags.add(new PrDiagnostic(pos[0], pos[1], pos[0], pos[2], "error", err, null));
            }
            return Collections.unmodifiableList(diags);
        }

        // 3. Fase de Linter Semántico (Coherencia atom <-> translate)
        List<PrDiagnostic> diags = new ArrayList<>();
        try {
            PrismService service = new PrismService();
            List<String> avisos = PrSemanticLinter.diagnosticarPrograma(program, service);
            String[] lineas = source.split("\n", -1);
            for (String av : avisos) {
                int lin = 1;
                int col = 1;
                int endCol = 30;
                String sugerencia = null;

                int q1 = av.indexOf('\'');
                int q2 = av.indexOf('\'', q1 + 1);
                if (q1 != -1 && q2 != -1) {
                    String atomo = av.substring(q1 + 1, q2);
                    for (int i = 0; i < lineas.length; i++) {
                        if (lineas[i].matches(".*\\batom\\s+" + Pattern.quote(atomo) + "\\b.*")) {
                            lin = i + 1;
                            col = 1;
                            endCol = lineas[i].length() + 1;
                            break;
                        }
                    }
                }

                int idxSug = av.indexOf("¿Quisiste decir: ");
                if (idxSug != -1) {
                    int finSug = av.indexOf('?', idxSug);
                    if (finSug != -1) {
                        sugerencia = av.substring(idxSug + "¿Quisiste decir: ".length(), finSug).trim();
                    }
                }

                diags.add(new PrDiagnostic(lin, col, lin, endCol, "warning", av, sugerencia));
            }
        } catch (Exception ignored) {
        }

        return Collections.unmodifiableList(diags);
    }

    /**
     * Valida un archivo e imprime los resultados en formato legible de consola.
     */
    public static boolean checkArchivo(String ruta) {
        List<PrDiagnostic> diags = diagnosticarArchivo(ruta);
        boolean hayErrores = false;
        for (PrDiagnostic d : diags) {
            if ("error".equals(d.severity())) {
                System.err.println(d.message());
                hayErrores = true;
            } else {
                System.err.println(d.message());
            }
        }
        if (hayErrores) {
            return false;
        }
        System.out.println("OK: " + ruta);
        return true;
    }

    private static int[] ubicarErrorValidacion(String err, String[] lineas) {
        List<String> comillas = extraerComillasSimples(err);
        String primero = comillas.isEmpty() ? null : comillas.get(0);
        String segundo = comillas.size() >= 2 ? comillas.get(1) : null;

        if (err.startsWith("El module '") && primero != null) {
            int[] pos = buscarPatron(lineas, Pattern.compile("\\bmodule\\s+(" + Pattern.quote(primero) + ")\\b"), 1);
            if (pos != null)
                return pos;
        }

        if (err.startsWith("Redeclaración de identificador '") && primero != null) {
            Pattern decl = Pattern.compile("\\b(?:formula|atom|nl|let)\\s+(" + Pattern.quote(primero) + ")\\b");
            int[] segundoDecl = buscarPatronEnOcurrencia(lineas, decl, 1, 2);
            if (segundoDecl != null)
                return segundoDecl;
            int[] primeroDecl = buscarPatron(lineas, decl, 1);
            if (primeroDecl != null)
                return primeroDecl;
        }

        if (err.startsWith("Identificador '") && primero != null && segundo != null) {
            int[] pos = buscarUsoEnContexto(lineas, segundo, primero);
            if (pos != null)
                return pos;
        }

        if (err.startsWith("Átomo no declarado en '") && primero != null) {
            String atomo = extraerDespuesDosPuntos(err);
            if (atomo != null) {
                int[] pos = buscarUsoEnContexto(lineas, primero, atomo);
                if (pos != null)
                    return pos;
            }
            if ("vertauto inline".equals(primero)) {
                int[] pos = buscarPatron(lineas, Pattern.compile("\\b(vertauto)\\b"), 1);
                if (pos != null)
                    return pos;
            }
        }

        if (primero != null) {
            if ("vertauto inline".equals(primero)) {
                int[] pos = buscarPatron(lineas, Pattern.compile("\\b(vertauto)\\b"), 1);
                if (pos != null)
                    return pos;
            }
            int[] pos = buscarDeclaracionOPalabra(lineas, primero);
            if (pos != null)
                return pos;
        }

        String cola = extraerDespuesDosPuntos(err);
        if (cola != null && !cola.isBlank()) {
            String token = cola.split("\\s+")[0];
            int[] pos = buscarDeclaracionOPalabra(lineas, token);
            if (pos != null)
                return pos;
        }

        return new int[] { 1, 1, 20 };
    }

    private static List<String> extraerComillasSimples(String texto) {
        List<String> res = new ArrayList<>();
        int idx = 0;
        while (idx < texto.length()) {
            int q1 = texto.indexOf('\'', idx);
            if (q1 == -1)
                break;
            int q2 = texto.indexOf('\'', q1 + 1);
            if (q2 == -1)
                break;
            res.add(texto.substring(q1 + 1, q2));
            idx = q2 + 1;
        }
        return res;
    }

    private static String extraerDespuesDosPuntos(String err) {
        int sep = err.lastIndexOf(": ");
        if (sep == -1 || sep + 2 >= err.length()) {
            return null;
        }
        return err.substring(sep + 2).trim();
    }

    private static int[] buscarUsoEnContexto(String[] lineas, String contexto, String simbolo) {
        Pattern patInicio = "vertauto inline".equals(contexto)
                ? Pattern.compile("\\bvertauto\\s*\\(")
                : Pattern.compile("\\bformula\\s+" + Pattern.quote(contexto) + "\\b");
        Pattern patSimbolo = Pattern.compile("\\b(" + Pattern.quote(simbolo) + ")\\b");
        Pattern patNuevaSentencia = Pattern.compile("^\\s*(?:module|use|atom|nl|let|formula|vertauto)\\b");

        for (int i = 0; i < lineas.length; i++) {
            String codigo = quitarComentarioLinea(lineas[i]);
            Matcher mCtx = patInicio.matcher(codigo);
            if (!mCtx.find()) {
                continue;
            }
            int inicioCol = mCtx.start();
            for (int j = i; j < lineas.length; j++) {
                String codigoLinea = quitarComentarioLinea(lineas[j]);
                if (j > i && patNuevaSentencia.matcher(codigoLinea).find()) {
                    break;
                }
                Matcher mSim = patSimbolo.matcher(codigoLinea);
                int buscarDesde = (j == i) ? inicioCol : 0;
                while (mSim.find()) {
                    if (mSim.start(1) >= buscarDesde) {
                        // Evita emparejar el nombre declarado a la izquierda de '=' en 'formula x =
                        // ...'
                        int eqIdx = codigoLinea.indexOf('=');
                        if (j == i && !"vertauto inline".equals(contexto) && eqIdx != -1 && mSim.start(1) < eqIdx) {
                            continue;
                        }
                        int col = mSim.start(1) + 1;
                        return new int[] { j + 1, col, col + simbolo.length() };
                    }
                }
            }
            if (!"vertauto inline".equals(contexto)) {
                int col = mCtx.start() + 1;
                return new int[] { i + 1, col, col + contexto.length() };
            }
        }
        return null;
    }

    private static int[] buscarDeclaracionOPalabra(String[] lineas, String ident) {
        Pattern patDecl = Pattern.compile("\\b(?:formula|atom|nl|let)\\s+(" + Pattern.quote(ident) + ")\\b");
        int[] decl = buscarPatron(lineas, patDecl, 1);
        if (decl != null) {
            return decl;
        }
        Pattern patPalabra = Pattern.compile("\\b(" + Pattern.quote(ident) + ")\\b");
        int[] palabra = buscarPatron(lineas, patPalabra, 1);
        if (palabra != null) {
            return palabra;
        }
        for (int i = 0; i < lineas.length; i++) {
            int idx = lineas[i].indexOf(ident);
            if (idx != -1) {
                int col = idx + 1;
                return new int[] { i + 1, col, col + ident.length() };
            }
        }
        return null;
    }

    private static int[] buscarPatron(String[] lineas, Pattern pattern, int grupo) {
        return buscarPatronEnOcurrencia(lineas, pattern, grupo, 1);
    }

    private static int[] buscarPatronEnOcurrencia(String[] lineas, Pattern pattern, int grupo, int ocurrenciaObjetivo) {
        int encontradas = 0;
        for (int i = 0; i < lineas.length; i++) {
            String codigo = quitarComentarioLinea(lineas[i]);
            Matcher m = pattern.matcher(codigo);
            while (m.find()) {
                encontradas++;
                if (encontradas == ocurrenciaObjetivo) {
                    int start = m.start(grupo);
                    int end = m.end(grupo);
                    return new int[]{i + 1, start + 1, end + 1};
                }
            }
        }
        return null;
    }

    private static String quitarComentarioLinea(String linea) {
        boolean enComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '\\' && enComillas && i + 1 < linea.length()) {
                i++;
                continue;
            }
            if (c == '"') {
                enComillas = !enComillas;
            } else if (!enComillas) {
                if (c == '#') {
                    return linea.substring(0, i);
                }
                if (c == '/' && i + 1 < linea.length() && linea.charAt(i + 1) == '/') {
                    return linea.substring(0, i);
                }
            }
        }
        return linea;
    }

    private static boolean esModuloLexicoPrism(String source) {
        String[] lineas = source.split("\n");
        for (String raw : lineas) {
            String t = raw.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            String lower = t.toLowerCase();
            return lower.startsWith("version ")
                    || lower.startsWith("lexrule ")
                    || lower.startsWith("lemma ")
                    || lower.startsWith("antonym ")
                    || lower.startsWith("pattern ");
        }
        return false;
    }

    private static List<PrDiagnostic> diagnosticarFuenteLexico(String source, String ruta) {
        String etiqueta = (ruta == null || ruta.isBlank()) ? "lexico.pr" : ruta;
        ResultadoCargaLgs res = LgsCargador.cargarConDiagnostico(
                new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)),
                etiqueta);
        if (!res.bloqueaTraduccion()) {
            return List.of();
        }
        String msg = res.mensaje() != null ? res.mensaje() : "Error de sintaxis en módulo léxico.";
        int line = 1;
        Matcher m = Pattern.compile("Línea\\s+(\\d+)").matcher(msg);
        if (m.find()) {
            try {
                line = Math.max(1, Integer.parseInt(m.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        String[] lineas = source.split("\n", -1);
        int endCol = line <= lineas.length ? Math.max(2, lineas[line - 1].length() + 1) : 20;
        return List.of(new PrDiagnostic(line, 1, line, endCol, "error", msg, null));
    }
}
