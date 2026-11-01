package prism.vertauto;

import prism.PrismResult;
import prism.PrismService;
import prism.ir.LogicExpr;

/**
 * API pública de {@code vertauto}: evalúa fórmulas en notación clásica o sintaxis {@code .ls}.
 */
public final class VertautoService {

    private final PrismService translateService;

    public VertautoService() {
        this(new PrismService());
    }

    public VertautoService(PrismService translateService) {
        this.translateService = translateService;
    }

    /** Evalúa cadena en notación clásica ({@code (p → q)}, {@code (p ∨ ¬p)}, …). */
    public VertautoResult vertautoClasica(String formulaClasica) {
        LogicExpr expr = ParserNotacionClasica.parsear(formulaClasica);
        return EvaluadorProposicional.evaluar(expr, formulaClasica.trim());
    }

    /** Evalúa expresión en sintaxis Prism ({@code p or not p}, {@code p impl q}, …). */
    public VertautoResult vertautoExpr(String expresionPrism) {
        LogicExpr expr = ParserFormulaPrism.parsear(expresionPrism);
        String mostrada = expresionPrism.trim();
        return EvaluadorProposicional.evaluar(expr, mostrada);
    }

    /** Traduce NL y evalúa la fórmula emitida (incluye pasos de traducción en el resultado). */
    public VertautoResult vertautoDesdeNl(String textoNatural) {
        PrismResult traduccion = translateService.traducir(textoNatural);
        if (!traduccion.isExito()) {
            throw new IllegalArgumentException(traduccion.getMensaje());
        }
        VertautoResult evaluacion = vertautoClasica(traduccion.getFormula());
        return new VertautoResult(
                evaluacion.getFormula(),
                evaluacion.getDictamen(),
                evaluacion.getAtomos(),
                evaluacion.getFilasTabla(),
                evaluacion.getPasosEvaluacion(),
                textoNatural.trim(),
                traduccion.getPasosDeAnalisis(),
                traduccion.getProposiciones()
        );
    }

    public VertautoResult vertauto(LogicExpr expr, String formulaMostrada) {
        return EvaluadorProposicional.evaluar(expr, formulaMostrada);
    }
}
