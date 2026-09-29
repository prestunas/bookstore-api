# Backend Code Style Guidelines

Apply these rules whenever creating, modifying, refactoring, or reviewing backend Java code.

## Naming conventions

- Use singular package names.
- Examples of valid package names: `cartographer`, `repository`, `mapper`, `util`, and `dto`.
- The package names `imports` and `exports` are allowed exceptions.
- Do not create plural package names such as `mappers`, `repositories`, `utils`, or `dtos`.
- Use singular class names unless a plural name is required by the business domain.
- Do not use numeric digits in method or variable names.
- Replace numeric digits with words. For example, use `exportToPdf` instead of `export2pdf`.

## Class fields and access modifiers

- Use Lombok `@FieldDefaults` to define class field access modifiers.
- For private fields, use:

```java
@FieldDefaults(level = AccessLevel.PRIVATE)
```

- When fields must also be final, use:

```java
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
```

- Do not explicitly declare fields as `private final` when the same modifiers are already provided by `@FieldDefaults`.

Incorrect:

```java
private final FooService fooService;
```

Correct:

```java
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class FooServiceClient {

    FooService fooService;
}
```

## Dependency injection

- Use constructor-based dependency injection.
- Prefer Lombok `@RequiredArgsConstructor` for injecting required dependencies.
- Use `@AllArgsConstructor` or `@NoArgsConstructor` only when they are appropriate for the class design or required by a framework.
- Do not use field injection.
- Do not use `@Autowired`.
- Do not manually create constructors when an appropriate Lombok annotation can generate them.

## REST controllers

- REST controllers must not contain business logic.
- Controllers may only:
  - accept and validate requests;
  - extract request parameters, path variables, and headers;
  - delegate processing to a service;
  - return the service result.
- User identification, authorization, data retrieval, data transformation, and business processing must be implemented in the service layer or another appropriate component.

Example:

```java
@PostMapping
public FooResponse doSomething(
        @RequestHeader(USER_TOKEN) String userToken,
        @Valid @RequestBody FooDto fooDto
) {
    return fooService.doSomething(userToken, fooDto);
}
```

## Logging

- Use the logging framework already configured in the project.
- Use parameterized logging instead of string concatenation.
- Reuse existing message and error constants when available.
- Do not log secrets, access tokens, passwords, or other sensitive information.

Example:

```java
log.info("Operation completed for userId={}", userId);
```

## Mappers

- Use MapStruct for mapping between entities, DTOs, and other application models.
- Do not manually implement mapping logic when it can be handled by MapStruct.
- Follow the existing project configuration for `componentModel` and mapper injection.

Example:

```java
@Mapper(componentModel = "spring")
public interface FooMapper {

    Foo toEntity(FooDto dto);

    FooDto toDto(Foo entity);
}
```

## Existing project conventions

- Before implementing changes, inspect similar existing classes in the same module.
- Follow the existing project architecture, package structure, naming, formatting, and implementation patterns.
- Reuse existing utilities, constants, mappers, repositories, validators, and exception-handling mechanisms.
- Do not introduce a new dependency, library, or architectural pattern when the project already has an established solution.
- Keep changes limited to the requested functionality.
- Do not modify unrelated code.

## Change scope

- Make only the changes required by the task.
- Do not refactor unrelated code.
- Do not rename existing classes, methods, fields, endpoints, or configuration properties unless explicitly requested.
- Do not change public APIs, database schemas, event contracts, or message formats without explicit approval.
- Preserve backward compatibility unless the task explicitly requires a breaking change.
- Before creating a new component, verify that an equivalent implementation does not already exist.

## Code consistency

- Inspect similar implementations in the same module before writing new code.
- Prefer extending an existing solution over introducing a parallel implementation.
- Do not introduce unnecessary abstractions, interfaces, helper classes, or design patterns.
- Do not duplicate code when an existing reusable implementation is available.
- Keep methods focused and reasonably small.
- Use guard clauses to avoid unnecessary nesting.

## Error handling

