# Solvix Security and Threat Model

## Purpose

This document is the root-level security baseline for the project. It summarizes the security model, critical risk areas, and control strategy for Solvix.

## Security model summary

- identity and access are required for all protected operations
- authorization is enforced at the backend boundary
- AI actions do not directly mutate protected business state
- human approval is required for sensitive AI-driven actions
- audit logging tracks state-changing behavior and approvals
- data exposure is reduced through least privilege and scoped retrieval

## Primary threats

- broken authorization
- SQL injection
- prompt injection and unsafe AI outputs
- unauthorized data access through AI retrieval
- duplicate execution and retry problems
- secret leakage and config exposure
- incomplete audit coverage

## Detailed design

See [security/threat-model.md](./security/threat-model.md) for the full threat model and OWASP-aligned control checklist.
