export type AuthSession = {
  token: string;
  username: string;
  role: string;
};

export type AuthProfile = Omit<AuthSession, "token">;

const PENDING_AUTH_KEY = "solvix.auth0.pending-auth";

type Auth0Config = {
  domain: URL;
  clientId: string;
  audience: string;
  redirectUri: string;
  logoutUri: string;
};

type PendingAuth = {
  state: string;
  verifier: string;
  createdAt: number;
};

function requiredEnvironmentValue(name: string, value: string | undefined): string {
  if (!value) {
    throw new Error(`Missing required frontend configuration: ${name}.`);
  }
  return value;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function parseRedirectUri(name: string, value: string): string {
  let uri: URL;
  try {
    uri = new URL(value);
  } catch {
    throw new Error(`${name} must be an absolute URL.`);
  }

  const isLocalHttp =
    uri.protocol === "http:" && ["localhost", "127.0.0.1", "[::1]"].includes(uri.hostname);
  if (uri.protocol !== "https:" && !isLocalHttp) {
    throw new Error(`${name} must use HTTPS outside localhost.`);
  }
  return value;
}

function getAuth0Config(): Auth0Config {
  const domainValue = requiredEnvironmentValue(
    "NEXT_PUBLIC_AUTH0_DOMAIN",
    process.env.NEXT_PUBLIC_AUTH0_DOMAIN,
  );
  let domain: URL;
  try {
    domain = new URL(domainValue);
  } catch {
    throw new Error("NEXT_PUBLIC_AUTH0_DOMAIN must be an absolute URL.");
  }
  const clientId = requiredEnvironmentValue(
    "NEXT_PUBLIC_AUTH0_CLIENT_ID",
    process.env.NEXT_PUBLIC_AUTH0_CLIENT_ID,
  );
  const audience = requiredEnvironmentValue(
    "NEXT_PUBLIC_AUTH0_API_AUDIENCE",
    process.env.NEXT_PUBLIC_AUTH0_API_AUDIENCE,
  );
  const redirectUri = parseRedirectUri(
    "NEXT_PUBLIC_AUTH0_REDIRECT_URI",
    requiredEnvironmentValue(
      "NEXT_PUBLIC_AUTH0_REDIRECT_URI",
      process.env.NEXT_PUBLIC_AUTH0_REDIRECT_URI,
    ),
  );
  const logoutUri = parseRedirectUri(
    "NEXT_PUBLIC_AUTH0_LOGOUT_URI",
    requiredEnvironmentValue(
      "NEXT_PUBLIC_AUTH0_LOGOUT_URI",
      process.env.NEXT_PUBLIC_AUTH0_LOGOUT_URI,
    ),
  );

  if (domain.protocol !== "https:" || domain.pathname !== "/" || domain.search || domain.hash) {
    throw new Error("The Auth0 tenant domain must be an HTTPS origin without a path.");
  }

  return { domain, clientId, audience, redirectUri, logoutUri };
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}

function randomUrlSafeValue(): string {
  return base64Url(crypto.getRandomValues(new Uint8Array(32)));
}

export async function beginAuth0SignIn(): Promise<void> {
  const config = getAuth0Config();
  const verifier = randomUrlSafeValue();
  const state = randomUrlSafeValue();
  const challenge = base64Url(
    new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier))),
  );
  const pendingAuth: PendingAuth = { state, verifier, createdAt: Date.now() };
  sessionStorage.setItem(PENDING_AUTH_KEY, JSON.stringify(pendingAuth));

  const authorizeUrl = new URL("/authorize", config.domain);
  authorizeUrl.searchParams.set("client_id", config.clientId);
  authorizeUrl.searchParams.set("audience", config.audience);
  authorizeUrl.searchParams.set("response_type", "code");
  authorizeUrl.searchParams.set("scope", "openid profile email");
  authorizeUrl.searchParams.set("redirect_uri", config.redirectUri);
  authorizeUrl.searchParams.set("state", state);
  authorizeUrl.searchParams.set("code_challenge_method", "S256");
  authorizeUrl.searchParams.set("code_challenge", challenge);
  window.location.assign(authorizeUrl.toString());
}

