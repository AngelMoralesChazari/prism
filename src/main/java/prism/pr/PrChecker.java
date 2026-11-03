package prism.pr;

import prism.PrismService;
import prism.pr.ast.PrProgram;
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
        try {
            String source = PrSourceLoader.leer(ruta);
            PrProgram program = PrParser.parse(source);
            List<String> avisos = PrSemanticLinter.diagnosticarPrograma(program, new PrismService());
            PrSemanticLinter.imprimirAvisos(avisos);
        } catch (Exception ignored) {
        }
        System.out.println("OK: " + ruta);
        return true;
    }
}
