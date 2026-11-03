# Notas — identificadores, `let`, `nl` y ejemplos en la documentación

**Versión:** 1.0  
**Relacionado con:** `VISION-Y-DEFINICION-GENERAL.md` §5.4 y §5.6

Este documento aclara **qué forma parte del lenguaje Prism v1.0** y **qué son solo convenciones de ejemplo** en los fragmentos de código de la documentación.

---

## 1. Regla general

| En el código de ejemplo | ¿Está en el lenguaje? |
|-------------------------|------------------------|
| Palabras en **negrita** de la tabla §2 (palabras reservadas) | **Sí** — sintaxis oficial |
| Nombres como `f1`, `phi`, `linea1`, `rain_logic` | **No** — los elige el programador |
| Texto entre comillas `"si llueve..."` | **Dato** — contenido libre (español, inglés, etc.) |
| Comentarios `# ...` | **Sí** — línea ignorada por el compilador |

Si aparece en un ejemplo y **no** está en la lista de palabras reservadas ni en las reglas de §5.6, **no** asumir que existe: puede ser solo ilustración hasta que se documente aquí o en la EBNF.

---

## 2. ¿Por qué `let`?

`let` **sí está definido** en Prism v1.0.

### Definición

```text
let <identificador> = <expresión>
```

- **Propósito:** crear un **enlace local** (binding) al resultado de una expresión, para reutilizarlo después (p. ej. en `vertauto`, otra `formula`, otro `translate`).
- **Ámbito:** el módulo (archivo `.pr`) donde se declara; reglas de bloque anidadas se precisarán en EBNF.
- **Inmutabilidad:** en v1.0 cada `let` se asigna **una vez**; redeclarar el mismo nombre en el mismo ámbito es **error de compilación**.
- **Tipo del valor:** depende de la expresión (véase §4).

### Analogía (solo pedagógica, no es Prolog ni Java)

Similar a `let` en Rust/ML o a una variable local de solo lectura: **nombre = valor calculado**.

### Ejemplos válidos

```ls
let f_implicacion = translate(linea_lluvia)
let f_directa     = translate("si llueve llevo paraguas")
let f_ingles      = translate("if it rains I carry an umbrella", module: core_en)

vertauto f_implicacion
```

Los nombres `f_implicacion`, `f_directa`, `f_ingles` son **elección del programador**; podrían ser `a`, `resultado`, `x`, etc.

---

## 3. ¿Por qué `f1`, `phi`, `psi`?

**No tienen significado especial en el lenguaje.** Son **identificadores de ejemplo** en la documentación.

| Nombre en docs | Intención del ejemplo | ¿Obligatorio usar ese nombre? |
|----------------|----------------------|------------------------------|
| `f1` | “primera fórmula del ejercicio” | No |
| `f2`, `g`, `h` | otras fórmulas en el mismo archivo | No |
| `phi` (φ) | convención matemática de “fórmula” en textos de lógica | No |
| `psi` (ψ) | segunda fórmula en un texto académico | No |
| `linea1` | primera línea de NL guardada en variable | No |

El compilador **no** trata `phi` distinto de `f1`. Solo importa que el identificador:

1. Cumpla la gramática de nombres (§5).
2. Haya sido declarado antes con `let`, `formula`, `nl`, etc.
3. Se use con el **tipo** correcto (`vertauto` espera una fórmula, no un texto NL crudo).

### Convención recomendada (opcional, no del lenguaje)

- `f_*` o `formula_*` — fórmulas.
- `linea_*` o `texto_*` — cadenas NL.
- `p`, `q`, `r` — átomos proposicionales en expresiones `impl`, `or`, etc.

---

## 4. Tipos de valores (v1.0)

Prism v1.0 es declarativo; los tipos son **conceptuales** para el programador y el compilador:

| Tipo | Cómo se obtiene | Uso típico |
|------|-----------------|------------|
| **Text** | literal `"..."` o `nl id = "..."` | Entrada de `translate` |
| **Formula** | `formula id = ...`, `let id = translate(...)`, expresión entre paréntesis | `vertauto`, `formula` compuesta |
| **Atom** | `atom p = "..."` | Símbolos `p`, `q` en expresiones lógicas |
| **ModuleRef** | nombre en `use core` | Solo en `module:` de `translate` |

Un error de compilación ocurre si pasas un **Text** donde se espera una **Formula** (p. ej. `vertauto linea1` sin `translate`).

---

## 5. Gramática de identificadores

