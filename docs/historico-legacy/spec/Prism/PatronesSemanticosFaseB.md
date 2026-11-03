# Fase B — Patrones semánticos elípticos compuestos (v0.5)

Extiende LogicScript para enunciados del curso donde el **antecedente** de una implicación elíptica combina dos proposiciones con **y** u **o**, sin la palabra *entonces*, y usando cualquiera de los conectores condicionales **si**, **cuando** o **siempre que**.

**Estado:** implementado en `core.lgs` v0.5.

---

## Problema que resuelve

Antes de v0.5, frases como estas caían en **fallback** (solo el primer literal como átomo `p`):

| Entrada | Resultado v0.4 | Resultado v0.5 |
|---------|----------------|----------------|
| *si llueve o solea salgo* | `p` | `((p ∨ q) → r)` |
| *siempre que estudio y practico apruebo* | `p` | `((p ∧ q) → r)` |
| *cuando llueve o solea salgo* | `p` | `((p ∨ q) → r)` |

El lexer ya partía correctamente tras **y** dentro de un bloque *si …* (patrón `SI_CONJ_Y_ELIPTICO`, v0.3). Faltaba:

1. El mismo comportamiento tras **o** (disyunción en antecedente elíptico).
2. Los mismos patrones para **cuando** y **siempre que** (no solo *si*).
3. Declarar los cinco patrones nuevos en `core.lgs` y en el respaldo embebido de `SemanticMapper`.

---

## Los cinco patrones nuevos

Todos usan **5 tokens** (sin *entonces*). El consecuente es el último literal.

| Nombre | Forma LN | Fórmula | Directiva `.lgs` |
|--------|----------|---------|------------------|
| `SI_DISY_O_ELIPTICO` | si *X* o *Y* *Z* | `((p ∨ q) → r)` | `imp_or_ant left=1 mid=3 right=4` |
| `SIEMPRE_QUE_CONJ_Y_ELIPTICO` | siempre que *X* y *Y* *Z* | `((p ∧ q) → r)` | `imp_and left=1 mid=3 right=4` |
| `SIEMPRE_QUE_DISY_O_ELIPTICO` | siempre que *X* o *Y* *Z* | `((p ∨ q) → r)` | `imp_or_ant left=1 mid=3 right=4` |
| `CUANDO_CONJ_Y_ELIPTICO` | cuando *X* y *Y* *Z* | `((p ∧ q) → r)` | `imp_and left=1 mid=3 right=4` |
| `CUANDO_DISY_O_ELIPTICO` | cuando *X* o *Y* *Z* | `((p ∨ q) → r)` | `imp_or_ant left=1 mid=3 right=4` |

`SI_CONJ_Y_ELIPTICO` (*si X y Y Z*) existía desde v0.3; en v0.5 se mantiene y se documenta junto a la familia elíptica compuesta.

### Orden en `core.lgs`

Los patrones de **más tokens** deben ir **antes** de los más cortos (`SI_ENTONCES`, `SI_ELIPTICO`, etc.). En v0.5 el bloque relevante es:

```text
pattern SI_DISY_O_ENTONCES si literal o literal entonces literal => imp_or_ant left=1 mid=3 right=5
pattern SI_DISY_O_ELIPTICO si literal o literal literal => imp_or_ant left=1 mid=3 right=4
pattern SIEMPRE_QUE_CONJ_Y_ELIPTICO siempre_que literal y literal literal => imp_and left=1 mid=3 right=4
pattern SIEMPRE_QUE_DISY_O_ELIPTICO siempre_que literal o literal literal => imp_or_ant left=1 mid=3 right=4
pattern CUANDO_CONJ_Y_ELIPTICO cuando literal y literal literal => imp_and left=1 mid=3 right=4
pattern CUANDO_DISY_O_ELIPTICO cuando literal o literal literal => imp_or_ant left=1 mid=3 right=4
pattern SI_CONJ_Y_ELIPTICO si literal y literal literal => imp_and left=1 mid=3 right=4
```

`SI_DISY_O_ELIPTICO` va **después** de `SI_DISY_O_ENTONCES` (6 tokens) y **antes** de los elípticos con *cuando* / *siempre que*.

---

## Ejemplo completo: `SI_DISY_O_ELIPTICO`

**Entrada:** *si llueve o solea salgo*

### 1. Normalización

`NormalizadorTexto` deja minúsculas, espacios simples, sin cambios semánticos.

### 2. Tokenización (`NaturalLexer`)

| Índice | Tipo | Lexema | Notas |
|--------|------|--------|-------|
| 0 | `SI` | si | inicio bloque condicional |
| 1 | `LITERAL` | llueve | un verbo → un literal |
| 2 | `O` | o | conector disyuntivo |
| 3 | `LITERAL` | solea | parte tras `o` (ver abajo) |
| 4 | `LITERAL` | salgo | consecuente elíptico |

**Regla clave del lexer:** tras `Y` u `O`, si aún no apareció `entonces` y ya hubo `si` / `cuando` / `siempre que`, el siguiente fragmento multi-palabra se **parte en un literal por palabra** (`ultimoTokenParteLiteralEnPalabras`). Así *solea salgo* no queda como un solo literal.

### 3. Coincidencia de patrón (`SemanticMapper`)

Forma del patrón: `[SI, LITERAL, O, LITERAL, LITERAL]` — coincide con los 5 tokens.

### 4. Construcción IR

Directiva `imp_or_ant left=1 mid=3 right=4`:

- `left` (índice 1) → *llueve* → `llover`
- `mid` (índice 3) → *solea* → `hacer_sol` (lemma)
- `right` (índice 4) → *salgo* → `salir` (lemma)

