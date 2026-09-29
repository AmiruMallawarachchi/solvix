# Solvix Frontend

A small Next.js and TypeScript interface for the current human ticket-workflow MVP.

## Features

- Customer and support-agent sign-in
- Ticket creation and role-scoped ticket listing
- Ticket details, comments, and activity history
- Support-agent assignment and legal status transitions

AI, teams, dashboards, search, notifications, and attachments are outside this MVP.

## Run locally

Start the backend and PostgreSQL as described in [the backend README](../backend/README.md). Then:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. The browser calls `http://localhost:8080` by default. Override the API address with `NEXT_PUBLIC_API_URL` if needed; configure the backend's `SOLVIX_CORS_ALLOWED_ORIGIN` to match the frontend origin.

The UI keeps the JWT in memory only. Refreshing the page signs the user out.

## Validate

```powershell
npm run lint
npm run typecheck
npm run build
```
