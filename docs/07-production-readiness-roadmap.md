# Solvix Product and Pilot Roadmap

## Purpose and current position

Solvix has a working ticket workflow foundation: a Spring Boot API, Next.js interface, PostgreSQL persistence and Flyway migrations, ticket ownership checks, support-agent operations, comments, lifecycle history, and automated backend/frontend checks. The public deployment is a synthetic-data portfolio demo, not a customer service or production pilot.

The next target is a secure, invite-only single-organization pilot on the current Vercel, Render, and Neon hosting setup. Keep the Spring Boot modular monolith and add capabilities in reviewable vertical slices. AWS deployment and multi-tenant SaaS are explicitly deferred; the existing AWS materials are future architecture documentation only.

## Agreed decisions and boundaries

- Start with one organization and a small, known pilot group. Do not claim multi-tenant isolation.
- Keep the public demo isolated from the pilot, synthetic-data-only, and clearly labelled as a demo.
- Continue using Vercel, Render, and Neon for this roadmap. Recheck current service limits and costs before relying on free tiers.
- Use Clerk as the proposed managed identity provider only after confirming current free-tier, production-domain, role/organization, and hosting fit. Pause before integration if any required capability is unavailable or has unacceptable cost.
- Use Groq as the initial AI provider, behind a backend provider interface. Keep the API key only in server-side secret configuration.
- AI may prepare advice and proposals. It must not autonomously change ticket status, assignment, or send customer communications. Require explicit approval by an authorized human and audit the decision and execution.
- Do not use real customer data until privacy, access, retention, support, and recovery controls have passed the pilot gates.
- Do not provision AWS as part of this plan. Revisit it only as a separately approved and costed effort.

## Delivery stages

Every stage is a separately reviewable slice with acceptance evidence. A capability is not considered implemented merely because it appears in the architecture or requirements.

### 1. Product contract and measurable pilot gates

- Reconcile the charter, requirements, architecture, API, and data model with the single-organization pilot and accurately distinguish implemented, planned, and deferred capabilities.
- Specify roles/capabilities, ticket visibility, team ownership, categories, lifecycle rules, retention/deletion, and customer communication.
- Agree measurable pilot gates for access control, expected workload, supported browsers, response time, service health, recovery, retention, operational ownership, and cost alerts.
- Map each included acceptance criterion to an automated test or recorded verification.

**Gate:** scope and pilot acceptance criteria are explicit, consistent, testable, and accepted by the project owner.

### 2. Delivery foundations and identity-provider validation

- Verify the current repository branch, worktree, PR state, and baseline before each slice; preserve all user changes.
- Keep `main` releasable and use focused `feat/<scope>`, `fix/<scope>`, or `docs/<scope>` branches, conventional commit messages, and a PR per slice.
- Extend CI coverage to backend, frontend, Docker/Compose, database migrations/integration tests, AI/worker/evaluation code, infrastructure, and relevant documentation.
- Use non-production secrets and data for previews. Promote production only from reviewed, passing changes merged to `main`; require explicit review for authentication, authorization, and database changes.
- Validate Clerk's current pricing/limits, custom-domain requirement, supported role/organization model, and integration with the current frontend/backend before implementation.
- Document secret ownership, deploy health checks, rollback steps, and separation among public demo, previews, and pilot.

**Gate:** delivery checks and preview isolation are working; Clerk is viable for the agreed pilot or the identity decision is revisited before code integration.

### 3. Secure API, PostgreSQL, and identity

- Complete API validation, bounded pagination/filtering, consistent error handling, concurrency behavior, and durable audit events.
- Integrate Clerk OIDC/JWT validation in the backend. Keep local test/development authentication isolated from deployed environments.
- Map authenticated identities to application users and enforce approved customer, support-agent, developer, team-lead, manager, and administrator capabilities server-side.
- Enforce owner/team scope on all reads and writes, including search, dashboards, attachments, and AI context as those surfaces are added.
- Test against PostgreSQL, not only H2. Verify forward-only Flyway migrations from a representative existing database and exercise backup/restore procedures.
- Add appropriate rate limits, secure CORS/headers, dependency and secret checks, and an ASVS-informed security checklist; make no certification claim.

**Gate:** golden ticket workflows pass with managed identity; negative tests prove user/team isolation; PostgreSQL migration and recovery evidence is recorded.

### 4. Complete ticket workflows and agent/customer experience

- Complete responsive, accessible customer and agent flows for intake, details, comments, status, priority, assignment, and activity.
- Add server-side, bounded, permission-aware search, filtering, sorting, and pagination. Keep UI behavior consistent with API semantics.
- Ensure authorization remains a backend responsibility; display appropriate loading, empty, validation, and recovery states in the UI.

**Gate:** critical workflows pass automated API/UI coverage and keyboard, mobile, and supported-browser checks.

### 5. Teams and administration

- Add team and membership models, assignment history, user status, and approved roles with forward-only migrations.
- Add secured team/user/role administration and audit all privilege and membership changes.
- Test both allowed and denied operations, including cross-team access, disabled users, and privilege changes.

**Gate:** administration is least-privileged and access-isolation tests pass.

