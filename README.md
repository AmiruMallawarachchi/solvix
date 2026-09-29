# solvix
AI-assisted enterprise operations platform — ticketing + agentic AI resolution layer.

## Documentation

- [Project charter](./docs/00-project-charter.md)
- [Product requirements](./docs/01-product-requirements.md)
- [System architecture](./docs/02-system-architecture.md)
- [Domain model and database design](./docs/03-domain-model-and-database.md)
- [API specification](./docs/04-api-specification.md)
- [Security and threat model](./docs/05-security-threat-model.md)
- [AI workflow design](./docs/06-ai-workflow-design.md)
- [Production readiness roadmap](./docs/07-production-readiness-roadmap.md)

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

The frontend defaults to local username/password login. Copy [frontend/.env.example](./frontend/.env.example) to `frontend/.env.local` to customize its API URL. Cognito mode is for a provisioned production user pool; required app-client and API settings are documented in the [production readiness roadmap](./docs/07-production-readiness-roadmap.md).
