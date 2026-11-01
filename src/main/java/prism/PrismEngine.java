package prism;

import java.util.ArrayList;
import java.util.List;
import prism.ir.AndExpr;
import prism.ir.AtomExpr;
import prism.ir.EmitidorFormula;
import prism.ir.LogicExpr;
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

    public PrismResult traducir(String textoOriginal) {
        List<String> pasosDeAnalisis = new ArrayList<>();

        if (resultadoCargaLgs.bloqueaTraduccion()) {
            pasosDeAnalisis.add("Error de carga Prism: " + resultadoCargaLgs.mensajeParaUsuario());
            return PrismResult.error(resultadoCargaLgs.mensajeParaUsuario(), pasosDeAnalisis);
        }
        if (resultadoCargaLgs.estado() == EstadoCargaLgs.RECURSO_NO_ENCONTRADO) {
            pasosDeAnalisis.add("Advertencia: " + resultadoCargaLgs.mensajeParaUsuario()
                    + " Se usan lemas y patrones embebidos.");
        }

        RegistroProposiciones registro = new RegistroProposiciones();

        String texto = normalizador.normalizar(textoOriginal);
        if (texto.isEmpty()) {
            return PrismResult.error("No se ingresó texto en lenguaje natural.", pasosDeAnalisis);
        }
        pasosDeAnalisis.add("Entrada normalizada: " + texto);
        List<TokenNatural> lexemas = lexer.tokenizar(texto);
        pasosDeAnalisis.add("Lexemas LN: " + lexemas);

        List<String> segmentos = segmentosCompuestos(texto);
        if (segmentos.isEmpty()) {
            return PrismResult.error(
                    "No pude mapear el enunciado a una forma lógica con las reglas actuales.",
                    pasosDeAnalisis);
        }
        if (segmentos.size() > 1) {
            pasosDeAnalisis.add("Composición: " + segmentos.size() + " bloques unidos por conjunción (∧).");
        }

        LogicExpr expresionFinal = null;
        for (int i = 0; i < segmentos.size(); i++) {
            pasosDeAnalisis.add("Bloque " + (i + 1) + ": " + segmentos.get(i));
            LogicExpr bloque = traducirBloque(segmentos.get(i), pasosDeAnalisis);
            if (bloque == null) {
                return PrismResult.error(
                        "No pude mapear el enunciado a una forma lógica con las reglas actuales.",
                        pasosDeAnalisis);
            }
            expresionFinal = expresionFinal == null ? bloque : new AndExpr(expresionFinal, bloque);
        }

        String formula = EmitidorFormula.emitir(expresionFinal, registro, pasosDeAnalisis);
        return PrismResult.exito(
                formula,
                "Traducción Prism completada.",
                pasosDeAnalisis,
                registro.mapaParaResultado());
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
