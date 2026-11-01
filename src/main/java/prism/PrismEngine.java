package prism;

import java.util.ArrayList;
import java.util.List;
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
 * Orquesta normalización → lexemas (diagnóstico) → patrones → IR → emisión de fórmula.
 * Patrones semánticos: declarativos en {@code prism/core.pr} o respaldo embebido en {@link SemanticMapper}.
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
    private final SemanticMapper semanticMapper = new SemanticMapper(baseConocimiento, contenidoLgs.patronesSemanticos());

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

        RegistroProposiciones registro = registroPredefinido != null ? registroPredefinido : new RegistroProposiciones();

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

    private static record ConectorMatch(int indice, int longitud) {}

    private static ConectorMatch encontrarSeparadorDiscursivo(String s) {
        String[] conPuntuacion = {
                ", pero ",
                ", sin embargo, ",
                ", sin embargo ",
                ", no obstante, ",
                ", no obstante ",
                "; pero ",
                "; "
        };
        for (String c : conPuntuacion) {
            int idx = s.indexOf(c);
            if (idx > 0) {
                return new ConectorMatch(idx, c.length());
            }
        }
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

    private LogicExpr traducirJerarquico(String texto, List<String> pasosDeAnalisis) {
        String limpio = limpiarSegmento(texto);
        if (limpio.isEmpty()) {
            return null;
        }

        // 0. Coordinación discursiva inter-oracional (adversativas y coordinadas de nivel superior: ", pero ", etc.)
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

        // 1. Conector mayor: bicondicional "si y solo si"
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

        // 2. Composición de cláusulas condicionales separadas por coma
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

        // 3. Negación oracional composicional ("no es cierto que [E]", "no es verdad que [E]", etc.)
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

        // 4. Cláusula base (mapeo léxico-semántico)
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
            boolean validoDespues = (fin == s.length() || Character.isWhitespace(s.charAt(fin)) || s.charAt(fin) == ',');
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
        while (r.startsWith(",") || r.startsWith(";")) {
            r = r.substring(1).trim();
        }
        while (r.endsWith(",") || r.endsWith(";")) {
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
     * Parte el texto en bloques traducibles: coma seguida de {@code si } (nueva cláusula condicional)
     * y el caso especial {@code , en caso de que} (segundo bloque con prefijo restaurado).
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
