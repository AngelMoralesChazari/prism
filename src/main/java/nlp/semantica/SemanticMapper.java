package nlp.semantica;

import java.util.List;
import prism.ir.AndExpr;
import prism.ir.AtomExpr;
import prism.ir.EquivExpr;
import prism.ir.ImpExpr;
import prism.ir.LogicExpr;
import prism.ir.NegExpr;
import prism.ir.OrExpr;
import nlp.lexer.TipoTokenNatural;
import nlp.lexer.TokenNatural;
import nlp.lexicon.BaseConocimiento;
import nlp.lexicon.PatronSemanticoLgs;
import nlp.lexicon.TipoSalidaIrPatron;

/**
 * Traduce secuencias léxicas de lenguaje natural a la IR de Prism.
 * Los patrones se cargan desde {@code prism/core.pr} (directiva {@code pattern});
 * si el archivo no declara ninguno, se usan los patrones MVP embebidos en código.
 */
public class SemanticMapper {
    private final BaseConocimiento baseConocimiento;
    private final List<PatronSemanticoLgs> patrones;

    public SemanticMapper(BaseConocimiento baseConocimiento) {
        this(baseConocimiento, null);
    }

    /**
     * @param patronesDesdeArchivo lista desde {@link nlp.lexicon.ContenidoLgs#patronesSemanticos()};
     *                             si es null o vacía, se aplican patrones predeterminados en código.
     */
    public SemanticMapper(BaseConocimiento baseConocimiento, List<PatronSemanticoLgs> patronesDesdeArchivo) {
        this.baseConocimiento = baseConocimiento;
        if (patronesDesdeArchivo == null || patronesDesdeArchivo.isEmpty()) {
            this.patrones = patronesPredeterminados();
        } else {
            this.patrones = List.copyOf(patronesDesdeArchivo);
        }
    }

