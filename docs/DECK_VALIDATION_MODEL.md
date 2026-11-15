# Modelo de validación de mazos

- **Estado:** aprobado; fases 1-5 implementadas
- **Alcance:** estados de mazo, diagnóstico de legalidad, contrato y esquema
- **Relacionado:** ADR-007 (reemplaza ADR-002), ADR-006, `docs/SCHEMA_DESIGN.md`, D-012

## 1. Problema

La legalidad se comprobaba en dos sitios que actuaban como **veto**:

- `DeckValidationServiceImpl` (existente pero **sin cablear**: `DeckServiceImpl` no lo inyecta).
- Trigger `check_card_quantity` sobre `card_deck` (`V1:344`).

Ese veto impide guardar mazos en construcción y devuelve un único mensaje sin saber a
qué carta corresponde. Además duplica la política en dos lenguajes, no puede replicar
las reglas de companion (que viven en Java) y no cubre `DELETE`.

## 2. Principios

1. **Guardar es libre; publicar es estricto.**
2. **La forma del dato vive en la BD** (FK, `CHECK`, `UNIQUE`); **la política vive en Java**.
3. **El diagnóstico se calcula, no se lanza.**
4. **Cada problema identifica su carta** (`card_id`, `deck_content_id`) para que la
   interfaz coloree filas sin parsear mensajes.
5. **Una sola definición de cada regla.**

## 3. Estados del mazo

| `decks.status` | Significado | Reglas de formato |
|---|---|---|
| `draft` | En construcción | No se aplican |
| `published` | Listo para jugar | Se aplican en `publish()` |

- Default `draft`. `published_at` (`TIMESTAMPTZ`, nullable).
- **Editar un mazo publicado lo devuelve a `draft`** y limpia `published_at`.
- `unpublish` vuelve a `draft` sin validar.
- **Se permite publicar con solo warnings** (`LEGALITY_UNKNOWN`). Se rechaza con
  cualquier `error`.

## 4. Códigos de problema

`severity = error` bloquea la publicación; `warning` solo informa.

| Código | Sev. | Cuándo |
|---|---|---|
| `CARD_BANNED` | error | `legality_status = 'banned'` |
| `CARD_NOT_LEGAL` | error | `legality_status = 'not_legal'` |
| `MAX_COPIES_EXCEEDED` | error | Copias de la carta (por `oracle_id`, main + sideboard) > límite |
| `SINGLETON_VIOLATION` | error | Formato singleton y más de 1 copia |
| `SIDEBOARD_TOO_LARGE` | error | Sideboard > `max_sideboard_size` |
| `DECK_TOO_LARGE` | error | Main > `max_deck_size` |
| `DECK_TOO_SMALL` | error | Main < `min_deck_size`; se emite siempre, solo bloquea al publicar |
| `MISSING_COMMANDER` | error | **Diferido** (D-012): no hay campo de comandante en esquema ni contrato |
| `COMPANION_RULE_VIOLATION` | error | `CompanionRules`, Umori, Yorion, Zirda |
| `LEGALITY_UNKNOWN` | warning | El formato rastrea legalidad pero no hay fila para esa carta |

Reglas de aplicación:

- `maybeboard` no cuenta para copias ni tamaños.
- `is_basic_land` exime del límite de copias.
- `formats.tracks_legality = FALSE` (Casual) desactiva legalidad **y**
  `LEGALITY_UNKNOWN`: no se emite ningún aviso por falta de datos.

## 5. Contrato (`openapi.yml`)

### 5.1 Campos nuevos y cambios (implementado)

`Deck`:

| Campo | Tipo | Notas |
|---|---|---|
| `format` | `integer` | **`required`**. Sustituye al 409 por `NOT NULL` |
| `status` | `draft` \| `published` | `readOnly` |
| `validation_status` | `legal` \| `illegal` \| `unknown` | `readOnly`, calculado; `null` en la lista |
| `issue_count` | `{errors, warnings}` | `readOnly`, calculado; `null` en la lista |
| `issues` | `array<DeckIssue>` | `readOnly`, calculado; `null` en la lista |

`CompleteDeck`: expone los mismos cuatro campos de diagnóstico que `Deck`.

