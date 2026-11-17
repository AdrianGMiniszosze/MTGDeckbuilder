# Importación de datos

- **Estado:** cargador provisional, no el importador definitivo
- **Alcance:** cómo poblar `cards`, `card_legality`, `sets` y `card_keywords` desde Scryfall
- **Relacionado:** ADR-007, `docs/DECK_VALIDATION_MODEL.md`, `docs/DECISION_LOG.md`
- **Sustitución planificada:** `docs/IMPORTER_PLAN.md` (fase 6, decisiones D-023–D-031)

## 1. Qué hay hoy

`src/main/resources/scripts/import_cards.py` lee un fichero JSONL de Scryfall
(`default-cards-*.jsonl`, una carta por línea) y lo **inserta o actualiza** en
PostgreSQL.

Es un **cargador de pruebas**: sirve para tener cartas reales en local, no para
producción. Su objetivo es que el validador y los endpoints de publicación se puedan
probar con datos del juego real en lugar de fixtures inventados.

### Lo que hace

| Comportamiento | Detalle |
|---|---|
| Identidad de impresión | `scryfall_id` = campo `id` de Scryfall; `oracle_id` aparte |
| Una fila por impresión | Varias impresiones del mismo `oracle_id` conviven sin problema |
| Upsert idempotente | Si el `scryfall_id` existe, actualiza la fila en vez de duplicarla |
| Caras sintéticas | Cada cara se guarda sin `scryfall_id`; se localiza por `(card_name, set_id, collector_number)` |
| `set_id` | Usa `sets.id` local y rellena `sets.code` con el código de Scryfall; el `set_id` UUID de Scryfall no se usa |
| Límite de copias | `copies_limit` derivado de `oracle_text` (ver tabla) |
| Tierras básicas | `is_basic_land = TRUE` cuando el tipo es `Basic ... Land` |
| Legalidades | Escribe `card_legality` para los formatos sembrados |
| Palabras clave | Escribe `card_keywords`; no crea `tags` ni `card_tag` (reservados a tags semánticos, D-019) |
| Caras múltiples | Separa cada cara y crea una carta combinada con relaciones |
| Hijos idempotentes | `ON CONFLICT DO NOTHING` en colores, tipos, keywords y relaciones |
| Aislamiento de fallos | Savepoint por carta/cara: una fila mala se descarta y el lote sigue |
| Lectura en streaming | Recorre el JSONL línea a línea; no carga los ~600 MB en memoria |
| Consola Windows | Reconfigura `stdout`/`stderr` a UTF-8 y tolera BOM en el fichero |

### Lo que no hace

| Hueco | Consecuencia |
|---|---|
| No marca obsoletas | Las cartas que salen de Scryfall se quedan para siempre |
| No importa precios | `usd`, `eur`, `tix` quedan `NULL` |
| No importa `rulings` | `card_rulings` queda vacía |
| No borra filas | Correcto: `card_deck.card_id` es `ON DELETE CASCADE` y vaciaría mazos |

## 2. Mapeos

### 2.1 Legalidades

`card_legality` se rellena solo para los formatos que existen en `formats`. Claves de
Scryfall sin formato equivalente se ignoran a propósito: la API las reporta como
`LEGALITY_UNKNOWN`, que es un aviso, no un error.

| Clave de Scryfall | `formats.format_name` |
|---|---|
| `standard` | Standard |
| `modern` | Modern |
| `legacy` | Legacy |
| `vintage` | Vintage |
| `pauper` | Pauper |
| `commander` | Commander |

Valores guardados: `legal`, `not_legal`, `restricted`, `banned`.

### 2.2 Límite de copias

Scryfall no expone "cuántas copias permite esta carta", así que se deduce del texto.
Solo cuenta el texto de **construcción de mazo**; un tutorizador puede decir "up to
three cards named" sin limitar el mazo (Howling Wolf).

| Regla en `oracle_text` | `copies_limit` | Ejemplos |
|---|---|---|
| `A deck can have any number of cards named X` | `0` (sin límite) | Persistent Petitioners, Relentless Rats, Dragon's Approach |
| `A deck can have up to N cards named X` | `N` | Seven Dwarves (`7`), Nazgûl (`9`) |
| Ninguna de las anteriores | `NULL` (manda el formato) | Resto de cartas, incluido Howling Wolf |

Se aplica a todas las impresiones de un `oracle_id` porque el texto es el mismo.

### 2.3 Caras múltiples

Una carta con `card_faces` (adventure, split, transform, modal DFC, flip) genera tres
tipos de fila:

| Fila | `scryfall_id` | `collector_number` | Identidad en reimportación |
|---|---|---|---|
| Carta combinada (`A // B`) | UUID de la impresión | `272` | `scryfall_id` |
| Cara A | `NULL` | `272a` | `(card_name, set_id, collector_number)` |
| Cara B | `NULL` | `272b` | `(card_name, set_id, collector_number)` |

Las caras sintéticas no tienen impresión propia en Scryfall, así que ese es el único
trío estable para reconocerlas. `card_relationships` enlaza cada cara con la combinada
en ambos sentidos y usa `ON CONFLICT DO NOTHING`.

