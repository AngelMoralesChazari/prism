package prism.pr;

import prism.ir.AndExpr;
import prism.ir.AtomExpr;
import prism.ir.EmitidorFormula;
import prism.ir.EquivExpr;
import prism.ir.ImpExpr;
import prism.ir.LogicExpr;
import prism.ir.NegExpr;
import prism.ir.OrExpr;
import prism.pr.ast.AtomRef;
import prism.pr.ast.BinaryExpr;
import prism.pr.ast.BinaryOp;
import prism.pr.ast.FormulaExpr;
import prism.pr.ast.NotExpr;
import java.util.ArrayList;
import java.util.List;

public final class PrFormulaBridge {

    private PrFormulaBridge() {
    }

    public static LogicExpr toLogicExpr(FormulaExpr expr) {
        if (expr instanceof AtomRef a) {
            return new AtomExpr(a.name(), false);
        }
        if (expr instanceof NotExpr n) {
            return new NegExpr(toLogicExpr(n.inner()));
        }
        if (expr instanceof BinaryExpr b) {
            LogicExpr left = toLogicExpr(b.left());
            LogicExpr right = toLogicExpr(b.right());
            return switch (b.op()) {
                case AND -> new AndExpr(left, right);
                case OR -> new OrExpr(left, right);
                case IMPL -> new ImpExpr(left, right);
                case IFF -> new EquivExpr(left, right);
            };
        }
        throw new IllegalStateException("Expresión no contemplada: " + expr.getClass());
    }

    public static String emitirFormulaClasica(FormulaExpr expr, PrAtomRegistry atomos) {
        LogicExpr logicExpr = toLogicExpr(expr);
        List<String> pasos = new ArrayList<>();
        return EmitidorFormula.emitir(logicExpr, atomos.toRegistroProposiciones(), pasos);
    }

    public static String toDisplayString(FormulaExpr expr) {
        if (expr instanceof AtomRef a) {
            return a.name();
        }
        if (expr instanceof NotExpr n) {
            return "not " + toDisplayString(n.inner());
        }
        if (expr instanceof BinaryExpr b) {
            String op = switch (b.op()) {
                case AND -> "and";
                case OR -> "or";
                case IMPL -> "impl";
                case IFF -> "iff";
            };
            return toDisplayString(b.left()) + " " + op + " " + toDisplayString(b.right());
        }
        throw new IllegalStateException("Expresión no contemplada: " + expr.getClass());
    }
}
