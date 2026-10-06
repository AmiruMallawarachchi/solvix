# Solvix Product Requirements

## 1. Purpose

This document translates the Solvix business problem and project charter into concrete, testable product requirements. The intent is to define what the system must do before implementation begins, so that design, code, and validation all remain aligned with the product mission.

The requirements below cover the core Solvix workflow:

Problem reported
  -> Ticket created
  -> Triage
  -> Evidence + knowledge
  -> Correct team
  -> Investigation
  -> Resolution
  -> Customer / stakeholder update
  -> Audit trail + analytics

---

## 2. Product assumptions

- Solvix is a software platform first, with AI as a supporting subsystem.
- Human users remain accountable for final operational decisions.
- High-impact actions require explicit human approval.
- The product must support a single integrated workflow rather than isolated tools.
- The current public deployment is a synthetic-data portfolio demo; it is not a production pilot.
- The next target is a secure, invite-only single-organization pilot on Vercel, Render, and Neon.
- AWS deployment and multi-tenant SaaS are deferred. AI, evidence retrieval, teams, dashboards, server-side search, notifications, attachments, and administration are staged capabilities and must not be represented as implemented until their acceptance criteria pass.
- The initial AI provider is planned to be Groq, with credentials held only in backend secrets and consequential actions gated by explicit human approval.

### Current implementation boundary

The current implementation provides ticket intake, ownership-scoped viewing, support-agent assignment, lifecycle status changes, attributed comments, activity history, and web screens for those capabilities. This is a working ticket-workflow foundation and public demo, not the complete product or a secure production pilot. The remaining requirements describe the staged product target and are not implied to be implemented. In particular:

- FR-003 current updates are limited to the operations implemented by the API; each additional field/action requires authorization and audit tests.
- Team administration, AI, evidence retrieval, search, dashboards, notifications, attachments, and administration are planned, not current capabilities.
- Current local/demo authentication is not the selected pilot identity design. The selected candidate is Auth0 Free: use its hosted tenant domain and an exact Vercel callback allowlist; keep application roles in Solvix because organization-level RBAC and separate development/production tenants are not included in the Free plan. Recheck current terms before provisioning.

The broader product is intended to be delivered in stages rather than treated as permanently out of scope. See the [production readiness roadmap](./07-production-readiness-roadmap.md) for the sequence and release gates. A requirement remains planned until its acceptance criteria are implemented and verified.

---

## 3. Functional requirements

### FR-001: Ticket creation

A customer or support user shall be able to create a new ticket with a title, description, category, and optional metadata.

Acceptance criteria:
- A valid request with required fields is accepted.
- A ticket is persisted with a unique identifier.
- A ticket cannot be created with a blank title or description.
- The system records the creator and creation timestamp.
- Invalid data returns a validation error with clear feedback.

Priority: High

### FR-002: Ticket viewing

An authenticated user shall be able to view the details of a ticket they are authorized to access.

Acceptance criteria:
- A user can retrieve a ticket by unique ID.
- Unauthorized users cannot view tickets outside their permission scope.
- The response includes status, assignee, category, created date, and current comments/history.

Priority: High

### FR-003: Ticket update

An authorized user shall be able to update a ticket's fields, including status, priority, category, assignee, and description.

Acceptance criteria:
- Valid updates are persisted.
- Only authorized users can modify restricted fields.
- The system records the update event in the audit trail.
- A status change creates a history entry.

Priority: High

### FR-004: Ticket status tracking

The system shall maintain the status history of each ticket.

Acceptance criteria:
- Every status change creates an auditable event.
- The ticket current state reflects the latest valid transition.
- A historical timeline can be retrieved for the ticket.
- Valid transitions are NEW -> TRIAGED -> ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED.
- A RESOLVED ticket may be reopened to IN_PROGRESS; other invalid transitions are rejected.

Priority: High

### FR-005: Ticket assignment

A support agent, team lead, or authorized admin shall be able to assign a ticket to a user or team.

Acceptance criteria:
- Assignment is rejected if the target user is not authorized for the ticket context.
- Assignee changes are recorded in the ticket history.
- The assigned owner is visible in the ticket details.

Priority: High

### FR-006: Comments and collaboration

Users with access shall be able to add comments to a ticket to support investigation and communication.

Acceptance criteria:
- Comments are stored against the correct ticket.
- Each comment stores its text, authenticated author, and timestamp.
- Users can review ticket comment threads.
- Unauthorized users cannot comment on restricted tickets.

Priority: High

### FR-007: Team-based ownership

The system shall support teams and team membership for workload routing and accountability.

Acceptance criteria:
- A ticket can be associated with one or more relevant teams.
- Team membership is defined and enforceable by authorization rules.
- Users can be filtered or assigned by team membership.

Priority: High

### FR-008: User authentication and session access

The system shall require authentication for access to protected operations and data.

