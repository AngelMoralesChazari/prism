# Documentación LS-J: LogicScript para Desarrolladores Java

Esta guía documenta la **API pública de LogicScript** para su integración en proyectos Java (o Kotlin/Scala/Android). Explica qué bibliotecas importar, qué responsabilidades tiene cada clase, qué métodos expone y **qué hace y qué no hace** cada componente.

---

## 1. Configuración de la Dependencia

Para usar LogicScript en tu proyecto Java, agrega la dependencia desde JitPack:

### Maven (`pom.xml`)
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
        <artifactId>LogicScript</artifactId>
        <version>v0.6.1</version>
    </dependency>
</dependencies>
```

### Gradle (`build.gradle`)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.AngelMoralesChazari:LogicScript:v0.6.1'
}
```

---

## 2. Mapa General de Paquetes

| Paquete | Rol Principal |
| :--- | :--- |
| **`logicscript`** | Motor de procesamiento de Lenguaje Natural (Español) y DTOs de traducción. |
| **`logicscript.vertauto`** | Motor de evaluación semántica: tablas de verdad y dictámenes lógicos. |
| **`logicscript.ls`** | Intérprete y evaluador del lenguaje declarativo `.ls`. |
| **`logicscript.ir`** | Representación Intermedia (AST proposicional inmutable). |

---

## 3. Módulo de Lenguaje Natural (`logicscript`)

### `logicscript.LogicScriptService`
Es la **fachada principal** para traducir español controlado a lógica proposicional formal.

#### Qué hace:
* Recibe oraciones en español (ej. `"si estudio apruebo"` o `"llueve o truena"`).
* Carga automáticamente el conocimiento léxico y sintáctico (`core.lgs`).
* Aplica normalización, lematización, análisis sintáctico y mapeo semántico.
* Asigna variables atómicas proposicionales (`p`, `q`, `r`...).
* Construye el árbol lógico IR y emite la fórmula simbólica.

#### Qué NO hace:
* **No calcula tablas de verdad ni dictámenes** (para eso se usa `VertautoService`).
* **No ejecuta scripts `.ls`** (para eso se usa `LsInterpreter`).
* **No lanza excepciones no controladas** si la frase no se entiende; en su lugar, devuelve un `LogicScriptResult` con `isExito() == false`.

#### Métodos principales:
```java
public LogicScriptResult traducir(String textoNatural);
```

#### Ejemplo de uso:
```java
import logicscript.LogicScriptService;
import logicscript.LogicScriptResult;

LogicScriptService service = new LogicScriptService();
LogicScriptResult resultado = service.traducir("si no llueve voy al parque");

if (resultado.isExito()) {
    System.out.println("Fórmula: " + resultado.getFormula()); // (¬p → q)
}
```

---

### `logicscript.LogicScriptResult`
Es el **DTO inmutable** que encapsula el resultado del proceso de traducción.

#### Qué hace:
* Informa si el procesamiento tuvo éxito (`isExito()`).
* Contiene la fórmula resultante en formato cadena (ej. `"(p → q)"`).
* Ofrece el mapa de correspondencia entre fragmentos de texto y variables proposicionales (ej. `{llueve=p, paraguas=q}`).
* Contiene la traza detallada paso a paso del análisis lingüístico (`getPasosDeAnalisis()`).
* Devuelve el mensaje de error o éxito (`getMensaje()`).

#### Qué NO hace:
* No permite mutar sus datos (es de solo lectura).
* No realiza validaciones adicionales.

#### Métodos principales:
| Método | Tipo devuelto | Descripción |
| :--- | :--- | :--- |
| `isExito()` | `boolean` | `true` si se reconoció la frase y se emitió la fórmula. |
| `getFormula()` | `String` | La fórmula simbólica generada (ej. `"(p → q)"`). |
| `getMensaje()` | `String` | Mensaje informativo o descripción del error. |
| `getProposiciones()` | `Map<String, String>` | Mapa de fragmentos a letras: `{estudio=p, apruebo=q}`. |
| `getPasosDeAnalisis()`| `List<String>` | Lista de pasos que siguió el motor (tokenización, patrón detectado, etc.). |

---

## 4. Módulo de Tablas de Verdad y Evaluación (`logicscript.vertauto`)

