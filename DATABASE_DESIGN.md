# Warehouse Fulfillment Platform — Database Design

**Status:** Logical database baseline
**Version:** 1.0
**Database:** PostgreSQL

## 1. Database ownership principles

Each microservice owns its data and is the only service allowed to modify it.

For local development, the services may use one PostgreSQL server with separate databases. Separate schemas and database users are an acceptable early local-development compromise, provided permissions enforce ownership.

| Service     | Suggested database      | Owns                                         |
| ----------- | ----------------------- | -------------------------------------------- |
| Catalog     | `warehouse_catalog`     | Products                                     |
| Inventory   | `warehouse_inventory`   | Locations, balances, reservations, movements |
| Order       | `warehouse_orders`      | Orders, lines, status history                |
| Fulfillment | `warehouse_fulfillment` | Tasks, picks, packages                       |
| Shipping    | `warehouse_shipping`    | Shipments and status history                 |
| Identity    | `warehouse_identity`    | Users, roles, assignments                    |

Never create cross-service foreign keys or cross-service SQL joins. Store external identifiers as UUID values and retrieve remote details through APIs or events.

## 2. Data types and conventions

- Primary keys: UUID.
- Quantities: `NUMERIC`, not floating point.
- Money, if introduced: `NUMERIC(precision, scale)` with an explicit currency.
- Timestamps: `TIMESTAMPTZ`, stored and exchanged in UTC.
- Lifecycle states: constrained strings or PostgreSQL enums; choose one approach consistently and account for migration complexity.
- Boolean flags: `BOOLEAN NOT NULL` with explicit defaults where appropriate.
- Add `created_at` and `updated_at` to mutable business entities.
- Add indexes based on real query patterns.
- Use Flyway or Liquibase for schema migrations.
- Do not use Hibernate schema auto-update in shared or production environments.

Recommended common audit fields:

```sql
created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
```

`updated_at` must be maintained by application logic or a deliberately adopted database mechanism.

## 3. Catalog Service schema

### 3.1 `products`

| Column            | Type         | Constraints            |
| ----------------- | ------------ | ---------------------- |
| `id`              | UUID         | Primary key            |
| `sku`             | VARCHAR(80)  | Not null, unique       |
| `name`            | VARCHAR(200) | Not null               |
| `description`     | TEXT         | Nullable               |
| `unit_of_measure` | VARCHAR(30)  | Not null               |
| `active`          | BOOLEAN      | Not null, default true |
| `created_at`      | TIMESTAMPTZ  | Not null               |
| `updated_at`      | TIMESTAMPTZ  | Not null               |

Indexes:

- Unique index on `sku`.
- Optional index on `active` if active-product filtering is frequent.

Products should be deactivated rather than physically deleted after being referenced by orders or inventory.

## 4. Inventory Service schema

### 4.1 `warehouse_locations`

| Column       | Type        | Constraints            |
| ------------ | ----------- | ---------------------- |
| `id`         | UUID        | Primary key            |
| `code`       | VARCHAR(80) | Not null, unique       |
| `zone`       | VARCHAR(40) | Nullable               |
| `aisle`      | VARCHAR(40) | Nullable               |
| `rack`       | VARCHAR(40) | Nullable               |
| `shelf`      | VARCHAR(40) | Nullable               |
| `bin`        | VARCHAR(40) | Nullable               |
| `active`     | BOOLEAN     | Not null, default true |
| `created_at` | TIMESTAMPTZ | Not null               |
| `updated_at` | TIMESTAMPTZ | Not null               |

### 4.2 `stock_balances`

| Column              | Type          | Constraints                          |
| ------------------- | ------------- | ------------------------------------ |
| `id`                | UUID          | Primary key                          |
| `product_id`        | UUID          | External Catalog Service identifier  |
| `location_id`       | UUID          | FK to local `warehouse_locations.id` |
| `on_hand_quantity`  | NUMERIC(14,3) | Not null, >= 0                       |
| `reserved_quantity` | NUMERIC(14,3) | Not null, >= 0                       |
| `version`           | BIGINT        | Not null, optimistic locking         |
| `created_at`        | TIMESTAMPTZ   | Not null                             |
| `updated_at`        | TIMESTAMPTZ   | Not null                             |

Constraints:

- Unique `(product_id, location_id)`.
- `on_hand_quantity >= 0`.
- `reserved_quantity >= 0`.
- `reserved_quantity <= on_hand_quantity`.

Available quantity is calculated as:

```text
available_quantity = on_hand_quantity - reserved_quantity
```

Do not store available quantity independently unless a documented performance requirement justifies denormalization.

### 4.3 `inventory_reservations`

