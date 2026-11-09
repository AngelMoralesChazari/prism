package prism;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nlp.lexicon.ContenidoLgs;
import nlp.lexicon.EstadoCargaLgs;
import nlp.lexicon.LgsCargador;
import nlp.lexicon.NormalizadorMorfologico;
import nlp.lexicon.ResultadoCargaLgs;

/**
 * Pruebas de regresión del pipeline Prism (LN → fórmula).
 * Ejecutar tras compilar y copiar recursos:
 * {@code java -cp out prism.PrismRegressionHarness}
 * <p>
 * Documentación: {@code docs/Prism/PruebasRegresion.md}
 */
public final class PrismRegressionHarness {

    private static final String IMP = "\u2192";
    private static final String AND = "\u2227";
    private static final String OR = "\u2228";
    private static final String IFF = "\u2194";

    private record Caso(String id, String entrada, boolean esperaExito, String formulaEsperada) {
        Caso {
            Objects.requireNonNull(id);
            Objects.requireNonNull(entrada);
            formulaEsperada = formulaEsperada != null ? formulaEsperada : "";
        }
    }

    public static void main(String[] args) {
        int fallos = 0;
        fallos += verificarDiagnosticoCargaLgs();
        fallos += verificarLexruleCarga();
        fallos += verificarDosPasadasCompilador();
        fallos += verificarLinterSemantico();
        fallos += verificarComposicionModular();
        fallos += verificarRechazoCiclosYNlEnFormula();

        List<Caso> casos = new ArrayList<>();
        casos.add(new Caso("si_entonces", "si llueve entonces llevo paraguas", true, "(p " + IMP + " q)"));
        casos.add(new Caso("consecuente_si", "llevo paraguas si llueve", true, "(p " + IMP + " q)"));
        casos.add(new Caso("en_caso_de_que", "en caso de que llueva, llevo paraguas", true, "(p " + IMP + " q)"));
        casos.add(new Caso("conjuncion", "llueve y estudio", true, "(p " + AND + " q)"));
        casos.add(new Caso("disyuncion", "llueve o estudio", true, "(p " + OR + " q)"));
        casos.add(new Caso("lemmas_si", "si estudio entonces apruebo", true, "(p " + IMP + " q)"));
        casos.add(new Caso("si_eliptico", "si estudio apruebo", true, "(p " + IMP + " q)"));
        casos.add(new Caso(
                "si_eliptico_negado",
                "si no estudio no apruebo",
                true,
                "(\u00acp " + IMP + " \u00acq)"));
        casos.add(new Caso(
                "composicion_coma_si",
                "si estudio apruebo, si no estudio no apruebo",
                true,
                "((p " + IMP + " q) " + AND + " (\u00acp " + IMP + " \u00acq))"));
        casos.add(new Caso("siempre_que_eliptico", "siempre que estudio apruebo", true, "(p " + IMP + " q)"));
        casos.add(new Caso("siempre_que_entonces", "siempre que trabajo entonces descanso", true, "(p " + IMP + " q)"));
        casos.add(new Caso("equivalencia", "apruebo si y solo si estudio", true, "(p " + IFF + " q)"));
        casos.add(new Caso("lemmas_trabajo", "si trabajo descanso", true, "(p " + IMP + " q)"));
        casos.add(new Caso("lemmas_practico", "practico si y solo si estudio", true, "(p " + IFF + " q)"));
        casos.add(new Caso("conjuncion_clima", "solea y hace calor", true, "(p " + AND + " q)"));
        casos.add(new Caso(
                "si_conj_y_entonces",
                "si estudio y practico entonces apruebo",
                true,
                "((" + "p " + AND + " q) " + IMP + " r)"));
        casos.add(new Caso(
                "si_entonces_disy_cons",
                "si llueve entonces llevo paraguas o gorra",
                true,
                "(p " + IMP + " (q " + OR + " r))"));
        casos.add(new Caso(
                "si_conj_y_eliptico",
                "si estudio y practico apruebo",
                true,
                "((" + "p " + AND + " q) " + IMP + " r)"));
        casos.add(new Caso(
                "si_disy_o_entonces",
                "si llueve o solea entonces salgo",
                true,
                "((" + "p " + OR + " q) " + IMP + " r)"));
        casos.add(new Caso(
                "si_entonces_conj_cons",
                "si llueve entonces llevo paraguas y gorra",
                true,
                "(p " + IMP + " (q " + AND + " r))"));
        casos.add(new Caso("cuando_eliptico", "cuando llueve llevo paraguas", true, "(p " + IMP + " q)"));
        casos.add(new Caso("cuando_entonces", "cuando trabajo entonces descanso", true, "(p " + IMP + " q)"));
        casos.add(new Caso("solo_si", "apruebo solo si estudio", true, "(p " + IMP + " q)"));
        casos.add(new Caso(
                "a_menos_que",
                "salgo a menos que llueva",
                true,
                "(\u00acp " + IMP + " q)"));
        casos.add(new Caso(
                "dos_bloques",
                "si llueve entonces llevo paraguas, en caso de que estudio, apruebo",
                true,
                "((p " + IMP + " q) " + AND + " (r " + IMP + " s))"));
        casos.add(new Caso("morph_si_duermo", "si duermo descanso", true, "(p " + IMP + " q)"));
        casos.add(new Caso("morph_estudian", "si estudian aprueban", true, "(p " + IMP + " q)"));
        casos.add(new Caso("morph_practico", "practico si y solo si estudio", true, "(p " + IFF + " q)"));
        casos.add(new Caso("morph_llego", "si llego descanso", true, "(p " + IMP + " q)"));
        casos.add(new Caso("lemma_duerme", "si duerme descansa", true, "(p " + IMP + " q)"));
        casos.add(new Caso("lemma_duermes", "si duermes descansas", true, "(p " + IMP + " q)"));
        casos.add(new Caso("lemma_puede", "si puede descansar", true, "(p " + IMP + " q)"));
        casos.add(new Caso(
                "si_disy_o_eliptico",
                "si llueve o solea salgo",
                true,
                "((" + "p " + OR + " q) " + IMP + " r)"));
        casos.add(new Caso(
                "siempre_que_conj_eliptico",
                "siempre que estudio y practico apruebo",
                true,
                "((" + "p " + AND + " q) " + IMP + " r)"));
        casos.add(new Caso(
                "siempre_que_disy_eliptico",
                "siempre que llueve o solea salgo",
                true,
                "((" + "p " + OR + " q) " + IMP + " r)"));
        casos.add(new Caso(
                "cuando_conj_eliptico",
                "cuando estudio y practico apruebo",
                true,
                "((" + "p " + AND + " q) " + IMP + " r)"));
        casos.add(new Caso(
                "cuando_disy_eliptico",
                "cuando llueve o solea salgo",
                true,
                "((" + "p " + OR + " q) " + IMP + " r)"));
        casos.add(new Caso(
                "composicion_cuando_si",
                "cuando llueve llevo paraguas, si estudio apruebo",
                true,
                "((p " + IMP + " q) " + AND + " (r " + IMP + " s))"));
        casos.add(new Caso(
                "equivalencia_implicaciones_contrapositivas",
                "si el proceso no libera la memoria entonces el sistema colapsa si y solo si si el sistema no colapsa entonces el proceso libera la memoria",
                true,
                "((\u00acp " + IMP + " q) " + IFF + " (\u00acq " + IMP + " p))"));
        casos.add(new Caso("ni_ni", "ni llueve ni hace calor", true, "(\u00acp " + AND + " \u00acq)"));
        casos.add(new Caso("o_bien", "o bien estudio o bien descanso", true, "(p " + OR + " q)"));
        casos.add(new Caso("no_es_cierto", "no es cierto que llueve", true, "\u00acp"));
        casos.add(new Caso("dado_que", "dado que llueve entonces llevo paraguas", true, "(p " + IMP + " q)"));
        casos.add(new Caso(
                "si_conj_y_entonces_conj",
                "si estudio y practico entonces apruebo y descanso",
                true,
                "((p " + AND + " q) " + IMP + " (r " + AND + " s))"));
        casos.add(new Caso(
                "sustantivos_sin_mutilar",
                "si el socket falla entonces el servido reinicia",
                true,
                "(p " + IMP + " q)"));
        casos.add(new Caso(
                "adversativa_pero_simple",
                "estudio pero no apruebo",
                true,
                "(p " + AND + " \u00acq)"));
        casos.add(new Caso(
                "adversativa_con_antonimo_contradiccion",
                "el microservicio responde con exito si y solo si la base de datos esta activa, pero el microservicio responde con exito y la base de datos esta inactiva",
                true,
                "((p " + IFF + " q) " + AND + " (p " + AND + " \u00acq))"));
        casos.add(new Caso(
                "antonimo_privativo_inactivo",
                "el servidor esta inactivo",
                true,
                "\u00acp"));
        casos.add(new Caso(
                "doble_negacion_antonimo",
                "el servidor no esta inactivo",
                true,
                "p"));
        casos.add(new Caso(
                "negacion_oracional_condicional_tautologia",
                "no es cierto que si el compilador optimiza el codigo entonces la ejecucion sea lenta, si y solo si el compilador optimiza el codigo y la ejecucion no es lenta",
                true,
                "(\u00ac((p " + IMP + " q)) " + IFF + " (p " + AND + " \u00acq))"));
        casos.add(new Caso(
                "alternancia_modo_indicativo_subjuntivo_disyuncion",
                "Si la cola de mensajes esta vacia, entonces el procesador se suspende o la tarea se reintenta; pero no es verdad que el procesador se suspenda o la tarea se reintente.",
                true,
                "((p " + IMP + " (q " + OR + " r)) " + AND + " \u00ac((q " + OR + " r)))"));
        casos.add(new Caso(
                "de_morgan_disyuncion_tautologia_anaforica",
                "El token de autenticacion expira o el cortafuegos bloquea la peticion, si y solo si no es verdad que el token no expire y el cortafuegos no bloquee la peticion",
                true,
                "((p " + OR + " q) " + IFF + " \u00ac((\u00acp " + AND + " \u00acq)))"));
        casos.add(new Caso(
                "correferencia_pronombres_este_ella",
                "Si la cola de mensajes esta vacia, entonces el procesador se suspende o la tarea se reintenta; pero no es verdad que este se suspenda o ella se reintente.",
                true,
                "((p " + IMP + " (q " + OR + " r)) " + AND + " \u00ac((q " + OR + " r)))"));
        casos.add(new Caso(
                "correferencia_pronombres_ellos_ellas",
                "Si los procesadores se suspenden entonces las tareas se reintentan; pero no es verdad que ellos se suspendan o ellas se reintenten.",
                true,
                "((p " + IMP + " q) " + AND + " \u00ac((p " + OR + " q)))"));
        casos.add(new Caso(
                "modificadores_adverbiales_accesorios_mente",
                "Si el sistema responde rapidamente, entonces la tarea finaliza exitosamente.",
                true,
                "(p " + IMP + " q)"));
        casos.add(new Caso(
                "modificadores_adverbiales_intensificador_y_locucion",
                "Si el token expira muy rapidamente, entonces el cortafuegos bloquea la peticion de manera automatica.",
                true,
                "(p " + IMP + " q)"));
        casos.add(new Caso(
                "argumento_modus_ponens_nl",
                "Si llueve entonces la calle se moja; llueve; por lo tanto la calle se moja.",
                true,
                "(((p " + IMP + " q) " + AND + " p) " + IMP + " q)"));
        casos.add(new Caso(
                "argumento_modus_tollens_nl",
                "Si llueve entonces la calle se moja; la calle no se moja; por lo tanto no llueve.",
                true,
                "(((p " + IMP + " q) " + AND + " \u00acq) " + IMP + " \u00acp)"));
        casos.add(new Caso(
                "argumento_modus_ponens_coma_y",
                "Si estudio apruebo, y estudio, por consiguiente apruebo",
                true,
                "(((p " + IMP + " q) " + AND + " p) " + IMP + " q)"));
        casos.add(new Caso(
                "argumento_modus_tollens_punto_luego",
                "Si llueve entonces la calle se moja. No es verdad que la calle se moje, luego no llueve",
                true,
                "(((p " + IMP + " q) " + AND + " \u00acq) " + IMP + " \u00acp)"));
        casos.add(new Caso(
                "argumento_silogismo_hipotetico_nl",
                "Si estudio entonces aprendo; si aprendo entonces apruebo; por lo tanto si estudio entonces apruebo.",
                true,
                "(((p " + IMP + " q) " + AND + " (q " + IMP + " r)) " + IMP + " (p " + IMP + " r))"));
        casos.add(new Caso(
                "argumento_silogismo_disyuntivo_coma",
                "Estudio o trabajo, no estudio; por lo tanto trabajo",
                true,
                "(((p " + OR + " q) " + AND + " \u00acp) " + IMP + " q)"));
        casos.add(new Caso(
                "argumento_silogismo_disyuntivo_doble_o",
                "O estudio o trabajo; no estudio; en consecuencia trabajo",
                true,
                "(((p " + OR + " q) " + AND + " \u00acp) " + IMP + " q)"));
        casos.add(new Caso(
                "argumento_silogismo_disyuntivo_o_bien",
                "O bien estudio o bien descanso; no estudio; por lo tanto descanso",
                true,
                "(((p " + OR + " q) " + AND + " \u00acp) " + IMP + " q)"));
        casos.add(new Caso(
                "de_morgan_conclusivo_subjuntivo",
                "No es verdad que llueva o duerma; por lo tanto no llueve y no duerme.",
                true,
                "(\u00ac((p " + OR + " q)) " + IMP + " (\u00acp " + AND + " \u00acq))"));
        casos.add(new Caso(
                "contraposicion_asi_que",
                "Si el servidor falla entonces la base de datos se detiene; as\u00ed que si la base de datos no se detiene entonces el servidor no falla.",
                true,
                "((p " + IMP + " q) " + IMP + " (\u00acq " + IMP + " \u00acp))"));
        casos.add(new Caso(
                "falacia_afirmacion_consecuente_nl",
                "Si llueve entonces la calle se moja; la calle se moja; por lo tanto llueve.",
                true,
                "(((p " + IMP + " q) " + AND + " q) " + IMP + " p)"));
        casos.add(new Caso(
                "falacia_negacion_antecedente_nl",
                "Si llueve entonces la calle se moja; no llueve; por lo tanto la calle no se moja.",
                true,
                "(((p " + IMP + " q) " + AND + " \u00acp) " + IMP + " \u00acq)"));
        casos.add(new Caso("vacio", "", false, ""));
        casos.add(new Caso("solo_espacios", "   \t  ", false, ""));

        PrismService servicio = new PrismService();
        for (Caso c : casos) {
            PrismResult r = servicio.traducir(c.entrada());
            boolean okExito = r.isExito() == c.esperaExito();
            boolean okFormula = !c.esperaExito() || r.getFormula().equals(c.formulaEsperada());
            if (!okExito || !okFormula) {
                fallos++;
                System.err.println("FALLO [" + c.id() + "] entrada=" + repr(c.entrada()));
                System.err.println("  esperaba exito=" + c.esperaExito() + ", obtuvo=" + r.isExito());
                if (c.esperaExito()) {
                    System.err.println("  formula esperada: " + c.formulaEsperada());
                    System.err.println("  formula obtenida: " + r.getFormula());
                }
                if (!r.isExito()) {
                    System.err.println("  mensaje: " + r.getMensaje());
                }
            }
        }
        if (fallos == 0) {
            System.out.println("PrismRegressionHarness: OK (" + casos.size() + " casos LN + diagnóstico .pr).");
        } else {
            System.err.println("PrismRegressionHarness: " + fallos + " fallo(s).");
            System.exit(1);
        }
    }

