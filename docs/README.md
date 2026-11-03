# Prism — Centro de Documentación

Bienvenido al centro de documentación del lenguaje de programación y motor deductivo **Prism**.

La documentación se encuentra organizada en dos grandes vertientes complementarias:

---

## Organización de Carpetas

```
docs/
├── 1-documentacion-lenguaje/     ──► Documentación para Usuarios, Estudiantes y Programadores
│   ├── README.md                 (Índice de manuales y guías de uso)
│   ├── 01-especificacion-lenguaje-v1.0.md
│   ├── 02-guia-rapida-ejercicios.md
│   ├── 03-autocompletado-y-snippets.md
│   ├── 04-extension-vscode.md
│   └── 05-sintaxis-y-reglas-identificadores.md
│
├── 2-documentacion-creacion/     ──► Documentación de Investigación para el Artículo Científico
│   ├── README.md                 (Estructura y guía de redacción del paper académico)
│   ├── 01-bitacora-investigacion-nlp.md
│   ├── 02-vision-y-arquitectura-general.md
│   ├── 03-arquitectura-interprete-pr.md
│   ├── 04-motor-deductivo-vertauto.md
│   └── 05-inventario-clases-compilador.md
│
├── historico-legacy/             ──► Archivos históricos de etapas previas del proyecto (LogicScript)
│
└── PROJECT_CONTEXT.md            ──► Contexto técnico del proyecto para herramientas y entornos de desarrollo
```

---

### 1. [Documentación del Lenguaje (`1-documentacion-lenguaje/`)](./1-documentacion-lenguaje/README.md)
* **Público objetivo:** Personas que van a **escribir código en Prism**.
* **Contenido:**
  * Cómo escribir un programa `.pr`.
  * La especificación formal v1.0 (EBNF, tipos de datos, palabras reservadas).
  * Las **3 formas de evaluación** (`translate`, `formula`, `vertauto inline`).
  * Catálogo de snippets de autocompletado en VS Code (`prnl`, `prform`, `prinl`, `prnlit`, `prog`).
  * Comandos de consola (`prc run`, `prc check`).

* [Ir a Documentación del Lenguaje](./1-documentacion-lenguaje/README.md)

---

### 2. [Documentación de la Creación del Lenguaje (`2-documentacion-creacion/`)](./2-documentacion-creacion/README.md)
* **Público objetivo:** Para el autor / investigador para la **redacción del artículo de investigación (paper científico)**.
* **Contenido:**
  * **Bitácora de investigación:** El paso a paso de cómo se resolvieron los desafíos morfológicos (las 15 mejoras NLP, fonética agnóstica, pronombres, sufijos, elisiones).
  * **Visión y arquitectura:** La teoría formal detrás del diseño de Prism y su decibilidad proposicional.
  * **Diseño del compilador:** AST, IR, motor de evaluación de tablas de verdad ($2^n$).
  * **Validación experimental:** Suite de 58 pruebas de regresión automatizadas.

* [Ir a Documentación de Creación y Artículo](./2-documentacion-creacion/README.md)

