# LogicScript: propuesta formal de un pseudo-lenguaje declarativo para el puente entre lenguaje natural y lógica proposicional

**Documento tipo paper (curso de Lenguajes Formales y Autómatas / Ciencias de la Computación)**  
**Contexto:** implementación en el proyecto *TautoTeacher 2.0* (Java).  
**Versión del artefacto textual:** `.lgs` v0.2.

---

## Resumen

Se presenta **LogicScript** no solo como utilidad de software, sino como **pseudo-lenguaje** cuya especificación emerge de la **abstracción inversa** sobre un sistema implementado: de clases Java, estructuras de datos y archivos de configuración hacia una descripción que podría evaluarse como **lenguaje formal de especificación** (léxico, reglas de formación, semántica operacional de traducción). Se argumenta que LogicScript separa **conocimiento lingüístico declarativo** de **motor de evaluación proposicional**, formalizando el flujo español normalizado → tokens → reglas de patrón → representación intermedia (IR) → cadena clásica → tablas de verdad. Se incluyen gramáticas estilo BNF para el sublenguaje de archivo `.lgs` y para la IR, y se justifica el término **`lemma`** en relación con morfología computacional y lógica informal. Donde el código aún no fija un aspecto, se **propone** una extensión coherente (§8).

**Palabras clave:** lenguajes formales, DSL, lógica proposicional, representación intermedia, lexer, patrones semánticos, lematización.

---

## 1. Origen del lenguaje y problema que resuelve

### 1.1 Motivación

La **lógica proposicional clásica** suele enseñarse con **símbolos abstractos** \(p, q, r\) y conectivas \((\neg, \land, \lor, \rightarrow)\). Los estudiantes, en cambio, razonan a menudo en **lenguaje natural**, donde:

- las **mismas proposiciones** aparecen bajo **variantes morfológicas** (*llueve*, *llueva*, *llovió*);
- los **conectores** son **léxicamente diversos** (*si… entonces…*, *en caso de que…*, *y*, *o*) y **sintácticamente flexibles** (*Y si X* frente a *Si X entonces Y*);
- la **superficie textual** no coincide con un árbol sintáctico proposicional único sin reglas explícitas.

**Limitación pedagógica y de ingeniería:** si el evaluador proposicional exige directamente una cadena como \((p \rightarrow q)\), entonces o bien la **interfaz** codifica frases en `if` dispersos, o bien el **motor lógico** queda **acoplado al español**. Ambas opciones dificultan mantenimiento, extensión y **trazabilidad** (explicar *qué regla* produjo *qué fórmula*).

**LogicScript** se concibe como **capa intermedia declarativa**: un **contrato** entre (i) descripción **explícita** de equivalencias y plantillas lingüísticas y (ii) un **evaluador** que solo conoce fórmulas en notación simbólica. Así se **formaliza el diseño** del puente, no solo su uso.

### 1.2 Qué *no* pretende ser

LogicScript **no** es un lenguaje de programación de propósito general ni un cálculo de predicados completo. Es un **DSL de especificación y transformación** orientado a un subconjunto controlado de construcciones españolas y a la lógica proposicional finitaria que ya implementa el componente `MotorLogico` del sistema.

---

## 2. De la implementación Java al pseudo-lenguaje: abstracción y correspondencia

La tesis central de este documento es que **cada bloque implementado en Java puede leerse como la semántica operacional de un constructor del pseudo-lenguaje LogicScript**. La tabla siguiente resume la **morfología implementación → concepto lingüístico formal**.

