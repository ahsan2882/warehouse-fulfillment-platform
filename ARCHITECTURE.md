# Warehouse Fulfillment Platform — Architecture

**Status:** Architecture baseline
**Version:** 1.0
**Runtime:** Java 21 · Spring Boot · Maven · PostgreSQL

## 1. Architectural goals

The platform manages a warehouse order lifecycle: product definition, stock receipt and reservation, order allocation, picking, packing, and shipment dispatch.

The architecture must provide:

- Clear domain and data ownership.
- Independently buildable and deployable services.
- Explicit, versioned service contracts.
- Reliable inventory and order state transitions.
- A gradual path from synchronous REST to asynchronous messaging.
- A CI/CD workflow that demonstrates meaningful Jenkins capabilities.

This document defines the target architecture. It does not require every target component to be implemented in the first milestone.

## 2. High-level architecture

```text
                    External Clients
                           |
                           v
                    API Gateway
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
     Catalog API      Order API       Identity API
          |                |
          v                v
     Catalog DB       Order DB
                           |
                     REST calls
                           |
                  +--------+--------+
                  |                 |
                  v                 v
             Inventory API    Fulfillment API
                  |                 |
                  v                 v
             Inventory DB     Fulfillment DB
                                    |
                                    v
                               Shipping API
                                    |
                                    v
                                Shipping DB

       Later: RabbitMQ for selected domain events
```

For local development, services may share one PostgreSQL server while retaining separate databases or schemas and separate database users. A shared PostgreSQL server does not imply shared data ownership.

## 3. Services and responsibilities

| Service             | Responsibilities                                                 | Owns                            |
| ------------------- | ---------------------------------------------------------------- | ------------------------------- |
| API Gateway         | External routing and later cross-cutting request policies        | No business data                |
| Identity Service    | Authentication, users, roles, token issuance                     | Users, roles, assignments       |
| Catalog Service     | Product/SKU management                                           | Products and product attributes |
| Inventory Service   | Locations, stock, receipts, adjustments, reservations, movements | Inventory records               |
| Order Service       | Order creation, order lines, cancellation, order lifecycle       | Orders and order history        |
| Fulfillment Service | Task assignment, picking, packing, exceptions                    | Fulfillment tasks and packages  |
| Shipping Service    | Shipment creation, dispatch, tracking, delivery status           | Shipments and shipment history  |

### 3.1 Service boundary rules

1. A service is the sole writer of its database.
2. Other services interact through documented APIs or events, never direct SQL.
3. Cross-service identifiers are stored as scalar IDs, not cross-database JPA relationships.
4. Public API DTOs are not JPA entities.
5. Domain rules live in the service that owns the domain.
6. Do not introduce a shared domain module that couples service internals.
7. A service must remain independently buildable and testable.

## 4. Repository and build architecture

Use a Maven multi-module monorepo. Each service is a separate Spring Boot application and produces its own artifact.

```text
warehouse-fulfillment/
├── pom.xml
├── README.md
├── PROJECT_SPEC.md
├── ARCHITECTURE.md
├── DATABASE_DESIGN.md
├── API_CONVENTIONS.md
├── DEVELOPMENT_ROADMAP.md
├── JENKINS_CICD_PLAN.md
├── AGENTS.md
├── docs/
│   └── adr/
├── services/
│   ├── catalog-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── fulfillment-service/
│   ├── shipping-service/
│   └── identity-service/
├── infrastructure/
│   ├── docker/
│   └── jenkins/
└── scripts/
```

Only create the first three service modules when implementing the early milestones. Add later modules when their milestone begins.

The root POM manages Java release, Spring Boot dependency management, plugin versions, and common build conventions. Service POMs declare only service-specific dependencies.

Example commands:

```bash
# Build all modules
mvn clean verify

# Build one service and required reactor dependencies
mvn -pl services/inventory-service -am clean verify

# Package one service
mvn -pl services/catalog-service -am package
```

## 5. Internal service architecture

Use a clean, dependency-directed structure:

```text
Inbound adapters (REST controllers)
              |
              v
Application layer (use cases and orchestration)
              |
              v
Domain layer (business rules and models)

Infrastructure adapters (JPA, HTTP clients, messaging)
implement ports required by the application.
```

Suggested package structure:

```text
com.example.warehouse.inventory
├── domain/
│   ├── model/
│   ├── exception/
│   └── repository/
├── application/
│   ├── port/
│   ├── service/
│   └── dto/
├── infrastructure/
│   ├── persistence/
│   └── client/
└── presentation/
    └── rest/
        ├── controller/
        ├── request/
        └── response/
```

