# Ampliación de lemas y patrones (v0.3)

Guía local para la base léxica y patrones de LogicScript **Fase 1**.

---

## Qué incluye v0.3

### Lemas nuevos (`core.lgs`)

Además de las familias previas: **nevar**, **calor/frio**, **ir**, **llegar**, **comprar**, **necesitar**, **poder**, **querer**, **entrar**, **pasar**, más formas de **salir**.

### Conectores

Ver `docs/LogicScript/ConectoresNuevos.md`: *cuando*, *solo si*, *a menos que*.

### Patrones compuestos ampliados

Ver `docs/LogicScript/PatronesCompuestos.md`: `SI_CONJ_Y_ELIPTICO`, `SI_DISY_O_ENTONCES`, `SI_ENTONCES_CONJ_CONS`.

### Patrones previos (v0.2)

| Patrón | Frase de ejemplo | Fórmula |
|--------|------------------|---------|
| `SIEMPRE_QUE_ELIPTICO` | siempre que estudio apruebo | `(p → q)` |
| `SIEMPRE_QUE_ENTONCES` | siempre que trabajo entonces descanso | `(p → q)` |
| `EQUIVALENCIA` | apruebo si y solo si estudio | `(p ↔ q)` |

### Código de soporte

- Lexer: `si y solo si`, `siempre que`
- IR: `EquivExpr` y emisión `↔`
- Composición por coma: también parte en `, siempre que`

---

## Cómo compilar (obligatorio tras cambiar `core.lgs`)

Abre **PowerShell** en la carpeta del proyecto (`TautoTeacher2.0\TautoTeacher2.0`) y ejecuta:

```powershell
.\compile.ps1 -JdkHome "C:\Program Files\Java\jdk-25.0.2"
```

Si tu JDK está en otra ruta (por ejemplo Adoptium 17):

```powershell
.\compile.ps1 -JdkHome "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
```

El script compila el `.java` y **copia** `src\main\resources` a `out\`. Sin ese paso, la app sigue usando un `core.lgs` viejo.

---

## Cómo probar sin la interfaz gráfica (más rápido)

Misma carpeta, después de compilar:

```powershell
java -cp out tautoteacher2.logicscript.LogicScriptCli "siempre que estudio apruebo"
java -cp out tautoteacher2.logicscript.LogicScriptCli "apruebo si y solo si estudio"
java -cp out tautoteacher2.logicscript.LogicScriptCli "si trabajo descanso"
java -cp out tautoteacher2.logicscript.LogicScriptCli "solea y hace calor"
java -cp out tautoteacher2.logicscript.LogicScriptCli "practico si y solo si estudio"
```

Debes ver `Estado: OK` y una fórmula con `→` o `↔` (no solo `p`).

Regresión automática de todos los casos:

```powershell
java -cp out tautoteacher2.logicscript.LogicScriptRegressionHarness
```

Salida esperada: `LogicScriptRegressionHarness: OK (18 casos LN + diagnóstico .lgs).`

---

## Cómo probar en la app (ventana gráfica)

1. **Cierra** la ventana de TautoTeacher si ya estaba abierta (usa código viejo en memoria).
2. Compila con `.\compile.ps1` (arriba).
3. Arranca de nuevo:

   ```powershell
   java -cp out tautoteacher2.Main
   ```

   O desde Cursor/VS Code: ejecutar `Main.java` (F5).

4. Pestaña **Lenguaje natural**.
5. Escribe una frase de la tabla y pulsa el botón de procesar.
6. Debe mostrar clasificación (tautología / contingencia) y **forma lógica interna** distinta de un solo `p`.

---

## Si algo falla

| Síntoma | Causa habitual | Qué hacer |
|---------|----------------|-----------|
| Fórmula solo `p` | `out\logicscript\core.lgs` desactualizado | Volver a ejecutar `compile.ps1` |
| Error al compilar en `LgsCargador` | Conflicto `y`/`o` en switches | Usar `and`/`or` tras `=>` en patrones; `y`/`o` solo en la forma del patrón |
| App sin cambios | No reiniciaste tras compilar | Cerrar ventana y volver a abrir `Main` |

---

## Siguiente paso (opción B)

Conectar en la UI: mapa de proposiciones (`p = estudiar`), pasos de análisis y panel de explicación educativa.
