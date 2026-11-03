# Directiva `lexrule` — morfología declarativa (Fase C, v0.6)

## Resumen ejecutivo

La **Fase C** del roadmap léxico permite declarar **reglas morfológicas** en `core.lgs` con la directiva `lexrule`, en lugar de codificarlas solo en Java (`NormalizadorMorfologico.java`).

| Antes (Fase A) | Ahora (Fase C) |
|----------------|----------------|
| Reglas de sufijos **hardcodeadas** en Java | Mismas reglas **copiadas** en `core.lgs` |
| Cambiar morfología → **recompilar** | Cambiar morfología → **editar `.lgs`** y recompilar solo para copiar recursos |
| `lemma` para excepciones | `lemma` **sigue** para irregulares; `lexrule` para patrones productivos |
| Sin validación de sintaxis de reglas en archivo | `LgsCargador` valida `lexrule` con diagnóstico por línea |

El motor Java **sigue ejecutando** la lógica morfológica; el `.lgs` solo **configura** qué reglas aplicar y en qué orden.

---

## Justificación del diseño

### Problema que teníamos (Fase A)

En v0.4, `NormalizadorMorfologico.java` contenía un array de sufijos:

```java
String reglas[][] = {
    {"amos", "a"}, {"an", "a"}, {"o", "?"},
    // ...
};
```

Funcionaba bien para el curso, pero:

1. **Cada ajuste** (añadir `-aba`, cambiar orden) exigía tocar Java y recompilar.
2. El profesor o mantenedor del léxico **no podía** experimentar sin IDE.
3. Las reglas estaban **duplicadas conceptualmente** respecto a lo que ya vivía en `core.lgs` (`lemma`).

### Objetivo Fase C

Separar **datos** (reglas editables) de **motor** (algoritmo de aplicación):

```text
core.lgs                    Java
─────────                   ────
lexrule sufijo an ...  →    NormalizadorMorfologico.aplicarReglas()
lemma apruebo -> ...   →    BaseConocimiento (prioridad máxima)
pattern SI_ELIPTICO    →    SemanticMapper (capa distinta)
```

### Por qué no un analizador morfológico completo

LogicScript es un **traductor didáctico**, no un lematizador del español general. `lexrule` cubre:

- Sufijos flexivos del curso (-ar regulares, 1.ª persona -o).
- Exclusiones de sustantivos (`gorra`, `calor`).
- Una heurística especial para `-o` (*estudio* / *duermo*).

No pretende reemplazar FreeLing ni Stanza.

### Por qué conservar respaldo embebido en Java

Si `core.lgs` no tiene bloque `lexrule` (recurso ausente, archivo vacío), `NormalizadorMorfologico` usa **`reglasPredeterminadas()`** idénticas a las de v0.4. Así:

- El harness y la app **no se rompen** si alguien borra las reglas del archivo.
- Las pruebas de regresión `morph_*` siguen siendo válidas con o sin migración completa.

---

## Orden de prioridad en la canonización (sin cambios conceptuales)

Para cada palabra de un literal:

```text
1. ¿Existe lemma en core.lgs?          → usar lema (apruebo → aprobar)
2. ¿Aplica alguna lexrule?             → infinitivo aproximado (estudio → estudiar)
3. Si no                               → palabra tal cual (identidad)
```

Código: `BaseConocimiento.canonicalizarPalabra()`.

**Importante:** `lemma` **siempre gana** sobre `lexrule`. Las reglas morfológicas no sustituyen irregulares.

---

## Sintaxis de `lexrule` (v0.6)

### 1. Excluir palabras (no flexionar)

Evita que sustantivos del dominio reciban sufijos verbales (*gorra* → *gorrar*).

```text
lexrule excluir gorra sombrero paraguas calor frio sol cielo nube nubes lluvia examen clase
```

- Una línea, varias palabras tras `excluir`.
- Minúsculas (coherente con `NormalizadorTexto`).

### 2. Regla de sufijo simple

```text
lexrule sufijo <sufijo> infinitivo ar|er|ir
```