### 6. Governed knowledge and evidence

- Add reviewed, versioned knowledge records with provenance, publication state, and access scope.
- Start with permission-aware PostgreSQL text search. Add vector retrieval only if a reviewed evaluation set demonstrates a material improvement.
- Preserve source references so agents can inspect evidence; never retrieve data outside the requesting user's permissions.

**Gate:** evidence is traceable, current, and permission-scoped.

### 7. Asynchronous Groq AI triage

- Add a backend provider interface and call Groq only from server-side code. Configure the key as a Render secret, never in browser-visible variables, source control, or logs.
- Persist job and suggestion state; choose a durable worker/retry mechanism only after validating Render/Neon constraints. Include idempotency, bounded retries, timeouts, visible failures, and a kill switch.
- Begin with advisory category/priority/summary, rationale, permitted evidence, and optional draft reply. Validate structured output and record model, prompt, and source metadata.
- Treat ticket and knowledge content as untrusted input. Implement data minimization, redaction, prompt-injection/data-leakage tests, quality evaluation, and usage/cost limits.
- Bind each approval to the exact proposal. Only an authorized human may approve; record approval/rejection and any execution. Never automatically change state, assign work, or contact customers.

**Gate:** the ticket workflow remains usable when AI is unavailable; AI is opt-in, auditable, permission-scoped, evaluated, and explicitly disableable.

### 8. Notifications and private attachments

- Add notification preferences, delivery state, bounded retries, and privacy/opt-out handling using a validated hosting-compatible provider.
- Add private attachment storage with authorized short-lived access, size/type limits, malware scanning before trusted use, audit metadata, and retention/deletion rules.
- Select providers only after validating security, durability, and cost; do not assume a particular queue or object store before that review.

**Gate:** delivery failures are visible and recoverable; unauthorized users cannot retrieve attachments or notification content.

### 9. Dashboards and operational readiness

- Add role-scoped backlog, status, priority, workload, and resolution reporting with documented metric definitions.
- Set performance budgets and use measured query plans before adding indexes or separate search infrastructure.
- Add health/readiness checks, structured logs/metrics, alerting, incident/support runbooks, operational ownership, retention/deletion procedures, and deployment rollback guidance.
- Exercise backup restore and recovery in a production-like environment.

**Gate:** operators can detect and respond to common failures; restore and rollback have been exercised and evidenced.

### 10. Controlled pilot release

- Run production-like end-to-end, migration, authorization, accessibility, browser, load/resilience, restore, and focused security validation.
- Resolve critical/high security findings before inviting pilot users. Describe independent review accurately; do not claim certification.
- Invite only known pilot users. Keep synthetic data until the owner accepts privacy, access, retention, support, and recovery controls.
- Deploy only from reviewed `main` changes, perform post-deploy smoke checks, monitor errors/latency/usage/cost, and retain an actionable rollback path.

**Gate:** all agreed pilot criteria pass and the owner accepts documented limitations before real data is introduced.

## Branch, commit, CI/CD, and database workflow

1. Keep `main` releasable. Start each slice from the latest approved `main` using `feat/<scope>`, `fix/<scope>`, or `docs/<scope>`. If the current product baseline is still on an unmerged branch, integrate that baseline through review rather than silently branching future releases from the starter scaffold.
2. Keep commits focused and use prefixes such as `feat:`, `fix:`, `test:`, `docs:`, and `ci:`. Never commit credentials or bundle unrelated changes.
3. Open a PR for each slice. Require relevant CI checks and explicit review for identity, authorization, secret, and schema changes. Merge only after checks pass.
4. Previews use non-production secrets and data. Production promotion is from approved changes merged to `main`; changes with migration/auth risk include a reviewed rollout and rollback strategy.
5. Use an isolated Neon branch for schema work where practical. Inspect schema diffs, verify forward migrations against representative data, and never direct preview/test automation to production data.
6. Run post-deploy smoke checks and inspect service health/logs. Roll back the application or use a reviewed database recovery procedure; never improvise a destructive migration rollback.
7. Update relevant docs and record acceptance evidence in the PR.

## Production readiness definition

Production readiness is a release gate, not a successful deploy alone. For the pilot capabilities, require:

- explicit requirements and acceptance criteria
- tested authentication, authorization, and data isolation
- repeatable PostgreSQL migrations and a successful restore exercise
- managed secrets, least privilege, and secure network transport
- relevant CI checks and a reviewed deploy/rollback path
- monitored health/errors/capacity and cost
- agreed recovery, retention, and support ownership
- security and accessibility verification appropriate to exposure

This checklist does not imply an SLA, certification, or multi-tenant isolation.

## Open validations

- Recheck Clerk free-tier terms, production-domain requirements, role/organization fit, and Vercel compatibility before implementing identity.
- Recheck Vercel, Render, Neon, Groq, notification, and attachment-provider quotas/terms/costs when each capability is scheduled.
- Agree numeric workload, performance, recovery, retention, and cost-alert targets with the pilot owner before production launch.
- Confirm who operates and supports the pilot, who can access its data, and how retention/deletion requests are handled.
- Revisit AWS and multi-tenant architecture only as separately approved scope.
