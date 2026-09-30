export type AuthSession = {
  token: string;
  username: string;
  role: string;
};

const PENDING_AUTH_KEY = "solvix.cognito.pending-auth";
const ROLES = [
  "CUSTOMER",
  "SUPPORT_AGENT",
  "DEVELOPER",
  "TEAM_LEAD",
  "MANAGER",
  "ADMINISTRATOR",
] as const;
const ROLE_SET = new Set<string>(ROLES);

type CognitoConfig = {
  domain: URL;
  clientId: string;
  redirectUri: string;
  logoutUri: string;
};

type PendingAuth = {
  state: string;
  verifier: string;
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

function getCognitoConfig(): CognitoConfig {
  const domainValue = requiredEnvironmentValue(
    "NEXT_PUBLIC_COGNITO_DOMAIN",
    process.env.NEXT_PUBLIC_COGNITO_DOMAIN,
  );
  let domain: URL;
  try {
    domain = new URL(domainValue);
  } catch {
    throw new Error("NEXT_PUBLIC_COGNITO_DOMAIN must be an absolute URL.");
  }
  const clientId = requiredEnvironmentValue(
    "NEXT_PUBLIC_COGNITO_CLIENT_ID",
    process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID,
  );
  const redirectUri = parseRedirectUri(
    "NEXT_PUBLIC_COGNITO_REDIRECT_URI",
    requiredEnvironmentValue(
      "NEXT_PUBLIC_COGNITO_REDIRECT_URI",
      process.env.NEXT_PUBLIC_COGNITO_REDIRECT_URI,
    ),
  );
  const logoutUri = parseRedirectUri(
    "NEXT_PUBLIC_COGNITO_LOGOUT_URI",
    requiredEnvironmentValue(
      "NEXT_PUBLIC_COGNITO_LOGOUT_URI",
      process.env.NEXT_PUBLIC_COGNITO_LOGOUT_URI,
    ),
  );

  if (domain.protocol !== "https:" || domain.pathname !== "/" || domain.search || domain.hash) {
    throw new Error("The Cognito hosted UI domain must use HTTPS.");
  }

  return { domain, clientId, redirectUri, logoutUri };
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}

function randomUrlSafeValue(): string {
  return base64Url(crypto.getRandomValues(new Uint8Array(32)));
}

export async function beginCognitoSignIn(): Promise<void> {
  const config = getCognitoConfig();
  const verifier = randomUrlSafeValue();
  const state = randomUrlSafeValue();
  const challenge = base64Url(
    new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier))),
  );
  const pendingAuth: PendingAuth = { state, verifier };
  sessionStorage.setItem(PENDING_AUTH_KEY, JSON.stringify(pendingAuth));

  const authorizeUrl = new URL("/oauth2/authorize", config.domain);
  authorizeUrl.searchParams.set("client_id", config.clientId);
  authorizeUrl.searchParams.set("response_type", "code");
  authorizeUrl.searchParams.set("scope", "openid email profile");
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
    typeof pending.verifier !== "string"
  ) {
    throw new Error("The sign-in request could not be verified. Please try again.");
  }

  return { state: pending.state, verifier: pending.verifier };
}

function decodeAccessToken(accessToken: string): Record<string, unknown> {
  const payload = accessToken.split(".")[1];
  if (!payload) throw new Error("Cognito returned an invalid access token.");

  try {
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const binary = atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "="));
    const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
    const claims: unknown = JSON.parse(new TextDecoder().decode(bytes));
    if (!isRecord(claims)) throw new Error("Cognito returned invalid access-token claims.");
    return claims;
  } catch (error) {
    if (error instanceof Error && error.message.startsWith("Cognito returned")) throw error;
    throw new Error("Cognito returned an invalid access token.");
  }
}

function stringClaim(claims: Record<string, unknown>, names: string[]): string | null {
  for (const name of names) {
    const value = claims[name];
    if (typeof value === "string" && value.length > 0) return value;
  }
  return null;
}

function getRole(claims: Record<string, unknown>): string {
  const groups = claims["cognito:groups"];
  if (Array.isArray(groups)) {
    const role = groups.find(
      (group): group is string => typeof group === "string" && ROLE_SET.has(group.toUpperCase()),
    );
    if (role) return role.toUpperCase();
  }
  return "CUSTOMER";
}

export async function completeCognitoSignIn(): Promise<AuthSession | null> {
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
    throw new Error(oauthErrorDescription ?? "Cognito sign-in failed.");
  }
  if (!code) {
    throw new Error("Cognito did not return an authorization code. Please try signing in again.");
  }

  const config = getCognitoConfig();
  const tokenUrl = new URL("/oauth2/token", config.domain);
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
    throw new Error("Could not reach Cognito to complete sign-in. Please try again.");
  }

  if (!response.ok) {
    throw new Error(`Cognito could not complete sign-in (${response.status}).`);
  }

  let tokens: unknown;
  try {
    tokens = await response.json();
  } catch {
    throw new Error("Cognito returned an invalid token response.");
  }
  if (
    !isRecord(tokens) ||
    !("access_token" in tokens) ||
    typeof tokens.access_token !== "string"
  ) {
    throw new Error("Cognito returned an invalid token response.");
  }

  const claims = decodeAccessToken(tokens.access_token);
  if (typeof claims.exp !== "number" || claims.exp * 1000 <= Date.now()) {
    throw new Error("Cognito returned an expired access token. Please sign in again.");
  }

  return {
    token: tokens.access_token,
    username: stringClaim(claims, ["preferred_username", "username", "email", "sub"]) ?? "Solvix user",
    role: getRole(claims),
  };
}

export function redirectToCognitoSignOut(): void {
  const config = getCognitoConfig();
  const logoutUrl = new URL("/logout", config.domain);
  logoutUrl.searchParams.set("client_id", config.clientId);
  logoutUrl.searchParams.set("logout_uri", config.logoutUri);
  window.location.assign(logoutUrl.toString());
}
