# Bitácora de Investigación y Mejoras del Motor NLP — Prism
**Documento de soporte para publicación académica y tesis**  
**Proyecto:** Prism (Lenguaje Declarativo de Dominio Específico para Lógica Proposicional)  
**Fecha:** Noviembre 2026  

---

## Índice de Mejoras Enumeradas

1. [Mejora #1: Delimitación Léxica de Cláusulas Explícitas frente a Elípticas](#mejora-1-delimitación-léxica-de-cláusulas-explícitas-frente-a-elípticas)
2. [Mejora #2: Extracción de Negación Verbal Intra-Predicativa](#mejora-2-extracción-de-negación-verbal-intra-predicativa)
3. [Mejora #3: Parser Jerárquico Recursivo de Conectores Discursivos Mayores](#mejora-3-parser-jerárquico-recursivo-de-conectores-discursivos-mayores)
4. [Mejora #4: Extensión del Léxico Declarativo para Sustantivos Técnicos Computacionales](#mejora-4-extensión-del-léxico-declarativo-para-sustantivos-técnicos-computacionales)
5. [Caso de Estudio Canónico: Demostración de Equivalencia Contrapositiva](#caso-de-estudio-canónico)

---

### Mejora #1: Delimitación Léxica de Cláusulas Explícitas frente a Elípticas

- **Componente:** `nlp.lexer.NaturalLexer` (`NaturalLexer.java`)
- **Problema formal identificado:**
  El analizador léxico original asumía que toda secuencia posterior al token `SI` pertenecía a una construcción condicional elíptica informal (p. ej. *"si estudio apruebo"*), fragmentando indiscriminadamente cualquier literal de tres o más palabras en dos tokens independientes: la primera palabra y el resto del enunciado. 
  Al ingresar una oración técnica como *"si el proceso no libera la memoria entonces el sistema colapsa"*, el tokenizador generaba:
  $$\text{Tokens} = [\text{SI}, \text{LITERAL("el")}, \text{LITERAL("proceso no libera la memoria")}, \text{ENTONCES}, \dots]$$
  Esto rompía el patrón gramatical canónico `SI LITERAL ENTONCES LITERAL`, obligando al mapper semántico a degradar la fórmula a un átomo trivial de fallback ($p = \text{"el"}$).

- **Fundamento teórico:**
  En lingüística computacional y teoría de analizadores sintácticos para español:
  1. **Condicional Explícito:** La presencia de la partícula correlativa *entonces* (o de una pausa ortográfica delimitadora `,`) define inequívocamente que todo el segmento comprendido entre `si` y `entonces` constituye una única proposición atómica/antecedente, sin importar su longitud en palabras.
  2. **Condicional Elíptico:** La omisión de *entonces* y de comas (p. ej. *"si llueve llevo paraguas"*) es el único caso donde la heurística de segmentación verbal debe inferir la frontera entre el verbo del antecedente y el predicado consecuente.

- **Solución implementada:**
  Se introdujo la función de anticipación léxica (*lookahead*) `hayEntoncesSiguiente(s, j)`. Al escanear el literal que sigue a `SI`:
  - Si en el flujo subsecuente de la cláusula se detecta el conector `ENTONCES` o el delimitador de coma `,`, se activa la bandera `delimitadoExplicito = true`.
  - El literal se emite completo e íntegro como un único `TokenNatural(LITERAL, "el proceso no libera la memoria")`.
  - La fragmentación heurística se restringe estrictamente a oraciones genuinamente elípticas carentes de conectores correlativos.

---

### Mejora #2: Extracción de Negación Verbal Intra-Predicativa

- **Componente:** `nlp.semantica.SemanticMapper` (`SemanticMapper.java`)
- **Problema formal identificado:**
  La detección de negaciones solo verificaba si la proposición iniciaba exactamente con el prefijo `"no "` (`limpio.startsWith("no ")`). En la sintaxis del español, cuando una proposición contiene sujeto explícito (*"el proceso no libera la memoria"*, *"el sistema no colapsa"*), el adverbio de negación se ubica entre el núcleo del sujeto y el verbo principal ($\text{Sujeto} + \text{no} + \text{Verbo} + \text{Objeto}$). 
  Como resultado, la frase negativa se registraba como un átomo positivo nuevo e inconexo, impidiendo que el motor unificara la variable lógica con su contraparte afirmativa.

- **Fundamento teórico:**
  Sea una proposición atómica $\phi$ cuyo predicado nuclear es $P(x)$. La afirmación es $P(x)$ y su negación lógica formal es $\neg P(x)$. Para que el motor deductivo opere de forma consistente, ambas formas deben proyectarse sobre la **misma clave canónica en la base de conocimiento léxica**, variando únicamente el operador unario de negación en el término de la Representación Intermedia ($\mathcal{IR}$):
  $$\text{AtomExpr}(\text{clave}=\lambda(\phi), \text{negada}=\text{true}) \implies \neg p$$

- **Solución implementada:**
  Se actualizó el método `atomoDesde(String fragmentoOriginal)` utilizando expresiones regulares con fronteras de palabra (`\bno\b`):
  1. Si la cadena contiene el adverbio de negación aislado en cualquier posición del predicado, se marca la bandera booleana `negada = true`.
  2. Se extirpa el adverbio `no` preservando el orden y los complementos verbales.
  3. El resto afirmativo se somete a la función canónica de lematización $\lambda$:
     - *"el proceso no libera la memoria"* $\to$ extrae `no` $\to$ canoniza *"proceso liberar la memoria"* $\to$ asigna $\neg p$.
     - *"el proceso libera la memoria"* $\to$ sin `no` $\to$ canoniza *"proceso liberar la memoria"* $\to$ asigna $p$.
  4. Ambas apariciones comparten exactamente el mismo símbolo atómico $p$.

---

### Mejora #3: Parser Jerárquico Recursivo de Conectores Discursivos Mayores

- **Componente:** `prism.PrismEngine` (`PrismEngine.java`)
- **Problema formal identificado:**
  El pipeline original de `PrismEngine` asumía una estructura plana donde toda la oración debía encajar en una única regla de sustitución, o bien dividirse únicamente por comas de conjunción ($\land$). No existía un parser con capacidad de evaluar **conectores proposicionales de orden superior** como el bicondicional (*"si y solo si"* / $\leftrightarrow$) cuando este vincula dos proposiciones que son a su vez sentencias condicionales compuestas.

- **Fundamento teórico:**
  Gramática libre de contexto para el lenguaje de discurso proposicional:
  $$E \longrightarrow E \longleftrightarrow E \mid E \land E \mid E \lor E \mid C$$
  $$C \longrightarrow \mathbf{si}\; L \;\mathbf{entonces}\; L \mid \mathbf{si}\; L\; L \mid L$$
  El operador bicondicional $\longleftrightarrow$ posee menor precedencia discursiva que la implicación interna $\rightarrow$ de cada cláusula, permitiendo estructurar árboles sintácticos de la forma:
  $$\text{EquivExpr}\Big(\text{ImpExpr}(\alpha_1, \beta_1),\; \text{ImpExpr}(\alpha_2, \beta_2)\Big)$$

- **Solución implementada:**
  Se sustituyó la ejecución plana por el método recursivo `traducirJerarquico(String texto, List<String> pasos)`:
  1. **Nivel 1 (Bicondicional Mayor):** Detecta la presencia de *"si y solo si"* como partición mayor. Divide la entrada en dos sub-oraciones (Lado Izquierdo y Lado Derecho) y las procesa recursivamente, retornando un nodo `EquivExpr(izq, der)`.
  2. **Nivel 2 (Conjunción de Cláusulas):** Divide por delimitadores correlativos de premisas (`, si `, `, cuando `) retornando árboles `AndExpr`.
  3. **Nivel 3 (Cláusula Atómica/Condicional):** Resuelve el árbol léxico-semántico de la cláusula individual mediante `SemanticMapper`.

---

### Mejora #4: Extensión del Léxico Declarativo para Sustantivos Técnicos Computacionales

- **Componente:** `src/main/resources/prism/core.pr`
- **Problema formal identificado:**
  Las reglas heurísticas de morfología declarativa (`lexrule`) incluían transformaciones universales como `lexrule sufijo a infinitivo ar` y `lexrule sufijo o heuristica primera_persona`. Esto provocaba que sustantivos comunes como *sistema*, *memoria* y *proceso* sufrieran sobre-regularización morfológica hacia verbos inventados (*sistemar*, *memoriar*, *procesar*).

- **Fundamento teórico:**
  En morfología declarativa, las reglas de exclusión poseen prioridad sobre las reglas de sufijación derivativa para proteger el vocabulario terminológico propio del dominio del discurso (lexicografía controlada).

- **Solución implementada:**
  Se incorporaron a la directiva declarativa `lexrule excluir` de `core.pr` los sustantivos del ámbito computacional:
  ```text
  lexrule excluir ... proceso sistema memoria datos red archivo servidor usuario
  ```
  Con esto, *sistema*, *memoria* y *proceso* mantienen su integridad léxica mientras que los verbos *libera* y *colapsa* se lematizan adecuadamente a sus formas infinitivas *liberar* y *colapsar*.

---

### Caso de Estudio Canónico

#### 1. Enunciado en Lenguaje Natural:
> *"si el proceso no libera la memoria entonces el sistema colapsa si y solo si si el sistema no colapsa entonces el proceso libera la memoria"*

#### 2. Derivación Léxica y Sintáctica:
- **Partición Jerárquica:**
  - **Sub-cláusula $\alpha$:** `"si el proceso no libera la memoria entonces el sistema colapsa"`
  - **Conector mayor:** $\longleftrightarrow$ (`si y solo si`)
  - **Sub-cláusula $\beta$:** `"si el sistema no colapsa entonces el proceso libera la memoria"`
- **Mapeo Atómico:**
  - $p$: *"proceso liberar la memoria"*
  - $q$: *"sistema colapsar"*
- **Construcción de Términos IR:**
  - $\alpha \Longrightarrow (\neg p \rightarrow q)$
  - $\beta \Longrightarrow (\neg q \rightarrow p)$
  - Término Final $\Longrightarrow ((\neg p \rightarrow q) \longleftrightarrow (\neg q \rightarrow p))$

#### 3. Evaluación Lógica Formal (`vertauto`):
Espacio de estados generado para $2^2 = 4$ asignaciones de verdad:

| $p$ | $q$ | $\neg p$ | $\neg p \rightarrow q$ | $\neg q$ | $\neg q \rightarrow p$ | $((\neg p \rightarrow q) \longleftrightarrow (\neg q \rightarrow p))$ |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **F** | **F** | V | **F** | V | **F** | **V** |
| **F** | **V** | V | **V** | F | **V** | **V** |
| **V** | **F** | F | **V** | V | **V** | **V** |
| **V** | **V** | F | **V** | F | **V** | **V** |

**Dictamen:** `tautology` (Demostración de equivalencia lógica por contraposición deductiva).

#### 4. Programa Ejecutable en Prism (`examples/prueba_proceso_memoria.pr`):
```prism
module prueba_proceso_memoria

use core

atom p = "proceso liberar la memoria"
atom q = "sistema colapsar"

nl premisa = "si el proceso no libera la memoria entonces el sistema colapsa si y solo si si el sistema no colapsa entonces el proceso libera la memoria"
let equivalencia = translate(premisa)

vertauto equivalencia {
    steps
    table
    verdict
}
```
**Resultado de ejecución:**
```text
OK: examples/prueba_proceso_memoria.pr -> tautology (100% verificado)
```
