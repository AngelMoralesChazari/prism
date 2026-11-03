# Composición por coma entre cláusulas *si…*

Permite traducir enunciados con **varias condicionales** unidas por coma, leyendo la frase completa como **conjunción** (∧) de implicaciones independientes.

---

## Motivación

Enunciados didácticos frecuentes:

- *Si estudio apruebo, si no estudio no apruebo.*
- *Si llueve llevo paraguas, si hace sol uso gorra.*

Cada mitad es una implicación (patrón `SI_ELIPTICO` o `SI_ENTONCES`). Sin composición, el motor trataba la frase entera como un solo bloque y caía en **fallback** (`p`).

---

## Regla de partición

Tras la normalización, `LogicScriptEngine.segmentosCompuestos` divide el texto en dos tipos de corte:

| Separador | Efecto |
|-----------|--------|
| `, si ` (coma + nueva cláusula *si*) | Nuevo bloque; cada uno se traduce por separado |
| `, en caso de que ` | Segundo bloque con prefijo `en caso de que ` restaurado (comportamiento previo) |

Los bloques se traducen en orden y se combinan con `AndExpr` → fórmula `(A ∧ B ∧ …)`.

**Orden:** primero se parte por `, si `; dentro de cada tramo, si aplica, por `, en caso de que `.

---

## Ejemplo principal

**Entrada:** `si estudio apruebo, si no estudio no apruebo`

1. Segmentos: `si estudio apruebo` | `si no estudio no apruebo`
2. Bloque 1 → patrón `SI_ELIPTICO` → `(p → q)` con *estudiar*, *aprobar*
3. Bloque 2 → `SI_ELIPTICO` con literales `no estudio`, `no apruebo` (fusión lexer) → `(¬p → ¬q)`
4. **Fórmula:** `((p → q) ∧ (¬p → ¬q))`

La segunda cláusula expresa la **contraposición informal** (*si no estudio, no apruebo*), habitual en explicaciones de lógica proposicional.

---

## Negación tras *si* (lexer)

Para `si no estudio no apruebo`, tras `SI` el lexer parte el complemento en palabras y **fusiona** `no` con la siguiente (`no estudio`, `no apruebo`). Así el patrón de tres tokens sigue aplicando y `SemanticMapper.atomoDesde` reconoce el prefijo `no ` como átomo negado.

---

## Contraste con otros casos

| Entrada | Partición | Resultado |
|---------|-----------|-----------|
| `si llueve entonces llevo paraguas, en caso de que estudio, apruebo` | Solo `, en caso de que` | `((p → q) ∧ (r → s))` |
| `si estudio apruebo, si no estudio no apruebo` | `, si ` | `((p → q) ∧ (¬p → ¬q))` |
| `si estudio apruebo` | Un bloque | `(p → q)` |

La coma **interior** de *en caso de que estudio, apruebo* no activa el corte por `, si ` porque lo que sigue a la coma no es `si `.

---

## Implementación Java

| Pieza | Ubicación |
|-------|-----------|
| Partición de bloques | `LogicScriptEngine.segmentosCompuestos` |
| Fusión `no` + palabra | `NaturalLexer.fusionarPrefijosNo` |
| Pruebas | `LogicScriptRegressionHarness` casos `si_eliptico_negado`, `composicion_coma_si` |

---

## Limitaciones (v0.2)

- Solo se reconoce coma seguida de **`si `** como inicio de nuevo bloque condicional; otras secuencias (*, y si*, *, cuando*) quedan fuera.
- Más de dos cláusulas *si…* separadas por coma sí están soportadas (cadena de `AndExpr`).
- No hay patrón declarativo en `.lgs` para la composición: es **orquestación** del motor, no un `pattern` semántico.

---

## Referencias

- `docs/LogicScript/PatronSiElliptico.md` — implicación sin *entonces*
- `docs/LogicScript/PruebasRegresion.md` — harness
