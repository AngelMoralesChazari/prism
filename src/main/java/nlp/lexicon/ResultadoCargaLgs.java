package nlp.lexicon;

import java.util.Objects;

/**
 * Resultado explícito de {@link LgsCargador#cargarConDiagnostico(String)}:
 * contenido (puede estar vacío), estado y mensaje para diagnóstico.
 *
 * @see docs/Prism/ErroresCargaLgs.md
 */
public final class ResultadoCargaLgs {

    private final ContenidoLgs contenido;
    private final EstadoCargaLgs estado;
    private final String mensaje;
    private final String rutaRecurso;

    public ResultadoCargaLgs(ContenidoLgs contenido, EstadoCargaLgs estado, String mensaje, String rutaRecurso) {
        this.contenido = Objects.requireNonNull(contenido, "contenido");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.mensaje = mensaje == null ? "" : mensaje;
        this.rutaRecurso = rutaRecurso == null ? "" : rutaRecurso;
    }

    public ContenidoLgs contenido() {
        return contenido;
    }

    public EstadoCargaLgs estado() {
        return estado;
    }

    public String mensaje() {
        return mensaje;
    }

    public String rutaRecurso() {
        return rutaRecurso;
    }

    /** {@code true} si la traducción LN no debe continuar (error de sintaxis o lectura). */
    public boolean bloqueaTraduccion() {
        return estado == EstadoCargaLgs.ERROR_SINTAXIS || estado == EstadoCargaLgs.ERROR_LECTURA;
    }

    public boolean exito() {
        return estado == EstadoCargaLgs.EXITO;
    }

    /** Mensaje listo para {@link prism.PrismResult#error}. */
    public String mensajeParaUsuario() {
        if (mensaje.isEmpty()) {
            return "Error al cargar " + rutaRecurso;
        }
        if (rutaRecurso.isEmpty()) {
            return mensaje;
        }
        return rutaRecurso + ": " + mensaje;
    }
}
