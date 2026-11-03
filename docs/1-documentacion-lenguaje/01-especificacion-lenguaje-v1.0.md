# Prism — Especificación del lenguaje `.pr` v1.0

**Versión:** 1.0 (borrador congelable)  
**Fecha:** junio 2026  
**Relacionado con:** `VISION-Y-DEFINICION-GENERAL.md`, `NOTAS-IDENTIFICADORES-LET-Y-EJEMPLOS.md`  
**Estado de implementación:** intérprete mínimo `.pr` operativo (`prism.pr`, `prc run`); ver `IMPLEMENTACION-INTERPRETE-LS.md`. `translate` y `vertauto` integrados en runtime Java.

---

## 1. Alcance

Este documento define la **gramática** y **semántica superficial** de los programas Prism (extensión `.pr`).

No redefine el sublenguaje de datos `.lgs` (véase `docs/SINTAXIS-DSL-RESUMEN.md`).

---

## 2. Lexical (tokens)

### 2.1 Caracteres ignorables

- Espacios: espacio, tabulación.
- Saltos de línea: `\n`, `\r\n` (separadores de sentencia).
- Comentarios: desde `#` hasta fin de línea.

### 2.2 Literales de cadena

```ebnf
string_literal ::= '"' { string_char } '"'
string_char    ::= /* cualquier carácter excepto " y salto de línea, o escape \" */
```

Codificación del archivo fuente: **UTF-8**.

### 2.3 Identificadores

```ebnf
identifier ::= letter { letter | digit | '_' }
letter     ::= 'A'..'Z' | 'a'..'z'
digit      ::= '0'..'9'
```

- **Case-sensitive:** `ejercicio1` y `Ejercicio1` son nombres distintos.
- **Primer carácter:** debe ser **letra** (`a`–`z`, `A`–`Z`). No puede ser dígito ni `_`.
- **Resto:** letras, dígitos o `_`.
- **Prohibido:** coincidir con palabra reservada (§2.4).
- **Longitud máxima:** por definir en implementación (mínimo recomendado: 64 caracteres).

#### Ejemplos de identificadores válidos e inválidos

| Identificador | ¿Válido? | Motivo |
|---------------|----------|--------|
| `ejercicio1` | Sí | Letra inicial + dígitos |
| `Ejercicio1` | Sí | Mayúsculas permitidas |
| `ejercicio_1` | Sí | Guión bajo interno |
| `p`, `q`, `f`, `implicacion` | Sí | Uso típico en fórmulas y `let` |
| `_privado` | **No** | No puede empezar con `_` |
| `1ejercicio` | **No** | Empieza con dígito |
| `ejercicio-1` | **No** | Guión `-` no permitido |
| `mi programa` | **No** | Espacios no permitidos |
| `let`, `atom`, `module` | **No** | Palabra reservada |

Estas reglas aplican a: nombre de `module`, variables `let`/`nl`/`formula`/`atom`, nombres en `use`, etc.

### 2.4 Archivos `.pr` y nombres en disco

El **nombre del archivo** en el sistema operativo y el **identificador `module`** en el código son cosas relacionadas pero **no idénticas**.

| Concepto | Qué es | Ejemplo |
|---------|--------|---------|
| **Archivo** | Nombre en disco + extensión `.pr` | `ejercicio1.pr` |
| **`module`** | Nombre lógico del programa dentro del lenguaje | `module ejercicio1` |

#### Convención obligatoria (v1.0)

1. Todo programa `.pr` **debe** comenzar con exactamente una línea `module <identificador>`.
2. **Recomendado:** el identificador del `module` coincide con el nombre del archivo **sin** `.pr`.
3. El compilador `lsc` (cuando exista) **advertirá** si `ejercicio1.pr` declara `module otro_nombre`.

#### Reglas para nombres de archivo (disco)

Recomendación del proyecto (no gramática del lenguaje, pero buena práctica):

| Regla | Recomendado | Evitar |
|-------|-------------|--------|
| Extensión | `.pr` | `.txt`, sin extensión |
| Mayúsculas | minúsculas (`ejercicio1.pr`) | mezcla confusa salvo convención del curso |
| Separadores | `_` (`mi_ejercicio.pr`) | espacios, `-` en nombres muy largos |
| Primer carácter | letra | empezar por número (`1ejercicio.pr`) |
| Caracteres especiales | `a-z`, `0-9`, `_` | `ñ` en nombre de archivo (usar `n`); símbolos `@#` |