IR: `ImpExpr(OrExpr(llover, hacer_sol), salir)`

### 5. Emisión

`EmitidorFormula` asigna letras `p`, `q`, `r` → **`((p ∨ q) → r)`**.

---

## Ejemplo: antecedente con ∧ (`SIEMPRE_QUE_CONJ_Y_ELIPTICO`)

**Entrada:** *siempre que estudio y practico apruebo*

| Índice | Token |
|--------|-------|
| 0 | `SIEMPRE_QUE` |
| 1 | `LITERAL` estudio |
| 2 | `Y` |
| 3 | `LITERAL` practico |
| 4 | `LITERAL` apruebo |

Directiva `imp_and` → `ImpExpr(AndExpr(estudiar, practicar), aprobar)` → **`((p ∧ q) → r)`**.

Los verbos *estudio* y *practico* se resuelven por **morfología** (Fase A); *apruebo* por **lemma** irregular.

---

## Composición por coma (sin cambio de motor)

**Entrada:** *cuando llueve llevo paraguas, si estudio apruebo*

`LogicScriptEngine.segmentosCompuestos` parte en la coma seguida de *si* / *cuando* / *siempre que*:

1. `cuando llueve llevo paraguas` → `CUANDO_ELIPTICO` → `(p → q)`
2. `si estudio apruebo` → `SI_ELIPTICO` → `(r → s)`

Composición final: **`((p → q) ∧ (r → s))`**.

Caso de regresión: `composicion_cuando_si`.

---

## Cambio en `NaturalLexer` (Java)

Método `ultimoTokenParteLiteralEnPalabras`:

- **Antes (v0.3):** solo consideraba bloques que empezaban con `SI`.
- **Después (v0.5):** considera `SI`, `SIEMPRE_QUE` y `CUANDO` (`esInicioBloqueCondicional`).
- Sigue devolviendo `false` si ya apareció `ENTONCES` en el bloque (el consecuente multi-palabra tras *entonces* sigue siendo un solo literal, p. ej. *llevo paraguas*).

```text
si llueve o solea salgo
     │      │  │     │
     SI     │  O     └── literales separados (tras O, sin entonces)
            └── literal único
```

---

## Archivos tocados

| Archivo | Cambio |
|---------|--------|
| `NaturalLexer.java` | Partir literales tras `y`/`o` en bloques `si` / `cuando` / `siempre que` elípticos |
| `core.lgs` | v0.5 + 5 directivas `pattern` |
| `SemanticMapper.java` | Respaldo embebido con los mismos 5 patrones |
| `LogicScriptRegressionHarness.java` | 6 casos nuevos (37 total) |

---

## Casos de regresión

| ID | Entrada | Fórmula esperada |
|----|---------|------------------|
| `si_disy_o_eliptico` | si llueve o solea salgo | `((p ∨ q) → r)` |
| `siempre_que_conj_eliptico` | siempre que estudio y practico apruebo | `((p ∧ q) → r)` |
| `siempre_que_disy_eliptico` | siempre que llueve o solea salgo | `((p ∨ q) → r)` |
| `cuando_conj_eliptico` | cuando estudio y practico apruebo | `((p ∧ q) → r)` |
| `cuando_disy_eliptico` | cuando llueve o solea salgo | `((p ∨ q) → r)` |
| `composicion_cuando_si` | cuando llueve llevo paraguas, si estudio apruebo | `((p → q) ∧ (r → s))` |

---

## Cómo probar

```powershell
cd TautoTeacher2.0
.\compile.ps1 -JdkHome "C:\Program Files\Java\jdk-25.0.2"
java -cp out tautoteacher2.logicscript.LogicScriptCli "si llueve o solea salgo"
java -cp out tautoteacher2.logicscript.LogicScriptRegressionHarness
```

Salida esperada del harness: **`OK (37 casos LN + diagnóstico .lgs)`**.

En la app: pestaña **Lenguaje natural** → procesar → comprobar fórmula y tabla de verdad (3 variables para patrones compuestos).

---

## Operativa: recursos en `out/`

El motor carga `logicscript/core.lgs` desde el classpath (`out/logicscript/...`). Tras editar el `.lgs` en `src/main/resources`, **hay que recompilar** (`compile.ps1` copia recursos). Si solo se cambió Java, igual conviene recompilar para no dejar `out/logicscript/core.lgs` desfasado respecto a `src`.

---

## Limitaciones (v0.5)

| Fuera de alcance | Ejemplo |
|------------------|---------|
| Consecuente compuesto elíptico sin *entonces* | *si llueve salgo o me quedo* |
| Anidación sin `si`/`cuando` | *llueve y estudio o descanso* |
| Tres o más átomos en antecedente | *si estudio y practico y duermo apruebo* |
| Negación dentro del antecedente compuesto | *si no llueve o solea salgo* (parcial; ver casos `no` en lexer) |

---

## Relación con otras fases

| Fase | Qué aporta a estas frases |
|------|---------------------------|
| **A (morfología)** | *estudio*, *practico* sin `lemma` extra |
| **B (esta)** | Estructura `((p ∧ q) → r)` / `((p ∨ q) → r)` con conectores variados |
| **C (`lexrule`)** | Futuro: reglas morfológicas en `.lgs` sin tocar Java |

---

## Referencias

- `PatronesCompuestos.md` — tabla ampliada v0.5
- `PatronSiElliptico.md` — implicación sin *entonces* (2 tokens)
- `RoadmapLexico.md` — Fase B marcada como hecha
- `PruebasRegresion.md` — harness y conteo de casos
- `Pattern.md` — sintaxis de directivas `pattern`