Acceptance criteria:
- Only authenticated users can create, view, or modify tickets.
- Sessions are invalidated after logout or expiration.
- Authenticated users receive role-aware authorization context.

Priority: Critical

### FR-009: Authorization and role enforcement

The system shall enforce role-based access control for protected resources and actions.

Acceptance criteria:
- Customer, support, developer, team lead, manager, and admin roles are distinguishable.
- A user cannot perform actions outside their role permissions.
- Sensitive actions trigger authorization checks before execution.

Priority: Critical

### FR-010: Audit log

The system shall maintain an audit trail of material ticket and system events.

Acceptance criteria:
- Ticket create, update, assignment, comment, and status changes are logged.
- User identity is associated with logged events.
- Historical actions are available for review.
- Sensitive operations are captured separately when required.

Priority: Critical

### FR-011: AI triage classification

An AI triage job shall classify newly created or updated tickets asynchronously.

Acceptance criteria:
- New tickets trigger an AI triage job.
- The AI output includes category, severity, suggested team, and justification or confidence metadata.
- The result is persisted and associated with the ticket.
- The system does not require the AI result to complete before the ticket is created.

Priority: High

### FR-012: AI evidence retrieval

The system shall support AI-driven retrieval of relevant operational evidence and knowledge for a ticket.

Acceptance criteria:
- The system can gather contextual evidence from internal knowledge and associated ticket history.
- Retrieved evidence is attached to the AI workflow context.
- The evidence source is traceable for review.
- Empty or irrelevant evidence results are handled gracefully.

Priority: High

### FR-013: Human approval for protected AI actions

The system shall require human approval before performing protected AI-driven actions that affect ticket state, assignments, or external operations.

Acceptance criteria:
- A protected action is blocked until explicit approval.
- Approval metadata is stored with the decision.
- Rejected or expired approvals are not executed.
- Approval decisions are logged in the audit trail.

Priority: Critical

### FR-014: Ticket lifecycle workflow

The system shall support a structured ticket lifecycle from creation to resolution.

Acceptance criteria:
- Each ticket moves through defined states.
- State transitions are visible to authorized users.
- Resolution is clearly differentiated from closure or escalation.
- Ticket resolution is associated with a user or team decision.

Priority: High

### FR-015: Dashboard overview

A manager or team lead shall be able to view operational summaries and current queue status.

Acceptance criteria:
- The dashboard shows counts by status, team, and priority.
- Users can understand backlog and active work at a glance.
- Access to dashboard data respects role permissions.

Priority: Medium

### FR-016: Search and filtering

Authorized users shall be able to search and filter tickets by key criteria.

Acceptance criteria:
- Tickets can be searched by title, ID, assignee, status, team, or category.
- Filters can be combined logically.
- Search results are paginated or otherwise bounded for usability.

Priority: High

### FR-017: Notifications

The system shall support notification of relevant stakeholders for ticket updates or actions.

Acceptance criteria:
- Status changes or assignment updates can trigger notifications.
- Notifications are targeted to authorized users or teams.
- The delivery state is visible or auditable when applicable.

Priority: Medium

### FR-018: Attachment support

Users shall be able to attach files or evidence to a ticket when relevant.

Acceptance criteria:
- Relevant file metadata is stored with the ticket.
- Only authorized users can upload or view attachments.
- Attachment references are retained in the ticket history.

Priority: Medium

### FR-019: Knowledge base association

The system shall support linking tickets to knowledge items or operational documentation.

Acceptance criteria:
- Relevant documents can be referenced within a ticket workflow.
- Retrieval can surface known information or prior resolutions.
- Newly created knowledge can be linked to future tickets.

Priority: Medium

### FR-020: AI workflow telemetry

The system shall record telemetry for AI jobs and workflow execution so they can be evaluated and debugged.

Acceptance criteria:
- AI jobs have timestamps, status, inputs, outputs, and policy state.
- Failures and retries are visible.
- Telemetry supports audit and debugging without exposing sensitive data.

Priority: Medium

---

## 4. Non-functional requirements

### NFR-001: Security

The system shall protect sensitive data and enforce least-privilege access.

Measurement expectations:
- authorization checks are enforced before protected actions
- secrets are not stored in source control
- access to ticket data follows role and ownership rules
- security controls are reviewed against relevant OWASP ASVS principles

Priority: Critical

### NFR-002: Data integrity

The system shall preserve trustworthy and consistent records of tickets, actions, and AI outputs.

Measurement expectations:
- updates are transactional where appropriate
- history is preserved for all material changes
- duplicate or conflicting writes are prevented or handled deterministically

Priority: Critical

### NFR-003: Reliability

The system shall support consistent core operational workflows under expected traffic and normal failure conditions.

Measurement expectations:
- core ticket operations succeed reliably
- failed AI jobs do not corrupt ticket state
- retries and queue processing are handled safely

Priority: High

### NFR-004: Performance