Un archivo `1ejercicio.pr` puede existir en Windows, pero **`module 1ejercicio` es inválido** en Prism (§2.3).

#### Estructura mínima de un archivo `.pr`

```ls
module ejercicio1

use core

# declaraciones y sentencias (atom, nl, let, formula, vertauto, …)
```

Orden canónico recomendado (método en papel):

1. `module` (identificador del programa = nombre de archivo)
2. `use` (cero o más importaciones, e.g. `use core`)
3. `nl` (enunciado del problema en lenguaje natural)
4. `atom` (definición de proposiciones atómicas $p, q, \dots$)
5. `let` / `formula` (formalización deductiva a fórmula proposicional)
6. `vertauto` (evaluación formal de tabla de verdad y dictamen)

> [!NOTE] Flexibilidad Declarativa (Compilador en Dos Pasadas - Two-Pass)
> Aunque el orden canónico recomendado es el método en papel (`nl` → `atom` → `let` → `vertauto`), el compilador de Prism procesa los programas en **dos pasadas** (*Two-Pass Symbol Collection*). Esto otorga plena libertad al programador para organizar sus declaraciones (por ejemplo, declarando los `atom` arriba como variables globales, o abajo como glosario) sin provocar errores de referencia hacia adelante (*forward references*).

> [!TIP] Diagnóstico de Coherencia Semántica y Sugerencias Didácticas ("Did you mean...?")
> Al utilizar `let f = translate(premisa)`, Prism analiza automáticamente que las etiquetas de tus declaraciones `atom` coincidan fielmente con las proposiciones identificadas en el texto natural. Si por divergencia conceptual o léxica declaras una frase no coincidente (por ejemplo, `atom p = "el procesador se apaga"` cuando el texto dice `"se suspende"`), el compilador emite un aviso formativo inmediato en consola (`prc run` y `prc check`) sugiriéndote la proposición exacta no enlazada:
> ```text
> [Aviso Semántico] El átomo declarado 'p' ("el procesador se apaga") no coincide con ninguna cláusula de 'premisa'.
>    Sugerencia: Se detectó una cláusula no enlazada en el texto: "procesador se suspende" (asignada automáticamente al símbolo 'v').
>    --> ¿Quisiste decir: atom p = "procesador se suspende"?
> ```
> Esto evita la trampa de los diccionarios de sinónimos abiertos, garantizando rigor formal y guiando al estudiante a modelar la proposición canónica correcta.

### 2.5 Palabras reservadas

```text
module  use  atom  formula  nl  let  rule  run
translate  vertauto
when  and  then  means  is  module
not  or  impl  iff
steps  table  verdict
```

### 2.6 Símbolos de puntuación

```text
=  (  )  {  }  ,  :
```

---

## 3. Gramática del programa (EBNF)

```ebnf
program        ::= module_decl { use_decl } { top_level_stmt }

module_decl    ::= 'module' identifier

use_decl       ::= 'use' identifier

top_level_stmt ::= atom_decl
                 | formula_decl
                 | nl_decl
                 | let_decl
                 | rule_decl
                 | run_stmt
                 | vertauto_stmt

atom_decl      ::= 'atom' identifier '=' string_literal

formula_decl   ::= 'formula' identifier '=' formula_expr

nl_decl        ::= 'nl' identifier '=' string_literal

let_decl       ::= 'let' identifier '=' expression

expression     ::= translate_call

translate_call ::= 'translate' '(' text_expr { ',' 'module' ':' identifier } ')'

text_expr      ::= identifier | string_literal

rule_decl      ::= 'rule' identifier '{' rule_body '}'

rule_body      ::= 'when' rule_atom
                   { 'and' rule_atom }
                   'then' rule_formula

rule_atom      ::= 'atom' identifier 'means' string_literal

rule_formula   ::= 'formula' identifier 'is' formula_expr

run_stmt       ::= 'run' identifier

vertauto_stmt  ::= 'vertauto' formula_ref vertauto_suffix?

formula_ref    ::= identifier | '(' formula_expr ')'

vertauto_suffix ::= '{' vertauto_option { vertauto_option } '}'

vertauto_option ::= 'steps' | 'table' | 'verdict'
```

---

## 4. Expresiones de fórmula (`.pr`)