### `logicscript.vertauto.VertautoService`
Es el motor encargado de evaluar la semántica formal de una proposición y calcular su tabla de verdad.

#### Qué hace:
* Evalúa fórmulas en **notación clásica**: `(p → q)`, `(p ∧ ¬q)`, `(p ∨ q)`.
* Evalúa expresiones en **sintaxis LogicScript**: `p or not p`, `p impl q`.
* Permite evaluar directamente desde español (`vertautoDesdeNl`), combinando traducción y evaluación en una sola llamada.
* Determina el espacio de estados ($2^n$ filas) y calcula el resultado booleano de cada fila.

#### Qué NO hace:
* No guarda memoria entre evaluaciones; cada llamada es pura e independiente.

#### Métodos principales:
```java
// 1. Evaluar fórmula simbólica clásica
public VertautoResult vertautoClasica(String formulaClasica);

// 2. Evaluar expresión en sintaxis LogicScript
public VertautoResult vertautoExpr(String expresionLogicScript);

// 3. Traducir español y evaluar en un solo paso
public VertautoResult vertautoDesdeNl(String textoNatural);
```

#### Ejemplo de uso:
```java
import logicscript.vertauto.VertautoService;
import logicscript.vertauto.VertautoResult;

VertautoService vertauto = new VertautoService();

// Evaluar una expresión lógica
VertautoResult res = vertauto.vertautoExpr("p or not p");

System.out.println("Dictamen: " + res.getDictamen()); // TAUTOLOGY
res.imprimirModoCorto(); // Imprime en consola: (p ∨ ¬p)  tautology
```

---

### `logicscript.vertauto.VertautoResult`
Es el objeto que contiene el análisis de verdad completo.

#### Qué hace:
* Almacena el dictamen formal (`TipoDictamen`).
* Lista de variables atómicas involucradas (`getAtomos()`).
* Lista de filas calculadas (`getFilasTabla()`).
* Pasos paso a paso de evaluación (`getPasosEvaluacion()`).
* Métodos de utilidad para imprimir formateado en consola: `imprimirModoCorto()` y `imprimirExtendido(steps, table, verdict)`.

#### Qué NO hace:
* No recalcula filas; es un reporte estático.

#### Métodos principales:
| Método | Tipo devuelto | Descripción |
| :--- | :--- | :--- |
| `getDictamen()` | `TipoDictamen` | Enum con el resultado: `TAUTOLOGY`, `CONTINGENCY` o `CONTRADICTION`. |
| `getFormula()` | `String` | Cadena de la fórmula evaluada. |
| `getAtomos()` | `List<String>` | Lista de proposiciones atómicas (ej. `["p", "q"]`). |
| `getFilasTabla()` | `List<String>` | Representación textual de las filas de la tabla de verdad. |
| `imprimirExtendido(boolean steps, boolean table, boolean verdict)` | `void` | Imprime la tabla y el dictamen con formato tabular en `System.out`. |

---

### `logicscript.vertauto.TipoDictamen` (Enum)
Representa los tres posibles veredictos lógicos:
```java
public enum TipoDictamen {
    TAUTOLOGY,      // Verdadera en el 100% de las filas
    CONTINGENCY,    // Verdadera en algunas filas y falsa en otras
    CONTRADICTION   // Falsa en el 100% de las filas
}
```

---

## 5. Módulo del Lenguaje Declarativo (`logicscript.ls`)

### `logicscript.ls.LsInterpreter`
Es el **intérprete oficial** para ejecutar programas escritos en el lenguaje `.ls` directamente desde código Java.

#### Qué hace:
* Ejecuta archivos `.ls` completos leyendo desde el disco (`ejecutarArchivo(ruta)`).
* O ejecuta un AST parsed (`ejecutar(LsProgram)`).
* Soporta todas las sentencias del lenguaje: `module`, `use`, `atom`, `nl`, `let translate(...)`, `formula`, y `vertauto`.
* Imprime automáticamente los pasos, tablas de verdad y dictámenes configurados en el archivo `.ls`.

#### Qué NO hace:
* **No compila a archivos `.class` de bytecode** (funciona como un intérprete dinámico en memoria).
* No permite importar módulos externos además de `core`.

