# Solvix System Architecture

## 1. Architectural intent

Solvix is designed as a real operational platform for issue intake, triage, investigation, team coordination, and auditability. AI is included as a bounded subsystem that supports decision-making, evidence retrieval, and workflow assistance without directly owning high-impact business actions.

The architecture follows a practical, portfolio-credible model:

- a user-facing application for ticket workflows
- a central backend for business logic and authorization
- a structured database for operational records and history
- an AI service for classification, retrieval, and workflow orchestration
- asynchronous processing for AI and background tasks
- explicit approval checks for protected actions
- secure observability and auditability across the system

This is intentionally not designed as a distributed microservices platform from day one. The initial architecture should be structured to allow future modularization without creating unnecessary complexity.

---

## 2. Architectural principles

### 2.1 Software system first
Solvix is primarily a workflow and operations platform. AI provides assistance, not full autonomous control.

### 2.2 Human accountability remains central
High-impact operations such as status changes, assignment updates, or external actions require human approval.

### 2.3 Clear separation of concerns
Core business logic should be separated from AI orchestration, retrieval logic, and external integrations.

### 2.4 Evidence-driven operations
Users and AI workflows should operate with ticket context, historical record, and relevant knowledge attached to the case.

### 2.5 Observability and traceability
Every meaningful state change and AI action must be auditable.

### 2.6 Incremental evolution
The architecture should support a modular monolith or layered backend that can evolve into more granular components as the product matures.

---

## 3. Architectural view: C4 Context

### Level 1 — System context

```
+-------------------+            +--------------------------+
| Customer /        |            | Solvix                  |
| Requester         |  requests  |                          |
+-------------------+----------->| Ticketing + AI Workflow  |
                                  |                          |
+-------------------+            | +----------------------+ |
| Support Agent     |  uses      | | issue intake        | |
+-------------------+----------->| triage               | |
                                  | assignment           | |
                                  | evidence retrieval   | |
                                  | audit & dashboards   | |
                                  +----------------------+ |
                                            |
                                            v
                                  +----------------------+
                                  | External systems     |
                                  | docs, knowledge,     |
                                  | notifications,       |
                                  | identity providers   |
                                  +----------------------+
```

### What this answers
This view explains who interacts with Solvix and what broader environment it depends on.

Main actors:
- Customer / Requester
- Support Agent
- Developer
- Team Lead
- Manager
- Administrator

Main external dependencies:
- user identity / auth provider
- knowledge repositories
- notification systems
- optional external operational tools

---

## 4. C4 Container view

### Level 2 — Containers

```
+----------------------+
| Web Client / Browser |
| Next.js App          |
+----------+-----------+
           |
           | HTTPS / API
           v
+----------------------+
| API Layer            |
| Backend Service      |
| (Auth, Tickets,      |
|  Teams, Dashboard)   |
+----------+-----------+
           |
           +-------------------------------+
           |                               |
           v                               v
+-------------------+           +------------------------+
| Relational DB    |           | Cache / Queue Layer    |
| PostgreSQL       |           | Redis / async workers  |
+-------------------+           +-----------+------------+
                                              |
                                              v
                                  +------------------------+
                                  | AI Service             |
                                  | LangGraph + Retrieval  |
                                  | Classification + Ops   |
                                  +-----------+------------+
                                              |
                                              v
                                  +------------------------+
                                  | Knowledge Sources      |
                                  | docs, tickets, KB      |
                                  +------------------------+
```

### Container responsibilities

#### 1. Web application
Provides the front-end experience for:
- login
- ticket creation
- ticket details
- assignment and status updates
- dashboards and reports

#### 2. Backend API
Owns the core business capabilities:
- authentication and authorization
- ticket lifecycle logic
- role and team management
- comments and history
- audit logging
- dashboard data aggregation

#### 3. Database
Stores:
- users and roles
- teams and membership
- tickets and ticket lifecycle
- comments and attachments
- audit records
- AI results and telemetry metadata

#### 4. Queue / background worker layer
Handles asynchronous work including:
- AI classification jobs
- evidence retrieval jobs
- notifications
- retries and failure processing

#### 5. AI service
Hosts the AI execution path:
- context assembly
- retrieval and ranking
- LangGraph orchestration
- tool policy enforcement
- approval checks
- output validation

---

## 5. C4 Component view

### Level 3 — AI service component breakdown

```
AI Service
  |
  +-- Input Validator
  |
  +-- Context Builder
  |      + ticket details
  |      + recent history
  |      + team context
  |      + relevant knowledge
  |
  +-- Retriever / RAG Pipeline
  |      + search index
  |      + knowledge sources
  |      + ranking / filtering
  |
  +-- LangGraph Orchestrator
  |      + workflow steps
  |      + tool decisions
  |      + state transitions
  |
  +-- Tool Policy / MCP Gateway
  |      + allowed actions
  |      + permissions
  |      + execution guardrails
  |
  +-- Output Validator
  |      + schema enforcement
  |      + business-rule validation
  |
  +-- Approval Manager
  |      + requires approval for sensitive actions
  |
  +-- AI Telemetry
  |      + logs, traces, failures, metrics
  |
  +-- Result Writer
         + persist classification / summary / evidence metadata
```

### Component responsibilities in the backend

The backend API can also be segmented conceptually as:

