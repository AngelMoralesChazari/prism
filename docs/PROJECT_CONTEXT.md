# Prism — contexto del proyecto

**Audiencia:** desarrolladores e IAs que necesitan la visión global sin leer todo el código.  
**Actualizado:** noviembre 2026 · refleja el código en `src/main/java` (intérprete `.pr` operativo).

---

## 1. Objetivo general

Prism es un **lenguaje de programación declarativo** orientado a la enseñanza y formalización de **lógica proposicional**:

1. Aceptar un subconjunto controlado de **lenguaje natural (español)**.
2. **Formalizarlo** a una fórmula proposicional (`translate`).
3. **Evaluarlo** con tabla de verdad y dictamen (`vertauto`: tautology / contingency / contradiction).
4. Permitir también fórmulas **simbólicas** escritas a mano (`formula f = p or not p`).

Separación deliberada:

| Capa | Formato | Rol |
|------|---------|-----|
| Programas | `.pr` | Sintaxis del lenguaje (module, let, vertauto, …) |
| Conocimiento léxico | `.pr` | Lemas, lexrules, patterns (`core.pr`) |
| Motor | Java 25 | Interpreta `.pr`, carga recursos `.pr`, construye IR, evalúa |

**No es** una aplicación web, ni un ORM, ni un framework fullstack. Es un **runtime de lenguaje + herramientas CLI** sobre la JVM.

---

## 2. Funcionalidades clave (implementadas)

| Capacidad | Cómo |
|-----------|------|
| Ejecutar programas `.pr` | `java -cp out prism.pr.PrcCli run <archivo.pr>` |
| Validar sin ejecutar | `prc check` (`PrValidator` + `PrChecker`) |
| NL → fórmula | `let f = translate(nl)` o `translate("literal")` |
| Fórmula simbólica | `formula f = p impl q` |
| Evaluación lógica | `vertauto f` o `vertauto f { steps table verdict }` |
| Átomos con etiqueta | `atom p = "estudio"` → etiquetas en salida |
| Traducir frase (CLI legado) | `prism.PrismCli "..."` |
| Vertauto CLI | `prism.vertauto.VertautoCli` |
| Extensión editor | Snippets + grammar TextMate en `extensions/prism/` |

### No implementado (roadmap)

- `prc compile` → bytecode / `.class` por módulo
- `translate(x, modulo)` con archivo léxico `.pr` distinto de `core`
- Constructos `rule` / `run`
- Import de otros `.pr`
- Diagnósticos LSP en tiempo real

---

## 3. Mapa de módulos principales

### 3.1 `prism` (raíz del paquete)

| Clase | Responsabilidad |
|-------|-----------------|
| `PrismEngine` | Pipeline NL: normalizar → lexemas → patrones → IR → emitir fórmula |
| `PrismService` | Fachada de `traducir` |
| `PrismCli` | CLI de una frase |
| `RegistroProposiciones` | Asigna `p,q,r…` a fragmentos; soporta átomos declarados |
| `PrismRegressionHarness` | ~40 casos de regresión NL |

### 3.2 `prism.ir`

IR proposicional: `LogicExpr` y nodos (`AtomExpr`, `NegExpr`, `AndExpr`, `OrExpr`, `ImpExpr`, `EquivExpr`), más `EmitidorFormula`.

### 3.3 `prism.pr` — intérprete del lenguaje `.pr`

| Pieza | Rol |
|-------|-----|
| `PrcCli` | Entry point `run` / `check` / `--version` |
| `PrLexer` / `PrParser` | Tokens → `PrProgram` |
| `pr.ast.*` | AST (`AtomDecl`, `LetDecl`, `VertautoStmt`, …) |
| `PrValidator` | Análisis estático (refs, redeclaraciones, module↔archivo) |
| `PrInterpreter` | Ejecuta AST; orquesta translate + vertauto |
| `PrAtomRegistry` | Enlace `atom` ↔ etiquetas en salida |
| `PrFormulaBridge` | AST fórmula → IR |

**Este paquete es el “intérprete”.** No hay compilador `.pr` todavía.

### 3.4 `prism.vertauto`

