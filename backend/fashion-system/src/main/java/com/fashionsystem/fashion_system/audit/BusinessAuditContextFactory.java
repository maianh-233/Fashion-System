package com.fashionsystem.fashion_system.audit;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class BusinessAuditContextFactory {
    private static final ThreadLocal<String> JOB_NAME = new ThreadLocal<>();

    public BusinessAuditContext forCurrentRequest(String domain, String methodName) {
        String action = action(domain, methodName);
        String jobName = JOB_NAME.get();
        if (jobName != null) {
            return new BusinessAuditContext(AuditActorType.SYSTEM, null, "SYSTEM", action,
                    null, null, null, jobName, null, null, Instant.now());
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Business audit requires an authenticated user or explicit system job scope");
        }
        var attributes = RequestContextHolder.getRequestAttributes();
        var request = attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
        return new BusinessAuditContext(AuditActorType.EMPLOYEE, user.userId(), user.username(), action,
                request == null ? null : request.getHeader("X-Request-ID"),
                request == null ? null : request.getMethod(),
                request == null ? null : request.getRequestURI(), null,
                request == null ? null : request.getRemoteAddr(),
                request == null ? null : request.getHeader("User-Agent"), Instant.now());
    }

    public static JobScope systemJob(String jobName) {
        if (jobName == null || jobName.isBlank()) throw new IllegalArgumentException("jobName is required");
        String previous = JOB_NAME.get();
        JOB_NAME.set(jobName);
        return new JobScope(previous);
    }

    private static String action(String domain, String methodName) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(methodName, "methodName");
        return domain.toUpperCase(Locale.ROOT) + "_" + methodName
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);
    }

    public static final class JobScope implements AutoCloseable {
        private final String previous;
        private boolean closed;
        private JobScope(String previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return;
            if (previous == null) JOB_NAME.remove();
            else JOB_NAME.set(previous);
            closed = true;
        }
    }
}
