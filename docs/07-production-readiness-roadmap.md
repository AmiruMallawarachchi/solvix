# Solvix Production Readiness Roadmap

## Purpose and current position

Solvix has an MVP ticket workflow: a Spring Boot API, a Next.js user interface, PostgreSQL persistence and migrations, customer ownership checks, support-agent operations, ticket comments, lifecycle history, and automated backend/frontend checks.

The MVP is a foundation, not the complete product and not yet a production launch. The immediate deployment objective is a low-traffic, interactive portfolio demo under free-plan limits; it does not require an always-on AWS environment. See the [portfolio demo deployment plan](./08-portfolio-demo-deployment.md). This production roadmap remains the separately staged path toward the wider product described in the charter. The implementation should remain a modular monolith until measured needs justify splitting services.

## Decisions for the first production releases

- **Cloud:** AWS, initially in `ap-south-1` (Mumbai).
- **Initial cost posture:** minimize recurring spend while retaining managed security, persistence, backups, and operational visibility.
- **Initial audience:** invite-only pilot for a small, known group. Public self-service signup is disabled initially.
- **Organization model:** one organization for the initial deployment. Do not imply multi-tenant isolation until it is explicitly designed and tested.
- **Availability trade-off:** start with a single-AZ PostgreSQL deployment and automated backups. Backups are not high availability; an AZ failure may cause downtime. Revisit multi-AZ when user needs and budget are known.
- **Production identity:** Amazon Cognito using OIDC/JWT. Keep local development/test login isolated from production identity.
- **AI provider:** Amazon Bedrock, subject to model, quota, data-handling, and cost validation in the selected region.
- **AI authority:** AI can recommend and prepare proposals, but cannot directly execute consequential ticket or external actions. An authorized human must approve the exact proposal, and the decision must be auditable.
- **Data:** do not put secrets, credentials, or unapproved real customer data in source control, prompts, logs, or test fixtures.

## Delivery stages and release gates

Each stage is a separately reviewable release. The portfolio demo is a separate, synthetic-data demonstration and is not a production release or customer pilot. The first secure ticket workflow may be deployed as an invite-only beta before all later product features are ready.

### 1. Product contract and measurable launch gates

- Reconcile charter, requirements, API, domain, security, and AI documents with the agreed phased scope.
- Specify role capabilities, ticket categories, team boundaries, data classification, retention/deletion, expected workload, and supported user experience.
- Set measurable initial availability, recovery time/data loss, backup retention, log/audit retention, response-time, accessibility, and monthly cost-alert targets before production sign-off.
- Connect each acceptance criterion to automated tests or documented verification evidence.

### 2. Core workflow, identity, and security

- Complete production-grade API behavior: validation, consistent errors, pagination/filtering, concurrency handling, transactions, and durable audit coverage.
- Integrate Cognito OIDC/JWT validation; retire bootstrap-password login from production while retaining a clearly isolated local/test path.
- Implement server-side least-privilege authorization for customers, support agents, developers, team leads, managers, and administrators.
- Verify PostgreSQL behavior and forward-only migrations against PostgreSQL itself, including legacy data migration and restore testing.
- Add rate limiting, safe CORS/security headers, secret/dependency scanning, security regression tests, and an OWASP ASVS-informed review checklist. Do not claim compliance or certification without evidence.

### Cognito integration configuration

The production code now supports a Cognito authorization-code flow with PKCE in the browser and Cognito access-token validation in the API. It remains unconfigured and undeployed until a user pool and app client are provisioned.

- Configure the Cognito app client for authorization code only, public-client PKCE, and the `openid`, `email`, and `profile` scopes. Do not create or expose a client secret in the browser.
- Register the exact frontend callback and sign-out URLs with Cognito. The frontend requires `NEXT_PUBLIC_AUTH_MODE=cognito`, `NEXT_PUBLIC_COGNITO_DOMAIN`, `NEXT_PUBLIC_COGNITO_CLIENT_ID`, `NEXT_PUBLIC_COGNITO_REDIRECT_URI`, and `NEXT_PUBLIC_COGNITO_LOGOUT_URI`; see [frontend/.env.example](../frontend/.env.example).
- Configure the API with `SPRING_PROFILES_ACTIVE=production`, `COGNITO_ISSUER_URI`, `COGNITO_CLIENT_ID` (matching the frontend app client), and `CORS_ALLOWED_ORIGIN`. `COGNITO_JWK_SET_URI` can override the issuer-derived JWKS URL.
- Cognito groups must use the backend role names (`CUSTOMER`, `SUPPORT_AGENT`, `DEVELOPER`, `TEAM_LEAD`, `MANAGER`, `ADMINISTRATOR`). Unknown groups do not grant backend roles.
- The browser keeps the access token in memory; a page refresh requires signing in again. Authorization is enforced by the API, not by frontend role display or UI controls.
- Before pilot use, configure a real pool, exercise login/logout and denied-access cases in a browser, and verify the configured callback, sign-out, issuer, client ID, groups, and CORS origin. Passing local tests alone is not deployment evidence.