function removeAuthorizationParameters(url: URL): void {
  for (const key of ["code", "state", "error", "error_description", "scope", "session_state"]) {
    url.searchParams.delete(key);
  }
  window.history.replaceState(window.history.state, "", `${url.pathname}${url.search}${url.hash}`);
}

function parsePendingAuth(value: string | null): PendingAuth {
  if (!value) throw new Error("The sign-in request expired. Please try again.");

  let pending: unknown;
  try {
    pending = JSON.parse(value);
  } catch {
    throw new Error("The sign-in request could not be verified. Please try again.");
  }

  if (
    typeof pending !== "object" ||
    pending === null ||
    !("state" in pending) ||
    typeof pending.state !== "string" ||
    !("verifier" in pending) ||
    typeof pending.verifier !== "string" ||
    !("createdAt" in pending) ||
    typeof pending.createdAt !== "number" ||
    Date.now() - pending.createdAt > 10 * 60 * 1000 ||
    Date.now() < pending.createdAt
  ) {
    throw new Error("The sign-in request could not be verified. Please try again.");
  }

  return { state: pending.state, verifier: pending.verifier, createdAt: pending.createdAt };
}

export async function completeAuth0SignIn(): Promise<string | null> {
  const callbackUrl = new URL(window.location.href);
  const code = callbackUrl.searchParams.get("code");
  const oauthError = callbackUrl.searchParams.get("error");
  const oauthErrorDescription = callbackUrl.searchParams.get("error_description");
  const state = callbackUrl.searchParams.get("state");
  if (!code && !oauthError) return null;

  const pendingValue = sessionStorage.getItem(PENDING_AUTH_KEY);
  sessionStorage.removeItem(PENDING_AUTH_KEY);
  removeAuthorizationParameters(callbackUrl);

  const pending = parsePendingAuth(pendingValue);
  if (!state || state !== pending.state) {
    throw new Error("The sign-in response could not be verified. Please try again.");
  }
  if (oauthError) {
    throw new Error(oauthErrorDescription ?? "Auth0 sign-in failed.");
  }
  if (!code) {
    throw new Error("Auth0 did not return an authorization code. Please try signing in again.");
  }

  const config = getAuth0Config();
  const tokenUrl = new URL("/oauth/token", config.domain);
  let response: Response;
  try {
    response = await fetch(tokenUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        client_id: config.clientId,
        code,
        redirect_uri: config.redirectUri,
        code_verifier: pending.verifier,
      }),
    });
  } catch {
    throw new Error("Could not reach Auth0 to complete sign-in. Please try again.");
  }

  if (!response.ok) {
    throw new Error(`Auth0 could not complete sign-in (${response.status}).`);
  }

  let tokens: unknown;
  try {
    tokens = await response.json();
  } catch {
    throw new Error("Auth0 returned an invalid token response.");
  }
  if (
    !isRecord(tokens) ||
    !("access_token" in tokens) ||
    typeof tokens.access_token !== "string" ||
    tokens.access_token.length === 0 ||
    !("token_type" in tokens) ||
    typeof tokens.token_type !== "string" ||
    tokens.token_type.toLowerCase() !== "bearer"
  ) {
    throw new Error("Auth0 returned an invalid token response.");
  }

  return tokens.access_token;
}

export function redirectToAuth0SignOut(): void {
  const config = getAuth0Config();
  const logoutUrl = new URL("/v2/logout", config.domain);
  logoutUrl.searchParams.set("client_id", config.clientId);
  logoutUrl.searchParams.set("returnTo", config.logoutUri);
  window.location.assign(logoutUrl.toString());
}
