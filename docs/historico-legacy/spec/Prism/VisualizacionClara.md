# Visualización Clara (tabla de verdad en UI)

Panel accesible desde la tarjeta **Visualización Clara** en la ventana principal.

---

## Qué muestra

Tras procesar un enunciado, abra manualmente la tarjeta **Visualización Clara** (pestañas **Tabla** y **Árbol**).

1. **Fórmula** evaluada.
2. **Leyenda** de símbolos cuando hay traducción LogicScript (`p = estudiar`, `q = aprobar`, …).
3. **Tabla de verdad** con columnas alineadas:
   - Una columna por variable (cabecera: `p (estudiar)`).
   - Columna **Resultado** (valor V/F de la fórmula completa).
4. **Pestañas:**
   - **Tabla** — columnas alineadas con colores V/F.
   - **Árbol** — evaluación de la interpretación elegida (desplegable o fila de Tabla).
5. **Colores:**
   - **V** — fondo verde claro.
   - **F** — fondo rojo claro.

Hasta **4 variables** (16 filas). Con más variables se muestra un mensaje informativo.

---

## Dónde vive en el código

| Pieza | Ubicación |
|-------|-----------|
| Datos estructurados | `TablaVerdad.java` |
| Evaluación por fila | `MotorLogico.evaluarExpresionSustituida` |
| Árbol | `ConstructorArbolEvaluacion`, `RenderizadorArbolEvaluacion` |
| UI tabla + árbol | `PanelVisualizacion.java` |
| Conexión al procesar | `TautoTeacherApp.actualizarVisualizacion` |

La tabla **ya no** se duplica en el panel **Explicación** (solo demostración educativa y pasos LogicScript).

---

## Cómo probar

1. Compilar y abrir `tautoteacher2.Main`.
2. Procesar, por ejemplo: `si estudio y practico entonces apruebo`.
3. Clic en tarjeta **Visualización Clara**.

Debería verse tabla de **3 variables** (8 filas) con leyendas en cabecera.

---

## Referencias

- `docs/LogicScript/PatronesCompuestos.md` — frases con varias variables
- Panel **Explicación** — demostración educativa (sin tabla)
