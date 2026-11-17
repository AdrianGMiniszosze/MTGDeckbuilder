# Plan del importador Java (fase 6)

- **Estado:** aprobada el 2026-11-17; D-023–D-031 aceptadas en `docs/DECISION_LOG.md`
- **Alcance:** sustituir `import_cards.py` por una CLI Java idempotente que mantenga
  `cards`, `card_legality`, `card_keywords`, `sets`, `card_faces`, `card_rulings` y precios
- **Relacionado:** `docs/DATA_IMPORT.md` (estado actual), `docs/DECK_VALIDATION_MODEL.md` §10,
  `docs/DECISION_LOG.md` (D-018, D-019 y las decisiones D-023–D-031)
- **No es:** la fase de embeddings/recomendación ni un job programado

## 1. Objetivo

Que los datos reales de Scryfall se carguen con el mismo lenguaje, configuración y tests
que el resto de la aplicación, sin `psycopg2` ni un Python concreto en el servidor, y sin
riesgo de vaciar mazos.

Al terminar la fase 6:

- `import_cards.py` deja de ser necesario y se retira del repositorio.
- La importación es una CLI que termina con código de salida y un resumen auditable.
- Los datos que hoy quedan `NULL` o vacíos (precios, rulings, `card_faces`) se pueblan.
- Las cartas que desaparecen de Scryfall se marcan, no se borran.
- El esquema sigue siendo Flyway y el `FlywayMigrationSmokeIntegrationTest` cubre lo nuevo.

## 2. Entradas y salidas

| Entrada | Obligatoria | Uso |
|---|---|---|
| Bulk `default-cards-*.jsonl` (una impresión por línea) | Sí | Cartas, caras, legalidades, keywords, precios |
| Bulk `rulings-*.jsonl` | No | `card_rulings` por `oracle_id` |
| Configuración JDBC (`SPRING_DATASOURCE_*`) | Sí | Misma que la aplicación |

| Salida | Detalle |
|---|---|
| Filas actualizadas | Inserta o refresca por `scryfall_id`; ids hijos reemplazados |
| `cards.is_obsolete` | Marca las impresiones no vistas en esta pasada |
| Resumen | Insertadas / actualizadas / obsoletas / fallidas + duración |
| Código de salida | `0` correcto, `1` con filas fallidas, `2` configuración/IO/bloqueo |

Tamaño esperado: cientos de miles de líneas y ~1 GB. Nada se carga entero en memoria.

## 3. Arquitectura

Se añade un adaptador de entrada no HTTP, `importer`, junto a `contract`. Las capas
existentes no cambian de responsabilidad.

| Paquete | Clase | Responsabilidad |
|---|---|---|
| `com.deckbuilder.mtgdeckbuilder.importer` | `ScryfallImportRunner` | `@Profile("import")`, `ApplicationRunner` + `ExitCodeGenerator`; lee flags, arranca, resume y sale |
| `com.deckbuilder.mtgdeckbuilder.importer` | `ImportOptions` | `--import.file`, `--import.rulings`, `--import.batch-size`, `--import.limit`; valida rutas |
| `…application.importing` | `CardImportService` | Orquesta lectura → mapeo → escritura → obsoletas; devuelve `ImportSummary` |
| `…application.importing` | `CopyLimitRules`, `LegalityRules`, `CardShapeRules` | Reglas puras, sin BD, testeables con JUnit |
| `…application.importing` | `ScryfallCardMapper` | `JsonNode` → `ImportCard` (record), incluido `copies_limit`, caras y precios |
| `…infrastructure.importer` | `ScryfallBulkReader` | `Files.lines` UTF-8 en streaming, tolerancia a BOM, una línea → `JsonNode`; el `Stream` debe cerrarse |
| `…infrastructure.importer` | `CardImportDao` | `JdbcTemplate`: upserts, reemplazo de hijos, caras, precios, obsoletas |
| `…infrastructure.importer` | `ImportLock` | `pg_try_advisory_lock` por sesión |
| `src/main/resources/application-import.properties` | — | `spring.main.web-application-type=none`, banner off, logging |

El perfil `import` **no** arranca Tomcat ni expone endpoints. Flyway se ejecuta al
arrancar, así que la CLI siempre ve la BD migrada.

