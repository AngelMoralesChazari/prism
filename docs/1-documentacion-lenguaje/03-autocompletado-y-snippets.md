# Autocompletado, snippets y capacidades — Prism

**Documento vivo:** actualizar aquí cada vez que se añada un snippet, comando `lsc`, tarea del editor o capacidad del lenguaje.

**Versión:** junio 2026 · extensión **v0.2.0**  
**Relacionado con:** `GUIA-RAPIDA-EJERCICIOS.md`, `EXTENSION-VSCODE.md`, `Language-v1.0.md`, `IMPLEMENTACION-INTERPRETE-LS.md`  
**Fuente de snippets:** `extensions/logicscript/snippets/prism.code-snippets`

---

## Cómo mantener este documento

Al agregar algo nuevo al proyecto:

1. Añadir fila en la tabla correspondiente (snippets, CLI, lenguaje, etc.).
2. Si es snippet, copiar prefijos exactos desde `prism.code-snippets`.
3. Anotar fecha y versión de extensión en el **Historial de cambios** (§8).
4. Opcional: ejemplo en `examples/` con comentario que apunte a este doc.

---

## 1. Activar autocompletado (extensión)

| Paso | Acción |
|------|--------|
| 1 | `.\compile.ps1` (una vez por sesión) |
| 2 | **Ctrl+Shift+P** → **Developer: Install Extension from Location...** |
| 3 | Carpeta: `<repo>/extensions/logicscript` |
| 4 | **Recargar ventana** tras cada actualización de snippets |

**Uso:** en un archivo `.pr`, escribe el **prefijo** → **Tab** (o elige en la lista). **Tab** salta entre campos `${1}`, `${2}`, …

`${TM_FILENAME_BASE}` rellena el `module` con el nombre del archivo (sin `.pr`).

### Tareas del editor (opcional)

```powershell
New-Item -ItemType Directory -Force .vscode
Copy-Item tooling/vscode/tasks.json .vscode/
Copy-Item tooling/vscode/settings.json.example .vscode/settings.json
```

| Tarea | Qué hace |
|-------|----------|
| Prism: compilar proyecto | `compile.ps1` |
| Prism: check archivo actual | `prc check` del `.pr` abierto |
| Prism: run archivo actual | `prc run` del `.pr` abierto |

---

## 2. ¿Qué snippet usar? (Árbol de Decisión y Comparativa)

```
¿Qué tipo de ejercicio vas a resolver?
│
├─► 1. Enunciado en Español / Lenguaje Natural
│    │
│    ├─ ¿Quieres seguir el flujo formal de examen en papel (Recomendado)?
│    │    └─► prnl (o nlex)  ──  nl + atom + let translate(premisa) + vertauto
│    │
│    └─ ¿Es una sola frase corta y quieres ahorrar código?
│         └─► prnlit         ──  atom + let translate("literal") + vertauto
│
├─► 2. Lógica Simbólica Pura (Álgebra de Boole, sin texto en español)
│    │
│    ├─ ¿Quieres darle un nombre a la fórmula para reutilizarla?
│    │    └─► prform         ──  atom + formula f = expr + vertauto f
│    │
│    └─ ¿Es una prueba rápida / cálculo al vuelo sin nombres intermedios?
│         └─► prinl (vainline) ── atom + vertauto (expr inline)
│
└─► 3. Estructura Libre desde Cero
     └─► prog (o prprog)    ──  Solo module + use core (sin plantillas fijas)
```

### Tabla Comparativa de Diferencias y Casos de Uso

