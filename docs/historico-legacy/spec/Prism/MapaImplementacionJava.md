# LogicScript: mapa concepto → implementación Java

Este documento responde **de dónde “procede”** cada pieza de LogicScript en el código: **qué clase** la materializa, **qué hace a nivel de lógica** (no un tutorial línea a línea) y **fragmentos de referencia** al fuente.

Orden aproximado del flujo en runtime:

```text
Texto  →  NormalizadorTexto  →  NaturalLexer  →  SemanticMapper (+ BaseConocimiento)  →  LogicExpr (IR)
  →  EmitidorFormula (+ RegistroProposiciones)  →  cadena para MotorLogico
```

Los datos declarativos (`lemma`, `pattern`) viven en **`src/main/resources/logicscript/core.lgs`** y entran por **`LgsCargador`**.

---

## 1. Archivo `.lgs` y directivas `lemma` / `pattern`

| Concepto | Clase Java principal | Rol lógico |
|----------|----------------------|------------|
| Leer `.lgs`, reconocer directivas | `tautoteacher2.nlp.lexicon.LgsCargador` | Parser **línea a línea** (no hay gramática BNF completa): ignora vacías, `#`, `version`; despacha `lemma` y `pattern`; error si la línea no es reconocida. |
| Resultado del parseo | `tautoteacher2.nlp.lexicon.ContenidoLgs` | **DTO inmutable**: mapa de lemas + lista ordenada de patrones. Un solo objeto comparte **motor** (lemas + patrones del mismo archivo). |

**Por qué esta idea:** separar **datos** (`.lgs`) de **código** (Java). El cargador es el único lugar que interpreta la sintaxis textual; el resto del sistema solo ve estructuras tipadas.

Lectura y despacho de directivas:

```53:80:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/lexicon/LgsCargador.java
    public static ContenidoLgs cargar(InputStream entrada) throws IOException {
        Map<String, String> lemas = new LinkedHashMap<>();
        List<PatronSemanticoLgs> patrones = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String linea;
            int num = 0;
            while ((linea = br.readLine()) != null) {
                num++;
                String t = linea.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                String tl = t.toLowerCase(Locale.ROOT);
                if (tl.startsWith("version")) {
                    continue;
                }
                if (tl.startsWith("lemma ")) {
                    parsearLemma(num, t, lemas);
                    continue;
                }
                if (tl.startsWith("pattern ")) {
                    patrones.add(parsearPattern(num, t));
                    continue;
                }
                throw new IOException("Línea " + num + ": directiva no reconocida: " + linea);
            }
        }
        return new ContenidoLgs(lemas, patrones);
    }
```

Carga desde classpath (recurso ausente o `IOException` → bundle vacío; entonces el motor usa respaldos embebidos en otras clases):

```30:44:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/lexicon/LgsCargador.java
    public static ContenidoLgs cargarDesdeClasspath(String rutaRecurso) {
        Objects.requireNonNull(rutaRecurso, "rutaRecurso");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = LgsCargador.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(rutaRecurso)) {
            if (in == null) {
                return ContenidoLgs.vacio();
            }
            return cargar(in);
        } catch (IOException e) {
            return ContenidoLgs.vacio();
        }
    }
```

---

## 2. Directiva `lemma` (forma → lema canónico)

| Concepto | Clase Java | Rol lógico |
|----------|------------|------------|
| Parseo `lemma forma -> lema` | `LgsCargador.parsearLemma` | Parte la línea por `->`, normaliza a minúsculas, rellena un `Map<String,String>`. |
| **Uso** del mapa (canonización) | `tautoteacher2.nlp.lexicon.BaseConocimiento` | Sustituye **palabra a palabra** por lema si existe entrada; limpia artículos al inicio. Si el `.lgs` no aporta lemas, usa un mapa **embebido** equivalente al demo. |

**Por qué esta idea:** misma noción que en **morfología / NLP**: varias superficies (*llueve*, *llueva*) → un **predicado estable** para no duplicar proposiciones en el registro de símbolos.

Parseo de `lemma`:

