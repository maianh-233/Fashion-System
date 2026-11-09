package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.RefundAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Khoản hoàn tiền từ thanh toán online; phạm vi cửa hàng kiểm tra trong service. */
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser) and hasAuthority('REFUND_MANAGE')")
public class AdminRefundController {
    private final RefundAdminService refunds;

    public record CompleteRequest(@NotBlank @Size(max = 100) String transactionCode) {}

    public record RejectRequest(@NotBlank @Size(max = 500) String reason) {}

    @GetMapping
    public Page<RefundAdminService.RefundRow> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @RequestParam(required = false) String status,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return refunds.list(user.userId(), status, pageable);
    }

    @PostMapping("/{id}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                         @Valid @RequestBody CompleteRequest request) {
        refunds.complete(user.userId(), id, request.transactionCode());
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                       @Valid @RequestBody RejectRequest request) {
        refunds.reject(user.userId(), id, request.reason());
    }
}
