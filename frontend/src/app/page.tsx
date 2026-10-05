"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import {
  type AuthProfile,
  type AuthSession,
  beginAuth0SignIn,
  completeAuth0SignIn,
  redirectToAuth0SignOut,
} from "@/lib/auth0-auth";
import { SolvixMark } from "@/components/solvix-mark";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const AUTH_MODE = process.env.NEXT_PUBLIC_AUTH_MODE ?? "local";
const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"] as const;

type TicketStatus = "NEW" | "TRIAGED" | "ASSIGNED" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";
type TicketPriority = (typeof PRIORITIES)[number];
type QueueFilter = "ALL" | "OPEN" | "IN_PROGRESS" | "RESOLVED";

type Comment = {
  id: number;
  text: string;
  author: string;
  createdAt: string;
};

type Ticket = {
  id: string;
  title: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  createdBy: string;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
  comments: Comment[];
};

type Activity = {
  id: string;
  ticketId: string;
  type: string;
  actor: string;
  description: string;
  createdAt: string;
};

class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, token?: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: {
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    });
  } catch {
    throw new ApiError("Could not reach the Solvix API. Check that the backend is running.", 0);
  }

  const content = await response.text();
  let payload: unknown;
  try {
    payload = content ? JSON.parse(content) : undefined;
  } catch {
    payload = content;
  }

  if (!response.ok) {
    const message =
      typeof payload === "object" && payload !== null && "message" in payload
        ? String(payload.message)
        : typeof payload === "string" && payload
          ? payload
          : `The request failed (${response.status}).`;
    throw new ApiError(message, response.status);
  }

  return payload as T;
}

function nextStatuses(status: TicketStatus): TicketStatus[] {
  switch (status) {
    case "NEW":
      return ["TRIAGED"];
    case "TRIAGED":
      return ["ASSIGNED"];
    case "ASSIGNED":
      return ["IN_PROGRESS"];
    case "IN_PROGRESS":
      return ["RESOLVED"];
    case "RESOLVED":
      return ["IN_PROGRESS", "CLOSED"];
    case "CLOSED":
      return [];
  }
}