    public LogicExpr mapearBloque(String textoBloque, List<TokenNatural> tokens, List<String> pasosDeAnalisis) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }

        for (PatronSemanticoLgs p : patrones) {
            if (coincideForma(tokens, p.forma())) {
                pasosDeAnalisis.add("SemanticMapper: patrón " + p.nombre() + " detectado.");
                return construirDesdePatron(p, tokens);
            }
        }

        String literal = primerLiteral(tokens);
        if (literal != null) {
            pasosDeAnalisis.add("SemanticMapper: fallback a átomo simple.");
            return atomoDesde(literal);
        }

        if (textoBloque == null || textoBloque.isBlank()) {
            return null;
        }
        pasosDeAnalisis.add("SemanticMapper: fallback por texto completo.");
        return atomoDesde(textoBloque);
    }

    private LogicExpr construirDesdePatron(PatronSemanticoLgs p, List<TokenNatural> tokens) {
        AtomExpr izq = atomoDesde(tokens.get(p.indiceIzq()).getLexema());
        AtomExpr der = atomoDesde(tokens.get(p.indiceDer()).getLexema());
        return switch (p.tipoIr()) {
            case IMP -> new ImpExpr(izq, der);
            case AND -> new AndExpr(izq, der);
            case OR -> new OrExpr(izq, der);
            case EQUIV -> new EquivExpr(izq, der);
            case IMP_AND -> {
                AtomExpr medio = atomoDesde(tokens.get(p.indiceMedio()).getLexema());
                yield new ImpExpr(new AndExpr(izq, medio), der);
            }
            case IMP_OR -> {
                AtomExpr medio = atomoDesde(tokens.get(p.indiceMedio()).getLexema());
                yield new ImpExpr(izq, new OrExpr(medio, der));
            }
            case IMP_OR_ANT -> {
                AtomExpr medio = atomoDesde(tokens.get(p.indiceMedio()).getLexema());
                yield new ImpExpr(new OrExpr(izq, medio), der);
            }
            case IMP_AND_CONS -> {
                AtomExpr medio = atomoDesde(tokens.get(p.indiceMedio()).getLexema());
                yield new ImpExpr(izq, new AndExpr(medio, der));
            }
            case IMP_UNLESS -> new ImpExpr(new NegExpr(der), izq);
        };
    }

    private static List<PatronSemanticoLgs> patronesPredeterminados() {
        return List.of(
                new PatronSemanticoLgs(
                        "SI_CONJ_Y_ENTONCES",
                        List.of(
                                TipoTokenNatural.SI,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.Y,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.ENTONCES,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_AND,
                        1,
                        5,
                        3),
                new PatronSemanticoLgs(
                        "SI_ENTONCES_DISY_CONS",
                        List.of(
                                TipoTokenNatural.SI,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.ENTONCES,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.O,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_OR,
                        1,
                        5,
                        3),
                new PatronSemanticoLgs(
                        "SI_ENTONCES_CONJ_CONS",
                        List.of(
                                TipoTokenNatural.SI,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.ENTONCES,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.Y,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_AND_CONS,
                        1,
                        5,
                        3),
                new PatronSemanticoLgs(
                        "SI_DISY_O_ENTONCES",
                        List.of(
                                TipoTokenNatural.SI,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.O,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.ENTONCES,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_OR_ANT,
                        1,
                        5,
                        3),
                new PatronSemanticoLgs(
                        "SI_DISY_O_ELIPTICO",
                        List.of(
                                TipoTokenNatural.SI,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.O,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_OR_ANT,
                        1,
                        4,
                        3),
                new PatronSemanticoLgs(
                        "SIEMPRE_QUE_CONJ_Y_ELIPTICO",
                        List.of(
                                TipoTokenNatural.SIEMPRE_QUE,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.Y,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_AND,
                        1,
                        4,
                        3),
                new PatronSemanticoLgs(
                        "SIEMPRE_QUE_DISY_O_ELIPTICO",
                        List.of(
                                TipoTokenNatural.SIEMPRE_QUE,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.O,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_OR_ANT,
                        1,
                        4,
                        3),
                new PatronSemanticoLgs(
                        "CUANDO_CONJ_Y_ELIPTICO",
                        List.of(
                                TipoTokenNatural.CUANDO,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.Y,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_AND,
                        1,
                        4,
                        3),
                new PatronSemanticoLgs(
                        "CUANDO_DISY_O_ELIPTICO",
                        List.of(
                                TipoTokenNatural.CUANDO,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.O,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_OR_ANT,
                        1,
                        4,
                        3),
                new PatronSemanticoLgs(
                        "SI_CONJ_Y_ELIPTICO",
                        List.of(
                                TipoTokenNatural.SI,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.Y,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_AND,
                        1,
                        4,
                        3),
                new PatronSemanticoLgs(
                        "SI_ENTONCES",
                        List.of(TipoTokenNatural.SI, TipoTokenNatural.LITERAL, TipoTokenNatural.ENTONCES, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        1,
                        3),
                new PatronSemanticoLgs(
                        "SI_ELIPTICO",
                        List.of(TipoTokenNatural.SI, TipoTokenNatural.LITERAL, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        1,
                        2),
                new PatronSemanticoLgs(
                        "CUANDO_ENTONCES",
                        List.of(
                                TipoTokenNatural.CUANDO,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.ENTONCES,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        1,
                        3),
                new PatronSemanticoLgs(
                        "CUANDO_ELIPTICO",
                        List.of(TipoTokenNatural.CUANDO, TipoTokenNatural.LITERAL, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        1,
                        2),
                new PatronSemanticoLgs(
                        "SOLO_SI",
                        List.of(TipoTokenNatural.LITERAL, TipoTokenNatural.SOLO_SI, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        0,
                        2),
                new PatronSemanticoLgs(
                        "A_MENOS_QUE",
                        List.of(TipoTokenNatural.LITERAL, TipoTokenNatural.A_MENOS_QUE, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP_UNLESS,
                        0,
                        2),
                new PatronSemanticoLgs(
                        "SIEMPRE_QUE_ENTONCES",
                        List.of(
                                TipoTokenNatural.SIEMPRE_QUE,
                                TipoTokenNatural.LITERAL,
                                TipoTokenNatural.ENTONCES,
                                TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        1,
                        3),
                new PatronSemanticoLgs(
                        "SIEMPRE_QUE_ELIPTICO",
                        List.of(TipoTokenNatural.SIEMPRE_QUE, TipoTokenNatural.LITERAL, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        1,
                        2),
                new PatronSemanticoLgs(
                        "CONSECUENTE_SI_ANTECEDENTE",
                        List.of(TipoTokenNatural.LITERAL, TipoTokenNatural.SI, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        2,
                        0),
                new PatronSemanticoLgs(
                        "EN_CASO_DE_QUE",
                        List.of(TipoTokenNatural.EN_CASO_DE_QUE, TipoTokenNatural.LITERAL, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.IMP,
                        1,
                        2),
                new PatronSemanticoLgs(
                        "EQUIVALENCIA",
                        List.of(TipoTokenNatural.LITERAL, TipoTokenNatural.SI_Y_SOLO_SI, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.EQUIV,
                        0,
                        2),
                new PatronSemanticoLgs(
                        "CONJUNCION",
                        List.of(TipoTokenNatural.LITERAL, TipoTokenNatural.Y, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.AND,
                        0,
                        2),
                new PatronSemanticoLgs(
                        "DISYUNCION",
                        List.of(TipoTokenNatural.LITERAL, TipoTokenNatural.O, TipoTokenNatural.LITERAL),
                        TipoSalidaIrPatron.OR,
                        0,
                        2));
    }

    private AtomExpr atomoDesde(String fragmentoOriginal) {
        String limpio = fragmentoOriginal == null ? "" : fragmentoOriginal.trim();
        boolean negada = false;
        if (limpio.startsWith("no ")) {
            negada = true;
            limpio = limpio.substring(3).trim();
        }
        String canonico = baseConocimiento.canonicalizarFragmento(limpio);
        return new AtomExpr(canonico.isBlank() ? limpio : canonico, negada);
    }

    private static boolean coincideForma(List<TokenNatural> tokens, List<TipoTokenNatural> forma) {
        if (tokens.size() != forma.size()) {
            return false;
        }
        for (int i = 0; i < forma.size(); i++) {
            if (tokens.get(i).getTipo() != forma.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static String primerLiteral(List<TokenNatural> tokens) {
        for (TokenNatural t : tokens) {
            if (t.getTipo() == TipoTokenNatural.LITERAL && !t.getLexema().isBlank()) {
                return t.getLexema();
            }
        }
        return null;
    }
}