| Artefacto Java | Paquete / rol | Concepto en LogicScript |
|----------------|---------------|-------------------------|
| `NormalizadorTexto` | `nlp.normalizacion` | **Preprocesador léxico** del canal español: homogeneiza alfabeto y espacios para que el vocabulario del `.lgs` y el lexer compartan un **universo de cadenas**. |
| `NaturalLexer`, `TipoTokenNatural`, `TokenNatural` | `nlp.lexer` | **Alfabeto de tokens de superficie** \(T_{\mathrm{LN}}\): conectores fijos + metasímbolo `LITERAL` para fragmentos no analizados como conector. |
| `LgsCargador`, `ContenidoLgs` | `nlp.lexicon` | **Módulo de especificación externa**: lectura de un **documento LogicScript** (extensión `.lgs`) que instancia **lexicon** (`lemma`) y **gramática de patrones** (`pattern`). |
| `PatronSemanticoLgs`, `TipoSalidaIrPatron` | `nlp.lexicon` | **Reglas de producción semántica** indexadas: una secuencia de tipos en \(T_{\mathrm{LN}}\) se asocia a un **constructor IR** (`imp`, `and`, `or`) y a **posiciones de argumentos** (análogo a acciones semánticas `$i` en Yacc). |
| `BaseConocimiento` | `nlp.lexicon` | **Función de lematización** \(\lambda: \Sigma^* \to \Sigma^*\) palabra a palabra, inducida por el mapa `lemma`. |
| `SemanticMapper` | `nlp.semantica` | **Intérprete de patrones**: recorre la lista ordenada de reglas; la primera coincidencia **deriva** un término IR. |
| `LogicExpr` (sellada), `AtomExpr`, `ImpExpr`, … | `logicscript.ir` | **Álgebra de términos** \(\mathcal{E}\): **sintaxis abstracta** de LogicScript a nivel lógico (independiente del español). |
| `EmitidorFormula`, `RegistroProposiciones` | `logicscript` / `ir` | **Homomorfismo sintáctico** \(\mathcal{E} \to \mathcal{F}\), donde \(\mathcal{F}\) es el conjunto de cadenas bien formadas del **lenguaje de fórmulas** del motor. |
| `LogicScriptEngine`, `LogicScriptService` | `logicscript` | **Orquestación**: composición de fases como **pipeline** (analogía: compilador con front-end LN e IR compartida). |
| `MotorLogico` | `core.logica` | **Semántica denotacional estándar** vía tablas de verdad sobre la cadena emitida (externa al núcleo LogicScript pero **objetivo final** de la traducción). |

**Proceso de abstracción:** el código sugiere que LogicScript posee **dos sublenguajes acoplados**:

1. **LGS** (LogicScript Grammar Surface, archivo `.lgs`): notación textual para **datos lingüísticos** (`lemma`, `pattern`).
2. **IR-LS** (Intermediate Representation LogicScript): árbol finito sobre firmas \(\{\mathrm{atom}, \neg, \land, \lor, \rightarrow\}\) materializado como tipos Java sellados.

La **“sintaxis del pseudo-lenguaje”** en sentido amplio es la **unión** de las cadenas aceptadas por el pipeline más la notación `.lgs`, aunque solo la segunda esté serializada como archivo con extensión propia.

---

## 3. Definición formal (enfoque BNF y reglas de formación)

### 3.1 Alfabetos y conjuntos auxiliares

- Sea \(\Sigma\) el alfabeto Unicode UTF-8 restringido por la normalización (texto en minúsculas sin marcas diacríticas combinadas en la práctica del normalizador).
- **Tokens de lenguaje natural analizado** (salida del lexer):
  \[
  T_{\mathrm{LN}} = \{\texttt{SI}, \texttt{ENTONCES}, \texttt{EN\_CASO\_DE\_QUE}, \texttt{Y}, \texttt{O}, \texttt{LITERAL}\}.
  \]
- **Metavariables del archivo `.lgs`:**
  - \(\textit{ident}\): identificador alfanumérico para nombre de patrón.
  - \(\textit{forma}\): secuencia de **símbolos de tipo** pertenecientes al vocabulario \(\{\texttt{si}, \texttt{entonces}, \texttt{en\_caso\_de\_que}, \texttt{y}, \texttt{o}, \texttt{literal}\}\) (lexema en archivo; mapeo a \(T_{\mathrm{LN}}\) biyectivo salvo normalización de guiones).
  - \(\textit{tipoIr} \in \{\texttt{imp}, \texttt{and}, \texttt{or}\}\) (con alias documentados en el cargador).

### 3.2 Gramática del documento `.lgs` (versión v0.2, estilo BNF)

Se describe a nivel de **línea** (el analizador real es lineal; comentarios y vacías se filtran).

