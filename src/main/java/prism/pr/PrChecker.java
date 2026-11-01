package prism.pr;

import java.util.List;

public final class PrChecker {

    private PrChecker() {
    }

    public static boolean checkArchivo(String ruta) {
        List<String> errores = PrValidator.validarArchivo(ruta);
        if (!errores.isEmpty()) {
            for (String error : errores) {
                System.err.println(error);
            }
            return false;
        }
        System.out.println("OK: " + ruta);
        return true;
    }
}
