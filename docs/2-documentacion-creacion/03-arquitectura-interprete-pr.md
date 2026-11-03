# Implementación del intérprete `.pr` — Prism v1.0

**Fecha:** junio 2026  
**Relacionado con:** `Language-v1.0.md`, `VISION-Y-DEFINICION-GENERAL.md`, `IMPLEMENTACION-VERTAUTO-FASE2.md`  
**Estado:** intérprete mínimo operativo (`prc run`)

---

## 1. Resumen

El paquete `prism.pr` implementa el **pipeline fuente → tokens → AST → ejecución** para programas `.pr`. Reutiliza el runtime existente:

| Capacidad runtime | Clase |
|-------------------|-------|
| `translate` (NL → fórmula) | `PrismService` / `PrismEngine` |
| `vertauto` (evaluación + tabla) | `VertautoService` / `EvaluadorProposicional` |

Los archivos `examples/*.pr` dejan de ser solo documentación y se ejecutan con:

```powershell
.\compile.ps1
java -cp out prism.pr.PrcCli run examples/ejercicio_lluvia.pr
```

---

## 2. Arquitectura

```
archivo.pr
    │
    ▼
LsSourceLoader.leer()
    │
    ▼
PrLexer ──► List<LsToken>
    │
    ▼
PrParser ──► LsProgram (AST)
    │
    ▼
PrInterpreter.ejecutar()
    ├── use core     → validación (core.lgs ya cargado por el motor)
    ├── nl / atom    → entorno de símbolos
    ├── let translate→ PrismService.traducir()
    ├── formula      → LsFormulaBridge → LogicExpr
    └── vertauto     → VertautoService → salida stdout
```

### 2.1 Paquetes y archivos

```
src/main/java/logicscript/ls/
├── LsTokenType.java          # enum de tipos léxicos
├── LsToken.java              # record (tipo, lexema, línea)
├── PrLexer.java              # analizador léxico
├── LsParseException.java     # error de sintaxis
├── PrParser.java             # analizador sintáctico (descenso recursivo)
├── LsFormulaBridge.java      # AST fórmula → LogicExpr / cadena mostrada
├── PrInterpreter.java        # evaluador del programa
├── LsSourceLoader.java       # lectura UTF-8 del archivo
├── LsRuntimeException.java   # error en tiempo de ejecución
├── LscCli.java               # punto de entrada CLI
├── PrInterpreterHarness.java # pruebas de regresión del intérprete
└── ast/
    ├── LsProgram.java        # raíz: module + uses + statements
    ├── LsStmt.java           # interfaz sellada de sentencias
    ├── AtomDecl, NlDecl, LetDecl, FormulaDecl, VertautoStmt
    ├── TextRef, TranslateCall
    ├── FormulaExpr, AtomRef, NotExpr, BinaryExpr, BinaryOp
    ├── FormulaRef, VertautoOptions
    └── ...
```

---

## 3. Lexer (`PrLexer`)

### 3.1 Comportamiento

- Ignora espacios, tabuladores y saltos de línea.
- Comentarios de línea: `#` hasta fin de línea.
- Palabras reservadas reconocidas por texto exacto (case-sensitive).
- Cadenas entre comillas dobles `"..."` con escapes básicos: `\n`, `\t`, `\"`, `\\`.
- Identificadores: letra inicial, luego letras, dígitos o `_`.

### 3.2 Palabras reservadas

`module`, `use`, `atom`, `formula`, `nl`, `let`, `translate`, `vertauto`,  
`not`, `and`, `or`, `impl`, `iff`, `steps`, `table`, `verdict`

### 3.3 Puntuación

`=`, `:`, `,`, `(`, `)`, `{`, `}`

---

## 4. Parser (`PrParser`)

Gramática implementada (subset v1.0 de `Language-v1.0.md`):

```ebnf
program        ::= 'module' identifier { use_decl | statement }
use_decl       ::= 'use' identifier
statement      ::= atom_decl | nl_decl | let_decl | formula_decl | vertauto_stmt

atom_decl      ::= 'atom' identifier '=' string_literal
nl_decl        ::= 'nl' identifier '=' string_literal
let_decl       ::= 'let' identifier '=' translate_call
translate_call ::= 'translate' '(' text_ref [ ',' identifier ] ')'
text_ref       ::= string_literal | identifier

formula_decl   ::= 'formula' identifier '=' formula_expr
formula_expr   ::= iff_expr
iff_expr       ::= impl_expr { 'iff' impl_expr }
impl_expr      ::= or_expr { 'impl' impl_expr }
or_expr        ::= and_expr { 'or' and_expr }
and_expr       ::= not_expr { 'and' not_expr }
not_expr       ::= 'not' not_expr | primary_expr
primary_expr   ::= identifier | '(' formula_expr ')'

vertauto_stmt  ::= 'vertauto' formula_ref [ vertauto_block ]
formula_ref    ::= identifier | '(' formula_expr ')'
vertauto_block ::= '{' { 'steps' | 'table' | 'verdict' } '}'
```