```bnf
<documento>   ::= <linea>*
<linea>       ::= <vacía> | <comentario> | <version> | <lemma> | <pattern>

<vacía>       ::= /* cadena vacía o solo espacios */
<comentario>  ::= '#' <cualquier_texto_hasta_fin_de_línea>
<version>     ::= 'version' <texto_libre>

<lemma>       ::= 'lemma' <WS> <forma_lexica> <WS> '->' <WS> <lema_canónico>
<forma_lexica>::= /* secuencia de caracteres no vacía, sin salto de línea */
<lema_canónico>::= /* idem */

<pattern>     ::= 'pattern' <WS> <ident> <WS> <lista_tipos> <WS> '=>' <WS> <cuerpo_salida>
<lista_tipos> ::= <tipo_token> (<WS> <tipo_token>)*
<tipo_token>  ::= 'si' | 'entonces' | 'en_caso_de_que' | 'y' | 'o' | 'literal'

<cuerpo_salida> ::= <tipoIr> <WS> 'left=' <entero> <WS> 'right=' <entero>
<tipoIr>      ::= 'imp' | 'and' | 'or' | /* alias permitidos por implementación */
```

**Restricciones semánticas estáticas** (no capturadas puramente por BNF anterior):

- En `pattern`, los índices `left` y `right` deben ser **válidos** en la lista de tipos y apuntar a posiciones cuyo tipo es `literal` (validado en `PatronSemanticoLgs`).
- Para `imp`, la implementación interpreta `left` como **índice del antecedente** y `right` como **índice del consecuente** en la construcción de la implicación material.

### 3.3 Gramática de la IR (sintaxis abstracta)

Sea \(\mathcal{E}\) el conjunto de expresiones generado por:

```bnf
<E> ::= <átomo>
      | 'Neg' '(' <E> ')'
      | 'And' '(' <E> ',' <E> ')'
      | 'Or'  '(' <E> ',' <E> ')'
      | 'Imp' '(' <E> ',' <E> ')'

<átomo> ::= Atom( w , b )
```

donde \(w\) es una cadena (fragmento normalizado / canónico) y \(b \in \{\bot, \top\}\) modela **negación superficial** del átomo en la implementación actual (`AtomExpr` con bandera `negada`). A nivel Java, esto corresponde a la interfaz sellada `LogicExpr` con variantes `AtomExpr`, `NegExpr`, `AndExpr`, `OrExpr`, `ImpExpr`.

### 3.4 Lenguaje de fórmulas emitidas (\(\mathcal{F}\))

El emisor produce cadenas sobre un alfabeto que incluye símbolos proposicionales \(\{p, q, r, \ldots\}\), paréntesis y conectivas Unicode **materializadas** como en el código: `¬`, `∧`, `∨`, `→` (y eventualmente `↔` si se extiende el emisor). La **jerarquía** se fija por paréntesis explícitos en la emisión recursiva (`EmitidorFormula`).

---

## 4. Semántica y evaluación

### 4.1 Semántica de traducción (español normalizado → IR)

Definimos una **relación de derivación por patrones** (no determinismo acotado por **orden** de la lista de reglas):

- Entrada: secuencia finita \(\tau = t_1 t_2 \ldots t_n\) con \(t_i \in T_{\mathrm{LN}}\) y lexemas asociados para cada token.
- Para cada regla \(R_k\) de forma \(\langle A_1, \ldots, A_n \rangle\): si \(\mathrm{type}(t_i) = A_i\) para todo \(i\), entonces **dispara** \(R_k\) y construye \(e \in \mathcal{E}\) según `tipoIr` y los lexemas en posiciones `left` y `right`, aplicando después **lematización** palabra a palabra al construir átomos (`BaseConocimiento`).

Si ninguna regla coincide, la implementación aplica **estrategias de respaldo** (primer literal o texto completo como átomo), lo cual es **semántica operacional parcial** documentada.

### 4.2 Semántica de la IR → cadena (\(\mathcal{E} \to \mathcal{F}\))

`EmitidorFormula` define un **homomorfismo** estructural: cada constructor IR tiene imagen fija en términos de símbolos y conectivas. `RegistroProposiciones` define una función **inyectiva en la práctica** (saldo colisiones) de **claves textuales** (fragmentos canónicos) a **símbolos** \(p, q, \ldots\), estable para una misma ejecución.

### 4.3 Semántica del motor lógico (denotacional clásica)

Dada \(F \in \mathcal{F}\), `MotorLogico` interpreta conectivas según la semántica estándar de lógica proposicional y **exhaustión** de interpretaciones sobre el conjunto finito de variables libres. Así:

- **Tautología:** valor verdadero bajo todas las valuaciones.
- **Contradicción:** falso bajo todas.
- **Contingencia:** mixto.

