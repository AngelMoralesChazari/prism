package prism.pr;

import nlp.lexicon.BaseConocimiento;
import prism.PrismResult;
import prism.RegistroProposiciones;
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
import prism.pr.ast.FormulaExpr;
import prism.pr.ast.NotExpr;
import prism.vertauto.ParserNotacionClasica;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PrFormulaBridge {

    public record ResolvedFormula(
            LogicExpr logicExpr,
            String formulaClasica,
            Map<String, String> etiquetasPorSimbolo) {
    }

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

    /**
     * Expande una expresión de fórmula resolviendo referencias a átomos declarados,
     * resultados de {@code let ... = translate(...)} y otras definiciones
     * {@code formula}.
     */
    public static ResolvedFormula resolverCompuesto(
            FormulaExpr expr,
            PrAtomRegistry atomRegistry,
            BaseConocimiento baseConocimiento,
            Map<String, PrismResult> letResultados,
            Map<String, FormulaExpr> formulaExprs) {
        PrAtomRegistry registrySeguro = atomRegistry != null ? atomRegistry : PrAtomRegistry.vacio();
        RegistroProposiciones registroUnificado = registrySeguro.toRegistroProposiciones(baseConocimiento);
        Map<String, String> etiquetasPorSimbolo = new LinkedHashMap<>();
        Set<String> visitando = new HashSet<>();

        LogicExpr logicExpr = expandir(
                expr,
                registrySeguro,
                baseConocimiento,
                letResultados == null ? Map.of() : letResultados,
                formulaExprs == null ? Map.of() : formulaExprs,
                registroUnificado,
                etiquetasPorSimbolo,
                visitando);
        String formulaClasica = EmitidorFormula.emitirSinRegistro(logicExpr);
        return new ResolvedFormula(logicExpr, formulaClasica, Collections.unmodifiableMap(etiquetasPorSimbolo));
    }

    private static LogicExpr expandir(
            FormulaExpr expr,
            PrAtomRegistry atomRegistry,
            BaseConocimiento baseConocimiento,
            Map<String, PrismResult> letResultados,
            Map<String, FormulaExpr> formulaExprs,
            RegistroProposiciones registroUnificado,
            Map<String, String> etiquetasPorSimbolo,
            Set<String> visitando) {
        if (expr instanceof AtomRef a) {
            String nombre = a.name();
            if (atomRegistry.contieneSimbolo(nombre)) {
                etiquetasPorSimbolo.put(nombre, atomRegistry.etiquetaDe(nombre));
                return new AtomExpr(nombre, false);
            }
            PrismResult letRes = letResultados.get(nombre);
            if (letRes != null) {
                return expandirDesdeLet(letRes, atomRegistry, baseConocimiento, registroUnificado, etiquetasPorSimbolo);
            }
            FormulaExpr subFormula = formulaExprs.get(nombre);
            if (subFormula != null) {
                if (!visitando.add(nombre)) {
                    throw new PrRuntimeException("Referencia circular en fórmula: " + nombre);
                }
                try {
                    return expandir(subFormula, atomRegistry, baseConocimiento, letResultados, formulaExprs,
                            registroUnificado, etiquetasPorSimbolo, visitando);
                } finally {
                    visitando.remove(nombre);
                }
            }
            return new AtomExpr(nombre, false);
        }
        if (expr instanceof NotExpr n) {
            return new NegExpr(expandir(n.inner(), atomRegistry, baseConocimiento, letResultados, formulaExprs,
                    registroUnificado, etiquetasPorSimbolo, visitando));
        }
        if (expr instanceof BinaryExpr b) {
            LogicExpr left = expandir(b.left(), atomRegistry, baseConocimiento, letResultados, formulaExprs,
                    registroUnificado, etiquetasPorSimbolo, visitando);
            LogicExpr right = expandir(b.right(), atomRegistry, baseConocimiento, letResultados, formulaExprs,
                    registroUnificado, etiquetasPorSimbolo, visitando);
            return switch (b.op()) {
                case AND -> new AndExpr(left, right);
                case OR -> new OrExpr(left, right);
                case IMPL -> new ImpExpr(left, right);
                case IFF -> new EquivExpr(left, right);
            };
        }
        throw new IllegalStateException("Expresión no contemplada: " + expr.getClass());
    }

    private static LogicExpr expandirDesdeLet(
            PrismResult letRes,
            PrAtomRegistry atomRegistry,
            BaseConocimiento baseConocimiento,
            RegistroProposiciones registroUnificado,
            Map<String, String> etiquetasPorSimbolo) {
        LogicExpr parsed = ParserNotacionClasica.parsear(letRes.getFormula());
        Map<String, String> localSimAFragmento = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : letRes.getProposiciones().entrySet()) {
            localSimAFragmento.putIfAbsent(entry.getValue(), entry.getKey());
        }

        Map<String, String> remap = new HashMap<>();
        for (String simLocal : recolectarAtomosEnOrden(parsed)) {
            if (atomRegistry.contieneSimbolo(simLocal)) {
                remap.put(simLocal, simLocal);
                etiquetasPorSimbolo.put(simLocal, atomRegistry.etiquetaDe(simLocal));
                continue;
            }
            String frag = localSimAFragmento.get(simLocal);
            if (frag != null && !frag.isBlank()) {
                String clave = baseConocimiento != null ? baseConocimiento.canonicalizarFragmento(frag) : frag;
                String target = registroUnificado.simboloPara(clave.isBlank() ? frag : clave, null);
                remap.put(simLocal, target);
                etiquetasPorSimbolo.putIfAbsent(target, frag);
            } else {
                String target = registroUnificado.simboloPara(simLocal, null);
                remap.put(simLocal, target);
            }
        }
        return renombrarAtomos(parsed, remap);
    }

    private static List<String> recolectarAtomosEnOrden(LogicExpr expr) {
        Set<String> vistos = new LinkedHashSet<>();
        recolectarRec(expr, vistos);
        return new ArrayList<>(vistos);
    }

    private static void recolectarRec(LogicExpr expr, Set<String> out) {
        if (expr instanceof AtomExpr a) {
            out.add(a.fragmentoNormalizado());
        } else if (expr instanceof NegExpr n) {
            recolectarRec(n.interior(), out);
        } else if (expr instanceof AndExpr y) {
            recolectarRec(y.izquierda(), out);
            recolectarRec(y.derecha(), out);
        } else if (expr instanceof OrExpr o) {
            recolectarRec(o.izquierda(), out);
            recolectarRec(o.derecha(), out);
        } else if (expr instanceof ImpExpr i) {
            recolectarRec(i.antecedente(), out);
            recolectarRec(i.consecuente(), out);
        } else if (expr instanceof EquivExpr e) {
            recolectarRec(e.izquierda(), out);
            recolectarRec(e.derecha(), out);
        }
    }

    private static LogicExpr renombrarAtomos(LogicExpr expr, Map<String, String> remap) {
        if (expr instanceof AtomExpr a) {
            String nuevo = remap.getOrDefault(a.fragmentoNormalizado(), a.fragmentoNormalizado());
            return new AtomExpr(nuevo, a.negada());
        }
        if (expr instanceof NegExpr n) {
            return new NegExpr(renombrarAtomos(n.interior(), remap));
        }
        if (expr instanceof AndExpr y) {
            return new AndExpr(renombrarAtomos(y.izquierda(), remap), renombrarAtomos(y.derecha(), remap));
        }
        if (expr instanceof OrExpr o) {
            return new OrExpr(renombrarAtomos(o.izquierda(), remap), renombrarAtomos(o.derecha(), remap));
        }
        if (expr instanceof ImpExpr i) {
            return new ImpExpr(renombrarAtomos(i.antecedente(), remap), renombrarAtomos(i.consecuente(), remap));
        }
        if (expr instanceof EquivExpr e) {
            return new EquivExpr(renombrarAtomos(e.izquierda(), remap), renombrarAtomos(e.derecha(), remap));
        }
        throw new IllegalStateException("Tipo IR no contemplado: " + expr.getClass());
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
