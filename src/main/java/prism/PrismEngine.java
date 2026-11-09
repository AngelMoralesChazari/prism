package prism;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import prism.ir.AndExpr;
import prism.ir.AtomExpr;
import prism.ir.EmitidorFormula;
import prism.ir.EquivExpr;
import prism.ir.ImpExpr;
import prism.ir.LogicExpr;
import prism.ir.NegExpr;
import prism.ir.OrExpr;
import nlp.lexer.NaturalLexer;
import nlp.lexer.TokenNatural;
import nlp.lexicon.BaseConocimiento;
import nlp.lexicon.ContenidoLgs;
import nlp.lexicon.EstadoCargaLgs;
import nlp.lexicon.LgsCargador;
import nlp.lexicon.ResultadoCargaLgs;
import nlp.normalizacion.NormalizadorTexto;
import nlp.semantica.SemanticMapper;

/**
 * Orquesta normalización → lexemas (diagnóstico) → patrones → IR → emisión de
 * fórmula.
 * Patrones semánticos: declarativos en {@code prism/core.pr} o respaldo
 * embebido en {@link SemanticMapper}.
 */
public class PrismEngine {
    private static final String RECURSO_LGS = "prism/core.pr";

    private final NormalizadorTexto normalizador = new NormalizadorTexto();
    private final NaturalLexer lexer = new NaturalLexer();
    private final ResultadoCargaLgs resultadoCargaLgs = LgsCargador.cargarConDiagnostico(RECURSO_LGS);
    private final ContenidoLgs contenidoLgs = resultadoCargaLgs.bloqueaTraduccion()
            ? ContenidoLgs.vacio()
            : resultadoCargaLgs.contenido();
    private final BaseConocimiento baseConocimiento = new BaseConocimiento(contenidoLgs);
    private final SemanticMapper semanticMapper = new SemanticMapper(baseConocimiento,
            contenidoLgs.patronesSemanticos());

    public BaseConocimiento getBaseConocimiento() {
        return baseConocimiento;
    }

    public PrismResult traducir(String textoOriginal) {
        return traducir(textoOriginal, null);
    }

    public PrismResult traducir(String textoOriginal, RegistroProposiciones registroPredefinido) {
        List<String> pasosDeAnalisis = new ArrayList<>();

        if (resultadoCargaLgs.bloqueaTraduccion()) {
            pasosDeAnalisis.add("Error de carga Prism: " + resultadoCargaLgs.mensajeParaUsuario());
            return PrismResult.error(resultadoCargaLgs.mensajeParaUsuario(), pasosDeAnalisis);
        }
        if (resultadoCargaLgs.estado() == EstadoCargaLgs.RECURSO_NO_ENCONTRADO) {
            pasosDeAnalisis.add("Advertencia: " + resultadoCargaLgs.mensajeParaUsuario()
                    + " Se usan lemas y patrones embebidos.");
        }

        RegistroProposiciones registro = registroPredefinido != null ? registroPredefinido
                : new RegistroProposiciones();

        String texto = normalizador.normalizar(textoOriginal);
        if (texto.isEmpty()) {
            return PrismResult.error("No se ingresó texto en lenguaje natural.", pasosDeAnalisis);
        }
        pasosDeAnalisis.add("Entrada normalizada: " + texto);
        List<TokenNatural> lexemas = lexer.tokenizar(texto);
        pasosDeAnalisis.add("Lexemas LN: " + lexemas);

        LogicExpr expresionFinal = traducirJerarquico(texto, pasosDeAnalisis);
        if (expresionFinal == null) {
            return PrismResult.error(
                    "No pude mapear el enunciado a una forma lógica con las reglas actuales.",
                    pasosDeAnalisis);
        }

        String formula = EmitidorFormula.emitir(expresionFinal, registro, pasosDeAnalisis);
        return PrismResult.exito(
                formula,
                "Traducción Prism completada.",
                pasosDeAnalisis,
                registro.mapaParaResultado());
    }

    private static record ConectorMatch(int indice, int longitud) {
    }

    private static final Pattern PATRON_CONCLUSIVO_FUERTE = Pattern.compile(
            "(?i)(?:\\s*[.;,]\\s*(?:y(?:\\s*,\\s*|\\s+))?|\\s+y(?:\\s*,\\s*|\\s+)|\\s+)"
                    + "(?:por\\s+lo\\s+tanto|por\\s+consiguiente|en\\s+consecuencia|asi\\s+que|asi\\s+pues)"
                    + "(?=\\s|,|$)");