Controllers should translate HTTP requests and responses. Application services coordinate use cases. Domain objects enforce invariants. Infrastructure adapters implement persistence and remote communication.

Use constructor injection. Introduce interfaces at meaningful boundaries rather than creating one for every class.

## 6. Initial synchronous workflows

### 6.1 Order creation and allocation

1. Client submits an order to Order Service.
2. Order Service validates the request and obtains product information from Catalog Service.
3. Order Service persists the order as `PENDING`.
4. Order Service requests an all-or-nothing reservation from Inventory Service.
5. Inventory Service atomically reserves stock and returns a reservation reference.
6. Order Service records allocation and advances the order state.
7. If reservation fails, Order Service records a suitable failure or `ON_HOLD` state.

A timeout is an ambiguous result: the downstream operation may have succeeded even if the response was lost. Use idempotency keys and reservation references so retries do not create duplicate reservations.

### 6.2 Picking, packing, and shipping

1. Fulfillment Service creates tasks for an allocated order.
2. An operator records picked quantities and exceptions.
3. Fulfillment Service validates and completes packing.
4. Shipping Service creates a shipment for a packed order.
5. Shipping Service records dispatch and subsequent status updates.
6. Order status is updated through an explicit service contract.

These are synchronous REST interactions initially. Avoid distributed transactions.

## 7. Resilience and consistency

- Apply connection and response timeouts to every remote HTTP call.
- Use bounded retries only for operations that are safe to retry.
- Use idempotency keys for order creation, reservation, and other non-repeatable commands.
- Do not interpret a timeout as proof that a command failed.
- Use optimistic locking or row-level locking for stock updates.
- Use transactions only within a service's own database.
- Record correlation IDs across service calls.
- Return a documented unavailable/dependency error when a required downstream service cannot be reached.
- Do not add a distributed transaction coordinator.

For workflows that span multiple services, use explicit orchestration first. Introduce saga compensation only when a real multi-step failure scenario requires it.

## 8. Future asynchronous communication

Introduce RabbitMQ after the REST workflow is stable.

Candidate events:

- `OrderCreated`
- `InventoryReserved`
- `InventoryReservationFailed`
- `OrderAllocated`
- `PickingCompleted`
- `PackingCompleted`
- `ShipmentDispatched`
- `ShipmentDelivered`
- `OrderCancelled`

An event envelope should contain:

```json
{
  "eventId": "uuid",
  "eventType": "InventoryReserved",
  "eventVersion": 1,
  "occurredAt": "2026-11-05T09:00:00Z",
  "aggregateId": "uuid",
  "correlationId": "uuid",
  "payload": {}
}
```

Consumers must be idempotent. Plan for duplicate delivery, retry limits, dead-letter queues, and eventual consistency. Consider the transactional outbox pattern when a database update and event publication must remain consistent.

## 9. Security architecture

- Identity Service authenticates users and issues signed JWT access tokens.
- Services validate token signature, issuer, expiry, and relevant claims.
- Services enforce authorization at their own boundaries.
- API Gateway may apply coarse routing policies but is not the only authorization layer.
- Database users are scoped to their service's database/schema.
- Secrets are externalized and never committed to Git.
- Production traffic uses HTTPS.

Initial roles: `ADMIN`, `INVENTORY_OPERATOR`, `FULFILLMENT_OPERATOR`, `SHIPPING_OPERATOR`, `ORDER_MANAGER`, and `READ_ONLY`.

## 10. Architecture decisions to record

Use Architecture Decision Records under `docs/adr/`. Each ADR should state context, decision, alternatives, consequences, and status.

Initial decisions:

| Decision                 | Baseline                                     |
| ------------------------ | -------------------------------------------- |
| Application architecture | Microservices                                |
| Repository               | Maven multi-module monorepo                  |
| Service communication    | REST initially                               |
| Persistence              | PostgreSQL, service-owned database/schema    |
| ORM                      | Spring Data JPA / Hibernate                  |
| Security                 | Spring Security + JWT                        |
| Messaging                | RabbitMQ later                               |
| Gateway                  | Spring Cloud Gateway later                   |
| Schema migrations        | Flyway or Liquibase; choose one consistently |
| Testing                  | JUnit 5, Mockito, Testcontainers             |
| Containerization         | Docker, introduced incrementally             |
| CI/CD                    | Jenkins Declarative Pipeline                 |

## 11. Explicitly deferred

Do not introduce these in the first vertical slice:

- RabbitMQ
- API Gateway
- Kubernetes
- Multi-warehouse allocation
- External carrier integrations
- Payment processing
- Advanced picking optimization
- Distributed tracing stack
- Full production cloud deployment

Add each only when a milestone has a concrete requirement for it.
