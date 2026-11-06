# Next Steps - Warehouse Fulfillment Platform

## ✅ Completed: Milestones 0 & 1

### Milestone 0 (Environment & Repository Foundation) — COMPLETE ✅

All deliverables implemented and verified. See `project-state/CURRENT_STATE.md` for full summary.

### Milestone 1 (Catalog Service Business Logic) — COMPLETE ✅

Integration test skeleton fixed, all tests passing:

- ✅ CatalogServiceIntegrationTests.java rewritten with proper @SpringBootTest setup
- ✅ Unique SKUs per test to avoid duplicate key violations
- ✅ DynamicPropertySource for unique H2 database per test class
- ✅ All 7 tests passing (3 unit + 4 integration)
- ✅ Build verification: `mvn clean verify` → BUILD SUCCESS

---

## Milestone 2: Inventory Service Basics (Next Phase)

### Overview

Implement the Inventory Service domain service for stock management, building on the Catalog Service foundation.

### Step 2.1: Create Directories

```bash
mkdir -p services/inventory-service/src/main/java/com/example/warehouse/inventory/domain
mkdir -p services/inventory-service/src/main/java/com/example/warehouse/inventory/presentation/rest/controller
mkdir -p services/inventory-service/src/main/java/com/example/warehouse/inventory/presentation/rest/request
mkdir -p services/inventory-service/src/main/java/com/example/warehouse/inventory/presentation/rest/response
mkdir -p services/inventory-service/src/main/java/com/example/warehouse/inventory/application/service
mkdir -p services/inventory-service/src/main/java/com/example/warehouse/inventory/domain/repository
mkdir -p services/inventory-service/src/test/java/com/example/warehouse/inventory/domain
mkdir -p services/inventory-service/src/test/resources
```

### Step 2.2: Create `services/inventory-service/pom.xml`

- Spring Boot Web starter
- Spring Data JPA starter
- PostgreSQL driver (runtime scope)
- Jakarta Validation API
- OpenAPI/springdoc dependency
- Test dependencies (Spring Boot Test, JUnit 5, Mockito)

### Step 2.3: Create Inventory Domain Model

**`Inventory.java`** - Core entity with:

- UUID PK for inventory record
- SKU reference (to Catalog Service products)
- Quantity on hand (NUMERIC type for precision)
- Reserved quantity
- Available quantity (computed or denormalized)
- Low stock threshold
- updatedAt audit field

**`StockKeepingUnit.java`** - Entity representing unique SKUs in inventory:

- UUID PK
- SKU code (unique, references Catalog product)
- Reorder point
- Unit of measure
- Warehouse location (optional)

