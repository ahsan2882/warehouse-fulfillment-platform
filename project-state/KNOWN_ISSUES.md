# Known Issues & Technical Debt - Warehouse Fulfillment Platform

## Identified Risks (Pre-Implementation)

### 1. Cross-Service Coupling Risk [HIGH]

**Issue:** Services may inadvertently create cross-database foreign keys or direct SQL joins.
**Mitigation:** Enforce database ownership principle; store external identifiers as UUID scalars only; document API contracts before implementation.

### 2. Inventory Overselling Under Concurrency [CRITICAL]

**Issue:** Concurrent reservation requests can oversell stock if optimistic locking not properly implemented.
**Mitigation:** Implement optimistic locking (`version` column) immediately; test with 100+ concurrent requests in integration tests.

### 3. Maven Multi-Module Complexity [MEDIUM]

**Issue:** Adding all microservices at once creates unnecessary complexity before first workflow is understood.
**Mitigation:** Build ONE service first (Catalog); add modules incrementally as milestones dictate.

### 4. Jenkins Controller Overloading [HIGH]

**Issue:** Routine builds on controller executors slow down CI/CD operations and affect scheduled jobs.
**Mitigation:** Use labeled agents (`java`, `integration-test`, `docker`) from Milestone 0; never run builds on controller.

### 5. Secret Management [CRITICAL]

**Issue:** Hardcoding secrets in config files or committing `.env` files with passwords/keys.
**Mitigation:** Use Jenkins Credentials for ALL secrets (Git, registry, deployment); never commit `.env`; use environment-specific profiles.

### 6. Testcontainers Resource Exhaustion [MEDIUM]

**Issue:** Running multiple integration tests concurrently can exhaust CPU/memory/Docker capacity on workstation.
**Mitigation:** Run integration tests sequentially or with strict concurrency limits; use agent labels to throttle.

## Missing Configuration Items (Milestone 0)

### Not Yet Created

- [ ] Root `pom.xml` - Maven multi-module parent
- [ ] `services/catalog-service/pom.xml` - Service POM
- [ ] `infrastructure/` directory structure
- [ ] `docs/adr/` directory for Architecture Decision Records

### Needs Updates

- `.gitignore` - Missing IDE (IntelliJ, Eclipse) entries; Maven wrapper config
- No `.mvn/wrapper/maven-wrapper.jar` - Consider adding Maven Wrapper for reproducibility

## Technical Debt to Avoid

| Anti-Pattern                               | Rule                                                                                  |
| ------------------------------------------ | ------------------------------------------------------------------------------------- |
| Catch-all exception handling               | Use specific exception types; never catch `Exception` without rethrowing with context |
| Hibernate schema auto-update in production | Use Flyway/Liquibase migrations only                                                  |
| Floating-point quantities for stock        | Use `NUMERIC` type exclusively                                                        |
| Cross-service foreign keys                 | Never create; use external ID references only                                         |
| Shared domain modules                      | Avoid tight coupling; prefer DTO duplication over shared entities                     |

---

**Last Updated:** 2026-11-05
**Status:** Documented before Milestone 0 implementation begins
