package prism.pr;

import nlp.normalizacion.NormalizadorTexto;
import prism.PrismResult;
import prism.PrismService;
import prism.ir.LogicExpr;
import prism.pr.ast.AtomDecl;
import prism.pr.ast.LetDecl;
import prism.pr.ast.NlDecl;
import prism.pr.ast.PrProgram;
import prism.pr.ast.PrStmt;
import prism.pr.ast.TextRef;
import prism.vertauto.EvaluadorProposicional;
import prism.vertauto.ParserNotacionClasica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Linter de coherencia semántica para declaraciones {@code atom} y llamadas
 * {@code translate}.
 * <p>
 * Detecta átomos declarados que no coinciden con las proposiciones del lenguaje
 * natural ("átomos huérfanos")
 * y genera sugerencias automáticas de tipo "Did you mean...?" mediante
 * solapamiento léxico de tokens
 * y núcleos conceptuales, resolviendo el problema de la sinonimia abierta de
 * forma pedagógica y determinista.
 */
public final class PrSemanticLinter {

    private static final Set<String> STOPWORDS = Set.of(
            "el", "la", "los", "las", "un", "una", "unos", "unas",
            "de", "del", "a", "al", "en", "se", "es", "son", "fue", "era", "ser", "estar",
            "que", "y", "o", "no", "por", "para", "con", "su", "sus", "si", "entonces", "pero",
            "este", "esta", "estos", "estas", "aquel", "aquella", "aquellos", "aquellas",
            "lo", "le", "les", "me", "te", "nos", "él", "ella", "ellos", "ellas");

    private PrSemanticLinter() {
    }

