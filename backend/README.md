# Solvix Backend

Java 17 + Spring Boot 3 backend for the Solvix ticketing workflow.

## Current slice

The first backend slice intentionally uses an in-memory repository. It supports:

- create a ticket
- list tickets
- view a ticket
- change ticket status
- assign a ticket
- add a comment

This gives us a working domain and API flow before introducing PostgreSQL, authentication, background workers, and AI.

## Run

From this directory:

```powershell
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

## Test

```powershell
mvn test
```

## Example request

```http
POST /api/v1/tickets
Content-Type: application/json

{
  "title": "Payment issue",
  "description": "Payment succeeded but subscription is inactive",
  "priority": "HIGH",
  "createdBy": "customer-1"
}
```

## Package structure

- `domain.ticket`: ticket rules and domain values
- `application.ticket`: use cases and repository port
- `infrastructure.ticket`: current in-memory repository adapter
- `web.ticket`: HTTP controller and request/response DTOs
- `web.error`: consistent HTTP error mapping
