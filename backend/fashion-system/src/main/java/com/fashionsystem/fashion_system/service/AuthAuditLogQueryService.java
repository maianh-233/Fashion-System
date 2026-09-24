package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.AuthAuditLogResponse;
import com.fashionsystem.fashion_system.repository.AuthAuditLogRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthAuditLogQueryService {
    private final AuthAuditLogRepository authLogs;

    @Transactional(readOnly = true)
    public Page<AuthAuditLogResponse> search(UUID actorUserId, String action,
            LocalDateTime fromAt, LocalDateTime toAt, Pageable pageable) {
        return authLogs.searchHistory(actorUserId, action == null || action.isBlank() ? null : action.trim(),
                fromAt, toAt, PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 200)))
                .map(a -> new AuthAuditLogResponse(a.getId(), a.getUserId(), a.getAction(),
                        a.getDescription(), a.getIpAddress(), a.getUserAgent(), a.getCreatedAt()));
    }
}
