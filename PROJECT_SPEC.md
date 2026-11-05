# Warehouse Fulfillment Platform --- Project Specification

**Document:** `PROJECT_SPEC.md`\
**Status:** Initial architecture and development baseline\
**Version:** 1.0\
**Language:** Java 21\
**Primary framework:** Spring Boot\
**Build system:** Maven\
**Database:** PostgreSQL

---

## 1. Project Overview

The Warehouse Fulfillment Platform is a backend system for managing the
journey of a customer order from order placement through inventory
allocation, warehouse picking, packing, and shipment.

The platform models the operational workflow of a warehouse. It should
make it possible to determine:

- Which products are stored in the warehouse and where they are
  located.
- How much stock is physically available, reserved, or unavailable.
- Which customer orders need to be fulfilled.
- Which items warehouse staff need to pick and pack.
- Whether an order is ready to ship and its shipment status.
- What happened to an order, inventory item, or fulfillment task over
  time.

The project is intended to be a realistic, portfolio-quality
microservices application and a practical learning environment for Java,
Spring Boot, Maven, automated testing, Docker, and Jenkins.

### 1.1 Scope boundaries

The initial system is a single-warehouse platform. The domain model
should not make multiple warehouses impossible, but multi-warehouse
allocation, carrier integrations, payment processing, customer
storefronts, and advanced warehouse optimization are outside the first
milestone.

The application is initially a backend REST platform. A frontend is not
required for the first development milestones; APIs can be exercised
with `curl`, Postman, or another HTTP client.

### 1.2 Primary actors

---

Actor Responsibilities

---

Warehouse administrator Manages warehouse configuration,
users, and operational settings

Inventory operator Receives stock, adjusts quantities,
and investigates discrepancies

Fulfillment operator Views assigned work and performs
picking and packing

Shipping operator Creates shipments, records
dispatch, and updates delivery
status

Customer/service client Places orders and checks order
status

System administrator Manages technical access,
configuration, and deployments

---

---

## 2. Business Requirements

### 2.1 Product and warehouse configuration

The platform must support:

- Creating, viewing, updating, and deactivating products.
- A unique SKU for each product.
- Product name, description, unit of measure, and active status.
- Warehouse locations such as zones, aisles, racks, shelves, and bins.
- Assigning inventory to a warehouse location.
- Preventing the deletion of products or locations that are referenced
  by operational records; use deactivation where appropriate.

**Not in the first milestone:** product variants, serial-number
tracking, lot/batch tracking, expiry-date management, and multiple
warehouses.

### 2.2 Inventory management

The platform must support:

- Receiving stock into a warehouse location.
- Recording inventory adjustments with a reason.
- Viewing on-hand, reserved, and available quantities.
- Reserving stock for an order.
- Releasing a reservation when an order is cancelled or allocation
  fails.
- Preventing reservations that would make available stock negative.
- Recording inventory movements for traceability.
- Viewing inventory by SKU and location.

Use the following quantity definitions:

- **On-hand:** physical quantity recorded at a location.
- **Reserved:** quantity committed to active orders.
- **Available:** `on-hand - reserved`.

A reservation is not a physical stock movement. Picking moves stock from
the available warehouse location into a picked/packed operational state
according to the inventory model adopted by the Inventory Service.

### 2.3 Order management

The platform must support:

- Creating an order containing one or more order lines.
- Validating that requested products exist and are active.
- Storing SKU and product-description snapshots on order lines so
  historical orders remain understandable if product details change.
- Viewing an order and its status.
- Cancelling an order when its current state allows cancellation.
- Tracking order status changes and timestamps.

Suggested order states:

`PENDING → CONFIRMED → ALLOCATED → FULFILLING → READY_TO_SHIP → SHIPPED → COMPLETED`

Exceptional states include `CANCELLED` and `ON_HOLD`. Define and enforce
valid transitions; do not allow arbitrary status updates.

### 2.4 Fulfillment operations

The platform must support:

- Creating fulfillment work for an allocated order.
- Creating pick tasks for the order lines.
- Assigning work to a fulfillment operator.
- Recording picked quantities.
- Recording short picks and exceptions.
- Completing picking before packing.
- Recording packed items and package details.
- Marking an order ready to ship only after fulfillment validation
  succeeds.

The initial implementation can use a simple picking strategy. Wave
picking, batch picking, route optimization, and barcode scanning are
future enhancements.

### 2.5 Shipping

The platform must support:

- Creating a shipment for a packed order.
- Recording a carrier name and tracking reference.
- Recording dispatch time.
- Updating shipment status.
- Viewing shipment details and status history.

Suggested shipment states:

`CREATED → DISPATCHED → IN_TRANSIT → DELIVERED`

