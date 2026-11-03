# LogicScript

**LogicScript** es el pseudolenguaje y la capa de especificación interna de **TautoTeacher 2.0** para conectar **lenguaje natural (español)** con la **lógica proposicional** que ya evalúa el motor del sistema.

Este documento es la referencia viva del proyecto: idea, motivación, implementación prevista, funcionamiento, filosofía y evolución.

---

## 1. Idea de LogicScript

LogicScript no pretende ser un lenguaje de programación general. Es un **lenguaje intermedio declarativo**: una forma **explícita y verificable** de representar:

- reglas léxicas y morfológicas (variantes de la misma raíz: *llueve* / *llueva*, etc.);
- patrones de enunciados (condicionales, negaciones, coordinaciones);
- equivalencias entre construcciones del español y **formas lógicas** (proposiciones, conectivas, agrupación).

El flujo conceptual es:

```text
Texto en español  →  [LogicScript + base de conocimiento]  →  Fórmula proposicional  →  MotorLogico
```

La “sintaxis” de LogicScript en código será lo que definamos en **Java** (clases, tablas de reglas, formatos de entrada opcionalmente serializables). El nombre agrupa **toda esa capa**: reglas, datos y convenciones, no solo un archivo de texto con una gramática BNF.

---

## 2. Por qué crearlo

### 2.1 El español no se comporta como una fórmula

- La gramática típica (sujeto + verbo + complemento) **no siempre aparece explícita** (*“Si llueve, entonces llevo paraguas”* puede omitir sujeto en una de las cláusulas).
- Una misma **raíz léxica** adopta **formas distintas** según modo, tiempo o subjuntivo (*llueve* / *llueva*), pero el usuario piensa en **un mismo predicado**.
- Los **conectores naturales** (*si… entonces*, *en caso de que*, *a menos que*…) son muchos y **ambigüos** sin contexto.

Sin una capa intermedia, o bien hardcodeamos casos sueltos en la UI, o bien acoplamos el motor lógico a frases en español. LogicScript evita ambos extremos.

### 2.2 Base de conocimiento, estilo “lógica declarativa”

La inspiración en sistemas tipo **Prolog / SWI-Prolog** es **filosófica**, no obligatoria en runtime:

- separar **hechos y reglas** (qué tokens, qué plantillas, qué equivalencias) del **motor de inferencia** que ya tienes para tautologías;
- poder **ampliar** el lenguaje cubierto sin reescribir todo el analizador cada vez;
- documentar decisiones (*por qué* “si” mapea a implicación en un contexto y no en otro).

LogicScript es el nombre del **contrato** entre “conocimiento lingüístico” y “evaluación lógica”.

### 2.3 Trazabilidad y enseñanza

Para un producto **educativo**, conviene poder explicar:

- qué regla disparó la traducción;
- qué fórmula intermedia se generó;
- por qué un enunciado quedó como **contingencia** / **tautología** / **contradicción**.

LogicScript da un lugar donde **fijar esas reglas por escrito** (este documento + código + **pasos de análisis** en el resultado).

---

## 3. Con qué se construye (preferencia: Java)

- **Lenguaje de implementación:** **Java** (mismo stack que TautoTeacher 2.0).
- **Ubicación prevista en el árbol de fuentes:** paquete dedicado, por ejemplo `tautoteacher2.logicscript`, con subpaquetes según crezca el diseño (`lexicon`, `patterns`, `ir`, `pipeline`).
- **Dependencias:** en una primera fase, **solo la JDK** (colecciones, regex, I/O, opcionalmente `java.text` para normalización). Si más adelante se añade un parser formal (ANTLR, etc.), se documentará aquí la versión y el motivo.
- **Persistencia de la base de conocimiento:** por definir (propiedades, JSON en classpath, o recursos embebidos); la filosofía es que el conocimiento sea **datos + código mínimo**, no magia en la UI.

---

## 4. Cómo funciona (visión de arquitectura)

Esta sección describe el **comportamiento objetivo**; el orden exacto de módulos se irá alineando con el código.

1. **Normalización del texto de entrada**  
   Minúsculas donde aplique, unificación de espacios, puntuación básica, y preparación para tokenización.

2. **Lexicon / lematización ligera**  
   Tablas o reglas que agrupen variantes bajo **predicados lógicos** (p. ej. mismas proposiciones *p*, *q* para hechos distintos del mundo real, según el diseño pedagógico).

