# Referencia rápida — sintaxis DSL `.lgs` v0.6

Resumen consolidado para el proyecto independiente. Especificación completa en `documentacion-original/LogicScript/`.

---

## Reglas generales del archivo

| Regla | Valor |
|-------|-------|
| Codificación | UTF-8 |
| Comentarios | Líneas que empiezan con `#` |
| Vacías | Ignoradas |
| Versión | `version 0.6` (informativa) |
| Orden de evaluación | `pattern`: primera coincidencia gana; `lexrule`: primera regla que aplica gana |

---

## Directiva `lemma`

**Propósito:** excepciones léxicas (irregulares, sustantivos, locuciones).

```text
lemma <forma> -> <lema>
```

Ejemplos:

```text
lemma apruebo -> aprobar
lemma voy -> ir
lemma solea -> hacer_sol
```

**Prioridad:** máxima en canonización (gana sobre `lexrule`).

---

## Directiva `lexrule`

**Propósito:** morfología declarativa (sufijos, exclusiones, heurística -o).

### Excluir (no flexionar)

```text
lexrule excluir gorra sombrero paraguas calor frio sol ...
```

### Sufijo → infinitivo

```text
lexrule sufijo <sufijo> infinitivo ar|er|ir
```

Ejemplo: `lexrule sufijo an infinitivo ar` → *estudian* → *estudiar*

### Primera persona -o

```text
lexrule sufijo o heuristica primera_persona
```

**Orden:** sufijos más largos primero; heurística `-o` al final del bloque.

---

## Directiva `pattern`

**Propósito:** estructura lógica — tokens → conectiva IR.

### Formas básicas

```text
pattern <NOMBRE> <tipo_token> ... => <tipo_ir> left=<i> right=<j>
pattern <NOMBRE> ... => imp_and left=<i> mid=<k> right=<j>
pattern <NOMBRE> ... => imp_or left=<i> mid=<k> right=<j>
pattern <NOMBRE> ... => imp_or_ant left=<i> mid=<k> right=<j>
pattern <NOMBRE> ... => imp_and_cons left=<i> mid=<k> right=<j>
pattern <NOMBRE> ... => imp_unless left=<i> right=<j>
```

### Tokens de forma permitidos

| En `.lgs` | Significado |
|-----------|-------------|
| `si` | Conector condicional |
| `entonces` | Consecuente explícito |
| `en_caso_de_que` | Condicional |
| `siempre_que` | Condicional |
| `cuando` | Condicional |
| `solo_si` | Solo si |
| `a_menos_que` | A menos que |
| `y` | Conjunción |
| `o` | Disyunción |
| `si_y_solo_si` | Equivalencia (lexer) |
| `literal` | Fragmento de proposición |

### Tipos IR (`tipo_ir`)

| Valor | IR Java | Lógica |
|-------|---------|--------|
| `imp` | ImpExpr | Implicación → |
| `and` | AndExpr | Conjunción ∧ |
| `or` | OrExpr | Disyunción ∨ |
| `equiv` | EquivExpr | Equivalencia ↔ |
| `imp_and` | ImpExpr(AndExpr(...), ...) | (X∧Y)→Z |
| `imp_or` | ImpExpr(..., OrExpr(...)) | X→(Y∨Z) |
| `imp_or_ant` | ImpExpr(OrExpr(...), ...) | (X∨Y)→Z |
| `imp_and_cons` | ImpExpr(..., AndExpr(...)) | X→(Y∧Z) |
| `imp_unless` | ImpExpr con negación | A menos que |

### Índices `left`, `right`, `mid`

- **Base 0** sobre la lista de tokens de la forma.
- Solo posiciones de tipo `literal` pueden ser operandos.
- En `imp`: `left` = antecedente, `right` = consecuente.

Ejemplo clásico:

```text
pattern SI_ENTONCES si literal entonces literal => imp left=1 right=3
```

Frase: `si llueve entonces llevo paraguas`

| Índice | Token |
|--------|-------|
| 0 | si |
| 1 | llueve ← left |
| 2 | entonces |
| 3 | llevo paraguas ← right |

---

## Patrones incluidos en `core.lgs` v0.6

| Nombre | Idea |
|--------|------|
| SI_ENTONCES | si X entonces Y |
| SI_ELIPTICO | si X Y (sin entonces) |
| SI_CONJ_Y_ENTONCES | si X y Y entonces Z |
| SI_ENTONCES_DISY_CONS | si X entonces Y o Z |
| SI_DISY_O_ENTONCES | si X o Y entonces Z |
| SI_DISY_O_ELIPTICO | si X o Y Z |
| SIEMPRE_QUE_* / CUANDO_* | Variantes elípticas y con entonces |
| SOLO_SI, A_MENOS_QUE | Condicionales inversas |
| EQUIVALENCIA | X si y solo si Y |
| CONJUNCION / DISYUNCION | X y Y / X o Y |
| EN_CASO_DE_QUE | en caso de que X Y |
| CONSECUENTE_SI_ANTECEDENTE | Y si X |

Lista completa: `ejemplos/core.lgs`.

---

## IR (representación intermedia)

```text
AtomExpr(texto, negada?)
NegExpr(interior)
AndExpr(izq, der)
OrExpr(izq, der)
ImpExpr(antecedente, consecuente)
EquivExpr(izq, der)
```

Emisión: `¬` `∧` `∨` `→` `↔` y símbolos proposicionales `p`, `q`, `r`…

---

## Pipeline (no es sintaxis .lgs, es runtime)

```text
1. NormalizadorTexto
2. NaturalLexer → List<TokenNatural>
3. SemanticMapper (pattern) → LogicExpr
4. BaseConocimiento (lemma + lexrule) en cada literal
5. EmitidorFormula → String
```

Composición multi-cláusula: coma + `si` → varios bloques unidos con ∧.

---

## Directivas futuras (no implementadas)

- `synonym` — sinónimos de conectores
- `connector` — nuevos conectores sin tocar lexer Java
- `import` — varios archivos `.lgs`
- Negación declarativa en archivo

---

## Documentación detallada

| Tema | Archivo |
|------|---------|
| Lemma | `documentacion-original/LogicScript/Lemma.md` |
| Lexrule | `documentacion-original/LogicScript/Lexrule.md` |
| Pattern | `documentacion-original/LogicScript/Pattern.md` |
| Formato | `documentacion-original/LogicScript/FormatoLGS.md` |
| Patrones compuestos | `documentacion-original/LogicScript/PatronesCompuestos.md` |
| Formal BNF | `documentacion-original/LogicScript/PaperFormalLogicScript.md` |
