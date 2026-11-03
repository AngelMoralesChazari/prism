# Directiva `pattern` en archivos `.lgs`

Los **patrones semánticos** describen formas fijas de secuencias de tokens de lenguaje natural (tras `NormalizadorTexto` y `NaturalLexer`) y cómo se traducen a la IR (`ImpExpr`, `AndExpr`, `OrExpr`).

## Sintaxis (una línea)

```text
pattern <nombre> <token> ... => <tipo_ir> left=<i> right=<j>
pattern <nombre> ... => imp_and left=<i> mid=<k> right=<j>
pattern <nombre> ... => imp_or left=<i> mid=<k> right=<j>
pattern <nombre> ... => imp_or_ant left=<i> mid=<k> right=<j>
pattern <nombre> ... => imp_and_cons left=<i> mid=<k> right=<j>
pattern <nombre> ... => imp_unless left=<i> right=<j>
```

- **`<nombre>`**: identificador sin espacios (aparece en los pasos de análisis).
- **`<token> ...`**: secuencia de tipos léxicos, en el mismo orden que debe producir el lexer. Valores permitidos (insensibles a mayúsculas; guiones se normalizan a guión bajo):

  | En archivo        | `TipoTokenNatural`   |
  |-------------------|----------------------|
  | `si`              | `SI`                 |
  | `entonces`        | `ENTONCES`           |
  | `en_caso_de_que`  | `EN_CASO_DE_QUE`     |
  | `siempre_que`     | `SIEMPRE_QUE`        |
  | `cuando`          | `CUANDO`             |
  | `solo_si`         | `SOLO_SI`            |
  | `a_menos_que`     | `A_MENOS_QUE`        |
  | `y`               | `Y`                  |
  | `o`               | `O`                  |
  | `literal`         | `LITERAL`            |

- **`=>`**: separa la forma del patrón de la salida lógica.
- **`<tipo_ir>`**: `imp`, `and`, `or`, `equiv`, `imp_and`, `imp_or`, `imp_or_ant`, `imp_and_cons`, `imp_unless`. Alias: `implica`, `implies`, `equivalente`, `iff`, `bicondicional`.
- **`left=` / `right=`**: índices **0-based** en la lista de tokens de la forma. Ambos deben apuntar a posiciones cuyo tipo es **`literal`**.
- **`mid=`**: obligatorio para `imp_and`, `imp_or`, `imp_or_ant` e `imp_and_cons`. Ver `docs/LogicScript/PatronesCompuestos.md` y `docs/LogicScript/ConectoresNuevos.md`.

### Implicación (`imp`)

Para `imp`, **`left`** es el índice del **antecedente** y **`right`** el del **consecuente** (orden de argumentos de `ImpExpr` en Java).

Ejemplos:

- `si X entonces Y` → antecedente en el literal tras `si`, consecuente tras `entonces`:

  ```text
  pattern SI_ENTONCES si literal entonces literal => imp left=1 right=3
  ```

- `Y si X` (consecuente antes en la frase) → antecedente es el segundo literal:

  ```text
  pattern CONSECUENTE_SI_ANTECEDENTE literal si literal => imp left=2 right=0
  ```

- **Elipsis sin *entonces*** (*si X Y* = *si X entonces Y* en lectura didáctica):

  ```text
  pattern SI_ELIPTICO si literal literal => imp left=1 right=2
  ```

  Documentación ampliada: **`docs/LogicScript/PatronSiElliptico.md`**.

---

## De dónde salen `left=1` y `right=3` en `SI_ENTONCES`

La línea:

```text
pattern SI_ENTONCES si literal entonces literal => imp left=1 right=3
```

no asigna números “a ojo”: siguen una **convención explícita** que conviene entender antes de escribir patrones nuevos.

### 1. La “forma” del patrón es una lista ordenada (como un vector)

Después de `SI_ENTONCES`, lo que sigue hasta `=>` es una **secuencia fija de tipos de token**, en el mismo orden en que `NaturalLexer` debe emitirlos para una frase válida.

Esa secuencia se puede numerar **desde cero**, igual que un arreglo en Java, C, Python, JavaScript, etc.:

| Índice | Tipo en el archivo | Significado |
|--------|-------------------|-------------|
| **0** | `si` | Token `SI`: la palabra conectora *si*. |
| **1** | `literal` | Primer `LITERAL`: el fragmento de texto que hace de **antecedente** (la condición). |
| **2** | `entonces` | Token `ENTONCES`. |
| **3** | `literal` | Segundo `LITERAL`: el fragmento que hace de **consecuente** (lo que se afirma si se cumple la condición). |

Por tanto:

- **`left=1`** toma el lexema del token en la posición **1** → la proposición **X** en *si X entonces Y*.
- **`right=3`** toma el lexema del token en la posición **3** → la proposición **Y**.

Los índices **0** y **2** no se usan como operandos lógicos porque ahí no hay `literal`: son **conectores léxicos** fijos. La IR solo necesita el texto de las dos proposiciones; los conectores ya cumplieron su papel al **hacer coincidir** la forma.

### 2. Qué significan `left` y `right` en `imp` (idea lógica, no geometría)

En **`imp`**, los nombres `left` y `right` en el archivo significan:

- **`left`** → índice del **antecedente** (hipótesis / condición) de la implicación.
- **`right`** → índice del **consecuente** (tesis / conclusión).

En código Java eso se traduce en `ImpExpr(antecedente, consecuente)` usando esos dos literales. No significan “izquierda de la pantalla”: son **roles en la implicación material** que estudia la lógica proposicional (*si antecedente, entonces consecuente*).

