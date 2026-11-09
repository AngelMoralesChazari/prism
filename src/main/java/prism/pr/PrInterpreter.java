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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PrInterpreter {

    private final PrismService translateService;
    private final VertautoService vertautoService;
    private boolean imprimirAvisos = true;

    public PrInterpreter() {
        this(new PrismService(), new VertautoService());
    }

    public PrInterpreter(PrismService translateService, VertautoService vertautoService) {
        this.translateService = translateService;
        this.vertautoService = vertautoService;
    }

    public boolean isImprimirAvisos() {
        return imprimirAvisos;
    }

    public void setImprimirAvisos(boolean imprimirAvisos) {
        this.imprimirAvisos = imprimirAvisos;
    }

    public static void ejecutarCompilado(String moduleName, String sourceCode) {
        PrProgram program = PrParser.parse(sourceCode);
        new PrInterpreter().ejecutar(program);
    }

    public void ejecutar(PrProgram program) {
        PrValidator.validarOError(program);
        Map<String, String> nlTextos = new HashMap<>();
        Map<String, String> atomEtiquetas = new LinkedHashMap<>();
        Map<String, FormulaExpr> formulaExprs = new LinkedHashMap<>();
        Map<String, PrismResult> letResultados = new LinkedHashMap<>();
        Map<String, FormulaBinding> formulas = new HashMap<>();

        // Pase 1: Recolectar todos los textos NL, átomos y definiciones de fórmulas del
        // módulo (Two-Pass)
        for (PrStmt stmt : program.statements()) {
            if (stmt instanceof AtomDecl) {
                AtomDecl a = (AtomDecl) stmt;
                atomEtiquetas.put(a.name(), a.label());
            } else if (stmt instanceof NlDecl) {
                NlDecl n = (NlDecl) stmt;
                nlTextos.put(n.name(), n.text());
            } else if (stmt instanceof FormulaDecl) {
                FormulaDecl f = (FormulaDecl) stmt;
                formulaExprs.put(f.name(), f.expression());
            }
        }

        PrAtomRegistry atomRegistry = new PrAtomRegistry(atomEtiquetas);

        // Pase 2a: Evaluar todas las traducciones let (base para composición y linter)
        List<PrSemanticLinter.ResultadoLet> resultadosLint = new ArrayList<>();
        for (PrStmt stmt : program.statements()) {
            if (stmt instanceof LetDecl) {
                LetDecl l = (LetDecl) stmt;
                FormulaBinding binding = evaluarTranslate(l.translate(), nlTextos, atomRegistry);
                formulas.put(l.name(), binding);
                letResultados.put(l.name(), binding.traduccion);
                String contexto = l.translate().text().isLiteral()
                        ? "\"" + l.translate().text().literal() + "\""
                        : l.translate().text().variableName();
                resultadosLint.add(new PrSemanticLinter.ResultadoLet(contexto, binding.traduccion));
            }
        }

        // Pase 2b: Diagnóstico de coherencia semántica a nivel de módulo completo
        if (imprimirAvisos) {
            List<String> avisos = PrSemanticLinter.diagnosticarResultados(program, resultadosLint, atomRegistry);
            PrSemanticLinter.imprimirAvisos(avisos);
        }

        // Pase 2c: Evaluar definiciones formula (con composición modular) y sentencias
        // vertauto
        for (PrStmt stmt : program.statements()) {
            if (stmt instanceof FormulaDecl) {
                FormulaDecl f = (FormulaDecl) stmt;
                formulas.put(f.name(), evaluarFormula(f.expression(), atomRegistry, letResultados, formulaExprs));
            } else if (stmt instanceof VertautoStmt) {
                VertautoStmt v = (VertautoStmt) stmt;
                ejecutarVertauto(v, formulas, atomRegistry, letResultados, formulaExprs);
            } else if (!(stmt instanceof AtomDecl) && !(stmt instanceof NlDecl) && !(stmt instanceof LetDecl)) {
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
            PrAtomRegistry atomRegistry) {
        if (call.moduleOverride() != null) {
            throw new PrRuntimeException("translate con módulo explícito aún no implementado: "
                    + call.moduleOverride());
        }
        String texto = resolverTexto(call.text(), nlTextos);
        PrismResult traduccion = translateService.traducir(
                texto,
                atomRegistry.toRegistroProposiciones(translateService.getBaseConocimiento()));
        if (!traduccion.isExito()) {
            throw new PrRuntimeException("translate falló: " + traduccion.getMensaje());
        }
        return FormulaBinding.desdeTraduccion(traduccion, texto, atomRegistry, List.of());
    }

    private FormulaBinding evaluarFormula(
            FormulaExpr expr,
            PrAtomRegistry atomRegistry,
            Map<String, PrismResult> letResultados,
            Map<String, FormulaExpr> formulaExprs) {
        PrFormulaBridge.ResolvedFormula resuelta = PrFormulaBridge.resolverCompuesto(
                expr,
                atomRegistry,
                translateService.getBaseConocimiento(),
                letResultados,
                formulaExprs);
        return FormulaBinding.desdeExpresion(
                resuelta.logicExpr(),
                resuelta.formulaClasica(),
                atomRegistry,
                resuelta.etiquetasPorSimbolo());
    }

    private void ejecutarVertauto(
            VertautoStmt stmt,
            Map<String, FormulaBinding> formulas,
            PrAtomRegistry atomRegistry,
            Map<String, PrismResult> letResultados,
            Map<String, FormulaExpr> formulaExprs) {
        FormulaBinding binding = resolverFormula(stmt.target(), formulas, atomRegistry, letResultados, formulaExprs);
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
            PrAtomRegistry atomRegistry,
            Map<String, PrismResult> letResultados,
            Map<String, FormulaExpr> formulaExprs) {
        if (ref.isVariable()) {
            FormulaBinding binding = formulas.get(ref.variableName());
            if (binding == null) {
                throw new PrRuntimeException("Fórmula no definida: " + ref.variableName());
            }
            return binding;
        }
        return evaluarFormula(ref.inlineExpression(), atomRegistry, letResultados, formulaExprs);
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
        private final List<String> avisosSemanticos;
        private final Map<String, String> etiquetasExtra;

        private FormulaBinding(
                String formulaClasica,
                LogicExpr expr,
                String formulaMostrada,
                PrismResult traduccion,
                String textoNatural,
                PrAtomRegistry atomRegistry,
                List<String> avisosSemanticos,
                Map<String, String> etiquetasExtra) {
            this.formulaClasica = formulaClasica;
            this.expr = expr;
            this.formulaMostrada = formulaMostrada;
            this.traduccion = traduccion;
            this.textoNatural = textoNatural;
            this.atomRegistry = atomRegistry == null ? PrAtomRegistry.vacio() : atomRegistry;
            this.avisosSemanticos = avisosSemanticos == null ? List.of() : List.copyOf(avisosSemanticos);
            this.etiquetasExtra = etiquetasExtra == null ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(etiquetasExtra));
        }

        static FormulaBinding desdeTraduccion(
                PrismResult traduccion,
                String textoNatural,
                PrAtomRegistry atomRegistry) {
            return desdeTraduccion(traduccion, textoNatural, atomRegistry, List.of());
        }

        static FormulaBinding desdeTraduccion(
                PrismResult traduccion,
                String textoNatural,
                PrAtomRegistry atomRegistry,
                List<String> avisosSemanticos) {
            return new FormulaBinding(
                    traduccion.getFormula(),
                    null,
                    traduccion.getFormula(),
                    traduccion,
                    textoNatural,
                    atomRegistry,
                    avisosSemanticos,
                    Map.of());
        }

        static FormulaBinding desdeExpresion(
                LogicExpr expr,
                String mostrada,
                PrAtomRegistry atomRegistry) {
            return desdeExpresion(expr, mostrada, atomRegistry, Map.of());
        }

        static FormulaBinding desdeExpresion(
                LogicExpr expr,
                String mostrada,
                PrAtomRegistry atomRegistry,
                Map<String, String> etiquetasExtra) {
            return new FormulaBinding(null, expr, mostrada, null, null, atomRegistry, List.of(), etiquetasExtra);
        }

        public List<String> avisosSemanticos() {
            return avisosSemanticos;
        }

        VertautoResult evaluar(VertautoService vertautoService) {
            if (traduccion != null) {
                VertautoResult evaluacion = vertautoService.vertautoClasica(traduccion.getFormula());
                Map<String, String> proposiciones = atomRegistry.proposicionesDesdeTraduccion(
                        traduccion.getProposiciones());
                List<String> pasos = new ArrayList<>(traduccion.getPasosDeAnalisis());
                if (!avisosSemanticos.isEmpty()) {
                    pasos.addAll(avisosSemanticos);
                }
                return new VertautoResult(
                        evaluacion.getFormula(),
                        evaluacion.getDictamen(),
                        evaluacion.getAtomos(),
                        evaluacion.getFilasTabla(),
                        evaluacion.getPasosEvaluacion(),
                        textoNatural,
                        Collections.unmodifiableList(pasos),
                        proposiciones);
            }
            if (expr != null) {
                VertautoResult evaluacion = vertautoService.vertauto(expr, formulaMostrada);
                List<String> simbolos = EvaluadorProposicional.recolectarAtomos(expr);
                Map<String, String> proposiciones = construirProposicionesCompuestas(simbolos);
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
                        proposiciones);
            }
            return vertautoService.vertautoClasica(formulaClasica);
        }

        private Map<String, String> construirProposicionesCompuestas(List<String> simbolos) {
            if (simbolos.isEmpty()) {
                return Map.of();
            }
            if (etiquetasExtra.isEmpty()) {
                return atomRegistry.proposicionesParaSimbolos(simbolos);
            }
            Map<String, String> resultado = new LinkedHashMap<>();
            for (String sim : simbolos) {
                String etiqueta = atomRegistry.etiquetaDe(sim);
                if (etiqueta == null) {
                    etiqueta = etiquetasExtra.getOrDefault(sim, sim);
                }
                if (resultado.containsKey(etiqueta)) {
                    resultado.put(etiqueta + " (" + sim + ")", sim);
                } else {
                    resultado.put(etiqueta, sim);
                }
            }
            return Collections.unmodifiableMap(resultado);
        }
    }
}
