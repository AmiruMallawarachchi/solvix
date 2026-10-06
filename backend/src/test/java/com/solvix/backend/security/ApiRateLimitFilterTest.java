package com.solvix.backend.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRateLimitFilterTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-06T10:00:30Z"), ZoneOffset.UTC);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void limitsAuthenticatedRequestsPerUserAndReturnsRetryAfter() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(1, 1, CLOCK);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("pilot-user", null, List.of())
        );

        MockHttpServletResponse first = send(filter, "192.0.2.1");
        MockHttpServletResponse second = send(filter, "192.0.2.1");

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(429);
        assertThat(second.getHeader("Retry-After")).isEqualTo("30");
        assertThat(second.getContentAsString()).contains("\"RATE_LIMITED\"");
    }

    @Test
    void limitsUnauthenticatedRequestsPerRemoteAddress() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(10, 1, CLOCK);

        assertThat(send(filter, "192.0.2.2").getStatus()).isEqualTo(200);
        assertThat(send(filter, "192.0.2.2").getStatus()).isEqualTo(429);
        assertThat(send(filter, "192.0.2.3").getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotLimitCorsPreflightRequests() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter(1, 1, CLOCK);
        AtomicInteger chainCalls = new AtomicInteger();
        FilterChain chain = (request, response) -> chainCalls.incrementAndGet();

        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest request = request("192.0.2.4");
            request.setMethod("OPTIONS");
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        assertThat(chainCalls).hasValue(2);
    }

    private MockHttpServletResponse send(ApiRateLimitFilter filter, String remoteAddress) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(remoteAddress), response, (request, ignored) -> {});
        return response;
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