- Use the existing project exception hierarchy and error constants.
- Do not catch generic `Exception` unless it is required at an application boundary.
- Do not silently ignore exceptions.
- Do not return `null` to hide an error.
- Preserve the original exception as the cause when wrapping it.
- Do not expose internal exception details, stack traces, database information, or sensitive data through REST responses.
- Log an exception only where it is handled to avoid duplicate logging.

## Validation

- Validate incoming data at the application boundary.
- Use Bean Validation annotations where appropriate.
- Do not rely only on controller validation when the same service can be called from another entry point.
- Validate business rules in the service layer.
- Reuse existing validation utilities and error codes.
- Do not silently accept unsupported enum values or malformed input.

## Null handling and collections

- Avoid returning `null` collections. Return an empty collection instead.
- Do not use `Optional` as a class field, DTO field, or method parameter.
- Use `Optional` only as a return type when the absence of a value is expected.
- Do not call `Optional.get()` without first verifying that a value is present.
- Prefer immutable collections when returned data must not be modified.

## Database and transactions

- Follow existing repository patterns.
- Do not access the database directly from REST controllers.
- Avoid database queries inside loops.
- Do not load all records when filtering or pagination can be performed by the database.
- Use `@Transactional` only at the appropriate service-layer boundary.
- Use read-only transactions for read operations when consistent with existing project conventions.
- Do not change database schemas or existing data without an explicit migration.
- Do not create or modify a migration that has already been applied in another environment.

## API contracts

- Preserve existing HTTP status codes and response structures unless explicitly requested.
- Do not expose persistence entities directly through REST APIs.
- Use DTOs for request and response contracts.
- Keep OpenAPI annotations and API documentation consistent with the implementation.
- When changing an API contract, update its DTOs, validation, documentation, tests, and consumers where applicable.

## Security

- Never hardcode passwords, tokens, API keys, connection strings, or other secrets.
- Do not log authentication tokens, authorization headers, passwords, or personal data.
- Do not weaken authentication, authorization, validation, or security configuration to make a test pass.
- Reuse the project's existing authorization mechanisms.
- Treat all external input as untrusted.
- Avoid exposing internal identifiers or implementation details unless required by the API contract.

## Date and time

- Use the Java Time API.
- Do not use legacy `Date` or `Calendar` classes in new code.
- Do not assume the system default time zone.
- Use the time-zone strategy already established by the project.
- Use `Clock` when time-dependent business logic must be unit tested.
- Do not call `LocalDateTime.now()` directly in business logic that requires deterministic tests.

## Tests

- Add or update tests for every behavior change.
- Test the successful scenario, validation failures, edge cases, and error handling.
- Follow the existing test naming and structure used by the module.
- Prefer focused unit tests and add integration tests when behavior crosses component boundaries.
- Do not delete, disable, or weaken an existing test to make the build pass.
- Do not change expected values merely to match an incorrect implementation.
- Mock external dependencies, not the class being tested.
- Avoid unnecessary mocking of simple domain objects.
- Tests must be deterministic and independent of execution order.
- Do not use arbitrary delays such as `Thread.sleep()` in tests.
- Verify both the returned result and important side effects.
- Run the relevant tests after making changes and report any tests that could not be executed.

## Dependencies and configuration

- Do not add, remove, or upgrade dependencies unless required by the task.
- Reuse libraries already available in the project.
- Do not change application configuration, CI/CD configuration, Docker files, or environment settings unless required.
- Do not hardcode environment-specific values.
- Preserve existing configuration defaults.

## Generated and licensed files

- Preserve existing copyright and license headers.
- Use the current year and formatting established in the repository.
- Do not manually modify generated source files.
- Modify the generator or source definition instead and regenerate the output.
- Do not include build output, temporary files, IDE files, or secrets in the repository.

## Completion requirements

Before completing a coding task:

1. Review the final diff for unintended changes.
2. Confirm that the implementation follows these rules and existing project patterns.
3. Run the relevant unit and integration tests when available.
4. Run formatting or static analysis checks when configured.
5. Report:
   - what was changed;
   - which tests were executed;
   - any tests or checks that could not be executed;
   - remaining risks or assumptions.