### 4.1 Precedencia de operadores (de menor a mayor)

1. `iff`
2. `impl` (asociativo por la derecha en el parser)
3. `or`
4. `and`
5. `not` (prefijo)

### 4.2 Errores

`LsParseException` con número de línea del token actual.

---

## 5. AST (`prism.pr.ast`)

| Nodo | Campos | Rol |
|------|--------|-----|
| `LsProgram` | `moduleName`, `uses`, `statements` | Raíz |
| `AtomDecl` | `name`, `label` | Declaración documental de átomo |
| `NlDecl` | `name`, `text` | Texto en lenguaje natural |
| `LetDecl` | `name`, `translate` | Enlace inmutable vía `translate` |
| `FormulaDecl` | `name`, `expression` | Fórmula simbólica |
| `VertautoStmt` | `target`, `options` | Evaluación lógica |
| `TranslateCall` | `text`, `moduleOverride` | Llamada a translate |
| `TextRef` | literal o variable | Argumento de translate |
| `FormulaRef` | variable o expresión inline | Objetivo de vertauto |
| `VertautoOptions` | `steps`, `table`, `verdict` | Modo corto vs extendido |

Interfaces selladas (`LsStmt`, `FormulaExpr`) permiten extensión futura sin romper el compilador.

---

## 6. Puente de fórmulas (`LsFormulaBridge`)

Convierte el AST de expresiones `.pr` al IR existente (`prism.ir.*`):

| Sintaxis `.pr` | IR Java |
|----------------|---------|
| `p` | `AtomExpr("p", false)` |
| `not x` | `NegExpr` |
| `a and b` | `AndExpr` |
| `a or b` | `OrExpr` |
| `a impl b` | `ImpExpr` |
| `a iff b` | `EquivExpr` |

`toDisplayString()` genera la cadena mostrada en pasos de evaluación (p. ej. `p or not p`).

---

## 7. Intérprete (`PrInterpreter`)

### 7.1 Entorno de ejecución

Tres mapas durante la ejecución secuencial de sentencias:

| Mapa | Contenido |
|------|-----------|
| `nlTextos` | nombre → texto NL |
| `atomEtiquetas` | nombre → etiqueta NL (v1 documental; sin enlace con translate) |
| `formulas` | nombre → `FormulaBinding` |

### 7.2 `LsAtomRegistry` — enlace `atom`

Las declaraciones `atom p = "llueve"` alimentan:

| Uso | Efecto |
|-----|--------|
| `formula f = p impl q` | Emisión clásica `(p → q)` con símbolos fijos |
| `let f = translate(...)` + `vertauto f` | Proposiciones muestran etiquetas declaradas (`llueve=p`) en lugar de fragmentos NL internos (`llover=p`) |
| `vertauto tautologia { steps ... }` | Sección **Átomos declarados** con `{estudio=p}` |

Clase: `LsAtomRegistry`. El motor `RegistroProposiciones` expone `registrarAtomoDeclarado`.

### 7.3 `FormulaBinding`

Encapsula el resultado de `let` o `formula`:

- **Desde translate:** guarda `PrismResult`, texto NL y fórmula clásica emitida.
- **Desde formula:** guarda `LogicExpr` y cadena mostrada.

Al evaluar `vertauto`:

1. Si hay traducción NL → `vertautoClasica` + reinyectar pasos de `translate` en `VertautoResult`.
2. Si hay `LogicExpr` → `vertautoService.vertauto(expr, mostrada)`.
3. Si solo hay cadena clásica → `vertautoClasica`.

### 7.3 `use core`

Valida que el módulo esté en `MODULOS_CONOCIDOS` (`core`). El motor `PrismEngine` ya carga `logicscript/core.lgs` desde el classpath; `use` es declaración explícita en el fuente.

### 7.4 Modos de `vertauto`

| Forma | Salida |
|-------|--------|
| `vertauto f` | Modo corto: `(p → q)  contingency` |
| `vertauto f { steps table verdict }` | Modo extendido: traducción (si NL), pasos, tabla, dictamen |

### 7.5 Compilación y Evaluación en Dos Pasadas (Two-Pass Symbol Collection)

Para evitar la rigidez secuencial de los compiladores de una sola pasada y permitir ordenación declarativa libre:

1. **Pase 1 (Recolección de Símbolos y Declaraciones de Datos):**
   - Recorre el AST registrando identificadores únicos (`PrValidator`).
   - Construye el catálogo de textos `nl` y el registro canónico de átomos (`PrAtomRegistry`) con todas las declaraciones del módulo, independientemente de si aparecen al inicio, en medio o al final del archivo.
2. **Pase 2 (Validación Semántica y Evaluación):**
   - Ejecuta `translate(...)`, `formula` y `vertauto` disponiendo del catálogo completo de símbolos.
   - Resuelve referencias hacia adelante (*forward references*), garantizando que `translate` vincule los símbolos declarados por el usuario incluso si los `atom` fueron escritos al pie del documento.

