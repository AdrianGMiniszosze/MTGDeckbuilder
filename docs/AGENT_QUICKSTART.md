# AGENT_QUICKSTART

Guia rapida para que un agente empiece a trabajar en `MTGDeckbuilder` en 5-10 minutos.

## 1) Objetivo

Entrar al repo, ubicar las capas clave, ejecutar lo minimo y aplicar cambios sin romper contrato API ni reglas de deck.

## 2) Donde mirar primero (orden)

1. `docs/TECHNICAL_AGENT_CONTEXT.md`
2. `src/main/resources/api/openapi.yml`
3. `src/main/resources/scripts/01-create-schema.sql`
4. `infra/docker-compose.yml`
5. `src/main/java/com/deckbuilder/mtgdeckbuilder/application/`
6. `src/main/java/com/deckbuilder/mtgdeckbuilder/contract/`

## 3) Mapa mental rapido

- `contract/`: endpoints REST.
- `application/`: casos de uso y reglas de negocio.
- `infrastructure/`: repositorios y persistencia.
- `model/`: entidades y estructuras de dominio.

Regla de oro: mantener logica de negocio en `application`/`model`, no en controllers.

## 4) Comandos minimos

Usa comandos genericos de Maven para mantener la guia independiente del sistema operativo.

```bash
# Desde la raiz del repo
mvn -q -DskipTests compile
mvn test
```

Si necesitas regenerar codigo por cambios en OpenAPI:

```bash
mvn generate-sources
```

Infra local (si aplica para tu tarea):

```bash
cd infra
docker compose up -d
cd ..
```

## 5) Reglas criticas que no puedes ignorar

- El contrato vive en `src/main/resources/api/openapi.yml`.
- Hay validacion en SQL trigger (`check_card_quantity`) sobre `card_deck`.
- `decks.last_modification` debe mantenerse correcto al modificar mazos.
- `card_deck.section` y `decks.deck_type` usan `main|sideboard|maybeboard`.
- No editar codigo generado en `target/generated-sources/`.

## 6) Flujo seguro para cambios

1. Ubica endpoint en `contract`.
2. Traza el caso de uso a `application`.
3. Revisa repositorios afectados en `infrastructure`.
4. Verifica impacto en SQL constraints/trigger.
5. Ajusta pruebas unitarias e integracion.
6. Si tocaste OpenAPI, regenera fuentes y valida build.

## 7) Checklist de salida (antes de cerrar tarea)

- [ ] Contrato e implementacion estan alineados.
- [ ] Tests relevantes pasan.
- [ ] No hay cambios manuales en codigo generado.
- [ ] Reglas de legalidad/cantidad siguen consistentes entre Java y SQL.
- [ ] Cambios documentados (si afectan arquitectura, actualizar ADRs).

## 8) Trampas frecuentes

- Inconsistencia de nombres de paginacion (`pageSize/pageNumber` vs `pagesize/pagenumber`).
- Cambiar reglas de deck en Java sin revisar trigger SQL.
- Editar scripts SQL sin considerar diferencia entre schema base y enhanced.

## 9) Si tienes 2 minutos extra

- Deja una nota corta en PR: endpoint afectado, tablas impactadas, riesgo principal y test agregado.
- Si el cambio es estructural, agrega entrada en `docs/ARCHITECTURE_DECISIONS.md`.