Exceptional states include `FAILED_DELIVERY`, `RETURNED`, and
`CANCELLED`.

The first implementation records shipment information; it does not
integrate with an external carrier.

### 2.6 Auditability

The platform must retain operational history for important actions,
including:

- Stock receipts and adjustments.
- Reservations and releases.
- Order status transitions.
- Pick and pack completion.
- Shipment status changes.

Audit records should identify the action, entity, timestamp, actor where
available, and relevant before/after values or a structured description.
Do not rely only on application log messages as the business audit
trail.

### 2.7 Business invariants

These rules must be enforced by the owning service:

1. SKU values are unique.
2. Inventory quantities cannot become negative.
3. Reserved quantity cannot exceed on-hand quantity.
4. An order cannot be allocated unless all required lines can be
   reserved.
5. A shipment cannot be dispatched before its order is ready to ship.
6. A completed pick task cannot be completed again.
7. An order cannot transition to an invalid or terminally incompatible
   state.
8. A service must not directly modify another service's database.
9. Retried requests must not accidentally create duplicate orders,
   reservations, or shipments where idempotency is required.

---

## 3. Technology Stack

The following is the target technology stack. It is an architectural
direction, not a requirement to introduce every component in the first
milestone.

Concern Technology Introduction

---

Language Java 21 Milestone 1
Application framework Spring Boot Milestone 1
Build and dependency management Maven Milestone 1
Persistence Spring Data JPA / Hibernate Milestone 1
Database PostgreSQL Milestone 1
Validation Jakarta Bean Validation Milestone 1
Security Spring Security + JWT Milestone 2
Unit testing JUnit 5, Mockito Milestone 1
Integration testing Testcontainers Milestone 2
API documentation OpenAPI / springdoc Milestone 1
API gateway Spring Cloud Gateway Later milestone
Messaging RabbitMQ Later milestone
Containerization Docker Milestone 2
CI/CD Jenkins Milestone 1 pipeline; expand incrementally
Logging SLF4J with structured logging Milestone 1
Observability Actuator; metrics/tracing later Incremental

Use compatible Spring Boot and dependency versions selected at project
initialization. Pin versions through the Maven parent/dependency
management rather than allowing individual services to drift.

---

## 4. Application Architecture

### 4.1 Architectural style

Use microservices with independently deployable applications and clear
domain ownership.

The initial repository is a **Maven multi-module monorepo**. A shared
repository does not mean the services are one deployable application:
each service must have its own application entry point, configuration,
database boundary, tests, and buildable artifact.

Initial communication is synchronous REST. RabbitMQ is reserved for a
later milestone when the domain workflows and service boundaries are
stable.

### 4.2 Target service landscape

---

Service Responsibility Owns

---

API Gateway External entry point, No business data
routing, and later
cross-cutting request
policies

Identity Service User identity, Users, roles, user-role
credential assignments
verification, token
issuance, and role
assignment

Catalog Service Product and SKU Products, product
information attributes

Inventory Service Stock balances, Locations, stock
warehouse locations, balances, reservations,
receipts, adjustments, inventory movements
reservations, and
movements

Order Service Order creation, order Orders, order lines,
lines, cancellation, order status history
and order lifecycle

Fulfillment Service Picking, packing, Fulfillment tasks, pick
operator assignments, records, packages
and fulfillment
exceptions

Shipping Service Shipment creation, Shipments, shipment
dispatch, tracking status history
references, and
shipment status

---

The service list describes the intended target architecture. Do not
scaffold all services before the first workflow is understood. Build the
smallest useful vertical slice first.

### 4.3 Service responsibility rules

- A service owns its domain rules and persistence.
- A service exposes business operations through its API; it must not
  expose its JPA entities as public contracts.
- Other services may store identifiers and immutable snapshots from
  another service, but must not write to its tables.
- Cross-service workflows are coordinated through application services
  and explicit API contracts.
- Avoid a generic `CommonService` or shared domain module that causes
  services to depend on each other's internal models.
- Shared code should be limited to genuinely technical concerns, such
  as build conventions or carefully versioned API utilities. Prefer
  duplication of a small DTO over tight coupling between domain
  models.

### 4.4 Initial REST workflows

**Order allocation workflow:**

1. A client submits an order to the Order Service.
2. Order Service validates the request and obtains required product
   information from Catalog Service.
3. Order Service stores the order as `PENDING`.
4. Order Service requests an all-or-nothing stock reservation from
   Inventory Service.
5. If reservation succeeds, Order Service records the allocation and
   advances the order state.
6. If reservation fails, Order Service leaves the order unallocated and
   records a suitable failure or `ON_HOLD` state.
7. The client can retrieve the order and its current status.

