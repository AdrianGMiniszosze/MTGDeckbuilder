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
| D-011 | 2026-11-14 | Directorios en minúsculas, verificados en CI | Aceptada |
| D-012 | 2026-11-14 | `MISSING_COMMANDER` diferido hasta tener campo de comandante | Aceptada (diferida) |
| D-013 | 2026-11-14 | Los issues viven solo en el mazo, no en cada fila | Aceptada |
| D-014 | 2026-11-14 | La lista de mazos no calcula el diagnóstico | Aceptada |
| D-015 | 2026-11-15 | Los listados cargan los tableros con una consulta extra | Aceptada |
| D-016 | 2026-11-15 | JaCoCo mide cobertura y actúa como trinquete | Aceptada |
| D-017 | 2026-11-15 | Las consultas nativas parametrizan todos los filtros | Aceptada |
| D-018 | 2026-11-15 | El cargador identifica impresiones por `scryfall_id` y es idempotente | Aceptada |
| D-019 | 2026-11-16 | `card_tag` se reserva para tags semánticos | Aceptada |
| D-020 | 2026-11-16 | Los enums del contrato se convierten por valor, no por nombre | Aceptada |

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
- **Actualización (D-018):** El upsert por `scryfall_id` y la idempotencia de las tablas
  hijas ya están implementados en el script; sigue pendiente `is_obsolete`, que va con
  el importador Java.
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

### D-011 - Directorios en minúsculas, verificados en CI

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** Todo segmento de directorio de una ruta versionada va en minúsculas y
  coincide con el `package` declarado. CI lo comprueba y falla si encuentra una mayúscula.
- **Contexto:** El índice guardaba 120 rutas como `com/deckbuilder/Mtgdeckbuilder/`
  mientras el disco y los `package` usaban `mtgdeckbuilder`; otras 7 ya estaban en
  minúsculas. Con `core.ignorecase=true` el desajuste es invisible en Windows, pero un
  checkout en Linux crea dos directorios para el mismo paquete. Ya obligó a añadir
  `DeckStatus.java` con `git update-index --cacheinfo`.
- **Por qué:** Un árbol que se ve distinto según el sistema operativo no es un contrato
  fiable: duplica clases, esconde la mitad del código a los refactors y obliga a un truco
  manual por cada fichero nuevo. El truco recurrente cuesta más que el arreglo único.
- **Alternativas descartadas:** dejar el caso y convivir con él (empeora con cada fichero
  nuevo); renombrar el paquete a algo más corto (cambio interno sin beneficio funcional,
  sigue siendo posible después); confiar en `.gitattributes` (no controla el caso de las
  rutas).
- **Consecuencias:** Commit `c7876a2` con 120 renombrados y cero cambios de contenido.
  Paso nuevo en `ci.yml`. En Windows, un renombrado solo de caso se hace con nombre
  temporal o re-indexando desde disco.
- **Revisar si:** se adopta un mono-repo con otros lenguajes y la regla necesita
  excepciones.

### D-012 - `MISSING_COMMANDER` diferido hasta tener campo de comandante

- **Fecha:** 2026-11-14
- **Estado:** Aceptada (diferida)
- **Decisión:** No emitir `MISSING_COMMANDER` en `evaluate()` por ahora. El código queda
  reservado, pero la regla se implementa junto con un campo de comandante en el esquema
  y en el contrato.
- **Contexto:** El modelo aprobado incluye `formats.requires_commander`, pero ni
  `decks` ni `openapi.yml` tienen un campo de comandante. Un mazo Commander cumpliría
  `requires_commander` sin poder declararlo, así que la regla rechazaría todos los mazos
  del formato sin ofrecer una salida.
- **Por qué:** Una validación insatisfacible es peor que una ausente: bloquea publicar
  sin que el usuario pueda arreglarlo. Es preferible publicar sin la comprobación y
  añadirla cuando exista el dato.
- **Alternativas descartadas:** usar `companionCardId` como comandante (son conceptos
  distintos y el contrato tampoco lo expone); derivar el comandante de la primera carta
  legendaria (heurística, no un dato); bloquear Commander hasta la fase 7 (impide probar
  el resto del modelo).