function dateLabel(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function filterTickets(
  tickets: Ticket[],
  queueFilter: QueueFilter,
  ticketSearch: string,
  priorityFilter: TicketPriority | "ALL",
): Ticket[] {
  const search = ticketSearch.trim().toLocaleLowerCase();
  return tickets.filter((ticket) => {
    const matchesQueue =
      queueFilter === "ALL" ||
      (queueFilter === "OPEN" && ["NEW", "TRIAGED", "ASSIGNED"].includes(ticket.status)) ||
      (queueFilter === "IN_PROGRESS" && ticket.status === "IN_PROGRESS") ||
      (queueFilter === "RESOLVED" && ["RESOLVED", "CLOSED"].includes(ticket.status));
    const matchesPriority = priorityFilter === "ALL" || ticket.priority === priorityFilter;
    const matchesSearch =
      !search ||
      [ticket.title, ticket.id, ticket.createdBy, ticket.assignee ?? "", ticket.description]
        .some((value) => value.toLocaleLowerCase().includes(search));
    return matchesQueue && matchesPriority && matchesSearch;
  });
}

export default function Home() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [queueFilter, setQueueFilter] = useState<QueueFilter>("ALL");
  const [ticketSearch, setTicketSearch] = useState("");
  const [priorityFilter, setPriorityFilter] = useState<TicketPriority | "ALL">("ALL");
  const isLocalAuth = AUTH_MODE === "local";
  const isAuth0Auth = AUTH_MODE === "auth0";

  const isSupport = session?.role === "SUPPORT_AGENT";
  const ticketCounts = useMemo(() => ({
    open: tickets.filter((ticket) => ["NEW", "TRIAGED", "ASSIGNED"].includes(ticket.status)).length,
    inProgress: tickets.filter((ticket) => ticket.status === "IN_PROGRESS").length,
    resolved: tickets.filter((ticket) => ["RESOLVED", "CLOSED"].includes(ticket.status)).length,
  }), [tickets]);
  const visibleTickets = useMemo(
    () => filterTickets(tickets, queueFilter, ticketSearch, priorityFilter),
    [priorityFilter, queueFilter, ticketSearch, tickets],
  );
  const selectedTicket = visibleTickets.find((ticket) => ticket.id === selectedId) ?? null;

  const handleFailure = useCallback(
    (reason: unknown) => {
      if (reason instanceof ApiError && reason.status === 401) {
        setSession(null);
        setTickets([]);
        setSelectedId(null);
        setActivities([]);
        setError("Your session expired. Please sign in again.");
        return;
      }
      setError(reason instanceof Error ? reason.message : "Something went wrong.");
    },
    [],
  );

  const refreshTickets = useCallback(
    async (token: string, preferId?: string) => {
      const result = await request<Ticket[]>("/api/v1/tickets", token);
      setTickets(result);
      setSelectedId((current) => {
        const next = preferId ?? current;
        return next && result.some((ticket) => ticket.id === next)
          ? next
          : (result[0]?.id ?? null);
      });
      return result;
    },
    [],
  );

  const loadHistory = useCallback(
    async (ticketId: string, token: string) => {
      const result = await request<Activity[]>(`/api/v1/tickets/${ticketId}/history`, token);
      setActivities(result);
    },
    [],
  );

  useEffect(() => {
    if (!isAuth0Auth) return;

    let active = true;
    void (async () => {
      setBusy(true);
      try {
        const token = await completeAuth0SignIn();
        if (!token || !active) return;

        const profile = await request<AuthProfile>("/api/v1/auth/me", token);
        if (!active) return;
        const auth: AuthSession = { ...profile, token };
        setSession(auth);
        await refreshTickets(token);
      } catch (reason) {
        if (active) handleFailure(reason);
      } finally {
        if (active) setBusy(false);
      }
    })();

    return () => {
      active = false;
    };
  }, [handleFailure, isAuth0Auth, refreshTickets]);

  async function submitLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const data = new FormData(event.currentTarget);
    try {
      const auth = await request<AuthSession>("/api/v1/auth/login", undefined, {
        method: "POST",
        body: JSON.stringify({
          username: data.get("username"),
          password: data.get("password"),
        }),
      });
      setSession(auth);
      await refreshTickets(auth.token);
    } catch (reason) {
      if (reason instanceof ApiError && reason.status === 401) {
        setError("Invalid username or password.");
      } else {
        handleFailure(reason);
      }
    } finally {
      setBusy(false);
    }
  }

  async function submitTicket(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) return;
    const form = event.currentTarget;
    setBusy(true);
    setError("");
    const data = new FormData(form);
    try {
      const ticket = await request<Ticket>("/api/v1/tickets", session.token, {
        method: "POST",
        body: JSON.stringify({
          title: data.get("title"),
          description: data.get("description"),
          priority: data.get("priority"),
        }),
      });
      form.reset();
      await refreshTickets(session.token, ticket.id);
      await loadHistory(ticket.id, session.token);
    } catch (reason) {
      handleFailure(reason);
    } finally {
      setBusy(false);
    }
  }

  async function submitComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !selectedTicket) return;
    const form = event.currentTarget;
    setBusy(true);
    setError("");
    const data = new FormData(form);
    try {
      const updated = await request<Ticket>(`/api/v1/tickets/${selectedTicket.id}/comments`, session.token, {
        method: "POST",
        body: JSON.stringify({ comment: data.get("comment") }),
      });
      setTickets((current) => current.map((ticket) => ticket.id === updated.id ? updated : ticket));
      form.reset();
      await loadHistory(updated.id, session.token);
    } catch (reason) {
      handleFailure(reason);
    } finally {
      setBusy(false);
    }
  }

  async function changeStatus(status: TicketStatus) {
    if (!session || !selectedTicket) return;
    setBusy(true);
    setError("");
    try {
      const updated = await request<Ticket>(`/api/v1/tickets/${selectedTicket.id}/status`, session.token, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      });
      setTickets((current) => current.map((ticket) => ticket.id === updated.id ? updated : ticket));
      await loadHistory(updated.id, session.token);
    } catch (reason) {
      handleFailure(reason);
    } finally {
      setBusy(false);
    }
  }

  async function assignTicket(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session || !selectedTicket) return;
    const form = event.currentTarget;
    setBusy(true);
    setError("");
    const data = new FormData(form);
    try {
      const updated = await request<Ticket>(`/api/v1/tickets/${selectedTicket.id}/assign`, session.token, {
        method: "POST",
        body: JSON.stringify({ assignee: data.get("assignee") }),
      });
      setTickets((current) => current.map((ticket) => ticket.id === updated.id ? updated : ticket));
      await loadHistory(updated.id, session.token);
    } catch (reason) {
      handleFailure(reason);
    } finally {
      setBusy(false);
    }
  }

  async function selectTicket(ticket: Ticket) {
    setSelectedId(ticket.id);
    setError("");
    if (!session) return;
    try {
      await loadHistory(ticket.id, session.token);
    } catch (reason) {
      handleFailure(reason);
    }
  }

  function signOut() {
    setSession(null);
    setTickets([]);
    setSelectedId(null);
    setActivities([]);
    setError("");
    if (isAuth0Auth) {
      try {
        redirectToAuth0SignOut();
      } catch (reason) {
        setError(reason instanceof Error ? reason.message : "Could not sign out.");
      }
    }
  }

  function changeQueueFilter(filter: QueueFilter) {
    setQueueFilter(filter);
    const firstTicket = filterTickets(tickets, filter, ticketSearch, priorityFilter)[0];
    setSelectedId(firstTicket?.id ?? null);
    setActivities([]);
    if (firstTicket && session) {
      void loadHistory(firstTicket.id, session.token).catch(handleFailure);
    }
  }

  if (!session) {
    return (
      <main className="login-shell">
        <div className="login-layout">
          <section className="login-intro">
            <div className="brand-mark"><SolvixMark /></div>
            <p className="eyebrow">PORTFOLIO DEMO · SUPPORT OPERATIONS</p>
            <h1>A calmer way to move support work forward.</h1>
            <p className="login-lede">
              Solvix is a focused ticket workspace that connects customer requests,
              support workflows, and an auditable activity trail.
            </p>
            <div className="feature-list">
              <span><strong>01</strong> Create and prioritize requests</span>
              <span><strong>02</strong> Collaborate through comments</span>
              <span><strong>03</strong> Progress work with governed statuses</span>
            </div>
            <p className="stack-note">Next.js · Spring Boot · PostgreSQL · Docker</p>
          </section>

          <section className="login-card">
            <p className="eyebrow">LIVE RECRUITER SANDBOX</p>
            <h2>Explore the workspace</h2>
            <p className="muted">Use the synthetic demo account to try the complete flow.</p>
            {isLocalAuth ? (
              <form className="form-stack" onSubmit={submitLogin}>
                <label>
                  Username
                  <input name="username" autoComplete="username" defaultValue="solvix-demo-user" required />
                </label>
                <label>
                  Password
                  <input name="password" type="password" autoComplete="current-password" defaultValue="SolvixDemo-2026!Ticket" required />
                </label>
                {error && <p className="error-message" role="alert">{error}</p>}
                <button className="primary-button" disabled={busy}>
                  {busy ? "Signing in..." : "Enter the demo"}
                </button>
              </form>
            ) : isAuth0Auth ? (
              <div className="form-stack">
                {error && <p className="error-message" role="alert">{error}</p>}
                <button
                  className="primary-button"
                  disabled={busy}
                  onClick={() => {
                    setBusy(true);
                    setError("");
                    void beginAuth0SignIn().catch((reason: unknown) => {
                      setBusy(false);
                      setError(reason instanceof Error ? reason.message : "Could not start sign-in.");
                    });
                  }}
                >
                  {busy ? "Checking sign-in..." : "Continue with Auth0"}
                </button>
              </div>
            ) : (
              <p className="error-message" role="alert">Unsupported authentication mode: {AUTH_MODE}.</p>
            )}
            <div className="demo-note">
              <strong>Synthetic data only</strong>
              <span>Free-tier services may take a few seconds to wake up.</span>
            </div>
            <p className="footnote">Your access token stays in memory and ends when you sign out or refresh.</p>
          </section>
        </div>
      </main>
    );
  }

  return (
    <main className="workspace">
      <header className="topbar">
        <div className="brand-lockup">
          <div className="brand-mark small"><SolvixMark /></div>
          <span>solvix</span>
          <span className="workspace-tag">TICKETS</span>
        </div>
        <div className="account">
          <span className="role-pill">{session.role.replace("_", " ")}</span>
          <span className="account-name">{session.username}</span>
          <button className="quiet-button" onClick={signOut}>Sign out</button>
        </div>
      </header>

      <div className="page-heading">
        <div>
          <p className="eyebrow">SUPPORT OPERATIONS</p>
          <h1>{isSupport ? "Support inbox" : "Your requests"}</h1>
          <p className="muted">
            {isSupport ? "Review incoming requests and keep every issue moving." : "Track your issues and follow the conversation."}
          </p>
        </div>
        <div className="queue-health"><span className="health-indicator" />Workspace active</div>
      </div>

      {error && <p className="error-banner" role="alert">{error}</p>}

      <section className="queue-overview" aria-label="Ticket overview">
        <button className={`overview-card ${queueFilter === "ALL" ? "overview-selected" : ""}`} onClick={() => changeQueueFilter("ALL")} aria-pressed={queueFilter === "ALL"}>
          <span className="overview-label">All requests</span>
          <strong>{tickets.length}</strong>
          <span className="overview-caption">Across your workspace</span>
        </button>
        <button className={`overview-card ${queueFilter === "OPEN" ? "overview-selected" : ""}`} onClick={() => changeQueueFilter("OPEN")} aria-pressed={queueFilter === "OPEN"}>
          <span className="overview-label">Open</span>
          <strong>{ticketCounts.open}</strong>
          <span className="overview-caption">New and awaiting action</span>
        </button>
        <button className={`overview-card ${queueFilter === "IN_PROGRESS" ? "overview-selected" : ""}`} onClick={() => changeQueueFilter("IN_PROGRESS")} aria-pressed={queueFilter === "IN_PROGRESS"}>
          <span className="overview-label">In progress</span>
          <strong>{ticketCounts.inProgress}</strong>
          <span className="overview-caption">Currently being worked</span>
        </button>
        <button className={`overview-card ${queueFilter === "RESOLVED" ? "overview-selected" : ""}`} onClick={() => changeQueueFilter("RESOLVED")} aria-pressed={queueFilter === "RESOLVED"}>
          <span className="overview-label">Resolved</span>
          <strong>{ticketCounts.resolved}</strong>
          <span className="overview-caption">Completed requests</span>
        </button>
      </section>

      <div className="workspace-grid">
        <aside className="left-column">
          <section className="panel create-panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">NEW REQUEST</p>
                <h2>Create a ticket</h2>
              </div>
            </div>
            <form className="form-stack" onSubmit={submitTicket}>
              <label>
                Title
                <input name="title" maxLength={255} placeholder="What needs attention?" required />
              </label>
              <label>
                Description
                <textarea name="description" maxLength={4000} rows={4} placeholder="Describe the issue and what you expected..." required />
              </label>
              <label>
                Priority
                <select name="priority" defaultValue="MEDIUM">
                  {PRIORITIES.map((priority) => <option key={priority} value={priority}>{priority}</option>)}
                </select>
              </label>
              <button className="primary-button" disabled={busy}>Create ticket</button>
            </form>
          </section>

          <section className="panel ticket-list-panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">{isSupport ? "QUEUE" : "REQUESTS"}</p>
                <h2>Ticket list <span className="list-count">{visibleTickets.length}</span></h2>
              </div>
              <button className="icon-button" aria-label="Refresh tickets" onClick={() => refreshTickets(session.token)}>↻</button>
            </div>
            <div className="queue-controls">
              <label className="search-field">
                <span className="sr-only">Search tickets</span>
                <span aria-hidden="true">⌕</span>
                <input
                  type="search"
                  value={ticketSearch}
                  onChange={(event) => setTicketSearch(event.target.value)}
                  placeholder="Search requests"
                />
              </label>
              <label className="priority-filter">
                <span className="sr-only">Filter by priority</span>
                <select value={priorityFilter} onChange={(event) => setPriorityFilter(event.target.value as TicketPriority | "ALL")}>
                  <option value="ALL">Any priority</option>
                  {PRIORITIES.map((priority) => <option key={priority} value={priority}>{priority}</option>)}
                </select>
              </label>
            </div>
            {tickets.length === 0 ? (
              <p className="empty-state">No tickets yet. Create one to get started.</p>
            ) : visibleTickets.length === 0 ? (
              <p className="empty-state">No requests match these filters. Try another search or priority.</p>
            ) : (
              <div className="ticket-list">
                {visibleTickets.map((ticket) => (
                  <button
                    className={`ticket-row ${selectedId === ticket.id ? "selected" : ""}`}
                    key={ticket.id}
                    onClick={() => selectTicket(ticket)}
                  >
                    <span className="ticket-row-top"><span className={`status-dot status-${ticket.status.toLowerCase()}`} />{ticket.status.replace("_", " ")}<span className={`priority priority-${ticket.priority.toLowerCase()}`}>{ticket.priority}</span></span>
                    <strong>{ticket.title}</strong>
                    <span className="ticket-row-bottom">{ticket.id.slice(0, 8)} · {dateLabel(ticket.updatedAt)}</span>
                  </button>
                ))}
              </div>
            )}
          </section>
        </aside>

        <section className="panel detail-panel">
          {!selectedTicket ? (
            <div className="detail-empty">
              <div className="empty-icon">◎</div>
              <h2>{tickets.length > 0 ? "No request selected" : "Select a ticket"}</h2>
              <p className="muted">
                {tickets.length > 0
                  ? "Choose a matching request from the queue to review its details."
                  : "Ticket details, conversation, and activity will appear here."}
              </p>
            </div>
          ) : (
            <>
              <div className="detail-header">
                <div>
                  <p className="eyebrow">TICKET {selectedTicket.id}</p>
                  <h2>{selectedTicket.title}</h2>
                  <p className="muted">Opened by {selectedTicket.createdBy} · {dateLabel(selectedTicket.createdAt)}</p>
                </div>
                <span className={`status-label status-${selectedTicket.status.toLowerCase()}`}>{selectedTicket.status.replace("_", " ")}</span>
              </div>

              <div className="ticket-summary">
                <p>{selectedTicket.description}</p>
                <div className="summary-meta">
                  <span><small>PRIORITY</small><strong className={`priority-text priority-${selectedTicket.priority.toLowerCase()}`}>{selectedTicket.priority}</strong></span>
                  <span><small>ASSIGNEE</small><strong>{selectedTicket.assignee ?? "Unassigned"}</strong></span>
                  <span><small>UPDATED</small><strong>{dateLabel(selectedTicket.updatedAt)}</strong></span>
                </div>
              </div>

              {isSupport && (
                <div className="support-actions">
                  <div>
                    <p className="eyebrow">WORKFLOW</p>
                    <h3>Support actions</h3>
                  </div>
                  <div className="action-controls">
                    {selectedTicket.status === "TRIAGED" || selectedTicket.status === "ASSIGNED" || selectedTicket.status === "IN_PROGRESS" ? (
                      <form className="inline-form" onSubmit={assignTicket}>
                        <input name="assignee" defaultValue={selectedTicket.assignee ?? ""} placeholder="Support username" required />
                        <button className="secondary-button" disabled={busy}>Assign</button>
                      </form>
                    ) : null}
                    <label className="status-control">
                      <span className="sr-only">Move ticket to status</span>
                      <select
                        value=""
                        disabled={busy || nextStatuses(selectedTicket.status).length === 0}
                        onChange={(event) => {
                          if (event.target.value) void changeStatus(event.target.value as TicketStatus);
                        }}
                      >
                        <option value="" disabled>Move status...</option>
                        {nextStatuses(selectedTicket.status).map((status) => <option key={status} value={status}>{status.replace("_", " ")}</option>)}
                      </select>
                    </label>
                  </div>
                </div>
              )}

              <div className="content-columns">
                <section className="conversation">
                  <div className="section-heading"><h3>Conversation</h3><span>{selectedTicket.comments.length}</span></div>
                  {selectedTicket.comments.length === 0 ? (
                    <p className="empty-state">No comments yet.</p>
                  ) : (
                    <div className="comment-list">
                      {selectedTicket.comments.map((comment) => (
                        <article className="comment-card" key={comment.id}>
                          <div className="comment-meta"><strong>{comment.author}</strong><time>{dateLabel(comment.createdAt)}</time></div>
                          <p>{comment.text}</p>
                        </article>
                      ))}
                    </div>
                  )}
                  <form className="comment-form" onSubmit={submitComment}>
                    <label className="sr-only" htmlFor="comment">Add a comment</label>
                    <textarea id="comment" name="comment" maxLength={4000} rows={3} placeholder="Write a comment..." required />
                    <button className="secondary-button" disabled={busy}>Add comment</button>
                  </form>
                </section>

                <section className="activity">
                  <div className="section-heading"><h3>Activity</h3><span>{activities.length}</span></div>
                  {activities.length === 0 ? (
                    <p className="empty-state">No activity recorded.</p>
                  ) : (
                    <ol className="activity-list">
                      {activities.map((activity) => (
                        <li key={activity.id}>
                          <span className="activity-marker" />
                          <div><strong>{activity.description}</strong><p>{activity.actor} · {dateLabel(activity.createdAt)}</p></div>
                        </li>
                      ))}
                    </ol>
                  )}
                </section>
              </div>
            </>
          )}
        </section>
      </div>
    </main>
  );
}
