# Modelo de validación de mazos

- **Estado:** aprobado; fases 1-5 y comandante (fase 7) implementados
- **Alcance:** estados de mazo, diagnóstico de legalidad, contrato y esquema
- **Relacionado:** ADR-007 (reemplaza ADR-002), ADR-006, `docs/SCHEMA_DESIGN.md`, D-012, D-021

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
| `MISSING_COMMANDER` | error | `requires_commander` y el mazo no declara ningún comandante |
| `TOO_MANY_COMMANDERS` | error | Más de dos filas en `deck_commander` (defensa; el contrato ya limita a 2) |
| `INVALID_COMMANDER` | error | El comandante no está en `main`, no existe, o no puede ser comandante |
| `INVALID_COMMANDER_PAIR` | error | Dos comandantes sin una mecánica de pareja válida |
| `COMMANDER_NOT_LEGAL` | error | El comandante tiene `banned` o `not_legal` en el formato |
| `COLOR_IDENTITY_VIOLATION` | error | Carta de main/sideboard fuera de la identidad de color de los comandantes |
| `COMPANION_RULE_VIOLATION` | error | `CompanionRules`, Umori, Yorion, Zirda |
| `LEGALITY_UNKNOWN` | warning | El formato rastrea legalidad pero no hay fila para esa carta |

Reglas de aplicación:

- `maybeboard` no cuenta para copias ni tamaños.
- `is_basic_land` exime del límite de copias.
- `formats.tracks_legality = FALSE` (Casual) desactiva legalidad **y**
  `LEGALITY_UNKNOWN`: no se emite ningún aviso por falta de datos.

### 4.1 Comandante

- El comandante es una carta **del mazo**: debe existir en `card_deck` con
  `section = main`. El campo solo marca la fila; no añade cartas.
- Solo se evalúa con `formats.requires_commander = TRUE`. En el resto de formatos,
  `commander_card_ids` se persiste igual pero no genera issues.
- Carta válida como comandante (`CommanderRules`): criatura legendaria, o cualquier carta
  cuyo texto diga "can be your commander" (planeswalkers legendarios, por ejemplo).
- Parejas admitidas:
  | Mecánica | Condición |
  |---|---|
  | Partner with `<nombre>` | El nombre debe coincidir en ambas direcciones |
  | Partner (sin nombre) | Ambos comandantes con `Partner` genérico |
  | Friends forever | Ambos con la keyword |
  | Choose a Background | Uno con la keyword y el otro un Background |
  | Doctor's companion | Uno con la keyword y el otro un Doctor |
- Un Background declarado solo es `INVALID_COMMANDER`: no es comandante por sí mismo.
- La identidad de color se lee de `cards.color_identity`; la unión de los comandantes
  declarados debe contener la identidad de cada carta de `main` y `sideboard`
  (`maybeboard` queda fuera). Un comandante ilegal en el formato usa
  `COMMANDER_NOT_LEGAL`, no `CARD_BANNED`/`CARD_NOT_LEGAL`.
- Recordar que `MISSING_COMMANDER` es un error de publicación: se puede guardar en
  `draft` sin comandante y verlo como aviso/error en el diagnóstico.

## 5. Contrato (`openapi.yml`)

### 5.1 Campos nuevos y cambios (implementado)

`Deck`:

| Campo | Tipo | Notas |
|---|---|---|
| `format` | `integer` | **`required`**. Sustituye al 409 por `NOT NULL` |
| `commander_card_ids` | `array<int>` | `maxItems: 2`; cartas de `deck_contents` (main) que son comandante |
| `status` | `draft` \| `published` | `readOnly` |
| `validation_status` | `legal` \| `illegal` \| `unknown` | `readOnly`, calculado; `null` en la lista |
| `issue_count` | `{errors, warnings}` | `readOnly`, calculado; `null` en la lista |
| `issues` | `array<DeckIssue>` | `readOnly`, calculado; `null` en la lista |

`CompleteDeck`: expone los mismos cuatro campos de diagnóstico que `Deck` y también
`commander_card_ids`.

