# Bookstore API - Local Development Setup

## Prerequisites
- Java 21
- PostgreSQL running locally or in Docker

## Environment Configuration

### 1. JWT Secret Configuration
Set the `JWT_SECRET` environment variable (minimum 256 bits / 32 bytes) prior to starting the application:
```bash
export JWT_SECRET="your-256-bit-base64-or-plain-secret-key-min-32-bytes"
```

### 2. PostgreSQL Database Configuration
If using custom database credentials or ports:
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/bookstoredb"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="yourpassword"
```

### 3. Running the Application
```bash
./mvnw spring-boot:run
```

Once started:
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI Specification: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
