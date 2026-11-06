package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.StocktakeDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.StocktakeService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Kiểm kê kho, phiếu điều chỉnh và báo cáo hao hụt; phạm vi cửa hàng kiểm tra trong service. */
@RestController
@RequestMapping("/api/admin/stocktakes")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)"
        + " and hasAnyAuthority('STOCKTAKE_VIEW', 'STOCKTAKE_COUNT', 'STOCKTAKE_APPROVE')")
public class AdminStocktakeController {
    private static final String COUNT = "hasAuthority('STOCKTAKE_COUNT')";
    private static final String APPROVE = "hasAuthority('STOCKTAKE_APPROVE')";

    private final StocktakeService stocktakes;

    @GetMapping("/lookups")
    public StocktakeDtos.Lookups lookups(@AuthenticationPrincipal AuthenticatedUser user) {
        return stocktakes.lookups(user.userId());
    }

    @GetMapping
    public Page<StocktakeDtos.Stocktake> list(@AuthenticationPrincipal AuthenticatedUser user,
                                              @RequestParam(required = false) UUID storeId,
                                              @RequestParam(required = false) String status,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return stocktakes.list(user.userId(), storeId, status, pageable);
    }

    @GetMapping("/report")
    public StocktakeDtos.Report report(@AuthenticationPrincipal AuthenticatedUser user,
                                       @RequestParam(required = false) UUID storeId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stocktakes.report(user.userId(), storeId, from, to);
    }

    @GetMapping("/{id}")
    public StocktakeDtos.Stocktake detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return stocktakes.detail(user.userId(), id);
    }

    @GetMapping("/{id}/items")
    public Page<StocktakeDtos.Item> items(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                          @RequestParam(required = false) String filter,
                                          @RequestParam(required = false) String keyword,
                                          @PageableDefault(size = 50) Pageable pageable) {
        return stocktakes.items(user.userId(), id, filter, keyword, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(COUNT)
    public StocktakeDtos.Stocktake create(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody StocktakeDtos.CreateRequest request) {
        return stocktakes.create(user.userId(), request);
    }

    @PostMapping("/{id}/scan")
    @PreAuthorize(COUNT)
    public StocktakeDtos.ScanResult scan(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                         @Valid @RequestBody StocktakeDtos.ScanRequest request) {
        return stocktakes.scan(user.userId(), id, request);
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize(COUNT)
    public StocktakeDtos.Item setCount(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                       @PathVariable UUID itemId, @Valid @RequestBody StocktakeDtos.CountRequest request) {
        return stocktakes.setCount(user.userId(), id, itemId, request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize(COUNT)
    public StocktakeDtos.Stocktake submit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                          @RequestBody(required = false) StocktakeDtos.SubmitRequest request) {
        return stocktakes.submit(user.userId(), id, request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(APPROVE)
    public StocktakeDtos.Stocktake approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                           @Valid @RequestBody(required = false) StocktakeDtos.Decision decision) {
        return stocktakes.approve(user.userId(), id, decision);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(APPROVE)
    public StocktakeDtos.Stocktake reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                          @Valid @RequestBody StocktakeDtos.Rejection rejection) {
        return stocktakes.reject(user.userId(), id, rejection);
    }

    @PostMapping("/{id}/cancel")
    public StocktakeDtos.Stocktake cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return stocktakes.cancel(user.userId(), id);
    }
}