```
Backend Service
  |
  +-- Auth Module
  |
  +-- Ticket Service
  |
  +-- Team / User Service
  |
  +-- Comment Service
  |
  +-- Assignment Service
  |
  +-- Audit Service
  |
  +-- Dashboard Service
  |
  +-- Notification Service
  |
  +-- Workflow Orchestrator
  |
  +-- Policy Enforcement Layer
```

This structure keeps business processes authoritative while AI remains a specialized support layer.

---

## 6. Runtime workflow

### 6.1 Ticket creation and triage flow

```
Customer / Support User
        |
        v
Web App
        |
        v
Backend API
        |
        +--> Validate request
        +--> Authorize user
        +--> Create ticket record
        +--> Write audit event
        +--> Enqueue AI triage job
        v
Queue / Worker
        |
        v
AI Service
        |
        +--> Build ticket context
        +--> Retrieve relevant evidence
        +--> Classify / route / summarize
        +--> Validate result schema
        +--> Store AI output and telemetry
        v
Backend API
        |
        +--> Present classification to user / team
```

### 6.2 Protected action flow

```
AI Service proposes action
        |
        v
Approval Manager
        |
        +--> requires approval for protected action
        |
        +-- if approved --> Backend API executes action
        |
        +-- if rejected --> action not performed; audit recorded
```

This is a critical architecture pattern for Solvix: the AI may propose or recommend, but the backend remains responsible for actual state change.

---

## 7. Core domain boundaries

### 7.1 Identity and access
Ownership of all protected actions and data access sits in the authentication and authorization layer.

Responsibilities:
- user authentication
- role definition
- permissions and policy enforcement
- session handling
- access checks for ticket and dashboard endpoints

### 7.2 Ticketing and workflow
This is the core business domain.

Responsibilities:
- ticket lifecycle
- assignments
- comments
- status transitions
- priority and categories
- history and event logging

### 7.3 AI and evidence layer
This is a supporting domain that interprets, enriches, and assists operational work.

Responsibilities:
- classification
- summarization
- retrieval from knowledge sources
- workflow orchestration
- tool execution within policy constraints

### 7.4 Observability and audit
This cross-cutting domain records what happened and why.

Responsibilities:
- system logs
- AI job telemetry
- approval records
- audit history for operational accountability

---

## 8. Data flow principles

The system should follow a few disciplined rules:

1. Business writes originate from backend services, not directly from AI.
2. AI outputs are data, not autonomous commands.
3. Protected actions require policy evaluation and human approval.
4. Ticket records are the source of truth for operational state.
5. AI telemetry is retained for evaluation and debugging.

This creates a reliable pattern for compliance, safety, and operational control.

---

## 9. Security architecture implications

The architecture must include explicit security boundaries:

- authenticated users only for protected routes
- authorization before ticket access or mutation
- backend-level enforcement of all state-changing actions
- AI tool access restricted by policy
- approval gating before executing sensitive actions
- audit log separation from the normal business data flow
- no direct persistence of secrets in code or insecure config

These concerns are not optional add-ons. They are part of the architecture itself.

---

## 10. Deployment architecture (conceptual)

```
+---------------------+
| Internet / Users    |
+----------+----------+
           |
           v
+---------------------+
| Web Application     |
| Next.js             |
+----------+----------+
           |
           v
+---------------------+
| Backend API         |
| Node / Java / etc.  |
+----------+----------+
           |
     +-----+------+-----+
     |                  |
     v                  v
+-----------+      +-------------------+
| PostgreSQL|      | Background Jobs  |
| primary   |      | workers          |
+-----------+      +----------+--------+
                              |
                              v
                      +-------------------+
                      | AI Service        |
                      | LangGraph + RAG   |
                      +-------------------+
```

### Deployment attributes
- web app and API can be deployed together or separately in the first implementation
- database is the primary transactional store
- async job workers run platform tasks without blocking user workflows
- AI service is isolated enough to fail independently without corrupting ticket state

---

## 11. Architectural trade-offs

### Why not microservices immediately?
A full microservices design would add:
- deployment complexity
- inter-service contract management
- operational burden
- debugging and tracing overhead

For a single-person or small-team project, a modular monolith or layered backend is a superior early decision.

### Why not allow direct AI mutations?
Direct AI mutation would introduce:
- unsafe state changes
- harder auditability
- more difficult permissions enforcement
- higher risk of policy violation and approval bypass

The current architecture keeps AI inside a controlled harness with strong backend oversight.

---

## 12. Expected architecture footprint

The initial implementation is expected to look like:

- frontend application for ticket workflows
- backend API with domain services
- PostgreSQL for primary system state
- Redis or a similar queue/cache layer for async processing
- AI service for classification and retrieval
- knowledge store or metadata store for searchable context
- audit/event logging for operational history

This is a realistic and technically defensible architecture for the MVP.

---

## 13. Architectural deliverables to follow

The architecture phase should next produce:

1. Domain model and entity relationships
2. Database schema and ERD
3. API contract specification
4. Security model and threat model
5. ADRs for major architectural decisions
6. MVP implementation plan with vertical slices

These artifacts will turn the architecture from a high-level concept into an engineering plan.

---

## 14. Conclusion

Solvix should be designed as a structured business workflow platform with AI integrated as a governance-aware support layer. The architecture makes the ticketing system the source of truth, keeps human accountability in the loop, and ensures that AI operates in a controlled, measurable, and auditable environment.

This is the kind of architecture that demonstrates real software engineering discipline and makes the project credible as both a product and an AI-enabled system.