| Pieza | Rol |
|-------|-----|
| `VertautoService` | API: clásica, expr `.pr`, desde NL |
| `EvaluadorProposicional` | Tabla de verdad + dictamen |
| `VertautoResult` | Impresión modo corto / extendido |
| Parsers | Notación clásica y sintaxis `p or not p` (`ParserFormulaPrism`) |

### 3.5 `nlp`

| Subpaquete | Rol |
|------------|-----|
| `nlp.normalizacion` | Normalizar texto de entrada |
| `nlp.lexer` | Tokens NL (`SI`, `LITERAL`, …) |
| `nlp.lexicon` | Carga léxico `.pr`, lemas, lexrules, morfología |
| `nlp.semantica` | `SemanticMapper` (patrones → IR) |

### 3.6 Recursos y ejemplos

- `src/main/resources/prism/core.pr` — léxico/patrones por defecto  
- `examples/*.pr` — programas ejecutables (`ejercicio_lluvia.pr`, `translate_literal.pr`, …)  
- `extensions/prism/` — editor, no runtime  
- `tooling/vscode/` — tareas check/run/compile (copiar a `.vscode/`)

---

## 4. Reglas de negocio / semántica críticas

1. **`module` ↔ archivo:** el identificador de `module` debe coincidir con el nombre del `.pr` (sin extensión). `prc check` y `prc run` lo validan.
2. **Un `module` por archivo;** `use` solo conoce `core` en v1 del intérprete.
3. **`translate`:** entrada Text (literal o variable `nl`); salida Formula (cadena clásica + pasos). Usa `core.pr` vía el engine.
4. **`atom`:** no es solo documentación; alimenta etiquetas de proposiciones en `vertauto` cuando hay registro de átomos.
5. **`formula`:** solo puede referenciar átomos **declarados** (validador).
6. **Redeclaración** del mismo identificador → error.
7. **Dictámenes:**  
   - `tautology` — verdadera en todas las filas  
   - `contingency` — verdaderas y falsas  
   - `contradiction` — falsa en todas  
8. **Keywords del lenguaje** son fijas; el contenido entre comillas puede ser español.
9. **Build:** `compile.ps1` compila todo `src/main/java` a `out/` y copia resources; sin classpath de terceros.

---

## 5. Flujo de un programa típico

```pr
module ejercicio_lluvia
use core
atom p = "llueve"
atom q = "paraguas"
nl linea = "si llueve llevo paraguas"
let implicacion = translate(linea)
vertauto implicacion { steps table verdict }
```

1. Parse → AST  
2. Validar usos y referencias  
3. `translate` → `(p → q)` (+ pasos NL)  
4. `vertauto` → tabla 4 filas → **contingency**  
5. Proposiciones mostradas con etiquetas de `atom` (`llueve=p`, …)

---

## 6. Cómo construir y probar

```powershell
.\compile.ps1
java -cp out prism.pr.PrcCli run examples/ejercicio_lluvia.pr
java -cp out prism.pr.PrcCli check examples/ejercicio_lluvia.pr
java -cp out prism.pr.PrInterpreterHarness
java -cp out prism.pr.PrCheckHarness
java -cp out prism.vertauto.VertautoHarness
java -cp out prism.PrismRegressionHarness
```

---

## 7. Documentación relacionada

| Ubicación | Contenido | ¿En git? |
|-----------|-----------|----------|
| `docs/PROJECT_CONTEXT.md` | Este archivo | Sí (objetivo) |
| `.cursor/rules/*.mdc` | Reglas para agentes Cursor | Sí |
| `.cursorrules` | Resumen global | Sí |
| `documentacion Prism/` | Specs largas, guías, snippets vivos | **No** (gitignore) |
| `README.md` | Intro pública | Sí |

Al añadir capacidades del lenguaje o snippets, actualizar este contexto y el documento vivo local de autocompletado si aplica.

---

## 8. Visión vs estado

| Visión | Estado |
|--------|--------|
| Lenguaje `.pr` usable | Operativo (Intérprete + check) |
| Compilación a JVM | Pendiente |
| Lexicón declarativo `.pr` | Operativo (`core.pr` + cargador) |
| Extensión de editor | Operativo (highlight + snippets) |
| Tooling Maven | Operativo (`pom.xml`) |