    private static int verificarDiagnosticoCargaLgs() {
        int fallos = 0;
        String invalido = "lemma sin flecha\n";
        ResultadoCargaLgs r = LgsCargador.cargarConDiagnostico(
                new ByteArrayInputStream(invalido.getBytes(StandardCharsets.UTF_8)),
                "test-invalido.pr");
        if (!r.bloqueaTraduccion() || r.estado() != EstadoCargaLgs.ERROR_SINTAXIS) {
            fallos++;
            System.err.println("FALLO [lgs_sintaxis]: se esperaba ERROR_SINTAXIS bloqueante");
        } else if (!r.mensaje().contains("Línea 1")) {
            fallos++;
            System.err.println("FALLO [lgs_sintaxis]: mensaje sin número de línea: " + r.mensaje());
        }
        return fallos;
    }

    private static int verificarLexruleCarga() {
        int fallos = 0;
        String invalido = "lexrule sufijo o infinitivo xyz\n";
        ResultadoCargaLgs mal = LgsCargador.cargarConDiagnostico(
                new ByteArrayInputStream(invalido.getBytes(StandardCharsets.UTF_8)),
                "test-lexrule-invalido.pr");
        if (!mal.bloqueaTraduccion() || mal.estado() != EstadoCargaLgs.ERROR_SINTAXIS) {
            fallos++;
            System.err.println("FALLO [lexrule_sintaxis]: se esperaba ERROR_SINTAXIS bloqueante");
        }

        String personalizado = "version 0.6\nlexrule sufijo ado infinitivo ar\n";
        try {
            ContenidoLgs contenido = LgsCargador.cargar(
                    new ByteArrayInputStream(personalizado.getBytes(StandardCharsets.UTF_8)));
            NormalizadorMorfologico morfo = new NormalizadorMorfologico(contenido.morfologia());
            String canon = morfo.normalizar("organizado");
            if (!"organizar".equals(canon)) {
                fallos++;
                System.err.println("FALLO [lexrule_carga]: organizado -> " + canon + ", esperaba organizar");
            }
        } catch (IOException e) {
            fallos++;
            System.err.println("FALLO [lexrule_carga]: " + e.getMessage());
        }
        return fallos;
    }