3. **Detección de patrones (plantillas)**  
   Patrones para condicionales, negación, “y”, “o”, “si y solo si” en lenguaje coloquial y académico.

4. **IR (representación intermedia) de LogicScript**  
   Estructura en memoria (árbol o grafo de proposiciones y conectivas) **antes** de volcar a la cadena que entiende `MotorLogico`.

5. **Emisión de fórmula**  
   Generación de la cadena con símbolos `¬`, `∧`, `∨`, `→`, `↔` y paréntesis correctos.

6. **Evaluación**  
   Reutilización del **motor existente** (tautología, tipo de fórmula, mensajes al usuario).

Los fallos en cualquier paso deben producir **mensajes claros** (no solo “error”), alineados con la pestaña de lenguaje natural.

---

## 5. Filosofía de diseño

|        Principio        |                                            Significado                                                            |
|-------------------------|-------------------------------------------------------------------------------------------------------------------|
| **Declaratividad**      | Las reglas describen *qué* corresponde a *qué*; el flujo de ejecución las aplica de forma uniforme.               |
| **Separación de capas** | UI → LogicScript → fórmula → motor. Sin mezclar español dentro del evaluador proposicional.                       |
| **Extensibilidad**      | Nuevas locuciones o sinónimos deben poder añadirse ampliando conocimiento, no reescribiendo el núcleo entero.     |
| **Honestidad**          | Si un enunciado es **ambiguo** o **fuera de cobertura**, el sistema lo declara; no forzar una fórmula incorrecta. |
| **Pedagogía**           | Priorizar explicabilidad sobre cobertura máxima en la primera versión.                                            |

---

## 6. Relación con TautoTeacher 2.0

- **Pestaña “Lenguaje natural”** en la UI consume el **pipeline LogicScript** cuando esté cableado.
- **Pestaña “Fórmula lógica”** sigue yendo **directo** al motor (sin pasar por LogicScript).
- **Sección “Explicación”** podrá usar en el futuro la **misma IR** o la fórmula generada para generar pasos, en línea con el software original `TautoTeacher.java`.

---

## 7. Evolución de este documento

- Cada vez que se añada un **módulo Java** relevante, actualizar la **§10 Mapa de clases** y, si cambia el flujo, la **§4**.
- Cuando exista una **sintaxis textual** opcional de LogicScript (si se define un DSL en archivo), añadir aquí **gramática**, ejemplos y semántica.
- Mantener un **changelog** breve al final del archivo (fechas y cambios conceptuales importantes).

### Changelog

| Fecha | Cambio |
|-------|--------|
| (inicial) | Creación del documento: definición de LogicScript, motivación, Java, arquitectura y filosofía. |
| 2026-04-08 | Implementación inicial en Java: `LogicScriptService`, `LogicScriptEngine`, `LogicScriptResult`; integración básica en la pestaña LN. |
| 2026-04-08 | §8–§11, mapa de clases y roadmap; `pasosDeAnalisis`; capa NLP (`NormalizadorTexto`, `NaturalLexer`); IR (`LogicExpr`, `EmitidorFormula`, `RegistroProposiciones`); `LogicScriptEngine` como orquestador. |
| 2026-04-09 | Base de conocimiento (`BaseConocimiento`) y `SemanticMapper` implementados; documentación separada en `docs/LogicScript/`. |
| 2026-04-09 | Formato `.lgs` v0.1: `core.lgs`, `LgsCargador`, `LogicScriptCli`; `compile.ps1` copia recursos a `out`. |
| 2026-04-09 | Documento completo de `lemma`: definición, sintaxis, flujo y límites en `docs/LogicScript/Lemma.md`. |

---

## 8. Nombre y marca

**LogicScript** identifica el pseudolenguaje y el subsistema en conversaciones técnicas, README y comentarios de código. La UI puede mostrar al usuario una etiqueta amigable en español (por ejemplo *“traducción LogicScript”*) cuando convenga claridad sin sobrecargar la pantalla.

---

## 9. Convención de documentación (antecedente histórico)

Para que LogicScript pueda evolucionar a **archivos con extensión propia** y herramientas externas sin perder el hilo de **por qué** y **de dónde salió** cada pieza:

| Dónde | Qué documentar |
|-------|----------------|
| **`docs/LogicScript.md`** | Visión, filosofía, **mapa de clases** (esta sección §10), **roadmap** (§11), changelog, gramática textual cuando exista. Es el “libro blanco” del pseudolenguaje. |
| **JavaDoc** en cada clase pública | Rol en el pipeline, entradas/salidas relevantes, relación con otras clases y nota “MVP / en evolución” si aplica. |
| **Commits / PR** | Cambios de comportamiento del traductor o del formato de salida (breve, en español si el equipo lo usa así). |

