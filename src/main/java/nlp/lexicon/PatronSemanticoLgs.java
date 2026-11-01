package nlp.lexicon;

import java.util.List;
import nlp.lexer.TipoTokenNatural;

/**
 * Patrón declarativo: secuencia de tipos de token y qué slots {@link TipoTokenNatural#LITERAL}
 * alimentan la IR (conectiva binaria o implicación con antecedente/consecuente compuesto).
 */
public record PatronSemanticoLgs(
        String nombre,
        List<TipoTokenNatural> forma,
        TipoSalidaIrPatron tipoIr,
        int indiceIzq,
        int indiceDer,
        int indiceMedio) {

    public PatronSemanticoLgs(
            String nombre,
            List<TipoTokenNatural> forma,
            TipoSalidaIrPatron tipoIr,
            int indiceIzq,
            int indiceDer) {
        this(nombre, forma, tipoIr, indiceIzq, indiceDer, -1);
    }

    public PatronSemanticoLgs {
        forma = List.copyOf(forma);
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre vacío");
        }
        validarIndice(forma, indiceIzq, "left");
        validarIndice(forma, indiceDer, "right");
        if (tipoIr == TipoSalidaIrPatron.IMP_AND
                || tipoIr == TipoSalidaIrPatron.IMP_OR
                || tipoIr == TipoSalidaIrPatron.IMP_OR_ANT
                || tipoIr == TipoSalidaIrPatron.IMP_AND_CONS) {
            validarIndice(forma, indiceMedio, "mid");
        } else if (indiceMedio >= 0) {
            throw new IllegalArgumentException("mid solo aplica a imp_and/imp_or/imp_or_ant/imp_and_cons: " + nombre);
        }
    }

    private static void validarIndice(List<TipoTokenNatural> forma, int indice, String etiqueta) {
        if (indice < 0 || indice >= forma.size()) {
            throw new IllegalArgumentException("índice " + etiqueta + " fuera de rango");
        }
        if (forma.get(indice) != TipoTokenNatural.LITERAL) {
            throw new IllegalArgumentException("slot " + etiqueta + " debe ser literal");
        }
    }
}