---

## 8. CLI (`LscCli`)

```
java -cp out prism.pr.PrcCli run <archivo.pr>
java -cp out prism.pr.PrcCli check <archivo.pr>
java -cp out prism.pr.PrcCli --version
java -cp out prism.pr.PrcCli help
```

| Comando | Acción |
|---------|--------|
| `run` | Parsea, valida y ejecuta |
| `check` | Parsea y valida sin ejecutar (código 0 = OK) |
| `--version` | Versión de `lsc` |

Códigos de salida: `0` éxito, `1` error de uso, sintaxis, validación o ejecución.

---

## 9. Validación estática (`PrValidator`)

`prc check` y `prc run` comparten las mismas reglas antes de ejecutar:

| Regla | Ejemplo de error |
|-------|------------------|
| Módulo `use` conocido | `Módulo desconocido en use: foo` |
| Sin redeclaraciones | `Redeclaración de identificador 'p'` |
| `translate` referencia `nl` existente | `Texto nl no definido en translate: falta` |
| Átomos en `formula` / `vertauto` inline | `Átomo no declarado en 'f': q` |
| `vertauto` referencia fórmula definida | `Fórmula no definida en vertauto: x` |
| Convención archivo ↔ `module` | `module 'x' no coincide con archivo 'y.pr'` |

Clases: `PrValidator`, `LsValidationException`, `PrChecker`.

Pruebas: `LsCheckHarness` (6 casos).

---

## 10. Pruebas (`PrInterpreterHarness`)

Tres casos automatizados:

1. Parseo de programa mínimo.
2. `examples/ejercicio_lluvia.pr` → dictamen `contingency`.
3. `examples/tautologia_estudio.pr` → dictamen `tautology`.

```powershell
java -cp out prism.pr.PrInterpreterHarness
```

Regresiones del motor (sin cambios):

```powershell
java -cp out prism.vertauto.VertautoHarness
java -cp out prism.PrismRegressionHarness
```

---

## 11. Ejemplos ejecutables

### `examples/ejercicio_lluvia.pr`

```ls
module ejercicio_lluvia
use core
atom p = "llueve"
atom q = "paraguas"
nl linea_lluvia = "si llueve llevo paraguas"
let implicacion = translate(linea_lluvia)
vertauto implicacion { steps table verdict }
```

Flujo: NL → `(p → q)` → tabla 4 filas → **contingency**.

### `examples/tautologia_estudio.pr`

```ls
module tautologia_estudio
atom p = "estudio"
formula tautologia = p or not p
vertauto tautologia { steps table verdict }
```

Flujo: expresión simbólica → **tautology**.

### `examples/plantilla_ejercicio.pr`

Plantilla copiable con patrones A–D comentados. Ver `GUIA-RAPIDA-EJERCICIOS.md`.

---

## 12. Herramientas de autor (junio 2026)

| Recurso | Ubicación |
|---------|-----------|
| Guía rápida | `documentacion Prism/GUIA-RAPIDA-EJERCICIOS.md` |
| **Autocompletado y snippets (documento vivo)** | `documentacion Prism/AUTOCOMPLETADO-SNIPPETS-Y-CAPACIDADES-LS.md` |
| Extensión editor | `extensions/logicscript/` + `EXTENSION-VSCODE.md` |
| Tareas VS Code | `tooling/vscode/tasks.json` (copiar a `.vscode/`) |

---

## 13. Limitaciones v1 (intencionadas)

| Constructo | Estado |
|------------|--------|
| `rule`, `run` | No implementado |
| `translate("literal")` sin `nl` | **Implementado** — ver `examples/translate_literal.pr`, snippet `lettrl` / `lsnlit` |
| `translate(t, modulo)` con módulo explícito | Error en runtime |
| `atom` enlazado a translate | **Implementado** — etiquetas en proposiciones de `vertauto` |
| Compilación JVM (`.class`) | Pendiente |
| `prc check` (solo validar) | **Implementado** |
| Extensión VS Code | **v0.2.0** — snippets + resaltado; ver `EXTENSION-VSCODE.md` |

---

## 14. Commits sugeridos (historial granular)

Orden recomendado al versionar:

1. `feat(ls): añadir lexer y tokens del lenguaje .pr`
2. `feat(ls): definir nodos AST para programas .pr`
3. `feat(ls): implementar parser descendente recursivo`
4. `feat(ls): puente AST fórmula hacia IR logicscript`
5. `feat(ls): intérprete con translate y vertauto`
6. `feat(ls): CLI prc run para ejecutar archivos .pr`
7. `test(ls): harness de regresión del intérprete`
8. `chore: actualizar examples y compile.ps1 con lsc`

---

## 15. Próximos pasos

1. Snippets VS Code (`lsprog`, `lsnl`, `vax`) — **implementado v0.2.0**
2. `translate(texto, modulo)` con carga dinámica de `.lgs`.
3. Compilador `.pr` → bytecode JVM (`lsc compile`).
