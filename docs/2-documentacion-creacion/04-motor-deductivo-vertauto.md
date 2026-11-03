# Implementación — motor `vertauto` (Fase 2)

**Versión:** 1.0  
**Fecha:** junio 2026  
**Código:** `src/main/java/logicscript/vertauto/`  
**Especificación:** `Language-v1.0.md` §5.4

> Este archivo vive en `documentacion Prism/` y **no se sube al repositorio** (solo código).

---

## 1. Objetivo

Proveer evaluación proposicional y dictamen (`tautology` / `contingency` / `contradiction`) con:

- **Modo corto:** una línea `(fórmula)  dictamen`
- **Modo extendido:** pasos, tabla de verdad, dictamen ampliado

---

## 2. Clases

| Clase | Responsabilidad |
|-------|-----------------|
| `TipoDictamen` | Enum de clasificación |
| `VertautoResult` | DTO + `formatoCorto()` + `imprimirExtendido()` |
| `ParserNotacionClasica` | Parsea `(p → q)`, `¬`, `∧`, `∨`, `↔` |
| `ParserFormulaPrism` | Parsea `p or not p`, `p impl q`, … |
| `EvaluadorProposicional` | Tabla 2^n, evaluación, pasos, dictamen |
| `VertautoService` | API: `vertautoClasica`, `vertautoExpr`, `vertautoDesdeNl` |
| `VertautoCli` | Demostración por consola |
| `VertautoHarness` | 6 pruebas de regresión |

---

## 3. Flujo interno

```text
Entrada (cadena clásica | expr .pr | NL)
    → Parser → LogicExpr (IR existente)
    → EvaluadorProposicional
        1. Recolectar átomos (p, q, …)
        2. Generar 2^n asignaciones
        3. Evaluar fila a fila
        4. Clasificar dictamen
    → VertautoResult
```

---

## 4. Uso (tras `compile.ps1`)

```powershell
# Modo corto
java -cp out tautoteacher2.prism.vertauto.VertautoCli "(p → q)"
java -cp out tautoteacher2.prism.vertauto.VertautoCli --expr "p or not p"

# NL → translate → vertauto
java -cp out tautoteacher2.prism.vertauto.VertautoCli --from-nl "si estudio apruebo"

# Modo extendido
java -cp out tautoteacher2.prism.vertauto.VertautoCli --expr "p or not p" --extended

# Regresión
java -cp out tautoteacher2.prism.vertauto.VertautoHarness
```

---

## 5. Equivalencia con ejemplos `.pr`

| Archivo `examples/` | Comando equivalente hoy |
|---------------------|-------------------------|
| `tautologia_estudio.pr` | `VertautoCli --expr "p or not p" --extended` |
| `ejercicio_lluvia.pr` | `VertautoCli --from-nl "si llueve llevo paraguas" --extended` |

El intérprete `.pr` completo conectará `vertauto` en una fase posterior.

---

## 6. Pendiente

- [ ] Parser / intérprete de archivos `.pr`
- [ ] Pasos pedagógicos más detallados (columnas intermedias ¬p, subexpresiones)
- [ ] Locale español en dictamen (`tautología`, …)
- [ ] Integración en compilador `lsc`

---

## 7. Changelog

| Fecha | Cambio |
|-------|--------|
| 2026-06 | Primera implementación Fase 2 |