`CardDeck`: sin `issues` (D-013). `Card`: `unlimited_copies` → **`copies_limit`**
(`integer`, nullable). Breaking, sin consumidores.

### 5.2 Esquemas nuevos

```yaml
DeckIssue:
  type: object
  required: [severity, code, message]
  properties:
    severity: { type: string, enum: [error, warning] }
    code: { type: string }
    message: { type: string }
    card_id: { type: integer }
    deck_content_id: { type: integer }
    section: { type: string, enum: [main, sideboard, maybeboard] }

DeckValidation:
  type: object
  required: [deck_id, status, validation_status, issues]
  properties:
    deck_id: { type: integer }
    status: { type: string, enum: [draft, published] }
    validation_status: { type: string, enum: [legal, illegal, unknown] }
    issues: { type: array, items: { $ref: '#/components/schemas/DeckIssue' } }

IssueCount:
  type: object
  required: [errors, warnings]
  properties:
    errors: { type: integer, minimum: 0 }
    warnings: { type: integer, minimum: 0 }
```

### 5.3 Endpoints (implementados en fases 3 y 5)

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/decks/{id}/validation` | `200` con `DeckValidation` (nunca falla por ilegalidad) |
| `POST` | `/decks/{id}/publish` | `200` con `DeckDTO`; `409` `DECK_NOT_LEGAL` + `issues` si hay errores |
| `POST` | `/decks/{id}/unpublish` | `200` con `DeckDTO`, vuelve a `draft` |
| `POST` | `/decks/{id}/cards` | `200` con `DeckDTO` y diagnóstico; suma copias |
| `DELETE` | `/decks/{id}/cards/{card_id}` | `200` con `DeckDTO`; resta copias y borra la fila al llegar a 0 |

`DeckCardRequest` (`card_id`, `quantity`, `section` con `main` por defecto) evita
reutilizar `CardDeck`, que exige `deck_id` en el cuerpo aunque venga en la ruta.

El cuerpo del `409` es `DeckNotLegalError`: los campos de `ErrorResponse` más
`issues`. `DeckValidationMapper` traduce el dominio a `DeckValidationDTO`, incluyendo
`section` (string) a su enum generado.

### 5.4 Errores

- Formato ausente o inválido: `400` `INVALID_REQUEST_BODY` (el DTO generado rechaza
  `format` nulo antes de entrar al controlador).
- Integridad: `400` `INVALID_DECK_COMPOSITION`; `404` `CARD_NOT_FOUND` /
  `DECK_NOT_FOUND` / `FORMAT_NOT_FOUND`.
- Publicación fallida: `409` `DECK_NOT_LEGAL` con `issues`.

## 6. Persistencia: `V3__deck_validation_model.sql`

Una sola migración con todo el modelo. Sin datos que preservar (BD recreable, nada
desplegado).

### 6.1 Estado del mazo

```sql
ALTER TABLE decks ADD COLUMN status TEXT NOT NULL DEFAULT 'draft';
ALTER TABLE decks ADD COLUMN published_at TIMESTAMPTZ NULL;

ALTER TABLE decks ADD CONSTRAINT decks_status_check
    CHECK (status IN ('draft', 'published'));
ALTER TABLE decks ADD CONSTRAINT decks_published_at_check
    CHECK ((status = 'published') = (published_at IS NOT NULL));

CREATE INDEX idx_decks_status ON decks (status);
```

### 6.2 Formato Casual y `tracks_legality`

```sql
ALTER TABLE formats ADD COLUMN tracks_legality BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO formats (format_name, description, min_deck_size, max_deck_size,
                     max_sideboard_size, max_copies, singleton, requires_commander,
                     tracks_legality)
VALUES ('Casual', 'Sin reglas de formato; solo integridad de datos.',
        0, NULL, NULL, 4, FALSE, FALSE, FALSE)
ON CONFLICT (format_name) DO NOTHING;
```

`Casual` usa `min_deck_size = 0` porque la columna es `NOT NULL` (`V1:180`);
`max_deck_size` y `max_sideboard_size` son `NULL` = sin tope. `max_copies` también es
`NOT NULL` (`V1:183`), de ahí que el "sin límite" se resuelva con `copies_limit`.

### 6.3 Límite de copias por carta

```sql
ALTER TABLE cards DROP COLUMN unlimited_copies;
ALTER TABLE cards ADD COLUMN copies_limit INTEGER NULL
    CHECK (copies_limit IS NULL OR copies_limit >= 0);
