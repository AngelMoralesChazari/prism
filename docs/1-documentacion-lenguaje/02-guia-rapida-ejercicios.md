# Guía rápida — programar ejercicios en Prism

**Versión:** junio 2026  
**Relacionado con:** `Language-v1.0.md`, `IMPLEMENTACION-INTERPRETE-LS.md`, `EXTENSION-VSCODE.md`, **`AUTOCOMPLETADO-SNIPPETS-Y-CAPACIDADES-LS.md`** (snippets y capacidades — documento vivo)

---

## 1. Flujo de trabajo

```powershell
# Una vez por sesión (o tras cambiar Java)
.\compile.ps1

# Validar sintaxis y referencias (sin ejecutar)
java -cp out prism.pr.PrcCli check examples/mi_ejercicio.pr

# Ejecutar
java -cp out prism.pr.PrcCli run examples/mi_ejercicio.pr
```

**Convención:** el `module` debe coincidir con el nombre del archivo sin `.pr`.

| Archivo | Primera línea obligatoria |
|---------|---------------------------|
| `mi_ejercicio.pr` | `module mi_ejercicio` |

---

## 2. Plantillas Rápidas con Snippets (`pr...`)

En lugar de copiar archivos manualmente, en un archivo `.pr` vacío escribe cualquiera de estos atajos y pulsa **Tab** o **Enter**:

| Atajo | Tipo de Ejercicio | Cuándo usarlo |
| :--- | :--- | :--- |
| **`prnl`** *(o `nlex`)* | **Lenguaje Natural Canónico** | Enunciado en español completo con variables atómicas identificadas y traducidas formalmente. |
| **`prnlit`** | **NL con Literal Directo** | Frase corta en español directamente dentro de `translate("...")` para ahorrar código. |
| **`prform`** | **Fórmula Simbólica** | Ejercicios de álgebra booleana pura (`and`, `or`, `impl`, `not`) sin texto en español. |
| **`prinl`** *(o `vainline`)* | **Directo / Inline** | Prueba rápida evaluando la expresión directamente en `vertauto (p and not q)`. |
| **`prog`** *(o `prprog`)* | **Programa Vacío** | Solo `module` + `use core` para escribir código libre sin plantilla predefinida. |

---

## 3. Operadores lógicos (`.pr`)

| Lenguaje natural | Prism | Notación clásica |
|------------------|-------------|------------------|
| y, además | `and` | ∧ |
| o, o bien | `or` | ∨ |
| si… entonces | `impl` | → |
| si y solo si | `iff` | ↔ |
| no | `not` | ¬ |

### Errores frecuentes

| Frase | Fórmula correcta | Incorrecta |
|-------|------------------|------------|
| «Estudio **o** no estudio» | `p or not p` | `p and not p` → **contradiction** |
| «Estudio **y** apruebo» | `p and q` | `p or q` |

---

## 4. Orden Canónico de Declaraciones (Método en Papel)

```prism
module nombre          # = nombre.pr
use core               # carga core.pr

# 1. Enunciado en lenguaje natural
nl premisa = "si llueve llevo paraguas"

# 2. Definición de átomos
atom p = "llueve"
atom q = "paraguas"

# 3. Traducción o formalización
let f = translate(premisa)
formula h = p impl q

# 4. Evaluación formal
vertauto f { steps table verdict }
```

### 4.1 Las 3 Formas de Evaluación en Prism

1. **Forma 1: Desde Lenguaje Natural (`nl` + `translate`)**
   ```prism
   nl premisa = "Si el procesador se suspende entonces la tarea se reintenta."
   atom p = "el procesador se suspende"
   atom q = "la tarea se reintenta"
   let f = translate(premisa)
   vertauto f { table verdict }
   ```
2. **Forma 2: Fórmula Simbólica con Nombre (`formula`)**
   ```prism
   atom p = "el procesador se suspende"
   atom q = "la tarea se reintenta"
   formula f = p impl q
   vertauto f { table verdict }
   ```
3. **Forma 3: Expresión Simbólica Directa / Inline (`vertauto (expr)`)**
   ```prism
   atom p = "el procesador se suspende"
   atom q = "la tarea se reintenta"
   vertauto (p and not q) { table verdict }
   # O en modo corto de 1 línea:
   vertauto (p impl q)
   ```

### `translate`: dos formas

| Forma | Cuándo usarla |
|-------|---------------|
| `nl t = "..."` + `translate(t)` | Reutilizas el mismo texto varias veces |
| `translate("...")` | Una sola frase, ejercicio más corto (snippet `lettrl` o `lsnlit`) |

---

## 5. Ejemplos en el repo

| Archivo | Qué demuestra |
|---------|---------------|
| `ejercicio_lluvia.pr` | NL + translate + vertauto extendido |
| `tautologia_estudio.pr` | `p or not p` tautología |
| `plantilla_ejercicio.pr` | Plantilla con patrones A–D |
| `prueba1.pr` | translate desde NL personalizado |
| `prueba2.pr` | Tautología `p or not p` |
| `translate_literal.pr` | `translate("...")` sin `nl` |

---

## 6. Dictámenes (`vertauto`)

| Salida | Significado |
|--------|-------------|
| `tautology` | Verdadera en todas las filas |
| `contingency` | Verdadera en algunas, falsa en otras |
| `contradiction` | Falsa en todas las filas |

---

## 7. Editor (VS Code / Cursor)

**Documento principal de snippets y autocompletado:** `AUTOCOMPLETADO-SNIPPETS-Y-CAPACIDADES-LS.md` (árbol de decisión, tablas, historial de cambios).

Resumen: extensión en `extensions/logicscript`, prefijos `lsprog`, `lsnl`, `lsform`, `lsnlit`, … + tareas `check` / `run`.

```powershell
Copy-Item -Recurse tooling/vscode/* .vscode/
```

Tras cambios en la extensión: reinstalar desde `extensions/logicscript` y recargar ventana.

---

## 8. Próximos pasos del lenguaje

- `translate(x, modulo)` con `.lgs` custom
- `lsc compile` → bytecode JVM
