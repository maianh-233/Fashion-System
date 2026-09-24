package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.SystemAuditLogDto;
import com.fashionsystem.fashion_system.service.SystemAuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final SystemAuditLogQueryService service;

    @GetMapping
    @PreAuthorize("hasAuthority('LOG_VIEW')")
    public Page<SystemAuditLogDto> search(
            @RequestParam(required = false) java.util.UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String table,
            @RequestParam(required = false) String rowId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime fromAt,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime toAt,
            @PageableDefault(size = 25, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return service.search(actorUserId, action, username, table, rowId, requestId, fromAt, toAt, pageable);
    }
}
