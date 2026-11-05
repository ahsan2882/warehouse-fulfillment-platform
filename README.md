# Warehouse Fulfillment Platform

Enterprise Warehouse and Order Fulfillment Platform built with **Java 21**, **Spring Boot**, and **Maven multi-module architecture**.

## 📋 Overview

This project demonstrates a production-quality microservices application for managing the warehouse order lifecycle:

- Product/SKU catalog management
- Inventory tracking (receipts, reservations, movements)
- Order creation and allocation across service boundaries
- Fulfillment workflow (pick, pack, ship)
- Shipment dispatch and tracking
- User authentication and authorization via JWT

### Project Goals

✅ **SOLID principles** - Maintainable, production-quality code
✅ **Clean architecture** - Clear separation of concerns
✅ **Microservices** - Independently deployable services
✅ **Database ownership** - Each service owns its data
✅ **Automated testing** - JUnit 5, Mockito, Testcontainers
✅ **CI/CD with Jenkins** - Declarative pipelines, upstream/downstream jobs
✅ **Docker & Kubernetes ready** - Containerized deployment

### Technologies

| Category         | Technology                       |
| ---------------- | -------------------------------- |
| Language         | Java 21                          |
| Framework        | Spring Boot 3.4.3                |
| Build Tool       | Maven 3.9+                       |
| Database         | PostgreSQL 15+                   |
| ORM              | Spring Data JPA / Hibernate      |
| Testing          | JUnit 5, Mockito, Testcontainers |
| CI/CD            | Jenkins                          |
| Containerization | Docker                           |

## 🏗️ Architecture

### High-Level Service Map

```text
┌─────────────────────────────────────────────────────────┐
│                   API Gateway (future)                   │
└──────────────────────┬──────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   ┌────────┐    ┌──────────┐  ┌──────────┐
   │Catalog │    │Inventory │  │  Orders  │
   │Service │    │ Service │  │ Service  │
   └────────┘    └──────────┘  └──────────┘
                         │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
       ┌────────┐  ┌──────────┐  ┌──────────┐
       │Fulfill│  │ Shipping │  │Identity  │
       │ Service│  │ Service │  │ Service  │
       └────────┘  └──────────┘  └──────────┘
```

### Microservices (Milestone Progress)

| Milestone | Service       | Status      | Database              |
| --------- | ------------- | ----------- | --------------------- |
| M0        | Foundation    | ✅ Ready    | -                     |
| M1        | **Catalog**   | 🚧 In Scope | warehouse_catalog     |
| M2        | **Inventory** | ⏳ Planned  | warehouse_inventory   |
| M3        | **Orders**    | ⏳ Planned  | warehouse_orders      |
| M4        | Fulfillment   | ⏳ Planned  | warehouse_fulfillment |
| M5        | Shipping      | ⏳ Planned  | warehouse_shipping    |
| M6        | Identity      | ⏳ Planned  | warehouse_identity    |

### Clean Architecture Per Service

```text
presentation/ (Controllers, Request DTOs)
         ▼
application/ (Use cases, Services, Application DTOs)
         ▼
domain/ (Entities, Value Objects, Domain Exceptions, Repository Interfaces)
         ▼
infrastructure/ (JPA, HTTP Clients, Messaging, Persistence Implementations)
```

## 📁 Project Structure

```text
warehouse-fulfillment-platform/
├── pom.xml                    # Root POM with dependency management
├── README.md                  # This file
├── AGENTS.md                  # Development guidelines
├── ARCHITECTURE.md            # Architecture decisions
├── API_CONVENTIONS.md         # API contract standards
├── DATABASE_DESIGN.md         # Database schema design
├── DEVELOPMENT_ROADMAP.md     # Milestone plan
├── JENKINS_CICD_PLAN.md       # CI/CD strategy
│
├── services/
│   ├── catalog-service/       # Product/SKU management API (M1)
│   ├── inventory-service/     # Stock and reservations (M2)
│   ├── order-service/         # Order lifecycle (M3)
│   ├── fulfillment-service/   # Pick/pack workflow (M4)
│   ├── shipping-service/      # Dispatch & tracking (M5)
│   └── identity-service/      # Auth & JWT (M6)
│
├── infrastructure/
│   ├── docker/                # Dockerfiles and compose files
│   └── jenkins/               # Jenkins pipeline templates
│
└── docs/
    └── adr/                   # Architecture Decision Records
```

