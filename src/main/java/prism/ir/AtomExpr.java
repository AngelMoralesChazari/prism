package prism.ir;

/**
 * Proposición atómica a partir de un fragmento de texto normalizado (clave estable para el registro de símbolos).
 */
public record AtomExpr(String fragmentoNormalizado, boolean negada) implements LogicExpr {
    public AtomExpr {
        if (fragmentoNormalizado == null || fragmentoNormalizado.isBlank()) {
            throw new IllegalArgumentException("El fragmento de proposición no puede ser vacío.");
        }
    }
}