    private static int verificarDosPasadasCompilador() {
        int fallos = 0;
        String codigo = "module test_dos_pasadas\n"
                + "use core\n"
                + "nl premisa = \"Si la tarea se reintenta entonces el procesador se suspende.\"\n"
                + "let f1 = translate(premisa)\n"
                + "formula f2 = q impl p\n"
                + "atom p = \"el procesador se suspende\"\n"
                + "atom q = \"la tarea se reintenta\"\n";
        try {
            prism.pr.ast.PrProgram program = prism.pr.PrParser.parse(codigo);
            List<String> errores = prism.pr.PrValidator.validar(program);
            if (!errores.isEmpty()) {
                fallos++;
                System.err.println("FALLO [dos_pasadas_validador]: " + errores);
            }
            prism.pr.PrInterpreter interprete = new prism.pr.PrInterpreter();
            interprete.ejecutar(program);
        } catch (Exception e) {
            fallos++;
            System.err.println("FALLO [dos_pasadas_interprete]: " + e.getMessage());
        }
        return fallos;
    }

    private static int verificarLinterSemantico() {
        int fallos = 0;
        PrismService service = new PrismService();

        // 1. Caso de divergencia sinonímica ("apaga" en atom vs "suspende" en NL)
        String texto = "Si el procesador se suspende entonces la tarea se reintenta; pero no es verdad que él se suspenda o ella se reintente.";
        prism.pr.PrAtomRegistry regDivergente = new prism.pr.PrAtomRegistry(Map.of(
                "p", "el procesador se apaga",
                "q", "la tarea se reintenta"
        ));
        PrismResult resDiv = service.traducir(texto, regDivergente.toRegistroProposiciones(service.getBaseConocimiento()));
        List<String> avisosDiv = prism.pr.PrSemanticLinter.diagnosticar("premisa", resDiv, regDivergente);
        if (avisosDiv.size() != 1) {
            fallos++;
            System.err.println("FALLO [linter_divergencia_tam]: se esperaba 1 aviso, obtuvo " + avisosDiv.size());
        } else {
            String aviso = avisosDiv.get(0);
            if (!aviso.contains("atom p = \"procesador se suspende\"") || !aviso.contains("símbolo 'v'")) {
                fallos++;
                System.err.println("FALLO [linter_divergencia_sugerencia]: sugerencia no contiene el reemplazo esperado: " + aviso);
            }
        }

        // 2. Caso canónico coincidente: 0 avisos
        prism.pr.PrAtomRegistry regCoincidente = new prism.pr.PrAtomRegistry(Map.of(
                "p", "el procesador se suspende",
                "q", "la tarea se reintenta"
        ));
        PrismResult resCoin = service.traducir(texto, regCoincidente.toRegistroProposiciones(service.getBaseConocimiento()));
        List<String> avisosCoin = prism.pr.PrSemanticLinter.diagnosticar("premisa", resCoin, regCoincidente);
        if (!avisosCoin.isEmpty()) {
            fallos++;
            System.err.println("FALLO [linter_coincidente]: se esperaban 0 avisos, obtuvo " + avisosCoin.size());
        }

        // 3. Caso programa completo AST vía diagnosticarPrograma
        String codigoPr = "module test_linter\n"
                + "use core\n"
                + "atom p = \"el procesador se apaga\"\n"
                + "atom q = \"la tarea se reintenta\"\n"
                + "nl premisa = \"Si el procesador se suspende entonces la tarea se reintenta.\"\n"
                + "let f = translate(premisa)\n";
        try {
            prism.pr.ast.PrProgram program = prism.pr.PrParser.parse(codigoPr);
            List<String> avisosProg = prism.pr.PrSemanticLinter.diagnosticarPrograma(program, service);
            if (avisosProg.isEmpty() || !avisosProg.get(0).contains("procesador se suspende")) {
                fallos++;
                System.err.println("FALLO [linter_programa_ast]: se esperaba sugerencia en programa AST, obtuvo: " + avisosProg);
            }
        } catch (Exception e) {
            fallos++;
            System.err.println("FALLO [linter_programa_ast_exc]: " + e.getMessage());
        }

        return fallos;
    }