```87:99:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/lexicon/LgsCargador.java
    private static void parsearLemma(int numLinea, String linea, Map<String, String> lemas) throws IOException {
        String resto = linea.substring(6).trim();
        int flecha = resto.indexOf("->");
        if (flecha < 0) {
            throw new IOException("Línea " + numLinea + ": falta '->' en lemma: " + linea);
        }
        String forma = resto.substring(0, flecha).trim().toLowerCase(Locale.ROOT);
        String lema = resto.substring(flecha + 2).trim().toLowerCase(Locale.ROOT);
        if (forma.isEmpty() || lema.isEmpty()) {
            throw new IOException("Línea " + numLinea + ": forma o lema vacío: " + linea);
        }
        lemas.put(forma, lema);
    }
```

Canonización consumiendo ese mapa:

```36:57:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/lexicon/BaseConocimiento.java
    public String canonicalizarFragmento(String fragmento) {
        if (fragmento == null) {
            return "";
        }
        String limpio = fragmento.trim().toLowerCase();
        if (limpio.isEmpty()) {
            return "";
        }

        // Limpieza superficial de palabras funcionales al inicio.
        limpio = limpio.replaceAll("^(el|la|los|las|un|una|unos|unas)\\s+", "");
        limpio = limpio.replaceAll("\\s+", " ").trim();

        String[] palabras = limpio.split(" ");
        for (int i = 0; i < palabras.length; i++) {
            String lema = lemas.get(palabras[i]);
            if (lema != null) {
                palabras[i] = lema;
            }
        }
        return String.join(" ", palabras).trim();
    }
```

`BaseConocimiento` puede construirse con el mismo `ContenidoLgs` que el motor (**solo usa** `lemas()`):

```19:34:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/lexicon/BaseConocimiento.java
    public BaseConocimiento() {
        this(LgsCargador.cargarDesdeClasspath(RECURSO_LEMAS));
    }

    /**
     * Usa el mismo {@link ContenidoLgs} que puede incluir patrones; aquí solo se consumen los lemas.
     */
    public BaseConocimiento(ContenidoLgs contenido) {
        Objects.requireNonNull(contenido, "contenido");
        Map<String, String> desdeArchivo = contenido.lemas();
        if (!desdeArchivo.isEmpty()) {
            lemas.putAll(desdeArchivo);
        } else {
            registrarLemasIniciales();
        }
    }
```

---

## 3. Directiva `pattern` (forma de tokens → conectiva IR)

| Concepto | Clase Java | Rol lógico |
|----------|------------|------------|
| Parseo de la línea `pattern` | `LgsCargador.parsearPattern` | Parte por `=>`, construye lista de `TipoTokenNatural`, lee `imp`/`and`/`or` y `left=`/`right=`, valida con `PatronSemanticoLgs`. |
| Registro de un patrón cargado | `tautoteacher2.nlp.lexicon.PatronSemanticoLgs` | **Record** inmutable: nombre, forma, tipo de salida, índices de literales. El compact constructor exige que esos índices apunten a `LITERAL`. |
| Tipo de conectiva de salida | `tautoteacher2.nlp.lexicon.TipoSalidaIrPatron` | Enum **puente** entre el archivo (texto `imp`, `and`, `or`) y las clases IR (`ImpExpr`, `AndExpr`, `OrExpr`). |

**Por qué esta idea:** es la parte **declarativa** del “parser de patrones”: igual que en compiladores se separa **reconocimiento de estructura** (lista de tipos) de **acción semántica** (qué hijos alimentan el nodo). Los índices son análogos a referencias por posición (`$1`, `$2`… en Yacc); la explicación detallada está en `docs/LogicScript/Pattern.md`.

Validación al construir el record:

```23:34:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/lexicon/PatronSemanticoLgs.java
    public PatronSemanticoLgs {
        forma = List.copyOf(forma);
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre vacío");
        }
        if (indiceIzq < 0 || indiceIzq >= forma.size() || indiceDer < 0 || indiceDer >= forma.size()) {
            throw new IllegalArgumentException("índices fuera de rango para forma: " + nombre);
        }
        if (forma.get(indiceIzq) != TipoTokenNatural.LITERAL || forma.get(indiceDer) != TipoTokenNatural.LITERAL) {
            throw new IllegalArgumentException("slots left/right deben ser literal en: " + nombre);
        }
    }
```

---

## 4. Normalización del texto (antes del lexer)

