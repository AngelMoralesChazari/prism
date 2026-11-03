# SemanticMapper (LogicScript)

`SemanticMapper` es la capa que transforma tokens de lenguaje natural en nodos de la IR (`LogicExpr`), sin emitir aún la fórmula final.

## Clase actual

- **Clase:** `tautoteacher2.nlp.semantica.SemanticMapper`
- **Entradas:** texto de bloque + `List<TokenNatural>` + lista de `pasosDeAnalisis`.
- **Salida:** `LogicExpr` o `null` si no hay patrón reconocido antes de los fallbacks.

## Origen de los patrones

1. **Archivo `.lgs`:** si `logicscript/core.lgs` declara una o más líneas `pattern`, se usan **solo** esas reglas, en orden de aparición.
2. **Respaldo embebido:** si no hay patrones en el archivo (recurso ausente, vacío o lista vacía), se aplican los **cinco patrones MVP** definidos en código (equivalentes al bloque `pattern` de ejemplo en `core.lgs`).

La sintaxis de `pattern` se documenta en **`docs/LogicScript/Pattern.md`**, incluida la sección **«De dónde salen `left=1` y `right=3`»** (tabla de índices, roles lógicos y analogías con compiladores).

## Patrones MVP (semántica)

1. `si X entonces Y` → `ImpExpr(X, Y)`
2. `Y si X` → `ImpExpr(X, Y)`
3. `en caso de que X, Y` → `ImpExpr(X, Y)` (con tokenización acorde al lexer)
4. `X y Y` → `AndExpr(X, Y)`
5. `X o Y` → `OrExpr(X, Y)`
6. Fallback → `AtomExpr` (primer literal o texto completo del bloque)

La negación local se maneja como prefijo `no` sobre el átomo (`AtomExpr(..., true)`).

## Relación con BaseConocimiento

Antes de crear un `AtomExpr`, el mapper pide a `BaseConocimiento` la forma canónica del fragmento para estabilizar la asignación proposicional.

## Por qué está separado del Engine

- El `LogicScriptEngine` queda como orquestador.
- Las reglas semánticas pueden evolucionar en datos (`.lgs`) sin recompilar la lógica de coincidencia base.
- Facilita pruebas del mapeo LN → IR.

## Evolución prevista

1. Patrones con longitud variable o comodines (sin romper el modelo actual).
2. Precedencia formal de conectores cuando haya más de un nivel.
3. Múltiples archivos `.lgs` fusionados con política explícita.
