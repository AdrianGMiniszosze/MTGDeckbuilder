# Registro de decisiones

- **Estado:** vivo, append-only
- **Alcance:** decisiones de producto, alcance y prioridad, con su motivo
- **Relacionado:** `docs/ARCHITECTURE_DECISIONS.md` (ADR), `docs/DECK_VALIDATION_MODEL.md`

## 1. Para qué existe

Los ADR explican **cómo está construido** el sistema. Este registro explica **qué se
decidió, cuándo y por qué**, incluido lo que se decidió *no* hacer. Sirve para responder
dentro de seis meses a la pregunta "¿esto por qué está así?" sin depender de la memoria
ni del historial de chat.

## 2. Reglas

1. **Append-only.** No se reescribe una entrada antigua. Si algo cambia, se añade una
   nueva que la reemplaza y se marca la anterior como `Reemplazada por D-XXX`.
2. **Una decisión por entrada.** Si una conversación produce cinco decisiones, son
   cinco entradas.
3. **Siempre con motivo.** "Porque lo dijo el usuario" no es un motivo; se anota la
   razón de producto o técnica.
4. **Alternativas descartadas.** Es la parte que más se agradece después.
5. **Señal de revisión.** Cuándo merece la pena volver a mirarla.
6. **Si es arquitectónica**, la entrada apunta al ADR que la desarrolla.

## 3. Índice

| ID | Fecha | Decisión | Estado |
|---|---|---|---|
| D-001 | 2026-11-14 | Mazos en `draft` permisivo y `published` estricto | Aceptada |
| D-002 | 2026-11-14 | Política de mazos en Java; la BD solo guarda la forma | Aceptada |
| D-003 | 2026-11-14 | Formato obligatorio y fila `Casual` en lugar de `NULL` | Aceptada |
| D-004 | 2026-11-14 | `copies_limit` sustituye a `unlimited_copies` | Aceptada |
| D-005 | 2026-11-14 | Recuento de copias por `oracle_id` | Aceptada |
| D-006 | 2026-11-14 | Publicación fallida devuelve `409 Conflict` | Aceptada |
| D-007 | 2026-11-14 | Publicar con `LEGALITY_UNKNOWN` está permitido | Aceptada |
| D-008 | 2026-11-14 | Importación: script manual ahora, CLI Java después | Aceptada |
| D-009 | 2026-11-14 | Colecciones y wishlist no viven en `decks` | Aceptada (sin fecha de inicio) |
| D-010 | 2026-11-14 | Este registro de decisiones existe | Aceptada |

## 4. Entradas

### D-001 - Mazos en `draft` permisivo y `published` estricto

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** Añadir `decks.status` (`draft` | `published`). Guardar un mazo nunca
  aplica reglas de formato; la legalidad se calcula como diagnóstico y solo bloquea al
  publicar. Editar un mazo publicado lo devuelve a `draft`.
- **Contexto:** El veto al guardar impedía tener mazos a medio construir y devolvía un
  único mensaje sin decir qué carta era el problema. La validación Java existía pero no
  estaba cableada.
- **Por qué:** El usuario quiere poder guardar cualquier cosa y ver avisos por carta.
  Un editor que rechaza el guardado no permite trabajar.
- **Alternativas descartadas:**
  - Bloquear al guardar: peor experiencia y sin información accionable.
  - No validar nunca: sin garantía en servidor de que lo publicado sea jugable.
- **Consecuencias:** `V3` añade `status` y `published_at`; el contrato publica
  `validation_status` e `issues`; hay que reescribir el smoke de Flyway.
- **Revisar si:** aparece un caso de uso de mazos colaborativos donde "publicado" y
  "editable" deban convivir.

### D-002 - Política de mazos en Java; la BD solo guarda la forma

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** Eliminar el trigger `check_card_quantity` y su función. La BD conserva
  FKs, `UNIQUE`, `CHECK` de cantidad y sección; las reglas de formato pasan a Java
  (`DeckValidationService`).
- **Contexto:** Las reglas estaban duplicadas en PL/pgSQL y Java, y el SQL no podía
  cubrir companions (Umori, Yorion, Zirda). El trigger tampoco cubría `DELETE`, así que
  `decks.updated_at` se quedaba desactualizado al quitar la última copia.
