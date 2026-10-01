# solvix
AI-assisted enterprise operations platform — ticketing + agentic AI resolution layer.

> **Live demo:** [Open Solvix](https://frontend-topaz-nine-06hcz5dhs9.vercel.app/)
>
> The portfolio demo runs on Vercel, Render, and Neon free-plan services. It uses synthetic data and may have a cold start after inactivity; it is not an always-on production service.

**Demo account:** `solvix-demo-user` / `SolvixDemo-2026!Ticket`

## Project highlights

- Spring Boot 3 / Java 17 API, Next.js UI, PostgreSQL, Flyway migrations, Docker, and CI.
- Cognito support in the production authentication profile; local login remains the default development flow.
- Staged [AWS production architecture](./docs/07-production-readiness-roadmap.md), documented separately from the low-cost portfolio demo.

## Documentation

- [Project charter](./docs/00-project-charter.md)
- [Product requirements](./docs/01-product-requirements.md)
- [System architecture](./docs/02-system-architecture.md)
- [Domain model and database design](./docs/03-domain-model-and-database.md)
- [API specification](./docs/04-api-specification.md)
- [Security and threat model](./docs/05-security-threat-model.md)
- [AI workflow design](./docs/06-ai-workflow-design.md)
- [Production readiness roadmap](./docs/07-production-readiness-roadmap.md)
- [Portfolio demo deployment plan](./docs/08-portfolio-demo-deployment.md)

### Documentation index

- [docs/](./docs)

## Local backend setup

The backend persists tickets in PostgreSQL, with a lightweight Docker Compose setup for local development. Configure the required bootstrap credentials and JWT signing secret as described in [backend setup](./backend/README.md).

```powershell
docker compose up -d postgres
```

Start the backend in one PowerShell window:

```powershell
cd backend
mvn spring-boot:run
```

Then start the web app in another window:

```powershell
cd frontend
npm ci
npm run dev
```

The UI is available at `http://localhost:3000`; the API is at `http://localhost:8080`.

To run PostgreSQL and the API in containers instead, copy `.env.example` to `.env` and run `docker compose up --build -d`. PostgreSQL is exposed on host port `5433`; the API remains on `8080`. The example credentials are for local development only. Start the frontend as shown above.

The frontend defaults to local username/password login. Copy [frontend/.env.example](./frontend/.env.example) to `frontend/.env.local` to customize its API URL. Cognito mode is for a provisioned production user pool; required app-client and API settings are documented in the [production readiness roadmap](./docs/07-production-readiness-roadmap.md).
