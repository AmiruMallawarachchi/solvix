# Solvix

<p align="center">
  <img src="./frontend/public/solvix-mark.svg" alt="Solvix radial signal logo" width="76" />
</p>

**AI-assisted support operations platform for turning customer requests into
clear, accountable work.**

[![Live demo](https://img.shields.io/badge/Live%20demo-Try%20Solvix-18765d?style=for-the-badge)](https://frontend-topaz-nine-06hcz5dhs9.vercel.app/)
[![Frontend](https://img.shields.io/badge/frontend-Next.js-black)](./frontend)
[![Backend](https://img.shields.io/badge/backend-Spring%20Boot%203-6db33f)](./backend)
[![Database](https://img.shields.io/badge/database-PostgreSQL-336791)](./backend/src/main/resources/db/migration)

## Try the live demo

**[Open Solvix →](https://frontend-topaz-nine-06hcz5dhs9.vercel.app/)**

The public demo is a synthetic recruiter sandbox. It is deployed as:

```text
Browser
  ↓
Vercel · Next.js frontend
  ↓
Render · Spring Boot API
  ↓
Neon · PostgreSQL
```

Use the pre-filled demo account:

```text
Username: solvix-demo-user
Password: SolvixDemo-2026!Ticket
```

Once signed in, try the complete workflow:

1. Create a ticket with a priority.
2. Select the ticket and add a comment.
3. Review the activity history.
4. Move the ticket through its governed lifecycle.

The demo uses synthetic data only. Render's free instance may sleep after
inactivity, so the first request can take a few seconds.

## Why this project exists

Solvix demonstrates how a small operations product can be designed as a
maintainable system rather than a collection of screens. The current slice
focuses on the workflow fundamentals:

- Customer and support-agent roles with JWT authentication.
- Ticket creation, ownership, priorities, assignments, and status transitions.
- Durable comments and an activity history for auditability.
- PostgreSQL persistence managed by Flyway migrations.
- A responsive Next.js workspace designed for fast triage.
- Dockerized backend deployment with provider-managed configuration.

## Engineering highlights

| Area | Implementation |
| --- | --- |
| Frontend | Next.js 16, React 19, TypeScript, responsive CSS |
| API | Spring Boot 3.4, Java 17, REST controllers |
| Persistence | PostgreSQL, Spring Data JPA, Flyway |
| Security | BCrypt passwords, short-lived JWTs, CORS allow-list |
| Delivery | Docker, Render Blueprint, Vercel, Neon |
| Quality | Backend tests, frontend lint/type-check/build, GitHub Actions |

The backend enforces the ticket lifecycle:

```text
NEW → TRIAGED → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED
```

Invalid transitions are rejected by the domain layer, and ownership is taken
from the authenticated identity rather than trusted client input.

## Repository guide

```text
frontend/   Next.js recruiter-facing workspace
backend/    Spring Boot API and Flyway migrations
docs/       Charter, requirements, architecture, security, and deployment docs
infra/      AWS production architecture and infrastructure experiments
render.yaml Render Blueprint for the public backend demo
```

Start with:

- [Project charter](./docs/00-project-charter.md)
- [Product requirements](./docs/01-product-requirements.md)
- [System architecture](./docs/02-system-architecture.md)
- [Domain model and database design](./docs/03-domain-model-and-database.md)
- [API specification](./docs/04-api-specification.md)
- [Security and threat model](./docs/05-security-threat-model.md)
- [AI workflow design](./docs/06-ai-workflow-design.md)
- [Production readiness roadmap](./docs/07-production-readiness-roadmap.md)
- [Portfolio demo deployment](./docs/08-portfolio-demo-deployment.md)

## Run locally

Prerequisites: Java 17, Maven, Node.js, npm, and Docker Desktop.

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

Start the API:

```powershell
cd backend
mvn spring-boot:run
```

Start the frontend in a second terminal:

```powershell
cd frontend
npm ci
npm run dev
```

The local UI runs at `http://localhost:3000` and the API at
`http://localhost:8080`. Copy
[frontend/.env.example](./frontend/.env.example) to
`frontend/.env.local` when you need to change the API URL or authentication
mode.

To run the API and PostgreSQL together in containers, copy `.env.example` to
`.env` and run:

```powershell
docker compose up --build -d
```

## Verification

Backend tests:

```powershell
cd backend
mvn test
```

Frontend checks:

```powershell
cd frontend
npm run lint
npm run typecheck
npm run build
```

## Deployment note

The live portfolio deployment is intentionally separate from the documented
AWS production target. The demo prioritizes a real, low-cost URL a recruiter
can open, while the repository also documents an AWS path using Cognito,
ECS/Fargate, RDS, Secrets Manager, CloudWatch, and CDK. The AWS architecture
is not represented as continuously running infrastructure.

This repository is a portfolio project, not a service for real customer
support requests. Do not place sensitive or personal data in the public demo.