`commander_card_ids` se comporta como `deck_contents`: si el campo llega ausente o
`null` en `PUT`, la selección actual se conserva; una lista vacía la limpia. En `POST` y
`PUT` los ids se deduplican y se devuelven ordenados por `card_id`.

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

### 6.5 Comandante: `V4__commander.sql`

```sql
CREATE TABLE deck_commander (
    deck_id BIGINT NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
    card_id BIGINT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    PRIMARY KEY (deck_id, card_id)
);

CREATE INDEX idx_deck_commander_card_id ON deck_commander (card_id);
```

- Tabla propia en lugar de una columna en `decks` porque las parejas declaran dos
  comandantes (D-021).
- PK `(deck_id, card_id)` impide duplicados; las FKs con `ON DELETE CASCADE` borran las
  filas al eliminar el mazo o la carta.
- El índice por `card_id` responde a "¿qué mazos usan esta carta como comandante?".
- La BD **no** exige que la carta esté en `card_deck`: es política y vive en
  `DeckValidationService`.
- Entidad `DeckCommanderEntity` con `@IdClass(DeckCommanderId.class)` y
  `DeckCommanderRepository` (`findByDeckId`, `findByDeckIdIn`, `deleteByDeckId`), igual
  que `card_deck`, para no reintroducir N+1 en los listados.

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
- **Comandante (implementado, D-021):** si `requires_commander`, lee `deck_commander`,
  emite `MISSING_COMMANDER` sin filas y valida presencia en `main`, legalidad de cada
  comandante, pareja y identidad de color. La lista se ordena por `card_id` para que el
  diagnóstico sea determinista.

### 7.1.1 `CommanderRules`

Clase utilitaria sin estado ni base de datos, para poder probar cada variante de pareja
de forma aislada:

| Método | Uso |
|---|---|
| `isCommanderOnItsOwn(card)` | Criatura legendaria o texto "can be your commander" |
| `isBackground(card)` | Enchantment con subtipo Background |
| `isDoctor(card)` | Creature con subtipo Doctor |
| `validateAlone(card)` | Mensaje de por qué no puede ser comandante, o `null` |
| `validatePair(first, second)` | Mensaje si la pareja no es legal, o `null` |
| `isPartnerWithPair` / `isGenericPartnerPair` / `isFriendsForeverPair` / `isBackgroundPair` / `isDoctorsCompanionPair` | Cada mecánica por separado |
| `colorIdentity(card)` | `cards.color_identity` ("W,U") → `Set<String>` ordenado e inmutable |

Las keywords se leen de `cards.card_keywords`; si la fila no las tiene, se cae al texto
de la carta. Los apóstrofes tipográficos (`’`) se normalizan para que "Doctor’s
companion" funcione igual que "Doctor's companion".

### 7.2 `DeckServiceImpl` (implementado)

- `create` / `update`: sin validación de formato; fuerzan `status = draft` y limpian
  `published_at`. Persisten o reemplazan `deck_commander` cuando el payload trae
  `commander_card_ids`; `null` conserva la selección y `[]` la limpia. El borrado va
  seguido de `flush()` porque la clave es asignada: sin él, el reinsert de un comandante
  conservado se fusionaría con la fila recién borrada.
- `update` **no** fusiona una entidad nueva: `DeckEntityMapper.updateEntity` copia el
  modelo sobre la entidad ya gestionada e ignora `cards`. Fusionar una `DeckEntity`
  recién construida (con la lista de cartas vacía) hacía que `orphanRemoval` borrase
  todas las filas de `card_deck` aunque el payload no tocara `deck_contents`.
- `addCard` / `removeCard`: sin bloqueo por formato; comprueban que la carta existe
  (`404`), actualizan `decks.updated_at` y devuelven el mazo a `draft` (limpian
  `published_at`).
- `findById`: carga las filas de `card_deck` para que la respuesta incluya
  `deck_contents` y las de `deck_commander` para `commander_card_ids`; antes el detalle
  salía sin cartas.