    private static final Pattern PATRON_CONCLUSIVO_LUEGO = Pattern.compile(
            "(?i)\\s*[.;,]\\s*(?:y(?:\\s*,\\s*|\\s+))?luego(?=\\s|,|$)");

    private static final String[] SUFIJOS_CONTINUACION_DISCURSIVA = {
            "pero ",
            "sin embargo, ",
            "sin embargo ",
            "no obstante, ",
            "no obstante ",
            "ademas, ",
            "ademas ",
            "aunque ",
            "y "
    };

    private static ConectorMatch encontrarConectorConclusivo(String s) {
        for (Pattern patron : new Pattern[] { PATRON_CONCLUSIVO_FUERTE, PATRON_CONCLUSIVO_LUEGO }) {
            Matcher m = patron.matcher(s);
            while (m.find()) {
                if (m.start() <= 0 || m.end() > s.length()) {
                    continue;
                }
                String izq = limpiarSegmento(s.substring(0, m.start()));
                String der = limpiarSegmento(s.substring(m.end()));
                if (!izq.isEmpty() && !der.isEmpty()) {
                    return new ConectorMatch(m.start(), m.end() - m.start());
                }
            }
        }
        return null;
    }

    private static ConectorMatch encontrarDelimitadorMayor(String s, String delimitador) {
        int idx = s.lastIndexOf(delimitador);
        while (idx > 0) {
            int posDespues = idx + delimitador.length();
            int lenExtra = 0;
            String resto = s.substring(posDespues).toLowerCase();
            for (String suf : SUFIJOS_CONTINUACION_DISCURSIVA) {
                if (resto.startsWith(suf)) {
                    lenExtra = suf.length();
                    break;
                }
            }
            String izq = limpiarSegmento(s.substring(0, idx));
            String der = limpiarSegmento(s.substring(posDespues + lenExtra));
            if (!izq.isEmpty() && !der.isEmpty()) {
                return new ConectorMatch(idx, delimitador.length() + lenExtra);
            }
            idx = s.lastIndexOf(delimitador, idx - 1);
        }
        return null;
    }

    private ConectorMatch encontrarSeparadorDiscursivo(String s) {
        // Nivel 1a y 1b: límites explícitos de oración ('. ') y de cláusula ('; ')
        ConectorMatch porPunto = encontrarDelimitadorMayor(s, ". ");
        if (porPunto != null) {
            return porPunto;
        }
        ConectorMatch porPuntoYComa = encontrarDelimitadorMayor(s, "; ");
        if (porPuntoYComa != null) {
            return porPuntoYComa;
        }

        // Nivel 1c: adversativas y aditivas explícitas con coma
        String[] conComaExplicita = {
                ", pero ",
                ", sin embargo, ",
                ", sin embargo ",
                ", no obstante, ",
                ", no obstante ",
                ", ademas, ",
                ", ademas ",
                ", asimismo, ",
                ", asimismo ",
                ", asi mismo ",
                ", aunque "
        };
        for (String c : conComaExplicita) {
            int idx = s.indexOf(c);
            if (idx > 0) {
                return new ConectorMatch(idx, c.length());
            }
        }

        // Nivel 1d: coordinación ', y ' entre cláusulas independientes (sin romper
        // antecedentes condicionales)
        int idxComaY = s.indexOf(", y ");
        while (idxComaY > 0) {
            String izq = limpiarSegmento(s.substring(0, idxComaY));
            String der = limpiarSegmento(s.substring(idxComaY + ", y ".length()));
            boolean condicionalAbierto = esCondicionalSinCerrar(izq)
                    && (tieneEntoncesExterno(der) || !esCondicionalElipticoCerrado(izq));
            if (!izq.isEmpty() && !der.isEmpty() && !condicionalAbierto) {
                return new ConectorMatch(idxComaY, ", y ".length());
            }
            idxComaY = s.indexOf(", y ", idxComaY + 1);
        }

        // Nivel 1e: adversativas sin puntuación
        String[] sinPuntuacion = {
                " sin embargo ",
                " no obstante ",
                " pero "
        };
        for (String c : sinPuntuacion) {
            int idx = s.indexOf(c);
            if (idx > 0) {
                return new ConectorMatch(idx, c.length());
            }
        }
        return null;
    }