- **Consecuencias:** `MISSING_COMMANDER` no aparece en los resultados actuales. Commander
  se evalúa con copias, legalidad y tamaño, pero no con la presencia de comandante.
  Registrado en `docs/DECK_VALIDATION_MODEL.md` §4 y §7.1.
- **Revisar si:** se añade `commander_card_id` (o equivalente) a `decks` y al contrato.

### D-013 - Los issues viven solo en el mazo, no en cada fila

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** `DeckIssue` se expone en `Deck.issues` y `CompleteDeck.issues`; no se
  añade `CardDeck.issues`.
- **Contexto:** El modelo inicial listaba `issues` también por fila de `card_deck`. Cada
  `DeckIssue` ya lleva `card_id`, `deck_content_id` y `section`, así que la fila no aporta
  información nueva.
- **Por qué:** Duplicar el mismo dato en cada fila agranda la respuesta y obliga a mapear
  y recorrer la lista dos veces. El cliente puede agrupar por `deck_content_id` con los
  issues del mazo.
- **Alternativas descartadas:** `CardDeck.issues` completo (duplicado); solo un booleano
  por fila (otro campo que mantener sincronizado).
- **Consecuencias:** El editor colorea filas cruzando `issues[].deck_content_id` con el
  `CardDeck.deck_id`/`card_id`. `DeckValidationMapper` es la única traducción de issues.
- **Revisar si:** el cliente necesita el diagnóstico por fila sin cargar el mazo completo.

### D-014 - La lista de mazos no calcula el diagnóstico

- **Fecha:** 2026-11-14
- **Estado:** Aceptada
- **Decisión:** Las respuestas de un solo mazo (`GET /decks/{id}`, `POST /decks`,
  `PUT /decks/{id}`, `publish`, `unpublish`) incluyen `validation_status`, `issue_count`
  e `issues`. `GET /decks` deja esos campos en `null`.
- **Contexto:** `evaluate()` consulta carta, formato, legalidad y copias. Calcularlo para
  cada elemento de una página de 20 añade decenas de consultas a una ruta de listado.
- **Por qué:** El listado sirve para navegar, no para editar. El coste por escritura es un
  `evaluate()` extra sobre un único mazo; el coste por página sería multiplicativo y
  innecesario.
- **Alternativas descartadas:** calcular en la lista (N+1); guardar el diagnóstico en la
  tabla (deja de ser calculado y se queda obsoleto); parámetro `include=validation`
  (complica el contrato antes de tener un cliente que lo pida).
- **Consecuencias:** El campo es nullable en el contrato; el cliente que quiera el
  diagnóstico de un mazo llama a `GET /decks/{id}/validation` o a `GET /decks/{id}`.
- **Revisar si:** aparece una pantalla que necesite diagnósticos de una página entera.

### D-015 - Los listados cargan los tableros con una consulta extra

- **Fecha:** 2026-11-15
- **Estado:** Aceptada
- **Decisión:** `getAll`, `findByUserId` y `findByFormat` piden las filas de
  `card_deck` de toda la página con `findByDeckIdIn` y las reparten por `deck_id`. Cada
  mazo del listado lleva sus tableros (`main_board`, `side_board`, `maybe_board`).
- **Contexto:** El contrato devuelve `CompleteDeck` en `GET /decks`, pero
  `DeckEntity.cards` es `@OneToMany(mappedBy = "deck")` y el servicio inserta la relación
  solo con `deck_id`, sin tocar la colección JPA. Resultado: los listados devolvían
  tableros vacíos aunque el detalle ya los cargaba.
- **Por qué:** Un `CompleteDeck` sin cartas es una mentira del contrato en la ruta que
  usa cualquier listado. Cargarlo con dos consultas por página (cabeceras + filas) es
  barato y no cae en N+1.