| Column            | Type          | Constraints                         |
| ----------------- | ------------- | ----------------------------------- |
| `id`              | UUID          | Primary key                         |
| `order_id`        | UUID          | External Order Service identifier   |
| `product_id`      | UUID          | External Catalog Service identifier |
| `location_id`     | UUID          | Local location reference            |
| `quantity`        | NUMERIC(14,3) | Not null, > 0                       |
| `status`          | VARCHAR(30)   | Not null                            |
| `idempotency_key` | VARCHAR(120)  | Not null                            |
| `created_at`      | TIMESTAMPTZ   | Not null                            |
| `released_at`     | TIMESTAMPTZ   | Nullable                            |

Suggested statuses: `ACTIVE`, `RELEASED`, `CONSUMED`, `CANCELLED`.

Use an appropriate uniqueness rule for idempotency keys. Scope the key to the caller/operation if keys are not globally unique.

### 4.4 `inventory_movements`

| Column           | Type          | Constraints                          |
| ---------------- | ------------- | ------------------------------------ |
| `id`             | UUID          | Primary key                          |
| `product_id`     | UUID          | External Catalog Service identifier  |
| `location_id`    | UUID          | Local location reference             |
| `movement_type`  | VARCHAR(40)   | Not null                             |
| `quantity`       | NUMERIC(14,3) | Not null, > 0                        |
| `reference_type` | VARCHAR(40)   | Not null                             |
| `reference_id`   | UUID          | Not null                             |
| `reason`         | TEXT          | Nullable                             |
| `actor_id`       | UUID          | External Identity Service identifier |
| `created_at`     | TIMESTAMPTZ   | Not null                             |

Movements should be append-only from the application's perspective. Correct a mistake with a compensating movement rather than rewriting historical movement records.

Possible movement types: `RECEIPT`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `PICK`, `RETURN`, `TRANSFER_IN`, `TRANSFER_OUT`.

## 5. Order Service schema

### 5.1 `orders`

| Column               | Type         | Constraints                          |
| -------------------- | ------------ | ------------------------------------ |
| `id`                 | UUID         | Primary key                          |
| `order_number`       | VARCHAR(40)  | Not null, unique                     |
| `status`             | VARCHAR(30)  | Not null                             |
| `idempotency_key`    | VARCHAR(120) | Nullable/unique within defined scope |
| `customer_reference` | VARCHAR(120) | Nullable                             |
| `created_at`         | TIMESTAMPTZ  | Not null                             |
| `updated_at`         | TIMESTAMPTZ  | Not null                             |

Suggested states: `PENDING`, `CONFIRMED`, `ALLOCATED`, `FULFILLING`, `READY_TO_SHIP`, `SHIPPED`, `COMPLETED`, `ON_HOLD`, `CANCELLED`.

### 5.2 `order_lines`

| Column                  | Type          | Constraints                         |
| ----------------------- | ------------- | ----------------------------------- |
| `id`                    | UUID          | Primary key                         |
| `order_id`              | UUID          | FK to local `orders.id`             |
| `product_id`            | UUID          | External Catalog Service identifier |
| `sku_snapshot`          | VARCHAR(80)   | Not null                            |
| `product_name_snapshot` | VARCHAR(200)  | Not null                            |
| `requested_quantity`    | NUMERIC(14,3) | Not null, > 0                       |
| `unit_of_measure`       | VARCHAR(30)   | Not null                            |

One order has many order lines. Product name and SKU are snapshots to preserve historical meaning if Catalog data changes.

### 5.3 `order_status_history`

| Column            | Type        | Constraints                          |
| ----------------- | ----------- | ------------------------------------ |
| `id`              | UUID        | Primary key                          |
| `order_id`        | UUID        | FK to local `orders.id`              |
| `previous_status` | VARCHAR(30) | Nullable for initial state           |
| `new_status`      | VARCHAR(30) | Not null                             |
| `reason`          | TEXT        | Nullable                             |
| `actor_id`        | UUID        | External Identity Service identifier |
| `created_at`      | TIMESTAMPTZ | Not null                             |

Status history is append-only and should be written in the same transaction as the status update.

## 6. Fulfillment Service schema

### 6.1 `fulfillment_tasks`

| Column             | Type        | Constraints                          |
| ------------------ | ----------- | ------------------------------------ |
| `id`               | UUID        | Primary key                          |
| `order_id`         | UUID        | External Order Service identifier    |
| `status`           | VARCHAR(30) | Not null                             |
| `assigned_user_id` | UUID        | External Identity Service identifier |
| `created_at`       | TIMESTAMPTZ | Not null                             |
| `completed_at`     | TIMESTAMPTZ | Nullable                             |

### 6.2 `pick_records`

Store one or more records per task/order line as required by the picking workflow.

