package prism.ir;

import java.util.List;
import prism.RegistroProposiciones;

/**
 * Recorre la IR y produce la cadena de fórmula con símbolos proposicionales asignados por {@link RegistroProposiciones}.
 */
public final class EmitidorFormula {

    private EmitidorFormula() {
    }

    public static String emitir(LogicExpr expr, RegistroProposiciones registro, List<String> pasosDeAnalisis) {
        return emitirInterno(expr, registro, pasosDeAnalisis);
    }

    private static String emitirInterno(LogicExpr expr, RegistroProposiciones registro, List<String> pasosDeAnalisis) {
        if (expr instanceof AtomExpr a) {
            return emitirAtomo(a, registro, pasosDeAnalisis);
        }
        if (expr instanceof NegExpr n) {
            return "¬" + conParentesisSiHaceFalta(n.interior(), registro, pasosDeAnalisis);
        }
        if (expr instanceof AndExpr y) {
            return "(" + emitirInterno(y.izquierda(), registro, pasosDeAnalisis)
                    + " ∧ " + emitirInterno(y.derecha(), registro, pasosDeAnalisis) + ")";
        }
        if (expr instanceof OrExpr o) {
            return "(" + emitirInterno(o.izquierda(), registro, pasosDeAnalisis)
                    + " ∨ " + emitirInterno(o.derecha(), registro, pasosDeAnalisis) + ")";
        }
        if (expr instanceof ImpExpr i) {
            return "(" + emitirInterno(i.antecedente(), registro, pasosDeAnalisis)
                    + " → " + emitirInterno(i.consecuente(), registro, pasosDeAnalisis) + ")";
        }
        if (expr instanceof EquivExpr e) {
            return "(" + emitirInterno(e.izquierda(), registro, pasosDeAnalisis)
                    + " ↔ " + emitirInterno(e.derecha(), registro, pasosDeAnalisis) + ")";
        }
        throw new IllegalStateException("Tipo IR no contemplado: " + expr.getClass());
    }

    private static String emitirAtomo(AtomExpr a, RegistroProposiciones registro, List<String> pasosDeAnalisis) {
        String simbolo = registro.simboloPara(a.fragmentoNormalizado(), pasosDeAnalisis);
        return a.negada() ? "¬" + simbolo : simbolo;
    }

    private static String conParentesisSiHaceFalta(
            LogicExpr interior,
            RegistroProposiciones registro,
            List<String> pasosDeAnalisis
    ) {
        if (interior instanceof AtomExpr) {
            return emitirInterno(interior, registro, pasosDeAnalisis);
        }
        return "(" + emitirInterno(interior, registro, pasosDeAnalisis) + ")";
    }
}
