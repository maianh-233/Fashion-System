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
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String username,
            @PageableDefault(size = 25, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return service.search(category, action, username, pageable);
    }
}