Add JPA annotations (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`).

### Step 2.4: Create Inventory Repository

**`InventoryRepository.java`** extends `JpaRepository<Inventory, UUID>` with:

- Custom method to check stock availability for a SKU
- Method to find low stock items below threshold
- Query methods for filtering by warehouse or quantity ranges

### Step 2.5: Create DTOs

**Request DTOs:**

- `CreateInventoryRecordRequest` - For creating new inventory entries
- `UpdateInventoryStockRequest` - For adjusting stock levels
- `ReserveStockRequest` - For reserving quantities

**Response DTOs:**

- `InventoryRecordResponse` - Full inventory record for API responses
- `StockAvailabilityResponse` - Stock status and availability check
- `LowStockAlertResponse` - Low stock notification

Add validation annotations where appropriate.

### Step 2.6: Create InventoryController

Map to `/api/v1/inventory` endpoints:

- GET /inventory - List all inventory records (paginated)
- GET /inventory/sku/{sku} - Get stock status for a SKU
- POST /inventory - Create new inventory record
- PUT /inventory/{id} - Update inventory record
- PATCH /inventory/{id}/stock/adjust - Adjust stock level
- POST /inventory/reserve - Reserve stock for order
- GET /inventory/alerts/low - Get low stock alerts

Add OpenAPI annotations (`@Operation`, `@Parameter`, `@ApiResponse`).

### Step 2.7: Create Application Configuration

- `application.properties` - Base configuration (server.port, datasource placeholders)
- `application-local.properties` - Local development with PostgreSQL connection
- No passwords in committed config; use environment variables or Jenkins secrets

### Step 2.8: Add Flyway Migration for Inventory Service

**`V1__initial_inventory_schema.sql`** in `db/migration/`:

- Creates `inventory_records` table with all columns and constraints
- Creates `stock_keeping_units` table
- Adds unique index on SKU
- Includes audit column defaults (createdAt, updatedAt)
- Add trigger or view for available quantity computation if needed

### Step 2.9: Create Unit Tests

**`InventoryRepositoryTest.java`:**

```java
@SpringBootTest
@ActiveProfiles("test")
class InventoryRepositoryTest {
    @Autowired
    private InventoryRepository repository;

    // Test stock availability checks
    // Test low stock detection
    // Test CRUD operations
}
```

Target: 100% domain layer coverage.

### Step 2.10: Create Integration Tests

**`InventoryServiceIntegrationTests.java`:**

```java
@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceIntegrationTests {
    @Autowired
    private InventoryRepository inventoryRepository;

    // Test stock reservation logic
    // Test concurrent stock adjustments (optimistic locking)
    // Test low stock alerts
}
```

Target: 80%+ application layer coverage.

### Step 2.11: Create ADR for Inventory Service Design

**`docs/adr/adr-002-inventory-service-selection.md`:**

- Rationale for separate Inventory Service vs monolithic design
- Database schema decisions (separate tables vs single inventory table)
- Concurrency strategy for stock updates
- API contract definitions

---

## Phase 3: Create Jenkinsfile Updates (For Milestone 2)

Update root `Jenkinsfile` to include catalog-service builds in multi-module pipeline.

---

## Exit Criteria for Milestone 2 (Inventory Service Basics)

Before marking Milestone 2 complete, ensure all roadmap criteria below are implemented with their corresponding implementation steps added to the implementation checklist before using this checklist as the completion gate:

### ✅ Implementation Steps (Roadmap)

1. **Release Management**
   - Implement versioning strategy for inventory records
   - Add soft-delete capability with `deletedAt` audit field
   - Create retention policies for archived inventory data

2. **Idempotency**
   - Add idempotency keys to stock adjustment endpoints
   - Implement cache invalidation for stale inventory data
   - Ensure concurrent requests don't cause duplicate operations

3. **Concurrent Oversell Protection**
   - Add optimistic locking with `@Version` annotation on inventory table
   - Implement database-level row locks for critical stock operations
   - Add retry logic for failed concurrent updates with exponential backoff

4. **PostgreSQL Integration Tests**
   - Replace H2 in-memory DB with Testcontainers PostgreSQL for integration tests
   - Add schema validation tests against Flyway migrations
   - Verify migration compatibility across PostgreSQL versions

5. **Role Enforcement**
   - Implement service-level security filters for inventory endpoints
   - Add audit logging for stock adjustments and reservations
   - Create role-based access control (RBAC) for admin vs operational users

### Verification Checklist

- [ ] `mvn clean verify -pl services/inventory-service -am` succeeds locally
- [ ] Health endpoint returns `"status":"UP"` at `/actuator/health`
- [ ] Inventory records can be created via POST to `/api/v1/inventory`
- [ ] Stock availability check works: GET /inventory/sku/{sku}
- [ ] Unit tests pass (surefire-reports/ contains test results)
- [ ] Integration tests pass with Spring Boot context
- [ ] JAR artifact built in `inventory-service/target/*.jar`
- [ ] ADR documented architecture decisions

---

## Exit Criteria for Milestone 1 (Current Session - Verified ✅)

Before marking current session complete:

- [x] `mvn clean verify -pl services/catalog-service -am` succeeds locally
- [x] All unit tests pass (3 tests in ProductRepositoryTest)
- [x] All integration tests pass (4 tests in CatalogServiceIntegrationTests)
- [x] Checkpoint files updated: COMPLETED_TASKS.md, CURRENT_STATE.md, NEXT_STEPS.md
- [x] Graphify used for codebase analysis (graphify update executed)

---

## Session Status

**Current Milestone:** Milestone 1 — COMPLETE (PR review fixes applied)
**Build Status:** ✅ PASSING (Catalog Service verified - all PR comments addressed)
**Graphify Usage:** ✅ Used for codebase analysis and graph update

### PR Review Fixes Applied (codeRabbitAI comments):

1. **H2 Database URL Stability** — Changed from `System.nanoTime()` to timestamp-based unique ID stored in static final field:

   ```java
   private static final String TESTDB_URL = "jdbc:h2:mem:testdb-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
   ```

2. **Test Isolation for Active Product Count** — Added `repository.deleteAll()` at start of `should_find_all_active_products()` to ensure isolation from other tests (JUnit doesn't guarantee source-order execution).

**Next Action:** PR ready to merge into develop branch. Awaiting instruction to proceed with Milestone 2 (Inventory Service) or review current state.

---

**Last Updated:** 2026-11-06 17:26 PKT