- **Por qué:** Una sola definición de cada regla. La API es el único escritor de
  `decks` y `card_deck`, así que la red de seguridad SQL protege un escenario que no
  existe y a cambio garantiza deuda.
- **Alternativas descartadas:**
  - Trigger solo para `published`: mantiene dos implementaciones y sigue sin cubrir
    companions.
  - Trigger adicional sobre `UPDATE OF status` en `decks`: duplica el validador entero
    en SQL.
- **Consecuencias:** Una escritura manual por `psql` podría dejar un mazo publicado
  ilegal. `decks.updated_at` pasa a gestionarse en Java, incluido el caso `DELETE`.
- **Revisar si:** aparece un segundo escritor de mazos (importador, panel de
  administración, migración de datos). Entonces se replantea la red de seguridad.
- **ADR:** ADR-007 (reemplaza ADR-002).

### D-003 - Formato obligatorio y fila `Casual` en lugar de `NULL`

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** `format` pasa a `required` en `openapi.yml`. Los mazos "for fun" usan
  una fila `Casual` en `formats` (`min_deck_size = 0`, topes `NULL`,
  `tracks_legality = FALSE`).
- **Contexto:** `decks.format_id` es `NOT NULL` desde `V1`, pero el contrato lo daba por
  opcional, así que omitirlo producía un `409` de integridad en lugar de un `400` de
  validación. `NULL` habría exigido relajar el esquema.
- **Por qué:** `NULL` significa "no lo he rellenado"; `Casual` significa "no quiero
  reglas". Son cosas distintas y en la interfaz `Casual` es una opción visible.
- **Alternativas descartadas:** `format_id` nullable + ramificar la validación;
  enum de formato en el contrato (era un id numérico de `formats`).
- **Consecuencias:** Un formato más en el seed; `formats` gana `tracks_legality`.
- **Revisar si:** se quiere separar "sin reglas" de "reglas caseras configurables".

### D-004 - `copies_limit` sustituye a `unlimited_copies`

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** `cards.unlimited_copies BOOLEAN` pasa a `cards.copies_limit INTEGER`
  (`NULL` = manda el formato, `0` = sin límite, `N` = tope exacto).
- **Contexto:** El booleano no expresaba Seven Dwarves (7) ni Nazgûl (9), y nadie lo
  rellenaba: no hay importador que lo dedujera.
- **Por qué:** Un entero cubre los tres casos con una sola columna y el texto de la
  carta gana al formato, que es como funciona el juego.
- **Alternativas descartadas:** mantener el booleano y añadir una columna aparte solo
  para los topes numéricos (dos fuentes de verdad).
- **Consecuencias:** Cambio breaking en el contrato de `Card` (sin consumidores);
  `V3` recrea la columna; el importador la deriva del `oracle_text`.
- **Revisar si:** aparecen cartas con reglas que no encajen en "sin límite" o "tope
  exacto" (por ejemplo, límites que dependan de otra carta en el mazo).

### D-005 - Recuento de copias por `oracle_id`

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** El límite de copias se calcula agrupando por `oracle_id`, no por
  `card_id` ni por `card_name`.
- **Contexto:** `card_id` es la impresión y `card_deck` permite una fila por impresión,
  así que 4 copias de dos impresiones distintas son 8 copias de la misma carta.
- **Por qué:** La regla del juego es por nombre de carta. `oracle_id` es estable entre
  reimpresiones y no depende del idioma, mientras que `card_name` sí puede variar.
- **Alternativas descartadas:** por `card_id` (deja pasar el caso de varias
  impresiones); por `card_name` (frágil ante idiomas).
- **Consecuencias:** El validador agrupa por `oracle_id`; el importador debe guardarlo.
- **Revisar si:** se importan cartas en varios idiomas con nombres traducidos.

### D-006 - Publicación fallida devuelve `409 Conflict`

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** `POST /decks/{id}/publish` responde `409 Conflict` con código
  `DECK_NOT_LEGAL` y la lista de `issues`.
- **Contexto:** Había que elegir entre `409` y `422`. El mazo existe y la petición está
  bien formada; lo que falla es una regla de negocio sobre el estado del recurso.
