# 🔬 Prism — Documentación de Creación, Arquitectura y Base para Artículo Científico

Esta carpeta recopila el **acervo técnico y de investigación formal** generado durante el diseño, concepción e implementación de **Prism**. 

Sirve como **fuente primaria de consulta para la redacción del artículo de investigación (paper científico)** sobre compiladores pedagógicos, deducción formal de lógica proposicional y procesamiento de lenguaje natural agnóstico.

---

## 🗂️ Índice de Documentos de Creación

| Archivo | Contenido Central | Aporte al Artículo Científico |
| :--- | :--- | :--- |
| **[`01-bitacora-investigacion-nlp.md`](./01-bitacora-investigacion-nlp.md)** | Bitácora técnica exhaustiva con las **15 mejoras morfosintácticas**: alternancia indicativo/subjuntivo, prefijos privativos, correferencia anafórica de pronombres, modificadores adverbiales accesorios, fonética agnóstica y 58 pruebas de regresión. | **Sección de Metodología y Algoritmos:** Núcleo de la contribución científica sobre cómo procesar español proposicional sin diccionarios cerrados. |
| **[`02-vision-y-arquitectura-general.md`](./02-vision-y-arquitectura-general.md)** | Visión ontológica del lenguaje, justificación del DSL pedagógico frente a lenguajes tradicionales, decibilidad proposicional finita (Orden 0) y separación de responsabilidades. | **Sección de Introducción y Planteamiento del Problema:** Justificación formal de por qué era necesario un nuevo lenguaje como Prism. |
| **[`03-arquitectura-interprete-pr.md`](./03-arquitectura-interprete-pr.md)** | Pipeline de compilación: análisis léxico, parser AST, análisis estático de tipos, resolución de bindings en tiempo de ejecución. | **Sección de Diseño de Sistema e Implementación:** Arquitectura modular del compilador e intérprete. |
| **[`04-motor-deductivo-vertauto.md`](./04-motor-deductivo-vertauto.md)** | Motor deductivo de evaluación lógica: generación de espacio de estados ($2^n$), algoritmo de evaluación matricial y clasificación formal (tautología, contingencia, contradicción). | **Sección de Evaluación Lógica y Verificación:** Demostración de decibilidad determinista. |
| **[`05-inventario-clases-compilador.md`](./05-inventario-clases-compilador.md)** | Inventario modular de componentes Java, interfaces de bajo acoplamiento y paquetes del sistema. | **Sección de Ingeniería de Software:** Métricas de código, modularidad y extensibilidad. |

---

## 📝 Guía Estructural para el Artículo Científico (Paper Outline)

Para redactar el artículo académico a partir de esta documentación, se recomienda seguir esta estructura de secciones estándar (formato IEEE / ACM):

```
1. INTRODUCCIÓN
   - La enseñanza de la lógica proposicional en ciencias computacionales.
   - La brecha entre el lenguaje natural humano y la formalización simbólica.
   - Documento base: 02-vision-y-arquitectura-general.md

2. TRABAJOS RELACIONADOS Y ESTADO DEL ARTE
   - Comparación con herramientas de lógica asistida (Tarski's World, Isabelle, Prover9).
   - Limitaciones de los analizadores tradicionales basados en listas rígidas de palabras o redes neuronales no deterministas.

3. ARQUITECTURA DEL COMPILADOR PRISM
   - Gramática formal del DSL.
   - Representación Intermedia (IR) y Árbol de Sintaxis Abstracta (AST).
   - Documento base: 03-arquitectura-interprete-pr.md

4. MOTOR DE NORMALIZACIÓN MORFOSINTÁCTICA AGNÓSTICA (Contribución Principal)
   - Lematización heurística y desinencias regulares sin diccionarios de palabras.
   - Alternancia modal (indicativo <-> subjuntivo) y fonética regular.
   - Resolución de elisiones coordinadas y reducción de complementos preposicionales.
   - Correferencia de pronombres anafóricos (él, ella, este) con concordancia de género/número.
   - Abstracción de modificadores adverbiales accesorios (-mente, locuciones).
   - Documento base: 01-bitacora-investigacion-nlp.md

5. MOTOR DEDUCTIVO Y EVALUACIÓN DETERMINISTA
   - Algoritmo de evaluación en espacio de estados finito (2^n).
   - Clasificación matemática: tautología, contingencia, contradicción.
   - Documento base: 04-motor-deductivo-vertauto.md

6. VALIDACIÓN EXPERIMENTAL Y RESULTADOS
   - Suite de 58 pruebas de regresión automatizadas.
   - Comparativa de deduplicación de átomos (eliminación de variables parásitas).
   - Evaluación pedagógica del método canónico (enunciado -> átomos -> translate -> vertauto).

7. CONCLUSIONES Y TRABAJO FUTURO
   - Logros del orden 0 sin diccionarios.
   - Límites formales del sistema (cuantificadores de primer orden fuera de alcance).
```

