package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.StockTransferDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.StockTransferService;
import jakarta.validation.Valid;
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

/** Điều chuyển hàng giữa cửa hàng và danh sách hàng sắp hết; phạm vi theo cửa hàng của nhân viên. */
@RestController
@RequestMapping("/api/admin/transfers")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminStockTransferController {
    private final StockTransferService transfers;

    @GetMapping
    @PreAuthorize("hasAuthority('TRANSFER_VIEW')")
    public Page<StockTransferDtos.Transfer> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @RequestParam(required = false) UUID storeId,
                                                 @RequestParam(required = false) String direction,
                                                 @RequestParam(required = false) String status,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return transfers.list(user.userId(), storeId, direction, status, pageable);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('TRANSFER_VIEW')")
    public Page<StockTransferDtos.LowStock> lowStock(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @RequestParam(required = false) UUID storeId,
                                                     @RequestParam(required = false) String keyword,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return transfers.lowStock(user.userId(), storeId, keyword, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TRANSFER_VIEW')")
    public StockTransferDtos.Transfer detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return transfers.detail(user.userId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public StockTransferDtos.Transfer create(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @RequestBody StockTransferDtos.CreateRequest request) {
        return transfers.create(user.userId(), request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public StockTransferDtos.Transfer cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return transfers.cancel(user.userId(), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public StockTransferDtos.Transfer reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                             @Valid @RequestBody StockTransferDtos.RejectRequest request) {
        return transfers.reject(user.userId(), id, request.reason());
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public StockTransferDtos.Transfer ship(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                           @Valid @RequestBody(required = false) StockTransferDtos.ShipRequest request) {
        return transfers.ship(user.userId(), id, request == null ? null : request.quantities());
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('TRANSFER_MANAGE')")
    public StockTransferDtos.Transfer receive(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return transfers.receive(user.userId(), id);
    }
}