    private static ConectorMatch encontrarComaSeparadoraPremisas(String s) {
        int idx = s.indexOf(", ");
        while (idx > 0) {
            String izq = limpiarSegmento(s.substring(0, idx));
            String der = limpiarSegmento(s.substring(idx + 2));
            if (!izq.isEmpty()
                    && !der.isEmpty()
                    && !derIniciaConContinuacion(der)
                    && !izqTerminaEnConector(izq)
                    && !esCondicionalSinCerrar(izq)) {
                return new ConectorMatch(idx, 2);
            }
            idx = s.indexOf(", ", idx + 2);
        }
        return null;
    }

    private static boolean derIniciaConContinuacion(String der) {
        String d = der.toLowerCase();
        return d.equals("entonces")
                || d.startsWith("entonces ")
                || d.startsWith("si y solo si")
                || d.startsWith("pero ")
                || d.startsWith("sin embargo")
                || d.startsWith("no obstante")
                || d.startsWith("aunque ")
                || d.startsWith("sino ")
                || d.startsWith("o bien ")
                || d.startsWith("o ")
                || d.startsWith("u ")
                || d.startsWith("y ")
                || d.startsWith("ni ");
    }

    private static boolean izqTerminaEnConector(String izq) {
        String i = izq.toLowerCase();
        return i.equals("entonces") || i.endsWith(" entonces")
                || i.equals("y") || i.endsWith(" y")
                || i.equals("o") || i.endsWith(" o")
                || i.equals("ni") || i.endsWith(" ni")
                || i.equals("si") || i.endsWith(" si")
                || i.endsWith(" pero")
                || i.endsWith(" sin embargo")
                || i.endsWith(" no obstante")
                || i.endsWith(" aunque");
    }

    private static boolean esCondicionalSinCerrar(String izq) {
        String base = izq.toLowerCase().trim();
        String negador = encontrarPrefijoNegadorOracional(base);
        while (negador != null) {
            base = limpiarSegmento(base.substring(negador.length())).toLowerCase();
            negador = encontrarPrefijoNegadorOracional(base);
        }
        if (base.startsWith("si y solo si ")) {
            return false;
        }
        boolean iniciaCondicional = base.startsWith("si ")
                || base.startsWith("cuando ")
                || base.startsWith("siempre que ")
                || base.startsWith("en caso de que ")
                || base.startsWith("dado que ")
                || base.startsWith("puesto que ")
                || base.startsWith("ya que ");
        if (!iniciaCondicional) {
            return false;
        }
        // Condicional cerrado explícitamente solo si hay ' entonces <consecuente no
        // vacío>'
        Matcher m = Pattern.compile("\\bentonces\\b").matcher(base);
        int ultimoFinEntonces = -1;
        while (m.find()) {
            ultimoFinEntonces = m.end();
        }
        if (ultimoFinEntonces < 0) {
            return true;
        }
        String consecuente = limpiarSegmento(base.substring(ultimoFinEntonces));
        return consecuente.isEmpty();
    }

    private boolean esCondicionalElipticoCerrado(String izq) {
        String limpio = limpiarSegmento(izq);
        if (limpio.isEmpty()) {
            return false;
        }
        String negador = encontrarPrefijoNegadorOracional(limpio);
        while (negador != null) {
            limpio = limpiarSegmento(limpio.substring(negador.length()));
            negador = encontrarPrefijoNegadorOracional(limpio);
        }
        List<TokenNatural> tokens = lexer.tokenizar(limpio);
        LogicExpr expr = semanticMapper.mapearBloque(limpio, tokens, new ArrayList<>());
        return expr instanceof ImpExpr;
    }

    private static boolean tieneEntoncesExterno(String der) {
        String d = der.toLowerCase().trim();
        if (d.equals("entonces") || d.startsWith("entonces ")) {
            return true;
        }
        Matcher mEntonces = Pattern.compile("\\bentonces\\b").matcher(d);
        if (!mEntonces.find()) {
            return false;
        }
        int posEntonces = mEntonces.start();
        String antes = d.substring(0, posEntonces);
        return !(antes.startsWith("si ")
                || antes.startsWith("cuando ")
                || antes.startsWith("siempre que ")
                || antes.startsWith("en caso de que ")
                || antes.startsWith("dado que ")
                || antes.startsWith("puesto que ")
                || antes.startsWith("ya que "));
    }

