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

### Documentation index

- [docs/](./docs)

## Local backend setup

The backend now persists tickets in PostgreSQL, with a lightweight Docker Compose setup for local development.

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run
```

The default Postgres connection is configured for:

- database: `solvix`
- username: `solvix`
- password: `solvix`

The API still exposes the existing ticket endpoints at `http://localhost:8080/api/v1/tickets`.
