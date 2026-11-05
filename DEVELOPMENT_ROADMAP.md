# Warehouse Fulfillment Platform — Development Roadmap

**Status:** Implementation plan
**Version:** 1.0

## 1. Delivery strategy

Build the platform as a sequence of working vertical slices. Do not scaffold every microservice or introduce every target technology at the beginning.

The first objective is a reproducible Maven build and one complete service. The second is inventory correctness. The third is a real order-allocation workflow across service boundaries.

Each milestone must produce a demonstrable result, automated tests, and an updated Jenkins pipeline.

## 2. Milestone overview

| Milestone | Focus                      | Main deliverable                            |
| --------- | -------------------------- | ------------------------------------------- |
| 0         | Environment and repository | Reproducible Maven foundation               |
| 1         | Catalog Service            | Product API with PostgreSQL                 |
| 2         | Inventory Service          | Stock receipts and safe reservations        |
| 3         | Order Service              | Order creation and allocation               |
| 4         | Fulfillment and Shipping   | Pick, pack, and dispatch lifecycle          |
| 5         | Gateway and RabbitMQ       | Routing and selected asynchronous workflows |
| 6         | CI/CD maturity             | Multi-agent Jenkins delivery pipeline       |

## 3. Milestone 0 — Environment and repository foundation

### Objectives

- Inspect existing workstation tooling.
- Establish Java 21 and Maven conventions.
- Create the Git repository and root Maven parent.
- Prove a Spring Boot module can compile, test, and package.
- Create a minimal Jenkins pipeline.

### Tasks

1. Verify Java, Maven, Git, PostgreSQL, Docker, and Jenkins versions.
2. Check compatibility between Java, Spring Boot, and Maven.
3. Create the repository and root `pom.xml`.
4. Configure Java release 21 and managed dependency/plugin versions.
5. Create one minimal service module.
6. Add a health endpoint and one unit test.
7. Add `.gitignore`, README, and environment configuration guidance.
8. Create a Jenkinsfile that checks out, compiles, tests, and archives the JAR.
9. Ensure local and Jenkins builds use the same Maven lifecycle.

### Exit criteria

- A clean clone builds without IDE-specific steps.
- `mvn clean verify` succeeds.
- Jenkins can build the repository.
- No secrets or machine-specific configuration are committed.
- Build instructions are documented.

## 4. Milestone 1 — Catalog Service

### Objectives

Deliver the first complete vertical slice with a clean architecture and PostgreSQL persistence.

### Tasks

- Create product domain model and business rules.
- Implement create, retrieve, list, update, and deactivate use cases.
- Add PostgreSQL persistence.
- Add Flyway or Liquibase migrations.
- Add request and response DTOs.
- Add Jakarta Bean Validation.
- Add centralized Problem Details error handling.
- Add OpenAPI documentation.
- Add unit tests with JUnit 5 and Mockito.
- Add PostgreSQL integration tests when the local test setup is ready.
- Add Jenkins test-result publishing and artifact archiving.

### Exit criteria

- Product CRUD lifecycle works through HTTP.
- SKU uniqueness is enforced by the database.
- Invalid requests return documented errors.
- JPA entities are not exposed by the API.
- Migrations work from a clean database.
- Tests pass locally and in Jenkins.

## 5. Milestone 2 — Inventory Service

### Objectives

Implement stock management with strong consistency within the Inventory Service.

### Tasks

- Add warehouse locations.
- Add stock balances.
- Implement stock receipts and adjustments.
- Record inventory movements.
- Implement reservations and releases.
- Add idempotency for reservation commands.
- Prevent negative available stock under concurrent requests.
- Add optimistic or pessimistic locking.
- Add Testcontainers PostgreSQL integration tests.
- Add security with JWT validation and role-based authorization.
- Add Dockerfiles and local container execution.

### Exit criteria

- Stock receipts and adjustments update balances correctly.
- Reservation and release operations are idempotent.
- Concurrent reservations cannot oversell stock.
- Database constraints protect core invariants.
- Integration tests use PostgreSQL behavior.
- Protected endpoints enforce roles.

## 6. Milestone 3 — Order Service and first end-to-end workflow

### Objectives

Create orders and allocate stock through service APIs.

### Tasks

- Add orders, order lines, and status history.
- Store product SKU/name snapshots.
- Validate product references through Catalog Service.
- Implement order lifecycle transitions.
- Call Inventory Service to reserve all required lines.
- Handle reservation success, business failure, and ambiguous timeout outcomes.
- Add idempotency to order creation and allocation.
- Implement cancellation rules and reservation release.
- Add integration tests for remote client behavior.
- Add contract tests for Catalog and Inventory contracts.
- Expand Jenkins to build independent services in parallel where practical.

### Exit criteria

- An order can be created and allocated end-to-end.
- Services communicate only through documented APIs.
- No cross-service database access exists.
- Retries do not create duplicate orders or reservations.
- Invalid order state transitions are rejected.

