# Extensión VS Code / Cursor — Prism

**Versión:** 0.2.0  
**Ubicación en repo:** `extensions/logicscript/`  
**Relacionado con:** `GUIA-RAPIDA-EJERCICIOS.md`, **`AUTOCOMPLETADO-SNIPPETS-Y-CAPACIDADES-LS.md`**

---

## 1. Qué incluye

| Componente | Ruta | Función |
|------------|------|---------|
| Extensión | `extensions/logicscript/` | Resaltado `.pr` + **snippets** |
| Tareas | `tooling/vscode/tasks.json` | `prc check`, `prc run`, `compile.ps1` |
| Settings ejemplo | `tooling/vscode/settings.json.example` | Asociar `*.pr` → `logicscript` |

---

## 2. Instalar extensión (local)

1. Compila el proyecto: `.\compile.ps1`
2. En VS Code / Cursor: **Ctrl+Shift+P**
3. **Developer: Install Extension from Location...**
4. Carpeta: `<repo>/extensions/logicscript`
5. Recarga ventana si hace falta

---

## 3. Activar tareas

La carpeta `.vscode/` está en `.gitignore`. Copia manualmente:

```powershell
New-Item -ItemType Directory -Force .vscode
Copy-Item tooling/vscode/tasks.json .vscode/
Copy-Item tooling/vscode/settings.json.example .vscode/settings.json
```

### Tareas disponibles

| Tarea | Atajo sugerido |
|-------|----------------|
| Prism: compilar proyecto | `Ctrl+Shift+B` (grupo build) |
| Prism: check archivo actual | Tasks → Run Task |
| Prism: run archivo actual | Tasks → Run Task |

Requisito: archivo `.pr` abierto y foco en el editor para `${file}`.

---

## 4. Snippets (v0.2.0)

**Referencia completa (tablas, árbol de decisión, historial):** `AUTOCOMPLETADO-SNIPPETS-Y-CAPACIDADES-LS.md`

Resumen rápido — en un `.pr`, prefijo + **Tab**:

| Prefijo | Qué genera |
|---------|------------|
| `lsprog` | Programa vacío: `module` (= nombre archivo) + `use core` |
| `lsnl` | Ejercicio NL → `translate` → `vertauto` extendido |
| `lsform` | Ejercicio con `formula` simbólica + `vertauto` |
| `tauto` | Tautología `p or not p` |
| `contra` | Contradicción `p and not p` |
| `lettr` | `let f = translate(variable_nl)` |
| `lettrl` | `let f = translate("literal")` |
| `vax` | `vertauto f { steps table verdict }` |

Tras actualizar la extensión: **Developer: Install Extension from Location...** otra vez y recargar ventana.

Archivo: `extensions/logicscript/snippets/prism.code-snippets`

---

## 5. Gramática (keywords resaltadas)

**Control:** `module`, `use`, `atom`, `formula`, `nl`, `let`, `translate`, `vertauto`, `steps`, `table`, `verdict`

**Operadores:** `not`, `and`, `or`, `impl`, `iff`

**Comentarios:** `#` hasta fin de línea

**Cadenas:** `"..."`

---

## 6. Publicación futura

Para empaquetar VSIX (opcional):

```powershell
npm install -g @vscode/vsce
cd extensions/logicscript
vsce package
```

Instalar el `.vsix` generado desde el panel de extensiones.

---

## 7. Roadmap extensión

- Diagnósticos en tiempo real vía `prc check`
- Depurador
- Empaquetar `.vsix` para instalación sin carpeta local
