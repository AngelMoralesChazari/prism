# Prism — Visión y definición general del lenguaje

**Versión del documento:** 1.0 (borrador de especificación)  
**Fecha:** junio 2026  
**Estado:** definición de producto y arquitectura — guía para implementación

Este documento describe **qué es Prism**, **qué se construirá** y **cómo debe comportarse** el lenguaje completo: sintaxis propia, compilación a JVM, módulos `.lgs`, programas `.pr` y verificación lógica pedagógica.

---

## 1. Idea central

Prism es un **lenguaje de programación declarativo** especializado en **lógica proposicional**.

El programador **no escribe en español ni en inglés** para programar. Escribe en **sintaxis Prism** (palabras reservadas fijas: `atom`, `impl`, `vertauto`, etc.). Las proposiciones en lenguaje natural van como **datos** entre comillas; el motor las procesa según los módulos `.lgs` cargados.

Inspiración filosófica: **Prolog** (declarativo, datos + reglas, runtime que resuelve), pero con **sintaxis e identidad propias** — sin copiar `:-`, `?-` ni la forma de escribir de Prolog.

### En una frase

Un lenguaje declarativo donde tú **nombras** proposiciones (`p`, `q`…), **les das significado**, **construyes** la fórmula (`impl`, `or`, `not`…) y **ordenas** al motor que haga el trabajo formal: traducir, tabla de verdad, pasos y dictamen.

---

## 2. Tres capas del sistema

| Capa | Idioma | Ejemplo | Rol |
|------|--------|---------|-----|
| **Sintaxis del lenguaje** | Fija, propia de Prism | `atom`, `impl`, `vertauto` | Cómo se escribe el programa `.pr` |
| **Contenido / datos** | Lo que el programador pone entre comillas | `"llueve"`, `"si llueve llevo paraguas"` | Puede ser español, inglés u otro idioma soportado por `.lgs` |
| **Reglas de traducción** | Archivos `.lgs` declarativos | `lemma`, `pattern`, `lexrule` | Cómo el texto NL se mapea a estructura lógica |

El programador programa en **Prism**. El contenido NL es **dato**, no sintaxis del lenguaje.

---

## 3. Archivos y herramientas

| Artefacto | Extensión | Quién lo escribe | Contenido |
|-----------|-----------|------------------|-----------|
| Programa | `.pr` | Programador del ejercicio | `module`, `atom`, `formula`, `translate`, `vertauto`, `rule` |
| Conocimiento lingüístico | `.lgs` | Autor del módulo léxico | `lemma`, `pattern`, `lexrule` — NL → estructura |
| Bytecode compilado | `.class` | Generado por `lsc` | Ejecutable en JVM |
| Compilador | `lsc` | Herramienta del proyecto | `compile`, `run`, `check` |
| Runtime | `logicscript-runtime.jar` | Motor del proyecto | `translate`, IR, tablas de verdad, `vertauto` |

---

## 4. Arquitectura de compilación y ejecución

Modelo elegido: **Opción A — como Java** (compilado a bytecode JVM).

```text
┌─────────────────────────────────────────────────────────┐
│  programa.pr  — sintaxis Prism (keywords fijos)   │
│    atom p = "llueve"                                    │
│    vertauto (p impl q)                                  │
└──────────────────────────┬──────────────────────────────┘
                           │ compila (lsc)
                           ▼
┌─────────────────────────────────────────────────────────┐
│  programa.class  →  JVM                                 │
│    + logicscript-runtime.jar                            │
└──────────────────────────┬──────────────────────────────┘
                           │
         ┌─────────────────┴─────────────────┐
         ▼                                   ▼
  translate("...")                    vertauto(formula)
  usa módulos .lgs                  tabla + pasos + dictamen
  (español / inglés / …)            (independiente del idioma)
```

### Flujo de compilación

```text
ejercicio.pr
    → lsc compile
    → ejercicio.class + logicscript-runtime.jar
    → prc run ejercicio.pr   (o java con classpath)
    → JVM ejecuta vertauto, translate, etc.
```

