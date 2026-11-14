# Modelo de validación de mazos

- **Estado:** propuesta para aprobación
- **Alcance:** estados de mazo, diagnóstico de legalidad, contrato y trigger
- **Relacionado:** ADR-002 (reemplazada por ADR-007), ADR-006, `docs/SCHEMA_DESIGN.md`

## 1. Problema

Hoy la legalidad se comprueba en dos sitios que actúan como **veto**:

- `DeckValidationServiceImpl` (existente pero **sin cablear**: `DeckServiceImpl` no lo inyecta).
- Trigger `check_card_quantity` sobre `card_deck` (`V1__baseline_schema.sql:344`).

Ese veto es correcto para un mazo que va a jugarse, pero hostil durante la construcción:
el usuario no puede guardar un borrador incompleto y, cuando falla, recibe un único
mensaje sin saber a qué carta corresponde.

## 2. Principios

1. **Guardar es libre; publicar es estricto.** Un mazo en construcción puede estar
   incompleto o ser ilegal. Un mazo publicado no.
2. **La integridad nunca se negocia.** `quantity > 0`, sección válida, carta y mazo
   existentes, FKs. Eso no es una regla de formato y siempre bloquea.
3. **El diagnóstico se calcula, no se lanza.** Las reglas de formato producen una
   lista de problemas, no una excepción que aborte la escritura.
4. **La API identifica la carta culpable.** Cada problema lleva `card_id` y
   `deck_content_id` para que la interfaz coloree filas sin parsear mensajes.
5. **Una sola definición de cada regla.** Java decide y el trigger solo replica lo
   necesario para proteger mazos publicados.

## 3. Estados del mazo

| `decks.status` | Significado | Reglas de formato |
|---|---|---|
| `draft` | En construcción | No se aplican (solo integridad) |
| `published` | Listo para jugar | Se aplican en Java **y** en el trigger |

- Valor por defecto: `draft`.
- `published_at` (`TIMESTAMPTZ`, nullable) registra cuándo se publicó.
- Editar un mazo publicado lo devuelve a `draft` y limpia `published_at`. Fingir que
  sigue publicado sería mentir al usuario.
- Despublicar (`unpublish`) vuelve a `draft` sin validar nada.

## 4. Códigos de problema

`severity = error` bloquea la publicación. `severity = warning` solo informa.

| Código | Sev. | Cuándo |
|---|---|---|
| `CARD_BANNED` | error | `card_legality.legality_status = 'banned'` |
| `CARD_NOT_LEGAL` | error | `legality_status = 'not_legal'` |
| `MAX_COPIES_EXCEEDED` | error | Copias de la carta (main + sideboard) > `max_copies` |
| `SINGLETON_VIOLATION` | error | Formato singleton y más de 1 copia |
| `SIDEBOARD_TOO_LARGE` | error | Suma de sideboard > `max_sideboard_size` |
| `DECK_TOO_LARGE` | error | Suma de main > `max_deck_size` |
| `DECK_TOO_SMALL` | error | Suma de main < `min_deck_size` (solo al publicar) |
| `MISSING_COMMANDER` | error | `requires_commander` y no hay comandante |
| `COMPANION_RULE_VIOLATION` | error | `CompanionRules` / Umori / Yorion / Zirda |
| `LEGALITY_UNKNOWN` | warning | No hay fila en `card_legality` para esa carta y formato |

`LEGALITY_UNKNOWN` es deliberadamente un aviso: tras un import parcial de Scryfall no
existen filas de legalidad, y convertir eso en error dejaría todos los mazos ilegales.

`maybeboard` queda fuera de todo esto: es zona de borrador, no cuenta para copias ni
tamaños (igual que hoy en el trigger).

## 5. Contrato (cambios en `openapi.yml`)

### 5.1 Campos nuevos

`Deck`:

| Campo | Tipo | Notas |
|---|---|---|
| `status` | `draft` \| `published` | `readOnly` |
| `validation_status` | `legal` \| `illegal` \| `unknown` | `readOnly`, calculado |
| `issue_count` | objeto `{errors, warnings}` | `readOnly` |

`CardDeck`: `issues` (`array<DeckIssue>`, `readOnly`) con los problemas de esa fila.

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
    issues:
      type: array
      items: { $ref: '#/components/schemas/DeckIssue' }
```

### 5.3 Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| `GET` | `/decks/{id}/validation` | `200` con `DeckValidation` (nunca falla por ilegalidad) |
| `POST` | `/decks/{id}/publish` | `200` con `DeckDTO` si `validation_status != illegal`; `409` con `issues` si hay errores |
| `POST` | `/decks/{id}/unpublish` | `200` con `DeckDTO`, vuelve a `draft` |

Regla de publicación: se permite publicar con `unknown` (solo avisos) y se rechaza con
cualquier `error`.

`POST`/`PUT`/`GET /decks` no cambian de código: siempre `201`/`200` salvo integridad.

### 5.4 Errores

- Integridad: `400` `INVALID_DECK_COMPOSITION` (o `404` `CARD_NOT_FOUND` /
  `DECK_NOT_FOUND` / `FORMAT_NOT_FOUND` cuando el recurso no existe).
- Publicación fallida: `409` `DECK_NOT_LEGAL`, con `issues` en el cuerpo.

## 6. Persistencia

### 6.1 Migración `V3__add_deck_status.sql`

```sql
ALTER TABLE decks ADD COLUMN status TEXT NOT NULL DEFAULT 'draft';
ALTER TABLE decks ADD COLUMN published_at TIMESTAMPTZ NULL;

ALTER TABLE decks
    ADD CONSTRAINT decks_status_check
    CHECK (status IN ('draft', 'published'));