## 🚀 Quick Start

### Prerequisites

All required tools are already installed on your system:

- ✅ Java 21.0.12
- ✅ Maven 3.9.12
- ✅ Git 2.53.0
- ✅ Docker 29.9.3
- ✅ PostgreSQL 18
- ✅ Jenkins

### Initialize the Repository

```bash
# Navigate to project root
cd /mnt/drive1/Projects/warehouse-fulfillment-platform

# Create initial module directories
mkdir -p services/catalog-service
mkdir -p infrastructure/docker
mkdir -p docs/adr
```

### Build Verification

```bash
# Verify Maven setup
mvn --version

# Test compilation (creates minimal structure)
mvn clean compile -pl services/catalog-service -am

# Full build with tests (will create remaining modules on demand)
mvn clean verify
```

## 📚 Documentation Overview

### Core Documents

- **[PROJECT_SPEC.md](./PROJECT_SPEC.md)** - Complete project specification and scope
- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Microservices architecture and principles
- **[DATABASE_DESIGN.md](./DATABASE_DESIGN.md)** - Database schema and relationships
- **[API_CONVENTIONS.md](./API_CONVENTIONS.md)** - REST API design guidelines
- **[DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md)** - Milestone-by-milestone implementation plan

### CI/CD Documentation

- **[JENKINS_CICD_PLAN.md](./JENKINS_CICD_PLAN.md)** - Jenkins pipeline strategy and job topology

## 🎯 Development Roadmap

See [DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md) for milestone details:

| Milestone | Focus                  | Key Deliverable                           |
| --------- | ---------------------- | ----------------------------------------- |
| M0        | Environment            | Maven foundation, one working service     |
| M1        | Catalog Service        | Product CRUD API with PostgreSQL          |
| M2        | Inventory Service      | Stock management with concurrency control |
| M3        | Order Service          | End-to-end order allocation workflow      |
| M4        | Fulfillment & Shipping | Pick, pack, ship lifecycle                |
| M5        | Gateway & RabbitMQ     | Async events and API routing              |
| M6        | CI/CD Maturity         | Multi-agent Jenkins delivery pipeline     |

## 🔑 Key Principles

### 1. Service Ownership

- Each service is the sole owner of its data
- No cross-service database access
- External IDs stored as scalar values, not JPA relationships
- Services communicate via documented REST APIs (sync) and messaging (async)

### 2. Clean Architecture

Controllers → Use Cases → Domain → Infrastructure

```text
┌─────────────┐
│ Controllers │ HTTP Requests/Responses
└──────────┬──┘
           ▼
┌─────────────┐
│ Application │ Use cases, DTOs, Services
└──────────┬──┘
           ▼
┌─────────────┐
│    Domain   │ Entities, Value Objects, Business Rules
└──────────┬──┘
           ▼
┌─────────────┐
│Infrastructure│ JPA, HTTP Clients, Persistence
└─────────────┘
```

### 3. Testing Strategy

- **Unit Tests**: Domain and application layer behavior (JUnit 5 + Mockito)
- **Integration Tests**: PostgreSQL persistence via Testcontainers
- **Contract Tests**: Service-to-service API contracts (future)
- **Never skip tests to make build pass**

### 4. CI/CD Pipeline Flow

```text
Source Code Change
        │
        ▼
    [Checkout] → Compile → Unit Tests
        │                 ├─→ Publish Test Reports
        ▼                 └─→ Archive JAR
    Service Builds (parallel)
        │
        ▼
   Integration Tests
        │
        ▼
   Container Image Build
        │
        ▼
   Deploy to Test Environment
        │
        ▼
   Smoke Tests
        │
        ▼
   Approve → Release Pipeline
```

