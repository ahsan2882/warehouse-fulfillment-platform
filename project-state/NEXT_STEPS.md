# Next Steps - Warehouse Fulfillment Platform

## ✅ Completed: Milestone 0 (Environment & Repository Foundation)

All deliverables implemented and verified. See `project-state/CURRENT_STATE.md` for full summary.

---

## Milestone 1: Catalog Service Business Logic (Next Phase)

#### Step 1.1: Create Directories

```bash
mkdir -p services/catalog-service/src/main/java/com/example/warehouse/catalog/domain
mkdir -p services/catalog-service/src/main/java/com/example/warehouse/catalog/presentation/rest/controller
mkdir -p services/catalog-service/src/main/java/com/example/warehouse/catalog/presentation/rest/request
mkdir -p services/catalog-service/src/main/java/com/example/warehouse/catalog/presentation/rest/response
mkdir -p services/catalog-service/src/main/java/com/example/warehouse/catalog/application/service
mkdir -p services/catalog-service/src/main/java/com/example/warehouse/catalog/domain/repository
mkdir -p services/catalog-service/src/main/resources/db/migration
mkdir -p services/catalog-service/src/test/java/com/example/warehouse/catalog/domain
mkdir -p services/catalog-service/src/test/java/com/example/warehouse/catalog/presentation/rest/controller
mkdir -p services/catalog-service/src/test/resources
mkdir -p docs/adr
```

#### Step 1.2: Create Root `pom.xml`

- Configure Java 21 properties
- Add Spring Boot BOM for dependency management
- Configure Maven compiler, Surefire, and Spring Boot plugins

#### Step 1.3: Update `.gitignore`

Add missing patterns:

- IDE configurations (`.idea/`, `.vscode/`) - already present but verify
- Maven wrapper directory (`.mvn/wrapper/`)
- IntelliJ workspace files

### Phase 2: Implement Catalog Service Basics

#### Step 2.1: Create `catalog-service/pom.xml`

- Spring Boot Web starter
- Spring Data JPA starter
- PostgreSQL driver (runtime scope)
- Jakarta Validation API
- OpenAPI/springdoc dependency
- Test dependencies (Spring Boot Test, JUnit 5, Mockito)

#### Step 2.2: Create Product Domain Model

- `Product.java` - Entity with UUID PK, SKU, name, description, unitOfMeasure, active flag
- `ProductId.java` or use UUID directly for ID
- Add JPA annotations (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`)
- Add audit fields (`@CreatedDate` mapped to `created_at`)

#### Step 2.3: Create Product Repository

- `ProductRepository.java` extends `JpaRepository<Product, UUID>`
- Add custom method for active product listing (optional)

#### Step 2.4: Create DTOs

- `CreateProductRequest.java` - Request DTO with validation annotations
- `UpdateProductRequest.java` - Partial update request DTO
- `ProductResponse.java` - Response DTO exposing entity fields

#### Step 2.5: Create ProductController

- Map to `/api/v1/products` endpoints
- Implement create (POST), get by ID (GET), list (GET), update (PATCH)
- Add OpenAPI annotations (`@Operation`, `@Parameter`, `@ApiResponse`)

#### Step 2.6: Create Application Configuration

- `application.properties` - Base configuration (server.port, datasource placeholders)
- `application-local.properties` - Local development with PostgreSQL connection
- No passwords in committed config; use environment variables or Jenkins secrets

#### Step 2.7: Add Actuator Health Endpoint

- Configure Spring Boot Actuator
- Enable health check at `/actuator/health`

#### Step 2.8: Create Initial Flyway Migration

- `V1__initial_schema.sql` in `db/migration/`
- Creates `products` table with all columns and constraints
- Adds unique index on `sku`
- Includes audit column defaults

### Phase 3: Implement Tests

#### Step 3.1: Unit Test for Product Repository (Milestone 0)

```java
@SpringBootTest
class ProductRepositoryTest {
    @Autowired
    private ProductRepository repository;

    @Test
    void should_save_product_with_unique_sku() {
        // Create product with SKU "PROD-001"
        // Verify repository saves successfully
    }
}
```

#### Step 3.2: Integration Test (Milestone 1+)

```java
@SpringBootTest
@Testcontainers
class CatalogServiceIntegrationTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    // Test REST endpoints against real PostgreSQL
}
```

### Phase 4: Create Jenkinsfile (Initial)

#### Step 4.1: Declarative Pipeline

```groovy
pipeline {
    agent any

    stages {
        stage('Checkout') { steps { checkout scm() } }
        stage('Compile') { steps { sh 'mvn -B clean compile' } }
        stage('Unit Tests') {
            steps { sh 'mvn -B test' }
            post { always { junit '**/target/surefire-reports/*.xml' } }
        }
        stage('Package') { steps { sh 'mvn -B package' } }
    }

    post {
        success { archiveArtifacts artifacts: '**/catalog-service/target/*.jar' }
        failure { echo 'Build failed. Check logs.' }
        always { cleanWs() }
    }
}
```

#### Step 4.2: Configure Jenkins Agents

- Create agent with `java` label (Maven, JDK 21)
- Test pipeline manually before integrating with repository webhooks

### Phase 5: Verification

#### Step 5.1: Local Build Verification

```bash
cd /mnt/drive1/Projects/warehouse-fulfillment-platform
mvn clean verify -pl services/catalog-service -am
# Expected: BUILD SUCCESS
```

#### Step 5.2: Run Catalog Service Locally

```bash
cd services/catalog-service
mvn spring-boot:run -pl . -am
# Verify health endpoint at http://localhost:8080/actuator/health
```

#### Step 5.3: API Contract Test

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"PROD-001","name":"Widget","unitOfMeasure":"pcs"}'
# Expected: 201 Created with ProductResponse in body
```

---

## Exit Criteria for Milestone 0

Before marking Milestone 0 complete:

- [ ] `mvn clean verify -pl services/catalog-service -am` succeeds locally
- [ ] Health endpoint returns `"status":"UP"` at `/actuator/health`
- [ ] Product can be created via POST to `/api/v1/products`
- [ ] Unit tests pass (`surefire-reports/` contains test results)
- [ ] JAR artifact built in `catalog-service/target/catalog-service-0.1.0-SNAPSHOT.jar`
- [ ] Jenkins pipeline runs successfully on CI
- [ ] Documentation updated with build instructions

---

## After Milestone 0: Begin Milestone 1

Upon successful completion of Milestone 0, proceed to implement Catalog Service business logic per `DEVELOPMENT_ROADMAP.md` section 4.

---

**Last Updated:** 2026-11-05
**Current Session Status:** Environment assessment complete, awaiting approval for implementation
**Next Action:** Await user approval to create root pom.xml and first service module
