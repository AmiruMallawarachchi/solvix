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

### Implemented ticket management
- POST /api/v1/tickets
- GET /api/v1/tickets
- GET /api/v1/tickets/{ticketId}
- PATCH /api/v1/tickets/{ticketId}/status
- POST /api/v1/tickets/{ticketId}/assign
- POST /api/v1/tickets/{ticketId}/comments
- GET /api/v1/tickets/{ticketId}/history

The detailed specification distinguishes implemented behavior from planned endpoints. Pagination and filtering are not implemented yet.

## Design notes

The API should be thin and orchestrative. Business rules belong in the backend application services, not in the HTTP layer alone. Ticket lifecycle and authorization checks must be enforced on the server, and AI actions must be logged and approval-gated when they affect state.

The default local profile uses persistent users and stateless JWT bearer authentication. `POST /api/v1/auth/login` returns a short-lived token, and the authenticated principal supplies the ticket owner identity, so clients cannot set `createdBy` for another user. The production profile disables local login and validates Cognito access tokens on protected routes. The frontend uses Cognito authorization-code flow with PKCE when configured for production. A real Cognito user pool and deployment configuration are not yet provisioned; see the [production readiness roadmap](./07-production-readiness-roadmap.md).

The implemented ticket history endpoint returns persisted creation, status change, assignment, and comment activity, subject to the same ticket ownership rules as ticket details.

The first release deliberately excludes AI, evidence retrieval, teams, dashboards, search, notifications, attachments, and user administration. These capabilities are planned as staged follow-on releases; see the [production readiness roadmap](./07-production-readiness-roadmap.md). The ticket lifecycle is `NEW -> TRIAGED -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED`, with `RESOLVED -> IN_PROGRESS` allowed for reopening. Assignment is to a support-agent username and is available after triage.

## Detailed contract

See [api/api-specification.md](./api/api-specification.md) for the detailed request/response payload definitions and MVP endpoint flow.
