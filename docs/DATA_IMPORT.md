# Importación de datos

- **Estado:** importador Java en producción interna; el script Python ya no existe
- **Alcance:** cómo poblar `cards`, `card_faces`, `card_legality`, `card_keywords`,
  `card_rulings` y `sets` desde el bulk de Scryfall
- **Relacionado:** `docs/IMPORTER_PLAN.md` (diseño y fases), `docs/DECISION_LOG.md`
  (D-023–D-033), `docs/DECK_VALIDATION_MODEL.md` (reglas de mazo)
- **Sustituye a:** `src/main/resources/scripts/import_cards.py`, retirado en la fase 6.6

## 1. Qué hay hoy

La importación es una **CLI Java** dentro del JAR de la aplicación, activada con el perfil
`import`:

- No arranca Tomcat (`spring.main.web-application-type=none`): es un proceso que lee,
  escribe y termina.
- Recorre el JSONL en streaming (`ScryfallBulkReader`), sin cargar los ~600 MB en memoria.
- Escribe con **JDBC plano**, sin Hibernate: lotes de 500 filas, sin unidad de trabajo ni
  colecciones gestionadas.
- Es idempotente por `scryfall_id` y nunca borra cartas: las que dejan de venir se marcan
  `is_obsolete = TRUE`. La API las oculta por defecto; `include_obsolete=true` las devuelve
  en `/cards` y `/cards/search` (D-034).
- Toma un advisory lock de PostgreSQL, así que dos importaciones no pueden solaparse.

### Lo que hace

| Comportamiento | Detalle |
|---|---|
| Identidad de impresión | `scryfall_id` = campo `id` de Scryfall; `oracle_id` aparte y no único |
| Una fila por impresión | Varias impresiones del mismo `oracle_id` conviven |
| Upsert idempotente | `ON CONFLICT (scryfall_id) DO UPDATE`; `xmax = 0` distingue insertada de actualizada |
| Hijos reemplazados | Colores, identidad, tipos, supertipos, subtipos, keywords, legalidades y caras se borran y se reinsertan por carta (D-027) |
| Caras múltiples | Una fila por cara en `card_faces`, con `face_index`, textos, colores, poder/resistencia e imágenes |
| Precios | `usd`, `usd_foil`, `usd_etched`, `eur`, `eur_foil`, `eur_etched`, `tix` + `price_observed_at` |
| Legalidades | Solo formatos sembrados en `formats`; el resto se ignora |
| Palabras clave | `card_keywords`; no crea `tags` ni `card_tag` (reservados a tags semánticos, D-019) |
| Sets | Upsert por `sets.name`; rellena `code`, `released_at` y `set_type` sin pisar lo existente |
| Obsolescencia | Anti-join contra `import_seen` al final de una carga completa |
| Rulings | Espejo con staging (`V6`): la tabla real acaba siendo exactamente el fichero actual |
| Aislamiento de fallos | Un lote fallido se reintenta fila a fila; la fila mala se cuenta y el resto sigue |
| Resumen y códigos | `ImportSummary` al final y salida `0`/`1`/`2` (D-031) |
| Consola Windows | UTF-8 en `stdout`/`stderr` y BOM tolerado en el fichero |

### Lo que no hace

| Hueco | Consecuencia |
|---|---|
| No borra cartas | Correcto: `card_deck.card_id` es `ON DELETE CASCADE` y vaciaría mazos |
| No descarga el bulk | Hay que bajar el fichero de Scryfall a mano |
| No es un endpoint HTTP | Una carga de 600 MB no es una petición web |
| No corre al arrancar la app | El perfil `import` es obligatorio; el arranque normal no importa nada |

## 2. Requisitos

1. **Java 21** y el JAR construido (`.\mvnw.cmd clean package -DskipTests`).
2. **PostgreSQL + pgvector** levantado (Compose: `localhost:5433/mtg_db`). Flyway aplica
   `V1`–`V6` la primera vez; no hay que preparar el esquema a mano.
3. **Variables de conexión**: `DB_PASSWORD` es obligatoria fuera del perfil `dev`;
   `DB_URL` (por defecto `jdbc:postgresql://localhost:5433/mtg_db`) y `DB_USERNAME`
   (por defecto `user`) son opcionales.
