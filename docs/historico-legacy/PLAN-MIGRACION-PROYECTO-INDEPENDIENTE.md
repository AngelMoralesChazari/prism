# Plan de migración a proyecto independiente

Guía práctica para extraer LogicScript de TautoTeacher 2.0 y convertirlo en un **segundo repositorio** con su propio ciclo de vida.

---

## 1. Objetivo de la separación

| Antes | Después |
|-------|---------|
| `tautoteacher2.logicscript` + `tautoteacher2.nlp` dentro de TautoTeacher | Paquete raíz propio, p. ej. `org.logicscript` |
| `core.lgs` en recursos de la app | Recursos del proyecto LogicScript |
| `MotorLogico` acoplado en CLI demo | CLI solo traduce; evaluación es adaptador opcional |
| Documentación en `docs/` (gitignored parcialmente) | Documentación en repo LogicScript (versionada) |

---

## 2. Qué copiar al nuevo repositorio

### 2.1 Código Java (núcleo obligatorio)

**Paquete `logicscript` (orquestación + IR):**

| Archivo | Rol |
|---------|-----|
| `LogicScriptEngine.java` | Pipeline completo |
| `LogicScriptService.java` | API pública |
| `LogicScriptResult.java` | DTO de salida |
| `RegistroProposiciones.java` | Asignación p, q, r |
| `LogicScriptCli.java` | CLI (quitar dependencia de MotorLogico o mover a módulo `adapter`) |
| `LogicScriptRegressionHarness.java` | Regresión |
| `ir/LogicExpr.java` | Contrato IR sellado |
| `ir/AtomExpr.java`, `NegExpr.java`, `AndExpr.java`, `OrExpr.java`, `ImpExpr.java`, `EquivExpr.java` | Nodos IR |
| `ir/EmitidorFormula.java` | IR → cadena |

**Paquete `nlp` (front-end lingüístico):**

| Subpaquete | Archivos |
|------------|----------|
| `nlp.normalizacion` | `NormalizadorTexto.java` |
| `nlp.lexer` | `NaturalLexer.java`, `TokenNatural.java`, `TipoTokenNatural.java` |
| `nlp.lexicon` | `LgsCargador.java`, `ContenidoLgs.java`, `BaseConocimiento.java`, `PatronSemanticoLgs.java`, `TipoSalidaIrPatron.java`, `NormalizadorMorfologico.java`, `ReglaMorfologicaLgs.java`, `ConfiguracionMorfologiaLgs.java`, `ResultadoCargaLgs.java`, `EstadoCargaLgs.java` |
| `nlp.semantica` | `SemanticMapper.java` |

### 2.2 Código que NO copiar (permanece en TautoTeacher)

| Archivo / paquete | Motivo |
|-------------------|--------|
| `tautoteacher2.ui.*` | Interfaz Swing |
| `tautoteacher2.core.logica.MotorLogico` | Evaluador pedagógico (cliente) |
| `tautoteacher2.core.visualizacion.*` | Árboles y gráficos |
| `nlp.parser.*` (stubs vacíos) | No usados en el pipeline actual |
| `nlp.lexicon.Lexico.java`, `LexerNatural.java`, stubs duplicados | Legacy sin uso |
| `TautoTeacher.java`, `Main.java` | App principal |

### 2.3 Recursos y datos

```
src/main/resources/logicscript/core.lgs   → copiar (ver ejemplos/core.lgs)
```

### 2.4 Documentación

Copiar todo `migracion-logicscript/` al nuevo repo como `docs/` o raíz del proyecto.

### 2.5 Build

- `compile.ps1` adaptado (solo fuentes LogicScript + copia de resources)
- O migrar a **Maven** / **Gradle** con `groupId` propio

---

## 3. Estructura sugerida del nuevo repositorio

```text
logicscript/
├── README.md
├── LICENSE
├── pom.xml                    # o build.gradle.kts
├── docs/                      # contenido de migracion-logicscript
├── src/main/java/org/logicscript/
│   ├── api/                   # LogicScriptService, LogicScriptResult
│   ├── engine/                # LogicScriptEngine
│   ├── ir/                    # LogicExpr, EmitidorFormula, ...
│   ├── nlp/
│   │   ├── lexer/
│   │   ├── lexicon/
│   │   ├── normalizacion/
│   │   └── semantica/
│   └── cli/                   # LogicScriptCli, RegressionHarness
├── src/main/resources/
│   └── logicscript/core.lgs
└── src/test/java/             # futuro: JUnit desde harness
```

