# Base de Conocimiento (LogicScript)

Este documento describe la capa `BaseConocimiento` usada por LogicScript para
normalizar expresiones de lenguaje natural antes de construir la IR.

## Objetivo

- Evitar que variantes morfologicas creen proposiciones distintas.
- Consolidar frases equivalentes en una forma canonica.
- Mantener reglas lexicales en un punto central y extensible.

## Clase actual

- **Clase:** `tautoteacher2.nlp.lexicon.BaseConocimiento`
- **Metodo clave:** `canonicalizarFragmento(String fragmento)`
- **Lemas externos:** `classpath:logicscript/core.lgs` cargado por `LgsCargador`. Si el recurso falta, se usan lemas embebidos como respaldo.
- **Referencia conceptual de lemas:** `docs/LogicScript/Lemma.md`.

## Que hace hoy

1. Limpia articulos al inicio (`el`, `la`, `un`, etc.).
2. Normaliza espacios.
3. Aplica diccionario de lemas palabra por palabra.

Ejemplos:

- `llueve` -> `llover`
- `llueva` -> `llover`
- `llevo paraguas` -> `llevar paraguas`

## Alcance actual (MVP)

- Diccionario principal en **`core.lgs`**; respaldo mínimo en codigo si no hay recurso en el classpath.
- Sin desambiguacion semantica avanzada.
- Sin manejo de tiempos compuestos ni negaciones complejas.

## Evolucion prevista

1. Mover reglas a archivo declarativo (`.lgs` o similar).
2. Agregar sinonimos de conectores y expresiones multi-palabra.
3. Versionar la base por dominio (logica academica, ejemplos escolares, etc.).