The first version may use a synchronous orchestration flow. Handle
partial failure explicitly: a timeout does not prove that the downstream
operation failed. Use idempotency keys and reservation references so a
retry can safely determine the outcome.

**Fulfillment and shipping workflow:**

1. Fulfillment Service creates work for an allocated order.
2. Operators record picking and packing operations.
3. Fulfillment Service marks the order ready for shipping after its
   checks pass.
4. Shipping Service creates a shipment and records dispatch.
5. Order Service is updated when shipping milestones occur.

For the first version, these steps may be initiated through explicit
REST calls. Later, publish domain events and make consumers idempotent.

### 4.5 Later asynchronous architecture

Introduce RabbitMQ only after the synchronous workflow is reliable.

Potential events:

- `OrderCreated`
- `InventoryReserved`
- `InventoryReservationFailed`
- `OrderAllocated`
- `PickingCompleted`
- `PackingCompleted`
- `ShipmentDispatched`
- `ShipmentDelivered`
- `OrderCancelled`

Event envelopes should include an event ID, event type, event version,
occurrence timestamp, and aggregate/entity identifier. Consumers must be
idempotent. Plan for retries, dead-letter queues, duplicate delivery,
and eventual consistency.

Do not attempt distributed database transactions. For multi-service
workflows that require compensation, introduce a saga/process-manager
approach only when the use case justifies it.

---

## 5. Maven Repository Structure

Use a parent Maven project for dependency and plugin management, with
independently buildable service modules.

Suggested initial structure:

```text
warehouse-fulfillment/
├── pom.xml
├── README.md
├── PROJECT_SPEC.md
├── .gitignore
├── docs/
│   ├── architecture/
│   ├── adr/
│   └── api/
├── services/
│   ├── catalog-service/
│   │   ├── pom.xml
│   │   └── src/
│   ├── inventory-service/
│   │   ├── pom.xml
│   │   └── src/
│   └── order-service/
│       ├── pom.xml
│       └── src/
├── infrastructure/
│   ├── docker/
│   └── jenkins/
└── scripts/
```

Add `fulfillment-service`, `shipping-service`, `identity-service`, and
`api-gateway` as their milestones begin.

The root POM should:

- Set Java release to 21.
- Manage Spring Boot and dependency versions.
- Define common Maven plugin versions and quality/test conventions.
- Aggregate modules for convenient root builds.
- Avoid packaging all services into one application.

Each service POM should declare only the dependencies it needs. A
service should be buildable and testable independently, for example with
`mvn -pl services/inventory-service -am verify`.

---

## 6. Database Design and Ownership

### 6.1 Database isolation

Each service owns its database or, during local development, its own
PostgreSQL schema with a distinct database user and permissions.

Recommended progression:

- **Local Milestone 1:** one PostgreSQL server with a separate
  database per service, or separate schemas and users if that is
  operationally simpler.
- **Later environments:** separate database instances or managed
  database resources as deployment requirements justify.

Separate databases provide a stronger ownership boundary. Separate
schemas are a practical local-development compromise, not permission to
create cross-service joins.

No cross-service foreign keys. No direct cross-service SQL joins. No
service may update another service's tables.

### 6.2 Logical data ownership

---

Service Main entities Important relationships

---

Catalog `Product` One product has a
unique SKU

Inventory `WarehouseLocation`, Stock balance belongs
`StockBalance`, to a product reference
`InventoryReservation`, and location;
`InventoryMovement` reservations refer to
an order ID and product
reference

Order `Order`, `OrderLine`, One order has many
`OrderStatusHistory` lines and
status-history records

Fulfillment `FulfillmentTask`, A task references an
`PickRecord`, `Package`, order; package lines
`PackageLine` reference
order-line/product
identifiers

Shipping `Shipment`, A shipment references
`ShipmentStatusHistory` an order and package
identifiers

Identity `User`, `Role`, Users have one or more
`UserRole` roles

---

Identifiers owned by another service are stored as scalar IDs, not as
JPA relationships. For example, Inventory Service stores `productId` and
`orderId`; it does not map remote entities with `@ManyToOne`.

### 6.3 Suggested entity fields

**Product** - `id` (UUID) - `sku` (unique) - `name` - `description` -
`active` - `createdAt` - `updatedAt`

**WarehouseLocation** - `id` (UUID) - `code` (unique within the
warehouse) - `zone`, `aisle`, `rack`, `shelf`, `bin` - `active`

**StockBalance** - `id` (UUID) - `productId` - `locationId` -
`onHandQuantity` - `reservedQuantity` - `version` (optimistic locking) -
`updatedAt`

Enforce a unique constraint on `(productId, locationId)`. Use an
appropriate numeric type and quantity precision for the chosen
unit-of-measure model. Do not use floating-point types for stock
quantities.

