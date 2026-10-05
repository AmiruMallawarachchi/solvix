# Solvix AI Workflow Design

## Purpose

This document explains the initial AI design for Solvix and defines how the AI subsystem fits inside the larger operational system.

## Delivery status

AI triage and evidence retrieval are part of the longer-term product vision and are not implemented in the current deployment. The human ticket workflow is the foundation for a later AI stage. The planned first provider is Groq, called from the backend with credentials held only in server-side secrets. Add AI only after identity, permission-scoped knowledge, and operational foundations are ready; see the [production readiness roadmap](./07-production-readiness-roadmap.md).

## AI design principles

- AI assists workflow execution; it does not own business state.
- model outputs are validated before use
- the backend service remains the source of truth for state changes
- human approval is required for protected actions
- all AI executions are logged, measured, and traceable

## High-level AI flow

```text
Ticket
  -> Input validation
  -> Context builder
  -> Permission-scoped evidence retrieval
  -> Policy checks
  -> LangGraph orchestration
  -> Tool execution within guardrails
  -> Structured output validation
  -> Human approval for protected actions
  -> Backend action execution
  -> Audit + telemetry
```

## Core AI tasks

- ticket triage and categorization
- support routing suggestions
- evidence retrieval and summarization
- context assembly from ticket and historical records
- workflow recommendation and policy-aligned actions

## Guardrails

- allow-listed tools only
- permission-aware retrieval
- schema validation for model output
- human approval before consequential state-changing operations or external communication
- server-side provider credentials; never return secrets to the browser or log them
- asynchronous execution with bounded retries, timeouts, cost limits, and a disable switch
- treat ticket and knowledge content as untrusted input; test prompt-injection and data-leakage defenses
- explicit logging of job metadata, decisions, and tool use

## Detailed design

The detailed AI workflow, tool policy, and governance design should live in the AI engineering docs and implementation artifacts as the project matures.
