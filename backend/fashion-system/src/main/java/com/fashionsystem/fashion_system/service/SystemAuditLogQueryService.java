package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.SystemAuditLogDto;
import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
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
public class SystemAuditLogQueryService {
    private final AuditLogRepository auditLogs;

    @Transactional(readOnly = true)
    public Page<SystemAuditLogDto> search(UUID actorUserId, String action, String username,
            String table, String rowId, String requestId, LocalDateTime fromAt,
            LocalDateTime toAt, Pageable pageable) {
        return auditLogs.searchRequests(actorUserId, clean(action), clean(username), clean(table),
                clean(rowId), clean(requestId), fromAt, toAt,
                PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 200)))
                .map(this::toDto);
    }

    private SystemAuditLogDto toDto(AuditLog a) {
        boolean migrated = a.getNewData() != null
                && a.getNewData().path("migratedFromAuthAudit").asBoolean(false);
        return new SystemAuditLogDto(a.getId(), a.getEventId(), a.getAction(), a.getActorType(),
                a.getActorUserId(), a.getUsername(), a.getRequestId(), a.getMethod(), a.getPath(),
                a.getJobName(), a.getRowCount(), a.getChanges(), a.getIpAddress(), a.getUserAgent(),
                a.getOccurredAt(), a.getCreatedAt(), migrated,
                migrated ? a.getNewData().path("description").asText("") : null);
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