    /**
     * Extrae tokens de contenido significativo (sustantivos, verbos, adjetivos)
     * descartando palabras funcionales y normalizando diacríticos.
     */
    public static Set<String> extraerTokens(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }
        String normalizado = new NormalizadorTexto().normalizar(texto);
        String[] partes = normalizado.split("[^a-zA-Z0-9]+");
        Set<String> tokens = new LinkedHashSet<>();
        for (String p : partes) {
            if (p.length() >= 3 && !STOPWORDS.contains(p)) {
                tokens.add(p);
            }
        }
        return tokens;
    }

    /**
     * Diagnostica una traducción individual frente al registro de átomos
     * declarados.
     *
     * @param nombreContexto nombre de la variable o texto analizado (p. ej.
     *                       {@code "premisa"})
     * @param traduccion     resultado de {@code translate}
     * @param atomRegistry   registro de átomos declarados en el módulo
     * @return lista de mensajes de advertencia con sugerencias
     */
    public static List<String> diagnosticar(
            String nombreContexto,
            PrismResult traduccion,
            PrAtomRegistry atomRegistry) {
        if (traduccion == null || !traduccion.isExito() || atomRegistry == null) {
            return List.of();
        }

        Map<String, String> declarados = atomRegistry.mapaSimboloAEtiqueta();
        if (declarados.isEmpty()) {
            return List.of();
        }

        LogicExpr expr;
        try {
            expr = ParserNotacionClasica.parsear(traduccion.getFormula());
        } catch (Exception e) {
            return List.of();
        }

        List<String> simbolosEnFormula = EvaluadorProposicional.recolectarAtomos(expr);
        Set<String> setEnFormula = new HashSet<>(simbolosEnFormula);

        Set<String> atomosHuerfanos = new LinkedHashSet<>();
        for (String sym : declarados.keySet()) {
            if (!setEnFormula.contains(sym)) {
                atomosHuerfanos.add(sym);
            }
        }

        if (atomosHuerfanos.isEmpty()) {
            return List.of();
        }

        Set<String> simbolosAuto = new LinkedHashSet<>();
        for (String sym : simbolosEnFormula) {
            if (!declarados.containsKey(sym)) {
                simbolosAuto.add(sym);
            }
        }

        Map<String, String> clausulasAuto = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : traduccion.getProposiciones().entrySet()) {
            if (simbolosAuto.contains(e.getValue())) {
                clausulasAuto.put(e.getValue(), e.getKey());
            }
        }

        List<String> avisos = new ArrayList<>();
        Set<String> autosUsados = new HashSet<>();

        for (String huerfano : atomosHuerfanos) {
            String etiquetaHuerfano = declarados.get(huerfano);
            Set<String> tokensHuerfano = extraerTokens(etiquetaHuerfano);

            String mejorSimbolo = null;
            String mejorClausula = null;
            int maxCoincidencias = 0;

            for (Map.Entry<String, String> autoEntry : clausulasAuto.entrySet()) {
                if (autosUsados.contains(autoEntry.getKey())) {
                    continue;
                }
                Set<String> tokensAuto = extraerTokens(autoEntry.getValue());
                Set<String> interseccion = new LinkedHashSet<>(tokensHuerfano);
                interseccion.retainAll(tokensAuto);
                if (interseccion.size() > maxCoincidencias) {
                    maxCoincidencias = interseccion.size();
                    mejorSimbolo = autoEntry.getKey();
                    mejorClausula = autoEntry.getValue();
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[Aviso Semántico] El átomo declarado '").append(huerfano)
                    .append("' (\"").append(etiquetaHuerfano).append("\") no coincide con ninguna cláusula de '")
                    .append(nombreContexto).append("'.\n");

            if (mejorClausula != null && maxCoincidencias > 0) {
                autosUsados.add(mejorSimbolo);
                sb.append("Sugerencia: Se detectó una cláusula no enlazada en el texto: \"")
                        .append(mejorClausula).append("\" (asignada automáticamente al símbolo '")
                        .append(mejorSimbolo).append("').\n");
                sb.append("      --> ¿Quisiste decir: atom ").append(huerfano).append(" = \"")
                        .append(mejorClausula).append("\"?");
            } else if (!clausulasAuto.isEmpty()) {
                sb.append("Proposiciones detectadas en el texto que recibieron símbolos automáticos:\n");
                for (Map.Entry<String, String> e : clausulasAuto.entrySet()) {
                    sb.append("      - \"").append(e.getValue()).append("\" (símbolo '").append(e.getKey())
                            .append("')\n");
                }
            } else {
                sb.append("No se detectó ninguna proposición no enlazada en el texto analizado.");
            }
            avisos.add(sb.toString());
        }

        return Collections.unmodifiableList(avisos);
    }

    /**
     * Realiza un pase de linting semántico sobre todo un programa AST.
     */
    public static List<String> diagnosticarPrograma(PrProgram program, PrismService translateService) {
        if (program == null || translateService == null) {
            return List.of();
        }

        Map<String, String> nlTextos = new LinkedHashMap<>();
        Map<String, String> atomEtiquetas = new LinkedHashMap<>();

        for (PrStmt stmt : program.statements()) {
            if (stmt instanceof AtomDecl a) {
                atomEtiquetas.put(a.name(), a.label());
            } else if (stmt instanceof NlDecl n) {
                nlTextos.put(n.name(), n.text());
            }
        }

        if (atomEtiquetas.isEmpty()) {
            return List.of();
        }

        PrAtomRegistry registry = new PrAtomRegistry(atomEtiquetas);
        List<String> avisos = new ArrayList<>();

        for (PrStmt stmt : program.statements()) {
            if (stmt instanceof LetDecl l) {
                TextRef ref = l.translate().text();
                String texto = ref.isLiteral() ? ref.literal() : nlTextos.get(ref.variableName());
                String contexto = ref.isLiteral() ? "\"" + ref.literal() + "\"" : ref.variableName();
                if (texto != null && !texto.isBlank()) {
                    try {
                        PrismResult res = translateService.traducir(
                                texto,
                                registry.toRegistroProposiciones(translateService.getBaseConocimiento()));
                        avisos.addAll(diagnosticar(contexto, res, registry));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return Collections.unmodifiableList(avisos);
    }

    /**
     * Imprime avisos a {@link System#err}.
     */
    public static void imprimirAvisos(List<String> avisos) {
        if (avisos == null || avisos.isEmpty()) {
            return;
        }
        for (String av : avisos) {
            System.err.println(av);
        }
    }
}
