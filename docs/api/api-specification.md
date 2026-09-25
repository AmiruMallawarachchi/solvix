# Solvix API Specification

## 1. Purpose

This document defines the initial API contract for the Solvix MVP. It is intentionally resource-oriented and aligned with the product requirements and domain model.

The API is designed around the core workflows:

- authentication
- ticket creation and lifecycle management
- assignment and ownership
- collaboration through comments
- dashboard and operational reporting
- AI triage and approval actions

---

## 2. API conventions

### Base path

```text
/api/v1
```

### Common response format

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

### Error format

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Title is required",
    "details": []
  }
}
```

### Authentication

The current backend slice protects ticket routes with stateless HTTP Basic authentication. Production token-based login endpoints remain part of the planned identity-provider integration.

---

## 3. Authentication endpoints

### POST /api/v1/auth/login

> Planned contract. The current backend does not expose this endpoint yet; configure the local HTTP Basic users described in the backend README instead.

Request body:

```json
{
  "email": "user@example.com",
  "password": "secret"
}
```

Success response:

```json
{
  "success": true,
  "data": {
    "token": "jwt-token",
    "user": {
      "id": "usr_123",
      "email": "user@example.com",
      "role": "support_agent",
      "fullName": "Ava Chen"
    }
  },
  "error": null
}
```

### POST /api/v1/auth/logout

Invalidates the current session or token.

### GET /api/v1/auth/me

Returns the current authenticated user and scoped profile data.

---

## 4. User and team endpoints

### GET /api/v1/users/me

Returns the authenticated user profile and available permissions context.

### GET /api/v1/teams

Returns visible teams for the current user.

### GET /api/v1/teams/{teamId}/members

Returns members for a team.

---

## 5. Ticket endpoints

### POST /api/v1/tickets

Creates a new ticket.

Request body:

```json
{
  "title": "Billing sync issue",
  "description": "Customer subscription still shows inactive even though payment succeeded.",
  "category": "billing",
  "priority": "high",
  "source": "customer_portal",
  "teamId": "team_001"
}
```

Success response:

```json
{
  "success": true,
  "data": {
    "id": "tic_101",
    "title": "Billing sync issue",
    "description": "Customer subscription still shows inactive even though payment succeeded.",
    "category": "billing",
    "priority": "high",
    "status": "new",
    "createdBy": "usr_123",
    "createdAt": "2026-01-01T10:00:00Z"
  },
  "error": null
}
```

### GET /api/v1/tickets

Returns paginated list of tickets.

Query parameters:

- status
- priority
- category
- assigneeId
- teamId
- search
- page
- pageSize
- sortBy
- sortOrder

Example:

```text
GET /api/v1/tickets?status=open&priority=high&page=1&pageSize=20
```

### GET /api/v1/tickets/{ticketId}

Returns full details of one ticket, including:

- ticket data
- status history
- comments
- attachments
- current assignee
- metadata

### PATCH /api/v1/tickets/{ticketId}

Updates selected ticket fields.

Example:

```json
{
  "status": "in_progress",
  "priority": "urgent",
  "assigneeId": "usr_456"
}
```

### DELETE /api/v1/tickets/{ticketId}

May be restricted to admin or specific roles, depending on policy.

---

## 6. Ticket assignment endpoints

### POST /api/v1/tickets/{ticketId}/assign

Assigns a ticket to a user or team.

Request body:

```json
{
  "assigneeId": "usr_456",
  "teamId": "team_002",
  "reason": "Billing team ownership"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "ticketId": "tic_101",
    "assigneeId": "usr_456",
    "teamId": "team_002",
    "assignedAt": "2026-01-01T10:05:00Z"
  },
  "error": null
}
```

### GET /api/v1/tickets/{ticketId}/assignment-history

Returns assignment history for the ticket.

---

## 7. Comment endpoints

### POST /api/v1/tickets/{ticketId}/comments

Adds a comment to a ticket.

Request body:

```json
{
  "body": "We have verified that the payment succeeded, but the subscription status has not synced. Investigating the billing sync job."
}
```

### GET /api/v1/tickets/{ticketId}/comments

Returns comment history for the ticket.

---

## 8. Ticket status history endpoints

### GET /api/v1/tickets/{ticketId}/history

Returns the status timeline and event history.

Example response:

```json
{
  "success": true,
  "data": [
    {
      "id": "hist_1",
      "previousStatus": "new",
      "newStatus": "triaged",
      "changedBy": "usr_123",
      "reason": "Initial categorization",
      "createdAt": "2026-01-01T10:02:00Z"
    }
  ],
  "error": null
}
```

---

## 9. Attachment endpoints

### POST /api/v1/tickets/{ticketId}/attachments

Uploads an attachment file for a ticket.

Request: multipart/form-data

Fields:
- file
- description (optional)

### GET /api/v1/tickets/{ticketId}/attachments

Returns attachment metadata for the ticket.

---

## 10. Dashboard endpoints

### GET /api/v1/dashboard/overview

Returns metrics for the current user perspective.

Example response:

```json
{
  "success": true,
  "data": {
    "openTickets": 48,
    "resolvedToday": 12,
    "urgentTickets": 5,
    "teamLoad": [
      {"teamId": "team_01", "name": "Billing", "count": 18},
      {"teamId": "team_02", "name": "Engineering", "count": 15}
    ]
  },
  "error": null
}
```

### GET /api/v1/dashboard/teams/{teamId}

Returns operational details for one team.

---

## 11. AI triage endpoints

### POST /api/v1/ai/triage

Triggers AI triage for a ticket.

Request body:

```json
{
  "ticketId": "tic_101"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "jobId": "ai_job_900",
    "status": "queued",
    "ticketId": "tic_101"
  },
  "error": null
}
```

### GET /api/v1/ai/jobs/{jobId}

Returns AI job status, evidence summary, and output metadata.

---

## 12. Approval workflow endpoints

### POST /api/v1/ai/approvals

Records a human approval or rejection for a protected AI action.

Request body:

```json
{
  "jobId": "ai_job_900",
  "actionType": "ticket_assignment",
  "targetId": "tic_101",
  "decision": "approved",
  "reason": "Confirmed team ownership"
}
```

### GET /api/v1/ai/approvals/{jobId}

Returns current approval state for the specified AI job.

---

## 13. Search and filtering endpoints

### GET /api/v1/search/tickets

Searches tickets by phrase and filters.

Example query:

```text
GET /api/v1/search/tickets?q=billing sync&status=open&teamId=team_01
```

---

## 14. Security contract expectations

Every protected endpoint must enforce:

- authentication
- authorization by role and ownership scope
- validation of request payloads
- rate limiting where applicable
- audit logging for state-changing operations

Protected operations include:

- ticket creation
- ticket update
- assignment changes
- deletion or final state transitions
- AI approval actions
- dashboard access to restricted data

---

## 15. MVP endpoint prioritization

The first build should include the following endpoints first:

1. POST /api/v1/auth/login
2. GET /api/v1/auth/me
3. POST /api/v1/tickets
4. GET /api/v1/tickets
5. GET /api/v1/tickets/{ticketId}
6. PATCH /api/v1/tickets/{ticketId}
7. POST /api/v1/tickets/{ticketId}/assign
8. POST /api/v1/tickets/{ticketId}/comments
9. GET /api/v1/tickets/{ticketId}/history
10. POST /api/v1/ai/triage
11. POST /api/v1/ai/approvals
12. GET /api/v1/dashboard/overview

This set covers the essential operational loop and provides the first working vertical slice.

---

## 16. Implementation note

The API layer should remain thin and orchestrate business services rather than embed business rules directly in the HTTP layer. The backend should validate, authorize, persist, and trigger downstream tasks in a consistent order.

---

## 17. Summary

This API specification provides a practical and testable contract for the Solvix MVP. It supports real issue management, human oversight, and AI-assisted decision support — without letting the AI layer become the source of control for operational state.