## 7. Milestone 4 — Fulfillment and Shipping

### Objectives

Complete the warehouse operational lifecycle.

### Tasks

- Create fulfillment tasks for allocated orders.
- Implement operator assignment.
- Implement pick records and short-pick exceptions.
- Enforce picking completion rules.
- Implement packages and package lines.
- Validate packing completion.
- Implement shipment creation.
- Implement dispatch and shipment status history.
- Add role-specific authorization.
- Add end-to-end workflow tests.
- Add container image build and test-environment deployment stages to Jenkins.

### Exit criteria

A valid order can progress through:

```text
PENDING
  -> CONFIRMED
  -> ALLOCATED
  -> FULFILLING
  -> READY_TO_SHIP
  -> SHIPPED
  -> COMPLETED
```

All transitions and operational actions are auditable. Exceptional states are handled explicitly.

## 8. Milestone 5 — API Gateway and RabbitMQ

### Objectives

Introduce routing and asynchronous communication without weakening existing service boundaries.

### Tasks

- Add Spring Cloud Gateway.
- Route external requests through the gateway.
- Keep service-level authentication and authorization.
- Introduce RabbitMQ for one or two well-defined events.
- Define versioned event envelopes.
- Implement idempotent consumers.
- Add retries and dead-letter queues.
- Consider transactional outbox for reliable event publication.
- Add correlation IDs and structured logs.
- Document eventual consistency and failure recovery.

### Exit criteria

- External API routing is documented.
- At least one asynchronous workflow works end-to-end.
- Duplicate event delivery is safe.
- Failed messages can be investigated and recovered.
- Existing REST workflows remain covered by tests.

## 9. Milestone 6 — Jenkins CI/CD maturity

### Objectives

Demonstrate controller/agent separation and a controlled delivery lifecycle.

### Tasks

- Configure controller executors appropriately; do not run routine builds on the controller.
- Add labeled Java and Docker-capable agents.
- Demonstrate multiple agents or distinct agent environments.
- Configure Jenkins users, roles, and least-privilege permissions.
- Store credentials in Jenkins Credentials.
- Demonstrate Declarative Pipeline.
- Demonstrate upstream/downstream jobs.
- Add parallel service builds.
- Archive test reports and versioned artifacts.
- Publish container images.
- Add manually gated deployment.
- Add post-deployment smoke tests.
- Introduce a Shared Library only when reuse is justified.

### Exit criteria

A source change can be checked out, built, tested, packaged, published, deployed to a test environment, and smoke-tested through Jenkins.

## 10. Definition of Done

Every feature must meet the applicable requirements:

- Business rules and invariants are implemented in the owning service.
- API DTOs, validation, and error responses are documented.
- Persistence changes use a migration.
- Unit tests cover domain/application behavior.
- Integration tests cover persistence and framework boundaries.
- Contract tests cover established service dependencies.
- Security rules are enforced for protected operations.
- Logs contain useful context without secrets.
- Relevant Maven verification passes.
- Jenkins validates the change.
- Documentation is updated.

## 11. Working agreement for Hermes

Before each milestone:

1. Read `PROJECT_SPEC.md` and the relevant architecture/API/database documents.
2. Inspect the existing repository and current milestone status.
3. Present a short implementation plan and proposed file changes.
4. Identify dependencies and any required privileged operations.
5. Wait for approval before machine-level or security-sensitive changes.

During implementation:

- Work only on the active milestone.
- Do not scaffold future services prematurely.
- Do not add RabbitMQ, Gateway, Kubernetes, or unrelated libraries early.
- Follow SOLID and the service's package boundaries.
- Add tests with every business behavior change.
- Run the narrowest relevant tests, then the module's Maven verification.
- Do not suppress failing tests to make a build green.

At completion, report:

- Summary of behavior implemented.
- Files added or changed.
- Architecture decisions made.
- Commands executed.
- Test results.
- Known limitations and next steps.

## 12. Risks and mitigations

| Risk                               | Mitigation                                                |
| ---------------------------------- | --------------------------------------------------------- |
| Too many services too early        | Build one vertical slice at a time                        |
| Overselling inventory              | Atomic updates/locking and concurrency tests              |
| Cross-service data coupling        | Service-owned databases and API contracts                 |
| Duplicate commands after timeout   | Idempotency keys and durable operation references         |
| Distributed workflow inconsistency | Explicit orchestration; later saga/outbox where justified |
| Jenkins overloads workstation      | Separate agents, labels, and concurrency limits           |
| Agent performs unsafe host changes | Restricted account and human approval                     |
| Dependency/version drift           | Root POM dependency and plugin management                 |

## 13. Immediate next actions

1. Inspect the Ubuntu workstation and existing Jenkins installation.
2. Confirm Java 21 and Maven are available.
3. Create the Git repository and Maven parent.
4. Create only the initial Catalog Service module.
5. Prove local `mvn clean verify`.
6. Prove the same build through Jenkins.
7. Begin Catalog API implementation.
