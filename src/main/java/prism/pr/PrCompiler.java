package prism.pr;

import prism.pr.ast.AtomDecl;
import prism.pr.ast.FormulaDecl;
import prism.pr.ast.FormulaExpr;
import prism.pr.ast.FormulaRef;
import prism.pr.ast.LetDecl;
import prism.pr.ast.NlDecl;
import prism.pr.ast.PrProgram;
import prism.pr.ast.PrStmt;
import prism.pr.ast.TextRef;
import prism.pr.ast.VertautoOptions;
import prism.pr.ast.VertautoStmt;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Compilador oficial de Prism (prc compile).
 * Transpila un programa '.pr' a una clase ejecutable de la JVM (.class).
 */
public final class PrCompiler {

    public record CompileResult(
            boolean exito,
            String moduleName,
            Path classFile,
            Path javaFile,
            String mensaje) {
    }

    public static CompileResult compile(String prSourcePath, String outputDirPath, boolean keepJava) {
        Path prPath = Path.of(prSourcePath);
        if (!Files.exists(prPath)) {
            return new CompileResult(false, null, null, null, "Archivo no encontrado: " + prSourcePath);
        }

        try {
            String source = Files.readString(prPath, StandardCharsets.UTF_8);
            PrProgram program = PrParser.parse(source);

            List<String> errores = new ArrayList<>();
            PrValidator.validarArchivoConvencion(prSourcePath, program.moduleName(), errores);
            if (!errores.isEmpty()) {
                return new CompileResult(false, program.moduleName(), null, null, String.join("\n", errores));
            }

            PrValidator.validarOError(program);

            Path outDir = outputDirPath != null ? Path.of(outputDirPath) : prPath.toAbsolutePath().getParent();
            if (outDir == null) {
                outDir = Path.of(".");
            }
            Files.createDirectories(outDir);

            String moduleName = program.moduleName();
            String javaCode = generateJavaSource(program, source);

            Path javaFile = outDir.resolve(moduleName + ".java");
            Files.writeString(javaFile, javaCode, StandardCharsets.UTF_8);

            Path classFile = outDir.resolve(moduleName + ".class");

            // Compilar a bytecode .class
            boolean compileOk = compileJavaFile(javaFile, outDir);
            if (!compileOk) {
                return new CompileResult(false, moduleName, null, javaFile,
                        "Fallo al generar bytecode con el compilador de Java.");
            }

            if (!keepJava) {
                try {
                    Files.deleteIfExists(javaFile);
                } catch (IOException ignored) {
                }
            }

            return new CompileResult(true, moduleName, classFile, keepJava ? javaFile : null,
                    "Compilación exitosa -> " + classFile);

        } catch (PrParseException | PrValidationException e) {
            return new CompileResult(false, null, null, null, e.getMessage());
        } catch (Exception e) {
            return new CompileResult(false, null, null, null, "Error interno de compilación: " + e.getMessage());
        }
    }

    private static String generateJavaSource(PrProgram program, String originalSource) {
        String module = program.moduleName();
        StringBuilder sb = new StringBuilder();

        sb.append("// Código generado automáticamente por Prism Compiler (prc) v1.0.0\n");
        sb.append("// Módulo: ").append(module).append(".pr\n\n");
        sb.append("import prism.pr.PrInterpreter;\n\n");

        sb.append("public final class ").append(module).append(" {\n\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        String fuente = \"\"\"\n");
        sb.append(originalSource.replace("\"\"\"", "\\\"\\\"\\\""));
        if (!originalSource.endsWith("\n")) {
            sb.append("\n");
        }
        sb.append("\"\"\";\n");
        sb.append("        PrInterpreter.ejecutarCompilado(\"").append(escape(module)).append("\", fuente);\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static boolean compileJavaFile(Path javaFile, Path outDir) {
        String runtimeJar = getRuntimeJarLocation();
        String currentCp = System.getProperty("java.class.path");
        String classpath = (runtimeJar != null ? runtimeJar + File.pathSeparator : "") + currentCp + File.pathSeparator
                + outDir.toAbsolutePath();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            List<String> args = List.of(
                    "-encoding", "UTF-8",
                    "-cp", classpath,
                    "-d", outDir.toAbsolutePath().toString(),
                    javaFile.toAbsolutePath().toString());
            int exitCode = compiler.run(null, null, null, args.toArray(new String[0]));
            if (exitCode == 0) {
                return true;
            }
        }

        // Fallback a llamada de proceso javac externo
        try {
            String javacCmd = resolveJavacExecutable();
            Process process = new ProcessBuilder(
                    javacCmd,
                    "-encoding", "UTF-8",
                    "-cp", classpath,
                    "-d", outDir.toAbsolutePath().toString(),
                    javaFile.toAbsolutePath().toString()).redirectErrorStream(true).start();

            int exit = process.waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String resolveJavacExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
            Path candidate = Path.of(javaHome, "bin", isWin ? "javac.exe" : "javac");
            if (Files.isExecutable(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return "javac";
    }

    private static String getRuntimeJarLocation() {
        try {
            var codeSource = PrCompiler.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                try {
                    String path = Path.of(codeSource.getLocation().toURI()).toAbsolutePath().toString();
                    if (path.endsWith(".jar")) {
                        return path;
                    }
                } catch (Exception ignoredUri) {
                    String path = URLDecoder.decode(codeSource.getLocation().getPath(), StandardCharsets.UTF_8);
                    if (path.endsWith(".jar")) {
                        return path;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Fallbacks comunes
        if (Files.exists(Path.of("prism.jar"))) {
            return Path.of("prism.jar").toAbsolutePath().toString();
        }
        if (Files.exists(Path.of("target/classes"))) {
            return Path.of("target/classes").toAbsolutePath().toString();
        }
        return null;
    }

    private static String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}