The system shall provide responsive access for users performing everyday operational tasks.

Measurement expectations:
- page and API actions remain fast enough for interactive usage
- AI processing is asynchronous where it is not required to complete within the user action flow
- dashboards and search queries remain usable with expected ticket volumes

Priority: High

### NFR-005: Observability

The system shall provide enough operational visibility to understand workflow and failure states.

Measurement expectations:
- logs, metrics, and traceability exist for ticket changes and AI execution
- failed jobs can be diagnosed without guessing
- system health is visible to operators

Priority: High

### NFR-006: Scalability

The system shall support growth in ticket volume, users, and AI workflows without requiring re-architecture at the first milestone.

Measurement expectations:
- design supports moderate growth in a modular monolith or equivalent pattern
- AI workloads can be isolated from core business workflows
- database and queue patterns are chosen to avoid brittle coupling

Priority: Medium

### NFR-007: Usability

The system shall be understandable for support, engineering, and management users.

Measurement expectations:
- common workflows are clear and low-friction
- ticket details and current status are easy to understand
- admin and manager role views are tailored to the user’s operational responsibilities

Priority: Medium

### NFR-008: Maintainability

The system shall be structured so that it can evolve as the product grows.

Measurement expectations:
- modules and services are organized by business capability
- architecture decisions are documented
- requirements and tests are aligned with implementation work

Priority: High

---

## 5. User stories

### Customer / Requester
- As a customer, I want to submit a problem so that it is recorded and tracked.
- As a customer, I want to see that my issue has been received so that I know it is being handled.
- As a customer, I want status updates so that I understand progress and next steps.

### Support Agent
- As a support agent, I want to triage new tickets quickly so that I can route them correctly.
- As a support agent, I want to view ticket history so that I understand prior actions and context.
- As a support agent, I want to assign tickets to the right owner so that accountability is clear.

### Developer
- As a developer, I want ticket details and evidence so that I can investigate the issue efficiently.
- As a developer, I want clear handoff context so that I do not need to rediscover the issue from scratch.

### Team Lead
- As a team lead, I want workload visibility so that I can allocate and prioritize work.
- As a team lead, I want queue-level metrics so that I can monitor team performance.

### Manager
- As a manager, I want dashboard summaries so that I can assess operational performance.
- As a manager, I want resolution and backlog visibility so that I can identify bottlenecks.

### Administrator
- As an administrator, I want to manage users, teams, and permissions so that the system remains secure and governed.

---

## 6. Success metrics for validation

The following metrics should be measured once the system is implemented:

- Ticket creation success rate
- Ticket update success rate
- Average time to first assignment
- AI triage classification accuracy or confidence distribution
- Retrieval relevance for knowledge lookup
- Time to first human approval for protected AI actions
- Number of unauthorized access attempts blocked
- Dashboard freshness and accuracy
- p95 API latency for ticket actions
- AI workflow completion rate and failure rate

These are evaluation targets rather than fabricated claims. They should be collected and published as measured system evidence in the future.

---

## 7. Traceability and implementation mapping

The requirements in this document should map directly to product work items and test cases.

Example mapping:

- FR-001 -> Ticket creation API
- FR-003 -> Ticket update API
- FR-004 -> Ticket event history
- FR-005 -> Ticket assignment workflow
- FR-011 -> AI triage job
- FR-013 -> Human approval workflow
- NFR-001 -> Security review and ASVS checklist
- NFR-005 -> Observability and logging strategy

This preserves a direct path from requirement -> implementation -> test -> evidence.

---

## 8. Current foundation and next delivery

The existing ticket-workflow foundation includes:

1. ticket creation, ownership-scoped list, and details
2. lifecycle transitions, including reopening resolved tickets
3. support-agent assignment
4. comments with author and timestamp
5. activity history and role/ownership checks
6. a focused customer and support-agent web workflow
7. backend/frontend validation in CI

Next delivery stages are: secure product/identity contract and delivery gates; API and authorization hardening; complete workflows and bounded server-side search; teams/admin; governed knowledge; asynchronous, human-reviewed Groq triage; notifications/attachments; dashboards and operational readiness; controlled pilot. AWS and multi-tenant SaaS remain out of scope for this target.

---

## 9. Requirement and release evidence

Requirements remain active until their planned capability is implemented and verified. For each release slice:

- every major product workflow has a corresponding requirement
- security and audit requirements are explicit
- AI responsibilities are clearly bounded and human-controlled
- acceptance criteria map to automated tests or recorded verification evidence
- implemented, planned, and deferred capabilities are labelled accurately
- the release scope is linked to the staged roadmap and acceptance gates

The current ticket workflow is already implemented as a portfolio-demo foundation. The next product work is to harden that foundation for the approved single-organization pilot; see the [production readiness roadmap](./07-production-readiness-roadmap.md). Passing one slice does not imply that later capabilities or the full pilot are complete.