El usuario **no escribe Java**. El compilador `lsc` genera clases JVM que invocan el **runtime** (motor actual + evaluador de tablas de verdad).

### Nota sobre “híbrido”

El lenguaje es **compilado**. En desarrollo (REPL, botón Run en VS Code) se puede ejecutar parseando el AST sin escribir `.class` en disco; en producción y releases se compila siempre. Igual que Java con `javac` frente al “run” del IDE.

---

## 5. Sintaxis del lenguaje v1.0 (borrador)

Palabras reservadas en **inglés corto** (conjunto final sujeto a congelación en especificación EBNF).

### 5.1 Módulo y conocimiento

**Especificación completa:** `Language-v1.0.md` §2.3 (identificadores), §2.4 (archivos), §5.1–5.2 (`module` / `use`).

Resumen:

- **`module ejercicio1`** = nombre lógico del programa (debe ser la **primera** sentencia del `.pr`).
- **Convención:** archivo `ejercicio1.pr` → `module ejercicio1` (mismo nombre sin extensión).
- **Identificadores:** empiezan con **letra**; luego letras, dígitos o `_`; sensibles a mayúsculas; **no** empiezan con número.
- **`use core`** carga `core.lgs` (conocimiento léxico), no otro `.pr`.

```ls
module ejercicio1

use core          # carga core.lgs (patrones español)
use core_en       # opcional: patrones inglés
```

Ejemplo mínimo de archivo para un ejercicio:

```ls
module ejercicio1

use core

atom p = "llueve"
atom q = "paraguas"
formula f = p impl q

vertauto f
```

### 5.2 Proposiciones atómicas

El programador nombra símbolos y les asigna significado (texto NL o etiqueta):

```ls
atom p = "llueve"
atom q = "llevo paraguas"
```

### 5.3 Fórmulas simbólicas

```ls
formula f = p impl q
formula g = p or not p
formula h = (p and q) impl r
```

| Prism | Lógica clásica |
|-------------|----------------|
| `not p` | ¬p |
| `p and q` | p ∧ q |
| `p or q` | p ∨ q |
| `p impl q` | p → q |
| `p iff q` | p ↔ q |

En el fuente **no** se usan símbolos Unicode (`→`, `∧`); el motor los emite en salida formal si hace falta.

### 5.4 Texto NL y traducción

#### Declarar texto en lenguaje natural — `nl`

```text
nl <identificador> = <cadena>
```

- **Definido en el lenguaje.** Reserva un nombre para un fragmento NL (español, inglés, etc.).
- El identificador (`linea1`, `texto_lluvia`, …) lo elige el programador; **no** tiene significado especial.

```ls
nl linea_lluvia = "si llueve llevo paraguas"
```

#### Traducir a fórmula — `translate`

```text
translate(<expresión_texto>)
translate(<expresión_texto>, module: <identificador_módulo>)
```

- **Definido en el lenguaje.** `<expresión_texto>` puede ser una variable `nl` o una cadena literal `"..."`.
- `module:` es argumento **nombrado opcional**; el módulo debe estar disponible (p. ej. `use core_en`).
- Resultado: valor de tipo **Formula** (fórmula simbólica + metadatos internos para pasos y proposiciones).

#### Enlazar resultados — `let`

```text
let <identificador> = <expresión>
```

- **Definido en el lenguaje.** Crea un nombre local para el resultado de una expresión (típicamente `translate(...)`).
- Una sola asignación por nombre en el mismo ámbito (v1.0).
- Los nombres `f1`, `phi`, `psi` en los ejemplos son **solo ilustrativos**; ver nota `NOTAS-IDENTIFICADORES-LET-Y-EJEMPLOS.md`.

```ls
let implicacion = translate(linea_lluvia)
let f_directa   = translate("si llueve llevo paraguas")
let f_ingles    = translate("if it rains I carry an umbrella", module: core_en)
```

`translate` usa por defecto los módulos `.lgs` importados con `use`.

