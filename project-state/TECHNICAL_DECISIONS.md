# Technical Decisions - Warehouse Fulfillment Platform

## Session Start Checkpoint (Initial)

**Note:** This document will be populated as technical decisions are made during implementation. Key decisions should be recorded in Architecture Decision Records (ADRs) under `docs/adr/`.

## Pending Decisions (Milestone 0)

### Flyway vs Liquibase for Schema Migrations

**Decision Point:** Choose schema migration tool.

| Option        | Pros                                                                                     | Cons                                       |
| ------------- | ---------------------------------------------------------------------------------------- | ------------------------------------------ |
| **Flyway**    | Lightweight, language-agnostic, SQL-based (matches PostgreSQL), version control friendly | Requires writing raw SQL; less type safety |
| **Liquibase** | Change log XML/YML/SQL, Java support, database agnostic                                  | More verbose; XML can be unwieldy          |

**Proposed Decision:** Flyway
**Rationale:** Project uses PostgreSQL exclusively; SQL is the native migration language; simpler to understand and debug.

---

## Session Start: Decisions Made (Pre-Implementation)

### 1. Java Version

**Decision:** Java 21 (LTS)
**Rationale:** Project specification requires Java 21; Spring Boot 3.4.x supports Java 17+; verified availability.

### 2. Maven Version

**Decision:** Maven 3.9.x
**Rationale:** Latest stable release compatible with Java 21; project verification shows 3.9.12 available.

### 3. Spring Boot Version

**Decision:** Spring Boot 3.4.3
**Rationale:** Matches project specification; supports Java 21; current LTS minor version.

### 4. Database: PostgreSQL vs MySQL/Oracle

**Decision:** PostgreSQL
**Rationale:** Project specification requires PostgreSQL; verified availability (version 18); superior JSON support, UUID handling, and open-source licensing.

### 5. Primary Key Strategy

**Decision:** UUID primary keys throughout
**Rationale:** Distributed system requirement (microservices); prevents sequence conflicts across services; database-agnostic identifier format.

### 6. Repository Pattern

**Decision:** Spring Data JPA / Hibernate
**Rationale:** Reduces persistence boilerplate; opinionated defaults match microservices needs; supports optimistic locking out of the box.

### 7. Service Communication (Initial)

**Decision:** Synchronous REST first, RabbitMQ later
**Rationale:** Simpler to debug and test initially; introduce async messaging only after synchronous workflow is stable and reliable.

### 8. API Versioning

**Decision:** URI-based versioning under `/api/v1`
**Rationale:** Clear contract evolution path; matches Spring Boot Actuator conventions; avoids query parameter clutter.

### 9. Error Handling Strategy

**Decision:** Problem Details (RFC 9457) via Spring's problem detail support
**Rationale:** Standardized error format across services; machine-parseable error codes; consistent client handling.

### 10. Security Authentication (Milestone 2+)

**Decision:** Spring Security + JWT
**Rationale:** Stateless authentication ideal for microservices; JWT widely supported; Identity Service issues tokens validated by all other services.

### 11. Containerization Strategy

**Decision:** Docker first, Kubernetes later
**Rationale:** Simpler to set up local development (Milestone 2); Kubernetes added in Milestone 6 when multi-service deployment is required.

### 12. CI/CD: Jenkins Declarative Pipeline

**Decision:** Declarative Pipeline (`pipeline {}`) over Scripted initially
**Rationale:** More readable; easier for team members to understand and modify; can evolve to Shared Library when reuse justifies it (Milestone 6).

### 13. Testing Framework

**Decision:** JUnit 5 + Mockito
**Rationale:** Modern annotation-based API; excellent ecosystem support; Mockito integrates well with Spring Boot tests.

### 14. Integration Testing: Testcontainers

**Decision:** Testcontainers with PostgreSQL for integration tests
**Rationale:** Eliminates "works on my machine" problems; tests against real PostgreSQL behavior, not H2 in-memory database; matches production environment.

---

## Pending Architectural Decisions

### 15. Flyway vs Liquibase

**Status:** Proposed: Flyway (see above)

### 16. Container Registry for Images

**Status:** Pending - To decide between:

- Docker Hub (simple, public)
- GitHub Container Registry (private, integrated with GH)
- Private registry (for production deployment)

### 17. Monitoring Stack

**Status:** Deferred to Milestone 5/6

- Prometheus + Grafana?
- Micrometer + OpenTelemetry?
- Actuator metrics only initially?

---

## Last Updated

2026-11-05

**Note:** For detailed architectural decisions, refer to `docs/adr/` directory where ADRs are created during implementation.
