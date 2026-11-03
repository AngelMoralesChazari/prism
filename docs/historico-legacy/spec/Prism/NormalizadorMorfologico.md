# Normalizador morfológico (Fase A del léxico LogicScript)

## Resumen

El **normalizador morfológico** reduce la necesidad de declarar decenas de `lemma` por cada conjugación verbal. Aplica **reglas de sufijos** del español (heurísticas) para aproximar un **infinitivo** a partir de formas flexionadas.

No sustituye a los **patrones semánticos** (`pattern SI_ELIPTICO`, etc.): esas reglas definen la **estructura** de la frase. El normalizador solo unifica **cómo se nombra** cada proposición (`estudio` → `estudiar`).

---

## Justificación del diseño

### Problema anterior

En `core.lgs` v0.3 había bloques como:

```text
lemma duermo -> dormir
lemma duermes -> dormir
lemma duerme -> dormir
lemma duermen -> dormir
```

Cada verbo nuevo del curso exigía **5–10 líneas** de conjugaciones, aunque el patrón `SI_ELIPTICO` ya sabía interpretar *si X Y*.

### Objetivo Fase A

| Capa | Responsabilidad | Dónde |
|------|-----------------|-------|
| **Semántica** | *si estudio apruebo* → `(p → q)` | `pattern` + `SemanticMapper` |
| **Léxico** | `estudio` → `estudiar`, `duermo` → `dormir` | `lemma` + **NormalizadorMorfologico** |

### Por qué `lexrule` en `.lgs` (Fase C) — hecho en v0.6

Las reglas morfológicas viven ahora en `core.lgs` con la directiva `lexrule`. Java conserva el **motor** de aplicación y un **respaldo embebido** si el archivo no declara reglas. Ver `docs/LogicScript/Lexrule.md`.

---

## Orden de prioridad en `BaseConocimiento`

Para cada palabra de un literal:

```text
1. ¿Existe lemma en core.lgs?     → usar lema (apruebo → aprobar, salgo → salir)
2. ¿Regla morfológica aplica?     → infinitivo aproximado (estudio → estudiar)
3. Si no                        → palabra tal cual (identidad)
```

Código: `BaseConocimiento.canonicalizarPalabra()`.

---

## Reglas implementadas (v0.4)

Clase: `tautoteacher2.nlp.lexicon.NormalizadorMorfologico`

### Infinitivos

Si la palabra ya termina en `-ar`, `-er`, `-ir` (longitud ≥ 4) o es `ir`, no se modifica.

### Lista de no-verbo

Sustantivos del dominio que **no** deben recibir sufijos verbales:

`gorra`, `paraguas`, `calor`, `frio`, `sol`, etc.

Evita errores del tipo `gorra` → `gorrar`.

### Sufijos flexivos (orden: más largo primero)

| Sufijos (ejemplos) | Infinitivo generado |
|--------------------|---------------------|
| `-amos`, `-ais`, `-an`, `-as`, `-a`, … | raíz + `ar` |
| `-emos`, `-eis` | raíz + `er` |
| `-imos` | raíz + `ir` |
| `-o` (1.ª persona) | heurística por raíz (ver abajo) |

### Heurística de `-o` (1.ª persona presente)

- Raíz termina en **vocal** → `raíz + ar` (*estudio* → *estudiar*).
- Raíz termina en **consonante**:
  - Si termina en `rm`, `mm`, `rc`, `rt` → `raíz + ir` (*duermo* → *dormir*).
  - En otro caso → `raíz + ar` (*llego* → *llegar*, *descanso* → *descansar*).

---

## Qué cubre y qué no (límites honestos)

### Funciona bien sin `lemma`

- Verbos regulares **-ar**: *estudio*, *estudian*, *practico*, *trabajo*, *descanso*.
- Algunos **-ir** en 1.ª persona: *duermo* → *dormir*.
- *llego* → *llegar*.

### Sigue necesitando `lemma`

| Caso | Ejemplo | Motivo |
|------|---------|--------|
| Irregulares | *apruebo*, *salgo*, *voy* | Cambio de raíz |
| Cambio vocálico | *duerme*, *duermes* | *duerm* + *ir* ≠ *dormir* |
| Lluvia | *llueve*, *llueva* | Raíz *llov-* |
| Locuciones | *solea* → *hacer_sol* | No es infinitivo simple |
| Sustantivos | *gorra*, *paraguas* | Protegidos o lemma fijo |

### Fallback identidad

Si ninguna regla aplica, la palabra se usa **como está**. La frase **sigue traduciéndose** si el patrón semántico coincide; solo que `p` podría llamarse `duerme` en lugar de `dormir`.

---

## Impacto en `core.lgs` v0.4

- Eliminadas decenas de `lemma` regulares (-ar).
- Conservados `lemma` para irregulares, clima y sustantivos.
- Comentarios indican qué familias cubre la morfología.

---

## Flujo completo (ejemplo)

**Entrada:** `si duermo descanso`

```text
NormalizadorTexto  → si duermo descanso
NaturalLexer       → SI | duermo | descanso
SemanticMapper     → SI_ELIPTICO
canonicalizar      → dormir | descansar   (sin lemma de dormir)
EmitidorFormula    → (p → q)
Proposiciones      → p = dormir, q = descansar
```

**Entrada:** `si estudio apruebo`

```text
canonicalizar      → estudiar | aprobar   (estudiar por morfo; aprobar por lemma apruebo)
```

---

## Pruebas de regresión

En `LogicScriptRegressionHarness`:

| id | Entrada | Comprueba |
|----|---------|-----------|
| `morph_si_duermo` | si duermo descanso | dormir sin lemma |
| `morph_estudian` | si estudian aprueban | plural -ar |
| `morph_practico` | practico si y solo si estudio | practicar + estudiar |
| `morph_llego` | si llego descanso | llegar por -o |

Ejecutar:

```powershell
java -cp out tautoteacher2.logicscript.LogicScriptRegressionHarness
```

---

## Roadmap léxico

| Fase | Contenido | Estado |
|------|-----------|--------|
| **A** | NormalizadorMorfologico + lemma mínimos | **Hecho** (v0.4) |
| **B** | Más patrones semánticos del curso | **Hecho** (v0.5) |
| **C** | Directiva `lexrule` en `.lgs` | **Hecho** (v0.6) |

Ver `docs/LogicScript/Lexrule.md` y `RoadmapLexico.md`.

---

## Referencias

- `Lemma.md` — cuándo seguir usando `lemma`
- `Pattern.md` — patrones semánticos (capa distinta)
- `BaseConocimiento.java` — integración
- `NormalizadorMorfologico.java` — reglas
