package io.github.caioeduardopereirafelix.financeapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String bucket, int limit, Duration window) {
    }

    private final RateLimiter limiter = new RateLimiter();
    private final boolean enabled;
    private final boolean trustForwardedFor;
    private final Map<String, Rule> rules;

    public RateLimitFilter(
            @Value("${api.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${api.security.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor,
            @Value("${api.security.rate-limit.register-per-hour:10}") int registerPerHour,
            @Value("${api.security.rate-limit.forgot-per-hour:10}") int forgotPerHour,
            @Value("${api.security.rate-limit.reset-per-hour:20}") int resetPerHour,
            @Value("${api.security.rate-limit.verify-per-hour:30}") int verifyPerHour,
            @Value("${api.security.rate-limit.login-per-minute:30}") int loginPerMinute) {
        this.enabled = enabled;
        this.trustForwardedFor = trustForwardedFor;
        this.rules = Map.of(
                "/v1/auth/register", new Rule("register", registerPerHour, Duration.ofHours(1)),
                "/v1/auth/forgot-password", new Rule("forgot", forgotPerHour, Duration.ofHours(1)),
                "/v1/auth/reset-password", new Rule("reset", resetPerHour, Duration.ofHours(1)),
                "/v1/auth/verify-email", new Rule("verify", verifyPerHour, Duration.ofHours(1)),
                "/v1/auth/login", new Rule("login", loginPerMinute, Duration.ofMinutes(1)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || !rules.containsKey(path(request));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Rule rule = rules.get(path(request));
        long retryAfter = limiter.tryAcquire(rule.bucket() + "|" + clientIp(request), rule.limit(), rule.window());

        if (retryAfter > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":429,\"error\":\"Muitas requisições. Tente de novo em "
                    + retryAfter + " segundos.\",\"fieldsError\":[]}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        return uri.length() > 1 && uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String first = forwarded.split(",")[0].trim();
                if (!first.isEmpty() && first.length() <= 64) {
                    return first;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
