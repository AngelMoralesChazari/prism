package prism.vertauto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import prism.ir.AndExpr;
import prism.ir.AtomExpr;
import prism.ir.EquivExpr;
import prism.ir.ImpExpr;
import prism.ir.LogicExpr;
import prism.ir.NegExpr;
import prism.ir.OrExpr;

/**
 * Evalúa fórmulas proposicionales (IR), construye tabla de verdad y clasifica el dictamen.
 */
public final class EvaluadorProposicional {

    private EvaluadorProposicional() {
    }

    public static VertautoResult evaluar(LogicExpr expr, String formulaMostrada) {
        List<String> atomos = recolectarAtomos(expr);
        List<String> pasos = new ArrayList<>();
        pasos.add("Identificación de proposiciones atómicas: " + atomos);
        pasos.add("Formalización: " + formulaMostrada);
        int filas = atomos.isEmpty() ? 1 : (1 << atomos.size());
        pasos.add("Construcción del espacio de estados: 2^" + atomos.size() + " = " + filas + " filas.");

        List<String> filasTabla = new ArrayList<>();
        if (atomos.isEmpty()) {
            boolean valor = evaluarEn(expr, Map.of());
            filasTabla.add("resultado=" + marca(valor));
            TipoDictamen d = clasificar(List.of(valor));
            pasos.add("Dictamen: " + d.etiquetaCorta());
            return new VertautoResult(formulaMostrada, d, atomos, filasTabla, pasos);
        }

        String encabezado = String.join(" | ", atomos) + " | resultado";
        filasTabla.add(encabezado);
        List<Boolean> resultados = new ArrayList<>();

        int n = atomos.size();
        for (int mascara = 0; mascara < filas; mascara++) {
            Map<String, Boolean> asignacion = new TreeMap<>();
            StringBuilder filaHumana = new StringBuilder();
            for (int i = 0; i < n; i++) {
                boolean v = ((mascara >> (n - 1 - i)) & 1) == 1;
                asignacion.put(atomos.get(i), v);
                if (i > 0) {
                    filaHumana.append(" | ");
                }
                filaHumana.append(marca(v));
            }
            boolean resultado = evaluarEn(expr, asignacion);
            resultados.add(resultado);
            filaHumana.append(" | ").append(marca(resultado));
            filasTabla.add(filaHumana.toString());
            pasos.add("Fila " + (mascara + 1) + ": asignación " + asignacion + " → " + marca(resultado));
        }

        TipoDictamen dictamen = clasificar(resultados);
        pasos.add("Dictamen final: " + dictamen.etiquetaCorta());
        return new VertautoResult(formulaMostrada, dictamen, atomos, filasTabla, pasos);
    }

    public static List<String> recolectarAtomos(LogicExpr expr) {
        Set<String> set = new LinkedHashSet<>();
        recolectarAtomosRec(expr, set);
        List<String> lista = new ArrayList<>(set);
        Collections.sort(lista);
        return lista;
    }

    private static void recolectarAtomosRec(LogicExpr expr, Set<String> out) {
        if (expr instanceof AtomExpr a) {
            out.add(a.fragmentoNormalizado());
            return;
        }
        if (expr instanceof NegExpr n) {
            recolectarAtomosRec(n.interior(), out);
            return;
        }
        if (expr instanceof AndExpr y) {
            recolectarAtomosRec(y.izquierda(), out);
            recolectarAtomosRec(y.derecha(), out);
            return;
        }
        if (expr instanceof OrExpr o) {
            recolectarAtomosRec(o.izquierda(), out);
            recolectarAtomosRec(o.derecha(), out);
            return;
        }
        if (expr instanceof ImpExpr i) {
            recolectarAtomosRec(i.antecedente(), out);
            recolectarAtomosRec(i.consecuente(), out);
            return;
        }
        if (expr instanceof EquivExpr e) {
            recolectarAtomosRec(e.izquierda(), out);
            recolectarAtomosRec(e.derecha(), out);
        }
    }

    static boolean evaluarEn(LogicExpr expr, Map<String, Boolean> env) {
        if (expr instanceof AtomExpr a) {
            boolean base = env.getOrDefault(a.fragmentoNormalizado(), false);
            return a.negada() ? !base : base;
        }
        if (expr instanceof NegExpr n) {
            return !evaluarEn(n.interior(), env);
        }
        if (expr instanceof AndExpr y) {
            return evaluarEn(y.izquierda(), env) && evaluarEn(y.derecha(), env);
        }
        if (expr instanceof OrExpr o) {
            return evaluarEn(o.izquierda(), env) || evaluarEn(o.derecha(), env);
        }
        if (expr instanceof ImpExpr i) {
            return !evaluarEn(i.antecedente(), env) || evaluarEn(i.consecuente(), env);
        }
        if (expr instanceof EquivExpr e) {
            return evaluarEn(e.izquierda(), env) == evaluarEn(e.derecha(), env);
        }
        throw new IllegalStateException("Tipo IR no contemplado: " + expr.getClass());
    }

    static TipoDictamen clasificar(List<Boolean> resultados) {
        if (resultados.isEmpty()) {
            return TipoDictamen.CONTINGENCY;
        }
        boolean algunaV = false;
        boolean algunaF = false;
        for (boolean r : resultados) {
            if (r) {
                algunaV = true;
            } else {
                algunaF = true;
            }
        }
        if (algunaV && !algunaF) {
            return TipoDictamen.TAUTOLOGY;
        }
        if (algunaF && !algunaV) {
            return TipoDictamen.CONTRADICTION;
        }
        return TipoDictamen.CONTINGENCY;
    }

    private static char marca(boolean v) {
        return v ? 'V' : 'F';
    }
}