Esta capa **no** interpreta español; recibe solo \(\mathcal{F}\). La **composicionalidad** del sistema es:  
\(\llbracket \text{español} \rrbracket_{\mathrm{LS}} = \llbracket \; \mathcal{G}(\mathcal{T}(\text{normalizado})) \; \rrbracket_{\mathrm{val}}\)  
donde \(\mathcal{T}\) es traducción a IR y \(\mathcal{G}\) emisión a fórmula.

---

## 5. Justificación de decisiones de diseño

### 5.1 Operadores y símbolos en la fórmula emitida

Se adoptan **símbolos universitarios** (`¬`, `∧`, `∨`, `→`) por:

- **Congruencia** con textos de lógica en español e inglés.
- **Desambiguación** frente a ASCII sobrecargado (`&`, `|`).
- **Separación** del metalenguaje `.lgs` (ASCII, palabras clave en minúsculas) y del **objeto lógico** (Unicode).

### 5.2 Estructura general: datos `.lgs` + código Java

**Justificación:** separar **especificación** de **motor** es el mismo principio que en **interpretación abstracta** y **DSL externos**: el docente o ingeniero puede auditar **lemma** y **pattern** sin leer el grafo de llamadas completo. Cumple **modularidad** y **verificabilidad** (criterios de ingeniería de lenguajes).

### 5.3 Uso de `lemma` y el término “lemma”

**Origen conceptual:** en **lingüística computacional** y NLP, *lemma* (lema) es la **forma canónica** de un lexema (p. ej. infinitivo verbal). No es un término primitivo de la **lógica matemática** en el sentido de *lemma* como proposición auxiliar en una demostración (aunque la palabra inglesa es homónima). En LogicScript se usa en el **primer sentido**: **normalización morfológica superficial** hacia un **predicado estable** para la asignación de símbolos proposicionales.

**Ventajas:**

- **Claridad:** el docente reconoce el paralelo con “lematización” en corpus.
- **Modularidad:** el mapa `forma -> lema` es un **fragmento finito** de una relación de equivalencia sobre palabras.
- **Evaluación lógica:** al unificar superficies bajo un lema, se evita que *llueve* y *llueva* generen **variables proposicionales distintas** sin motivo pedagógico.

**Notación `lemma` … `->` …:** la flecha sugiere **reducción** o **rewrite** \(\textit{forma} \Rightarrow \textit{lema}\), alineada con reglas de **sistemas de reescritura** (aunque aquí solo se aplique palabra a palabra).

### 5.4 `pattern` y los índices `left` / `right`

**Justificación teórica:** los patrones son **producciones de longitud fija** sobre el alfabeto de tokens \(T_{\mathrm{LN}}\) con **acción semántica** que extrae subcadenas en posiciones dadas. Es isomorfo al enfoque de **atributos posicionales** en compiladores (cf. `$1`, `$2` en Yacc). El orden lineal de reglas impone **prioridad determinista**, evitando ambigüedad no resuelta.

### 5.5 Interfaz sellada `LogicExpr` en Java

**Justificación:** el sellado (`sealed interface`) fuerza **exhaustividad** en el emisor y en futuros análisis: es el análogo de definir \(\mathcal{E}\) como **tipo algebraico cerrado** en teoría de lenguajes y semántica denotacional.

---

## 6. Comparación conceptual

| Aspecto | Notación lógica clásica | LogicScript |
|---------|-------------------------|-------------|
| Entrada | Fórmula simbólica | Español (subconjunto) + especificación `.lgs` |
| Conectores | Símbolos fijos | Multitud de **realizaciones léxicas** mapeadas a pocos constructores IR |
| Variables | \(p, q\) dadas | **Generadas** por registro a partir de contenido semántico (lemas) |
| Extensión | Nuevos símbolos | Nuevas filas `lemma` / `pattern` (+ posible extensión del lexer) |

**Sistemas relacionados:**

- **Prolog / bases de reglas:** LogicScript comparte la **filosofía** de separar hechos/reglas del motor, pero **no** implementa unificación ni backtracking.
- **GF, Grammatical Framework (idea general):** separación entre **abstract syntax** y **concrete syntax**; aquí la IR juega el papel de sintaxis abstracta mínima y el español + `.lgs` el de concreciones parciales.
- **Compiladores:** pipeline lexer → reglas con acciones → AST → backend; LogicScript replica **parcialmente** esa arquitectura con propósito pedagógico.

