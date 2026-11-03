package prism.pr;

import prism.PrismResult;
import prism.PrismService;
import prism.ir.LogicExpr;
import prism.pr.ast.AtomDecl;
import prism.pr.ast.FormulaDecl;
import prism.pr.ast.FormulaExpr;
import prism.pr.ast.FormulaRef;
import prism.pr.ast.LetDecl;
import prism.pr.ast.PrProgram;
import prism.pr.ast.PrStmt;
import prism.pr.ast.NlDecl;
import prism.pr.ast.TextRef;
import prism.pr.ast.TranslateCall;
import prism.pr.ast.VertautoOptions;
import prism.pr.ast.VertautoStmt;
import prism.vertauto.VertautoResult;
import prism.vertauto.VertautoService;
import prism.vertauto.EvaluadorProposicional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PrInterpreter {

    private final PrismService translateService;
    private final VertautoService vertautoService;

    public PrInterpreter() {
        this(new PrismService(), new VertautoService());
    }

    public PrInterpreter(PrismService translateService, VertautoService vertautoService) {
        this.translateService = translateService;
        this.vertautoService = vertautoService;
    }

    public void ejecutar(PrProgram program) {
        PrValidator.validarOError(program);
        Map<String, String> nlTextos = new HashMap<>();
        Map<String, String> atomEtiquetas = new LinkedHashMap<>();
        Map<String, FormulaBinding> formulas = new HashMap<>();

        // Pase 1: Recolectar todos los textos NL y átomos declarados en el módulo (Two-Pass)
        for (PrStmt stmt : program.statements()) {
            if (stmt instanceof AtomDecl) {
                AtomDecl a = (AtomDecl) stmt;
                atomEtiquetas.put(a.name(), a.label());
            } else if (stmt instanceof NlDecl) {
                NlDecl n = (NlDecl) stmt;
                nlTextos.put(n.name(), n.text());
            }
        }

        PrAtomRegistry atomRegistry = new PrAtomRegistry(atomEtiquetas);

        // Pase 2: Evaluar traducciones, fórmulas y sentencias vertauto
        for (PrStmt stmt : program.statements()) {
            if (stmt instanceof LetDecl) {
                LetDecl l = (LetDecl) stmt;
                formulas.put(l.name(), evaluarTranslate(l.translate(), nlTextos, atomRegistry));
            } else if (stmt instanceof FormulaDecl) {
                FormulaDecl f = (FormulaDecl) stmt;
                formulas.put(f.name(), evaluarFormula(f.expression(), atomRegistry));
            } else if (stmt instanceof VertautoStmt) {
                VertautoStmt v = (VertautoStmt) stmt;
                ejecutarVertauto(v, formulas, atomRegistry);
            } else if (!(stmt instanceof AtomDecl) && !(stmt instanceof NlDecl)) {
                throw new IllegalStateException("Sentencia no contemplada: " + stmt.getClass());
            }
        }
    }

    public void ejecutarArchivo(String ruta) {
        String source = PrSourceLoader.leer(ruta);
        PrProgram program = PrParser.parse(source);
        List<String> errores = new ArrayList<>();
        PrValidator.validarArchivoConvencion(ruta, program.moduleName(), errores);
        if (!errores.isEmpty()) {
            throw new PrValidationException(errores);
        }
        ejecutar(program);
    }

    private FormulaBinding evaluarTranslate(
            TranslateCall call,
            Map<String, String> nlTextos,
            PrAtomRegistry atomRegistry
    ) {
        if (call.moduleOverride() != null) {
            throw new PrRuntimeException("translate con módulo explícito aún no implementado: "
                    + call.moduleOverride());
        }
        String texto = resolverTexto(call.text(), nlTextos);
        PrismResult traduccion = translateService.traducir(
                texto,
                atomRegistry.toRegistroProposiciones(translateService.getBaseConocimiento())
        );
        if (!traduccion.isExito()) {
            throw new PrRuntimeException("translate falló: " + traduccion.getMensaje());
        }
        return FormulaBinding.desdeTraduccion(traduccion, texto, atomRegistry);
    }

    private FormulaBinding evaluarFormula(FormulaExpr expr, PrAtomRegistry atomRegistry) {
        LogicExpr logicExpr = PrFormulaBridge.toLogicExpr(expr);
        String mostrada = PrFormulaBridge.emitirFormulaClasica(expr, atomRegistry);
        return FormulaBinding.desdeExpresion(logicExpr, mostrada, atomRegistry);
    }

    private void ejecutarVertauto(
            VertautoStmt stmt,
            Map<String, FormulaBinding> formulas,
            PrAtomRegistry atomRegistry
    ) {
        FormulaBinding binding = resolverFormula(stmt.target(), formulas, atomRegistry);
        VertautoResult resultado = binding.evaluar(vertautoService);
        VertautoOptions opts = stmt.options();
        if (!opts.steps() && !opts.table() && !opts.verdict()) {
            resultado.imprimirModoCorto();
        } else {
            resultado.imprimirExtendido(opts.steps(), opts.table(), opts.verdict());
        }
    }

    private FormulaBinding resolverFormula(
            FormulaRef ref,
            Map<String, FormulaBinding> formulas,
            PrAtomRegistry atomRegistry
    ) {
        if (ref.isVariable()) {
            FormulaBinding binding = formulas.get(ref.variableName());
            if (binding == null) {
                throw new PrRuntimeException("Fórmula no definida: " + ref.variableName());
            }
            return binding;
        }
        return evaluarFormula(ref.inlineExpression(), atomRegistry);
    }

    private String resolverTexto(TextRef ref, Map<String, String> nlTextos) {
        if (ref.isLiteral()) {
            return ref.literal();
        }
        String texto = nlTextos.get(ref.variableName());
        if (texto == null) {
            throw new PrRuntimeException("Texto nl no definido: " + ref.variableName());
        }
        return texto;
    }

    static final class FormulaBinding {
        private final String formulaClasica;
        private final LogicExpr expr;
        private final String formulaMostrada;
        private final PrismResult traduccion;
        private final String textoNatural;
        private final PrAtomRegistry atomRegistry;

        private FormulaBinding(
                String formulaClasica,
                LogicExpr expr,
                String formulaMostrada,
                PrismResult traduccion,
                String textoNatural,
                PrAtomRegistry atomRegistry
        ) {
            this.formulaClasica = formulaClasica;
            this.expr = expr;
            this.formulaMostrada = formulaMostrada;
            this.traduccion = traduccion;
            this.textoNatural = textoNatural;
            this.atomRegistry = atomRegistry == null ? PrAtomRegistry.vacio() : atomRegistry;
        }

        static FormulaBinding desdeTraduccion(
                PrismResult traduccion,
                String textoNatural,
                PrAtomRegistry atomRegistry
        ) {
            return new FormulaBinding(
                    traduccion.getFormula(),
                    null,
                    traduccion.getFormula(),
                    traduccion,
                    textoNatural,
                    atomRegistry
            );
        }

        static FormulaBinding desdeExpresion(
                LogicExpr expr,
                String mostrada,
                PrAtomRegistry atomRegistry
        ) {
            return new FormulaBinding(null, expr, mostrada, null, null, atomRegistry);
        }

        VertautoResult evaluar(VertautoService vertautoService) {
            if (traduccion != null) {
                VertautoResult evaluacion = vertautoService.vertautoClasica(traduccion.getFormula());
                Map<String, String> proposiciones = atomRegistry.proposicionesDesdeTraduccion(
                        traduccion.getProposiciones()
                );
                return new VertautoResult(
                        evaluacion.getFormula(),
                        evaluacion.getDictamen(),
                        evaluacion.getAtomos(),
                        evaluacion.getFilasTabla(),
                        evaluacion.getPasosEvaluacion(),
                        textoNatural,
                        traduccion.getPasosDeAnalisis(),
                        proposiciones
                );
            }
            if (expr != null) {
                VertautoResult evaluacion = vertautoService.vertauto(expr, formulaMostrada);
                List<String> simbolos = EvaluadorProposicional.recolectarAtomos(expr);
                Map<String, String> proposiciones = atomRegistry.proposicionesParaSimbolos(simbolos);
                if (proposiciones.isEmpty()) {
                    return evaluacion;
                }
                return new VertautoResult(
                        evaluacion.getFormula(),
                        evaluacion.getDictamen(),
                        evaluacion.getAtomos(),
                        evaluacion.getFilasTabla(),
                        evaluacion.getPasosEvaluacion(),
                        null,
                        List.of(),
                        proposiciones
                );
            }
            return vertautoService.vertautoClasica(formulaClasica);
        }
    }
}
