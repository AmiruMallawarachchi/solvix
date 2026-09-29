# Solvix Backend

Java 17 + Spring Boot 3 backend for the Solvix ticketing workflow.

## Current slice

The backend now uses PostgreSQL-backed persistence through JPA and versioned Flyway migrations. Hibernate validates the migrated schema instead of changing it automatically. Persistent users authenticate with short-lived JWT bearer tokens. Ticket changes are also recorded in a durable activity history. The API supports:

- create a ticket
- list tickets
- view a ticket
- change ticket status
- assign a ticket
- add a comment
- view ticket activity history

This keeps the domain model stable while moving the application from in-memory testing to a persistent, production-like storage layer.

## Run locally

### Run PostgreSQL and the API with Docker Compose

From the repository root, copy `.env.example` to `.env` and run:

```powershell
docker compose up --build -d
```

This starts PostgreSQL on host port `5433` and the API on port `8080`. The API waits for PostgreSQL to pass its readiness check. The example credentials are development-only; set private values in `.env` and never use these defaults in a deployed environment. Stop the services with `docker compose down`; the database volume is retained.

### Run PostgreSQL in Docker and the API with Maven

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
$env:SOLVIX_CORS_ALLOWED_ORIGIN = "http://localhost:3000"
```

Then run the API:

```powershell
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

On first startup, the two configured users are inserted into PostgreSQL if they do not already exist. Passwords are stored as BCrypt hashes. Log in with `POST /api/v1/auth/login`, then send the returned token as `Authorization: Bearer <token>`. Customers can access their own tickets and comments. Support agents can access all tickets and perform assignment and status changes. Assignment is available after triage; updating status does not assign a user. The ticket owner and comment author are taken from the authenticated username; clients cannot choose these values.

The status lifecycle is `NEW -> TRIAGED -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED`. A resolved ticket may be reopened to `IN_PROGRESS`. Invalid transitions are rejected.

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

## Continuous integration

GitHub Actions runs backend verification and frontend lint, type-check, and build checks for backend, frontend, and workflow changes on pull requests and pushes to `main` or `feat/*` branches.