**InventoryReservation** - `id` (UUID) - `orderId` - `productId` -
`locationId` - `quantity` - `status` - `idempotencyKey` - `createdAt` -
`releasedAt`

**InventoryMovement** - `id` (UUID) - `productId` - `locationId` -
`movementType` - `quantity` - `referenceType` - `referenceId` -
`reason` - `actorId` - `createdAt`

**Order** - `id` (UUID) - `orderNumber` (unique human-readable
reference) - `status` - `idempotencyKey` - `createdAt` - `updatedAt`

**OrderLine** - `id` (UUID) - `orderId` - `productId` - `skuSnapshot` -
`productNameSnapshot` - `requestedQuantity` - `unitOfMeasure`

**FulfillmentTask** - `id` (UUID) - `orderId` - `status` -
`assignedUserId` - `createdAt` - `completedAt`

**Shipment** - `id` (UUID) - `orderId` - `packageId` - `carrier` -
`trackingReference` - `status` - `dispatchedAt` - `deliveredAt`

These are logical starting points, not a demand to implement every table
immediately. Add constraints, indexes, and history tables as the
corresponding workflow is implemented.

### 6.4 Persistence rules

- Use UUID identifiers for service-owned entities.
- Use database constraints for uniqueness and referential integrity
  within a service.
- Use transactions for operations that must be atomic within one
  service.
- Use optimistic locking or suitable row-level locking to prevent
  concurrent reservations from overselling stock.
- Store timestamps consistently (UTC in persistence/API
  representation).
- Use Flyway or Liquibase for versioned schema migrations; do not rely
  on Hibernate auto-creating production schemas.
- Prefer explicit repository queries for important inventory
  operations and verify their concurrency behavior with integration
  tests.

---

## 7. API Conventions

### 7.1 REST conventions

- Use resource-oriented URLs and nouns.
- Use plural resource names.
- Use standard HTTP methods and status codes.
- Version public APIs, initially under `/api/v1`.
- Use pagination for potentially large collections.
- Support filtering and sorting only where the use case requires them.
- Use ISO-8601 timestamps with an explicit UTC offset or `Z`.
- Return `Location` on successful resource creation where appropriate.

Example endpoints:

---

Service Method Endpoint Purpose

---

Catalog `POST` `/api/v1/products` Create product

Catalog `GET` `/api/v1/products/{id}` Get product

Catalog `GET` `/api/v1/products` Search/list
products

Inventory `POST` `/api/v1/stock-receipts` Receive stock

Inventory `GET` `/api/v1/inventory/{productId}` View stock for a
product

Inventory `POST` `/api/v1/reservations` Reserve stock

Inventory `POST` `/api/v1/reservations/{id}/release` Release
reservation

Order `POST` `/api/v1/orders` Create order

Order `GET` `/api/v1/orders/{id}` Get order

Order `POST` `/api/v1/orders/{id}/cancel` Request
cancellation

Fulfillment `GET` `/api/v1/fulfillment/tasks` List fulfillment
tasks

Fulfillment `POST` `/api/v1/fulfillment/tasks/{id}/pick` Record picking

Fulfillment `POST` `/api/v1/fulfillment/tasks/{id}/pack` Complete packing

Shipping `POST` `/api/v1/shipments` Create shipment

Shipping `GET` `/api/v1/shipments/{id}` Get shipment

---

The exact endpoint set can evolve through documented decisions. Avoid
using `PUT` for action-like operations such as reserve, release,
dispatch, or cancel; model these as explicit commands where that
improves clarity.

### 7.2 DTO conventions

- Use request DTOs for input and response DTOs for output.
- Never expose JPA entities directly from controllers.
- Validate request DTOs with Jakarta Bean Validation.
- Keep mapping logic outside controllers; use explicit mapper classes
  or a mapping library if it provides a clear benefit.
- Do not reuse one DTO for unrelated create, update, and response
  operations.
- Do not accept server-managed fields such as IDs, status, or audit
  timestamps from clients unless a specific use case requires it.

Example naming:

- `CreateProductRequest`
- `UpdateProductRequest`
- `ProductResponse`
- `CreateOrderRequest`
- `OrderResponse`

### 7.3 Error handling

Use a consistent error response based on the Problem Details standard
(RFC 9457), implemented with Spring's problem-detail support where
practical.

Include: - `type` - `title` - `status` - `detail` - `instance` - A
stable application error code - Field validation errors where
applicable - A correlation/trace identifier where available

Map errors consistently:

---

Condition HTTP status

---

Invalid request or validation 400
failure

Missing/invalid authentication 401

Authenticated but not 403
permitted

Resource not found 404