- **Alternativas descartadas:** dejar los tableros vacíos y documentarlo (el cliente no
  podría pintar la lista); mapear la relación JPA en el servicio (exigiría mantener la
  colección sincronizada o reescribir la inserción); `JOIN FETCH` por mazo (N+1).
- **Consecuencias:** Una consulta extra por página, también en la lista vacía → se evita
  con un corto retorno temprano. `DeckServiceImplTest` deja de usar `toModelList`.
- **Revisar si:** la lista crece a cientos de filas por página o aparece un parámetro
  para pedir solo cabeceras.

### D-016 - JaCoCo mide cobertura y actúa como trinquete

- **Fecha:** 2026-11-15
- **Estado:** Aceptada
- **Decisión:** Añadir `jacoco-maven-plugin` (0.8.13) con agente en cada `test`, informe
  HTML en `target/site/jacoco/` y `check` en `verify`. CI ejecuta
  `jacoco:report jacoco:check` tras las tres tandas de tests y publica el informe como
  artefacto. Los umbrales son un trinquete de bundle (no por clase): ahora 99 % de
  líneas y 94 % de ramas, y solo suben.
- **Contexto:** No había ninguna medida de cobertura, así que "¿esto está probado?" se
  respondía leyendo el código. El primer informe combinado (unitarias + integración +
  smoke) dio 68,6 % de líneas y 42,3 % de ramas; las mayores lagunas eran
  `CardRepositoryImpl`, `GlobalExceptionHandler` y `CompanionRules`. Tras cubrirlas y
  añadir tests de `CardColor`, `CardTagServiceImpl` y el SQL real de búsqueda, el
  informe combinado subió a 91,6 % / 71,4 %, y el suelo se movió con él a 89 / 69. Al
  cubrir después la validación de mazos, los criterios de búsqueda, las claves
  compuestas, la paginación y los mapeadores, quedó en 99,9 % / 94,9 %, con el suelo en
  99 / 94. Una pasada solo de unitarias (79,4 / 56,9 al principio) no alcanza el suelo:
  el gate es de suite completa.
- **Por qué:** El suelo por debajo de la cifra actual impide retroceder sin obligar a
  arreglar de golpe toda la deuda. Un umbral por clase habría fallado el primer día en
  clases sin test y se habría desactivado.
- **Alternativas descartadas:** umbral por clase o por paquete desde el inicio (demasiado
  ruido); exigir 80 % antes de cubrir nada (bloquearía el trabajo en curso); sin `check`
  y solo informe (nadie mira un informe que no rompe nada).
- **Consecuencias:** Toda tanda de tests escribe `target/jacoco.exec`, así que el informe
  es acumulativo; hay que regenerarlo tras correr la suite completa y hacer `clean` para
  medir una tanda aislada. Una pasada solo de unitarias no pasa el gate por diseño. El
  `argLine` de surefire usa `@{argLine}` para no pisar el agente de Mockito. Se excluyen
  las clases generadas de OpenAPI, los `*Impl` de MapStruct y los métodos anotados con
  `@lombok.Generated`.
- **Revisar si:** se busca el último punto de cobertura (`main()` de
  `MtgdeckbuilderApplication` y las ramas sueltas de `CardServiceImpl` y
  `CompanionRules`); si no, el suelo ya no tiene margen para bajar y cualquier código
  nuevo sin test romperá el build, que es justo el objetivo.

### D-017 - Las consultas nativas parametrizan todos los filtros

- **Fecha:** 2026-11-15
- **Estado:** Aceptada
- **Decisión:** En `CardRepositoryImpl.findRandomCards`, las únicas partes que se
  construyen como texto son las cláusulas fijas; todo valor que venga de la petición
  (tipo, rareza, formato, límite) se enlaza con `setParameter`. Se eliminan
  `buildRandomQuerySql`, `findRandomCardsWithFormatNative` y el `CriteriaQuery` que no
  se llegaba a ejecutar cuando había `formatId`.
