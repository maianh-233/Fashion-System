package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.InventoryBalanceDto;
import com.fashionsystem.fashion_system.dto.InventoryTransactionDto;
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
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý số dư tồn kho và sổ giao dịch tồn kho append-only. */
@Service
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

    /** Lấy số dư tồn kho của một biến thể tại cửa hàng. */
    @Transactional(readOnly = true)
    public InventoryBalanceDto getBalance(UUID storeId, UUID variantId) {
        requireReferences(storeId, variantId);
        return balanceMapper.toDto(balanceRepository.findById(new InventoryBalanceId(storeId, variantId))
                .orElseGet(() -> emptyBalance(storeId, variantId)));
    }

    /** Lấy danh sách số dư tồn kho với bộ lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<InventoryBalanceDto> getBalances(
            UUID storeId, UUID variantId, Integer lowStockThreshold, Pageable pageable) {
        if (lowStockThreshold != null && lowStockThreshold < 0)
            throw BusinessException.badRequest("Ngưỡng tồn kho thấp không được âm");
        validateSort(pageable, BALANCE_SORT_FIELDS, "tồn kho");
        return balanceRepository.search(storeId, variantId, lowStockThreshold, pageable).map(balanceMapper::toDto);
    }

    /** Lấy lịch sử giao dịch tồn kho append-only theo bộ lọc. */
    @Transactional(readOnly = true)
    public Page<InventoryTransactionDto> getTransactions(
            UUID storeId, UUID variantId, String transactionType, String referenceType,
            UUID referenceId, Pageable pageable) {
        validateSort(pageable, TRANSACTION_SORT_FIELDS, "giao dịch tồn kho");
        return transactionRepository.search(
                        storeId, variantId, normalize(transactionType), normalize(referenceType), referenceId, pageable)
                .map(transactionMapper::toDto);
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
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
    private InventoryBalance emptyBalance(UUID storeId, UUID variantId) {
        return InventoryBalance.builder().storeId(storeId).productVariantId(variantId)
                .availableQuantity(0).reservedQuantity(0).damagedQuantity(0).build();
    }
}
