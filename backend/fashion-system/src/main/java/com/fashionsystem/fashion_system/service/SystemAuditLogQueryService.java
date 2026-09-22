package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.SystemAuditLogDto;
import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.entity.AuthAuditLog;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import com.fashionsystem.fashion_system.repository.AuthAuditLogRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemAuditLogQueryService {
    private final AuditLogRepository auditLogs;
    private final AuthAuditLogRepository authLogs;

    @Transactional(readOnly = true)
    public Page<SystemAuditLogDto> search(String category, String action, String username, Pageable pageable) {
        List<SystemAuditLogDto> rows = new ArrayList<>();
        if (category == null || category.isBlank() || "BUSINESS".equalsIgnoreCase(category)) {
            for (AuditLog a : auditLogs.findAllByOrderByCreatedAtDesc()) {
                if (matches(action, a.getAction()) && matches(username, a.getUsername())) {
                    rows.add(new SystemAuditLogDto(a.getId(), "BUSINESS", "INFO", a.getAction(),
                            a.getEntityType() + " " + a.getEntityId(), a.getActorUserId(), a.getUsername(),
                            a.getEntityType(), a.getEntityId(), a.getIpAddress(), a.getUserAgent(), a.getCreatedAt()));
                }
            }
        }
        if (category == null || category.isBlank() || "AUTH".equalsIgnoreCase(category)) {
            for (AuthAuditLog a : authLogs.findAllByOrderByCreatedAtDesc()) {
                if (matches(action, a.getAction())) {
                    rows.add(new SystemAuditLogDto(a.getId(), "AUTH", level(a.getAction()), a.getAction(),
                            a.getDescription(), a.getUserId(), null, null, null, a.getIpAddress(), a.getUserAgent(), a.getCreatedAt()));
                }
            }
        }
        rows.sort(Comparator.comparing(SystemAuditLogDto::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        int start = Math.min((int) pageable.getOffset(), rows.size());
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }
    private boolean matches(String filter, String value) { return filter == null || filter.isBlank() || (value != null && value.toLowerCase().contains(filter.toLowerCase())); }
    private String level(String action) { return action != null && (action.contains("FAILED") || action.contains("LOCKED")) ? "WARN" : "INFO"; }
}
