# Pruebas de regresión LogicScript (paso 1 del roadmap)

## Qué se hizo primero y por qué

El **primer paso** acordado fue **fijar el comportamiento actual** del pipeline *lenguaje natural → fórmula* antes de seguir con:

- mensajes de error más claros en el cargador `.lgs`,
- más directivas o archivos de datos,
- o cambios grandes en el lexer.

Sin pruebas automáticas, cada cambio en `NaturalLexer`, `LgsCargador`, `SemanticMapper` o `EmitidorFormula` puede romper frases que antes funcionaban **sin que nadie lo note** hasta probar a mano. Las regresiones son especialmente probables porque LogicScript combina **datos** (`core.lgs`) y **código** (lexer + mapper).

Por eso el paso 1 es un **harness de regresión**: una clase Java con `main` que ejecuta una **tabla de casos** (entrada → éxito/error, y si hay éxito, cadena de fórmula **exacta**).

---

## Qué incluye este paso

### 1. Clase `LogicScriptRegressionHarness`

- **Ubicación:** `tautoteacher2.logicscript.LogicScriptRegressionHarness`
- **Fuente:** `TautoTeacher2.0/src/main/java/tautoteacher2/logicscript/LogicScriptRegressionHarness.java`
- **Contrato:** usa `LogicScriptService` (misma fachada que la CLI y la UI), de modo que el test ejerce el **pipeline real** (normalización, lexer, mapper, emisión).
- **Criterios por caso:**
  - `isExito()` debe coincidir con lo esperado.
  - Si se espera éxito, `getFormula()` debe ser **igual** a la cadena esperada (incluye símbolos Unicode `→`, `∧`, `∨` como los emite `EmitidorFormula`).

Los identificadores de caso (`si_entonces`, `conjuncion`, …) sirven para localizar fallos en el informe por consola.

### 2. Corrección en `NaturalLexer` (descubierta al diseñar los casos)

Al preparar casos como **`llueve y estudio`** y **`a y b`**, el lexer producía un solo literal (`"y b"` absorbido en el literal) y **no** emitía el token `Y`. La causa era el orden del bucle: se **consumían espacios y comas antes** de comprobar si en la posición actual comenzaba una palabra clave como **` y `** (que incluye un espacio inicial).

**Cambio lógico:** intentar **`encontrarPalabraClave` primero** y solo si no hay coincidencia, entonces saltar espacios/comas y continuar con literal. Así, tras un literal que termina justo antes de un espacio seguido de ` y `, la coincidencia ocurre en el índice correcto.

- **Clase tocada:** `tautoteacher2.nlp.lexer.NaturalLexer`
- **Efecto:** los patrones `CONJUNCION` y `DISYUNCION` del `.lgs` pasan a aplicarse a frases naturales del tipo *X y Y* / *X o Y* sin requerir trucos de espaciado raros.

Este arreglo forma parte del **mismo paso** porque sin él los casos de regresión para `∧` y `∨` no serían representativos del comportamiento deseado del producto.

---

## Cómo ejecutar las pruebas

1. Compilar el proyecto y copiar `src/main/resources` a `out` (el script del repo ya lo hace):

   ```powershell
   .\compile.ps1 -JdkHome "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
   ```

2. Lanzar el harness:

   ```text
   java -cp out tautoteacher2.logicscript.LogicScriptRegressionHarness
   ```

- Salida **`LogicScriptRegressionHarness: OK (37 casos LN + diagnóstico .lgs).`** y código de salida **0** → todo bien.
- Cualquier discrepancia imprime detalle en **stderr** y el proceso termina con código **1**.

**Requisito:** el recurso `logicscript/core.lgs` debe estar en el classpath (`out/logicscript/...`), igual que para la CLI.

---

## Cómo añadir un caso nuevo

1. Abrir `LogicScriptRegressionHarness.java`.
2. Añadir una línea `casos.add(new Caso("id_unico", "texto ln", true|false, "formula o \"\"));`
   - Para éxito, la fórmula debe ser **idéntica** a la de `EmitidorFormula` (paréntesis incluidos). Los símbolos lógicos en el fuente del harness usan escapes Unicode (`\u2192`, `\u2227`, `\u2228`) para no depender del encoding del editor.
3. Si la frase nueva usa lemas, asegurarse de que existan en `core.lgs` (o en el respaldo embebido de `BaseConocimiento`) para que la canonización sea estable.
4. Recompilar y ejecutar el harness.

Si el cambio intencional **altera** el formato de la fórmula, habrá que **actualizar** las cadenas esperadas y documentar el cambio en el changelog de `docs/LogicScript.md`.

---

## Alcance y límites (hoy)

| Aspecto | Estado |
|---------|--------|
| Framework | Sin Maven/Gradle; **no** JUnit en el classpath por defecto. |
| Qué se comprueba | Fórmula final y éxito/error; **no** se asserta el mapa de proposiciones ni cada paso de análisis (se puede ampliar después). |
| Motor lógico | **No** forma parte del harness: solo el pipeline hasta la cadena simbolizada. |
| CI | Integrar este comando en el pipeline de integración continua cuando exista. |

---

## Próximo paso sugerido (paso 2, cuando toque)

~~Tras tener regresión verde, el siguiente hito lógico es **hacer visibles los errores de parseo del `.lgs`**~~ → **hecho:** ver `docs/LogicScript/ErroresCargaLgs.md`.

**Siguiente:** más **`lemma`** del curso, negación declarativa en `.lgs` y ampliar el parser (`ParserNatural`).

Composición por coma entre cláusulas *si…* → **hecho:** ver `docs/LogicScript/ComposicionComaSi.md` (casos `si_eliptico_negado`, `composicion_coma_si`).

**Fase 1 (v0.3):** conectores y patrones compuestos — ver `ConectoresNuevos.md`.

**Fase A léxica (v0.4):** `NormalizadorMorfologico` — ver `NormalizadorMorfologico.md`, casos `morph_*`.

**Fase B (v0.5):** patrones elípticos compuestos — ver `PatronesSemanticosFaseB.md`.

**Fase C (v0.6):** `lexrule` morfológica declarativa — ver `Lexrule.md`; diagnóstico `lexrule_sintaxis`, `lexrule_carga`.

---

## Referencias

- Mapa código ↔ conceptos: `docs/LogicScript/MapaImplementacionJava.md`
- Roadmap general: `docs/LogicScript.md` §11
- Integración pipeline: `docs/LogicScript/CambiosIntegracionGeneral.md`
