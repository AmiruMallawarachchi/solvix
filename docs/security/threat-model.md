# Solvix Security Model and Threat Model

## 1. Security intent

Solvix handles customer issues, internal operational workflows, and potentially sensitive business and technical data. Security is therefore a first-class product concern, not a later stage afterthought.

The system should enforce:

- authenticated access
- role-based authorization
- least-privilege access
- protected AI actions with human approval
- full auditability of state-changing operations
- prevention of data leakage and unauthorized system actions

---

## 2. Security principles

### Principle 1: Trust the backend, not the client
All business-critical validation and authorization decisions should be enforced on the server side.

### Principle 2: AI is not an unrestricted executor
The AI service may generate recommendations or proposals, but backend logic must execute protected state changes under policy and approval control.

### Principle 3: Audit everything meaningful
Ticket updates, assignments, comments, AI decisions, and approvals must be recorded.

### Principle 4: Data minimization and least privilege
Users should only access the data required for their role and scope.

### Principle 5: Secure by default
Secrets, tokens, and internal configuration should not be committed to source control or exposed unnecessarily.

---

## 3. Core security model

### 3.1 Identity and access

The first backend security slice uses stateless HTTP Basic authentication backed by configured application users. Credentials are supplied through environment variables and passwords are encoded with BCrypt. This is intentionally a small local/MVP step; production deployment should replace it with the organization’s identity provider and short-lived token flow.

### 3.2 Authorization model

The system should support at least:

- customer
- support_agent
- developer
- team_lead
- manager
- administrator

Authorization should be enforced at the resource layer and action layer.

The current ticket rules are:

- customers can create tickets and access only tickets they created
- customers can add comments to their own tickets
- support agents can view all tickets, assign tickets, change status, and comment
- the backend derives `createdBy` from the authenticated principal rather than trusting request data

Examples:
- a support agent may view assigned tickets
- a developer may work on escalated technical issues
- a manager may view dashboards but not modify restricted operational records
- an admin may manage users, roles, and system settings

### 3.3 Protected actions

Protected actions include:

- status transitions
- assignment changes
- ticket deletion or archival
- approval execution
- any action that modifies system state based on AI recommendations

These must pass through policy validation and, when required, human approval.

---

## 4. Threat model

### Threat: Broken authorization
Risk: a user gains access to another customer’s or team’s ticket data.

Mitigation:
- enforce role and ownership checks on every request
- ensure the backend authorizes all state-changing operations
- reject cross-tenant or cross-team access unless explicitly allowed

### Threat: SQL injection
Risk: malicious input reaches a query layer and alters database logic.

Mitigation:
- parameterized queries
- server-side validation
- proper ORM or query builder usage
- defensive validation and DB access patterns

### Threat: Prompt injection or manipulation of AI context
Risk: retrieved knowledge or user-provided content manipulates the model into unsafe or unauthorized output.

Mitigation:
- strict input validation
- policy-based tool restrictions
- schema-validated model outputs
- separation of data from command execution
- human approval for protected actions

### Threat: Data exfiltration through AI workflows
Risk: the AI system accesses information outside the current ticket context or user permission scope.

Mitigation:
- scoped retrieval context
- explicit allowlists for tools and access
- permission-aware context building
- logging of retrieved evidence sources and access

### Threat: Duplicate execution
Risk: retries or queue processing accidentally trigger the same action multiple times.

Mitigation:
- idempotent operation design
- deduplication keys
- approval records tied to action instances
- event design that avoids repeated state mutation

### Threat: Secret leakage
Risk: credentials or tokens are accidentally committed or exposed.

Mitigation:
- environment variables or secret stores
- .gitignore protections
- code review requirements
- secret scanning in CI

### Threat: Incomplete audit trails
Risk: actions occur without traceability, making investigations impossible.

Mitigation:
- log status transitions, assignments, comments, and AI actions
- tie events to actor and timestamp
- preserve approval decisions and policy outcomes

---

## 5. OWASP-aligned verification checklist

Solvix should be reviewed against relevant OWASP ASVS principles, including:

- authentication strength and session management
- authorization and entitlement enforcement
- input validation and output encoding
- secure data storage and management
- logging and monitoring
- secure communications
- secure dependency and configuration management

The goal is not to claim compliance without evidence, but to perform a structured review using standards-based controls.

---

## 6. AI-specific security controls

The AI subsystem requires explicit governance.

Required controls:

- restrict tool access to policy-approved actions
- validate all model output against expected schema
- treat all retrieved evidence as untrusted until reviewed
- require approval before executing protected business actions
- record tool calls and evidence retrieval for later review
- log failures, retries, and policy rejections

This is the most important design difference between a toy AI demo and a credible AI-enabled workflow platform.

---

## 7. Security evaluation plan

As the project matures, validate with:

- unit tests for validation and authorization rules
- integration tests for ticket access controls
- API security tests for unauthorized access paths
- AI safety checks for tool policy enforcement
- audit verification tests for meaningful event capture
- secret scanning in CI

---

## 8. Conclusion

Solvix should be designed with security as a core product property. The system must protect user data, enforce authorization, preserve auditability, and keep AI execution inside a controlled harness. That is what makes the platform credible as an operational software system with AI augmentation rather than an uncontrolled or risky AI prototype.