| Concepto | Clase Java | Rol lógico |
|----------|------------|------------|
| Texto estable para tokenizar | `tautoteacher2.nlp.normalizacion.NormalizadorTexto` | Minúsculas, NFD sin marcas diacríticas, unificación de espacios y `;` → `,`. **Un solo criterio** para que `lemma` y conectores del lexer coincidan con lo escrito en `.lgs`. |

```11:23:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/normalizacion/NormalizadorTexto.java
    public String normalizar(String textoOriginal) {
        if (textoOriginal == null) {
            return "";
        }
        String t = textoOriginal.trim().toLowerCase();
        if (t.isEmpty()) {
            return "";
        }
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        t = t.replaceAll("[;:]+", ",");
        t = t.replaceAll("\\s+", " ");
        return t;
    }
```

---

## 5. Lexer de lenguaje natural (tokens para los patrones)

| Concepto | Clase Java | Rol lógico |
|----------|------------|------------|
| Clasificación de unidades | `tautoteacher2.nlp.lexer.TipoTokenNatural` | Enum: conectores fijos + `LITERAL`. **Debe alinearse** con los nombres que `LgsCargador` acepta en `pattern` (`si`, `literal`, …). |
| Recorrido y emisión | `tautoteacher2.nlp.lexer.NaturalLexer` | Escaneo con **palabras clave ordenadas de más larga a más corta** (“en caso de que ” antes que “si ”) para evitar cortar mal fronteras. En cada posición se intenta **primero** coincidencia de palabra clave y **después** se consumen espacios/comas, para que patrones como ` y ` (con espacio inicial) apliquen tras un literal (*X y Y*). Todo lo que no es clave es un literal hasta la siguiente clave o coma. |
| Par (tipo, texto) | `tautoteacher2.nlp.lexer.TokenNatural` | Contenedor simple; `toString` ayuda a los pasos de análisis. |

**Por qué esta idea:** es el **analizador léxico** del pipeline (equivalente superficial a la fase *lex* de un compilador). Los `pattern` del `.lgs` no sustituyen este código: describen **qué secuencia de tipos** debe salir **si** el lexer sigue estas reglas.

Esqueleto del lexer:

```28:57:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/lexer/NaturalLexer.java
    public List<TokenNatural> tokenizar(String normalizado) {
        List<TokenNatural> salida = new ArrayList<>();
        if (normalizado == null || normalizado.isEmpty()) {
            return salida;
        }
        String s = normalizado;
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == ' ' || c == ',') {
                i++;
                continue;
            }
            int[] kw = encontrarPalabraClave(s, i);
            if (kw != null) {
                int fin = kw[0];
                int indiceTipo = kw[1];
                salida.add(new TokenNatural(TIPOS[indiceTipo], PALABRAS_CLAVE[indiceTipo].trim()));
                i = fin;
                continue;
            }
            int j = finLiteral(s, i);
            String lit = s.substring(i, j).trim();
            if (!lit.isEmpty()) {
                salida.add(new TokenNatural(TipoTokenNatural.LITERAL, lit));
            }
            i = j;
        }
        return salida;
    }
```

---

## 6. Mapeo tokens → IR (`SemanticMapper`)

| Concepto | Clase Java | Rol lógico |
|----------|------------|------------|
| Patrones + fallback | `tautoteacher2.nlp.semantica.SemanticMapper` | Recorre la lista de `PatronSemanticoLgs` **en orden**; la primera forma que coincida en longitud y **tipos** gana. Construye `ImpExpr` / `AndExpr` / `OrExpr` leyendo lexemas por índice. Si no hay `pattern` en archivo, usa `patronesPredeterminados()` (misma semántica que `core.lgs`). |
| Átomos y lematización en el mapper | `BaseConocimiento.canonicalizarFragmento` | Cada literal del patrón pasa por `atomoDesde`, que aplica **“no ”** opcional y canonización antes de crear `AtomExpr`. |

**Por qué esta idea:** capa de **acción semántica** ligada a **coincidencia de plantillas** sobre tokens (típico en motores de reglas antes de un parser recursivo completo). Mantiene el **motor** (`LogicScriptEngine`) libre de `if` por cada construcción española.

Bucle principal:

```41:75:TautoTeacher2.0/src/main/java/tautoteacher2/nlp/semantica/SemanticMapper.java
    public LogicExpr mapearBloque(String textoBloque, List<TokenNatural> tokens, List<String> pasosDeAnalisis) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }

        for (PatronSemanticoLgs p : patrones) {
            if (coincideForma(tokens, p.forma())) {
                pasosDeAnalisis.add("SemanticMapper: patrón " + p.nombre() + " detectado.");
                return construirDesdePatron(p, tokens);
            }
        }

        String literal = primerLiteral(tokens);
        if (literal != null) {
            pasosDeAnalisis.add("SemanticMapper: fallback a átomo simple.");
            return atomoDesde(literal);
        }

        if (textoBloque == null || textoBloque.isBlank()) {
            return null;
        }
        pasosDeAnalisis.add("SemanticMapper: fallback por texto completo.");
        return atomoDesde(textoBloque);
    }

    private LogicExpr construirDesdePatron(PatronSemanticoLgs p, List<TokenNatural> tokens) {
        String lexIzq = tokens.get(p.indiceIzq()).getLexema();
        String lexDer = tokens.get(p.indiceDer()).getLexema();
        AtomExpr a = atomoDesde(lexIzq);
        AtomExpr b = atomoDesde(lexDer);
        return switch (p.tipoIr()) {
            case IMP -> new ImpExpr(a, b);
            case AND -> new AndExpr(a, b);
            case OR -> new OrExpr(a, b);
        };
    }
```

---

## 7. IR (árbol de expresión proposicional)

| Concepto | Clase Java | Rol lógico |
|----------|------------|------------|
| Contrato sellado | `tautoteacher2.logicscript.ir.LogicExpr` | Interfaz **sealed**: solo los nodos permitidos pueden ser `LogicExpr`. Facilita `switch`/pattern matching exhaustivo al emitir. |
| Hojas | `AtomExpr` | Proposición atómica: texto ya normalizado/canónico + bandera `negada`. |
| Conectivas binarias | `AndExpr`, `OrExpr`, `ImpExpr` | Estructura de árbol binario; nombres (`antecedente`/`consecuente` en `ImpExpr`) reflejan lógica proposicional. |
| Negación (IR general) | `NegExpr` | Existe en el modelo; **los patrones actuales** no lo generan desde el `.lgs`, pero `EmitidorFormula` ya lo soporta para evolución. |

**Por qué esta idea:** misma función que un **AST** en un compilador: representación **independiente del español** que solo habla de conectivas y átomos. Así el **motor lógico** no conoce “si… entonces…”.

```7:8:TautoTeacher2.0/src/main/java/tautoteacher2/logicscript/ir/LogicExpr.java
public sealed interface LogicExpr permits AtomExpr, NegExpr, AndExpr, OrExpr, ImpExpr {
}
```

---

## 8. De IR a cadena de fórmula (`p`, `q`, `¬`, `∧`, `∨`, `→`)

| Concepto | Clase Java | Rol lógico |
|----------|------------|------------|
| Recorrido del árbol | `tautoteacher2.logicscript.ir.EmitidorFormula` | Visitador recursivo: convierte cada nodo en texto con símbolos lógicos. |
| Símbolos `p`, `q`, … | `tautoteacher2.logicscript.RegistroProposiciones` | **Por traducción**, asigna el mismo símbolo al mismo fragmento normalizado (orden de aparición estable con `LinkedHashMap`). |

**Por qué esta idea:** separar **significado** (IR) de **notación** (cadena esperada por `MotorLogico`). El registro evita que dos menciones del mismo predicado reciban letras distintas por azar.

Emisión (extracto):

```18:37:TautoTeacher2.0/src/main/java/tautoteacher2/logicscript/ir/EmitidorFormula.java
    private static String emitirInterno(LogicExpr expr, RegistroProposiciones registro, List<String> pasosDeAnalisis) {
        if (expr instanceof AtomExpr a) {
            return emitirAtomo(a, registro, pasosDeAnalisis);
        }
        if (expr instanceof NegExpr n) {
            return "¬" + conParentesisSiHaceFalta(n.interior(), registro, pasosDeAnalisis);
        }
        if (expr instanceof AndExpr y) {
            return "(" + emitirInterno(y.izquierda(), registro, pasosDeAnalisis)
                    + " ∧ " + emitirInterno(y.derecha(), registro, pasosDeAnalisis) + ")";
        }
        if (expr instanceof OrExpr o) {
            return "(" + emitirInterno(o.izquierda(), registro, pasosDeAnalisis)
                    + " ∨ " + emitirInterno(o.derecha(), registro, pasosDeAnalisis) + ")";
        }
        if (expr instanceof ImpExpr i) {
            return "(" + emitirInterno(i.antecedente(), registro, pasosDeAnalisis)
                    + " → " + emitirInterno(i.consecuente(), registro, pasosDeAnalisis) + ")";
        }
        throw new IllegalStateException("Tipo IR no contemplado: " + expr.getClass());
    }
```