Genera: `raíz + vocal + "r"` donde la vocal es `a`, `e` o `i` según `ar`, `er`, `ir`.

| Entrada en `.lgs` | Palabra | Raíz | Resultado |
|-------------------|---------|------|-----------|
| `lexrule sufijo an infinitivo ar` | estudian | estudi | estudiar |
| `lexrule sufijo emos infinitivo er` | comemos | com | comer |
| `lexrule sufijo imos infinitivo ir` | dormimos | dorm | dormir |

### 3. Regla de sufijo con condición (reservado v0.6+)

```text
lexrule sufijo <s> infinitivo ar|er|ir si raiz_vocal
lexrule sufijo <s> infinitivo ar|er|ir si raiz_consonante
lexrule sufijo <s> infinitivo ar|er|ir si raiz_termina rm mm rc rt
```

El parser las acepta; en `core.lgs` v0.6 la heurística de `-o` se expresa con la forma especial siguiente (más clara para el curso).

### 4. Heurística 1.ª persona (`-o`)

```text
lexrule sufijo o heuristica primera_persona
```

Replica la lógica Java de Fase A:

| Palabra | Raíz | Regla interna | Infinitivo |
|---------|------|---------------|------------|
| estudio | estudi | raíz vocal | estudiar |
| duermo | duerm | raíz termina rm | dormir |
| llego | lleg | consonante, longitud ≥ 4 | llegar |
| descanso | descans | consonante | descansar |

### Orden en el archivo

**La primera regla que coincida gana.** Por eso en `core.lgs`:

1. Sufijos **más largos** primero (`iaria` antes que `ia`, `an` antes que `a`).
2. `lexrule sufijo o heuristica primera_persona` **al final** del bloque de sufijos.

---

## Bloque real en `core.lgs` v0.6

```text
version 0.6

lexrule excluir gorra sombrero paraguas calor frio sol cielo nube nubes lluvia examen clase

lexrule sufijo iaria infinitivo ar
lexrule sufijo aria infinitivo ar
# ... (resto de sufijos -ar/-er/-ir del curso)
lexrule sufijo a infinitivo ar
lexrule sufijo o heuristica primera_persona

lemma apruebo -> aprobar
# ...
```

Las reglas morfológicas van **antes** de los `lemma` por legibilidad (el cargador no exige ese orden).

---

## Antes vs ahora — ejemplos concretos

### Ejemplo 1: verbo regular (sin cambio de comportamiento)

**Frase:** *si estudian aprueban*

| Etapa | Fase A (solo Java) | Fase C (lexrule en .lgs) |
|-------|--------------------|---------------------------|
| Patrón | `SI_ELIPTICO` | `SI_ELIPTICO` |
| estudian | `estudiar` (sufijo `-an` en Java) | `estudiar` (sufijo `-an` en `.lgs`) |
| aprueban | `aprobar` (`lemma`) | `aprobar` (`lemma`) |
| Fórmula | `(p → q)` | `(p → q)` |

**Conclusión:** mismo resultado; la regla `-an` pasó de Java a datos.

### Ejemplo 2: añadir un sufijo nuevo (el beneficio real)

**Antes:** querer reconocer *organizado* → *organizar* sin `lemma`.

1. Editar `NormalizadorMorfologico.java`.
2. Recompilar todo el proyecto.
3. Commit de código.

**Ahora:**

1. Añadir en `core.lgs`: `lexrule sufijo ado infinitivo ar` (si no estaba; en v0.6 ya existe `ado`).
2. `.\compile.ps1` (copia recursos a `out/`).
3. Commit solo de `core.lgs` (datos).

### Ejemplo 3: error de sintaxis

**Archivo inválido:**

```text
lexrule sufijo o infinitivo xyz
```

**Antes:** no aplicaba (directiva desconocida → error de carga).

**Ahora:** `LgsCargador` devuelve `ERROR_SINTAXIS` con número de línea; la traducción se **bloquea** (igual que un `lemma` mal formado).

### Ejemplo 4: sustantivo protegido