---

## 4. Pasos de migración (orden recomendado)

### Fase 1 — Extracción mecánica
1. Crear repositorio `logicscript` (GitHub/GitLab).
2. Copiar clases listadas en §2.1 con refactor de paquete `tautoteacher2.*` → `org.logicscript.*`.
3. Copiar `core.lgs` y script de compilación.
4. Ajustar imports internos; compilar hasta verde.
5. Ejecutar `LogicScriptRegressionHarness` en el nuevo repo.

### Fase 2 — Desacoplar TautoTeacher
1. Publicar JAR local o usar submódulo git.
2. En TautoTeacher: dependencia al JAR; `TautoTeacherApp` llama a la nueva API.
3. Eliminar duplicados de código en TautoTeacher cuando el JAR funcione.

### Fase 3 — Identidad de lenguaje
1. Especificación `.lgs` v1.0 (evolucionar desde v0.6).
2. Versionado semántico del formato.
3. CLI con subcomandos: `validate`, `translate`, `explain`.

### Fase 4 — Ampliación declarativa
1. Directivas `synonym`, `connector` (previstas en roadmap).
2. Negación declarativa en `.lgs`.
3. Múltiples archivos `.lgs` con `import` o capas.

---

## 5. API pública mínima (contrato estable)

```java
public interface LogicScript {
    LogicScriptResult traducir(String textoNatural);
}

public final class LogicScriptResult {
    boolean isExito();
    String getFormula();
    String getMensaje();
    List<String> getPasosDeAnalisis();
    Map<String, String> getProposiciones(); // fragmento → p, q, ...
    // Futuro: LogicExpr getIr();
}
```

TautoTeacher solo necesita esta fachada + opcionalmente la IR para explicaciones avanzadas.

---

## 6. Cambios en LogicScriptCli

Hoy `LogicScriptCli` importa `MotorLogico` de TautoTeacher. En el proyecto independiente:

- **Opción A:** CLI solo imprime fórmula y pasos (recomendado para núcleo).
- **Opción B:** Módulo `logicscript-adapter-tautoteacher` con evaluación (dependencia opcional).

---

## 7. Renombrado de paquetes (tabla)

| Actual | Propuesto |
|--------|-----------|
| `tautoteacher2.logicscript` | `org.logicscript.core` o `org.logicscript.engine` |
| `tautoteacher2.logicscript.ir` | `org.logicscript.ir` |
| `tautoteacher2.nlp.lexer` | `org.logicscript.nlp.lexer` |
| `tautoteacher2.nlp.lexicon` | `org.logicscript.nlp.lexicon` |
| `tautoteacher2.nlp.semantica` | `org.logicscript.nlp.semantica` |
| `tautoteacher2.nlp.normalizacion` | `org.logicscript.nlp.normalizacion` |

---

## 8. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| Regresiones al mover paquetes | Harness 40 casos antes de cada release |
| TautoTeacher roto | Mantener JAR versionado; integración en rama aparte |
| Documentación desactualizada | `docs/` en repo LogicScript, no gitignored |
| Duplicación de `core.lgs` | Una sola fuente de verdad en repo LogicScript; TautoTeacher consume JAR |

---

## 9. Checklist de cierre

- [ ] Repo `logicscript` compila y harness OK
- [ ] Sin referencias a `tautoteacher2` en el núcleo
- [ ] `core.lgs` v0.6 incluido en JAR
- [ ] README con instalación y ejemplos
- [ ] TautoTeacher depende del artefacto publicado
- [ ] Carpeta `migracion-logicscript` en TautoTeacher apunta al nuevo repo (URL)

---

## 10. Referencias

- `FILOSOFIA-Y-OBJETIVO.txt` — visión del lenguaje
- `INVENTARIO-CLASES-JAVA.md` — lista completa de archivos
- `documentacion-original/LogicScript/MapaImplementacionJava.md` — trazabilidad concepto → código
