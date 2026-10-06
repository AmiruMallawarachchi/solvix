package com.solvix.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_CLIENTS = 10_000;
    private static final long WINDOW_SECONDS = 60;
    private static final String RATE_LIMIT_BODY =
            "{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests. Try again later.\"}";

    private final int authenticatedLimit;
    private final int unauthenticatedLimit;
    private final Clock clock;
    private final Map<String, Window> windows = new HashMap<>();

    ApiRateLimitFilter(int authenticatedLimit, int unauthenticatedLimit) {
        this(authenticatedLimit, unauthenticatedLimit, Clock.systemUTC());
    }

    ApiRateLimitFilter(int authenticatedLimit, int unauthenticatedLimit, Clock clock) {
        if (authenticatedLimit < 1 || unauthenticatedLimit < 1) {
            throw new IllegalArgumentException("Rate limits must be positive.");
        }
        this.authenticatedLimit = authenticatedLimit;
        this.unauthenticatedLimit = unauthenticatedLimit;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Instant now = clock.instant();
        long minute = now.getEpochSecond() / WINDOW_SECONDS;
        Client client = client(request);
        int limit = client.authenticated() ? authenticatedLimit : unauthenticatedLimit;

        if (!allow(client.key(), minute, limit)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(WINDOW_SECONDS - now.getEpochSecond() % WINDOW_SECONDS));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(RATE_LIMIT_BODY);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Client client(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return new Client("user:" + authentication.getName(), true);
        }
        String remoteAddress = request.getRemoteAddr();
        return new Client("address:" + (remoteAddress == null ? "unknown" : remoteAddress), false);
    }

    private synchronized boolean allow(String key, long minute, int limit) {
        Window current = windows.get(key);
        if (current == null || current.minute() != minute) {
            pruneExpired(minute);
            if (current == null && windows.size() >= MAX_CLIENTS) {
                return false;
            }
            current = new Window(minute, 0);
        }
        if (current.count() >= limit) {
            return false;
        }
        windows.put(key, new Window(minute, current.count() + 1));
        return true;
    }

    private void pruneExpired(long minute) {
        Iterator<Window> iterator = windows.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().minute() < minute) {
                iterator.remove();
            }
        }
    }

    private record Client(String key, boolean authenticated) {}

    private record Window(long minute, int count) {}
}