Precedencia de operadores (de menor a mayor binding):

| Nivel | Operadores | Asociatividad |
|-------|------------|---------------|
| 1 | `iff` | izquierda |
| 2 | `impl` | derecha |
| 3 | `or` | izquierda |
| 4 | `and` | izquierda |
| 5 | `not` | prefijo |
| 6 | átomo, `( ... )` | — |

```ebnf
formula_expr   ::= iff_expr

iff_expr       ::= impl_expr { 'iff' impl_expr }

impl_expr      ::= or_expr { 'impl' or_expr }

or_expr        ::= and_expr { 'or' and_expr }

and_expr       ::= not_expr { 'and' not_expr }

not_expr       ::= 'not' not_expr
                 | primary_expr

primary_expr   ::= identifier
                 | '(' formula_expr ')'
```

**Átomos en expresiones:** identificadores declarados con `atom` en el mismo módulo (p. ej. `p`, `q`). El compilador verifica existencia en análisis semántico.

---

## 5. Semántica (v1.0)

### 5.1 `module` — nombre del programa

```text
module <identificador>
```

**Significado:** declara el **nombre lógico del programa** contenido en ese archivo `.pr`. Es la unidad de compilación y ejecución (equivalente conceptual al “nombre del proyecto” o unidad principal, no a una función suelta).

| Pregunta | Respuesta |
|----------|-----------|
| ¿Es el nombre del archivo? | **Casi siempre sí** por convención; formalmente es el nombre **del programa en código**. |
| ¿Cuántos por archivo? | **Exactamente uno.** |
| ¿Dónde va? | **Primera sentencia** del archivo (tras comentarios iniciales opcionales). |
| ¿Puede repetirse en otro `.pr`? | Sí en v1.0 (sin `import` de `.pr`); en proyectos grandes conviene nombres únicos. |

#### Ejemplo: ejercicio en clase

Archivo creado por el alumno: `ejercicio1.pr`

```ls
module ejercicio1

use core

atom p = "llueve"
atom q = "paraguas"
formula f = p impl q

vertauto f
```

- `ejercicio1` en `module` = identificador del programa.
- `ejercicio1.pr` = archivo en disco; **deben coincidir** por convención.
- `p`, `q`, `f` = otros identificadores libres (átomos y fórmula), regidos por §2.3.

#### Relación archivo ↔ `module` ↔ compilación (objetivo)

```text
ejercicio1.pr
  module ejercicio1     →  unidad lógica "ejercicio1"
  lsc compile           →  ejercicio1.class (nombre derivado del module)
  prc run ejercicio1.pr →  ejecuta el module ejercicio1
```

### 5.2 `use` — módulos de conocimiento `.lgs`

```text
use <identificador>
```

- Carga el recurso léxico `logicscript/<identificador>.lgs` (p. ej. `use core` → `core.lgs`).
- El identificador en `use` sigue las mismas reglas que §2.3.
- Cero o más líneas `use` después del `module`.
- Sin `use`, `translate` falla salvo módulo por defecto configurado en runtime.

**Nota:** `use core` referencia un **módulo de datos** `.lgs`, no otro programa `.pr`. Importar programas `.pr` entre sí está **fuera de alcance** en v1.0.

### 5.3 Declaraciones

| Declaración | Efecto |
|-------------|--------|
| `atom p = "..."` | Registra átomo `p` con etiqueta NL (presentación / trazas). |
| `formula f = ...` | Construye fórmula simbólica en IR interna. |
| `nl t = "..."` | Registra texto NL tipo **Text**. |
| `let x = translate(...)` | Evalúa `translate`; enlaza **Formula** a `x`. Inmutable en el ámbito del módulo. |

Redeclarar el mismo identificador en el mismo ámbito: **error**.

### 5.4 `translate`

- Entrada: **Text** (literal o variable `nl`).
- Salida: **Formula** (cadena clásica + IR + mapa de proposiciones).
- Usa patrones del `.lgs` importado.
- Argumento opcional `module: nombre` fuerza un módulo léxico concreto.

### 5.5 `vertauto`

Evalúa una **Formula** y clasifica:

| Dictamen | Condición |
|----------|-----------|
| `tautology` | Verdadera en todas las filas de la tabla de verdad |
| `contingency` | Verdadera en algunas filas y falsa en otras |
| `contradiction` | Falsa en todas las filas |