En otros patrones (`and`, `or`), `left` y `right` son simplemente los **dos literales** que van al operador binario (primer y segundo operando según el orden en la lista de tokens), que suele coincidir con el orden natural en la frase (*X y Y*, *X o Y*).

### 3. ¿De dónde sale la idea? (compiladores, lenguajes y plantillas)

No es un formalismo estándar único con un nombre ISO; es una **sintesis deliberada y simple** inspirada en prácticas muy comunes:

1. **Análisis léxico + lista de tokens**  
   Igual que en un compilador: primero se obtiene una secuencia de tokens (`IF`, `IDENT`, `THEN`, …). Aquí usamos `SI`, `LITERAL`, `ENTONCES`, `LITERAL`. El patrón dice: “si la secuencia de **tipos** coincide, entonces…”

2. **Acciones semánticas por posición (como en muchos generadores de parsers)**  
   En herramientas clásicas (**Yacc / Bison**), las reglas tienen acciones que se refieren a **`$1`, `$2`, `$3`…**: el valor asociado al primer, segundo, tercer símbolo de la producción. En **ANTLR** y gramáticas similares se usan etiquetas o referencias a hijos del nodo. La idea es la misma: **tras el encaje de la regla, ¿qué partes del árbol o de la secuencia alimentan la construcción del significado?**  
   En LogicScript, en lugar de `$2` y `$4`, escribimos explícitamente `left=1` y `right=3` porque nuestra lista es solo de **tipos** y los índices van de **0** a **n−1**.

3. **Índices base 0**  
   Coinciden con la convención de la mayoría de lenguajes de programación que usamos en el proyecto (Java, etc.) y con cómo `List.get(i)` accede a la posición `i` en el mapper.

4. **Plantillas o “slots”**  
   También se parece a rellenar huecos en una plantilla: la forma fija es el molde; `left` y `right` indican **qué huecos** (qué posiciones de tipo `literal`) se vuelcan en cada argumento de la conectiva.

5. **No es lo mismo que grupos de una regex sobre caracteres**  
   Una expresión regular puede tener **grupos capturadores** `(…)` y luego `$1`, `$2` sobre **subcadenas de texto**. Aquí los “huecos” son **tokens ya clasificados** por el lexer; el patrón opera a nivel **tipo + orden**, no a nivel carácter.

En resumen: **la lógica es la de un compilador muy reducido** (lexer → secuencia de tokens → regla declarativa → construcción de un fragmento de IR), pero expresada en un archivo de datos para no hardcodear cada patrón en Java.

### 4. Comprobar el encaje con un ejemplo concreto

Frase normalizada: `si llueve entonces llevo paraguas`.

Tokens típicos:

```text
[0] SI("si")
[1] LITERAL("llueve")
[2] ENTONCES("entonces")
[3] LITERAL("llevo paraguas")
```

Coincide con `si literal entonces literal` → se construye `ImpExpr` con el lexema de **1** y el de **3**. Por eso **`left=1`** y **`right=3`** son la traducción directa de esa tabla.

### 5. Patrón `CONSECUENTE_SI_ANTECEDENTE`: por qué `left=2` y `right=0`

Forma: `literal si literal` → índices:

| Índice | Tipo |
|--------|------|
| 0 | `literal` (en la frase coloquial suele ser el **consecuente** dicho primero) |
| 1 | `si` |
| 2 | `literal` (suele ser el **antecedente**) |

En lógica queremos **siempre** `ImpExpr(antecedente, consecuente)`. Por eso:

- **`left=2`** → antecedente (segundo literal).
- **`right=0`** → consecuente (primer literal en la superficie textual).

El español permite invertir el orden respecto a *si X entonces Y*; los índices **corrigen** ese orden para la IR.

---

## Orden y sustitución completa

Las líneas `pattern` se evalúan en **orden de aparición** en el archivo: el primer patrón que coincida con la longitud y tipos de tokens gana.

**Importante:** si el archivo declara **al menos un** `pattern`, **solo** esos patrones se usan (no se mezclan con la lista embebida en código). Si **no** hay ninguna línea `pattern`, `SemanticMapper` aplica los **cinco patrones MVP** compilados por defecto (equivalentes a los de `core.lgs` de ejemplo).

Para ampliar el lenguaje, copia los patrones que necesitas conservar y añade los nuevos **debajo o en el orden deseado** (los más específicos suelen ir antes).

## Relación con el lexer

Los patrones no sustituyen las reglas de `NaturalLexer`: las palabras clave deben tokenizarse igual que hoy (`si `, `entonces `, `en caso de que `, ` y `, ` o `). Cualquier nuevo conector exige, además de un `pattern`, extender el lexer si la frase no se parte en los tokens esperados.

## Cargador

- **Clase:** `tautoteacher2.nlp.lexicon.LgsCargador`
- **Método:** `cargarDesdeClasspath(String)` → `ContenidoLgs` con `lemas()` y `patronesSemanticos()`.

Errores de sintaxis en una línea útil provocan fallo de lectura del stream; el cargador desde classpath devuelve entonces un bundle vacío y la aplicación usa lemas y patrones **embebidos** (véase `BaseConocimiento` y `SemanticMapper`).

## Véase también

- `docs/LogicScript/FormatoLGS.md` — directivas generales del archivo.
- `docs/LogicScript/SemanticMapper.md` — uso en el pipeline.
- `docs/IntegracionApp/MigracionPatronesDeclarativos.md` — impacto en despliegue y app.