Regla práctica: **cada clase nueva** del subsistema LogicScript (o `nlp` ligado a LogicScript) debe tener **al menos** una fila en la tabla de la §10 en el mismo PR o inmediatamente después.

---

## 10. Mapa de clases (implementación Java)

Para el **desglose por directiva** (`lemma`, `pattern`), el **flujo del pipeline** y **extractos del código fuente** que implementan cada idea, usar **`docs/LogicScript/MapaImplementacionJava.md`**.

Paquete actual: `tautoteacher2.logicscript`.

| Clase | Responsabilidad | Origen / nota |
|-------|-----------------|----------------|
| `LogicScriptService` | Fachada estable hacia la UI y tests; delega en el motor. | Punto de entrada único recomendado para no acoplar consumidores a la implementación interna. |
| `LogicScriptEngine` | Orquesta: `NormalizadorTexto` → `NaturalLexer` → `SemanticMapper` (patrones `.lgs` o embebidos) → IR → `EmitidorFormula`. | Carga un único `ContenidoLgs` para lemas y patrones. |
| `LogicScriptResult` | DTO inmutable: éxito, fórmula, mensaje, `pasosDeAnalisis`, mapa texto→símbolo. | Contrato entre LogicScript y `TautoTeacherApp` / futura Explicación. |
| `RegistroProposiciones` | Asigna `p, q, r, …` a fragmentos normalizados durante una traducción. | Usado solo en la fase de emisión (`EmitidorFormula`). |
| `EmitidorFormula` | Recorre `LogicExpr` y produce la cadena con `¬`, `∧`, `∨`, `→`. | `tautoteacher2.logicscript.ir` |
| `LogicExpr` (sellada) | Contrato IR: `AtomExpr`, `NegExpr`, `AndExpr`, `OrExpr`, `ImpExpr`. | `tautoteacher2.logicscript.ir` |
| `NormalizadorTexto` | Minúsculas, sin diacríticos combinados, espacios y puntuación básica. | `tautoteacher2.nlp.normalizacion` |
| `NaturalLexer` | Tokeniza conectores frecuentes y literales sobre texto ya normalizado. | `tautoteacher2.nlp.lexer` |
| `TokenNatural`, `TipoTokenNatural` | Modelo léxico mínimo. | `tautoteacher2.nlp.lexer` |
| `BaseConocimiento` | Canonicaliza fragmentos; lemas desde `logicscript/core.lgs` o respaldo embebido. | `tautoteacher2.nlp.lexicon` |
| `ContenidoLgs` | Resultado de parsear `.lgs`: mapa de lemas + lista ordenada de `PatronSemanticoLgs`. | `tautoteacher2.nlp.lexicon` |
| `PatronSemanticoLgs` | Registro inmutable: nombre, forma (`TipoTokenNatural`), tipo IR (`imp`/`and`/`or`), índices `left`/`right`. | `tautoteacher2.nlp.lexicon` |
| `LgsCargador` | Lee `.lgs` (v0.2: `lemma`, `pattern`); `cargarConDiagnostico` → `ResultadoCargaLgs`. | `tautoteacher2.nlp.lexicon` |
| `ResultadoCargaLgs`, `EstadoCargaLgs` | Diagnóstico de carga; bloqueo del motor si hay error de sintaxis. | `tautoteacher2.nlp.lexicon` |
| `LogicScriptCli` | Demo por consola: LN → fórmula → `MotorLogico`. | `tautoteacher2.logicscript` |
| `LogicScriptRegressionHarness` | `main`: tabla de casos LN → fórmula; sale con código 1 si falla. | `tautoteacher2.logicscript`; véase `docs/LogicScript/PruebasRegresion.md` |
| `SemanticMapper` | Convierte tokens LN a IR (`LogicExpr`) con patrones desde `.lgs` o lista embebida; fallback a átomo. | `tautoteacher2.nlp.semantica` |

**Consumidor directo (no es LogicScript, pero forma parte del flujo):** `tautoteacher2.ui.TautoTeacherApp` — en modo LN llama a `LogicScriptService.traducir` y luego a `MotorLogico`.

### Clases previstas (siguiente oleada)

| Clase (planeada) | Paquete sugerido | Responsabilidad |
|------------------|------------------|-----------------|
| Cargador `.lgs` (o extensión acordada) | `tautoteacher2.logicscript` | Reglas y léxico declarativos en archivo. |

