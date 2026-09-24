package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.AuthAuditLogResponse;
import com.fashionsystem.fashion_system.service.AuthAuditLogQueryService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth-audit-logs")
@RequiredArgsConstructor
public class AuthAuditLogController {
    private final AuthAuditLogQueryService service;

    @GetMapping
    @PreAuthorize("hasAuthority('LOG_VIEW')")
    public Page<AuthAuditLogResponse> search(
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toAt,
            @PageableDefault(size = 25) Pageable pageable) {
        return service.search(actorUserId, action, fromAt, toAt, pageable);
    }
}
