# Patrón `SI_ELIPTICO` (implicación sin *entonces*)

Extiende LogicScript para enunciados coloquiales del tipo **«si X Y»**, donde falta el conector explícito *entonces* pero la lectura didáctica sigue siendo **implicación material** (*si X, entonces Y*).

---

## Motivación

En español oral y escrito es frecuente:

- *Si estudio apruebo.*
- *Si llueve llevo paraguas.*

Antes del patrón, el lexer producía `[SI, LITERAL, LITERAL]` pero **ningún** `pattern` coincidía (el patrón `SI_ENTONCES` exige cuatro tokens con `entonces` en medio). El mapper caía en **fallback** y la fórmula quedaba reducida a un solo átomo (`p`).

---

## Definición en `core.lgs`

```text
pattern SI_ELIPTICO si literal literal => imp left=1 right=2
```

### Tabla de índices

| Índice | Token | Rol en la frase *si estudio apruebo* |
|--------|-------|--------------------------------------|
| 0 | `si` | Conector (no entra en la fórmula) |
| 1 | `literal` | Antecedente (*estudio* → lema *estudiar*) |
| 2 | `literal` | Consecuente (*apruebo* → lema *aprobar*) |

Para `imp`, **`left=1`** es el antecedente y **`right=2`** el consecuente → `ImpExpr(antecedente, consecuente)`.

---

## Ejemplo completo

**Entrada:** `si estudio apruebo`

1. Normalización: sin cambios relevantes.
2. Lexer: `[SI("si"), LITERAL("estudio"), LITERAL("apruebo")]`.
3. Patrón `SI_ELIPTICO` coincide.
4. Lemas: *estudiar*, *aprobar*.
5. **Fórmula emitida:** `(p → q)` (símbolos asignados por orden de aparición).
6. **Clasificación típica del motor:** CONTINGENCIA.

**Contraste con la forma explícita:** `si estudio entonces apruebo` usa el patrón `SI_ENTONCES` (cuatro tokens) y produce la **misma** lectura lógica.

---

## Orden respecto a otros patrones

En `core.lgs`, `SI_ENTONCES` va **antes** de `SI_ELIPTICO`. No compiten: longitudes distintas (4 vs 3 tokens). Una frase con *entonces* solo puede coincidir con la forma de cuatro tokens.

---

## Implementación Java

| Pieza | Ubicación |
|-------|-----------|
| Declaración | `src/main/resources/logicscript/core.lgs` |
| Respaldo embebido | `SemanticMapper.patronesPredeterminados()` → entrada `SI_ELIPTICO` |
| Prueba | `LogicScriptRegressionHarness` caso `si_eliptico` |

No requiere un conector nuevo en el lexer: `si ` ya se tokeniza como `SI`. Tras `SI`, si el fragmento literal siguiente tiene **varias palabras** sin conectores internos (`entonces`, ` y `, etc.), el lexer emite **un `LITERAL` por palabra** para permitir el patrón de tres tokens. Tras `entonces`, los complementos multi-palabra (*llevo paraguas*) siguen siendo **un solo** literal.

---

## Limitaciones (v0.2)

- Solo **dos** literales tras `si` (no cubre *si estudio mucho apruebo* con tres proposiciones sin más reglas).
- No distingue *si* temporal de *si* condicional; se asume **condicional lógico** en contexto del curso.
- Frases con **coma** entre dos *si…* (*si estudio apruebo, si no estudio no apruebo*) usan composición de bloques; ver `docs/LogicScript/ComposicionComaSi.md`.

---

## Referencias

- `docs/LogicScript/Pattern.md` — sintaxis general de `pattern`.
- `docs/LogicScript/PruebasRegresion.md` — harness.