#### Métodos principales:
```java
public void ejecutarArchivo(String rutaArchivo);
public void ejecutar(LsProgram program);
```

#### Ejemplo de uso:
```java
import logicscript.ls.LsInterpreter;

public class EjecutorDemo {
    public static void main(String[] args) {
        LsInterpreter interpreter = new LsInterpreter();

        // Ejecuta el programa .ls y muestra todo en consola
        interpreter.ejecutarArchivo("src/prueba.ls");
    }
}
```

---

## 6. Módulo de Representación Intermedia (`logicscript.ir`)

Para proyectos avanzados que necesitan manipular matemáticamente las expresiones lógicas en memoria como árboles de objetos:

### `logicscript.ir.LogicExpr` (Interface Sellada)
Es el contrato raíz para cualquier nodo del árbol de sintaxis abstracta lógica.

Nodos disponibles:
* **`AtomExpr(String name)`**: Proposición atómica (ej. `p`, `q`).
* **`NegExpr(LogicExpr child)`**: Negación lógica (`¬A`).
* **`AndExpr(LogicExpr left, LogicExpr right)`**: Conjunción (`A ∧ B`).
* **`OrExpr(LogicExpr left, LogicExpr right)`**: Disyunción (`A ∨ B`).
* **`ImpExpr(LogicExpr left, LogicExpr right)`**: Condicional / Implicación (`A → B`).
* **`EquivExpr(LogicExpr left, LogicExpr right)`**: Bicondicional / Equivalencia (`A ↔ B`).

### `logicscript.ir.EmitidorFormula`
Convierte cualquier árbol `LogicExpr` en una cadena formateada legible:

```java
import logicscript.ir.*;

// Construir manualmente: (p ∧ q) → r
LogicExpr arbol = new ImpExpr(
    new AndExpr(new AtomExpr("p"), new AtomExpr("q")),
    new AtomExpr("r")
);

String salida = EmitidorFormula.emitir(arbol);
System.out.println(salida); // ((p ∧ q) → r)
```

---

## 7. Casos de Uso Típicos en Proyectos Reales

### Caso A: Crear un Validador Escolar o Tutor Inteligente
Permite a una plataforma web evaluar enunciados ingresados por estudiantes en un campo de texto:

```java
import logicscript.LogicScriptService;
import logicscript.LogicScriptResult;
import logicscript.vertauto.VertautoService;
import logicscript.vertauto.VertautoResult;

public class ValidadorTutor {
    private final LogicScriptService nlService = new LogicScriptService();
    private final VertautoService vertautoService = new VertautoService();

    public void analizarRespuestaEstudiante(String entradaEstudiante) {
        LogicScriptResult trad = nlService.traducir(entradaEstudiante);

        if (!trad.isExito()) {
            System.out.println("No se pudo formalizar: " + trad.getMensaje());
            return;
        }

        System.out.println("Formalización: " + trad.getFormula());
        System.out.println("Proposiciones: " + trad.getProposiciones());

        VertautoResult evaluacion = vertautoService.vertautoClasica(trad.getFormula());
        System.out.println("Veredicto: " + evaluacion.getDictamen());
    }
}
```

---

### Caso B: Embeber y Ejecutar Scripts `.ls` como Motor de Reglas
Cargar archivos `.ls` externos para definir escenarios lógicos sin recompilar tu aplicación Java:

```java
import logicscript.ls.LsInterpreter;

public class SistemaReglas {
    public static void main(String[] args) {
        LsInterpreter interpreter = new LsInterpreter();
        // Ejecuta dinámicamente un archivo de reglas
        interpreter.ejecutarArchivo("reglas/caso_seguridad.ls");
    }
}
```

---

## 8. Resumen de Buenas Prácticas

1. **Verificar siempre `isExito()`:** Antes de llamar a `resultado.getFormula()`, verifica siempre `if (resultado.isExito())` para evitar trabajar con fórmulas vacías si la frase no fue reconocida.
2. **Uso de Comentarios en scripts `.ls`:** Recuerda que dentro de archivos `.ls` los comentarios se escriben con `#` (estilo script), no con `//`.
3. **Nombres de Módulo:** El nombre declarado en `module mi_nombre` debe coincidir exactamente con el nombre de archivo `mi_nombre.ls`.