State or business-rule 409
conflict

Unsupported content type 415

Unexpected server failure 500

Required downstream service 503 or a documented gateway/service
unavailable error

---

Do not return stack traces, SQL details, secrets, or internal
implementation information to clients. Log diagnostic details
server-side with a correlation ID.

### 7.4 API documentation

- Publish OpenAPI documentation for each service.
- Document request/response schemas, validation constraints, security
  requirements, status codes, and examples.
- Keep API contracts in sync with implementation.
- Use contract tests to detect breaking changes once multiple services
  depend on one another.

---

## 8. Security

### 8.1 Authentication

Use Spring Security with JWT-based authentication.

The intended flow is:

1. A user authenticates with Identity Service.
2. Identity Service verifies credentials and issues a signed access
   token.
3. Clients send the token using `Authorization: Bearer <token>`.
4. Each protected service validates the token signature, issuer,
   expiry, and relevant claims.
5. Services reject invalid or expired tokens.

Use a standard, well-supported signing approach and externalized
secrets. Do not hardcode signing keys or credentials in source control.
Define key rotation and refresh-token handling before production use.

For the first milestone, endpoints may be locally accessible without
authentication only if the environment is explicitly development-only.
Do not mistake this temporary setup for production security.

### 8.2 Authorization

Use role-based access control initially.

Suggested roles:

---

Role Typical permissions

---

`ADMIN` User/role administration and all
operational configuration

`INVENTORY_OPERATOR` Receive stock, adjust stock, view
inventory

`FULFILLMENT_OPERATOR` View assigned tasks, pick, and pack

`SHIPPING_OPERATOR` Create shipments and update
dispatch/delivery status

`ORDER_MANAGER` Create, view, and cancel orders

`READ_ONLY` Read permitted operational
resources

---

Enforce authorization at the service boundary, not only in the API
Gateway or UI. Prefer permissions tied to business operations over broad
checks scattered through controllers.

### 8.3 Additional security requirements

- Hash passwords using a modern adaptive password encoder such as
  BCrypt or Argon2.
- Validate and constrain all external input.
- Use least-privilege database users per service.
- Keep secrets out of Git, logs, and container images.
- Apply CORS rules deliberately; do not use wildcard origins with
  credentials.
- Use HTTPS in deployed environments.
- Avoid logging access tokens, passwords, or sensitive personal
  information.
- Add rate limiting and brute-force protections when the platform is
  exposed beyond a trusted development network.

---

## 9. Testing Strategy

Testing should validate business behavior and service boundaries, not
merely line coverage.

### 9.1 Unit tests

**Tools:** JUnit 5 and Mockito.

Test: - Domain rules and state transitions. - Inventory availability and
reservation calculations. - Order validation and lifecycle behavior. -
Fulfillment picking/packing rules. - Authorization decisions where
practical. - Error mapping and validation behavior.

Unit tests should be fast and isolated. Mock external service clients
and repositories at application-service boundaries, but do not mock the
class whose behavior is under test.

### 9.2 Integration tests

**Tools:** Spring Boot Test and Testcontainers with PostgreSQL.

Test: - Repository queries against real PostgreSQL. - Database
constraints and migrations. - Transaction boundaries and rollback
behavior. - Inventory concurrency and overselling protection. - REST
endpoints, serialization, validation, and exception handling. - Security
configuration and JWT validation. - Service-to-service HTTP behavior
using controlled test dependencies.

Prefer real PostgreSQL containers over H2 for tests that depend on
PostgreSQL behavior.

### 9.3 Contract tests

Add contract testing when service-to-service integrations are
established.

Tests should verify: - Provider response shapes and status codes. -
Consumer assumptions about required fields. - Compatibility of API
changes. - Error response contracts. - Authentication requirements for
protected endpoints.

Pact is a possible later tool; do not add it until there is a concrete
provider/consumer relationship to test.

### 9.4 Test quality gates

A service build must fail when: - Compilation fails. - Unit tests
fail. - Required integration tests fail. - Static checks configured for
that milestone fail.

Do not use code coverage as the only quality measure. Establish a modest
baseline and increase it as meaningful tests are added. Avoid tests that
merely mirror implementation details.

---

## 10. Coding Standards and Design Principles

### 10.1 SOLID and OOP

Apply SOLID principles where they improve separation of
responsibilities:

- **Single Responsibility:** controllers handle HTTP concerns;
  application services coordinate use cases; domain objects enforce
  business rules; repositories handle persistence.
- **Open/Closed:** extend behavior through focused abstractions where
  variation is expected, not through speculative frameworks.
- **Liskov Substitution:** implementations of an abstraction must
  preserve its contract.
- **Interface Segregation:** define small interfaces around actual
  client or domain needs.