### 5. Database Design

- UUID primary keys throughout
- Service-owned databases (no cross-service foreign keys)
- Flyway/Liquibase for schema migrations
- Optimistic locking on inventory records
- Append-only status history tables

## 🔐 Security Considerations

- JWT authentication via Identity Service
- Role-based authorization (ADMIN, INVENTORY_OPERATOR, FULFILLMENT_OPERATOR, etc.)
- Centralized exception handling with proper error codes
- Never commit secrets to Git
- Use Jenkins Credentials for CI/CD secrets

## 📊 API Endpoints Overview

See [API_CONVENTIONS.md](./API_CONVENTIONS.md) for full endpoint inventory:

| Service     | Key Endpoints                                                               |
| ----------- | --------------------------------------------------------------------------- |
| Catalog     | `POST /api/v1/products`, `GET /api/v1/products/{id}`                        |
| Inventory   | `POST /api/v1/stock-receipts`, `POST /api/v1/reservations`                  |
| Orders      | `POST /api/v1/orders`, `POST /api/v1/orders/{id}/cancel`                    |
| Fulfillment | `GET /api/v1/fulfillment/tasks`, `POST /api/v1/fulfillment/tasks/{id}/pick` |
| Shipping    | `POST /api/v1/shipments`, `POST /api/v1/shipments/{id}/dispatch`            |

## 🧪 Running Locally

### 1. Start PostgreSQL

```bash
# Using Docker Compose
docker-compose -f infrastructure/docker/docker-compose.yml up -d postgres
```

Or using Docker:

```bash
docker run --name warehouse-db \
  -e POSTGRES_DB=warehouse_catalog \
  -e POSTGRES_USER=app_user \
  -e POSTGRES_PASSWORD=app_password \
  -p 5432:5432 \
  postgres:15-alpine
```

### 2. Build and Run Catalog Service (Milestone 1)

```bash
# Navigate to service directory
cd services/catalog-service

# Build with Maven
mvn clean package

# Run locally (configure application.properties first)
mvn spring-boot:run -pl . -am
```

### 3. Configure Application

Each service has an `application.properties` template in:

- `src/main/resources/application.properties` - Common settings
- `src/main/resources/application-local.properties` - Local development
- `src/main/resources/application-test.properties` - Test environment

Example local configuration for catalog-service:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/warehouse_catalog
spring.datasource.username=app_user
spring.datasource.password=app_password
spring.datasource.driver-class-name=org.postgresql.Driver

server.port=8080

# API settings
api.version=1.0
```

## 🚢 Deployment

### Docker Compose (Multi-Service)

```bash
docker-compose -f infrastructure/docker/docker-compose.yml up -d
```

### Kubernetes

See `infrastructure/docker/k8s/` for K8s manifests:

- `statefulset.yaml` - Service deployments
- `service.yaml` - External service exposure
- `configmap.yaml` - Configuration management

## 🔍 Testing Guidelines

### Unit Tests

```java
@SpringBootTest
class ProductRepositoryTest {
    @Autowired
    private ProductRepository repository;

    @Test
    void should_save_product_with_unique_sku() {
        // Test implementation
    }
}
```

### Integration Tests (Testcontainers)

```java
@Testcontainers
@SpringBootTest
class CatalogServiceIntegrationTests {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("warehouse_catalog")
            .withUsername("app_user")
            .withPassword("app_password");