> **Nota:** Qué es sintaxis oficial y qué son nombres de ejemplo (`f1`, `phi`, `linea1`) está detallado en **`NOTAS-IDENTIFICADORES-LET-Y-EJEMPLOS.md`**.

### 5.5 Reglas declarativas

Espíritu declarativo sin sintaxis Prolog:

```ls
rule implica_lluvia {
    when atom p means "llueve"
    and  atom q means "paraguas"
    then formula f is p impl q
}

run implica_lluvia
vertauto f
```

### 5.6 Resumen de declaraciones y palabras reservadas (v1.0)

| Palabra / forma | Rol | Ejemplo |
|-----------------|-----|---------|
| `module` | Cabecera del programa | `module rain_logic` |
| `use` | Cargar `.lgs` | `use core` |
| `atom` | Átomo proposicional | `atom p = "llueve"` |
| `formula` | Fórmula simbólica | `formula f = p impl q` |
| `nl` | Texto NL (dato) | `nl t = "si p entonces q"` |
| `let` | Enlace a resultado de expresión | `let f = translate(t)` |
| `translate` | NL → Formula | `translate("...")` |
| `vertauto` | Evaluar / dictamen | `vertauto f` |
| `rule` / `run` | Regla declarativa | `rule r { ... }` |
| `not` `and` `or` `impl` `iff` | Conectores en expresiones | `p or not p` |
| `#` | Comentario | `# ejercicio 1` |

**Identificadores** (`p`, `f`, `linea_lluvia`, `phi`, …): libres salvo que choquen con palabras reservadas. Detalle en la nota citada arriba.

---

## 6. `vertauto` — verificación lógica y salida pedagógica

`vertauto` es el **predicado central** del runtime para evaluar una fórmula y, opcionalmente, mostrar el proceso pedagógico completo (como la resolución manual de un ejercicio de lógica).

### 6.1 Proceso interno (siempre ocurre)

Aunque la salida sea corta, el motor **siempre** ejecuta internamente:

1. **Identificación** de proposiciones atómicas (`p`, `q`, …)
2. **Formalización** de la fórmula (`p impl q`, etc.)
3. **Construcción del espacio de estados** — filas `2^n` para `n` átomos distintos
4. **Evaluación** columna a columna (negaciones, luego conectiva principal)
5. **Dictamen** — tautología, contingencia o contradicción según la columna del operador principal

### 6.2 Dos modos de salida (diseño acordado)

#### Modo corto — una línea

```ls
vertauto (p impl q)
vertauto f
```

**Comportamiento:** respuesta **mínima** en consola. Por defecto:

- Fórmula evaluada (notación simbólica)
- **Dictamen** en una línea: `tautology`, `contingency` o `contradiction` (o equivalente en español en locale, según configuración futura)

Ejemplo de salida esperada:

```text
(p → q)  contingency
```

Ideal cuando el programador solo quiere el **resultado**, no la demostración paso a paso.

#### Modo extendido — bloque pedagógico

```ls
vertauto f {
    steps
    table
    verdict
}
```

**Comportamiento:** el programador elige **qué secciones** imprimir:

| Clave en bloque | Contenido |
|-----------------|-----------|
| `steps` | Pasos de análisis (identificación, formalización, evaluación fila a fila) |
| `table` | Tabla de verdad completa |
| `verdict` | Dictamen final con explicación |

Se pueden usar **subconjuntos** del bloque:

```ls
vertauto f { verdict }           # solo dictamen ampliado
vertauto f { table verdict }     # tabla + dictamen, sin pasos
vertauto f { steps table verdict }  # informe completo
```

Si el bloque está **vacío** `vertauto f { }`, se recomienda tratarlo como error de compilación o equivalente a modo corto (decisión a fijar en EBNF).

### 6.3 Por qué este diseño

- **Modo corto:** scripts, automatización, respuestas rápidas en IDE.
- **Modo extendido:** enseñanza, depuración, alineado con el flujo “Estudio o no estudio” → `p ∨ ¬p` → tabla → tautología.
- Misma operación interna; solo cambia la **presentación** — no duplicar motores.

---

## 7. Ejemplos completos

