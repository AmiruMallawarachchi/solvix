# Solvix Backend

Java 17 + Spring Boot 3 backend for the Solvix ticketing workflow.

## Current slice

The backend now uses PostgreSQL-backed persistence through a JPA repository adapter and versioned Flyway migrations. Hibernate validates the migrated schema instead of changing it automatically. The API protects access with persistent users and short-lived JWT bearer tokens. It supports:

- create a ticket
- list tickets
- view a ticket
- change ticket status
- assign a ticket
- add a comment

This keeps the domain model stable while moving the application from in-memory testing to a persistent, production-like storage layer.

## Run locally

Start the database:

```bash
docker compose up -d postgres
```

Set local bootstrap credentials and a signing secret before starting the API:

```powershell
$env:SOLVIX_CUSTOMER_USERNAME = "customer-1"
$env:SOLVIX_CUSTOMER_PASSWORD = "change-me-customer"
$env:SOLVIX_SUPPORT_USERNAME = "support-1"
$env:SOLVIX_SUPPORT_PASSWORD = "change-me-support"
$env:SOLVIX_JWT_SECRET = "replace-with-at-least-32-random-characters"
```

Then run the API:

```powershell
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

On first startup, the two configured users are inserted into PostgreSQL if they do not already exist. Passwords are stored as BCrypt hashes. Log in with `POST /api/v1/auth/login`, then send the returned token as `Authorization: Bearer <token>`. Customers can access their own tickets and comments. Support agents can access all tickets and perform assignment and status changes. The ticket owner is taken from the authenticated username; clients cannot choose another `createdBy` value.

Database structure is managed by the migrations in `src/main/resources/db/migration`. Add a new numbered migration for every schema change; do not edit an already-applied migration.

## Test

```powershell
cd backend
mvn test
```

## Example request

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "customer-1",
  "password": "change-me-customer"
}
```

The login response contains a short-lived JWT. Use that token for ticket requests.

## Package structure

- `domain.ticket`: ticket rules and domain values
- `application.ticket`: use cases and repository port
- `infrastructure.ticket`: JPA persistence adapter and entity mapping
- `web.ticket`: HTTP controller and request/response DTOs
- `web.error`: consistent HTTP error mapping
