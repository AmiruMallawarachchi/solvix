# ADR-0002: Start with a Modular Monolith

- Status: Accepted
- Date: 2026-09-19

## Context

Solvix is being built as a practical portfolio project and should solve a real operational problem without adding premature distributed-system complexity.

The project is still in the phase where the most important work is to validate the product workflow, ticket lifecycle, authorization rules, and AI-assisted triage. A microservices architecture would add deployment, coordination, and debugging overhead before those workflows are proven.

## Decision

We will begin with a modular monolith or service-oriented backend structure and keep the domain boundaries explicit.

The system will separate concerns into modules such as:
- auth
- ticketing
- teams
- dashboard
- audit
- AI workflow

This gives us maintainable structure without introducing cross-service operational complexity too early.

## Consequences

### Positive
- simpler local development and deployment
- easier onboarding for a single developer or small team
- faster vertical-slice delivery
- a clear path to future decomposition if scaling or ownership demands it

### Negative
- long-term scale may require refactoring into separate services
- domain boundaries must be maintained carefully

## Alternatives considered

### Full microservices architecture
Rejected because it introduces operational complexity and coordination overhead before the product has proven its needs.

### Unstructured single app without module boundaries
Rejected because it becomes difficult to maintain clarity, testing, and domain separation over time.
