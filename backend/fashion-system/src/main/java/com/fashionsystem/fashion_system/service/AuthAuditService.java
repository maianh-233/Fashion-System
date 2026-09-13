package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.AuthAuditLog;
import com.fashionsystem.fashion_system.repository.AuthAuditLogRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ghi các sự kiện xác thực an toàn vào bảng audit hiện hữu. */
@Service
@RequiredArgsConstructor
public class AuthAuditService {
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String EMPLOYEE_CREATED = "EMPLOYEE_CREATED";
    public static final String EMPLOYEE_UPDATED = "EMPLOYEE_UPDATED";
    public static final String SUBORDINATE_ASSIGNED = "SUBORDINATE_ASSIGNED";
    public static final String SUBORDINATE_REMOVED = "SUBORDINATE_REMOVED";

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

    /** Ghi audit trong cùng giao dịch nghiệp vụ để dữ liệu và log cùng commit hoặc cùng rollback. */
    @Transactional
    public void recordTransactional(UUID userId, String action, String description) {
        save(userId, action, description);
    }

    private void save(UUID userId, String action, String description) {
        repository.save(AuthAuditLog.builder()
                .userId(userId)
                .action(action)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
