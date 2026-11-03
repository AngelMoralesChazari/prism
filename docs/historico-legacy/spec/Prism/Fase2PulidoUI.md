# Fase 2 — Pulido UI

Mejoras de experiencia tras la Fase 1 (motor LogicScript v0.3).

---

## Qué incluye

| Mejora | Descripción |
|--------|-------------|
| **Navegación manual** | Tras procesar, permanece en Análisis Rápido |
| **Explicación en contingencia** | Panel «Explicación»: casos V y F concretos con leyendas |
| **Árbol de evaluación** | Pestaña «Árbol» en Visualización Clara (separada de Tabla) |

---

## Auto-ir a Visualización

~~Tras procesar, la app abría Visualización automáticamente.~~ **Actualizado:** tras procesar permanece en **Análisis Rápido**; el usuario abre **Visualización Clara** manualmente.

---

## Pestañas en Visualización Clara

| Pestaña | Contenido |
|---------|-----------|
| **Tabla** | Tabla de verdad con colores V/F (como antes, a pantalla completa) |
| **Árbol** | Selector de interpretación (fila) + árbol ASCII de evaluación |

Al seleccionar una fila en **Tabla**, se sincroniza el selector de **Árbol**. También puede elegir la fila desde el desplegable en **Árbol**.

---

## Explicación en contingencia / contradicción

En `ExplicacionEducativaBuilder`, si el dictamen es **CONTINGENCIA** o **CONTRADICCIÓN**:

1. Sección **«Por qué es …»** con filas ejemplo de la tabla de verdad.
2. **Contingencia:** un caso donde la fórmula es **V** y otro donde es **F** (con leyendas `«estudiar»=V`, etc.).
3. **Contradicción:** mensaje breve (siempre F); sin demostración por refutación larga.
4. **Tautología:** sin cambio (demostración por refutación completa).

---

## Árbol de evaluación

- **Ubicación:** pestaña **Árbol** dentro de «Visualización Clara».
- **Uso:** desplegable «Interpretación» o fila seleccionada en pestaña Tabla.
- **Código:** `ConstructorArbolEvaluacion`, `RenderizadorArbolEvaluacion`, `NodoArbolEvaluacion`.

---

## Cómo probar

```powershell
.\compile.ps1 -JdkHome "C:\Program Files\Java\jdk-25.0.2"
java -cp out tautoteacher2.Main
```

### Frases recomendadas

| Frase | Dictamen | Qué verificar |
|-------|----------|----------------|
| `si estudio apruebo` | CONTINGENCIA | Permanece en Análisis + casos V/F en Explicación + pestaña Árbol |
| `p ∧ ¬p` (pestaña Fórmula) | CONTRADICCIÓN | Explicación «siempre F» + tabla toda roja en resultado |
| `(p → q) ∨ (q → p)` (Fórmula) | TAUTOLOGÍA | Árbol en fila cualquiera (raíz V) |

---

## Antes / después (ejemplo)

**Entrada LN:** `si estudio apruebo`

| Aspecto | Antes (split inferior) | Ahora |
|---------|------------------------|-------|
| Pantalla tras procesar | Saltaba a Visualización | Permanece en **Análisis Rápido** |
| Visualización | Tabla y árbol apilados | Pestañas **Tabla** y **Árbol** |
| Árbol | Debajo de la tabla | Solo en pestaña **Árbol** (desplegable de fila) |

**Fórmula:** `(p → q)` — contingencia clásica.

---

## Referencias

- `docs/LogicScript/VisualizacionClara.md` — tabla de verdad
- `docs/LogicScript/ConectoresNuevos.md` — frases LN Fase 1
