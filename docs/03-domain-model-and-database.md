# Solvix Domain Model and Database Design

## 1. Purpose

This document defines the core Solvix domain model and the initial database design needed to support the ticketing workflow, team operations, AI-assisted triage, and auditability. It translates the product requirements and architecture into a concrete entity model that can drive schema design, backend implementation, and testing.

The central principle is simple:

The ticket is the source of truth for operational work.

Everything around it — comments, status history, assignments, AI runs, and evidence — supports the ticket lifecycle.

---

## 2. Domain model overview

Solvix revolves around a few core domains:

- identity and access
- teams and ownership
- tickets and ticket lifecycle
- comments and collaboration
- knowledge and evidence
- AI workflow executions
- audit and observability

These domains should remain conceptually distinct even if implemented in a single relational database structure in the MVP.

---

## 3. Core entities

### 3.1 User
Represents a person with access to the platform.

Attributes:
- id
- email
- password_hash
- full_name
- role
- is_active
- created_at
- updated_at
- last_login_at

Relationships:
- belongs to one or more teams
- creates tickets
- comments on tickets
- owns assignments
- triggers audit records

Role examples:
- customer
- support_agent
- developer
- team_lead
- manager
- administrator

---

### 3.2 Team
Represents a group of users responsible for ownership, routing, and operational accountability.

Attributes:
- id
- name
- description
- created_at
- updated_at

Relationships:
- has many users
- owns many tickets
- may be responsible for a category or domain

---

### 3.3 TeamMember
Defines a user’s membership in a team.

Attributes:
- id
- team_id
- user_id
- role_in_team
- joined_at

Purpose:
- supports team assignment logic
- enables team-level visibility and routing
- preserves membership history if needed later

---

### 3.4 Ticket
Represents a customer problem, operational issue, or internal request.

Attributes:
- id
- title
- description
- category
- priority
- status
- source
- customer_reference
- created_by
- assigned_to
- team_id
- created_at
- updated_at
- resolved_at
- closed_at

Relationships:
- belongs to a creator (user)
- may be assigned to a user or team
- has many comments
- has many status events
- has many attachments
- has many AI runs
- may link to knowledge items

Status examples:
- new
- triaged
- assigned
- in_progress
- awaiting_info
- blocked
- resolved
- closed
- escalated

Priority examples:
- low
- medium
- high
- urgent

Category examples:
- billing
- technical_bug
- account_access
- feature_request
- infrastructure
- support_request

---

### 3.5 TicketStatusHistory
Tracks the lifecycle of a ticket by recording each meaningful state change.

Attributes:
- id
- ticket_id
- previous_status
- new_status
- changed_by
- reason
- created_at

Purpose:
- provides auditability
- supports timeline views
- helps analyze ticket flow and delay points

---

### 3.6 Comment
Represents collaboration on a ticket.

Attributes:
- id
- ticket_id
- author_id
- body
- created_at
- updated_at

Relationships:
- belongs to one ticket
- belongs to one author

Purpose:
- customer updates
- support notes
- developer investigation notes
- resolution communication

---

### 3.7 Attachment
Represents files, screenshots, logs, or evidence associated to a ticket.

Attributes:
- id
- ticket_id
- uploaded_by
- file_name
- storage_uri
- mime_type
- file_size
- created_at

Purpose:
- preserve supporting evidence
- support debugging and case resolution
- provide incident history

---

### 3.8 TicketAssignment
Captures assignment history for a ticket.

Attributes:
- id
- ticket_id
- assignee_user_id
- assignee_team_id
- assigned_by
- assigned_at
- reason

Purpose:
- preserves assignment provenance
- supports assignment accountability
- avoids losing assignment history when reassignment occurs

---

### 3.9 KnowledgeItem
Represents internal documentation, known issues, troubleshooting articles, or prior guidance.

Attributes:
- id
- title
- content
- source_type
- source_reference
- category
- created_by
- created_at
- updated_at

Purpose:
- enables AI retrieval and evidence lookup
- helps support and engineering find known guidance
- reduces repetitive investigation work

---

### 3.10 AIJob
Represents a run of an AI workflow for classification, retrieval, or triage.

Attributes:
- id
- ticket_id
- job_type
- status
- provider
- model_name
- input_summary
- output_summary
- created_at
- started_at
- finished_at
- error_message