```bash
java -jar mtgdeckbuilder.jar \
  --spring.profiles.active=import \
  --import.file=/data/default-cards-20261115.jsonl \
  --import.rulings=/data/rulings-20261115.jsonl
```

En Docker, el fichero se monta en el contenedor; no se copia a la imagen.

## 4. Flujo de una importación

1. Validar flags y rutas; abortar con `2` si falta el fichero. Si `--import.limit`
   está presente, es una carga parcial de desarrollo: se leen solo esas líneas y **no**
   se ejecuta el barrido de obsoletas (si no, se marcaría todo el catálogo como retirado).
2. Tomar `pg_try_advisory_lock(<clave fija>)`; si ya hay otra importación, `2`.
3. Generar un `run_id` (UUID) y limpiar restos de ejecuciones anteriores.
4. Leer el JSONL línea a línea:
   - descartar `token`, `emblem` y `art_series` **antes** de mirar `card_faces`;
   - mapear a `ImportCard` (record inmutable) y acumular en un lote de 500;
   - escribir el lote en una transacción.
5. Por carta, dentro del lote:
   - `INSERT … ON CONFLICT (scryfall_id) DO UPDATE … RETURNING id, (xmax = 0) AS inserted`;
   - reemplazar hijos (`card_colors`, `card_color_identity`, `card_types`,
     `card_supertypes`, `card_subtypes`, `card_keywords`);
   - reemplazar legalidades de los formatos sembrados;
   - reemplazar `card_faces` si `card_faces` viene en el JSON;
   - actualizar precios y `price_observed_at`;
   - `INSERT INTO import_seen (run_id, scryfall_id) … ON CONFLICT DO NOTHING`.
6. Si un lote falla, reintentar sus filas una a una para aislar la mala; contarla como
   fallida y seguir. Sin `savepoint` por carta: el lote ya es atómico.
7. Al terminar el fichero: `import_seen` de este `run_id` es la lista de vivos. Marcar
   obsoletas (con `updated_at = now()`) y desmarcar las que han vuelto. Se omite cuando la
   carga es parcial (`--import.limit`), con un `WARN` que lo deja claro en el resumen. Si la
   ejecución no vio ninguna carta (`seen = 0`, por fichero vacío o truncado), **no** se barre:
   se avisa y se sale con `2`.
8. Si hay fichero de rulings, segunda pasada con el mismo patrón (lote + upsert).
9. Borrar las filas de `import_seen` de este `run_id`, liberar el lock, imprimir resumen
   y salir con `0`/`1`.

## 5. Esquema

### 5.1 Migración nueva

`V5__import_support.sql`:

```sql
ALTER TABLE cards ADD COLUMN is_obsolete BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_cards_obsolete ON cards (is_obsolete) WHERE is_obsolete;

-- Set de scryfall_id vistos en la ejecución en curso. UNLOGGED: si el proceso
-- muere, Postgres la vacía y la importación se repite; no es dato de usuario.
CREATE UNLOGGED TABLE import_seen (
    run_id      UUID NOT NULL,
    scryfall_id UUID NOT NULL,
    PRIMARY KEY (run_id, scryfall_id)
);
```

- Dos preocupaciones en una migración porque ambas son soporte del importador y hoy no
  hay datos desplegados que migrar. Si se prefiere una por cambio, se separan en `V5` y
  `V6` sin más coste.
- La tabla es real (no `TEMP`) a propósito: el importador escribe por lotes con
  conexiones del pool y una tabla temporal desaparecería entre lotes. `run_id` aísla
  ejecuciones y el `PRIMARY KEY` hace de índice para el anti-join.
- `card_faces`, `card_rulings`, precios y `copies_limit` **ya existen** desde `V1`/`V3`;
  no hace falta migrar nada más.
- El `FlywayMigrationSmokeIntegrationTest` pasa a aplicar `V1`–`V5` y comprueba la
  columna, su `DEFAULT`, el índice y la tabla `import_seen`.

### 5.2 Caras sintéticas

Hoy las caras se guardan como filas de `cards` con `scryfall_id IS NULL`. El importador
Java escribe `card_faces` y deja de crearlas.

- No se borran automáticamente: `card_deck.card_id` es `ON DELETE CASCADE`.
- La BD local se recrea antes de la primera pasada completa (no hay nada desplegado).
- El importador registra un `WARN` si detecta filas sintéticas, para que nadie las
  confunda con datos buenos.
