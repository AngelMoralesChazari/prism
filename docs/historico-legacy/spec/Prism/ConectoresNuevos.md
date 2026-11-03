# Conectores nuevos (Fase 1 LogicScript v0.3)

Conectores de lenguaje natural añadidos al lexer y a `core.lgs` para cubrir frases habituales del curso.

---

## Resumen

| Conector | Lectura lógica | Patrón `.lgs` | Ejemplo |
|----------|----------------|---------------|---------|
| *cuando* | Implicación (≈ *si … entonces*) | `CUANDO_ELIPTICO`, `CUANDO_ENTONCES` | *cuando llueve llevo paraguas* → `(p → q)` |
| *solo si* | Condición necesaria: *P solo si Q* → *P → Q* | `SOLO_SI` | *apruebo solo si estudio* → `(p → q)` |
| *a menos que* | Excepción: *P a menos que Q* → *¬Q → P* | `A_MENOS_QUE` | *salgo a menos que llueva* → `(¬p → q)` |

---

## *cuando*

Tras normalizar, el lexer reconoce `cuando ` (con espacio) como `TipoTokenNatural.CUANDO`.

Comportamiento elíptico análogo a *si*:

- *cuando llueve llevo paraguas* → antecedente `llueve`, consecuente `llevo paraguas` (multi-palabra agrupada).
- Con *entonces*: *cuando trabajo entonces descanso* → patrón `CUANDO_ENTONCES`.

Composición por coma: `, cuando ` inicia un nuevo bloque (igual que `, si `).

```text
pattern CUANDO_ELIPTICO cuando literal literal => imp left=1 right=2
pattern CUANDO_ENTONCES cuando literal entonces literal => imp left=1 right=3
```

---

## *solo si*

**No** confundir con *si y solo si* (equivalencia ↔). El lexer prioriza la locución más larga.

- *apruebo solo si estudio* → `(aprobar → estudiar)` → `(p → q)`.
- Dirección: el fragmento **antes** de *solo si* es el antecedente lógico; el **después** es el consecuente.

```text
pattern SOLO_SI literal solo_si literal => imp left=0 right=2
```

---

## *a menos que*

Convención didáctica adoptada:

> *P a menos que Q* ≡ *si no Q entonces P* ≡ *¬Q → P*

Ejemplo: *salgo a menos que llueva* → `(¬llover → salir)`.

En la fórmula emitida, el símbolo de la excepción (*llueva*) recibe `p` por orden de visita en la IR (antecedente negado primero): `(¬p → q)` con `p = llover`, `q = salir`.

Tipo IR: `imp_unless` → `ImpExpr(NegExpr(right), left)`.

```text
pattern A_MENOS_QUE literal a_menos_que literal => imp_unless left=0 right=2
```

---

## Lexer (`NaturalLexer`)

Palabras clave nuevas (orden: más largas primero):

```text
a menos que
solo si
cuando
```

Reglas de partición de literales (v0.3):

1. Tras `SI`, `SIEMPRE_QUE` o `CUANDO`: si hay **3+ palabras** sin conectores, la primera es antecedente y el resto consecuente agrupado.
2. Tras `Y`/`O` **dentro** de un bloque `si …` **sin** `entonces`: cada palabra siguiente es un literal (para *si estudio y practico apruebo*).

---

## Regresión

Casos en `LogicScriptRegressionHarness`:

| id | Entrada |
|----|---------|
| `cuando_eliptico` | cuando llueve llevo paraguas |
| `cuando_entonces` | cuando trabajo entonces descanso |
| `solo_si` | apruebo solo si estudio |
| `a_menos_que` | salgo a menos que llueva |

---

## Referencias

- `docs/LogicScript/PatronesCompuestos.md` — patrones compuestos ampliados
- `docs/LogicScript/Pattern.md` — sintaxis general
- `src/main/resources/logicscript/core.lgs` — datos v0.3