    private LogicExpr traducirJerarquico(String texto, List<String> pasosDeAnalisis) {
        String limpio = limpiarSegmento(texto);
        if (limpio.isEmpty()) {
            return null;
        }

        // 0. Conector conclusivo de argumento de nivel superior ("por lo tanto", "por
        // consiguiente", etc.)
        ConectorMatch matchConclusivo = encontrarConectorConclusivo(limpio);
        if (matchConclusivo != null) {
            String izqTexto = limpiarSegmento(limpio.substring(0, matchConclusivo.indice()));
            String derTexto = limpiarSegmento(limpio.substring(matchConclusivo.indice() + matchConclusivo.longitud()));
            if (!izqTexto.isEmpty() && !derTexto.isEmpty()) {
                pasosDeAnalisis
                        .add("Jerarquía: conector conclusivo de argumento (→) conectando premisas con conclusión.");
                LogicExpr izq = traducirJerarquico(izqTexto, pasosDeAnalisis);
                LogicExpr der = traducirJerarquico(derTexto, pasosDeAnalisis);
                if (izq != null && der != null) {
                    return new ImpExpr(izq, der);
                }
            }
        }

        // 1. Coordinación discursiva / separadores de premisas (". ", "; ", ", pero ",
        // ", y ", etc.)
        ConectorMatch matchDiscursivo = encontrarSeparadorDiscursivo(limpio);
        if (matchDiscursivo != null) {
            String izqTexto = limpiarSegmento(limpio.substring(0, matchDiscursivo.indice()));
            String derTexto = limpiarSegmento(limpio.substring(matchDiscursivo.indice() + matchDiscursivo.longitud()));
            if (!izqTexto.isEmpty() && !derTexto.isEmpty()) {
                pasosDeAnalisis.add("Jerarquía: coordinación adversativa/discursiva (∧) conectando dos cláusulas.");
                LogicExpr izq = traducirJerarquico(izqTexto, pasosDeAnalisis);
                LogicExpr der = traducirJerarquico(derTexto, pasosDeAnalisis);
                if (izq != null && der != null) {
                    return new AndExpr(izq, der);
                }
            }
        }

        // 2. Conector mayor: bicondicional "si y solo si"
        int idxIff = encontrarConectorMayor(limpio, "si y solo si");
        if (idxIff >= 0) {
            String izqTexto = limpiarSegmento(limpio.substring(0, idxIff));
            String derTexto = limpiarSegmento(limpio.substring(idxIff + "si y solo si".length()));
            if (!izqTexto.isEmpty() && !derTexto.isEmpty()) {
                pasosDeAnalisis.add("Jerarquía: bicondicional (↔) conectando dos cláusulas.");
                LogicExpr izq = traducirJerarquico(izqTexto, pasosDeAnalisis);
                LogicExpr der = traducirJerarquico(derTexto, pasosDeAnalisis);
                if (izq != null && der != null) {
                    return new EquivExpr(izq, der);
                }
            }
        }

        // 3. Composición de cláusulas condicionales separadas por coma
        List<String> segmentos = segmentosCompuestos(limpio);
        if (segmentos.size() > 1) {
            pasosDeAnalisis.add("Composición: " + segmentos.size() + " bloques unidos por conjunción (∧).");
            LogicExpr acumulada = null;
            for (int i = 0; i < segmentos.size(); i++) {
                pasosDeAnalisis.add("Bloque " + (i + 1) + ": " + segmentos.get(i));
                LogicExpr bloque = traducirJerarquico(segmentos.get(i), pasosDeAnalisis);
                if (bloque == null) {
                    return null;
                }
                acumulada = acumulada == null ? bloque : new AndExpr(acumulada, bloque);
            }
            return acumulada;
        }

        // 4. Premisas independientes separadas por coma cuando no forman un único
        // condicional abierto
        ConectorMatch matchComa = encontrarComaSeparadoraPremisas(limpio);
        if (matchComa != null) {
            String izqTexto = limpiarSegmento(limpio.substring(0, matchComa.indice()));
            String derTexto = limpiarSegmento(limpio.substring(matchComa.indice() + matchComa.longitud()));
            if (!izqTexto.isEmpty() && !derTexto.isEmpty()) {
                pasosDeAnalisis.add("Jerarquía: conjunción de premisas separadas por coma (∧).");
                LogicExpr izq = traducirJerarquico(izqTexto, pasosDeAnalisis);
                LogicExpr der = traducirJerarquico(derTexto, pasosDeAnalisis);
                if (izq != null && der != null) {
                    return new AndExpr(izq, der);
                }
            }
        }

        // 5. Negación oracional composicional ("no es cierto que [E]", "no es verdad
        // que [E]", etc.)
        String negadorEncontrado = encontrarPrefijoNegadorOracional(limpio);
        if (negadorEncontrado != null) {
            String resto = limpiarSegmento(limpio.substring(negadorEncontrado.length()));
            if (!resto.isEmpty()) {
                pasosDeAnalisis.add("Jerarquía: negación oracional (¬) envolviendo proposición subordinada.");
                LogicExpr subExpr = traducirJerarquico(resto, pasosDeAnalisis);
                if (subExpr != null) {
                    return new NegExpr(subExpr);
                }
            }
        }

        // 6. Disyunción correlativa inicial "o A o B"
        String limpioLower = limpio.toLowerCase();
        if (limpioLower.startsWith("o ") && !limpioLower.startsWith("o bien ") && limpioLower.indexOf(" o ", 2) > 2) {
            limpio = limpio.substring(2).trim();
        }

        // 7. Cláusula base (mapeo léxico-semántico)
        return traducirBloque(limpio, pasosDeAnalisis);
    }