- Una limpieza automática queda fuera de alcance (D-028).

## 6. Reglas de dominio

Se portan del script, pero con tests y sin `print()`:

| Regla | Clase | Comportamiento |
|---|---|---|
| `copies_limit` | `CopyLimitRules` | `any number` → `0`; `up to N` → `N`; resto `NULL`; solo texto de construcción |
| Tierra básica | `CardShapeRules` | supertipo `Basic` + tipo `Land` → `is_basic_land` |
| Legalidades | `LegalityRules` | 6 claves de Scryfall → `format_id`; valores `legal`/`not_legal`/`restricted`/`banned`; claves sin formato se ignoran |
| Forma de la carta | `CardShapeRules` | `card_type`, `card_supertype`, `cmc`, `type_line`, `image_url` con los mismos fallbacks de hoy |
| Colores / keywords | `ScryfallCardMapper` | Desde los arrays de Scryfall; no se parsea el texto |
| Caras | `ScryfallCardMapper` | `face_index` por orden; columnas normalizadas + `face_data` crudo; el texto de las caras se concatena en `cards.card_text` |
| Precios | `ScryfallCardMapper` | `usd*`, `eur*`, `tix` si vienen; `price_observed_at = now()` solo si hay algún precio |
| Sets | `CardImportDao` | Busca `sets` por nombre, rellena `code`/`released_at`/`set_type` si faltan; el UUID de set de Scryfall no se usa |
| Marcas | `ScryfallCardMapper` | `promo`, `variation`, `foil`, `game_changer`, `language` |

Nota: `card_legality` solo se escribe para formatos sembrados; lo demás es
`LEGALITY_UNKNOWN` (aviso, no error).

## 7. Persistencia y rendimiento

- **JDBC, no Hibernate** (D-024): `JdbcTemplate` con SQL explícito y `RETURNING id`.
  Evita el coste del identity map y la clase de bugs de colecciones gestionadas que
  apareció en D-022.
- **Hijos reemplazados, no `DO NOTHING`** (D-027): si una carta pierde una keyword o una
  legalidad, la tabla hija debe reflejarlo. `DELETE … WHERE card_id = ?` + insert dentro
  de la misma transacción de la carta. Las tablas hijas no tienen referencias externas.
- **Lote de 500** con `reWriteBatchedInserts=true` en el `datasource.url` del perfil
  `import` (PostgreSQL JDBC). Configurable por flag.
- **Reintento fila a fila** solo cuando el lote falla (D-025): el caso normal no paga el
  coste.
- **`import_seen` en tabla real `UNLOGGED`** en lugar de una lista en memoria (D-026): no
  crece con el fichero y el `UPDATE` de obsoletas es un anti-join. No puede ser `TEMP`
  porque cada lote usa una conexión del pool.
- **`RETURNING (xmax = 0)`** distingue insertadas de actualizadas sin comparar contenido
  (truco de PostgreSQL: `xmax = 0` solo en la fila recién insertada).
- Los upserts actualizan `updated_at = now()`; `imported_at` conserva el `DEFAULT` de la
  primera inserción.
- **Advisory lock** (D-031): dos importaciones simultáneas corromperían contadores y
  marcas; la segunda sale con `2`.
- Sin `DELETE` de cartas en ningún camino.

## 8. Operación

| Flag | Defecto | Notas |
|---|---|---|
| `--import.file` | — | Obligatorio; JSONL de `default-cards` |
| `--import.rulings` | vacío | Opcional; bulk de rulings |
| `--import.batch-size` | `500` | 1–5000 |
| `--import.limit` | vacío (todo) | Solo desarrollo: primeras N líneas y **sin** barrido de obsoletas |
| `--spring.profiles.active` | — | `import` |

Resumen final (ejemplo):

```
cards: 512340 inserted, 0 updated, 512340 seen, 12 obsolete, 3 failed
faces: 48120, legality: 3074040, keywords: 1210044, prices: 498221
rulings: 30221 upserted
duration: 4m12s
```

Prueba rápida en desarrollo, sin bulk y sin marcar obsoletas:

```bash
java -jar mtgdeckbuilder.jar --spring.profiles.active=import \
  --import.file=src/test/resources/importer/default-cards-sample.jsonl
```