| Snippet | Nombre | Estructura que genera | ¿Cuándo usarlo? (Caso de uso exacto) |
| :--- | :--- | :--- | :--- |
| **`prnl`** *(o `nlex`)* | **NL Canónico (Método en Papel)** | `module` + `use core`<br>`nl premisa = "..."`<br>`atom p = "..."`<br>`let f = translate(premisa)`<br>`vertauto f { steps table verdict }` | **Es el snippet principal por defecto.** Úsalo cuando tengas un problema de examen o razonamiento en español. Permite ver el texto original, los átomos identificados y la fórmula resultante paso a paso. |
| **`prnlit`** | **NL con Translate Literal** | `module` + `use core`<br>`atom p = "..."`<br>`let f = translate("si... entonces...")`<br>`vertauto f { steps table verdict }` | Úsalo cuando la frase en español sea muy corta, no necesites reutilizar la variable `nl`, y quieras ahorrar una línea de código pasando el texto directamente a `translate("...")`. |
| **`prform`** | **Fórmula Simbólica Pura** | `module` + `use core`<br>`atom p = "..."`<br>`formula f = p impl q`<br>`vertauto f { steps table verdict }` | Úsalo cuando **no hay texto en español**: te dan directamente una fórmula matemática (e.g. $(p \rightarrow q) \leftrightarrow (\neg p \lor q)$) para probar leyes de De Morgan, tautologías o circuitos digitales. |
| **`prinl`** *(o `vainline`)* | **Simbólico Directo / Inline** | `module` + `use core`<br>`atom p = "..."`<br>`vertauto (p impl q) { steps table verdict }` | Úsalo para comprobaciones instantáneas o pruebas al vuelo entre paréntesis sin necesidad de inventar un nombre de variable (`formula f`). |
| **`prog`** *(o `prprog`)* | **Programa Vacío** | `module nombre`<br>`use core` | Úsalo cuando quieras libertad absoluta para escribir un programa complejo con múltiples enunciados, reglas `rule` o lógica avanzada sin una plantilla fija. |

---

## 3. Snippets de programa completo

Generan un ejercicio listo para editar y ejecutar.

| Prefijo | Nombre | Genera | Ejemplo en repo |
| :--- | :--- | :--- | :--- |
| `prog` `prprog` | Programa vacío | `module` + `use core` | — |
| `nlex` `prnl` | NL canónico (Forma 1) | `nl` + `atom` + `let translate` + `vertauto` | `ejercicio_lluvia.pr`, `prueba_nl.pr` |
| `prnlit` | NL con literal inline | `atom` + `let translate("...")` + `vertauto` | `translate_literal.pr` |
| `prform` | Fórmula simbólica (Forma 2) | `atom` + `formula` + `vertauto` | `test.pr`, `test2.pr` |
| `prinl` `vainline` | Directo / Inline (Forma 3) | `atom` + `vertauto (expr)` | `scratch_3_forms.pr` |
| `tauto` `tautologia` | Tautología | `p or not p` + `vertauto` extendido | `tautologia_estudio.pr` |
| `contra` `contradiccion` | Contradicción | `p and not p` + `vertauto` extendido | — |

---

## 4. Snippets de una línea (bloques sueltos)

Para ampliar un programa ya iniciado con `prog` u otro snippet.

| Prefijo | Inserta |
| :--- | :--- |
| `use` `usecore` | `use core` |
| `atom` | `atom p = "etiqueta"` |
| `nl` | `nl nombre = "texto natural"` |
| `lettr` `translate` | `let f = translate(variable_nl)` |
| `lettrl` `translatelit` | `let f = translate("si estudio apruebo")` |
| `formula` `form` | `formula f = p impl q` |
| `impl` `implica` | `formula f = p impl q` |
| `va` `vertauto` | `vertauto f` (modo corto) |
| `vax` `vertautoext` | `vertauto f { steps table verdict }` |

---

## 5. Comandos `lsc` (terminal)

| Comando | Acción | Código salida |
|---------|--------|---------------|
| `prc run <archivo.pr>` | Valida + ejecuta | 0 OK, 1 error |
| `prc check <archivo.pr>` | Solo valida (sin ejecutar) | 0 OK, 1 error |
| `lsc --version` | Versión de `lsc` | 0 |
| `lsc help` | Ayuda | 0 |

```powershell
java -cp out prism.pr.PrcCli run examples/mi_ejercicio.pr
java -cp out prism.pr.PrcCli check examples/mi_ejercicio.pr
```

**Convención:** `module nombre` debe coincidir con `nombre.pr`.

---

## 6. Qué puede hacer el lenguaje hoy

### Declaraciones

