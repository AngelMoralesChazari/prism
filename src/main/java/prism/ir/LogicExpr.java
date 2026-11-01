package prism.ir;

/**
 * Representación intermedia (IR) de Prism: expresión proposicional antes de emitir
 * la cadena para {@code MotorLogico}. Evolucionará con más conectivas y nodos.
 */
public sealed interface LogicExpr permits AtomExpr, NegExpr, AndExpr, OrExpr, ImpExpr, EquivExpr {
}
