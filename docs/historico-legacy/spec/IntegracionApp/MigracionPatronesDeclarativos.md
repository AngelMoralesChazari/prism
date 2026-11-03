# Migración: patrones semánticos declarativos (`.lgs`)

Este documento describe **cambios a nivel de aplicación** (despliegue, comportamiento y mantenimiento), no la gramática detallada de LogicScript.

## Qué cambió

- Los patrones que antes estaban **fijos en código** (`SemanticMapper` con varios `if`) pasan a poder definirse en **`src/main/resources/logicscript/core.lgs`** con la directiva **`pattern`**.
- **`LogicScriptEngine`** carga **una vez** el archivo completo (`ContenidoLgs`): mismos lemas para `BaseConocimiento` y lista de patrones para `SemanticMapper`.
- Si **`core.lgs` no está en el classpath**, si está vacío o si hay **error de parseo**, el bundle se trata como vacío: los **lemas** siguen el respaldo embebido en `BaseConocimiento` y los **patrones** el respaldo embebido en `SemanticMapper` (misma semántica MVP que antes).

## Impacto en compilación y ejecución

- Sigue siendo obligatorio copiar **`src/main/resources`** al directorio de salida (p. ej. `out`) al compilar a mano. El script **`compile.ps1`** del proyecto ya copia recursos.
- Cualquier entrada adicional (otro `.lgs`) requeriría ampliar el motor o la ruta de carga; hoy solo se usa **`logicscript/core.lgs`**.

## Mantenimiento recomendado

- Tras editar patrones o el lexer, ejecutar **`LogicScriptRegressionHarness`** (véase `docs/LogicScript/PruebasRegresion.md`) además de pruebas manuales con `LogicScriptCli`.
- Tras editar patrones, verificar en consola con `LogicScriptCli` frases representativas y revisar **pasos de análisis** (nombre del patrón detectado).
- Si se declara **cualquier** `pattern` en el archivo, la lista del archivo **reemplaza** por completo a la embebida: documentar en comentarios del propio `.lgs` los patrones mínimos necesarios.

## Documentación LogicScript (especificación)

La sintaxis `pattern`, tokens permitidos y semántica de `left`/`right` están en:

- `docs/LogicScript/Pattern.md`
- `docs/LogicScript/FormatoLGS.md`

El registro técnico de integración del pipeline sigue en `docs/LogicScript/CambiosIntegracionGeneral.md`.