    private static int verificarComposicionModular() {
        int fallos = 0;
        String codigo = "module test_modular\n"
                + "use core\n"
                + "formula mp = (regla and p) impl q\n"
                + "formula cadena = mp and not neg_q\n"
                + "atom p = \"llueve\"\n"
                + "atom q = \"la calle se moja\"\n"
                + "let regla = translate(\"Si llueve entonces la calle se moja.\")\n"
                + "let neg_q = translate(\"La calle no se moja.\")\n"
                + "let extra_a = translate(\"Si hace frio entonces uso abrigo.\")\n"
                + "let extra_b = translate(\"Uso abrigo.\")\n"
                + "let extra_c = translate(\"Hace frio.\")\n"
                + "vertauto mp { verdict }\n"
                + "vertauto ((extra_a and extra_b) impl extra_c) { verdict }\n";
        try {
            prism.pr.ast.PrProgram program = prism.pr.PrParser.parse(codigo);
            List<String> errores = prism.pr.PrValidator.validar(program);
            if (!errores.isEmpty()) {
                fallos++;
                System.err.println("FALLO [modular_validador]: " + errores);
            }
            List<String> avisos = prism.pr.PrSemanticLinter.diagnosticarPrograma(program, new PrismService());
            if (!avisos.isEmpty()) {
                fallos++;
                System.err.println("FALLO [modular_linter_sin_falsos_positivos]: se esperaban 0 avisos, obtuvo: " + avisos);
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            var originalOut = System.out;
            try {
                System.setOut(new java.io.PrintStream(buffer, true, StandardCharsets.UTF_8));
                new prism.pr.PrInterpreter().ejecutar(program);
            } finally {
                System.setOut(originalOut);
            }
            String salida = buffer.toString(StandardCharsets.UTF_8);
            if (!salida.contains("(((p " + IMP + " q) " + AND + " p) " + IMP + " q)  tautology")) {
                fallos++;
                System.err.println("FALLO [modular_mp_salida]:\n" + salida);
            }
            if (!salida.contains("contingency")) {
                fallos++;
                System.err.println("FALLO [modular_falacia_auto_salida]:\n" + salida);
            }
        } catch (Exception e) {
            fallos++;
            System.err.println("FALLO [modular_exc]: " + e.getMessage());
        }
        return fallos;
    }

    private static int verificarRechazoCiclosYNlEnFormula() {
        int fallos = 0;
        String codigo = "module test_ciclos\n"
                + "atom p = \"llueve\"\n"
                + "nl texto = \"si llueve salgo\"\n"
                + "formula a = b and p\n"
                + "formula b = a or p\n"
                + "formula c = not c\n"
                + "formula d = texto impl p\n"
                + "vertauto (texto and z)\n";
        prism.pr.ast.PrProgram program = prism.pr.PrParser.parse(codigo);
        List<String> errores = prism.pr.PrValidator.validar(program);
        String todos = String.join("\n", errores);
        if (!todos.contains("Referencia circular en la definición de la fórmula 'a'")) {
            fallos++;
            System.err.println("FALLO [ciclo_mutuo]: " + errores);
        }
        if (!todos.contains("Referencia circular en la definición de la fórmula 'c'")) {
            fallos++;
            System.err.println("FALLO [autociclo]: " + errores);
        }
        if (!todos.contains("es una variable nl")) {
            fallos++;
            System.err.println("FALLO [nl_en_formula]: " + errores);
        }
        if (!todos.contains("Átomo no declarado en 'vertauto inline': z")) {
            fallos++;
            System.err.println("FALLO [vertauto_inline_atomo]: " + errores);
        }
        return fallos;
    }

    private static String repr(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