- **Dependency Inversion:** high-level use cases depend on
  abstractions for infrastructure boundaries.

Use encapsulation and meaningful domain methods. Avoid anemic models
where important invariants are scattered across controllers and service
classes, but do not force complex domain-driven design patterns onto
simple CRUD behavior.

### 10.2 Clean architecture within each service

Use a consistent dependency direction:

```text
Inbound adapters (REST controllers)
              |
              v
Application layer (use cases / orchestration)
              |
              v
Domain layer (business rules and domain models)

Infrastructure adapters (JPA, HTTP clients, messaging)
implement ports required by the application/domain layers.
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

Keep framework annotations and persistence concerns out of the core
domain where practical. Do not create interfaces for every class
automatically; introduce abstractions at meaningful boundaries.

### 10.3 General conventions

- Use constructor injection; avoid field injection.
- Keep classes and methods focused.
- Prefer immutable request/response DTOs where practical (Java records
  are suitable).
- Use descriptive names and consistent formatting.
- Avoid primitive obsession for important domain concepts where value
  objects add clarity.
- Use enums or explicit state-transition methods for lifecycle states.
- Avoid catch-all exception handling that hides failures.
- Avoid `Optional` for entity fields and request DTO properties.
- Use logging with context; do not use `System.out.println`.
- Keep configuration externalized and environment-specific.
- Add comments for intent or non-obvious constraints, not to narrate
  obvious code.
- Do not add abstractions, libraries, or services without a
  demonstrated requirement.

---

## 11. CI/CD Objectives --- Jenkins Learning Requirements

Jenkins is both the automation platform for this project and a
deliberate learning objective. The pipeline should grow in stages,
demonstrating core Jenkins concepts without making the application
dependent on a complicated pipeline from day one.

### 11.1 Required Jenkins concepts

The completed project should demonstrate:

1. **Controller and agents**
   - A Jenkins controller manages jobs and schedules work.
   - Build/test workloads execute on agents.
   - Use labels to target appropriate agents, such as `java`,
     `docker`, and `integration-test`.
   - Do not run routine builds on the controller executor.
   - Demonstrate at least two agents or two distinct agent
     environments when resources permit.
2. **Users, roles, and permissions**
   - Create separate administrator and developer/operator access.
   - Apply least privilege.
   - Restrict credential and job-configuration access.
   - Demonstrate folder-level or project-level permissions if the
     installed authorization strategy supports them.
3. **Declarative Pipeline**
   - Keep a `Jenkinsfile` in source control.
   - Use stages, agents, environment variables, parameters, `post`
     actions, and failure handling.
   - Keep pipeline logic readable and reusable.
4. **Maven integration**
   - Configure JDK and Maven tools or use a controlled build image.
   - Run repeatable Maven commands.
   - Archive build artifacts and publish test reports.
5. **Upstream and downstream jobs**
   - Demonstrate one job triggering another.
   - Pass build identifiers or artifact references explicitly.
   - Avoid rebuilding different source revisions unknowingly between
     dependent jobs.
6. **Credentials management**
   - Store Git, registry, and deployment credentials in Jenkins
     Credentials.
   - Bind credentials only to the stages that need them.
   - Never print secrets or commit them to the repository.
7. **Build triggers**
   - Demonstrate SCM/webhook or polling-based triggers.
   - Demonstrate manual parameterized execution.
   - Use scheduled jobs only where they add value.
8. **Parallel execution**
   - Run independent service tests or quality checks in parallel.
   - Use agent labels and resource limits to avoid overloading the
     workstation.
9. **Artifact handling**
   - Archive JARs and test reports.
   - Demonstrate artifact promotion between pipeline stages/jobs.
   - Later, publish versioned Docker images to a registry.
10. **Pipeline quality gates**
    - Fail builds on compilation, test, or configured quality-check
      failures.
    - Make deployment stages conditional on successful validation.
11. **Pipeline reuse**
    - Introduce a Jenkins Shared Library only after multiple pipelines
      contain genuinely repeated logic.

### 11.2 Target pipeline

A mature service pipeline should resemble:

```text
Checkout
   |
   v
Compile
   |
   v
Unit Tests
   |
   v
Static / Dependency Checks
   |
   v
Integration Tests (PostgreSQL Testcontainers)
   |
   v
Package JAR
   |
   v
Archive / Publish Artifact
   |
   v
Build Container Image
   |
   v
Deploy to Test Environment
   |
   v