## 9. Tests

| Tipo | Nombre | Qué cubre |
|---|---|---|
| Unitario | `CopyLimitRulesTest` | Las tres ramas y los ejemplos del doc |
| Unitario | `LegalityRulesTest` | Mapeo, claves desconocidas, valores inválidos |
| Unitario | `CardShapeRulesTest` | Básicas, `type_line`, `cmc`, `image_url` |
| Unitario | `ScryfallCardMapperTest` | Caras, precios, tokens/emblem descartados |
| Unitario | `ImportOptionsTest` / `ScryfallImportRunnerTest` | Flags, rutas, códigos de salida |
| Integración | `CardImportIntegrationTest` | Fixture JSONL: dos impresiones del mismo `oracle_id`, una multiforma, un token, una legalidad desconocida, `any number`, `up to 7`, precios y rulings. Dos pasadas: idempotencia, obsoletas, hijos reemplazados, caras, precios, y `card_deck` intacto |
| Flyway | `FlywayMigrationSmokeIntegrationTest` | `V1`–`V5`, `is_obsolete`, default e índice |

- Fixture compartido: `src/test/resources/importer/default-cards-sample.jsonl`, unas 10
  líneas con los casos del párrafo anterior. Lo usa el IT y sirve para probar a mano
  (`--import.limit`) sin descargar el bulk.
- `src/test/resources/importer/default-cards-sample-v2.jsonl` solo trae las cartas que
  cambian; el IT lo usa para comprobar el reemplazo de hijos, legalidades, caras y precios
  sin duplicar filas.
- Los nombres mantienen los filtros de CI: `*Test` sin Docker, `*IntegrationTest` con
  Docker. No hace falta tocar `.github/workflows/ci.yml` ni `README.md` en ese punto.
- El trinquete de JaCoCo (**99,8 % líneas / 95,3 % ramas**) obliga a que cada rama nueva
  tenga test. El runner se prueba con un servicio falso, no con Spring completo.

## 10. Fases internas

| Fase | Entregable | Criterio de aceptación |
|---|---|---|
| 6.1 | `V5`, reglas puras y sus tests | `jacoco:check` verde sin tocar umbrales |
| 6.2 | Reader streaming + DAO de `cards` + IT mínimo | Fixture compartido, dos pasadas, mismos contadores |
| 6.3 | Hijos, legalidades, caras y precios | IT comprueba reemplazo de keywords y caras reales |
| 6.4 | `is_obsolete` + `import_seen` + lock | IT: carta retirada → `TRUE`; vuelve → `FALSE`; dos importaciones → `2` |
| 6.5 | Rulings, resumen y códigos de salida | Fallo de una fila → `1` y el resto cargado |
| 6.6 | Retirada del script y docs | Importación real completa en local + `DATA_IMPORT.md` actualizado |

## 11. Criterios de aceptación

- [ ] Segunda pasada: `inserted = 0`, mismas filas en todas las tablas hijas, `failed = 0`.
- [ ] Una línea corrupta se cuenta como fallida y no aborta la importación.
- [ ] Las cartas que dejan de venir se marcan `is_obsolete = TRUE`; ninguna se borra.
- [ ] Un `DELETE` de `cards` no aparece en el código del importador.
- [ ] Caras en `card_faces`, no en filas sintéticas.
- [ ] Precios y `price_observed_at` poblados cuando Scryfall los envía.
- [ ] `card_rulings` se llena solo si se pasa el segundo fichero.
- [ ] Memoria estable con el fichero completo (streaming, sin `readAllBytes`).
- [ ] `--import.limit N` carga N cartas, no marca obsoletas y lo avisa en el resumen.
- [ ] El fixture de `src/test/resources/importer/` carga sin descargar el bulk.
- [ ] `mvnw.cmd clean test …` + `jacoco:report jacoco:check` verdes.
- [ ] `import_cards.py` retirado y `docs/DATA_IMPORT.md` sin instrucciones de Python.

## 12. Riesgos