    @DynamicPropertySource
    static DynamicProperties postgresProps =
        DynamicProperties.of(postgres,
            (container) -> container.start());
}
```

## 📈 Build Verification Checklist

Before marking a milestone complete:

- [ ] `mvn clean verify` passes locally
- [ ] All unit tests pass
- [ ] Integration tests pass with PostgreSQL
- [ ] No new warnings in test output
- [ ] Code is formatted (Spotless)
- [ ] API endpoints documented (OpenAPI/Swagger)
- [ ] Database migrations work from clean DB
- [ ] Service can be built independently (`mvn -pl <service> -am`)

## 🚀 CI/CD Pipeline

### Jenkins Setup

1. Create Jenkins job for each service:
   - `catalog-ci`
   - `inventory-ci`
   - `order-ci`

2. Configure pipeline in each service's `Jenkinsfile`

3. Set up upstream/downstream dependencies:
   - Root CI job triggers service jobs
   - Service jobs trigger integration tests
   - Integration tests trigger deployment

### Pipeline Stages (Declarative)

```groovy
pipeline {
    agent { label 'java' }

    stages {
        stage('Checkout') { steps { checkout scm() } }
        stage('Compile') { steps { sh 'mvn clean compile' } }
        stage('Unit Tests') {
            steps { sh 'mvn test' }
            post { always { junit '**/target/surefire-reports/*.xml' } }
        }
        stage('Package') { steps { sh 'mvn package' } }
        stage('Integration Tests') {
            steps { sh 'mvn verify -pl <service> -am -Pit' }
        }
        stage('Build Container') {
            steps { sh 'mvn spring-boot:build-image' }
        }
        stage('Deploy to Test') {
            when { branch 'main' }
            steps { // Deploy logic }
        }
    }
}
```

## 🧩 Multi-Module Build Commands

### Build Specific Service with Dependencies

```bash
# Catalog service (no dependencies)
mvn clean verify -pl services/catalog-service -am

# Inventory service (depends on catalog-service)
mvn clean verify -pl services/inventory-service -am

# Order service (depends on catalog, inventory)
mvn clean verify -pl services/order-service -am
```

### Parallel Builds (Milestone 3+)

```bash
mvn clean verify \
  -T 2C \
  -pl services/catalog-service,services/inventory-service -am
```

## 📝 Code Style and Standards

- **Java Naming**: Package names lowercase, classes camelCase, constants UPPER_SNAKE_CASE
- **Spring Annotations**: `@RestController`, `@Service`, `@Repository`, `@Entity`
- **Exception Handling**: Custom exceptions with error codes (`ProblemDetails`)
- **DTOs**: Separate DTOs for requests and responses (never expose entities)
- **Validation**: Jakarta Bean Validation (`@NotBlank`, `@Size`, etc.)
- **Logging**: Use SLF4J with MDC correlation IDs

## 🐛 Debugging Tips

### View Service Logs

```bash
# Local development
docker logs <container-name>

# Kubernetes
kubectl logs deployment/catalog-service -f
```

### Check Database Connections

```sql
-- PostgreSQL query to check active connections
SELECT
    pid,
    usename,
    application_name,
    client_addr,
    state,
    wait_event_type,
    query
FROM pg_stat_activity
WHERE datname = 'warehouse_catalog';
```

## 📦 Common Issues & Solutions

### Issue: "Could not resolve dependencies"

**Solution**: Check internet connectivity and Maven repository access. Try cleaning:

```bash
mvn clean && rm -rf ~/.m2/repository/org/springframework/boot/*
```

### Issue: "PostgreSQL connection refused"

**Solution**: Ensure PostgreSQL container/service is running:

```bash
docker ps | grep postgres
# or check your local PostgreSQL service
```

### Issue: "BUILD SUCCESS but tests failed in CI"

**Solution**: Check for Testcontainers integration test exclusions. Integration tests may be excluded from default surefire plugin.

## 📚 Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)
- [Project Reactor (for async)](https://projectreactor.io/)
- [Testcontainers Documentation](https://testcontainers.com/)
- [Flyway Migrations](https://flywaydb.org/)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📜 License

Apache License 2.0

## 🆘 Support

For issues, questions, or contributions:

- Review relevant milestone in [DEVELOPMENT_ROADMAP.md](./DEVELOPMENT_ROADMAP.md)
- Check architecture decisions in [ARCHITECTURE.md](./ARCHITECTURE.md)
- See API conventions in [API_CONVENTIONS.md](./API_CONVENTIONS.md)

---

**Status**: Milestone 0 Complete - Foundation Ready
**Next Step**: Begin Milestone 1 (Catalog Service Implementation)
