# ADR 001: Catalog Service as First Vertical Slice

**Status:** Accepted
**Decided:** 2026-11-05
**Authors:** Development Team

---

## Context

Following the microservices architecture defined in ARCHITECTURE.md and DEVELOPMENT_ROADMAP.md, we need to implement the first service to establish patterns for subsequent services.

## Decision

The **Catalog Service** is selected as the first vertical slice implementation because:

1. **Low coupling:** Owns product data only; no cross-service foreign keys needed
2. **Clear boundaries:** Simple domain model (Product entity) with well-defined API contract
3. **Foundation for other services:** Inventory, Order, and Fulfillment services will reference this catalog
4. **MVP scope:** Demonstrates complete microservice pattern (entity → repository → controller → tests → migrations)

## Consequences

### Architecture Implications

- Catalog service runs on its own PostgreSQL database instance
- Other services (Inventory, Order, etc.) communicate via REST API calls
- No shared database schemas between services

### Implementation Implications

- Establish patterns for:
  - UUID-based primary keys
  - Flyway schema migrations
  - OpenAPI documentation
  - Unit + integration testing strategy
- Create baseline quality gates (code coverage, build verification)

### Future Work

Once Catalog Service patterns are stable and verified:

1. Implement Inventory Service (same team, reusing catalog patterns)
2. Implement Order Service
3. Introduce async messaging (RabbitMQ) for inventory reservation events

---

**References:**

- ARCHITECTURE.md: Database ownership boundaries
- DEVELOPMENT_ROADMAP.md: Milestone 1 details
- API_CONVENTIONS.md: REST contract standards
