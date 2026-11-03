# Patrones compuestos (implicación con ∧ u ∨)

Extiende LogicScript para enunciados donde el **antecedente** o el **consecuente** incluye más de un átomo, sin caer en fallback.

**v0.5 (Fase B):** antecedente compuesto elíptico con `∨` y variantes *cuando* / *siempre que*. Ver `docs/LogicScript/PatronesSemanticosFaseB.md`.

---

## Motivación

Frases habituales en clase que antes producían solo `p`:

- *Si estudio y practico entonces apruebo.*
- *Si llueve entonces llevo paraguas o gorra.*

Requieren anidar `∧` o `∨` **dentro** de una implicación, no solo patrones binarios planos.

---

## Patrones en `core.lgs`

| Nombre | Forma LN | Fórmula | Directiva |
|--------|----------|---------|-----------|
| `SI_CONJ_Y_ENTONCES` | si *X* y *Y* entonces *Z* | `((p ∧ q) → r)` | `imp_and left=1 mid=3 right=5` |
| `SI_ENTONCES_DISY_CONS` | si *X* entonces *Y* o *Z* | `(p → (q ∨ r))` | `imp_or left=1 mid=3 right=5` |
| `SI_ENTONCES_CONJ_CONS` | si *X* entonces *Y* y *Z* | `(p → (q ∧ r))` | `imp_and_cons left=1 mid=3 right=5` |
| `SI_DISY_O_ENTONCES` | si *X* o *Y* entonces *Z* | `((p ∨ q) → r)` | `imp_or_ant left=1 mid=3 right=5` |
| `SI_DISY_O_ELIPTICO` | si *X* o *Y* *Z* (sin *entonces*) | `((p ∨ q) → r)` | `imp_or_ant left=1 mid=3 right=4` |
| `SIEMPRE_QUE_CONJ_Y_ELIPTICO` | siempre que *X* y *Y* *Z* | `((p ∧ q) → r)` | `imp_and left=1 mid=3 right=4` |
| `SIEMPRE_QUE_DISY_O_ELIPTICO` | siempre que *X* o *Y* *Z* | `((p ∨ q) → r)` | `imp_or_ant left=1 mid=3 right=4` |
| `CUANDO_CONJ_Y_ELIPTICO` | cuando *X* y *Y* *Z* | `((p ∧ q) → r)` | `imp_and left=1 mid=3 right=4` |
| `CUANDO_DISY_O_ELIPTICO` | cuando *X* o *Y* *Z* | `((p ∨ q) → r)` | `imp_or_ant left=1 mid=3 right=4` |
| `SI_CONJ_Y_ELIPTICO` | si *X* y *Y* *Z* (sin *entonces*) | `((p ∧ q) → r)` | `imp_and left=1 mid=3 right=4` |

Deben ir **antes** de `SI_ENTONCES` en el archivo (más tokens = mayor especificidad).

### Ejemplo `SI_CONJ_Y_ENTONCES`

```text
pattern SI_CONJ_Y_ENTONCES si literal y literal entonces literal => imp_and left=1 mid=3 right=5
```

| Índice | Token | Ejemplo *si estudio y practico entonces apruebo* |
|--------|-------|--------------------------------------------------|
| 0 | `si` | conector |
| 1 | `literal` | estudio → estudiar |
| 2 | `y` | conjunción |
| 3 | `literal` | practico → practicar |
| 4 | `entonces` | conector |
| 5 | `literal` | apruebo → aprobar |

IR: `ImpExpr(AndExpr(estudiar, practicar), aprobar)` → `((p ∧ q) → r)`.

### Ejemplo `SI_ENTONCES_DISY_CONS`

```text
pattern SI_ENTONCES_DISY_CONS si literal entonces literal o literal => imp_or left=1 mid=3 right=5
```

Entrada: *si llueve entonces llevo paraguas o gorra*

- El lexer parte en `llueve` | `llevo paraguas` | `gorra` (detecta ` o ` tras `entonces`).
- Fórmula: `(p → (q ∨ r))` con *llover*, *llevar paraguas*, *gorra*.

---

## Tipos IR nuevos en `.lgs`

Tras `=>`:

| Token | IR generada |
|-------|-------------|
| `imp_and` | `(left ∧ mid) → right` |
| `imp_or` | `left → (mid ∨ right)` |
| `imp_or_ant` | `(left ∨ mid) → right` |
| `imp_and_cons` | `left → (mid ∧ right)` |
| `imp_unless` | `¬right → left` (*a menos que*) |

Requiere tres índices: `left=`, `mid=`, `right=` (slots `literal`).

Tipos previos (`imp`, `and`, `or`, `equiv`) siguen usando solo `left=` y `right=`.

---

## Implementación Java

| Pieza | Ubicación |
|-------|-----------|
| Enum `IMP_AND`, `IMP_OR` | `TipoSalidaIrPatron.java` |
| Campo `indiceMedio` | `PatronSemanticoLgs.java` |
| Parseo `mid=` | `LgsCargador.java` |
| Construcción IR anidada | `SemanticMapper.java` |
| Respaldo embebido | `SemanticMapper.patronesPredeterminados()` |
| Datos | `src/main/resources/logicscript/core.lgs` |
| Regresión | `LogicScriptRegressionHarness` casos `si_conj_y_entonces`, `si_entonces_disy_cons` |

---

## Cómo probar

```powershell
.\compile.ps1 -JdkHome "C:\Program Files\Java\jdk-25.0.2"
java -cp out tautoteacher2.logicscript.LogicScriptCli "si estudio y practico entonces apruebo"
java -cp out tautoteacher2.logicscript.LogicScriptRegressionHarness
```

En la app: pestaña **Lenguaje natural** → procesar → tarjeta **Explicación** (tabla de verdad con 3 variables).

---

## Limitaciones (v0.5)

- No cubre *llueve y estudio o descanso* (sin `si`) con anidación compleja.
- Consecuente compuesto elíptico sin *entonces* (p. ej. *si llueve salgo o me quedo*) fuera de alcance.
- Antecedente con más de dos átomos unidos por `y`/`o`.
- Tras editar `core.lgs`, **recompilar** para copiar recursos a `out/logicscript/`.

---

## Referencias

- `docs/LogicScript/PatronesSemanticosFaseB.md` — Fase B v0.5
- `docs/LogicScript/PatronSiElliptico.md` — implicación sin *entonces*
- `docs/LogicScript/PruebasRegresion.md` — harness
