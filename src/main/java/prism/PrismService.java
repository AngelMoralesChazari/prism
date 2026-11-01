package prism;

import nlp.lexicon.BaseConocimiento;

public class PrismService {
    private final PrismEngine engine;

    public PrismService() {
        this.engine = new PrismEngine();
    }

    public PrismResult traducir(String textoNatural) {
        return engine.traducir(textoNatural);
    }

    public PrismResult traducir(String textoNatural, RegistroProposiciones registro) {
        return engine.traducir(textoNatural, registro);
    }

    public BaseConocimiento getBaseConocimiento() {
        return engine.getBaseConocimiento();
    }
}