---

## 9. Orquestación y API pública

| Concepto | Clase Java | Rol lógico |
|----------|------------|------------|
| Pipeline completo | `tautoteacher2.logicscript.LogicScriptEngine` | Carga **un** `ContenidoLgs`, construye `BaseConocimiento` y `SemanticMapper`, normaliza, tokeniza, parte por `, en caso de que` para componer con `AndExpr` entre bloques, emite fórmula. |
| Fachada estable | `tautoteacher2.logicscript.LogicScriptService` | Delgada: instancia el engine y expone `traducir` (útil para UI/CLI sin acoplarse al motor). |
| Resultado | `tautoteacher2.logicscript.LogicScriptResult` | DTO: éxito, fórmula, mensaje, pasos de análisis, mapa proposiciones. |
| Demo consola | `tautoteacher2.logicscript.LogicScriptCli` | Usa `LogicScriptService` y opcionalmente `MotorLogico`. |
| Regresión | `tautoteacher2.logicscript.LogicScriptRegressionHarness` | Tabla de entradas y fórmulas esperadas; código de salida distinto de 0 si falla. Ver `docs/LogicScript/PruebasRegresion.md`. |

**Por qué esta idea:** `LogicScriptEngine` concentra el **orden de fases** (como un *driver* de compilador); las demás clases permanecen pequeñas y testeables.

Construcción del bundle y delegación:

```21:28:TautoTeacher2.0/src/main/java/tautoteacher2/logicscript/LogicScriptEngine.java
public class LogicScriptEngine {
    private static final String RECURSO_LGS = "logicscript/core.lgs";

    private final NormalizadorTexto normalizador = new NormalizadorTexto();
    private final NaturalLexer lexer = new NaturalLexer();
    private final ContenidoLgs contenidoLgs = LgsCargador.cargarDesdeClasspath(RECURSO_LGS);
    private final BaseConocimiento baseConocimiento = new BaseConocimiento(contenidoLgs);
    private final SemanticMapper semanticMapper = new SemanticMapper(baseConocimiento, contenidoLgs.patronesSemanticos());
```

---

## 10. Resumen tabla única

| Pieza LogicScript / pipeline | Clase(s) Java |
|-----------------------------|----------------|
| `.lgs` en classpath | Recurso + `LgsCargador` |
| `lemma` | `LgsCargador` → `BaseConocimiento` |
| `pattern` | `LgsCargador` → `PatronSemanticoLgs`, `TipoSalidaIrPatron` |
| Bundle parseado | `ContenidoLgs` |
| Normalización | `NormalizadorTexto` |
| Tokens LN | `NaturalLexer`, `TipoTokenNatural`, `TokenNatural` |
| LN → IR | `SemanticMapper` |
| IR | `LogicExpr`, `AtomExpr`, `AndExpr`, `OrExpr`, `ImpExpr`, `NegExpr` |
| IR → cadena | `EmitidorFormula`, `RegistroProposiciones` |
| Orquestación | `LogicScriptEngine`, `LogicScriptService`, `LogicScriptResult` |
| Regresión manual | `LogicScriptRegressionHarness` |

---

## 11. Motor lógico (fuera de LogicScript estricto)

La evaluación de tautologías / tipo de fórmula sobre la **cadena** emitida corresponde a **`tautoteacher2.core.logica.MotorLogico`** (y clases asociadas en ese paquete). No interpreta `.lgs` ni tokens: solo recibe la fórmula ya simbolizada.

---

## Véase también

- `docs/LogicScript.md` — visión general y filosofía.
- `docs/LogicScript/Pattern.md` — índices `left`/`right` y analogías con compiladores.
- `docs/LogicScript/FormatoLGS.md`, `Lemma.md`, `SemanticMapper.md`, `BaseConocimiento.md`, `PruebasRegresion.md`.
