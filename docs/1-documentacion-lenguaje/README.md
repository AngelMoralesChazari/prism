# 📖 Prism — Manual Oficial del Lenguaje y Guía de Uso

Esta carpeta contiene la **documentación oficial de uso del lenguaje Prism**, dirigida a desarrolladores, estudiantes, profesores y usuarios que deseen escribir, compilar y evaluar programas lógicos en Prism (`.pr`).

---

## 🗂️ Índice de Contenidos

| Archivo | Descripción | ¿Cuándo consultarlo? |
| :--- | :--- | :--- |
| **[`01-especificacion-lenguaje-v1.0.md`](./01-especificacion-lenguaje-v1.0.md)** | Especificación formal del lenguaje Prism (gramática EBNF, tipos `Text`, `Formula`, palabras reservadas, `translate`, `vertauto`). | Cuando necesites conocer las reglas formales, la sintaxis estricta o la semántica del lenguaje. |
| **[`02-guia-rapida-ejercicios.md`](./02-guia-rapida-ejercicios.md)** | Guía práctica paso a paso para programar ejercicios lógicos, comandos del compilador (`prc run`, `prc check`) y **las 3 formas de evaluación**. | Al comenzar a resolver ejercicios o para recordar la sintaxis del método en papel. |
| **[`03-autocompletado-y-snippets.md`](./03-autocompletado-y-snippets.md)** | Catálogo completo de snippets de autocompletado para VS Code (`prnl`, `prform`, `prinl`, `prnlit`, `prog`), árbol de decisión y atajos. | Cuando estés programando en el editor y quieras saber qué atajo usar para cada tipo de ejercicio. |
| **[`04-extension-vscode.md`](./04-extension-vscode.md)** | Guía de instalación y configuración de la extensión oficial de Prism para VS Code y Cursor (resaltado de sintaxis y snippets). | Para configurar tu entorno de desarrollo en una máquina nueva. |
| **[`05-sintaxis-y-reglas-identificadores.md`](./05-sintaxis-y-reglas-identificadores.md)** | Reglas sobre identificadores, convenciones de nombres entre archivos `.pr` y módulos, enlaces inmutables con `let`. | Para resolver dudas sobre nombres válidos de variables, fórmulas o módulos. |

---

## 🚀 Resumen Rápido: Las 3 Formas de Evaluar en Prism

1. **Forma 1: Lenguaje Natural Canónico (`nl` + `atom` + `translate` + `vertauto`)**
   ```prism
   module ejercicio1
   use core

   nl premisa = "Si el procesador se suspende entonces la tarea se reintenta."
   atom p = "el procesador se suspende"
   atom q = "la tarea se reintenta"

   let equivalencia = translate(premisa)

   vertauto equivalencia {
       steps
       table
       verdict
   }
   ```

2. **Forma 2: Fórmula Simbólica con Nombre (`formula`)**
   ```prism
   module ejercicio2
   use core

   atom p = "el procesador se suspende"
   atom q = "la tarea se reintenta"

   formula f = p impl q

   vertauto f {
       table
       verdict
   }
   ```

3. **Forma 3: Expresión Simbólica Directa / Inline (`vertauto (expr)`)**
   ```prism
   module ejercicio3
   use core

   atom p = "el procesador se suspende"
   atom q = "la tarea se reintenta"

   vertauto (p and not q) {
       table
       verdict
   }
   ```