### 7.1 Implicación desde lenguaje natural

```ls
module ejercicio_lluvia

use core

atom p = "llueve"
atom q = "paraguas"

nl linea_lluvia = "si llueve llevo paraguas"
let implicacion = translate(linea_lluvia)

vertauto implicacion {
    steps
    table
    verdict
}
```

### 7.2 Tautología clásica (solo símbolos)

```ls
module ejercicio_estudio

atom p = "estudio"
formula tautologia_estudio = p or not p

vertauto tautologia_estudio {
    steps
    table
    verdict
}
# Esperado: tautología; columna principal [V, V]
```

### 7.3 Respuesta rápida

```ls
module quick_check

formula g = p or not p
vertauto g
# Salida corta: dictamen en una línea
```

---

## 8. Relación con el código actual del repositorio

| Pieza existente | Rol en el lenguaje completo |
|-----------------|----------------------------|
| `core.lgs` + `LgsCargador` | Backend de `translate` y módulos `use` |
| `LogicExpr` + `EmitidorFormula` | IR y emisión de fórmulas |
| `PrismEngine` / `PrismService` | Pipeline NL → fórmula |
| `PrismRegressionHarness` | Regresión de `translate` y futuro compilador |
| **Por implementar** | Parser `.pr`, compilador `lsc`, codegen JVM |
| **Por implementar / portar** | Motor de tablas de verdad y pasos para `vertauto` (antes `MotorLogico` en TautoTeacher) |

---

## 9. Lo que Prism es y no es

### Es

- Lenguaje declarativo con sintaxis propia
- Compilado a bytecode JVM (`lsc` → `.class`)
- Separación `.pr` (programa) / `.lgs` (conocimiento NL)
- Datos en cualquier idioma soportado por módulos `.lgs`
- Verificación formal con salida corta o pedagógica (`vertauto`)

### No es

- Un lenguaje cuya sintaxis de programación sea español
- Una copia de Prolog
- Un traductor de español general sin reglas explícitas
- Un LLM ni motor estadístico

---

## 10. Roadmap de implementación (resumen)

| Fase | Entregable |
|------|------------|
| A | Especificación EBNF `.pr` (`docs/spec/Language-v1.0.md`) |
| B | Parser + AST |
| C | Runtime empaquetado (JAR) con `translate` + `vertauto` |
| D | Compilador `lsc` → bytecode JVM |
| E | CLI: `lsc compile`, `prc run`, `prc check` |
| F | Ejemplos en `examples/*.pr` |
| G | Extensión VS Code (syntax, Run, LSP) |

---

## 11. Criterios de éxito

- [ ] Programa `.pr` mínimo compila y ejecuta en JVM
- [ ] `translate` reutiliza `core.lgs` y harness existente sigue en verde
- [ ] `vertauto (f)` devuelve dictamen en una línea
- [ ] `vertauto f { steps table verdict }` reproduce flujo pedagógico completo
- [ ] Sintaxis claramente distinta de Prolog y de Java
- [ ] Documentación de lenguaje versionada en este directorio

---

## 12. Referencias en el repositorio

| Documento | Ubicación |
|-----------|-----------|
| Filosofía original | `docs/FILOSOFIA-Y-OBJETIVO.txt` |
| Sintaxis `.lgs` v0.6 | `docs/SINTAXIS-DSL-RESUMEN.md` |
| Plan de migración técnica | `docs/PLAN-MIGRACION-PROYECTO-INDEPENDIENTE.md` |
| Inventario Java actual | `docs/INVENTARIO-CLASES-JAVA.md` |
| **Este documento** | `documentacion Prism/VISION-Y-DEFINICION-GENERAL.md` |
| Especificación `.pr` v1.0 (EBNF, `module`, nombres) | `documentacion Prism/Language-v1.0.md` |
| Notas `let`, `nl`, identificadores | `documentacion Prism/NOTAS-IDENTIFICADORES-LET-Y-EJEMPLOS.md` |

---

*Prism: sintaxis fija, datos en tu idioma, motor que demuestra la lógica.*
