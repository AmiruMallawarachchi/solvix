# ADR-0003: PostgreSQL as the Primary Store with Async AI Workers

- Status: Accepted
- Date: 2026-09-19

## Context

Solvix needs to support operational records, ticket lifecycle history, assignment ownership, comments, AI evidence, and audit logs. These are strong candidates for a relational model because the data is structured and heavily tied to workflow state.

At the same time, AI classification, retrieval, and summarization do not need to block the user request path in many cases. They should run asynchronously.

## Decision

We will use PostgreSQL as the primary transactional database for the MVP.

We will also introduce a queue or async worker layer for AI and background tasks, including:
- ticket triage
- evidence retrieval
- notification dispatch
- retry handling

This keeps the data model reliable while allowing AI processing to happen outside the core user interaction flow.

## Consequences

### Positive
- strong data integrity for workflow records and history
- easier relational modeling for tickets, users, comments, teams, and audit trails
- AI work can be backgrounded without slowing the user experience
- simpler implementation for the MVP than a fully distributed architecture

### Negative
- AI workflows are still dependent on a supported queue and worker setup
- operational monitoring is needed for background job failures

## Alternatives considered

### NoSQL-first design
Rejected because ticket workflows, status history, and auditability fit relational semantics more naturally.

### Synchronous AI execution in the request path
Rejected because it would make user actions slower and create more failure coupling between the business workflow and the AI system.
