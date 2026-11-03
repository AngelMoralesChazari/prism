# Índice de documentación

Copia de referencia en `documentacion-original/`. Rutas relativas a `migracion-logicscript/`.

---

## Documentos de esta carpeta (síntesis migración)

| Archivo | Descripción |
|---------|-------------|
| `README.md` | Punto de entrada |
| `documentacion LS-J.md` | Guía de integración en Java (clases, métodos, qué hace y qué no) |
| `FILOSOFIA-Y-OBJETIVO.txt` | Visión, filosofía, qué es y qué queremos |
| `PLAN-MIGRACION-PROYECTO-INDEPENDIENTE.md` | Pasos para segundo repositorio |
| `INVENTARIO-CLASES-JAVA.md` | Clases Java a migrar |
| `SINTAXIS-DSL-RESUMEN.md` | Referencia rápida `.lgs` v0.6 |
| `INDICE-DOCUMENTACION.md` | Este índice |

---

## Libro blanco

| Archivo | Descripción |
|---------|-------------|
| `documentacion-original/LogicScript.md` | Visión general, mapa de clases, roadmap, changelog |

---

## Sintaxis y formato `.lgs`

| Archivo | Descripción |
|---------|-------------|
| `documentacion-original/LogicScript/FormatoLGS.md` | Reglas generales del archivo |
| `documentacion-original/LogicScript/Lemma.md` | Directiva `lemma` |
| `documentacion-original/LogicScript/Lexrule.md` | Directiva `lexrule` (Fase C) |
| `documentacion-original/LogicScript/Pattern.md` | Directiva `pattern`, índices left/right |
| `documentacion-original/LogicScript/PatronesCompuestos.md` | imp_and, imp_or, imp_or_ant, etc. |
| `documentacion-original/LogicScript/ConectoresNuevos.md` | Fase 1 conectores |
| `documentacion-original/LogicScript/PatronSiElliptico.md` | Patrón si X Y |
| `documentacion-original/LogicScript/ComposicionComaSi.md` | Coma entre cláusulas |
| `ejemplos/core.lgs` | Datos reales v0.6 |

---

## Arquitectura e implementación Java

| Archivo | Descripción |
|---------|-------------|
| `documentacion-original/LogicScript/MapaImplementacionJava.md` | Concepto → clase → citas código |
| `documentacion-original/LogicScript/SemanticMapper.md` | Capa semántica |
| `documentacion-original/LogicScript/BaseConocimiento.md` | Canonización léxica |
| `documentacion-original/LogicScript/NormalizadorMorfologico.md` | Morfología Fase A |
| `documentacion-original/LogicScript/ErroresCargaLgs.md` | Diagnóstico de parseo |
| `INVENTARIO-CLASES-JAVA.md` | Lista actualizada para migración |

---

## Roadmap y fases completadas

| Archivo | Descripción |
|---------|-------------|
| `documentacion-original/LogicScript/RoadmapLexico.md` | Fases A, B, C del léxico |
| `documentacion-original/LogicScript/PatronesSemanticosFaseB.md` | Patrones elípticos v0.5 |
| `documentacion-original/LogicScript/AmpliacionLemasPatrones.md` | Ampliación léxica |
| `documentacion-original/LogicScript/PruebasRegresion.md` | Harness de regresión |

---

## Formalización académica

| Archivo | Descripción |
|---------|-------------|
| `documentacion-original/LogicScript/PaperFormalLogicScript.md` | Paper: BNF, semántica, abstracción |

---

## Integración con TautoTeacher (histórico)

| Archivo | Descripción |
|---------|-------------|
| `documentacion-original/LogicScript/CambiosIntegracionGeneral.md` | Integración pipeline en app |
| `documentacion-original/IntegracionApp/MigracionPatronesDeclarativos.md` | Patrones declarativos en despliegue |
| `documentacion-original/LogicScript/Fase2PulidoUI.md` | UI (permanece en TautoTeacher) |
| `documentacion-original/LogicScript/VisualizacionClara.md` | Visualización (TautoTeacher) |

---

## Orden de lectura sugerido

1. `FILOSOFIA-Y-OBJETIVO.txt`
2. `documentacion-original/LogicScript.md`
3. `SINTAXIS-DSL-RESUMEN.md` + `ejemplos/core.lgs`
4. `documentacion-original/LogicScript/MapaImplementacionJava.md`
5. `INVENTARIO-CLASES-JAVA.md`
6. `PLAN-MIGRACION-PROYECTO-INDEPENDIENTE.md`
7. Resto según necesidad (Pattern, Lexrule, Paper…)

---

## Estado del roadmap léxico (resumen)

| Fase | Contenido | Versión | Estado |
|------|-----------|---------|--------|
| A | NormalizadorMorfologico | v0.4 | Hecho |
| B | Patrones elípticos compuestos | v0.5 | Hecho |
| C | `lexrule` en `.lgs` | v0.6 | Hecho |
| D | Proyecto independiente + lenguaje completo | v1.0+ | **En curso (esta migración)** |
