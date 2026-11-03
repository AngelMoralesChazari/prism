# Formato de archivo `.lgs` (LogicScript datos v0.2)

Extensión sugerida: **`.lgs`** (LogicScript). En esta versión el archivo es **texto UTF-8**, línea a línea.

## Reglas generales

- Codificación: **UTF-8**.
- Líneas vacías: se ignoran.
- Comentarios: líneas que empiezan por `#`.
- Cada línea útil debe ser una directiva reconocida (véase abajo).

## Directivas soportadas

### Versión (opcional, informativa)

```text
version 0.2
```

Se ignora en la carga actual; sirve para documentación y migraciones.

### Lemas

```text
lemma <forma> -> <lema>
```

- `<forma>` y `<lema>` se normalizan a **minúsculas** al cargar.
- Sin acentos en el archivo si el flujo ya pasó por `NormalizadorTexto` en otro paso; en `core.lgs` usamos formas sin tilde para consistencia con texto normalizado.
- Significado detallado: `docs/LogicScript/Lemma.md`.

Ejemplo:

```text
lemma llueva -> llover
lemma llevo -> llevar
```

### Patrones semánticos (v0.2)

```text
pattern <nombre> <token> ... => <imp|and|or> left=<i> right=<j>
```

Definen cómo una secuencia de tipos de token (salida de `NaturalLexer`) se traduce a IR. Especificación completa: **`docs/LogicScript/Pattern.md`**.

## Recurso incluido en el proyecto

- **Ruta en fuentes:** `src/main/resources/logicscript/core.lgs`
- **Classpath:** `logicscript/core.lgs`

Al compilar, hay que **copiar** `src/main/resources` a la carpeta de salida (`out`) para que `java -cp out` encuentre el recurso. El script `compile.ps1` ya hace esa copia.

## Cargador Java

- **Clase:** `tautoteacher2.nlp.lexicon.LgsCargador`
- **Métodos principales:**
  - `cargarConDiagnostico(String rutaRecurso)` → `ResultadoCargaLgs` (**recomendado**; errores visibles).
  - `cargarConDiagnostico(InputStream, String rutaEtiqueta)` → pruebas y herramientas.
  - `cargarDesdeClasspath(String)` → `ContenidoLgs` (legacy, sin mensaje de error).
  - `cargarLemasDesdeClasspath(String)` → solo mapa de lemas (compatibilidad).

Errores de sintaxis **bloquean** la traducción LN en `LogicScriptEngine`. Detalle: **`docs/LogicScript/ErroresCargaLgs.md`**.

## Evolución prevista

- Directivas `connector`, `synonym`.
- Varios diagnósticos por archivo (hoy: primer error detiene el parseo).
- Varias capas: `core.lgs`, `academico.lgs`, etc.