### 3. AWS infrastructure and first beta

Candidate architecture, subject to current service, pricing, and region verification:

- AWS CDK for infrastructure as code.
- Next.js hosting through AWS Amplify Hosting.
- The Spring Boot container on a managed AWS container service.
- Private Amazon RDS for PostgreSQL, initially single-AZ with automated backups.
- Cognito for identity; Secrets Manager for runtime secrets; CloudWatch for logs, metrics, dashboards, and alarms.
- HTTPS and a custom domain once domain ownership and DNS are available.

Release gates:

- Separate non-production and production environments; encrypt data in transit and at rest; use least-privilege IAM and private database networking.
- Add image/dependency checks, controlled schema migrations, production deployment approval, smoke tests, rollback instructions, and a tested backup restore.
- Configure budget alerts and review actual cost in the first AWS environment before inviting pilot users.
- Keep the first pilot invite-only; no public signup. Accept real user data only after privacy, access, support, and recovery controls are approved.

### 4. Teams, roles, and administration

- Add teams, memberships, expanded roles, team/user ticket ownership, and assignment history with forward-only database migrations.
- Add secure user, role, team, and membership administration.
- Verify both allowed and denied access across users, roles, teams, disabled accounts, and state-changing actions.

### 5. Search, queues, dashboards, and reporting

- Add bounded, paginated, permission-aware search and filters across ticket ID, title, status, priority, category, assignee, team, and dates.
- Add role-scoped queues and dashboards for backlog, status, priority, workload, and resolution trends.
- Define metric semantics; measure query plans and response time before adding indexes or separate search infrastructure.

### 6. Notifications, attachments, and knowledge

- Add reliable notification delivery with retries, dead-letter handling, delivery state, preferences, and appropriate opt-out behavior.
- Store attachments privately in S3 with short-lived authorized transfer URLs, type/size limits, malware scanning, audit events, and retention/deletion controls.
- Create permission-aware knowledge records with source/version provenance and a human-reviewed ingestion path.

### 7. Asynchronous Bedrock AI

- Add queued, idempotent background jobs with bounded retries, dead-letter handling, timeouts, visible state, and failure isolation from ticket operations.
- Verify Bedrock model availability, quotas, data handling, and costs in Mumbai before selecting a model.
- Start with structured triage and evidence suggestions. Validate model output, record model/prompt/version/source metadata, and evaluate against a human-reviewed dataset.
- Restrict retrieval to content the requesting user can access. Treat ticket text, uploads, and retrieved content as untrusted input; test prompt-injection and data-leakage defenses.
- Require a human approval bound to the exact proposed action before the backend executes it. Record proposals, approvals, rejections, expirations, and execution idempotently.
- Add AI spend controls, telemetry and redaction, safety regression tests, and a feature kill switch.

### 8. Production operations and broad launch

- Test end-to-end workflows, migrations, authorization, accessibility, browser behavior, performance/load, resilience, restore, and security in production-like environments.
- Complete threat-model review and independent security testing appropriate to exposure; resolve critical/high findings before broader launch.
- Provide monitored alarms, incident and recovery runbooks, operational ownership, data lifecycle procedures, patching, cost controls, and rollback.
- Roll out in controlled cohorts, verify production behavior, and revisit multi-AZ availability and multi-organization support only after explicit requirements and cost review.

## Production-ready definition

“Production-ready” means more than a successful deploy. A release must have:

- requirements and acceptance criteria for the included capabilities
- tested authorization and data isolation
- repeatable migrations and a successful backup-restore exercise
- managed secrets and least-privilege cloud roles
- CI checks and a reviewed deployment/rollback path
- monitored service health, error rates, database capacity, and cost
- agreed availability/recovery and data retention targets
- security and accessibility verification appropriate to the release
- operational ownership and a way for pilot users to report issues

No certification or availability guarantee is implied by this checklist alone.

## Open prerequisites

- An AWS account with billing alerts and deployment access.
- A custom domain and DNS access for a public HTTPS application.
- Agreed numeric launch targets for cost, traffic, availability, recovery, and retention.
- Confirmation that the selected Bedrock model is available and acceptable for the project’s data in `ap-south-1`.
- A decision on production pilot operators and support ownership.

AWS service versions, regional availability, and prices must be checked at implementation time rather than assumed from this roadmap.