| Constructo | Estado | Notas |
|------------|--------|-------|
| `module` | Disponible | Una por archivo; nombre = archivo |
| `use core` | Disponible | Carga `core.lgs` |
| `atom p = "..."` | Disponible | Enlaza etiquetas en salida de `vertauto` |
| `nl t = "..."` | Disponible | Texto NL reutilizable |
| `let x = translate(t)` | Disponible | Desde variable `nl` |
| `let x = translate("...")` | Disponible | Literal inline |
| `let x = translate(t, modulo)` | Pendiente | Pendiente |
| `formula f = ...` | Disponible | `not and or impl iff` |
| `vertauto f` | Disponible | Modo corto |
| `vertauto f { steps table verdict }` | Disponible | Modo extendido |
| `vertauto (p impl q)` | Disponible | Expresión inline |
| `rule`, `run` | Pendiente | Pendiente |
| `lsc compile` → JVM | Pendiente | Pendiente |

### Operadores en `formula`

| Prism | Significado | NL típico |
|-------------|-------------|-----------|
| `and` | Conjunción ∧ | y, además |
| `or` | Disyunción ∨ | o, o bien |
| `impl` | Implicación → | si… entonces |
| `iff` | Bicondicional ↔ | si y solo si |
| `not` | Negación ¬ | no |

### Dictámenes (`vertauto`)

| Salida | Significado |
|--------|-------------|
| `tautology` | Verdadera en todas las filas |
| `contingency` | Verdadera en algunas, falsa en otras |
| `contradiction` | Falsa en todas las filas |

### Validación (`prc check`)

- Módulos `use` conocidos
- Sin redeclarar identificadores
- Referencias `nl` / `formula` / átomos existentes
- Convención `module` ↔ nombre de archivo

---

## 7. Ejemplos en el repositorio

| Archivo | Patrón | Snippet equivalente |
|---------|--------|---------------------|
| `plantilla_ejercicio.pr` | Referencia comentada A–D | (manual / histórico) |
| `ejercicio_lluvia.pr` | NL + `nl` + translate | `lsnl` |
| `translate_literal.pr` | NL + translate literal | `lsnlit` |
| `tautologia_estudio.pr` | Tautología simbólica | `tauto` / `lsform` |
| `prueba1.pr` | NL personalizado | `lsnl` |
| `prueba2.pr` | `p or not p` | `tauto` |
| `prueba3.pr` | `p and not p` | `contra` |

Archivos personales (`test.pr`, `hola.pr`, etc.): mismas reglas; no es obligatorio versionarlos.

---

## 8. Historial de cambios (documento)

| Fecha | Versión ext. | Añadido |
|-------|--------------|---------|
| 2026-06 | 0.1.0 | Resaltado sintaxis `.pr` |
| 2026-06 | 0.1.0 | Tareas VS Code: compile, check, run |
| 2026-06 | 0.2.0 | Snippets: `lsprog`, `lsnl`, `lsform`, piezas sueltas |
| 2026-06 | 0.2.0 | Snippets: `tauto`, `contra`, `vax`, `lettrl` |
| 2026-06 | 0.2.0 | Snippet `lsnlit` (translate literal completo) |
| 2026-06 | — | `translate("literal")` documentado + `translate_literal.pr` |

### Próximo a documentar aquí cuando exista

- [ ] Snippet / soporte `translate(x, modulo)`
- [ ] `lsc compile`
- [ ] Snippets para `rule`, `run`
- [ ] Diagnósticos en tiempo real en el editor
- [ ] Autocompletado de identificadores (no solo snippets)

---

## 9. Flujo diario recomendado

1. **Archivo nuevo** → guardar como `nombre.pr`
2. Snippet según §2 (`lsprog` / `lsnl` / `lsform` / …)
3. **Tab** entre campos; declarar `atom` con etiquetas NL
4. **Task** o terminal: `prc check` → `prc run`
5. Revisar dictamen y tabla en salida extendida (`vax` o bloque `{ steps table verdict }`)

---

## 10. Referencias cruzadas

| Tema | Documento |
|------|-----------|
| Gramática EBNF | `Language-v1.0.md` |
| Intérprete, validador, arquitectura | `IMPLEMENTACION-INTERPRETE-LS.md` |
| Instalación extensión | `EXTENSION-VSCODE.md` |
| Ejercicios y operadores | `GUIA-RAPIDA-EJERCICIOS.md` |
| **Este documento** | Snippets, autocompletado, capacidades actuales |