4. **Ficheros JSONL** descargados del [bulk de Scryfall](https://scryfall.com/docs/api/bulk-data):
   `default-cards-*.jsonl` (todas las impresiones) y, opcionalmente, `rulings-*.jsonl`.
   Los `docs/*.jsonl` están en `.gitignore`: son artefactos locales.

## 3. Cómo se ejecuta

### 3.1 Carga completa con rulings

```powershell
$env:DB_PASSWORD = "..."
java -jar target\mtgdeckbuilder-0.0.1-SNAPSHOT.jar `
  --spring.profiles.active=import `
  --import.file=docs/default-cards-20260927090541.jsonl `
  --import.rulings=docs/rulings-20260927090032.jsonl
```

### 3.2 Carga parcial (solo desarrollo)

```powershell
java -jar target\mtgdeckbuilder-0.0.1-SNAPSHOT.jar `
  --spring.profiles.active=import `
  --import.file=docs/default-cards-20260927090541.jsonl `
  --import.limit=1000
```

`--import.limit N` procesa **N líneas** del fichero (no N cartas): las líneas de tokens,
emblems y art series cuentan y se saltan. Con límite activo:

- no se ejecuta el barrido de obsoletas (`sweepSkipped=true`),
- las rulings se omiten aunque se pase `--import.rulings` (`rulingMirrorSkipped=true`),
- el resumen sale con `partial=true`.

### 3.3 Opciones

| Opción | Por defecto | Límites |
|---|---|---|
| `--import.file` | obligatoria | fichero regular legible |
| `--import.rulings` | ninguna | fichero regular legible; sin él no se toca `card_rulings` |
| `--import.batch-size` | `500` | 1–5000 |
| `--import.limit` | sin límite | entero positivo; fuerza carga parcial |

### 3.4 Códigos de salida

| Código | Significado |
|---|---|
| `0` | Todo correcto, incluidas las filas saltadas (tokens, emblems, art series) |
| `1` | Alguna fila falló; el resto se cargó y los pasos destructivos se omitieron |
| `2` | Configuración, IO, lock ocupado, fichero vacío o error inesperado |

Los códigos son el contrato de automatización (D-031): un job puede distinguir «hay filas
malas pero se avanzó» de «no se tocó nada».

### 3.5 Resumen

El runner imprime una única línea al terminar:

```text
Import finished: cards inserted=937 updated=0 skipped=63 failed=0 obsolete=0;
rulings staged=0 inserted=0 deleted=0 skipped=0 failed=0;
partial=true sweepSkipped=true rulingMirrorSkipped=false; duration=9239ms
```

- `skipped`: líneas válidas que no son cartas jugables (token, emblem, art series).
- `failed`: líneas ilegibles o filas que la BD rechazó.
- `obsolete`: cartas marcadas como obsoletas en esta pasada.
- `rulings staged/inserted/deleted`: filas del fichero de rulings, altas y bajas del espejo.

## 4. Mapeos

### 4.1 Legalidades

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

### 4.2 Límite de copias

Scryfall no expone "cuántas copias permite esta carta", así que se deduce del texto
(`CopyLimitRules`). Solo cuenta el texto de **construcción de mazo**; un tutorizador puede
decir "up to three cards named" sin limitar el mazo (Howling Wolf).

| Regla en `oracle_text` | `copies_limit` | Ejemplos |
|---|---|---|
| `A deck can have any number of cards named X` | `0` (sin límite) | Persistent Petitioners, Relentless Rats, Dragon's Approach |
| `A deck can have up to N cards named X` | `N` | Seven Dwarves (`7`), Nazgûl (`9`) |
| Ninguna de las anteriores | `NULL` (manda el formato) | Resto de cartas, incluido Howling Wolf |

Se aplica a todas las impresiones de un `oracle_id` porque el texto es el mismo.

### 4.3 Caras múltiples

Una impresión con `card_faces` (adventure, split, transform, modal DFC, flip, battle)
mantiene **una sola fila** en `cards` y escribe una fila por cara en `card_faces`
(`face_index` 0..n-1). La fila de `cards` guarda el texto concatenado de las caras y la
unión de tipos, colores y keywords; cada cara conserva su propio texto, tipo, coste,
poder/resistencia, lealtad/defensa e imágenes.

Los layouts `token`, `emblem` y `art_series` se descartan **antes** de mirar `card_faces`:
un token de doble cara también tiene `//` en el nombre y, sin filtrar primero, entraría
como carta jugable. Se cuentan como `skipped`.

## 5. Idempotencia, obsolescencia y rulings

### 5.1 Segunda pasada

Ejecutar dos veces los mismos ficheros deja el mismo estado:

- `cards inserted = 0` y `updated` = cartas procesables,
- las tablas hijas acaban con las mismas filas (se reemplazan, no se acumulan),
- `obsolete = 0` si el fichero no cambió,
- `rulings staged` igual que la primera vez, `inserted = 0` y `deleted = 0`.

Si una carta cambia de texto, legalidad, keyword o precio, la pasada siguiente lo
sobrescribe.

### 5.2 Barrido de obsoletas

`import_seen` (`UNLOGGED`) guarda los `scryfall_id` vistos en la ejecución en curso. Al
final de una carga completa, un anti-join marca `is_obsolete = TRUE` en las cartas que
no aparecieron, actualiza `updated_at` y **no borra nada**. Una carta que vuelve a
aparecer se desmarca en su upsert.

Guardas (D-032):

- si el fichero no produjo ninguna carta vista, se aborta con código `2` en vez de
  retirar el catálogo entero;
- las filas de otros `run_id` se limpian al empezar y las propias al terminar, también
  cuando hay filas fallidas;
- si alguna fila de carta falló, el barrido se omite (`sweepSkipped=true`): «no se pudo
  leer» no es «ya no existe».

### 5.3 Rulings

`V6__ruling_import_staging.sql` añade `import_ruling` (`UNLOGGED`). La pasada hace
staging de todas las rulings del fichero y, solo si no hubo filas fallidas y el staging
no está vacío, **espeja** `card_rulings`:

- borra las filas que no están en el staging de esta ejecución (correcciones y retiradas),
- inserta las nuevas con `ON CONFLICT DO NOTHING`.

Scryfall no publica un id de ruling y la terna `(oracle_id, source, published_at)` no es
única (hasta 32 rulings comparten terna), así que el espejo es la única forma de reflejar
el fichero actual sin dejar filas viejas. Un fichero vacío o truncado no vacía la tabla:
se aborta con `2`. Sin `--import.rulings` la tabla no se toca.

## 6. Verificación

Tras una carga completa:

```sql
SELECT count(*) AS cards,
       count(*) FILTER (WHERE is_obsolete) AS obsolete,
       count(*) FILTER (WHERE scryfall_id IS NULL) AS without_printing
  FROM cards;

SELECT count(*) FROM card_faces;
SELECT count(*) FROM card_legality;
SELECT count(*) FROM card_keywords;
SELECT count(*) FROM card_rulings;
SELECT count(DISTINCT oracle_id) FROM card_rulings;

SELECT count(*) FROM cards WHERE usd IS NOT NULL AND price_observed_at IS NOT NULL;
SELECT copies_limit, count(*) FROM cards GROUP BY copies_limit ORDER BY 1 NULLS LAST;

-- Staging limpio tras cualquier ejecución (también fallida)
SELECT count(*) FROM import_seen;
SELECT count(*) FROM import_ruling;
```

La segunda pasada completa debe mantener los mismos totales y añadir solo `updated`.

## 7. Cuándo se actualiza

| Momento | Cómo | Frecuencia |
|---|---|---|
| Desarrollo | CLI a mano, con `--import.limit` si solo hacen falta muestras | Cuando haga falta |
| Ahora | CLI completa a mano sobre la BD local | Cada set nuevo, ban o cambio de precios |
| Usuarios reales | Job programado (Task Scheduler, cron, GitHub Actions) | Semanal o mensual |

Scryfall publica bulk data a diario; no hace falta ese ritmo. La colección cambia cada
3 meses y las legalidades con bans puntuales.

**Nunca** importar al arrancar la aplicación ni en el pipeline de despliegue: alarga el
boot, exige el fichero en el servidor y falla a medias sin dejar estado claro.
