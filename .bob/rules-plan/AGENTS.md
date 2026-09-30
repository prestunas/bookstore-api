# Plan Mode Architectural Constraints

This file provides guidance to agents when working with code in this repository.

- **Schema is owned by Flyway** (`ddl-auto=validate`). Every schema change requires a new versioned migration in `src/main/resources/db/migration/`. Never alter already-applied migrations.
- **`Clock` is a shared UTC bean** — any feature involving time-based business rules (e.g., cancellation window) must accept `Clock` as a dependency to remain testable.
- **`BookstoreProperties`** is the single source of truth for configurable business rules. Adding a new rule means adding a property there and wiring it via env var — never hardcoding in service logic.
- **Security filter chain** is defined entirely in `SecurityConfig`. Public vs. protected endpoint policy is centralised there; do not add `@PreAuthorize` or per-method security without first checking whether the config already handles it.
- **No MapStruct mappers exist yet** — if adding one, configure `componentModel = "spring"` and keep in mind the mandatory annotation processor order in `pom.xml`.
- **Integration tests hit real Postgres** — any feature requiring integration test coverage needs a local or CI Postgres instance. There is no embedded DB fallback.
- **JWT claims**: `sub` = username, `userId` = UUID string, `role` = role string. Downstream code reads these specific claim names — do not rename.
- **Reward-point accounting** is done synchronously in `OrderService`; cancellation reverses points. Any change to order lifecycle must account for point balance consistency (see `CustomerJourneyIntegrationTest` edge-case comments).
