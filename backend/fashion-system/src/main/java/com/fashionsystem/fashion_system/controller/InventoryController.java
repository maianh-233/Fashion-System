package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.InventoryAdjustmentRequest;
import com.fashionsystem.fashion_system.dto.InventoryBalanceDto;
import com.fashionsystem.fashion_system.dto.InventoryStatisticsDto;
import com.fashionsystem.fashion_system.dto.InventoryTransactionDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.InventoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Store-scoped Inventory balances, ledger, adjustments, and statistics API. */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class InventoryController {
    private final InventoryService inventoryService;

    /** Lists balances using a server-resolved Store filter and database pagination. */
    @GetMapping("/balances")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public Page<InventoryBalanceDto> getBalances(Authentication authentication,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID variantId,
            @RequestParam(required = false) Integer lowStockThreshold,
            @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable) {
        return inventoryService.getBalances(
                userId(authentication), storeId, variantId, lowStockThreshold, pageable);
    }

    /** Returns one balance only after checking its Store against the principal. */
    @GetMapping("/balances/{storeId}/{variantId}")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public InventoryBalanceDto getBalance(Authentication authentication,
            @PathVariable UUID storeId, @PathVariable UUID variantId) {
        return inventoryService.getBalance(userId(authentication), storeId, variantId);
    }

    /** Lists the append-only stock ledger within the effective Store scope. */
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public Page<InventoryTransactionDto> getTransactions(Authentication authentication,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID variantId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) UUID referenceId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return inventoryService.getTransactions(userId(authentication), storeId, variantId,
                transactionType, referenceType, referenceId, pageable);
    }

    /** Applies a validated stock delta; actor and resulting balance are never client-owned. */
    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('INVENTORY_ADJUST')")
    public InventoryBalanceDto adjust(Authentication authentication,
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        return inventoryService.adjust(userId(authentication), request.storeId(),
                request.productVariantId(), request.quantityDelta());
    }

    /** Returns Store-scoped or chain-wide database aggregates according to identity and RBAC. */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    public InventoryStatisticsDto getStatistics(Authentication authentication,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "5") int lowStockThreshold) {
        return inventoryService.getStatistics(userId(authentication), storeId, lowStockThreshold);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
