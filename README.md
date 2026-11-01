# Prism

[![JitPack](https://jitpack.io/v/AngelMoralesChazari/LogicScript.svg)](https://jitpack.io/#AngelMoralesChazari/LogicScript)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

Lenguaje de programación **declarativo** y motor simbólico para formalizar un subconjunto controlado de **lenguaje natural (español)** en **fórmulas de lógica proposicional** y evaluar automáticamente sus tablas de verdad.

---

## ⚡ El lenguaje puro (`.pr`) en acción

Escribe tu lógica en un archivo con extensión `.pr` (por ejemplo, `ejercicio.pr`):

```pr
module ejercicio
use core

atom p = "llueve"
atom q = "paraguas"

nl linea = "si llueve llevo paraguas"
let implicacion = translate(linea)

vertauto implicacion {
    steps
    table
    verdict
}
```

### Ejecutar en consola:

```bash
java -jar prism.jar run ejercicio.pr
```

### Salida generada:

```text
=== Traducción (NL → fórmula) ===
Entrada: si llueve llevo paraguas
  - Entrada normalizada: si llueve llevo paraguas
  - Lexemas LN: [SI("si"), LITERAL("llueve"), LITERAL("llevo paraguas")]
  - Bloque 1: si llueve llevo paraguas
  - Tokens bloque: [SI("si"), LITERAL("llueve"), LITERAL("llevo paraguas")]
  - SemanticMapper: patrón SI_ELIPTICO detectado.
  - Nueva proposición: p = "llover"
  - Nueva proposición: q = "llevar paraguas"
Proposiciones: {llueve=p, paraguas=q}
Fórmula emitida: (p → q)

=== Evaluación lógica ===
  - Identificación de proposiciones atómicas: [p, q]
  - Formalización: (p → q)
  - Construcción del espacio de estados: 2^2 = 4 filas.
  - Fila 1: asignación {p=false, q=false} → V
  - Fila 2: asignación {p=false, q=true} → V
  - Fila 3: asignación {p=true, q=false} → F
  - Fila 4: asignación {p=true, q=true} → V
  - Dictamen final: contingency

=== Tabla de verdad ===
p | q | resultado
F | F | V
F | V | V
V | F | F
V | V | V

=== Dictamen ===
(p → q)  contingency
Clasificación: contingency
```

---

## 🚀 Inicio rápido (Lenguaje Puro)

Si eres estudiante o docente y **solo quieres usar el lenguaje** sin programar en Java:

### 1. Descarga el intérprete
1. Ve a la sección de **[Releases](https://github.com/AngelMoralesChazari/LogicScript/releases)**.
2. Descarga el archivo **`prism.jar`**.
3. *(Requiere tener instalado Java 17 o superior).*

### 2. Ejecuta tus programas
* **Ejecutar un script:**
  ```bash
  java -jar prism.jar run tu_programa.pr
  ```
* **Verificar sintaxis (sin ejecutar):**
  ```bash
  java -jar prism.jar check tu_programa.pr
  ```

### 3. Extensión oficial para Visual Studio Code / Cursor
Para tener **coloreado de sintaxis**, **snippets** y autocompletado:
1. Instala la extensión desde `extensions/prism/` o descarga el `.vsix`.
2. En VS Code, ve a la pestaña **Extensions** (`Ctrl + Shift + X`).
3. Haz clic en el menú de tres puntos `...` arriba a la derecha y selecciona **"Install from VSIX..."** (o abre directamente la carpeta en modo desarrollo).
4. ¡Listo! Cualquier archivo `.pr` tendrá resaltado nativo.

---

## ☕ Uso como Librería en Java (Para Desarrolladores)

Si eres desarrollador y deseas integrar el motor de Prism en tus propias aplicaciones, tutores inteligentes o evaluadores:

### 1. Agregar dependencia (JitPack)

#### En Maven (`pom.xml`):
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.AngelMoralesChazari</groupId>
        <artifactId>Prism</artifactId>
        <version>v0.6.1</version>
    </dependency>
</dependencies>
```

#### En Gradle (`build.gradle`):
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.AngelMoralesChazari:Prism:v0.6.1'
}
```

---

### 2. Código Java: Traducir lenguaje natural a fórmula

```java
import prism.PrismService;
import prism.PrismResult;

public class App {
    public static void main(String[] args) {
        PrismService prism = new PrismService();

        PrismResult res = prism.traducir("si estudio apruebo");

        if (res.isExito()) {
            System.out.println("Fórmula: " + res.getFormula()); 
            // Salida: (p → q)
            System.out.println("Proposiciones: " + res.getProposiciones()); 
            // Salida: {estudio=p, apruebo=q}
        } else {
            System.err.println("Error: " + res.getMensaje());
        }
    }
}
```

---

### 3. Código Java: Ejecutar archivos `.pr` completos (Modo Embebido)

También puedes interpretar scripts `.pr` completos directamente desde tu aplicación Java:

```java
import prism.pr.PrInterpreter;

public class EvaluadorScript {
    public static void main(String[] args) {
        PrInterpreter interpreter = new PrInterpreter();
        // Ejecuta el archivo e imprime el análisis y tabla de verdad
        interpreter.ejecutarArchivo("ejercicio.pr");
    }
}
```

---

## 🛠️ Compilación desde el código fuente

Si deseas contribuir o modificar el motor:

```powershell
# Usando Maven (genera el JAR en target/)
mvn clean package

# O usando el script nativo de PowerShell
.\compile.ps1 -Release 17
```

---

## 📄 Licencia

Este proyecto está bajo la Licencia **Apache 2.0**. Consulta el archivo `LICENSE` para más detalles.