| Riesgo | Mitigación |
|---|---|
| Bulk de ~1 GB y cientos de miles de filas | Streaming, lotes, `reWriteBatchedInserts`, medir duración |
| Una fila con datos raros tumba el lote | Reintento fila a fila y contador de fallidas |
| Dos importaciones a la vez | Advisory lock |
| Caras sintéticas antiguas conviven con las nuevas | Aviso + recrear BD local; limpieza automática fuera de alcance |
| `card_rulings` con `comment` en la PK | Comentarios cortos; si algún día supera el límite de índice, migrar a clave sustituta |
| `orphanRemoval` u otro detalle de Hibernate | La escritura no usa Hibernate (D-024) |
| Trinquete de cobertura | Reglas puras y runner con dobles; sin clases "de pega" sin test |
| `DB_MIGRATION_STRATEGY.md` desactualizado | Actualizarlo: `V4` es comandante y la importación entra en `V5` |

## 13. Decisiones (D-023 – D-031)

Aceptadas el 2026-11-17 y anexadas a `docs/DECISION_LOG.md`. Aquí queda el texto para
consulta junto al plan.

### D-023 - Importador como CLI Java con perfil `import`

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** La importación es un `ApplicationRunner` con `@Profile("import")` que lee
  un JSONL, escribe y termina con código de salida. Se ejecuta con
  `--spring.profiles.active=import --import.file=…`; no hay endpoint HTTP, no corre en el
  arranque normal y no se usa Spring Batch.
- **Contexto:** El script Python ya se desincronizó varias veces del esquema
  (`unlimited_copies`, `card_set`, el `set_id` UUID de Scryfall) y no comparte tests ni
  configuración con la aplicación.
- **Por qué:** Un fichero de ~1 GB no es una petición web; la CLI da código de salida,
  logs y permite montar el fichero. Un único lenguaje evita la deriva. Spring Batch no
  aporta nada a un job de una pasada.
- **Alternativas descartadas:** endpoint administrativo (timeouts, seguridad, memoria);
  importar al arrancar (alarga el boot y falla a medias); Python con tests (sigue siendo
  un segundo stack); Spring Batch (dependencia y ceremonia para dos pasadas).
- **Consecuencias:** `import_cards.py` se retira tras validar la CLI. El JAR sirve para
  ambos modos.
- **Revisar si:** la importación pasa a ser un job programado con reintentos y alertas.
- **Detalle:** `docs/IMPORTER_PLAN.md` §3, §8; relacionada con D-018.

### D-024 - Escritura con JDBC y SQL explícito, sin Hibernate

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** El importador escribe con `JdbcTemplate`: `INSERT … ON CONFLICT …
  RETURNING id` y tablas hijas reemplazadas por carta.
- **Contexto:** Son cientos de miles de filas y siete tablas hijas; la ruta de escritura
  de la API ya demostró que las colecciones gestionadas de Hibernate esconden trampas
  (D-022).
- **Por qué:** El SQL es el mismo que ya valida Flyway y el script; no hay identity map,
  ni `flush`, ni `orphanRemoval`, ni lazy loading. El rendimiento es predecible y el
  `RETURNING id` evita una consulta por carta.
- **Alternativas descartadas:** repositorios JPA con `saveAll` y `batch_size` (dirty
  checking y merges innecesarios); `COPY` a tabla temporal + `MERGE` (más rápido pero
  duplica el modelo y complica las caras).
- **Consecuencias:** El importador tiene su propio DAO; los repositorios JPA no se
  tocan. Las reglas de dominio siguen en `application`.
- **Revisar si:** la duración real supera lo aceptable o se migra a `COPY`.
- **Detalle:** `docs/IMPORTER_PLAN.md` §7.

### D-025 - Streaming línea a línea y lotes con reintento fila a fila

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** Se lee el fichero línea a línea con un `BufferedReader` UTF-8 (tolerando
  BOM) y se escriben lotes de 500 cartas. Si un lote falla, se reintentan sus filas una a
  una para aislar la mala.
- **Contexto:** El bulk ronda 1 GB; cargarlo entero necesita varios GB de RAM. Un fallo
  de constraint en un lote de JDBC lo invalida completo.
- **Por qué:** Memoria constante y un dato corrupto cuesta una fila, no la importación.
- **Alternativas descartadas:** `readAllBytes`/`json.load` (RAM); savepoint por carta
  (más viajes a la BD en el camino feliz).
- **Consecuencias:** `--import.batch-size` es configurable; el resumen separa fallidas.
  `--import.limit` permite cargas parciales de desarrollo sobre el fixture.
