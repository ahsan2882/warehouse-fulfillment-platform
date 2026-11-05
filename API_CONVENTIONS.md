# Warehouse Fulfillment Platform — API Conventions

**Status:** API baseline
**Version:** 1.0
**Style:** REST over HTTP/JSON

## 1. General principles

- Use resource-oriented endpoints and plural nouns.
- Use JSON for request and response bodies unless a specific endpoint requires another media type.
- Version APIs under `/api/v1`.
- Keep APIs owned and implemented by the service that owns the resource.
- Use DTOs, never expose JPA entities.
- Document all public endpoints in OpenAPI.
- Validate all untrusted input.
- Return consistent errors using Problem Details (RFC 9457).
- Use idempotency for commands that may be retried.

## 2. URL and naming conventions

Preferred:

```text
GET    /api/v1/products
POST   /api/v1/products
GET    /api/v1/products/{productId}
PATCH  /api/v1/products/{productId}

GET    /api/v1/orders/{orderId}
POST   /api/v1/orders
POST   /api/v1/orders/{orderId}/cancel

POST   /api/v1/reservations
POST   /api/v1/reservations/{reservationId}/release
```

Avoid verbs in ordinary resource paths. Explicit action subresources are acceptable for domain commands such as `cancel`, `reserve`, `release`, `pick`, `pack`, and `dispatch`.

Use lowercase kebab-case for multiword path segments. Use camelCase for JSON properties.

## 3. HTTP methods and status codes

| Method   | Intended use                                       |
| -------- | -------------------------------------------------- |
| `GET`    | Read a resource or collection; safe and idempotent |
| `POST`   | Create a resource or execute a domain command      |
| `PUT`    | Replace a resource representation                  |
| `PATCH`  | Partially update a resource                        |
| `DELETE` | Delete only when domain rules permit it            |

Common success codes:

| Status | Use                                                   |
| ------ | ----------------------------------------------------- |
| 200    | Successful read or command returning a representation |
| 201    | Resource created; include `Location` when appropriate |
| 202    | Accepted for asynchronous processing                  |
| 204    | Successful operation with no response body            |

Common error codes:

| Status | Use                                                                                                      |
| ------ | -------------------------------------------------------------------------------------------------------- |
| 400    | Malformed request or validation failure                                                                  |
| 401    | Missing or invalid authentication                                                                        |
| 403    | Authenticated but unauthorized                                                                           |
| 404    | Resource not found                                                                                       |
| 409    | Business conflict, invalid state transition, duplicate/idempotency conflict                              |
| 415    | Unsupported media type                                                                                   |
| 422    | Optional use for syntactically valid but semantically unprocessable content; use consistently if adopted |
| 429    | Rate limit exceeded                                                                                      |
| 500    | Unexpected internal failure                                                                              |
| 503    | Required dependency unavailable                                                                          |

Do not use different status codes for the same category of failure without a documented reason.

## 4. DTO rules

Use separate DTOs for requests and responses.

Examples:

- `CreateProductRequest`
- `UpdateProductRequest`
- `ProductResponse`
- `CreateOrderRequest`
- `OrderResponse`
- `ReserveInventoryRequest`
- `ReservationResponse`

Rules:

1. Do not serialize JPA entities.
2. Do not accept server-generated IDs, audit timestamps, or lifecycle state from clients unless the use case explicitly allows it.
3. Validate with Jakarta Bean Validation.
4. Use immutable DTOs where practical; Java records are suitable.
5. Keep mapping outside controllers.
6. Avoid reusing one DTO for create, update, and response operations.
7. Do not leak internal entity relationships or persistence details.

Example:

```java
public record CreateProductRequest(
    @NotBlank @Size(max = 80) String sku,
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    @NotBlank @Size(max = 30) String unitOfMeasure
) {}
```

## 5. Validation

Validate at the API boundary and enforce domain invariants in the domain/application layer as well.

Examples:

- SKU and product name must not be blank.
- Quantities must be greater than zero.
- Requested order lines must not be empty.
- Duplicate product lines should be rejected or consolidated according to a documented rule.
- IDs must be valid UUIDs.
- State transitions must be validated by the owning service.

Do not trust frontend validation.

## 6. Error format

Use RFC 9457 Problem Details. A representative response:

```json
{
  "type": "https://example.com/problems/insufficient-inventory",
  "title": "Insufficient inventory",
  "status": 409,
  "detail": "The requested quantity is not currently available.",
  "instance": "/api/v1/reservations",
  "code": "INVENTORY_INSUFFICIENT",
  "traceId": "8b8e8d6a-9f8a-4f65-9c53-7f0b2c0e0b31"
}
```

For validation errors, include a stable list of field errors, for example:

```json
{
  "type": "https://example.com/problems/validation",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "code": "VALIDATION_FAILED",
  "fieldErrors": [
    {
      "field": "quantity",
      "message": "must be greater than zero"
    }
  ]
}
```

Requirements:

- Use stable machine-readable error codes.
- Do not return stack traces, SQL, internal hostnames, credentials, or implementation details.
- Log diagnostic information server-side.
- Include a correlation/trace identifier where available.
- Use a global exception-handling mechanism such as `@RestControllerAdvice`.

## 7. Pagination, filtering, and sorting

For collection endpoints, use a consistent convention:

```text
GET /api/v1/products?page=0&size=20&sort=name,asc
```

Recommended response:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

Set a maximum page size. Whitelist sortable fields; never concatenate arbitrary client-supplied sort expressions into SQL.

Use explicit filters where useful, for example:

```text
GET /api/v1/orders?status=ALLOCATED&createdAfter=2026-11-01T00:00:00Z
```

## 8. Idempotency

Commands such as order creation and inventory reservation may be retried after network failures.

For supported operations:

- Accept an `Idempotency-Key` request header.
- Scope keys to the authenticated caller and operation where necessary.
- Persist the key and operation result or a durable reference.
- Return the original result for a valid replay.
- Reject reuse of the same key with a materially different request.
- Define key retention and expiry.

Do not assume that a client timeout means the server did not complete the operation.

## 9. Authentication and authorization

Protected endpoints use:

```http
Authorization: Bearer <access-token>
```

JWT validation must check signature, issuer, expiry, and relevant claims. Use role/authority checks at the service boundary.

Suggested role-to-operation mapping:

| Role                   | Example permissions                          |
| ---------------------- | -------------------------------------------- |
| `ADMIN`                | Administration and operational configuration |
| `INVENTORY_OPERATOR`   | Receive, adjust, and view inventory          |
| `FULFILLMENT_OPERATOR` | View assigned tasks, pick, and pack          |
| `SHIPPING_OPERATOR`    | Create shipments and update shipping status  |
| `ORDER_MANAGER`        | Create, view, and cancel orders              |
| `READ_ONLY`            | Read permitted resources                     |

Return 401 for missing/invalid authentication and 403 for an authenticated user who lacks permission. Avoid relying solely on gateway authorization.

## 10. Service-to-service HTTP

- Use service-specific client components, not raw HTTP calls scattered across business classes.
- Configure connection and response timeouts.
- Propagate correlation IDs.
- Propagate user identity/authorization only through an explicitly designed mechanism.
- Use service credentials or workload identity for machine-to-machine calls where appropriate.
- Retry only safe/idempotent operations, or commands protected by idempotency.
- Map downstream errors into stable local error categories.
- Do not expose internal downstream response bodies directly to external clients.

## 11. API documentation and contract tests

- Generate OpenAPI documentation for each service.
- Document examples, constraints, security, errors, and pagination.
- Treat response fields and status codes as contracts.
- Add provider/consumer contract tests when services begin depending on one another.
- Review breaking changes before changing a shared contract.
- Consider Pact only when a real consumer/provider relationship exists.

## 12. Initial endpoint inventory

| Service     | Method | Endpoint                              | Purpose              |
| ----------- | ------ | ------------------------------------- | -------------------- |
| Catalog     | POST   | `/api/v1/products`                    | Create product       |
| Catalog     | GET    | `/api/v1/products/{id}`               | Retrieve product     |
| Catalog     | GET    | `/api/v1/products`                    | List/search products |
| Catalog     | PATCH  | `/api/v1/products/{id}`               | Update product       |
| Inventory   | POST   | `/api/v1/stock-receipts`              | Receive stock        |
| Inventory   | GET    | `/api/v1/inventory/{productId}`       | View stock           |
| Inventory   | POST   | `/api/v1/reservations`                | Reserve stock        |
| Inventory   | POST   | `/api/v1/reservations/{id}/release`   | Release reservation  |
| Order       | POST   | `/api/v1/orders`                      | Create order         |
| Order       | GET    | `/api/v1/orders/{id}`                 | Retrieve order       |
| Order       | POST   | `/api/v1/orders/{id}/cancel`          | Cancel order         |
| Fulfillment | GET    | `/api/v1/fulfillment/tasks`           | List tasks           |
| Fulfillment | POST   | `/api/v1/fulfillment/tasks/{id}/pick` | Record picking       |
| Fulfillment | POST   | `/api/v1/fulfillment/tasks/{id}/pack` | Complete packing     |
| Shipping    | POST   | `/api/v1/shipments`                   | Create shipment      |
| Shipping    | GET    | `/api/v1/shipments/{id}`              | Retrieve shipment    |
| Shipping    | POST   | `/api/v1/shipments/{id}/dispatch`     | Dispatch shipment    |

The endpoint inventory is a starting contract. Final request/response schemas should be defined as each milestone is implemented.