#### Las 3 Formas de Evaluación en Prism

Prism permite evaluar proposiciones lógicas a través de tres patrones de entrada:

| Forma | Origen | Sintaxis Canónica | Caso de Uso |
| :--- | :--- | :--- | :--- |
| **1. Desde Lenguaje Natural** | `nl` + `translate` | `let f = translate(premisa)`<br>`vertauto f { table verdict }` | Enunciados discursivos en español, problemas de examen o razonamiento del mundo real procesados por el motor NLP. |
| **2. Fórmula Simbólica Declarada** | `formula` | `formula f = p impl q`<br>`vertauto f { table verdict }` | Álgebra booleana y teoremas puros con conectores (`and`, `or`, `not`, `impl`, `iff`). |
| **3. Expresión Simbólica Directa (Inline)** | `vertauto (expr)` | `vertauto (p and not q) { table verdict }`<br>`vertauto (p impl q)` | Evaluación inmediata sin declarar identificadores temporales de fórmula. |

##### Ejemplos de las 3 Formas en Código `.pr`:

```prism
module ejemplo_formas

use core

# FORMA 1: Desde Lenguaje Natural (enunciado -> átomos -> translate -> vertauto)
nl premisa = "Si el procesador se suspende entonces la tarea se reintenta."

atom p = "el procesador se suspende"
atom q = "la tarea se reintenta"

let f1 = translate(premisa)

vertauto f1 {
    table
    verdict
}

# FORMA 2: Fórmula Simbólica con Nombre
formula f2 = p impl q

vertauto f2 {
    table
    verdict
}

# FORMA 3: Expresión Simbólica Directa / Inline
vertauto (p and not q) {
    table
    verdict
}
```

#### Modo corto

```ls
vertauto (p impl q)
vertauto f
```

Salida mínima (una línea):

```text
(p → q)  contingency
```

#### Modo extendido

```ls
vertauto f { steps table verdict }
```

Subconjuntos permitidos: `{ steps }`, `{ table verdict }`, etc.

| Opción | Contenido |
|--------|-----------|
| `steps` | Pasos pedagógicos (identificación, formalización, evaluación por fila). |
| `table` | Tabla de verdad completa. |
| `verdict` | Dictamen con explicación. |

### 5.6 `rule` / `run`

Bloque declarativo que agrupa definiciones `when`/`then`. `run id` materializa las ligaduras de la regla en el entorno del módulo y continúa la ejecución secuencial.

---

## 6. Tipos (modelo estático v1.0)

| Tipo | Origen | Consumido por |
|------|--------|---------------|
| **Text** | `nl`, literal en `translate` | `translate` |
| **Formula** | `formula`, `let`+`translate`, `formula_expr` | `vertauto` |
| **Atom** | `atom` | `formula_expr` |
| **ModuleRef** | `use` | `translate(..., module: ...)` |

Errores de tipo en compilación:

- `vertauto linea_nl` sin `translate` → error.
- Uso de `p` en fórmula sin `atom p` → error (salvo átomos implícitos futuros; v1.0 exige declaración).

---

## 7. Ejemplos canónicos

Ver archivos en `examples/`:

- `ejercicio_lluvia.pr` — NL + `translate` + `vertauto` extendido.
- `tautologia_estudio.pr` — fórmula simbólica `p or not p`.

---

## 8. Compilación y ejecución (objetivo)

```text
programa.pr  --lsc compile-->  programa.class  --JVM-->  ejecución
```

Runtime requerido: `logicscript-runtime.jar` con `translate`, `vertauto`, carga `.lgs`.

---

## 9. Estado de implementación (junio 2026)

| Constructo | Estado |
|------------|--------|
| `translate` (NL → fórmula) | Implementado (`PrismService`) |
| `vertauto` (cadena clásica / expr `.pr`) | Implementado (`VertautoService`) — Fase 2 |
| Parser / compilador `.pr` completo | Pendiente |
| Modo `vertauto { steps table verdict }` | Parcial (tabla + dictamen; pasos en evolución) |
| `rule` / `run` | Solo especificado |

---

## 10. Changelog

| Versión | Cambio |
|---------|--------|
| 1.0 | Gramática inicial `.pr`; modos `vertauto`; tipos y semántica superficial |
| 1.0.1 | Reglas de `module`, archivos `.pr`, identificadores y convención de nombres (§2.4, §5.1–5.2) |