- **Revisar si:** aparecen lotes fallidos con frecuencia (entonces conviene validar antes
  de escribir).
- **Detalle:** `docs/IMPORTER_PLAN.md` §4, §7.

### D-026 - `is_obsolete` para lo que desaparece; nunca borrar cartas

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** `V5` añade `cards.is_obsolete BOOLEAN NOT NULL DEFAULT FALSE` y una tabla
  `import_seen (run_id, scryfall_id)` `UNLOGGED`. Cada ejecución registra los
  `scryfall_id` vistos con su `run_id` y, al final, marca obsoletas las no vistas y
  desmarca las que vuelven.
- **Contexto:** `card_deck.card_id` es `ON DELETE CASCADE`: cualquier limpieza de cartas
  vacía mazos. El script actual no marca nada, así que lo retirado se queda para siempre
  como si fuera válido.
- **Por qué:** La obsolescencia es un dato, no una desaparición; se puede filtrar en
  búsquedas y el histórico de mazos no se rompe. La tabla evita mantener cientos de miles
  de UUID en memoria y funciona entre lotes; `run_id` aísla ejecuciones y `UNLOGGED`
  evita el coste de WAL en un dato que se puede perder (si el proceso muere, la
  importación se repite).
- **Alternativas descartadas:** `DELETE` de no vistos (cascada sobre mazos); lista en
  memoria (crece con el fichero); tabla `TEMP` (desaparece entre conexiones del pool);
  columna `last_seen_import` (mismo coste pero más ambigua de consultar).
- **Consecuencias:** Las consultas de búsqueda deberán decidir si filtran obsoletas
  (hoy no filtran). El `FlywayMigrationSmokeIntegrationTest` cubre la columna. Una
  carga parcial (`--import.limit`) nunca marca obsoletas: no ha visto el catálogo entero.
- **Revisar si:** se añade búsqueda semántica o el catálogo necesita ocultar retiradas.
- **Detalle:** `docs/IMPORTER_PLAN.md` §5, §7.

### D-027 - Hijos y legalidades se reemplazan por carta

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** Para cada carta reimportada se borran y reinsertan sus filas de
  `card_colors`, `card_color_identity`, `card_types`, `card_supertypes`, `card_subtypes`,
  `card_keywords`, `card_legality` y `card_faces`, dentro de la misma transacción.
- **Contexto:** El script usa `ON CONFLICT DO NOTHING`, así que una keyword o legalidad
  que Scryfall retira se queda para siempre y las tablas derivan.
- **Por qué:** El estado hijo debe ser exactamente el del bulk. Las tablas hijas no
  tienen referencias externas y son pequeñas por carta, así que el coste es bajo.
- **Alternativas descartadas:** `DO NOTHING` (deriva); `DELETE` global + recarga (rompe
  la atomicidad por carta y multiplica el trabajo).
- **Consecuencias:** Reimportar corrige datos antiguos, no solo añade.
- **Revisar si:** el reemplazo por carta domina el tiempo de importación.
- **Detalle:** `docs/IMPORTER_PLAN.md` §7.

### D-028 - Caras reales en `card_faces`; sin limpieza automática de sintéticas

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** Las caras se escriben en `card_faces` (`card_id`, `face_index`) con
  columnas normalizadas y `face_data` con el objeto crudo. El importador deja de crear
  filas de `cards` con `scryfall_id NULL`.
- **Contexto:** Las filas sintéticas existen para que el validador y el buscador
  funcionaran antes de tener `card_faces`; su identidad depende de una tupla
  `(card_name, set_id, collector_number)` y desaparece al rediseñar (D-018).
- **Por qué:** `card_faces` ya está en `V1` con entidad y claves; es el modelo correcto y
  no duplica cartas jugables.
- **Alternativas descartadas:** mantener ambas (dos fuentes de verdad); borrar sintéticas
  en la migración (`ON DELETE CASCADE` sobre `card_deck`).
- **Consecuencias:** La BD local se recrea antes de la primera pasada completa; el
  importador avisa si encuentra sintéticas. La limpieza automática queda fuera de alcance.
- **Revisar si:** hay una BD con mazos reales que no se pueda recrear.
- **Detalle:** `docs/IMPORTER_PLAN.md` §5.2.

