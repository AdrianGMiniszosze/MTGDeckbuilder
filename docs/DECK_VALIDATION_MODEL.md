# Modelo de validación de mazos

- **Estado:** aprobado para implementación
- **Alcance:** estados de mazo, diagnóstico de legalidad, contrato y esquema
- **Relacionado:** ADR-007 (reemplaza ADR-002), ADR-006, `docs/SCHEMA_DESIGN.md`

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
| `DECK_TOO_SMALL` | error | Main < `min_deck_size` (solo al publicar) |
| `MISSING_COMMANDER` | error | `requires_commander` sin comandante |
| `COMPANION_RULE_VIOLATION` | error | `CompanionRules`, Umori, Yorion, Zirda |
| `LEGALITY_UNKNOWN` | warning | El formato rastrea legalidad pero no hay fila para esa carta |

Reglas de aplicación:

- `maybeboard` no cuenta para copias ni tamaños.
- `is_basic_land` exime del límite de copias.
- `formats.tracks_legality = FALSE` (Casual) desactiva legalidad **y**
  `LEGALITY_UNKNOWN`: no se emite ningún aviso por falta de datos.

## 5. Contrato (`openapi.yml`)

### 5.1 Campos nuevos y cambios

`Deck`:

| Campo | Tipo | Notas |
|---|---|---|
| `format` | `integer` | **Pasa a `required`**. Sustituye al 409 por `NOT NULL` |
| `status` | `draft` \| `published` | `readOnly` |
| `validation_status` | `legal` \| `illegal` \| `unknown` | `readOnly`, calculado |
| `issue_count` | `{errors, warnings}` | `readOnly` |

`CardDeck`: `issues` (`array<DeckIssue>`, `readOnly`).

`Card`: `unlimited_copies` → **`copies_limit`** (`integer`, nullable). Breaking, sin consumidores.

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
```

### 5.3 Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/decks/{id}/validation` | `200` con `DeckValidation` (nunca falla por ilegalidad) |
| `POST` | `/decks/{id}/publish` | `200` con `DeckDTO`; `409` `DECK_NOT_LEGAL` + `issues` si hay errores |
| `POST` | `/decks/{id}/unpublish` | `200` con `DeckDTO`, vuelve a `draft` |

### 5.4 Errores

- Formato ausente o inválido: `400` de validación (campo `format`).
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
(`DeckServiceImpl.updateDeckModificationTime`, hoy sin uso), incluyendo el caso
`DELETE` que el trigger nunca cubrió.

**Trade-off aceptado:** la BD deja de garantizar que un mazo publicado sea legal ante
escrituras que no pasen por la API. Hoy la API es el único escritor. Si aparece un
segundo escritor (importador o scripts de administración), se reevalúa.

## 7. Java

### 7.1 Refactor del validador

```java
DeckValidationResult evaluate(Long deckId);
```

- `DeckValidationResult`: `status` + `List<DeckIssue>`.
- Sin excepciones para reglas de formato; `validateCardRemoval` se mantiene (integridad).
- `getMaxAllowedQuantity` pasa a considerar `cards.copies_limit` y a sumar por `oracle_id`.

### 7.2 `DeckServiceImpl`

- `create` / `update`: sin validación de formato; `update` fuerza `status = draft`.
- `publish(id)`: `evaluate()`; errores → `DeckNotLegalException` (nueva) → 409.
- `unpublish(id)`: `draft`.
- `addCard` / `removeCard`: sin bloqueo por formato; actualizan `decks.updated_at`.
- `create`/`update` recalculan `decks.updated_at` (ya lo hacen vía `save`).

### 7.3 `GlobalExceptionHandler`

Nuevo handler `DeckNotLegalException` → `409` `DECK_NOT_LEGAL` con `issues`.

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
| `DELETE /decks/{id}` | No | `204` |

## 9. Pruebas

`FlywayMigrationSmokeIntegrationTest` (obligatorio actualizar):

- `decks.status` existe con default `draft`, constraints e índice.
- `formats` incluye `Casual` con `tracks_legality = FALSE`.
- `cards.copies_limit` existe y `unlimited_copies` ya no.
- El trigger `check_card_quantity` **ya no existe**.
- Los constraints de `card_deck` siguen rechazando `quantity <= 0` y sección inválida.

Unitarias:

- `DeckValidationServiceImplTest`: un caso por código, `copies_limit` 0 / N / NULL,
  recuento por `oracle_id` con dos impresiones, `maybeboard` ignorado,
  `tracks_legality = FALSE` sin avisos.
- `DeckServiceImplTest`: `create`/`update` no bloquean, `update` degrada a `draft`,
  `publish` propaga errores, `addCard`/`removeCard` tocan `updated_at`.

Integración:

- `POST /decks` ilegal → `201`.
- `publish` ilegal → `409` con `issues`; publicar y editar → `draft`.
- 4 en main + 1 en sideboard → `MAX_COPIES_EXCEEDED`.

## 10. Fases

1. `V3` + entidades/dominio/mappers + smoke actualizado.
2. Refactor del validador a `evaluate()`.
3. Endpoints `validation`, `publish`, `unpublish` + handler 409.
4. Contrato: `format` requerido, `status`, `validation_status`, `issues`,
   `copies_limit`; regenerar.
5. `addCard` / `removeCard` por HTTP.
6. Importación de datos (ver `docs/DATA_IMPORT.md` cuando exista).

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

## 12. Riesgos

- **Sin datos de legalidad**, todo mazo sale `unknown` salvo en formatos con
  `tracks_legality = FALSE`. Depende del importador.
- **El script `src/main/resources/scripts/import_cards.py` escribe `unlimited_copies`**
  (línea 821), que desaparece en `V3`: hay que adaptarlo o retirarlo.
- **`DELETE` de cartas desde un import** borraría filas de `card_deck` por el
  `ON DELETE CASCADE`. Un import debe marcar obsoletas, nunca borrar.