| Column               | Type          | Constraints                         |
| -------------------- | ------------- | ----------------------------------- |
| `id`                 | UUID          | Primary key                         |
| `task_id`            | UUID          | FK to local `fulfillment_tasks.id`  |
| `order_line_id`      | UUID          | External Order Service identifier   |
| `product_id`         | UUID          | External Catalog Service identifier |
| `requested_quantity` | NUMERIC(14,3) | Not null                            |
| `picked_quantity`    | NUMERIC(14,3) | Not null, >= 0                      |
| `exception_reason`   | TEXT          | Nullable                            |
| `created_at`         | TIMESTAMPTZ   | Not null                            |

### 6.3 `packages` and `package_lines`

`packages` stores package identity, order reference, dimensions/weight if required, and packing time. `package_lines` stores the order-line/product references and packed quantities.

The package belongs to Fulfillment Service. Shipping Service stores the package ID as an external identifier; it does not create a cross-database foreign key.

## 7. Shipping Service schema

### 7.1 `shipments`

| Column               | Type         | Constraints                             |
| -------------------- | ------------ | --------------------------------------- |
| `id`                 | UUID         | Primary key                             |
| `order_id`           | UUID         | External Order Service identifier       |
| `package_id`         | UUID         | External Fulfillment Service identifier |
| `carrier`            | VARCHAR(100) | Nullable initially                      |
| `tracking_reference` | VARCHAR(150) | Nullable                                |
| `status`             | VARCHAR(30)  | Not null                                |
| `dispatched_at`      | TIMESTAMPTZ  | Nullable                                |
| `delivered_at`       | TIMESTAMPTZ  | Nullable                                |
| `created_at`         | TIMESTAMPTZ  | Not null                                |
| `updated_at`         | TIMESTAMPTZ  | Not null                                |

### 7.2 `shipment_status_history`

Append-only status history with shipment ID, previous status, new status, actor ID, reason, and timestamp.

Suggested statuses: `CREATED`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`, `FAILED_DELIVERY`, `RETURNED`, `CANCELLED`.

## 8. Identity Service schema

Initial entities:

- `users`: UUID, username/email, password hash, active flag, timestamps.
- `roles`: UUID, unique role code, description.
- `user_roles`: user ID, role ID, assignment timestamp.

Enforce uniqueness on normalized username/email and on `(user_id, role_id)`. Never store plaintext passwords.

## 9. Relationship overview

Relationships below are logical. A relationship crossing a service boundary is an identifier reference, not a database foreign key.

```text
Catalog:
Product

Inventory:
WarehouseLocation 1 ---- * StockBalance
Product (external ID) --- * StockBalance
Order (external ID) ----- * InventoryReservation
StockBalance             -> InventoryMovement history

Order:
Order 1 ---- * OrderLine
Order 1 ---- * OrderStatusHistory
Product (external ID) --- * OrderLine

Fulfillment:
FulfillmentTask 1 ---- * PickRecord
FulfillmentTask 1 ---- * Package
Package 1 ---- * PackageLine
Order/Product/OrderLine references are external IDs

Shipping:
Shipment -> Order ID (external)
Shipment -> Package ID (external)
Shipment 1 ---- * ShipmentStatusHistory
```

## 10. Concurrency and inventory correctness

Inventory is the most concurrency-sensitive service.

A reservation must be atomic. The implementation must ensure that concurrent requests cannot reserve more than available stock.

Possible strategies:

- Pessimistic row locking with `SELECT ... FOR UPDATE`.
- Optimistic locking with a version column and retry/conflict handling.
- Conditional atomic updates that verify sufficient availability.

Choose one strategy initially and prove it with PostgreSQL integration tests. Do not rely on a read-then-write sequence without concurrency control.

For a multi-line reservation, all lines must succeed or the service must compensate/revert every reservation created within that operation. The operation must be idempotent.

## 11. Indexing baseline

Add indexes for demonstrated query patterns, including:

- Product SKU unique lookup.
- Location code unique lookup.
- Stock balance `(product_id, location_id)` unique lookup.
- Reservation `(order_id, status)`.
- Movement `(product_id, created_at)`.
- Order number unique lookup.
- Orders `(status, created_at)`.
- Order lines `(order_id)`.
- Fulfillment tasks `(status, assigned_user_id)`.
- Shipments `(order_id)` and tracking reference where applicable.

Use `EXPLAIN ANALYZE` before adding speculative indexes.

## 12. Migration and retention rules

- Keep migrations in each service's own resources directory.
- Never share a migration history table between services.
- Test migrations against PostgreSQL.
- Do not edit an already-applied migration; create a new migration.
- Preserve order, inventory movement, and status history according to an explicit retention policy.
- Do not hard-delete operational records merely to simplify CRUD behavior.
