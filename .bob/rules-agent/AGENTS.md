# Agent Coding Rules

This file provides guidance to agents when working with code in this repository.

- Always use `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` + `@RequiredArgsConstructor` for DI classes. Never write `private final` explicitly; never use `@Autowired`.
- DTOs are Java `record` types, not classes. Keep them that way.
- Entity UUIDs are assigned in the service layer via `UUID.randomUUID()`, not via `@GeneratedValue`.
- Inject `Clock clock` (from `AppConfig`) for any time logic; never call `Instant.now()` directly in services.
- New domain exceptions must extend the existing hierarchy and be registered in `GlobalExceptionHandler` returning `ProblemDetail`.
- Book catalog filtering must go through `BookSpecification.buildFilter(...)` — do not write raw JPQL predicates outside this class.
- Business rules (points rate, shipping, cancellation window) live in `BookstoreProperties`; read from there, never hardcode.
- Controller tests require explicitly importing `SecurityConfig`, `JwtAuthenticationFilter`, `JwtUtils`, `JwtProperties`, and `GlobalExceptionHandler` into the `@WebMvcTest` context via `@Import`.
- Integration tests must supply `bookstore.security.jwt.secret` via `@TestPropertySource` (min 32 chars).
- Annotation processor order in `pom.xml` is significant — Lombok first, then lombok-mapstruct-binding, then mapstruct-processor. Do not modify it.
