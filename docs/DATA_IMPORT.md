# Importación de datos

- **Estado:** cargador provisional, no el importador definitivo
- **Alcance:** cómo poblar `cards`, `card_legality`, `sets` y `tags` desde Scryfall
- **Relacionado:** ADR-007, `docs/DECK_VALIDATION_MODEL.md`, `docs/DECISION_LOG.md`

## 1. Qué hay hoy

`src/main/resources/scripts/import_cards.py` lee un fichero JSONL de Scryfall
(`default-cards-*.jsonl`, una carta por línea) y lo inserta en PostgreSQL.

Es un **cargador de pruebas**: sirve para tener cartas reales en local, no para
producción. Su objetivo es que el validador y los endpoints de publicación se puedan
probar con datos del juego real en lugar de fixtures inventados.

### Lo que hace

| Comportamiento | Detalle |
|---|---|
| Identidad de impresión | Escribe `scryfall_id` (UUID de la impresión) y `oracle_id` |
| Una impresión por nombre | Si el nombre ya existe, la carta se salta |
| Límite de copias | `copies_limit` derivado de `oracle_text` (ver tabla) |
| Tierras básicas | `is_basic_land = TRUE` cuando el tipo es `Basic ... Land` |
| Legalidades | Escribe `card_legality` para los formatos sembrados |
| Palabras clave | Crea `tags` y `card_tag` con `source = 'scryfall_import'` |
| Caras múltiples | Separa cada cara y crea una carta combinada con relaciones |

### Lo que no hace

| Hueco | Consecuencia |
|---|---|
| No es idempotente | Reejecutar duplica filas; hay que recrear la BD |
| No actualiza cartas existentes | Un set nuevo no refresca precios ni textos |
| No marca obsoletas | Las cartas que salen de Scryfall se quedan para siempre |
| No importa precios | `usd`, `eur`, `tix` quedan `NULL` |
| No importa `rulings` | `card_rulings` queda vacía |
| Deduplica por nombre | No hay varias impresiones de la misma carta |

## 2. Mapeos

### 2.1 Legalidades

`cards_legality` se rellena solo para los formatos que existen en `formats`. Claves de
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

Scryfall no expone "cuántas copias permite esta carta", así que se deduce del texto:

| Regla en `oracle_text` | `copies_limit` | Ejemplos |
|---|---|---|
| `any number of cards named X` | `0` (sin límite) | Persistent Petitioners, Relentless Rats, Rat Colony |
| `up to N cards named X` | `N` | Seven Dwarves (`7`), Nazgûl (`9`) |
| Ninguna de las anteriores | `NULL` (manda el formato) | Resto de cartas |

Se aplica a todas las impresiones de un `oracle_id` porque el texto es el mismo.

## 3. Cómo ejecutarlo

Requisitos:

1. Docker con PostgreSQL/pgvector levantado y `V3__deck_validation_model.sql` aplicada.
2. Base de datos **vacía** salvo migraciones (el script no es idempotente).
3. Fichero JSONL de Scryfall descargado en local; los `docs/*.jsonl` están en
   `.gitignore`.

```powershell
$env:DB_HOST     = "localhost"
$env:DB_PORT     = "5433"
$env:DB_NAME     = "mtg_db"
$env:DB_USERNAME = "user"
$env:DB_PASSWORD = "..."

python src/main/resources/scripts/import_cards.py docs/default-cards-20260927090541.jsonl
```

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
2. El conocimiento del esquema debe vivir en un solo lenguaje; hoy el script ya se
   desincronizó una vez (`unlimited_copies`).
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
| Ahora | Script Python, a mano, recreando la BD | Cuando haga falta probar |
| API usable sin usuarios | CLI Java idempotente, a mano | Cada set nuevo o ban |
| Usuarios reales | Job programado (Task Scheduler, cron, GitHub Actions) | Semanal |

Scryfall publica bulk data a diario; no hace falta ese ritmo. La colección cambia cada
3 meses y las legalidades con bans puntuales.

**Nunca** importar al arrancar la aplicación ni en el pipeline de despliegue: alarga el
boot, exige el fichero en el servidor y falla a medias sin dejar estado claro.

## 6. Requisitos antes de automatizar

1. **Upsert por `scryfall_id`**: `ON CONFLICT (scryfall_id) DO UPDATE`. Nunca insert
   ciego.
2. **Nunca borrar cartas**: `card_deck.card_id` es `ON DELETE CASCADE`, así que un
   import que limpie vacía mazos. Las cartas ausentes se marcan con
   `cards.is_obsolete BOOLEAN NOT NULL DEFAULT FALSE`.
3. **Idempotencia**: ejecutar dos veces deja el mismo estado y no duplica `card_tag`,
   `card_legality` ni relaciones.
4. **Registro del resultado**: filas insertadas, actualizadas, marcadas obsoletas y
   errores por carta, en un resumen final y con código de salida distinto de cero si
   hay fallos.

`is_obsolete` todavía no existe en el esquema; entra cuando se escriba el importador
Java, no antes.

## 7. Fases

1. **Ahora**: script parcheado con `scryfall_id`, `oracle_id`, `copies_limit`,
   `is_basic_land` y legalidades. Manual, BD recreable. ✅
2. **Tras la API de mazos**: escritura del importador Java con upsert y `is_obsolete`.
3. **Con usuarios**: job programado semanal y alerta si el import falla.