Los layouts `token`, `emblem` y `art_series` se descartan **antes** de mirar
`card_faces`: un token de doble cara también tiene `//` en el nombre y si no se filtra
primero entra como si fuera una carta jugable.

Este diseño es provisional. Lo correcto es una tabla `card_faces` con una fila por
cara; las filas sintéticas existen para que el validador y el buscador funcionen ya.
El rediseño va con el importador Java, no antes.

## 3. Cómo ejecutarlo

Requisitos:

1. Docker con PostgreSQL/pgvector levantado y las migraciones `V1`–`V3` aplicadas
   (`V1__baseline_schema.sql`, `V2__seed_formats.sql`, `V3__deck_validation_model.sql`).
2. La BD puede contener datos: el script hace upsert, no hace falta recrearla.
3. Fichero JSONL de Scryfall descargado en local; los `docs/*.jsonl` están en
   `.gitignore`.
4. Python 3 con `psycopg2` instalado.

```powershell
$env:DB_HOST     = "localhost"
$env:DB_PORT     = "5433"
$env:DB_NAME     = "mtg_db"
$env:DB_USERNAME = "user"
$env:DB_PASSWORD = "..."

python src/main/resources/scripts/import_cards.py docs/default-cards-20260927090541.jsonl
```

El resumen final separa insertadas, actualizadas, saltadas y filas fallidas. Ejecutar
dos veces seguidas debe dar `inserted = 0` y el mismo número de `updated` que de filas
procesables, sin crecer el resto de contadores.

Verificación:

```sql
SELECT count(*) FROM cards;
SELECT count(*) FROM card_legality;
SELECT count(*) FROM cards WHERE scryfall_id IS NULL;
SELECT copies_limit, count(*) FROM cards GROUP BY copies_limit ORDER BY 1 NULLS LAST;
SELECT card_name, copies_limit FROM cards
 WHERE copies_limit IS NOT NULL ORDER BY card_name LIMIT 20;
```

## 4. Por qué no es el importador definitivo

El importador que se automatice debe ser **Java**, por tres motivos:

1. `copies_limit` y el mapeo de legalidades son reglas de dominio: se testean con JUnit
   y Testcontainers, no con `print()`.
2. El conocimiento del esquema debe vivir en un solo lenguaje; el script ya se
   desincronizó varias veces (`unlimited_copies`, `card_set` y el `set_id` UUID de
   Scryfall).
3. Comparte configuración, mappers y Flyway con la aplicación, sin `psycopg2` ni un
   Python concreto en el servidor.

Forma propuesta, sin Spring Batch (un job no justifica la dependencia):

```bash
java -jar mtgdeckbuilder.jar --spring.profiles.active=import \
  --import.file=docs/default-cards-XXXX.jsonl
```

Un componente `@Profile("import")` que importa y termina con código de salida.
**No** en el arranque normal de la aplicación y **no** como endpoint HTTP: una
importación de 600 MB no es una petición web.

## 5. Cuándo se actualiza

| Momento | Cómo | Frecuencia |
|---|---|---|
| Ahora | Script Python, a mano, sobre BD migrada (upsert) | Cuando haga falta probar |
| API usable sin usuarios | CLI Java idempotente, a mano | Cada set nuevo o ban |
| Usuarios reales | Job programado (Task Scheduler, cron, GitHub Actions) | Semanal |

Scryfall publica bulk data a diario; no hace falta ese ritmo. La colección cambia cada
3 meses y las legalidades con bans puntuales.

**Nunca** importar al arrancar la aplicación ni en el pipeline de despliegue: alarga el
boot, exige el fichero en el servidor y falla a medias sin dejar estado claro.

## 6. Requisitos antes de automatizar

1. **Upsert por `scryfall_id`**: `ON CONFLICT (scryfall_id) DO UPDATE`. Nunca insert
   ciego. ✅ (ya en el script)
2. **Nunca borrar cartas**: `card_deck.card_id` es `ON DELETE CASCADE`, así que un
   import que limpie vacía mazos. Las cartas ausentes se marcan con
   `cards.is_obsolete BOOLEAN NOT NULL DEFAULT FALSE`.
3. **Idempotencia**: ejecutar dos veces deja el mismo estado y no duplica
   `card_keywords`, `card_legality` ni relaciones. ✅ (ya en el script)
4. **Registro del resultado**: filas insertadas, actualizadas, marcadas obsoletas y
   errores por carta, en un resumen final y con código de salida distinto de cero si
   hay fallos. Insertadas/actualizadas/saltadas/fallidas ya se cuentan; faltan
   obsoletas y el código de salida.

`V5__import_support.sql` ya crea `is_obsolete` y la tabla `import_seen`; el importador
Java que las usa es la fase 6 de `docs/IMPORTER_PLAN.md`.

## 7. Fases

1. **Ahora**: script con `scryfall_id`, `oracle_id`, `copies_limit`, `is_basic_land`,
   legalidades, upsert idempotente y caras sintéticas por tupla. Manual, sobre BD
   migrada. ✅
2. **Tras la API de mazos**: escritura del importador Java con precios, `rulings` y
   `is_obsolete`; las caras múltiples deberían pasar a `card_faces` en lugar de filas
   sintéticas.
3. **Con usuarios**: job programado semanal y alerta si el import falla.