- **Contexto:** La versión anterior concatenaba `type`, `rarity` y `formatId` dentro del
  SQL (`LOWER('...')`, `format_id = 3`) y solo el límite era parámetro. El filtro de
  formato además usaba `LEFT JOIN`, así que un `formatId` presente devolvía también
  cartas sin fila en `card_legality`. Nadie lo explotaba, pero era inyección SQL en una
  ruta pública y un resultado incorrecto.
- **Por qué:** Un valor de request nunca debe convertirse en sintaxis. El `JOIN` interno
  además hace que "cartas de este formato" signifique exactamente eso.
- **Alternativas descartadas:** escapar comillas a mano (frágil y no cubre todos los
  casos); mantener el `CriteriaQuery` muerto (código que aparenta cubrir un caso y no lo
  hace); dejar el `LEFT JOIN` y filtrar en Java (más código y menos preciso).
- **Consecuencias:** La consulta tiene un único camino, con parámetros posicionales en
  orden. `CardRepositorySearchIntegrationTest` ejecuta el SQL real contra PostgreSQL,
  cubre cada combinación de filtros y comprueba que un payload tipo `' OR 1=1 --` no
  devuelve filas ni altera la tabla.
- **Revisar si:** se añaden más consultas nativas: todas deben seguir la misma regla, y
  cualquier `+ valor +` dentro de un `String` que acabe en `createNativeQuery` es un bug.

### D-018 - El cargador identifica impresiones por `scryfall_id` y es idempotente

- **Fecha:** 2026-11-15
- **Estado:** Aceptada
- **Decisión:** `import_cards.py` usa el campo `id` de Scryfall como `cards.scryfall_id`
  y hace upsert por esa columna. Las caras sintéticas de una carta multiforma, que no
  tienen impresión propia, se localizan por `(card_name, set_id, collector_number)`.
  Todas las tablas hijas usan `ON CONFLICT` y cada carta/cara se escribe bajo un
  savepoint propio. El fichero se lee en streaming.
- **Contexto:** El script leía `scryfall_id` de un campo que Scryfall no envía (el UUID
  está en `id`), así que la columna quedaba `NULL`. Además insertaba sin conflicto,
  deduplicaba por `card_name` (impidiendo varias impresiones), escribía en la tabla
  inexistente `card_set`, guardaba el UUID de set de Scryfall en `cards.set_id` y un
  error de una carta abortaba el lote entero. Los `ON CONFLICT` de los hijos faltaban,
  así que la segunda pasada rompía. `analyze_skips_in_json` y `diagnose_table_structure`
  cargaban los 632 MB en memoria.
- **Por qué:** `scryfall_id` es la única identidad de impresión estable: dos
  impresiones distintas del mismo `oracle_id` son filas distintas y un reimport debe
  refrescarlas, no duplicarlas ni saltarlas por nombre. Identificar las caras por la
  tupla es el único trío estable que queda cuando no hay UUID propio. Los savepoints
  convierten un dato corrupto en una fila fallida contada, no en una importación
  perdida.
- **Alternativas descartadas:**
  - Seguir deduplicando por `card_name`: pierde reprints, que son la mayoría del bulk.
  - `DELETE` + `INSERT` por lote: `card_deck.card_id` es `ON DELETE CASCADE` y vaciaría
    mazos.
  - Insertar las caras sintéticas con un `scryfall_id` inventado: rompe la unicidad y
    confunde la identidad de impresión.
  - Cargar el JSONL entero con `json.load`: necesita varios GB de RAM para un fichero
    de 632 MB.
- **Consecuencias:** Reejecutar el script deja el mismo número de filas y refresca
  textos, legalidades y keywords (y precios cuando se importen). Verificado sobre una
  muestra de 177 líneas: primera pasada 197 insertadas / 5 actualizadas / 5 saltadas,
  segunda pasada 0 insertadas / 202 actualizadas, contadores hijos idénticos. Un
  `scryfall_id` inválido descarta solo su fila. El diseño de caras sigue siendo
  provisional: el rediseño con `card_faces` va con el importador Java.
- **Revisar si:** se escribe el importador Java o se añade `card_faces`; entonces la
  tupla `(card_name, set_id, collector_number)` deja de ser necesaria.
