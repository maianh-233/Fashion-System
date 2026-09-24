package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.AuthAuditLog;
import com.fashionsystem.fashion_system.repository.AuthAuditLogRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/** Ghi các sự kiện xác thực an toàn vào bảng audit hiện hữu. */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(reason = "Authentication history only")
@RequiredArgsConstructor
public class AuthAuditService {
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";

    private final AuthAuditLogRepository repository;

    /**
     * Ghi một sự kiện auth mà không chứa credential hoặc token.
     *
     * @param userId user nội bộ liên quan, có thể null khi chưa xác định được tài khoản
     * @param action loại sự kiện xác thực
     * @param description mô tả không nhạy cảm
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String action, String description) {
        save(userId, action, description);
    }

    private void save(UUID userId, String action, String description) {
        if (!LOGIN_SUCCESS.equals(action) && !LOGIN_FAILED.equals(action) && !LOGOUT.equals(action)) {
            throw new IllegalArgumentException("Unsupported authentication audit action: " + action);
        }
        repository.save(AuthAuditLog.builder()
                .userId(userId)
                .action(action)
                .description(description)
                .ipAddress(requestValue("X-Forwarded-For", "X-Real-IP"))
                .userAgent(requestValue("User-Agent"))
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String requestValue(String... names) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        HttpServletRequest request = attributes.getRequest();
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                return name.equals("X-Forwarded-For") ? value.split(",")[0].trim() : value.substring(0, Math.min(512, value.length()));
            }
        }
        return null;
    }
}