- **Por qué:** `409` describe conflicto con el estado actual, que es exactamente el caso.
- **Alternativas descartadas:** `422` (más propio de validación de forma).
- **Consecuencias:** Nuevo handler en `GlobalExceptionHandler`; el cuerpo incluye
  `issues` para que la interfaz no parsee mensajes.
- **Revisar si:** se adopta un estándar de errores distinto para toda la API.

### D-007 - Publicar con `LEGALITY_UNKNOWN` está permitido

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** Los `issues` de severidad `warning` no bloquean la publicación. Solo los
  `error` lo hacen.
- **Contexto:** Tras un import parcial no hay filas en `card_legality`, así que todos los
  mazos saldrían ilegales por falta de datos, no por composición.
- **Por qué:** La ausencia de datos no es un defecto del mazo.
- **Alternativas descartadas:** tratar `unknown` como error (bloquea todo hasta tener
  legalidades completas).
- **Consecuencias:** `LEGALITY_UNKNOWN` se muestra como aviso; `tracks_legality = FALSE`
  no emite ni aviso.
- **Revisar si:** el import de legalidades pasa a ser fiable y se quiere endurecer.

### D-008 - Importación: script manual ahora, CLI Java después

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** Mantener `import_cards.py` como cargador de pruebas, parcheado para el
  esquema post-`V3`. El importador definitivo será una CLI Java con upsert idempotente.
  La primera carga y las posteriores serán manuales hasta que haya usuarios reales.
- **Contexto:** El script no guardaba `scryfall_id` ni `oracle_id`, no importaba
  legalidades y escribía `unlimited_copies`, que desaparece en `V3`.
- **Por qué:** Sin datos reales no se puede probar la validación. Automatizar antes de
  tener idempotencia y marca de obsoletas convierte un dato viejo en datos corruptos.
- **Alternativas descartadas:** endpoint HTTP de importación (600 MB por HTTP); Spring
  Batch (dependencia innecesaria para un job); importar al arrancar la aplicación
  (alarga el boot y corre en cada despliegue).
- **Consecuencias:** El script no es idempotente: hay que recrear la BD. Antes de
  automatizar hacen falta upsert por `scryfall_id`, `is_obsolete` y no borrar nunca.
- **Revisar si:** aparece la necesidad de refrescar precios o sets con frecuencia.
- **Detalle:** `docs/DATA_IMPORT.md`.

### D-009 - Colecciones y wishlist no viven en `decks`

- **Fecha:** 2026-11-14
- **Estado:** Aceptada, sin fecha de inicio
- **Decisión:** Una colección de cartas será una tabla propia (`user_card`), no un mazo.
  Wishlist será un flag o tabla gemela. No se implementa ahora.
- **Contexto:** Un mazo tiene formato, reglas y se juega; una colección es inventario
  sin formato ni legalidad.
- **Por qué:** Meterla en `decks` obligaría a un `format_id` falso y a excluirla de toda
  la validación.
- **Alternativas descartadas:** reutilizar `decks` con un formato "Collection"
  (contamina el modelo de mazos).
- **Consecuencias:** Ninguna hasta que se aborde; queda registrado para no confundirlo
  con "mazos sin formato".
- **Revisar si:** se prioriza inventario, wishlist o importación de colecciones.

### D-010 - Este registro de decisiones existe

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** Crear `docs/DECISION_LOG.md` como registro cronológico de decisiones y
  sus motivos, separado de `docs/ARCHITECTURE_DECISIONS.md`.
- **Contexto:** El usuario quiere poder consultar por qué se decidió algo. Los ADR solo
  cubren decisiones de arquitectura y son más pesados de escribir.
- **Por qué:** Dos ritmos distintos: el ADR justifica un diseño, el registro deja
  constancia rápida de producto y alcance. Forzar todo a ADR haría que no se escribiera
  nada.
- **Alternativas descartadas:** un único archivo mezclando ambos (se vuelve ilegible);
  solo ADR (demasiada ceremonia para decisiones pequeñas).
- **Consecuencias:** Cada decisión relevante se anota al cerrarse, con alternativas y
  señal de revisión. Las arquitectónicas apuntan a su ADR.
- **Revisar si:** el archivo crece hasta ser incómodo; entonces se parte por año o por
  área.
