# Ask Mode Context

This file provides guidance to agents when working with code in this repository.

- `application.properties` uses `${ENV_VAR:default}` for every externalisable value. The JWT secret has an empty default — the app will fail to start without `JWT_SECRET` set.
- There is no `mapper` package; mapping is done inline in services using manual field assignments and MapStruct interfaces (where present). No dedicated `*Mapper` classes exist yet.
- `BookSpecification` is a static utility class (not a Spring bean) in the `repository` package — counterintuitive placement.
- `SecurityConfig` lives in `config`, not `security`.
- Integration tests (`CustomerJourneyIntegrationTest`, `PaymentLifecycleIntegrationTest`) require a real running PostgreSQL database — they are not H2-backed.
- Swagger UI is available at `/swagger-ui.html`; API docs JSON at `/v3/api-docs`.
- The only role in use is `ROLE_CUSTOMER`; there is no admin role in the codebase yet.
