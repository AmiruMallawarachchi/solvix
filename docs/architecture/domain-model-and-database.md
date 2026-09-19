# Domain Model and Database Design

## Overview

This document captures the core Solvix domain model and the database structure that supports the ticket workflow and AI assistance layer.

## Core entities

```mermaid
erDiagram
    USER ||--o{ TICKET : creates
    USER ||--o{ COMMENT : writes
    USER ||--o{ TEAM_MEMBER : belongs_to
    TEAM ||--o{ TEAM_MEMBER : includes
    TEAM ||--o{ TICKET : owns
    TICKET ||--o{ COMMENT : has
    TICKET ||--o{ STATUS_HISTORY : tracks
    TICKET ||--o{ ATTACHMENT : contains
    TICKET ||--o{ AI_JOB : triggers
    AI_JOB ||--o{ AI_EVIDENCE : generates
    AI_JOB ||--o{ TOOL_CALL : invokes
    AI_JOB ||--o{ APPROVAL : requires
    USER ||--o{ AUDIT_LOG : performs

    USER {
        string id
        string email
        string role
        string full_name
    }

    TEAM {
        string id
        string name
    }

    TICKET {
        string id
        string title
        string description
        string status
        string priority
        string category
        datetime created_at
    }

    COMMENT {
        string id
        string body
        datetime created_at
    }

    STATUS_HISTORY {
        string id
        string previous_status
        string new_status
        datetime created_at
    }

    ATTACHMENT {
        string id
        string file_name
        string storage_uri
    }

    AI_JOB {
        string id
        string job_type
        string status
        datetime created_at
    }

    AI_EVIDENCE {
        string id
        string title
        string relevance
    }

    TOOL_CALL {
        string id
        string tool_name
        string status
    }

    APPROVAL {
        string id
        string decision
        datetime created_at
    }

    AUDIT_LOG {
        string id
        string action
        datetime created_at
    }
```

## Major tables

- users
- teams
- team_members
- tickets
- ticket_status_history
- comments
- attachments
- ai_jobs
- ai_job_evidence
- ai_job_tool_calls
- approval_decisions
- audit_logs
- knowledge_items

## Design principles

- Ticket is the source of truth.
- AI artifacts are linked but not authoritative.
- Status and assignment history must be retained.
- Audit log should capture meaningful changes.
- The schema should support search, filtering, and operational dashboards.

## Summary

The database is designed to support workflow processing, traceability, and AI-assisted operational decision-making without allowing AI to directly mutate protected state without validation and approval.
