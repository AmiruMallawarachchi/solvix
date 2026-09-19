# ADR-0001: Documentation as Code

- Status: Accepted
- Date: 2026-09-19

## Context

The Solvix project needs a structured way to track product definition, architecture, requirements, and engineering decisions in a reviewable and maintainable format.

Without a disciplined documentation approach, the project would become harder to reason about as the codebase grows. We also need a way to present the project clearly to interviewers, reviewers, and future contributors.

## Decision

We will keep project documentation in version-controlled Markdown files within the repository, supported by focused architecture and API references.

We will not rely on a single giant README as the only source of engineering truth.

## Consequences

### Positive
- documentation is searchable and reviewable in GitHub
- changes are versioned alongside code
- the project can present a clean engineering narrative
- internal and external reviewers can understand the system quickly

### Negative
- documentation must be maintained alongside code
- the team must keep the docs concise and practical

## Alternatives considered

### Single monolithic README
This was rejected because it becomes difficult to navigate and maintain as the project grows.

### Heavy wiki or external docs platform
This was rejected because it adds setup and governance overhead for a project that is still in an early engineering phase.