```

| `copies_limit` | Significado | Ejemplos |
|---|---|---|
| `NULL` | Sin excepción; manda el formato | Cualquier carta normal |
| `0` | Sin límite | Persistent Petitioners, Relentless Rats, Shadowborn Apostle, Dragon's Approach |
| `N > 0` | Tope exacto, prevalece sobre el formato | Seven Dwarves = 7, Nazgûl = 9 |

Se cuenta por `oracle_id` (no por `card_id` ni por `card_name`): la regla es por
**nombre** y cada `card_id` es una impresión distinta, así que cuatro copias de dos
impresiones son ocho copias de la misma carta.

### 6.4 Retirada del trigger de reglas

```sql
DROP TRIGGER IF EXISTS enforce_card_quantity_limit ON card_deck;
DROP FUNCTION IF EXISTS check_card_quantity();
```

Se conservan:

- FKs `card_deck.deck_id`, `card_deck.card_id` (`ON DELETE CASCADE`).
- `UNIQUE (deck_id, card_id, section)`.
- `CHECK (quantity > 0)`, `CHECK (section IN ('main','sideboard','maybeboard'))`.
- Triggers genéricos `set_updated_at()` de `users`, `sets`, `cards`, `formats`, `decks`.

`decks.updated_at` al cambiar cartas pasa a gestionarse en Java
(`DeckServiceImpl.updateDeckModificationTime`), incluyendo el caso `DELETE` que el
trigger nunca cubrió.

**Trade-off aceptado:** la BD deja de garantizar que un mazo publicado sea legal ante
escrituras que no pasen por la API. Hoy la API es el único escritor. Si aparece un
segundo escritor (importador o scripts de administración), se reevalúa.

## 7. Java

### 7.1 Refactor del validador (implementado)

```java
DeckValidationResult evaluate(Long deckId);
```

- `DeckValidationResult`: `deckId`, `status`, `validationStatus` (`legal` / `illegal` /
  `unknown`), `List<DeckIssue>`; helpers `getErrors`, `getWarnings`, `hasErrors`.
- `DeckIssue`: `severity`, `code`, `message`, `cardId`, `deckContentId`, `section`.
  `cardId`/`deckContentId` son `null` cuando el problema afecta al mazo completo o al
  grupo de copias repartido entre secciones.
- Sin excepciones para reglas de formato: solo lanza `DeckNotFoundException` y
  `FormatNotFoundException`. `validateCardRemoval` se mantiene (integridad de datos).
- `validateCardAddition` se elimina de la interfaz: el guardado ya no valida formato.
- `getMaxAllowedQuantity` considera `cards.copies_limit` y el estado `restricted`.
- `evaluate` agrupa copias por `oracle_id` (o `card_id` si falta el oracle), cuenta main
  + sideboard, ignora `maybeboard` y omite legalidad si `tracks_legality = FALSE`.
- **Comandante diferido (D-012):** `MISSING_COMMANDER` no se emite porque ni el esquema
  ni el contrato tienen un campo de comandante; declarar la regla la haría
  insatisfacible. Se implementará junto con ese campo.

### 7.2 `DeckServiceImpl` (implementado)

- `create` / `update`: sin validación de formato; fuerzan `status = draft` y limpian
  `published_at`.
- `addCard` / `removeCard`: sin bloqueo por formato; comprueban que la carta existe
  (`404`), actualizan `decks.updated_at` y devuelven el mazo a `draft` (limpian
  `published_at`).
- `findById`: carga las filas de `card_deck` para que la respuesta incluya
  `deck_contents`; antes el detalle salía sin cartas.
- `getAll` / `findByUserId` / `findByFormat`: cargan las filas de todas las páginas
  con `findByDeckIdIn` (una consulta extra por página, sin N+1) y las reparten por
  `deck_id`; antes los tableros de los listados salían vacíos porque
  `DeckEntity.cards` es `@OneToMany(mappedBy = "deck")` y el servicio inserta la
  relación solo con `deck_id`.
- `validate(id)`: delega en `evaluate()`; es de solo lectura.
- `publish(id)`: `evaluate()`; con errores lanza `DeckNotLegalException`; sin errores
  guarda `status = published` y `published_at = now()`.
- `unpublish(id)`: vuelve a `draft` y limpia `published_at`, sin validar.

### 7.3 `GlobalExceptionHandler` (implementado)

Handler `DeckNotLegalException` → `409` `DECK_NOT_LEGAL` con `issues`. `ErrorResponse`
incorpora `List<DeckIssue>` y `IssueSeverity` serializa su valor en minúsculas.

### 7.4 `DeckMapper` y respuestas de un mazo (implementado)

`DeckMapper.toDeckDTO(Deck, DeckValidationResult)` y
`toCompleteDeckDTO(Deck, DeckValidationResult)` añaden `validation_status`,
`issue_count` e `issues`; el `status` sale del dominio. `DeckController` llama a
`validate()` tras crear, actualizar, publicar y despublicar, y en el detalle.
`GET /decks` usa la versión de un solo argumento, que deja el diagnóstico a `null`,
pero el servicio sí adjunta los tableros (`main_board`, `side_board`, `maybe_board`).

## 8. Comportamiento por acción

| Acción | ¿Bloquea? | Resultado |
|---|---|---|
| `POST /decks` con cartas ilegales | No | `201`, `draft`, `validation_status=illegal` |
| `POST /decks` sin `format` | Sí | `400` validación |
| `POST /decks` con `card_id` inexistente | Sí | `404` |
| `POST /decks` con `quantity <= 0` | Sí | `400` |
| `PUT /decks/{id}` | No | `200`; si estaba publicado, vuelve a `draft` |
| `GET /decks/{id}` | No | Incluye `status`, `validation_status` e `issues` |
| `GET /decks/{id}/validation` | No | `200` siempre |
| `POST /decks/{id}/publish` con errores | Sí | `409` + `issues` |
| `POST /decks/{id}/publish` con warnings | No | `200`, `published` |
| `POST /decks/{id}/unpublish` | No | `200`, `draft` |
| `POST /decks/{id}/cards` | No | `200`; suma copias, mazo a `draft` |
| `DELETE /decks/{id}/cards/{card_id}` | No | `200`; resta copias, mazo a `draft` |
| `POST /decks/{id}/cards` con carta inexistente | Sí | `404` `CARD_NOT_FOUND` |
| `DELETE /decks/{id}` | No | `204` |

`GET /decks` deja `validation_status`, `issue_count` e `issues` en `null`; el resto de
respuestas de un solo mazo los rellenan (D-014). Los tres listados sí devuelven los
tableros de cada mazo.

## 9. Pruebas

`FlywayMigrationSmokeIntegrationTest` (obligatorio actualizar):

- `decks.status` existe con default `draft`, constraints e índice.
- `formats` incluye `Casual` con `tracks_legality = FALSE`.
- `cards.copies_limit` existe y `unlimited_copies` ya no.
- El trigger `check_card_quantity` **ya no existe**.
- Los constraints de `card_deck` siguen rechazando `quantity <= 0` y sección inválida.

Unitarias (implementadas):

- `DeckValidationServiceTest`: un caso por código, `copies_limit` 0 / N / NULL,
  recuento por `oracle_id` con dos impresiones, `maybeboard` ignorado,
  `tracks_legality = FALSE` sin avisos, `DECK_TOO_SMALL` siempre emitido.
- `DeckServiceImplTest`: `create`/`update` fuerzan `draft`, `addCard`/`removeCard`
  tocan `updated_at`, devuelven a `draft` un publicado y rechazan una carta
  inexistente, `validate` delega en `evaluate`, `publish` publica legal y
  solo con warnings, rechaza con errores sin guardar y propaga `404`,
  `unpublish` vuelve a `draft` y limpia `published_at`. Los listados adjuntan las
  cartas con una sola llamada a `findByDeckIdIn` y no consultan nada sin mazos.
- `DeckControllerTest`: los endpoints devuelven `200`, aplican `main` por defecto y
  delegan en el servicio; las respuestas de un mazo llaman a `validate` y mapean
  con el diagnóstico.
- `DeckValidationMapperTest`: snake_case (`deck_id`, `validation_status`,
  `card_id`, `deck_content_id`), enums de severidad y `section` nula o en
  minúsculas.
- `DeckMapperTest`: `status`, `validation_status`, `issue_count` e `issues` en `Deck`
  y `CompleteDeck`; la lista deja el diagnóstico a `null`.

Integración (implementadas):

- `DeckValidationIntegrationTest` ejecuta `evaluate` real: 4 en main + 1 en sideboard →
  `MAX_COPIES_EXCEEDED`; dos impresiones por `oracle_id`; baneada con `card_id` y
  `deck_content_id`; maybeboard ignorado; formato sin `tracks_legality`; mazo corto.
- El mismo test, con `MockMvc`, cubre el ciclo HTTP: `GET /validation` → `200` con
  `validation_status=illegal` e `issues`; `POST /publish` ilegal → `409`
  `DECK_NOT_LEGAL` con `issues`; publicar y despublicar un mazo legal → `200`.
- El detalle `GET /decks/{id}` incluye `status`, `validation_status`, `issue_count` e
  `issues`; `POST /decks` guarda un mazo ilegal con `201` y su diagnóstico, y responde
  `400` si falta `format`.
- `POST /decks/{id}/cards` y `DELETE /decks/{id}/cards/{card_id}` suman y restan copias
  sobre HTTP; editar un publicado lo devuelve a `draft` y una carta inexistente da
  `404 CARD_NOT_FOUND`.
- `GET /decks` devuelve los tableros de cada mazo (`main_board` con la carta y su
  cantidad), no solo las cabeceras.
- `DeckServiceH2IntegrationTest` (PostgreSQL/Testcontainers) guarda, publica y
  despublica contra la base real, y comprueba que un `publish` rechazado deja el
  mazo en `draft` sin `published_at`.

## 10. Fases

1. `V3` + entidades/dominio/mappers + smoke actualizado. **Hecha.**
2. Refactor del validador a `evaluate()`. **Hecha.**
3. Endpoints `validation`, `publish`, `unpublish` + handler 409. **Hecha.**
4. Contrato: `format` requerido, `status`, `validation_status`, `issues`,
   `copies_limit`; regenerar. **Hecha.**
5. `addCard` / `removeCard` por HTTP. **Hecha.**
6. Importación de datos (ver `docs/DATA_IMPORT.md` cuando exista).
7. Comandante: campo en esquema y contrato, después `MISSING_COMMANDER` (D-012).

## 11. Decisiones cerradas

| Decisión | Resultado |
|---|---|
| Publicación fallida | `409 Conflict` |
| `min_deck_size` | `NOT NULL`; Casual usa `0` |
| Editar publicado | Vuelve a `draft` |
| Ubicación del estado | Columna `decks.status` |
| `LEGALITY_UNKNOWN` | Permite publicar |
| Trigger de reglas | Eliminado; solo forma en la BD |
| Formato obligatorio | Sí, `format` `required` |
| Formato "for fun" | Fila `Casual`, no `NULL` |
| `unlimited_copies` | Sustituido por `copies_limit` (`NULL`/`0`/`N`) |
| Recuento de copias | Por `oracle_id` |
| `MISSING_COMMANDER` | Diferido hasta tener campo de comandante (D-012) |
| `issues` por fila | No; solo en el mazo (D-013) |
| Diagnóstico en la lista | No se calcula; `null` (D-014) |

## 12. Riesgos

- **Sin datos de legalidad**, todo mazo sale `unknown` salvo en formatos con
  `tracks_legality = FALSE`. Depende del importador.
- **El script `src/main/resources/scripts/import_cards.py`** ya escribe el esquema
  post-`V3` (`copies_limit`, `scryfall_id`, upsert idempotente); ver D-018 y
  `docs/DATA_IMPORT.md`. Sigue pendiente el rediseño de caras con `card_faces`.
- **`DELETE` de cartas desde un import** borraría filas de `card_deck` por el
  `ON DELETE CASCADE`. Un import debe marcar obsoletas, nunca borrar.