- `getAll` / `findByUserId` / `findByFormat`: cargan las filas de todas las páginas
  con `findByDeckIdIn` (una consulta extra por página, sin N+1) y las reparten por
  `deck_id`; lo mismo con los comandantes. Antes los tableros de los listados salían
  vacíos porque `DeckEntity.cards` es `@OneToMany(mappedBy = "deck")` y el servicio
  inserta la relación solo con `deck_id`.
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
| `POST`/`PUT` con `commander_card_ids` fuera de `main` | No | `200`; `INVALID_COMMANDER` en el diagnóstico |
| `POST /decks/{id}/publish` en Commander sin comandante | Sí | `409` + `MISSING_COMMANDER` |
| Publicar Commander con pareja ilegal | Sí | `409` + `INVALID_COMMANDER_PAIR` |
| Publicar Commander con carta fuera de identidad | Sí | `409` + `COLOR_IDENTITY_VIOLATION` |
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
- `deck_commander` existe con PK `(deck_id, card_id)`, rechaza duplicados y borra en
  cascada al eliminar el mazo o la carta (4 migraciones aplicadas).

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
  y `CompleteDeck`; la lista deja el diagnóstico a `null`; `commander_card_ids` va y
  vuelve en las cinco variantes.
- `CommanderRulesTest`: comandante por sí solo, Background aislado, cada variante de
  pareja (incluida la que no coincide), lectura por keyword y por texto, apóstrofes
  tipográficos, entradas nulas e identidad de color.
- `DeckValidationServiceTest`: además, `MISSING_COMMANDER`, `TOO_MANY_COMMANDERS`,
  comandante ausente del mazo o fuera de `main`, carta que no puede ser comandante,
  cada pareja válida e inválida, Background solo, Doctor no legendario,
  `COMMANDER_NOT_LEGAL`, comandante sin legalidad (`LEGALITY_UNKNOWN`) y
  `COLOR_IDENTITY_VIOLATION` en main/sideboard con maybeboard ignorado.
- `DeckServiceImplTest`: persistir comandantes al crear, reemplazarlos al actualizar,
  conservarlos si el campo es `null`, limpiarlos con `[]`, rechazar ids nulos y
  adjuntarlos en los listados con una sola consulta.
- `CompositeKeyTest`: `DeckCommanderId` por valor.

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
  despublica contra la base real, comprueba que un `publish` rechazado deja el
  mazo en `draft` sin `published_at` y recorre el ciclo de comandantes: crear con dos,
  reemplazar conservando uno, preservar al omitir el campo y limpiar con `[]`.
- `DeckValidationIntegrationTest` lee el comandante real de `deck_commander` y comprueba
  que sin fila el diagnóstico es `MISSING_COMMANDER`.

## 10. Fases

1. `V3` + entidades/dominio/mappers + smoke actualizado. **Hecha.**
2. Refactor del validador a `evaluate()`. **Hecha.**
3. Endpoints `validation`, `publish`, `unpublish` + handler 409. **Hecha.**
4. Contrato: `format` requerido, `status`, `validation_status`, `issues`,
   `copies_limit`; regenerar. **Hecha.**
5. `addCard` / `removeCard` por HTTP. **Hecha.**
6. Importación de datos (ver `docs/DATA_IMPORT.md` y el plan en
   `docs/IMPORTER_PLAN.md`, decisiones D-023–D-031).
7. Comandante: campo en esquema y contrato, `MISSING_COMMANDER`, parejas e identidad de
   color. **Hecha** (`V4`, D-021).

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
| `MISSING_COMMANDER` | Error cuando el formato exige comandante (D-012, D-021) |
| Comandante | Carta del mazo (fila en `main`), máx. 2, tabla `deck_commander` |
| Parejas | Partner with / Partner / Friends forever / Background / Doctor's companion |
| Identidad de color | Unión de comandantes; main + sideboard, `maybeboard` fuera |
| Comandante ilegal | `COMMANDER_NOT_LEGAL` en vez de `CARD_BANNED`/`CARD_NOT_LEGAL` |
| `commander_card_ids` ausente | Conserva la selección; `[]` la limpia |
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
- **Las parejas de comandante dependen de `card_keywords` y `card_text`.** Si un import
  deja la carta sin keywords, la lectura cae al texto; si también falta el texto, la
  pareja se rechaza. `V4` no puede validar esto y no debe intentarlo.