Smoke / Contract Tests
```

The initial pipeline does not need every stage. Add each stage when the
corresponding application capability exists.

### 11.3 Suggested Jenkins job topology

Use a deliberately understandable topology:

- `warehouse-ci`: validates the repository and coordinates service
  builds.
- `catalog-ci`: builds and tests Catalog Service.
- `inventory-ci`: builds and tests Inventory Service.
- `order-ci`: builds and tests Order Service.
- `warehouse-integration`: runs cross-service workflow tests.
- `warehouse-release`: packages/publishes approved artifacts and is
  manually gated initially.

Demonstrate an upstream/downstream relationship, for example
`warehouse-ci` triggering service jobs and the integration job running
only after required service jobs succeed. As the project matures,
compare this job topology with a single multibranch pipeline and
document the trade-offs.

### 11.4 Jenkins security and operational boundaries

- Keep the controller isolated from routine build execution.
- Use dedicated, least-privilege agent accounts.
- Do not give Jenkins agents unrestricted host root access.
- Do not expose the Docker socket to untrusted jobs; access to it is
  effectively highly privileged.
- Keep secrets in Jenkins Credentials.
- Restrict who can configure jobs and modify pipeline scripts.
- Pin or control build tool versions for reproducibility.
- Keep Jenkins configuration and pipeline definitions documented in
  the repository where practical.

---

## 12. Incremental Development Plan

Do not introduce all target technologies in the first milestone. Each
milestone should produce a working, testable increment.

### Milestone 0 --- Environment and repository foundation

**Goal:** establish a reproducible Java/Maven development environment.

- Verify JDK 21, Maven, Git, PostgreSQL, Docker, and Jenkins
  availability.
- Create the Git repository and root Maven parent project.
- Add formatting, compiler, and test plugin conventions.
- Create a minimal Spring Boot service and prove `mvn verify` works.
- Add a basic Jenkins pipeline that checks out, compiles, tests, and
  archives the result.
- Document local setup and required environment variables.

**Exit criteria:** a clean checkout can be built locally and by Jenkins
without manual IDE steps.

### Milestone 1 --- Catalog Service

**Goal:** deliver the first complete vertical slice.

- Implement product creation, retrieval, listing, update, and
  deactivation.
- Add PostgreSQL persistence and schema migrations.
- Add request/response DTOs and validation.
- Add consistent Problem Details error responses.
- Add OpenAPI documentation.
- Add unit tests and PostgreSQL integration tests.
- Add a Jenkins pipeline running Maven verification on a Java agent.

**Exit criteria:** Catalog API works through HTTP, persists data, has
automated tests, and builds in Jenkins.

### Milestone 2 --- Inventory Service

**Goal:** implement reliable stock management.

- Implement warehouse locations and stock balances.
- Implement stock receipts and adjustments.
- Implement inventory movements.
- Implement reservations and releases with idempotency.
- Protect concurrent stock updates from overselling.
- Add Testcontainers-based PostgreSQL integration tests.
- Add JWT resource-server validation and role-based authorization as
  part of the security increment.
- Add Dockerfiles and local container execution.

**Exit criteria:** inventory invariants hold under normal and concurrent
requests; unauthorized operations are rejected.

### Milestone 3 --- Order Service and first end-to-end workflow

**Goal:** create and allocate orders.

- Implement order and order-line management.
- Validate product references through Catalog Service.
- Coordinate reservation through Inventory Service.
- Implement legal order state transitions and cancellation rules.
- Add downstream failure and retry/idempotency handling.
- Add integration/contract tests for the service boundaries.
- Expand Jenkins to build service modules in parallel where
  appropriate.

**Exit criteria:** an order can be created and allocated end-to-end
without direct database access between services.

### Milestone 4 --- Fulfillment and Shipping

**Goal:** complete the warehouse order lifecycle.

- Implement pick tasks, assignment, picking, and short-pick
  exceptions.
- Implement packing and package records.
- Implement shipment creation, dispatch, and tracking status.
- Add role-specific authorization.
- Add end-to-end workflow tests.
- Add container image build and test-environment deployment stages to
  Jenkins.

**Exit criteria:** a valid order can move through picking, packing, and
dispatch with traceable status history.

### Milestone 5 --- API Gateway and asynchronous messaging

**Goal:** evolve the service landscape without destabilizing existing
workflows.

- Introduce Spring Cloud Gateway for external routing.
- Introduce RabbitMQ for selected domain events.
- Implement idempotent event consumers, retry handling, and
  dead-letter queues.
- Replace selected synchronous calls only where asynchronous behavior
  is beneficial.
- Add correlation IDs and improved observability.
- Document consistency and failure-recovery behavior.

**Exit criteria:** selected workflows operate through documented events
and recover safely from duplicate delivery and consumer failure.

### Milestone 6 --- CI/CD maturity

**Goal:** demonstrate a production-style delivery workflow.

- Separate controller and agent responsibilities.
- Add multiple labeled agents and parallel builds.
- Add role-based Jenkins access and credential management.
- Demonstrate upstream/downstream jobs and artifact promotion.
- Publish versioned container images.
- Add gated deployment and post-deployment smoke tests.
- Introduce a Shared Library if repeated pipeline logic warrants it.
- Document rollback and release procedures.

**Exit criteria:** a source change can be validated, packaged,
published, and deployed through a controlled Jenkins pipeline.

---

## 13. Development Environment Prerequisites

Prepare and verify these tools on the Ubuntu workstation before granting
an autonomous coding agent access.

---

Tool Purpose Verification

---

JDK 21 Java `java -version`
compilation/runtime

Maven Build and dependency `mvn -version`
management

Git Version control `git --version`

PostgreSQL Local persistence `psql --version` and
connection test

Docker Later container builds `docker --version` and
and Testcontainers `docker info`
support

Jenkins CI/CD learning and Open Jenkins UI and
pipeline execution verify a test job

VS Code / IntelliJ Code inspection and Open project and import
debugging Maven modules

curl / HTTP client API testing `curl --version`

---

Do not install a second copy of a tool if a working, supported
installation is already present. Record the actual versions in the
repository's development setup documentation.

### 13.1 Hermes agent access checklist

Before asking Hermes to implement features, verify that it can:

- Read and write files only within the intended project workspace.
- Execute `java`, `mvn`, and `git` commands.
- Run tests and inspect complete Maven/test output.
- Access the internet for official Java, Spring, Maven, PostgreSQL,
  and Jenkins documentation.
- Inspect the existing Jenkins installation through its UI or approved
  local configuration paths.
- Run the project without requiring interactive root access.
- Report commands executed, files changed, tests run, and unresolved
  issues.

Use a dedicated unprivileged OS account or a restricted workspace. Grant
only the directory and command permissions needed for development. Do
not provide unrestricted `sudo`, root credentials, unrestricted Docker
socket access, production secrets, or broad access to unrelated
home-directory files.

The agent should propose infrastructure changes and explain why elevated
permissions are needed. A human should approve privileged operations
such as installing system packages, changing firewall rules, modifying
system services, or changing Jenkins security settings.

### 13.2 Suggested project-level agent instructions

Keep a separate `AGENTS.md` or equivalent instruction file for the
coding agent. It should direct the agent to:

- Read `PROJECT_SPEC.md` before making architectural decisions.
- Implement only the current milestone.
- Avoid introducing later-milestone technologies prematurely.
- Follow the service's package boundaries and coding conventions.
- Add or update tests with every business behavior change.
- Run the relevant Maven verification command before reporting
  completion.
- Never edit secrets or machine-level configuration without approval.
- Summarize changed files, design decisions, commands, test results,
  and remaining work.

---

## 14. Definition of Done

A feature is complete only when all applicable conditions are met:

- Business behavior matches the requirement and invariants.
- The owning service contains the implementation; no other service
  writes its data.
- REST request/response DTOs and validation are in place.
- Error cases return documented responses.
- Authorization is enforced when the feature is protected.
- Unit tests cover core business rules.
- Integration tests cover persistence and framework boundaries where
  applicable.
- API documentation is updated.
- Database changes are represented by a migration.
- Logs provide useful diagnostic context without exposing secrets.
- Maven verification passes.
- The Jenkins pipeline validates the change.
- Documentation and examples are updated where needed.

---

## 15. Explicitly Deferred Features

Do not implement these until the core order-to-shipment workflow is
stable:

- Multiple warehouses and cross-warehouse allocation.
- External carrier integrations and live rate shopping.
- Payment processing and customer storefront.
- Advanced warehouse optimization, wave/batch picking, and slotting.
- Barcode/RFID hardware integration.
- Lot, batch, serial-number, and expiry-date tracking.
- Returns and reverse logistics.
- Forecasting and demand planning.
- Distributed tracing and a full observability stack.
- Kubernetes deployment and production cloud infrastructure.

These features may become future milestones, but they must not delay the
first working fulfillment workflow.

---

## 16. Initial Implementation Instruction

Start with **Milestone 0**, then implement **Catalog Service** as the
first vertical slice.

Before writing application code:

1. Inspect the existing workspace and installed JDK, Maven, PostgreSQL,
   Docker, and Jenkins versions.
2. Report what is already installed and working; do not reinstall tools
   unnecessarily.
3. Propose the initial repository tree and root/service Maven POM
   design.
4. Identify any compatibility issues.
5. Wait for approval before performing privileged machine-level
   changes.

Do not scaffold every planned microservice at once. Establish the build,
test, database-migration, API, and Jenkins conventions in the first
service, then reuse the proven approach for subsequent services.