### Documentación por componente (archivos separados)

- `docs/LogicScript/BaseConocimiento.md`
- `docs/LogicScript/SemanticMapper.md`
- `docs/LogicScript/CambiosIntegracionGeneral.md`
- `docs/LogicScript/FormatoLGS.md`
- `docs/LogicScript/Lemma.md`
- `docs/LogicScript/Pattern.md` — directiva `pattern` y tokens de forma
- `docs/LogicScript/MapaImplementacionJava.md` — **trazabilidad concepto → clase Java**, lógica de cada capa y citas al código
- `docs/LogicScript/PruebasRegresion.md` — **regresión LN → fórmula** (`LogicScriptRegressionHarness`) y corrección de orden en `NaturalLexer`
- `docs/LogicScript/PaperFormalLogicScript.md` — **paper académico**: formalización del pseudo-lenguaje, BNF, semántica, justificación de `lemma`/`pattern`, comparación y arquitectura
- `docs/LogicScript/ErroresCargaLgs.md` — **diagnóstico visible** al cargar `.lgs` (`ResultadoCargaLgs`, estados, bloqueo del motor)
- `docs/LogicScript/PatronSiElliptico.md` — patrón *si X Y* sin *entonces*
- `docs/IntegracionApp/MigracionPatronesDeclarativos.md` — impacto en app y despliegue (no es especificación de sintaxis)

---

## Changelog reciente (implementación)

- **2026-04:** Directiva **`pattern`** en `core.lgs`; `LgsCargador` expone `cargarDesdeClasspath` → `ContenidoLgs`; `SemanticMapper` consume lista declarativa con respaldo embebido si no hay patrones en archivo.
- **2026-04:** **`LogicScriptRegressionHarness`**: tabla de casos LN → fórmula; ajuste en **`NaturalLexer`** (comprobar palabra clave **antes** de consumir espacios) para que *X y Y* / *X o Y* tokenicen bien. Detalle en `docs/LogicScript/PruebasRegresion.md`.
- **2026-04:** **`ResultadoCargaLgs`** y **`EstadoCargaLgs`**: errores de sintaxis en `.lgs` visibles en `LogicScriptResult`; recurso ausente → advertencia + respaldo embebido. Ver `docs/LogicScript/ErroresCargaLgs.md`.
- **2026-04:** Patrón **`SI_ELIPTICO`** (*si literal literal* → implicación sin *entonces*). Ver `docs/LogicScript/PatronSiElliptico.md`.

---

## 11. Roadmap estructural (firme y seguro)

Orden recomendado para no romper lo que ya funciona:

1. ~~**Extraer normalización**~~ → hecho: `NormalizadorTexto`.
2. ~~**Lexer mínimo**~~ → hecho: `NaturalLexer` (hoy usado sobre todo en **pasos de análisis**; el parseo principal sigue en regex hasta el mapper).
3. ~~**IR explícita**~~ → hecho: `LogicExpr` + `EmitidorFormula` + `RegistroProposiciones`.
4. **Base de conocimiento léxica** (mapa forma→lema y sinónimos de conectores). *Motivo: `llueve` vs `llueva` sin ifs dispersos.*
5. **SemanticMapper** que consuma tokens + base y produzca IR. *Motivo: sustituir regex del engine por análisis sobre tokens.*
6. ~~**Sintaxis textual**~~ → en curso: `.lgs` con `lemma` y `pattern`; ampliar con más directivas según necesidad.
7. ~~**Pruebas**~~ → harness `LogicScriptRegressionHarness` + doc `PruebasRegresion.md`. *Siguiente: JUnit/CI si se adopta build tool.*
8. ~~**Errores visibles en `.lgs`**~~ → `ResultadoCargaLgs`, bloqueo en `LogicScriptEngine`. Ver `ErroresCargaLgs.md`.
9. ~~**Patrón elíptico *si X Y***~~ → `SI_ELIPTICO` en `core.lgs`. Ver `PatronSiElliptico.md`. *Siguiente: composición con coma entre cláusulas.*

Cada hito debería ir acompañado de: actualización de **§10**, **changelog** y JavaDoc de clases nuevas o tocadas.

---

*Documento mantenido por el equipo de TautoTeacher 2.0. Las decisiones de implementación concretas prevalecen sobre este texto cuando estén explícitas en el código; en caso de conflicto, actualizar este archivo.*
