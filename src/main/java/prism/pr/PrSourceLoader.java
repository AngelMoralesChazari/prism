package prism.pr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PrSourceLoader {

    private PrSourceLoader() {
    }

    public static String leer(String ruta) {
        try {
            return Files.readString(Path.of(ruta), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PrRuntimeException("No se pudo leer el archivo .pr: " + ruta + " — " + e.getMessage());
        }
    }
}