- **Detalle:** `docs/DATA_IMPORT.md`; relacionada con D-008 y D-004.

### D-019 - `card_tag` se reserva para tags semánticos

- **Fecha:** 2026-11-16
- **Estado:** Aceptada
- **Decisión:** El cargador deja de escribir `tags` y `card_tag` para las keywords
  de Scryfall; solo puebla `card_keywords`. `card_tag` queda para tags semánticos
  con un `source` del contrato. Se eliminan las filas existentes con
  `source = 'scryfall_import'` y los tags que queden huérfanos.
- **Contexto:** El script creaba un tag por keyword y lo enlazaba en `card_tag` con
  `weight = random()` y `source = 'scryfall_import'`, valor que no existe en el
  enum del contrato. Esas 68.156 filas rompían la lectura de tags y dejaban la
  tabla que usarán recomendación y embeddings llena de ruido.
- **Por qué:** Una keyword oficial y un tag semántico son conceptos distintos: la
  primera ya vive en `card_keywords` y el peso aleatorio no aporta información.
- **Alternativas descartadas:**
  - Añadir `scryfall_import` al enum: conserva el ruido y el peso inventado.
  - Mapear esos orígenes a `heuristic`: miente sobre de dónde salió el tag.
- **Consecuencias:** `tags` queda vacía hasta que exista etiquetado semántico y el
  resumen del import ya no muestra "Tags created". Si aparece un `source`
  desconocido en la BD, el mapper degrada a `heuristic` y registra un warning, para
  que un dato inesperado no tumbe la lectura de la carta.
- **Revisar si:** se implementa el etiquetado semántico, el agente o la
  recomendación, que son los primeros consumidores reales de `card_tag`.

### D-020 - Los enums del contrato se convierten por valor, no por nombre

- **Fecha:** 2026-11-16
- **Estado:** Aceptada
- **Decisión:** `CardMapper` y `CardTagMapper` convierten los enums del contrato
  con métodos `default` que comparan `getValue()` en minúsculas; nunca con
  `Enum.valueOf` sobre texto de la BD. El contrato de rareza acepta los seis
  valores de Scryfall, incluido `bonus`. Una rareza desconocida degrada a
  `special` y un origen desconocido a `heuristic`, en ambos casos con warning.
- **Contexto:** La prueba HTTP con datos reales devolvía `500` con
  `No enum constant ...CardDTO$Rarity.common` en `/cards`, `/cards/search`,
  `/cards/random` y `/cards/{id}`. MapStruct generaba
  `Enum.valueOf(Rarity.class, card.getRarity())`, y la BD guarda `common` en
  minúsculas mientras la constante generada es `COMMON`; en sentido inverso habría
  guardado `COMMON`. `CardTagDTO.Source` tenía el mismo problema. Los tests con
  mocks nunca ejecutaban la conversión.
- **Por qué:** Scryfall publica las rarezas en minúsculas. `bonus` es una etiqueta
  propia de Scryfall (las 9 cartas del slot especial de Vintage Masters, los Power
  Nine), distinta de `special` (timeshifted, masterpieces): remapearla perdería
  información y la rareza no interviene en la validación de mazos.
- **Alternativas descartadas:**
  - Remapear `bonus` a `special`: mezcla dos conceptos distintos.
  - Normalizar la BD a mayúsculas: rompe la fidelidad con Scryfall y los filtros.
  - Lanzar excepción ante un valor desconocido: un dato nuevo de Scryfall tumbaría
    el listado entero en vez de una sola etiqueta.
- **Consecuencias:** Tres enums de rareza del contrato pasan a seis valores; tests
  de mapper en ambas direcciones (incluye `bonus` y valores desconocidos); el
  smoke con datos reales deja de devolver `500`.
- **Revisar si:** Scryfall añade otra rareza (el fallback la degrada a `special`
  con warning) o si el contrato pasa a exigir fidelidad estricta de origen.
- **Detalle:** `docs/DATA_IMPORT.md`; relacionada con D-018 y D-019.
