# ARCHITECTURE_DECISIONS

Registro inicial de decisiones tecnicas (ADR lite) para `MTGDeckbuilder`.

## Formato

- **ID**: ADR-XXX
- **Estado**: Propuesta | Aceptada | Reemplazada
- **Fecha**: YYYY-MM-DD
- **Decision**: Que se decide
- **Contexto**: Problema y restricciones
- **Trade-offs**: Beneficios vs costos
- **Consecuencias**: Impacto tecnico/operativo
- **Alternativas consideradas**: Opciones descartadas

---

## ADR-001 - API-first con OpenAPI como fuente de verdad

- **Estado**: Aceptada
- **Fecha**: 2026-05-23
- **Decision**: Definir y mantener el contrato en `src/main/resources/api/openapi.yml` y generar artefactos con `scs-multiapi-maven-plugin`.
- **Contexto**: Se necesita consistencia entre consumidores API y backend, con menor deriva manual de DTOs.
- **Trade-offs**:
  - Beneficio: contrato explicito, versionable y validable.
  - Costo: disciplina de regeneracion y ajuste tras cambios en spec.
- **Consecuencias**:
  - Cambios de endpoint/modelo inician en OpenAPI.
  - Se evita editar manualmente codigo generado.
- **Alternativas consideradas**:
  - Code-first con anotaciones en controllers.
  - DTOs mantenidos manualmente sin codegen.

## ADR-002 - Reglas criticas de deck reforzadas en base de datos

- **Estado**: Aceptada
- **Fecha**: 2026-05-23
- **Decision**: Mantener validaciones de legalidad/cantidad/tamano de seccion en trigger SQL (`check_card_quantity`) sobre `card_deck`.
- **Contexto**: Reglas de negocio sensibles (banned, restricted, Commander, sideboard size) deben cumplirse incluso si falla la capa aplicacion.
- **Trade-offs**:
  - Beneficio: integridad fuerte centralizada.
  - Costo: mayor complejidad para evolucionar reglas y tests.
- **Consecuencias**:
  - La app debe permanecer alineada con trigger y constraints.
  - Pruebas de integracion se vuelven obligatorias para cambios de reglas.
- **Alternativas consideradas**:
  - Validacion solo en servicios Java.
  - Validacion parcial app + constraints minimas en DB.

## ADR-003 - PostgreSQL + pgvector para embeddings de cartas

- **Estado**: Aceptada
- **Fecha**: 2026-05-23
- **Decision**: Persistir embeddings en columna `VECTOR(1536)` y usar PostgreSQL con extension `vector`.
- **Contexto**: El dominio requiere capacidades semanticas/similitud sobre cartas.
- **Trade-offs**:
  - Beneficio: vector search cerca de los datos relacionales.
  - Costo: dependencia de extension y setup especifico de entorno.
- **Consecuencias**:
  - Entornos deben soportar pgvector.
  - Migraciones y scripts deben preservar compatibilidad de dimension.
- **Alternativas consideradas**:
  - Motor vectorial externo.
  - Sin embeddings (busqueda solo textual).

## ADR-004 - Arquitectura por capas (contract/application/infrastructure/model)

- **Estado**: Aceptada
- **Fecha**: 2026-05-23
- **Decision**: Mantener separacion estricta de responsabilidades por capa.
- **Contexto**: Se busca mantener mantenibilidad y testabilidad con crecimiento de dominio.
- **Trade-offs**:
  - Beneficio: cambios mas localizados y claros.
  - Costo: mayor numero de clases/mapeos y disciplina de diseño.
- **Consecuencias**:
  - Controllers ligeros.
  - Logica en servicios/modelo, persistencia en infraestructura.
- **Alternativas consideradas**:
  - Enfoque anemico centrado en controllers.
  - Arquitectura monolitica sin separacion clara.

## ADR-005 - Estrategia de pruebas mixta (unit + integration)

- **Estado**: Aceptada
- **Fecha**: 2026-05-23
- **Decision**: Cobertura por capa (servicios/controladores/repositorios) mas pruebas de integracion para flujos de deck.
- **Contexto**: Reglas repartidas entre Java y SQL requieren verificacion cruzada.
- **Trade-offs**:
  - Beneficio: mayor confianza en cambios de negocio.
  - Costo: tiempo de ejecucion y mantenimiento de tests.
- **Consecuencias**:
  - Cambios en reglas de mazo deben actualizar tests de integracion.
  - Fallos de contrato y de persistencia se detectan antes de merge.
- **Alternativas consideradas**:
  - Solo unit tests.
  - Solo end-to-end sin pruebas por capa.

---

## Como usar este archivo

- Agrega una nueva entrada cuando una decision cambie arquitectura, datos, contratos o estrategia de pruebas.
- Si una decision se reemplaza, marca la anterior como `Reemplazada` y referencia la nueva.
- Mantener entradas cortas y verificables en PRs.