```text
identificador ::= letra ( letra | dígito | '_' )*
letra         ::= 'a'..'z' | 'A'..'Z'
```

- **Case-sensitive:** `f` y `F` son distintos.
- **No** pueden ser palabra reservada (`let`, `atom`, `module`, …).
- Longitud máxima: por definir en implementación (sugerido ≥ 64 caracteres).

---

## 6. Palabras reservadas y constructos de declaración (v1.0)

Todo lo siguiente **está definido** en el lenguaje (lista viva hasta congelar EBNF):

### 6.1 Estructura del programa

| Constructo | Sintaxis | Rol |
|------------|----------|-----|
| `module` | `module <identificador>` | Nombre del programa/archivo |
| `use` | `use <identificador>` | Importa módulo `.lgs` (p. ej. `core`) |

### 6.2 Declaraciones (bindings)

| Constructo | Sintaxis | Tipo asignado |
|------------|----------|---------------|
| `atom` | `atom <id> = <string>` | Átomo proposicional con significado NL |
| `formula` | `formula <id> = <expr_formula>` | Fórmula simbólica |
| `nl` | `nl <id> = <string>` | Texto en lenguaje natural (dato) |
| `let` | `let <id> = <expresión>` | Resultado de la expresión (típ. `Formula`) |

### 6.3 Expresiones y llamadas

| Constructo | Sintaxis | Notas |
|------------|----------|-------|
| `translate` | `translate(<expr_texto>)` | Usa módulos del `use` |
| `translate` + módulo | `translate(<expr_texto>, module: <id>)` | Fuerza un `.lgs` concreto |
| `vertauto` corto | `vertauto <expr_formula>` | Salida mínima |
| `vertauto` bloque | `vertauto <id> { steps table verdict }` | Salida pedagógica parcial o total |

### 6.4 Reglas declarativas

| Constructo | Sintaxis | Rol |
|------------|----------|-----|
| `rule` | `rule <id> { ... }` | Bloque declarativo |
| `run` | `run <id>` | Ejecuta la regla |
| `when` / `and` / `then` | dentro de `rule` | Condiciones y conclusiones |
| `means` | `atom p means "..."` | Enlace en regla |
| `is` | `formula f is p impl q` | Definición en regla |

### 6.5 Comentarios

```text
# comentario hasta fin de línea
```

### 6.6 Conectores lógicos (en expresiones)

`not`, `and`, `or`, `impl`, `iff` — **operadores**, no declaraciones.

---

## 7. Desglose del ejemplo que generó la duda

```ls
nl linea1 = "si llueve llevo paraguas"
let f1 = translate(linea1)

let phi = translate("si llueve llevo paraguas")
let psi = translate("if it rains I carry an umbrella", module: core_en)
```

| Fragmento | ¿Definido? | Explicación |
|-----------|------------|-------------|
| `nl` | **Sí** | Declara `linea1` como texto NL |
| `linea1` | Identificador libre | Nombre elegido por el programador |
| `"si llueve..."` | Dato | Español; no es sintaxis |
| `let` | **Sí** | Asigna resultado de `translate` a `f1` |
| `f1` | Identificador libre | Podría ser `implicacion_lluvia` |
| `translate(...)` | **Sí** | Expresión del runtime |
| `phi`, `psi` | Identificadores libres | Convención matemática en el doc; sin significado interno |
| `module: core_en` | **Sí** | Argumento nombrado; `core_en` debe existir vía `use core_en` |

Las tres líneas `let` hacen **lo mismo conceptualmente** (obtener una `Formula` desde NL); el ejemplo muestra **tres estilos**:

1. NL en variable intermedia (`nl` + `translate(linea1)`).
2. NL inline en `translate("...")`.
3. NL en otro idioma con módulo léxico explícito.

---

## 8. Qué NO está definido aún (solo ideas en documentación)

Hasta que aparezcan en EBNF o en este documento como §6:

- Asignación mutable (`set`, `:=`, reasignación).
- Tipos anotados (`let f: Formula = ...`).
- Funciones definidas por el usuario (`fn`, `def`).
- `import` de otros `.pr` (solo `use` de `.lgs` en v1.0).
- Operador `module:` en otros comandos además de `translate`.

---

## 9. Changelog de esta nota

| Fecha | Cambio |
|-------|--------|
| 2026-06 | Creación: aclaración `let`, `nl`, identificadores de ejemplo, tabla de palabras reservadas v1.0 |
