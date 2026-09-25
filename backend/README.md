# Solvix Backend

Java 17 + Spring Boot 3 backend for the Solvix ticketing workflow.

## Current slice

The backend now uses PostgreSQL-backed persistence through a JPA repository adapter and protects the API with HTTP Basic authentication for the first security slice. It supports:

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

Set local credentials before starting the API:

```powershell
$env:SOLVIX_CUSTOMER_USERNAME = "customer-1"
$env:SOLVIX_CUSTOMER_PASSWORD = "change-me-customer"
$env:SOLVIX_SUPPORT_USERNAME = "support-1"
$env:SOLVIX_SUPPORT_PASSWORD = "change-me-support"
```

Then run the API:

```powershell
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

All ticket endpoints require authentication. Customers can access their own tickets and comments. Support agents can access all tickets and perform assignment and status changes. The ticket owner is taken from the authenticated username; clients cannot choose another `createdBy` value.

## Test

```powershell
cd backend
mvn test
```

## Example request

```http
POST /api/v1/tickets
Content-Type: application/json

{
  "title": "Payment issue",
  "description": "Payment succeeded but subscription is inactive",
  "priority": "HIGH"
}
```

## Package structure

- `domain.ticket`: ticket rules and domain values
- `application.ticket`: use cases and repository port
- `infrastructure.ticket`: JPA persistence adapter and entity mapping
- `web.ticket`: HTTP controller and request/response DTOs
- `web.error`: consistent HTTP error mapping