### D-029 - Precios: último valor en `cards`, sin histórico

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** Se escriben `usd`, `usd_foil`, `usd_etched`, `eur`, `eur_foil`,
  `eur_etched` y `tix` en `cards`, con `price_observed_at = now()` cuando llega algún
  precio.
- **Contexto:** Las columnas existen desde `V1` y hoy están `NULL`; el script no las
  importa.
- **Por qué:** Es el dato que necesita el constructor de mazos para estimar coste; un
  histórico multiplica filas y no hay caso de uso todavía.
- **Alternativas descartadas:** tabla `card_price_history` (volumen sin consumidor).
- **Consecuencias:** Cada importación sobrescribe el precio anterior; el histórico se
  añade cuando exista una pantalla que lo use.
- **Revisar si:** se quieren gráficas de precio o alertas.
- **Detalle:** `docs/IMPORTER_PLAN.md` §6.

### D-030 - Rulings por `oracle_id`, desde un bulk aparte

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** `card_rulings` se llena desde el bulk de rulings con
  `--import.rulings`. Se conserva la PK `(oracle_id, published_at, comment, source)` y
  `source` guarda el valor de Scryfall en minúsculas (`wotc`/`scryfall`).
- **Contexto:** La tabla existe desde `V1` y está vacía; las rulings son por carta, no por
  impresión, y se comparten entre reprints.
- **Por qué:** La PK actual deduplica sin clave artificial y ya está indexada por
  `oracle_id`. Las rulings no son necesarias para validar, así que el fichero es opcional.
- **Alternativas descartadas:** clave sustituta (`BIGSERIAL`): no aporta nada mientras los
  comentarios sean cortos; cargar rulings desde el bulk de cartas (no viene ahí).
- **Consecuencias:** Si un comentario superase el límite de índice de Postgres, se migra
  a clave sustituta. No hay endpoint que las exponga todavía.
- **Revisar si:** se añade una pantalla de rulings o el `comment` crece.
- **Detalle:** `docs/IMPORTER_PLAN.md` §4, §6.

### D-031 - Operación: advisory lock, códigos de salida y resumen

- **Fecha:** 2026-11-17
- **Estado:** Aceptada
- **Decisión:** La CLI toma un advisory lock de Postgres, imprime un resumen
  (insertadas, actualizadas, obsoletas, fallidas, hijos, duración) y sale con
  `0` correcto, `1` con filas fallidas y `2` configuración/IO/bloqueo.
- **Contexto:** Dos importaciones simultáneas falsearían el anti-join de obsoletas y
  duplicarían trabajo; un `exit 0` con filas perdidas oculta el problema a CI y a un cron.
- **Por qué:** El código de salida es la única señal fiable para automatizar; el resumen
  permite auditar sin abrir la BD.
- **Alternativas descartadas:** `System.exit(0)` siempre (fallos silenciosos); lock por
  tabla (bloquea escrituras de la API).
- **Consecuencias:** El job programado de la fase 3 de `DATA_IMPORT.md` podrá alertar con
  el código de salida. El importador no se ejecuta en el pipeline de despliegue.
  `--import.limit` existe solo para desarrollo y el resumen lo marca como carga parcial.
- **Revisar si:** se programa semanalmente o se quiere `--import.dry-run`.
- **Detalle:** `docs/IMPORTER_PLAN.md` §4, §8; relacionada con D-023.

## 14. Preguntas abiertas y valor por defecto

| Pregunta | Defecto propuesto |
|---|---|
| ¿`default-cards` u `oracle-cards`? | `default-cards` (una fila por impresión, igual que hoy) |
| ¿Una migración o dos? | Una: `V5__import_support.sql` (`is_obsolete` + `import_seen`) |
| ¿Se migran las caras sintéticas? | No; recrear la BD local y avisar si aparecen |
| ¿`source` de rulings tal cual? | Minúsculas; `scryfall` si falta |
| ¿Qué pasa con `tags`/`card_tag`? | El importador no las toca (D-019) |
| ¿Filtrar obsoletas en la API? | No en esta fase; decidir con la búsqueda |
| ¿Carga parcial? | `--import.limit` solo desarrollo; nunca marca obsoletas |
| ¿Job programado? | Fuera de la fase 6 |