    private static final String[] PREFIJOS_NEGADORES_ORACIONALES = {
            "no es cierto que ",
            "no es verdad que ",
            "es falso que ",
            "no ocurre que ",
            "no se da que ",
            "no se cumple que "
    };

    private static String encontrarPrefijoNegadorOracional(String s) {
        String minuscula = s.toLowerCase();
        for (String p : PREFIJOS_NEGADORES_ORACIONALES) {
            if (minuscula.startsWith(p)) {
                return p;
            }
        }
        return null;
    }

    private static int encontrarConectorMayor(String s, String conector) {
        int idx = s.indexOf(conector);
        while (idx >= 0) {
            boolean validoAntes = (idx == 0 || Character.isWhitespace(s.charAt(idx - 1)) || s.charAt(idx - 1) == ',');
            int fin = idx + conector.length();
            boolean validoDespues = (fin == s.length() || Character.isWhitespace(s.charAt(fin))
                    || s.charAt(fin) == ',');
            if (validoAntes && validoDespues) {
                return idx;
            }
            idx = s.indexOf(conector, idx + 1);
        }
        return -1;
    }

    private static String limpiarSegmento(String s) {
        if (s == null) {
            return "";
        }
        String r = s.trim();
        while (r.startsWith(",") || r.startsWith(";") || r.startsWith(".")) {
            r = r.substring(1).trim();
        }
        while (r.endsWith(",") || r.endsWith(";")
                || (r.endsWith(".") && (r.length() < 2 || !Character.isDigit(r.charAt(r.length() - 2))))) {
            r = r.substring(0, r.length() - 1).trim();
        }
        return r;
    }

    private LogicExpr traducirBloque(String bloque, List<String> pasosDeAnalisis) {
        String limpio = bloque.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        List<TokenNatural> tokensBloque = lexer.tokenizar(limpio);
        pasosDeAnalisis.add("Tokens bloque: " + tokensBloque);
        LogicExpr expr = semanticMapper.mapearBloque(limpio, tokensBloque, pasosDeAnalisis);
        if (expr != null) {
            return expr;
        }
        pasosDeAnalisis.add("Fallback local: átomo simple.");
        return new AtomExpr(baseConocimiento.canonicalizarFragmento(limpio), false);
    }

    /**
     * Parte el texto en bloques traducibles: coma seguida de {@code si } (nueva
     * cláusula condicional)
     * y el caso especial {@code , en caso de que} (segundo bloque con prefijo
     * restaurado).
     */
    private static List<String> segmentosCompuestos(String texto) {
        List<String> segmentos = new ArrayList<>();
        String[] porComaSi = texto.split("\\s*,\\s*(?=(?:si|siempre que|cuando)\\s)", -1);
        for (String tramo : porComaSi) {
            String limpio = tramo.trim();
            if (limpio.isEmpty()) {
                continue;
            }
            String[] porEnCaso = limpio.split("\\s*,\\s*en caso de que\\s+", 2);
            segmentos.add(porEnCaso[0].trim());
            if (porEnCaso.length == 2) {
                String segundo = porEnCaso[1].trim();
                if (!segundo.isEmpty()) {
                    segmentos.add("en caso de que " + segundo);
                }
            }
        }
        return segmentos;
    }
}
