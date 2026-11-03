# Prism Language Support

Soporte oficial para el **lenguaje de programación Prism (`.pr`)** en Visual Studio Code y Cursor. 

Prism es un lenguaje declarativo orientado a la **formalización, traducción y evaluación de lógica proposicional**, capaz de procesar lenguaje natural en español, traducirlo a fórmulas formales y calcular tablas de verdad y dictámenes lógicos.

---

## Características principales

* **Coloreado de sintaxis oficial:** Resaltado TextMate para palabras clave (`module`, `use`, `atom`, `nl`, `formula`, `let`, `translate`, `vertauto`), operadores lógicos y comentarios.
* **Diagnósticos y linter en tiempo real:** Detección inmediata de errores de sintaxis con ubicación exacta de columna y sugerencias automáticas (*"Quizás quisiste decir..."*).
* **Linter semántico preventivo:** Avisa si las proposiciones atómicas declaradas no concuerdan con los fragmentos detectados en el texto natural.
* **Acciones rápidas (Quick Fix):** Corrección instantánea de nombres de átomos y palabras clave con el foquito del editor (`Ctrl + .`).
* **Auto-indentación inteligente:** Sangría automática estándar de 1 tab al presionar `Enter` en premisas extensas multilínea.
* **Formateador de documento:** Presiona `Shift + Alt + F` (o clic derecho -> *Dar formato al documento*) para ordenar y alinear tu código automáticamente.
* **Comentarios de bloque (`/* ... */`):** Atajo rápido con `Ctrl + Shift + /`, `Ctrl + Shift + A` o mediante el menú contextual de clic derecho.
* **Colección de snippets:** Plantillas para generar módulos, premisas y tablas de verdad en segundos.

---

## Ejemplo de código `.pr`

```prism
module ejercicio_lluvia

use core

# 1. Enunciado del problema en lenguaje natural
nl premisa = "Si el procesador se suspende entonces la tarea se reintenta; 
    pero no es verdad que él se suspenda o ella se reintente."

# 2. Definición formal de proposiciones atómicas
atom p = "el procesador se suspende"
atom q = "la tarea se reintenta"

# 3. Traducción y formalización lógica
let equivalencia = translate(premisa)

# 4. Construcción de tabla de verdad y dictamen formal
vertauto equivalencia {
    steps
    table
    verdict
}
```

---

## Atajos de teclado útiles

| Acción | Atajo en Linux / Windows | Atajo en macOS |
|--------|--------------------------|----------------|
| **Alternar comentario de bloque** | `Ctrl + Shift + /` o `Ctrl + Shift + A` | `Cmd + Shift + /` |
| **Corrección rápida (Quick Fix)** | `Ctrl + .` | `Cmd + .` |
| **Formatear documento** | `Shift + Alt + F` | `Shift + Option + F` |

---

## Snippets integrados (Escribe el prefijo + `Tab`)

| Prefijo | Descripción | Código generado |
|---------|-------------|-----------------|
| `prnl` | Módulo completo NL | Estructura completa de traducción y `vertauto` extendido |
| `prform` | Módulo simbólico | Declaración de átomos, fórmula formal y `vertauto` |
| `atom` | Átomo proposicional | `atom p = "proposicion"` |
| `nl` | Premisa en lenguaje natural | `nl premisa = "texto..."` |
| `translate` | Traducción a fórmula | `let f = translate(premisa)` |
| `formula` | Fórmula lógica | `formula f = p impl q` |
| `vax` | Evaluación lógica extendida | `vertauto f { steps table verdict }` |

---

## Compilador y Runtime CLI (`prc`)

Esta extensión se integra de forma transparente con el compilador oficial de Prism. Puedes ejecutar tus programas directamente desde la terminal del editor:

```bash
# 1. Ejecutar programa directamente (modo intérprete)
prc run mi_programa.pr

# 2. Compilar a bytecode de la JVM (.class)
prc compile mi_programa.pr -d out/

# 3. Validar sintaxis y referencias sin ejecutar
prc check mi_programa.pr
```

---

## Requisitos

* **Visual Studio Code** 1.80.0 o superior (o Cursor).
* **Java (OpenJDK 17 o superior)** instalado en el sistema para ejecutar los diagnósticos y el compilador `prc`.

---

## Enlaces y soporte

* **Repositorio oficial en GitHub:** [github.com/AngelMoralesChazari/prism](https://github.com/AngelMoralesChazari/prism)
* **Reporte de incidencias y sugerencias:** [GitHub Issues](https://github.com/AngelMoralesChazari/prism/issues)
* **Licencia:** MIT © AB Software Group
