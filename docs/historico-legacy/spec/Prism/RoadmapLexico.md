# Roadmap del léxico LogicScript

Plan acordado para dejar de inflar `core.lgs` con conjugaciones y ampliar cobertura del curso.

---

## Visión: dos capas

```text
┌─────────────────────────────────────────┐
│  PATRONES SEMÁNTICOS (pattern)          │  Estructura: si X Y → (X → Y)
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│  CAPA LÉXICA                            │  Forma: estudio → estudiar
│  1. lemma (excepciones)                 │
│  2. NormalizadorMorfologico (reglas)    │
│  3. identidad (fallback)                │
└─────────────────────────────────────────┘
```

Los `pattern` **no** reemplazan los `lemma` ni la morfología: resuelven problemas distintos.

---

## Fase A — Normalizador morfológico

**Estado:** implementado en v0.4.

**Entregables:**

- `NormalizadorMorfologico.java`
- Integración en `BaseConocimiento`
- `core.lgs` reducido (lemma solo irregulares)
- Casos `morph_*` en regresión
- `docs/LogicScript/NormalizadorMorfologico.md`

**Criterio de éxito:** frases con verbos regulares -ar y casos como *si duermo descanso* sin bloques `lemma` de dormir.

---

## Fase B — Más patrones semánticos

**Estado:** implementado en v0.5.

**Objetivo:** frases del curso que fallan por **estructura**, no por palabras.

**Entregables:**

- Cinco patrones elípticos compuestos en `core.lgs` (+ familia `SI_CONJ_Y_ELIPTICO` ya existente)
- Ajuste `NaturalLexer` para `cuando` / `siempre que` y disyunción `o` en antecedente elíptico
- Respaldo embebido en `SemanticMapper`
- Casos `si_disy_o_eliptico`, `siempre_que_*_eliptico`, `cuando_*_eliptico`, `composicion_cuando_si`
- `docs/LogicScript/PatronesSemanticosFaseB.md`

**Criterio de éxito:** harness 37 casos en verde; *si llueve o solea salgo* → `((p ∨ q) → r)`.

---

## Fase C — `lexrule` en `.lgs`

**Estado:** implementado en v0.6.

**Objetivo:** reglas morfológicas editables sin recompilar Java (solo copiar recursos).

**Entregables:**

- Directiva `lexrule` (excluir, sufijo, heurística primera_persona)
- Parser en `LgsCargador`, modelos `ReglaMorfologicaLgs` / `ConfiguracionMorfologiaLgs`
- `NormalizadorMorfologico` configurable desde `core.lgs`
- Bloque morfológico migrado en `core.lgs` v0.6
- Pruebas `lexrule_sintaxis`, `lexrule_carga` en harness
- `docs/LogicScript/Lexrule.md`

**Criterio de éxito:** mismos casos `morph_*` en verde; reglas editables en `.lgs`.

---

## Mantenimiento de `core.lgs` tras Fase A

| Añadir `lemma` cuando… | No añadir cuando… |
|------------------------|-------------------|
| Verbo irregular (*apruebo*, *salgo*) | Presente -ar regular (*estudio*, *estudian*) |
| Cambio vocálico (*duerme* si falla) | *practico*, *trabajo*, *descanso* |
| Sustantivo fijo (*gorra*) | Infinitivo ya reconocible |
| Locución (*solea* → *hacer_sol*) | |

---

## Referencias

- `NormalizadorMorfologico.md` — detalle técnico Fase A
- `Lexrule.md` — detalle técnico Fase C
- `Lemma.md` — directiva `lemma`
- `Pattern.md` — directiva `pattern`