**Innovación relativa (en el marco del proyecto):** la **coespecificación** en `.lgs` de **morfología léxica** y **patrones sintácticos de superficie** en un solo artefacto versionado, enlazada a trazas de análisis en la salida del servicio.

---

## 7. Arquitectura conceptual del sistema

```text
                    ┌─────────────────────┐
  Texto español ──► │ NormalizadorTexto   │──► cadena normalizada
                    └──────────┬──────────┘
                               ▼
                    ┌─────────────────────┐
                    │ NaturalLexer        │──► secuencia TokenNatural
                    └──────────┬──────────┘
                               ▼
        ┌──────────────────────┴──────────────────────┐
        │ core.lgs (LogicScript superficial)          │
        │  lemma  → BaseConocimiento                  │
        │  pattern → lista PatronSemanticoLgs         │
        └──────────────────────┬──────────────────────┘
                               ▼
                    ┌─────────────────────┐
                    │ SemanticMapper      │──► LogicExpr (IR)
                    └──────────┬──────────┘
                               ▼
                    ┌─────────────────────┐
                    │ EmitidorFormula     │──► cadena en 𝒻
                    │ + RegistroProps.    │
                    └──────────┬──────────┘
                               ▼
                    ┌─────────────────────┐
                    │ MotorLogico         │──► tautología / tipo / explicación
                    └─────────────────────┘
```

**Papel del “parser”:** no hay un parser LR único; hay **dos niveles**: (1) lexer sobre cadena; (2) **matching** de patrones sobre la secuencia de tipos. El conjunto es **reconocible** por un autómata/producto finito si se acota longitud de literales como símbolos opacos (modelo habitual en compilación).

**Evaluador:** escindido en **traducción** (LogicScript) y **semántica booleana** (`MotorLogico`).

---

## 8. Aspectos no completamente cerrados en la implementación y extensiones propuestas

Donde el texto del código o la documentación operativa no cierra el formalismo, se proponen **extensiones coherentes** (para evaluación académica o iteración futura):

1. **Errores en `.lgs`:** hoy un fallo de lectura puede degradar a bundle vacío sin informar; propuesta: **tipo de resultado** `Either<Error, ContenidoLgs>` o mensaje en `LogicScriptResult` con número de línea (semántica de **diagnóstico** formal).
2. **Gramática de patrones de longitud variable:** introducir metasímbolo `literal+` o reglas con **comodín** con restricción de no ambigüedad; semántica: cola de tokens y unificación ligera.
3. **`NegExpr` en IR sin patrón `.lgs`:** añadir `pattern` con constructor `neg` o reglas de ámbito del literal; justificación: completar \(\mathcal{E}\) respecto a la negación interna.
4. **Bicondicional `↔`:** extender `TipoSalidaIrPatron` y `EmitidorFormula` con ecuivalencia; reglas naturales en español (*si y solo si*, *cuando y solo cuando*).
5. **Semántica composicional de bloques:** la división por `, en caso de que` en `LogicScriptEngine` es **hardcodeada**; propuesta: directiva `split` o `compose` en `.lgs` que liste **delimitadores de bloque** como producciones del pseudo-lenguaje.

---

## 9. Conclusión

LogicScript puede presentarse como **pseudo-lenguaje formal** compuesto por: (i) un **lenguaje superficial** `.lgs` con producciones `lemma` y `pattern`; (ii) un **cálculo de términos** \(\mathcal{E}\) materializado en Java como IR sellada; (iii) un **homomorfismo** hacia el lenguaje de fórmulas \(\mathcal{F}\) evaluable clásicamente. La implementación Java **no es mero detalle**: es la **semántica operacional** que ancla la especificación y permite a un evaluador de lenguajes formales **verificar coherencia** entre sintaxis declarada, reglas de traducción y semántica proposicional.

---

## Referencias internas al proyecto

- `docs/LogicScript.md` — visión general y filosofía.  
- `docs/LogicScript/Pattern.md` — índices y analogía con compiladores.  
- `docs/LogicScript/MapaImplementacionJava.md` — trazabilidad clase ↔ concepto.  
- `docs/LogicScript/FormatoLGS.md`, `Lemma.md`, `PruebasRegresion.md`.

---

*Documento elaborado para revisión académica. Si el tribunal exige una formalización aún más estricta (p. ej. semántica denotacional completa de \(\mathcal{E}\) en dominios booleanos con función de interpretación explícita), puede añadirse como apéndice matemático sobre el modelo finito de `MotorLogico`.*
