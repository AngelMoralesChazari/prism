package prism;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

    private static String repr(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
