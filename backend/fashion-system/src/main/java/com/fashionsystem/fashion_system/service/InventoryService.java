package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.InventoryBalanceDto;
import com.fashionsystem.fashion_system.dto.InventoryTransactionDto;
import com.fashionsystem.fashion_system.dto.InventoryStatisticsDto;
import com.fashionsystem.fashion_system.dto.StoreInventoryStatisticsDto;
import com.fashionsystem.fashion_system.entity.InventoryBalance;
import com.fashionsystem.fashion_system.entity.InventoryBalanceId;
import com.fashionsystem.fashion_system.entity.InventoryTransaction;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.InventoryBalanceMapper;
import com.fashionsystem.fashion_system.mapper.InventoryTransactionMapper;
import com.fashionsystem.fashion_system.repository.InventoryBalanceRepository;
import com.fashionsystem.fashion_system.repository.InventoryTransactionRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý số dư tồn kho và sổ giao dịch tồn kho append-only. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("INVENTORY")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService {
    private static final Set<String> BALANCE_SORT_FIELDS = Set.of(
            "storeId", "productVariantId", "availableQuantity", "reservedQuantity",
            "damagedQuantity", "updatedAt");
    private static final Set<String> TRANSACTION_SORT_FIELDS = Set.of(
            "id", "storeId", "productVariantId", "transactionType", "referenceType",
            "quantity", "balanceAfter", "createdAt");

    private final InventoryBalanceRepository balanceRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StoreRepository storeRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryBalanceMapper balanceMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final AuthorizationService authorizationService;
    private final UserScopeService userScopeService;

    /** Lấy số dư tồn kho của một biến thể tại cửa hàng. */
    @Transactional(readOnly = true)
    public InventoryBalanceDto getBalance(UUID actorId, UUID storeId, UUID variantId) {
        requirePermission(actorId, "INVENTORY_VIEW");
        userScopeService.requireStoreAccess(actorId, storeId);
        requireReferences(storeId, variantId);
        return balanceMapper.toDto(balanceRepository.findById(new InventoryBalanceId(storeId, variantId))
                .orElseGet(() -> emptyBalance(storeId, variantId)));
    }

    /** Lấy danh sách số dư tồn kho với bộ lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<InventoryBalanceDto> getBalances(
            UUID actorId, UUID requestedStoreId, UUID variantId,
            Integer lowStockThreshold, Pageable pageable) {
        requirePermission(actorId, "INVENTORY_VIEW");
        if (lowStockThreshold != null && lowStockThreshold < 0)
            throw BusinessException.badRequest("Ngưỡng tồn kho thấp không được âm");
        validatePageSize(pageable);
        validateSort(pageable, BALANCE_SORT_FIELDS, "tồn kho");
        UUID storeId = userScopeService.resolveStoreId(actorId, requestedStoreId);
        return balanceRepository.search(storeId, variantId, lowStockThreshold, pageable)
                .map(balanceMapper::toDto);
    }

    /** Lấy lịch sử giao dịch tồn kho append-only theo bộ lọc. */
    @Transactional(readOnly = true)
    public Page<InventoryTransactionDto> getTransactions(
            UUID actorId, UUID requestedStoreId, UUID variantId,
            String transactionType, String referenceType,
            UUID referenceId, Pageable pageable) {
        requirePermission(actorId, "INVENTORY_VIEW");
        validatePageSize(pageable);
        validateSort(pageable, TRANSACTION_SORT_FIELDS, "giao dịch tồn kho");
        UUID storeId = userScopeService.resolveStoreId(actorId, requestedStoreId);
        return transactionRepository.search(
                        storeId, variantId, normalize(transactionType), normalize(referenceType), referenceId, pageable)
                .map(transactionMapper::toDto);
    }

    /** Applies a manual delta using the authenticated actor and the locked database balance. */
    @Transactional
    public InventoryBalanceDto adjust(
            UUID actorId, UUID requestedStoreId, UUID variantId, int quantityDelta) {
        requirePermission(actorId, "INVENTORY_ADJUST");
        if (quantityDelta == 0) throw BusinessException.badRequest("Số lượng điều chỉnh phải khác 0");
        UUID storeId = userScopeService.resolveStoreId(actorId, requestedStoreId);
        if (storeId == null) throw BusinessException.badRequest("Phải chọn cửa hàng cần điều chỉnh tồn kho");
        InventoryBalance balance = lockBalance(storeId, variantId);
        if (quantityDelta < 0) ensureAvailable(balance, -quantityDelta);
        balance.setAvailableQuantity(balance.getAvailableQuantity() + quantityDelta);
        saveBalanceAndTransaction(
                balance, "ADJUST", "MANUAL_ADJUSTMENT", null, quantityDelta, actorId);
        return balanceMapper.toDto(balance);
    }

    /** Returns database aggregates limited to the authenticated employee's Store scope. */
    @Transactional(readOnly = true)
    public InventoryStatisticsDto getStatistics(
            UUID actorId, UUID requestedStoreId, int lowStockThreshold) {
        requirePermission(actorId, "INVENTORY_VIEW");
        if (lowStockThreshold < 0) {
            throw BusinessException.badRequest("Ngưỡng tồn kho thấp không được âm");
        }
        UUID storeId = userScopeService.resolveStoreId(actorId, requestedStoreId);
        UserScope scope = userScopeService.resolve(actorId);
        StoreInventoryStatisticsDto totals = balanceRepository.summarize(storeId, lowStockThreshold);
        List<StoreInventoryStatisticsDto> byStore = scope.isGlobal() && storeId == null
                ? balanceRepository.summarizeByStore(lowStockThreshold)
                : List.of(totals);
        return new InventoryStatisticsDto(scope.kind().name(), scope.storeId(), scope.storeName(), totals, byStore);
    }

    /** Ghi nhận hàng nhập đã duyệt vào tồn khả dụng. */
    @Transactional
    public InventoryBalanceDto receive(
            UUID storeId, UUID variantId, int quantity, UUID referenceId, UUID createdBy) {
        validatePositiveQuantity(quantity);
        InventoryBalance balance = lockBalance(storeId, variantId);
        balance.setAvailableQuantity(balance.getAvailableQuantity() + quantity);
        saveBalanceAndTransaction(balance, "RECEIVE", "GOODS_RECEIPT", referenceId, quantity, createdBy);
        return balanceMapper.toDto(balance);
    }

    /** Xuất hàng đã duyệt khỏi tồn khả dụng. */
    @Transactional
    public InventoryBalanceDto issue(
            UUID storeId, UUID variantId, int quantity, UUID referenceId, UUID createdBy) {
        validatePositiveQuantity(quantity);
        InventoryBalance balance = lockBalance(storeId, variantId);
        ensureAvailable(balance, quantity);
        balance.setAvailableQuantity(balance.getAvailableQuantity() - quantity);
        saveBalanceAndTransaction(balance, "ISSUE", "GOODS_ISSUE", referenceId, -quantity, createdBy);
        return balanceMapper.toDto(balance);
    }

    /** Chuyển số lượng từ tồn khả dụng sang tồn giữ chỗ. */
    @Transactional
    public InventoryBalanceDto reserve(
            UUID storeId, UUID variantId, int quantity, UUID referenceId, UUID createdBy) {
        validatePositiveQuantity(quantity);
        InventoryBalance balance = lockBalance(storeId, variantId);
        ensureAvailable(balance, quantity);
        balance.setAvailableQuantity(balance.getAvailableQuantity() - quantity);
        balance.setReservedQuantity(balance.getReservedQuantity() + quantity);
        saveBalanceAndTransaction(balance, "RESERVE", "STOCK_RESERVATION", referenceId, -quantity, createdBy);
        return balanceMapper.toDto(balance);
    }

    /** Hoàn số lượng giữ chỗ về tồn khả dụng. */
    @Transactional
    public InventoryBalanceDto release(
            UUID storeId, UUID variantId, int quantity, UUID referenceId, UUID createdBy) {
        validatePositiveQuantity(quantity);
        InventoryBalance balance = lockBalance(storeId, variantId);
        ensureReserved(balance, quantity);
        balance.setReservedQuantity(balance.getReservedQuantity() - quantity);
        balance.setAvailableQuantity(balance.getAvailableQuantity() + quantity);
        saveBalanceAndTransaction(balance, "RELEASE", "STOCK_RESERVATION", referenceId, quantity, createdBy);
        return balanceMapper.toDto(balance);
    }

    /** Tiêu thụ số lượng đã giữ chỗ khi đơn hàng được xuất kho. */
    @Transactional
    public InventoryBalanceDto consumeReservation(
            UUID storeId, UUID variantId, int quantity, UUID referenceId, UUID createdBy) {
        validatePositiveQuantity(quantity);
        InventoryBalance balance = lockBalance(storeId, variantId);
        ensureReserved(balance, quantity);
        balance.setReservedQuantity(balance.getReservedQuantity() - quantity);
        saveBalanceAndTransaction(balance, "RESERVATION_CONSUMED", "STOCK_RESERVATION", referenceId, -quantity, createdBy);
        return balanceMapper.toDto(balance);
    }

    private InventoryBalance lockBalance(UUID storeId, UUID variantId) {
        requireReferences(storeId, variantId);
        balanceRepository.initialize(storeId, variantId);
        return balanceRepository.findForUpdate(storeId, variantId)
                .orElseThrow(() -> BusinessException.invalidState("Không thể khởi tạo số dư tồn kho"));
    }

    private void saveBalanceAndTransaction(
            InventoryBalance balance, String type, String referenceType,
            UUID referenceId, int quantity, UUID createdBy) {
        balance.setUpdatedAt(LocalDateTime.now());
        balanceRepository.save(balance);
        transactionRepository.save(InventoryTransaction.builder()
                .storeId(balance.getStoreId())
                .productVariantId(balance.getProductVariantId())
                .transactionType(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .quantity(quantity)
                .balanceAfter(balance.getAvailableQuantity())
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void requireReferences(UUID storeId, UUID variantId) {
        if (!storeRepository.existsById(storeId)) throw BusinessException.notFound("Cửa hàng không tồn tại");
        if (!variantRepository.existsById(variantId)) throw BusinessException.notFound("Biến thể sản phẩm không tồn tại");
    }
    private void ensureAvailable(InventoryBalance balance, int quantity) {
        if (balance.getAvailableQuantity() < quantity) throw BusinessException.invalidState("Tồn kho khả dụng không đủ");
    }
    private void ensureReserved(InventoryBalance balance, int quantity) {
        if (balance.getReservedQuantity() < quantity) throw BusinessException.invalidState("Số lượng giữ chỗ không đủ");
    }
    private void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) throw BusinessException.badRequest("Số lượng phải lớn hơn 0");
    }
    private void validateSort(Pageable pageable, Set<String> fields, String subject) {
        if (pageable.getSort().stream().anyMatch(o -> !fields.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp " + subject + " không hợp lệ");
    }
    private void validatePageSize(Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw BusinessException.badRequest("Kích thước trang không được vượt quá 100");
        }
    }
    private void requirePermission(UUID actorId, String permissionCode) {
        if (!authorizationService.hasPermission(actorId, permissionCode)) {
            throw BusinessException.forbidden("Bạn không có quyền thực hiện thao tác này");
        }
    }
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
    private InventoryBalance emptyBalance(UUID storeId, UUID variantId) {
        return InventoryBalance.builder().storeId(storeId).productVariantId(variantId)
                .availableQuantity(0).reservedQuantity(0).damagedQuantity(0).build();
    }
}
