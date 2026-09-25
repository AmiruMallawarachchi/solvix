# Solvix API Specification

## Purpose

This document defines the MVP API contract for Solvix. It is the root-level reference for the product’s external interface while the detailed OpenAPI-style design lives in [api/api-specification.md](./api/api-specification.md).

## API principles

- resource-oriented endpoints
- authenticated access for protected resources
- backend-enforced authorization and validation
- asynchronous AI jobs where full completion is not required in the user request flow
- audit logging for all state-changing actions

## Base path

```text
/api/v1
```

## Core endpoints

### Authentication
- POST /api/v1/auth/login
- POST /api/v1/auth/logout
- GET /api/v1/auth/me

### Users and teams
- GET /api/v1/users/me
- GET /api/v1/teams
- GET /api/v1/teams/{teamId}/members

### Ticket management
- POST /api/v1/tickets
- GET /api/v1/tickets
- GET /api/v1/tickets/{ticketId}
- PATCH /api/v1/tickets/{ticketId}
- POST /api/v1/tickets/{ticketId}/assign
- GET /api/v1/tickets/{ticketId}/history
- POST /api/v1/tickets/{ticketId}/comments
- GET /api/v1/tickets/{ticketId}/comments
- POST /api/v1/tickets/{ticketId}/attachments
- GET /api/v1/tickets/{ticketId}/attachments

### Dashboard and search
- GET /api/v1/dashboard/overview
- GET /api/v1/search/tickets

### AI workflow
- POST /api/v1/ai/triage
- GET /api/v1/ai/jobs/{jobId}
- POST /api/v1/ai/approvals
- GET /api/v1/ai/approvals/{jobId}

## Design notes

The API should be thin and orchestrative. Business rules belong in the backend application services, not in the HTTP layer alone. Ticket lifecycle and authorization checks must be enforced on the server, and AI actions must be logged and approval-gated when they affect state.

The first implemented security slice uses stateless HTTP Basic authentication. The authenticated principal supplies the ticket owner identity, so clients cannot set `createdBy` for another user. JWT login and identity-provider integration remain planned work.

## Detailed contract

See [api/api-specification.md](./api/api-specification.md) for the detailed request/response payload definitions and MVP endpoint flow.