ALTER TABLE decks
    ADD CONSTRAINT decks_published_at_check
    CHECK ((status = 'published') = (published_at IS NOT NULL));

CREATE INDEX idx_decks_status ON decks (status);
```

Nota: `published_at` en la misma migración que `status` evita un `V4` de un solo campo.

### 6.2 Entidades

- `DeckEntity`: `status`, `publishedAt` (mapeados a `status`, `published_at`).
- Modelo `Deck`: `status` como enum (`Draft`, `Published`) para no repetir strings.
- `DeckMapper` (contract) y `DeckEntityMapper` (infrastructure) traducen enum ↔ valor BD,
  igual que se hizo con `section`.

## 7. Trigger `check_card_quantity`

Se mantiene el trigger, pero se separa lo que protege de lo que informa:

**Siempre (cualquier estado):**

- `quantity > 0`.
- `section IN ('main','sideboard','maybeboard')`.
- `decks.id` existe; `cards.id` existe.
- `UPDATE decks SET updated_at = now()`.
- `maybeboard` no aplica reglas.

**Solo si `decks.status = 'published'`:**

- Legalidad (`banned`, `not_legal`).
- Copias máximas / singleton / restricción.
- `max_sideboard_size` y `max_deck_size`.

Consecuencia: la BD deja de garantizar que un borrador sea legal, y pasa a garantizar
que **lo publicado lo sea**. La validación Java sigue siendo la que da el mensaje bueno;
el trigger es la red de seguridad ante escrituras que no pasen por la API.

## 8. Java

### 8.1 Refactor de `DeckValidationService`

Deja de lanzar excepciones para reglas de formato:

```java
DeckValidationResult evaluate(Long deckId);
```

- `DeckValidationResult`: `status` (`legal`/`illegal`/`unknown`) + `List<DeckIssue>`.
- Mantiene métodos de cálculo reutilizables (`getMaxAllowedQuantity`, `getMaxDeckSize`).
- `validateCardRemoval` se queda: es integridad, no formato.

### 8.2 `DeckServiceImpl`

- `create` / `update`: sin validación de formato. Siguen normalizando (merge de
  duplicados) y aplicando integridad. `update` fuerza `status = draft`.
- `publish(id)`: `evaluate(id)`; si hay errores → `DeckNotLegalException` (nueva) con
  la lista; si no, `status = published`, `published_at = now()`.
- `unpublish(id)`: `status = draft`, `published_at = null`.
- `addCard` / `removeCard`: sin bloqueo por formato.

### 8.3 `GlobalExceptionHandler`

Nuevo handler `DeckNotLegalException` → `409` `DECK_NOT_LEGAL` incluyendo `issues`.

## 9. Comportamiento por acción

| Acción | ¿Bloquea? | Resultado |
|---|---|---|
| `POST /decks` con cartas ilegales | No | `201`, `status=draft`, `validation_status=illegal` |
| `PUT /decks/{id}` | No | `200`; si estaba publicado, vuelve a `draft` |
| `POST /decks` con `card_id` inexistente | Sí | `404` `CARD_NOT_FOUND` |
| `POST /decks` con `quantity <= 0` | Sí | `400` `INVALID_DECK_COMPOSITION` |
| `GET /decks/{id}` | No | Incluye issues por carta y a nivel de mazo |
| `GET /decks/{id}/validation` | No | `200` siempre |
| `POST /decks/{id}/publish` con errores | Sí | `409` `DECK_NOT_LEGAL` + `issues` |
| `POST /decks/{id}/publish` con avisos | No | `200`, `status=published` |
| `POST /decks/{id}/unpublish` | No | `200`, `status=draft` |
| `DELETE /decks/{id}` | No | `204` |

Regla mental para el usuario: **"puedo guardar lo que quiera, no puedo publicar algo
roto sin saber por qué"**.

## 10. Pruebas

`FlywayMigrationSmokeIntegrationTest` (obligatorio actualizar):

- El trigger debe rechazar carta baneada **solo con `decks.status = 'published'`**.
- Debe aceptarla con `status = 'draft'`.
- Debe seguir rechazando `quantity <= 0` y sección inválida en ambos estados.
- Debe existir `decks.status` con default `draft` y sus dos constraints.

Unitarias:

- `DeckValidationServiceImplTest`: un caso por código de issue, incluido
  `LEGALITY_UNKNOWN` como warning y `maybeboard` ignorado.
- `DeckServiceImplTest`: `create`/`update` no bloquean por formato; `update` degrada a
  `draft`; `publish` delega en el validador y propaga errores.

Integración:

- `POST /decks` ilegal → `201`.
- `POST /decks/{id}/publish` ilegal → `409` con `issues`.
- Publicar, editar y comprobar que vuelve a `draft`.
- Suma cross-section (4 main + 1 sideboard) detectada como `MAX_COPIES_EXCEEDED`.

## 11. Fases

1. `V3__add_deck_status.sql` + entidad/modelo/mappers + smoke actualizado.
2. Refactor del validador a `evaluate()` sin excepciones.
3. Endpoints `validation`, `publish`, `unpublish` + handler `409`.
4. Campos `status` / `validation_status` / `issues` en el contrato y regeneración.
5. `addCard` / `removeCard` por HTTP (hoy solo existen en el servicio).

## 12. Decisiones abiertas

- **Código de publicación fallida:** `409 Conflict` (propuesto) vs `422 Unprocessable`.
- **`min_deck_size`:** ¿se exige al publicar siempre, o solo en formatos que lo definan?
  En `V1` está como nullable por formato; propuesta: si es `NULL`, no se exige.
- **Editar publicado:** vuelve a `draft` (propuesto) vs bloquear y obligar a duplicar.
- **Ubicación de `status`:** columna en `decks` (propuesto) vs tabla `deck_status` aparte.
