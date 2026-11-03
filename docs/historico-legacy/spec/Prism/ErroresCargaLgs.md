# Errores visibles al cargar archivos `.lgs`

Este documento describe el **paso 2 del roadmap** LogicScript: dejar de ocultar fallos de parseo del archivo de datos y exponer **diagnósticos claros** al usuario (CLI, UI vía `LogicScriptResult`).

---

## Problema anterior

`LgsCargador.cargarDesdeClasspath` capturaba cualquier `IOException` y devolvía `ContenidoLgs.vacio()`. El motor seguía con **lemas y patrones embebidos** en Java, como si el archivo no existiera. Un typo en `core.lgs` (línea `lemma` mal formada, `pattern` sin `=>`, etc.) **no se veía** al traducir enunciados.

---

## Comportamiento actual

### API de carga

| Método | Uso |
|--------|-----|
| `LgsCargador.cargarConDiagnostico(String rutaRecurso)` | **Recomendado.** Devuelve `ResultadoCargaLgs`. |
| `LgsCargador.cargarConDiagnostico(InputStream, String rutaEtiqueta)` | Mismo diagnóstico sobre un stream (pruebas). |
| `LgsCargador.cargarDesdeClasspath(String)` | Legacy: sigue devolviendo solo `ContenidoLgs` vacío ante fallo (sin mensaje). |

### Estados (`EstadoCargaLgs`)

| Estado | Significado | ¿Traduce LN? |
|--------|-------------|--------------|
| `EXITO` | Archivo leído y parseado. | Sí, con datos del `.lgs`. |
| `RECURSO_NO_ENCONTRADO` | No hay recurso en el classpath (p. ej. olvidó copiar `resources` a `out`). | Sí, con **respaldo embebido** + **advertencia** en pasos de análisis. |
| `ERROR_SINTAXIS` | Línea inválida; mensaje incluye **`Línea N:`**. | **No.** `LogicScriptResult` error con mensaje explícito. |
| `ERROR_LECTURA` | Otro fallo de I/O al abrir el stream. | **No.** |

### Integración en el motor

`LogicScriptEngine` carga **una vez** al construirse:

```text
ResultadoCargaLgs → si bloqueaTraduccion() → traducir() devuelve error inmediato
                  → si RECURSO_NO_ENCONTRADO → advertencia en pasos + respaldo embebido
                  → si EXITO → contenido del archivo
```

El mensaje de error para la UI/CLI usa `ResultadoCargaLgs.mensajeParaUsuario()`:

```text
logicscript/core.lgs: Línea 12: falta '->' en lemma: ...
```

---

## Clases Java

| Clase | Paquete | Rol |
|-------|---------|-----|
| `EstadoCargaLgs` | `nlp.lexicon` | Enum de estados de carga. |
| `ResultadoCargaLgs` | `nlp.lexicon` | Contenido + estado + mensaje + ruta. |
| `LgsCargador` | `nlp.lexicon` | Emite `ResultadoCargaLgs`; parseo sigue lanzando `IOException` con número de línea. |
| `LogicScriptEngine` | `logicscript` | Bloquea traducción si hay error de sintaxis/lectura. |

---

## Cómo probar manualmente

1. Introduce un error en `src/main/resources/logicscript/core.lgs`, por ejemplo:

   ```text
   lemma roto sin flecha
   ```

2. Recompila y copia recursos a `out`.

3. Ejecuta:

   ```text
   java -cp out tautoteacher2.logicscript.LogicScriptCli "si llueve entonces llevo paraguas"
   ```

4. Debes ver **Estado: ERROR** y un mensaje que cite **`logicscript/core.lgs`** y **`Línea N`**.

5. Restaura el archivo válido antes de commitear.

El harness `LogicScriptRegressionHarness` incluye una comprobación automática de `ERROR_SINTAXIS` sobre un stream de prueba (no modifica `core.lgs`). Ver también `docs/LogicScript/ErroresCargaLgs.md`.

---

## Errores de sintaxis que reporta el parser

Ejemplos (no exhaustivo):

- Directiva desconocida.
- `lemma` sin `->` o con forma/lema vacío.
- `pattern` sin `=>`, sin `left=`/`right=`, token de forma desconocido, tipo IR inválido.
- Índices fuera de rango o que no apuntan a `literal` (validado en `PatronSemanticoLgs`).

Todos incluyen **número de línea** en el mensaje de `IOException`.

---

## Evolución prevista

- Modo **estricto vs tolerante** configurable (hoy: sintaxis = bloqueante; recurso ausente = advertencia + respaldo).
- Acumular **varios** diagnósticos por archivo (hoy se detiene en el primer error).
- Surface del estado de carga en la UI (banner “Error en core.lgs” sin intentar traducir).

---

## Referencias

- `docs/LogicScript/FormatoLGS.md` — sintaxis válida del archivo.
- `docs/LogicScript/PruebasRegresion.md` — harness.
- `docs/LogicScript/MapaImplementacionJava.md` — trazabilidad clases.
