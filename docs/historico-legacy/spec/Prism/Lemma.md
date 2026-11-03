# Lema en LogicScript

Este documento explica de forma completa qué significa `lemma` en LogicScript, por qué existe y cómo se usa en el pipeline actual.

## Definición corta

Un **lemma** (lema) es la forma canónica de una palabra o expresión.

En LogicScript, `lemma` sirve para que distintas variantes del español representen la **misma proposición lógica**.

Ejemplo:

- `llueve` -> `llover`
- `llueva` -> `llover`

Sin esa unificación, el sistema podría crear dos símbolos distintos para la misma idea.

## Problema que resuelve

El español tiene variaciones morfológicas (tiempo, modo, persona). Si no se normalizan:

- `si llueve entonces llevo paraguas`
- `si llueva entonces llevo paraguas`

podrían generar proposiciones diferentes, aunque conceptualmente expresan el mismo predicado principal (`llover`).

Con `lemma`, ambas rutas convergen al mismo fragmento canónico antes de asignar `p`, `q`, etc.

## Sintaxis actual en `.lgs` (v0.1)

Formato:

```text
lemma <forma> -> <lema>
```

Ejemplos reales en `core.lgs`:

```text
lemma llueve -> llover
lemma llueva -> llover
lemma llevo -> llevar
```

## Dónde se guarda y cómo se carga

- Archivo principal: `src/main/resources/logicscript/core.lgs`
- Recurso en runtime: `classpath:logicscript/core.lgs`
- Cargador: `tautoteacher2.nlp.lexicon.LgsCargador`

Si el recurso no se encuentra, `BaseConocimiento` usa un respaldo embebido en código.

## Flujo técnico (paso a paso, v0.4)

1. `NormalizadorTexto` limpia y normaliza el enunciado.
2. `NaturalLexer` + `SemanticMapper` aplican **patrones semánticos** (`pattern`).
3. `BaseConocimiento.canonicalizarFragmento(...)` por cada palabra:
   - si hay `lemma` en `.lgs` → usarlo;
   - si no → reglas `lexrule` vía `NormalizadorMorfologico` (Fase C);
   - si no aplica regla → palabra literal.
4. Se crea `AtomExpr` con texto canónico.
5. `RegistroProposiciones` asigna símbolo estable (`p`, `q`, `r`).

Ver `docs/LogicScript/NormalizadorMorfologico.md` para la capa morfológica.

## Relación con patrones semánticos

| Mecanismo | Pregunta que responde |
|-----------|------------------------|
| `pattern SI_ELIPTICO` | ¿*si estudio apruebo* es implicación? |
| `lemma` / morfología | ¿`estudio` y `estudian` son la misma proposición? (`lexrule` + `lemma`) |

No sustituyas conjugaciones por más `pattern`; usa morfología o `lemma` puntual.

## Qué NO hace todavía

- No resuelve ambigüedad semántica profunda.
- No maneja contexto discursivo (anáforas, elipsis complejas).
- No hace análisis morfológico completo de todo el español.
- No aplica reglas multi-palabra avanzadas como unidad léxica única.

## Buenas prácticas para agregar lemas (v0.4)

- **Priorizar morfología** para verbos regulares -ar (-o, -as, -a, -an): no dupliques en `lemma`.
- Reservar `lemma` para **irregulares** (*apruebo*, *salgo*, *voy*), **sustantivos** (*gorra*) y **locuciones** (*solea* → *hacer_sol*).
- Mantener `<forma>` y `<lema>` en minúsculas.
- Usar formas consistentes con la salida de `NormalizadorTexto`.

## Ejemplo de impacto práctico

Entrada A:

```text
si llueve entonces llevo paraguas
```

Entrada B:

```text
si llueva entonces llevo paraguas
```

Con lemas correctos, ambas terminan en la misma estructura base:

- antecedente canónico: `llover`
- consecuente canónico: `llevar paraguas`

## Relación con la evolución del pseudo lenguaje

`lemma` fue la primera directiva declarativa de LogicScript en archivo externo.
Ya coexisten en el mismo `.lgs` las directivas **`pattern`** (patrones semánticos LN → IR); véase `docs/LogicScript/Pattern.md`.

Siguientes ampliaciones previstas:

- `connector`
- `synonym`

y acercarse a un DSL más completo con extensión propia.
