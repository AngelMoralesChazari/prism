# Migración LogicScript → lenguaje declarativo independiente

Este directorio reúne **toda la documentación, sintaxis, arquitectura y material de referencia** de **LogicScript** tal como existe hoy dentro de **TautoTeacher 2.0**, preparado para trabajarlo como **proyecto separado**.

## Propósito

LogicScript nació como **motor DSL embebido** en una aplicación educativa (Swing). El objetivo de esta carpeta es servir de **paquete de migración**: todo lo necesario para convertirlo en un **lenguaje de programación declarativo completo**, enfocado en la **lógica**, sin depender de la UI ni del resto de TautoTeacher.

## Empieza aquí

| Archivo | Contenido |
|---------|-----------|
| **`FILOSOFIA-Y-OBJETIVO.txt`** | Qué hace el DSL hoy, qué queremos que haga, filosofía y enfoque lógico (lectura principal). |
| **`PLAN-MIGRACION-PROYECTO-INDEPENDIENTE.md`** | Pasos concretos para extraer código, estructura del nuevo repo y frontera con TautoTeacher. |
| **`INVENTARIO-CLASES-JAVA.md`** | Todas las clases Java del pipeline, paquetes y responsabilidades. |
| **`SINTAXIS-DSL-RESUMEN.md`** | Referencia rápida de `.lgs` v0.6 (`lemma`, `lexrule`, `pattern`). |
| **`INDICE-DOCUMENTACION.md`** | Índice de los 20+ documentos copiados. |

## Estructura de carpetas

```text
migracion-logicscript/
├── README.md                          ← este archivo
├── FILOSOFIA-Y-OBJETIVO.txt
├── PLAN-MIGRACION-PROYECTO-INDEPENDIENTE.md
├── INVENTARIO-CLASES-JAVA.md
├── SINTAXIS-DSL-RESUMEN.md
├── INDICE-DOCUMENTACION.md
├── ejemplos/
│   └── core.lgs                       ← léxico y patrones v0.6 (copia de referencia)
└── documentacion-original/
    ├── LogicScript.md                 ← libro blanco del pseudolenguaje
    ├── LogicScript/                   ← documentación detallada por componente
    └── IntegracionApp/                ← notas de integración con la app (histórico)
```

## Estado actual del DSL (resumen)

- **Versión de datos:** `.lgs` **v0.6**
- **Directivas:** `version`, `lemma`, `lexrule`, `pattern`
- **Pipeline:** español normalizado → tokens → patrones semánticos → IR → fórmula proposicional
- **Implementación:** Java 17+, sin dependencias externas
- **Pruebas:** `LogicScriptRegressionHarness` (~40 casos LN → fórmula)
- **Ubicación en TautoTeacher:** paquetes `tautoteacher2.logicscript` y `tautoteacher2.nlp.*`

## Dos proyectos, una frontera clara

| Proyecto | Rol |
|----------|-----|
| **LogicScript** (futuro, independiente) | Lenguaje declarativo, compilador/intérprete, `.lgs`, IR, emisión de fórmulas, CLI propia. |
| **TautoTeacher 2.0** | Aplicación educativa: UI, visualización, `MotorLogico`, explicaciones pedagógicas. **Consume** LogicScript como dependencia o librería. |

## Código fuente (no copiado aquí)

El código Java sigue en el árbol de fuentes de TautoTeacher. Ver `INVENTARIO-CLASES-JAVA.md` para rutas exactas. Al crear el repo nuevo, copiar o extraer esos paquetes según `PLAN-MIGRACION-PROYECTO-INDEPENDIENTE.md`.

## Compilar y probar (en TautoTeacher, mientras tanto)

```powershell
cd TautoTeacher2.0
.\compile.ps1 -JdkHome "C:\Program Files\Java\jdk-25.0.2"
java -cp out tautoteacher2.logicscript.LogicScriptRegressionHarness
java -cp out tautoteacher2.logicscript.LogicScriptCli "si estudio apruebo"
```

---

*Generado para separar LogicScript de TautoTeacher 2.0. La documentación en `documentacion-original/` es copia fiel de `docs/` al momento de la migración.*
