package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.security.AuthenticatedAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService queryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<NotificationResponse> history(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return queryService.history(account.realtimeIdentity(), page, size);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public java.util.Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedAccount account) {
        return java.util.Map.of("count", queryService.unreadCount(account.realtimeIdentity()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public NotificationResponse markRead(@AuthenticationPrincipal AuthenticatedAccount account,
                                         @PathVariable java.util.UUID id) {
        return queryService.markRead(account.realtimeIdentity(), id);
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public java.util.Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthenticatedAccount account) {
        return java.util.Map.of("updated", queryService.markAllRead(account.realtimeIdentity()));
    }
}
