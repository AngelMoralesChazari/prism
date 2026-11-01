package prism;

public class PrismService {
    private final PrismEngine engine;

    public PrismService() {
        this.engine = new PrismEngine();
    }

    public PrismResult traducir(String textoNatural) {
        return engine.traducir(textoNatural);
    }
}
