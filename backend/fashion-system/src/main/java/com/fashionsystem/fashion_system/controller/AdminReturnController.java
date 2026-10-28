package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.order.ReturnDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ReturnRequestService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Nhân viên cửa hàng duyệt, nhận hàng và hoàn tất yêu cầu đổi / trả; phạm vi theo cửa hàng của nhân viên. */
@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminReturnController {
    private final ReturnRequestService returns;

    @GetMapping
    @PreAuthorize("hasAuthority('RETURN_VIEW')")
    public Page<ReturnDtos.ReturnRequestView> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @RequestParam(required = false) UUID storeId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return returns.list(user.userId(), storeId, status, keyword, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RETURN_VIEW')")
    public ReturnDtos.ReturnRequestView detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return returns.detail(user.userId(), id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('RETURN_MANAGE')")
    public ReturnDtos.ReturnRequestView approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return returns.approve(user.userId(), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('RETURN_MANAGE')")
    public ReturnDtos.ReturnRequestView reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                               @Valid @RequestBody ReturnDtos.RejectRequest request) {
        return returns.reject(user.userId(), id, request.reason());
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('RETURN_MANAGE')")
    public ReturnDtos.ReturnRequestView complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                 @Valid @RequestBody ReturnDtos.CompleteRequest request) {
        return returns.complete(user.userId(), id, request);
    }

    @PostMapping("/{id}/confirm-refund")
    @PreAuthorize("hasAuthority('RETURN_MANAGE')")
    public ReturnDtos.ReturnRequestView confirmRefund(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                                      @Valid @RequestBody ReturnDtos.ConfirmRefundRequest request) {
        return returns.confirmRefund(user.userId(), id, request.transactionCode());
    }
}
