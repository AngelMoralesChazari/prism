package prism.pr;

import prism.pr.ast.AtomDecl;
import prism.pr.ast.AtomRef;
import prism.pr.ast.BinaryExpr;
import prism.pr.ast.FormulaDecl;
import prism.pr.ast.FormulaExpr;
import prism.pr.ast.FormulaRef;
import prism.pr.ast.LetDecl;
import prism.pr.ast.PrProgram;
import prism.pr.ast.PrStmt;
import prism.pr.ast.NlDecl;
import prism.pr.ast.NotExpr;
import prism.pr.ast.TextRef;
import prism.pr.ast.TranslateCall;
import prism.pr.ast.VertautoStmt;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Análisis estático de programas {@code .ls}: módulos, redeclaraciones y referencias.
 */
public final class PrValidator {

    private static final Set<String> MODULOS_CONOCIDOS = Set.of("core");

    private PrValidator() {
    }

    public static List<String> validar(PrProgram program) {
        List<String> errores = new ArrayList<>();
        validarModulos(program, errores);

        Set<String> identificadores = new HashSet<>();
        Set<String> atomos = new HashSet<>();
        Set<String> textosNl = new HashSet<>();
        Set<String> formulas = new HashSet<>();

        for (PrStmt stmt : program.statements()) {
            if (stmt instanceof AtomDecl) {
                AtomDecl a = (AtomDecl) stmt;
                registrarIdentificador(a.name(), "átomo", identificadores, errores);
                atomos.add(a.name());
            } else if (stmt instanceof NlDecl) {
                NlDecl n = (NlDecl) stmt;
                registrarIdentificador(n.name(), "nl", identificadores, errores);
                textosNl.add(n.name());
            } else if (stmt instanceof LetDecl) {
                LetDecl l = (LetDecl) stmt;
                registrarIdentificador(l.name(), "let", identificadores, errores);
                validarTranslate(l.translate(), textosNl, errores);
                formulas.add(l.name());
            } else if (stmt instanceof FormulaDecl) {
                FormulaDecl f = (FormulaDecl) stmt;
                registrarIdentificador(f.name(), "formula", identificadores, errores);
                validarAtomosEnFormula(f.name(), f.expression(), atomos, errores);
                formulas.add(f.name());
            } else if (stmt instanceof VertautoStmt) {
                VertautoStmt v = (VertautoStmt) stmt;
                validarVertauto(v.target(), formulas, atomos, errores);
            } else {
                errores.add("Sentencia no contemplada: " + stmt.getClass().getSimpleName());
            }
        }
        return List.copyOf(errores);
    }

    public static void validarOError(PrProgram program) {
        List<String> errores = validar(program);
        if (!errores.isEmpty()) {
            throw new PrValidationException(errores);
        }
    }

    public static List<String> validarArchivo(String ruta) {
        String source = PrSourceLoader.leer(ruta);
        PrProgram program = PrParser.parse(source);
        List<String> errores = new ArrayList<>(validar(program));
        validarArchivoConvencion(ruta, program.moduleName(), errores);
        return errores;
    }

    private static void validarModulos(PrProgram program, List<String> errores) {
        for (String use : program.uses()) {
            if (!MODULOS_CONOCIDOS.contains(use)) {
                errores.add("Módulo desconocido en use: " + use + " (v1 solo soporta: core)");
            }
        }
    }

    public static void validarArchivoConvencion(String ruta, String moduleName, List<String> errores) {
        String nombreArchivo = java.nio.file.Path.of(ruta).getFileName().toString();
        if (!nombreArchivo.endsWith(".pr")) {
            return;
        }
        String esperado = nombreArchivo.substring(0, nombreArchivo.length() - 3);
        if (!esperado.equals(moduleName)) {
            errores.add("El module '" + moduleName + "' no coincide con el archivo '"
                    + nombreArchivo + "' (convención: module " + esperado + ")");
        }
    }

    private static void registrarIdentificador(
            String nombre,
            String tipo,
            Set<String> identificadores,
            List<String> errores
    ) {
        if (!identificadores.add(nombre)) {
            errores.add("Redeclaración de identificador '" + nombre + "' (" + tipo + ")");
        }
    }

    private static void validarTranslate(TranslateCall call, Set<String> textosNl, List<String> errores) {
        if (call.moduleOverride() != null) {
            errores.add("translate con módulo explícito aún no implementado: " + call.moduleOverride());
        }
        TextRef text = call.text();
        if (!text.isLiteral() && !textosNl.contains(text.variableName())) {
            errores.add("Texto nl no definido en translate: " + text.variableName());
        }
    }

    private static void validarVertauto(
            FormulaRef target,
            Set<String> formulas,
            Set<String> atomos,
            List<String> errores
    ) {
        if (target.isVariable()) {
            if (!formulas.contains(target.variableName())) {
                errores.add("Fórmula no definida en vertauto: " + target.variableName());
            }
            return;
        }
        validarAtomosEnFormula("vertauto inline", target.inlineExpression(), atomos, errores);
    }

    private static void validarAtomosEnFormula(
            String contexto,
            FormulaExpr expr,
            Set<String> atomos,
            List<String> errores
    ) {
        for (String atomo : atomosEn(expr)) {
            if (!atomos.contains(atomo)) {
                errores.add("Átomo no declarado en '" + contexto + "': " + atomo);
            }
        }
    }

    private static Set<String> atomosEn(FormulaExpr expr) {
        Set<String> nombres = new HashSet<>();
        recolectarAtomos(expr, nombres);
        return nombres;
    }

    private static void recolectarAtomos(FormulaExpr expr, Set<String> nombres) {
        if (expr instanceof AtomRef) {
            nombres.add(((AtomRef) expr).name());
        } else if (expr instanceof NotExpr) {
            recolectarAtomos(((NotExpr) expr).inner(), nombres);
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) expr;
            recolectarAtomos(b.left(), nombres);
            recolectarAtomos(b.right(), nombres);
        }
    }
}