Relationships:
- belongs to one ticket
- may have many AIJobEvidence entries
- may have many AIJobToolCalls
- may result in approval records

Job types:
- triage
- evidence_retrieval
- summarization
- routing
- resolution_suggestion

Status examples:
- queued
- running
- completed
- failed
- cancelled

---

### 3.11 AIJobEvidence
Stores the evidence retrieved or generated during an AI run.

Attributes:
- id
- ai_job_id
- source_type
- source_id
- title
- snippet
- relevance_score
- retrieved_at

Purpose:
- shows what evidence the model used
- supports explainability and review
- helps evaluation and debugging

---

### 3.12 AIJobToolCall
Records tool invocations performed during an AI workflow.

Attributes:
- id
- ai_job_id
- tool_name
- input_summary
- output_summary
- status
- created_at

Purpose:
- allows auditing of AI agent behavior
- supports policy compliance checks
- improves evaluation and failure investigation

---

### 3.13 ApprovalDecision
Tracks human approval or rejection of high-impact AI actions.

Attributes:
- id
- ai_job_id
- action_type
- action_target_id
- approver_id
- decision
- reason
- created_at

Purpose:
- ensures protected actions are approved
- supports audit and compliance review
- helps enforce human-in-the-loop safeguards

---

### 3.14 AuditLog
Represents system-level or domain-level events recorded for accountability.

Attributes:
- id
- entity_type
- entity_id
- action
- performed_by
- details_json
- created_at

Purpose:
- stores broad operational history
- supports compliance and investigations
- centralizes event trail across business and AI actions

---

## 4. Key relationships

### 4.1 User to Team
A user may belong to multiple teams.

Cardinality:
- User: many-to-many with Team via TeamMember

### 4.2 User to Ticket
A user creates tickets and can be assigned tickets.

Cardinality:
- User: one-to-many created tickets
- User: one-to-many assigned tickets

### 4.3 Ticket to Comment
A ticket has many comments.

Cardinality:
- Ticket: one-to-many Comment

### 4.4 Ticket to StatusHistory
A ticket has many historical status records.

Cardinality:
- Ticket: one-to-many TicketStatusHistory

### 4.5 Ticket to Attachment
A ticket can have multiple attachments.

Cardinality:
- Ticket: one-to-many Attachment

### 4.6 Ticket to AIJob
A ticket may have multiple AI runs associated with it.

Cardinality:
- Ticket: one-to-many AIJob

### 4.7 AIJob to Evidence and Tools
An AI job can produce evidence and call tools.

Cardinality:
- AIJob: one-to-many AIJobEvidence
- AIJob: one-to-many AIJobToolCall

### 4.8 AIJob to ApprovalDecision
A protected action may require a human approval record.

Cardinality:
- AIJob: one-to-many ApprovalDecision

### 4.9 Ticket to AuditLog
The ticket lifecycle and its changes should be captured in the audit trail.

Cardinality:
- Ticket: one-to-many AuditLog (conceptually)

---

## 5. Conceptual ERD

```text
User 1---* Ticket
User 1---* Comment
User 1---* TeamMember
Team 1---* TeamMember
Team 1---* Ticket

Ticket 1---* Comment
Ticket 1---* TicketStatusHistory
Ticket 1---* Attachment
Ticket 1---* TicketAssignment
Ticket 1---* AIJob
Ticket 1---* AuditLog

AIJob 1---* AIJobEvidence
AIJob 1---* AIJobToolCall
AIJob 1---* ApprovalDecision

KnowledgeItem 1---* AIJobEvidence
```

A more explicit relational form can be expressed as:

```text
users
  - id (PK)
  - email
  - role
  - full_name
  - password_hash
  - is_active
  - created_at
  - updated_at

teams
  - id (PK)
  - name
  - description
  - created_at

team_members
  - id (PK)
  - team_id (FK -> teams.id)
  - user_id (FK -> users.id)
  - joined_at

tickets
  - id (PK)
  - title
  - description
  - category
  - priority
  - status
  - source
  - created_by (FK -> users.id)
  - assigned_to (FK -> users.id, nullable)
  - team_id (FK -> teams.id, nullable)
  - created_at
  - updated_at
  - resolved_at
  - closed_at

ticket_status_history
  - id (PK)
  - ticket_id (FK -> tickets.id)
  - previous_status
  - new_status
  - changed_by (FK -> users.id)
  - reason
  - created_at

comments
  - id (PK)
  - ticket_id (FK -> tickets.id)
  - author_id (FK -> users.id)
  - body
  - created_at

attachments
  - id (PK)
  - ticket_id (FK -> tickets.id)
  - uploaded_by (FK -> users.id)
  - file_name
  - storage_uri
  - mime_type
  - file_size
  - created_at

knowledge_items
  - id (PK)
  - title
  - content
  - source_type
  - source_reference
  - category
  - created_by (FK -> users.id)
  - created_at

ai_jobs
  - id (PK)
  - ticket_id (FK -> tickets.id)
  - job_type
  - status
  - provider
  - model_name
  - input_summary
  - output_summary
  - created_at
  - started_at
  - finished_at
  - error_message

ai_job_evidence
  - id (PK)
  - ai_job_id (FK -> ai_jobs.id)
  - source_type
  - source_id
  - title
  - snippet
  - relevance_score
  - retrieved_at

ai_job_tool_calls
  - id (PK)
  - ai_job_id (FK -> ai_jobs.id)
  - tool_name
  - input_summary
  - output_summary
  - status
  - created_at

approval_decisions
  - id (PK)
  - ai_job_id (FK -> ai_jobs.id)
  - action_type
  - action_target_id
  - approver_id (FK -> users.id)
  - decision
  - reason
  - created_at

audit_logs
  - id (PK)
  - entity_type
  - entity_id
  - action
  - performed_by (FK -> users.id)
  - details_json
  - created_at
```

---

## 6. Database design principles

### 6.1 Relational-first design for the MVP
The initial system should be built with a relational database because:
- tickets and their history naturally fit relational structures
- access control and audit records are easier to reason about in SQL tables
- data integrity matters more than early flexibility in a workflow platform

### 6.2 Keep AI artifacts separate from operational state
AI output should not be treated as the same thing as the ticket source of truth.

Instead:
- the ticket remains the primary record
- AI runs are associated records
- the final human-safe action remains in the backend domain logic

### 6.3 Preserve history
Ticket status, assignments, and major events should not be overwritten without traceability. This supports investigation and compliance.

### 6.4 Use transactional boundaries carefully
Operations such as creating a ticket and creating its initial status history should be transactional as a unit. AI job creation can be asynchronous and separate.

---

## 7. Suggested schema conventions

- use UUIDs or strong numeric IDs for entity identity
- store timestamps in UTC
- manage schema creation and evolution with versioned Flyway migrations
- run Hibernate in validation mode so application startup never changes the schema implicitly
- keep audit trails immutable in practice
- use enums or constrained values for statuses, priorities, categories, and roles
- store large payloads such as file metadata and JSON details separately when practical

---

## 8. Initial MVP data scope

For the first implementation, it is enough to include:

- users
- teams and memberships
- tickets
- ticket status history
- comments
- assignments
- AI jobs
- AI evidence
- audit log

This set provides a complete operational loop without overengineering the database before the product is validated.

---

## 9. How this maps to requirements

This domain model supports the requirements in the earlier document:

- FR-001 / FR-003 / FR-004 / FR-005 -> ticket, status history, assignment
- FR-006 -> comment
- FR-007 -> team and team_members
- FR-010 -> audit_log
- FR-011 / FR-012 / FR-020 -> ai_jobs, ai_job_evidence, ai_job_tool_calls
- FR-013 -> approval_decisions
- FR-015 -> dashboard views can aggregate by ticket status/team/priority
- FR-016 -> search and filtering across ticket and user data

This gives a strong traceability chain from requirement to data model.

---

## 10. Recommended next step

The next artifact should be the API contract and endpoint design, built around this domain model. That should define:

- ticket endpoints
- auth endpoints
- assignment endpoints
- comment endpoints
- dashboard endpoints
- AI triage endpoints
- approval endpoints

This creates the bridge between schema and implementation.

---

## 11. Conclusion

The domain model for Solvix centers on tickets, status evolution, ownership, comments, knowledge, AI workflow execution, and auditability. It is intentionally designed to support a credible workflow product with AI augmentation while preserving safety, traceability, and accountability.

This is the foundation for the first working vertical slice and for all future engineering expansion.
