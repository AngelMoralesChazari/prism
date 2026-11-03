# Cambios de Integracion General

Este archivo registra cambios de integracion que impactan al programa general
(no solo una clase puntual de LogicScript).

## Actualizacion actual

### 1) Pipeline LN interno

`LogicScriptEngine` ahora orquesta:

1. `NormalizadorTexto`
2. `NaturalLexer`
3. `SemanticMapper`
4. `EmitidorFormula`
5. `RegistroProposiciones`

### 2) IR ampliada

Se agrego `OrExpr` y la IR sellada quedo:

- `AtomExpr`
- `NegExpr`
- `AndExpr`
- `OrExpr`
- `ImpExpr`

### 3) Emision de formula

`EmitidorFormula` ahora soporta emision de disyuncion (`∨`).

### 4) Lexer

`NaturalLexer` separa literales por coma, mejorando casos como:

- `en caso de que X, Y`

### 5) Compatibilidad UI

`TautoTeacherApp` no cambio su contrato: sigue consumiendo `LogicScriptService`
y el resultado `LogicScriptResult` como antes.

### 6) Archivo `.lgs` y demo consola

- Recurso: `src/main/resources/logicscript/core.lgs` (lemas y, desde v0.2, patrones `pattern`).
- Cargador: `tautoteacher2.nlp.lexicon.LgsCargador` → `ContenidoLgs` (`lemas`, `patronesSemanticos`).
- CLI: `tautoteacher2.logicscript.LogicScriptCli` (traducción + `MotorLogico`).
- `compile.ps1` copia `src/main/resources` a `out` tras `javac`.

### 7) Patrones semánticos declarativos (migración)

- `SemanticMapper` itera reglas `PatronSemanticoLgs` cargadas desde `.lgs` o, si no hay ninguna, la lista embebida equivalente al MVP.
- `LogicScriptEngine` construye un solo `ContenidoLgs` y lo pasa a `BaseConocimiento` (lemas) y a `SemanticMapper` (patrones).
- Documentación de despliegue y comportamiento ante archivo vacío o error: `docs/IntegracionApp/MigracionPatronesDeclarativos.md`.
- Especificación de la directiva: `docs/LogicScript/Pattern.md`.

### 8) Regresión del pipeline LN → fórmula

- Clase `tautoteacher2.logicscript.LogicScriptRegressionHarness` (sin JUnit): ejecutar tras compilar con recursos en `out`.
- Ajuste en `NaturalLexer`: comprobar palabra clave **antes** de saltar espacios/comas, para reconocer ` y ` / ` o ` tras un literal.
- Documentación: `docs/LogicScript/PruebasRegresion.md`.

### 9) Errores visibles al cargar `.lgs`

- `LgsCargador.cargarConDiagnostico` → `ResultadoCargaLgs` + `EstadoCargaLgs`.
- `LogicScriptEngine` **bloquea** la traducción LN si hay error de sintaxis o lectura en `core.lgs`.
- Recurso ausente: **advertencia** en pasos de análisis + respaldo embebido (no bloquea).
- Documentación: `docs/LogicScript/ErroresCargaLgs.md`.

### 10) Patrón elíptico `SI_ELIPTICO`

- Línea en `core.lgs`: `pattern SI_ELIPTICO si literal literal => imp left=1 right=2`.
- Cubre *si estudio apruebo* → `(p → q)` sin exigir *entonces*.
- Respaldo en `SemanticMapper.patronesPredeterminados()`; caso `si_eliptico` en el harness.
- Documentación: `docs/LogicScript/PatronSiElliptico.md`.