**Palabra:** *gorra* en *llevo gorra*

| Sin `excluir` | Con `lexrule excluir gorra` |
|---------------|------------------------------|
| Posible *gorrar* (incorrecto) | *gorra* sin cambio |

---

## Flujo técnico completo

```text
Entrada LN: "si duermo descanso"
        │
        ▼
NormalizadorTexto
        │
        ▼
NaturalLexer → SI | duermo | descanso
        │
        ▼
SemanticMapper → SI_ELIPTICO
        │
        ▼
BaseConocimiento.canonicalizarFragmento
   duermo  → NormalizadorMorfologico
             → lexrule sufijo o heuristica primera_persona
             → dormir
   descanso → lexrule sufijo o heuristica primera_persona
             → descansar
        │
        ▼
EmitidorFormula → (p → q)
```

---

## Implementación Java

| Pieza | Archivo | Rol |
|-------|---------|-----|
| Modelo de regla | `ReglaMorfologicaLgs.java` | Sufijo, condición, heurística `-o` |
| Configuración | `ConfiguracionMorfologiaLgs.java` | Lista de reglas + exclusiones |
| Contenedor `.lgs` | `ContenidoLgs.java` | Añade `morfologia()` |
| Parser | `LgsCargador.java` | `parsearLexrule(...)` |
| Motor | `NormalizadorMorfologico.java` | Aplica reglas del archivo o predeterminadas |
| Integración | `BaseConocimiento.java` | Pasa `contenido.morfologia()` al normalizador |

---

## Pruebas de regresión

En `LogicScriptRegressionHarness`:

| Verificación | Qué comprueba |
|--------------|---------------|
| Casos `morph_*` (37 LN) | Comportamiento idéntico a Fase A con `core.lgs` v0.6 |
| `lexrule_sintaxis` | `infinitivo xyz` → error bloqueante |
| `lexrule_carga` | Archivo mínimo con `lexrule sufijo ado` → *organizado* → *organizar* |

```powershell
.\compile.ps1 -JdkHome "C:\Program Files\Java\jdk-25.0.2"
java -cp out tautoteacher2.logicscript.LogicScriptRegressionHarness
```

---

## Limitaciones (v0.6)

| Fuera de alcance | Notas |
|------------------|-------|
| Irregulares | Siguen en `lemma` |
| Reglas multi-palabra | Una palabra por token |
| Condiciones compuestas | Una condición por regla |
| Recargar `.lgs` en caliente | Requiere reiniciar app / recompilar para copiar a `out/` |
| Orden automático de sufijos | El autor del `.lgs` debe ordenar de largo a corto |

---

## Buenas prácticas

1. **No dupliques** en `lemma` lo que ya cubre una `lexrule` (-ar regulares).
2. **Mantén el orden** de sufijos (más específicos arriba).
3. Tras editar `core.lgs`, **recompila** para copiar a `out/logicscript/`.
4. Si una regla experimental falla, el harness de sintaxis debe bloquear antes de llegar a la UI.
5. Usa `lexrule excluir` para sustantivos nuevos del curso antes de probar frases.

---

## Roadmap léxico actualizado

| Fase | Contenido | Estado |
|------|-----------|--------|
| A | Normalizador en Java + lemma mínimos | Hecho (v0.4) |
| B | Patrones elípticos compuestos | Hecho (v0.5) |
| **C** | **`lexrule` en `.lgs`** | **Hecho (v0.6)** |

Posibles ampliaciones futuras (no implementadas):

- `lexrule` con condiciones usadas en producción para `-o` desglosado.
- `synonym`, `connector` (mencionados en `Lemma.md`).
- Recarga de `.lgs` sin reinicio.

---

## Referencias

- `NormalizadorMorfologico.md` — historia Fase A y relación con Fase C
- `Lemma.md` — cuándo usar `lemma` vs `lexrule`
- `RoadmapLexico.md` — visión de capas
- `Pattern.md` — patrones semánticos (capa distinta)
- `ErroresCargaLgs.md` — diagnóstico de parseo
