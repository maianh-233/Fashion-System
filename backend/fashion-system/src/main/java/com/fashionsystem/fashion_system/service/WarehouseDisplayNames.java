package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.GoodsIssueDto;
import com.fashionsystem.fashion_system.dto.GoodsReceiptDto;
import com.fashionsystem.fashion_system.dto.InventoryTransactionDto;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.entity.Supplier;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.repository.GoodsIssueRepository;
import com.fashionsystem.fashion_system.repository.GoodsReceiptRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.SupplierRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Điền tên hiển thị (nhân viên, nhà cung cấp, sản phẩm, mã phiếu) vào DTO kho để giao diện không phải hiện UUID.
 * Mỗi loại tên được tải theo lô bằng một truy vấn cho cả trang dữ liệu.
 */
@Component
@RequiredArgsConstructor
public class WarehouseDisplayNames {
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final GoodsIssueRepository issueRepository;

    public void fillReceipts(Collection<GoodsReceiptDto> rows) {
        if (rows.isEmpty()) return;
        Map<UUID, String> users = userNames(rows.stream()
                .flatMap(row -> Stream.of(row.getReceivedBy(), row.getApprovedBy(), row.getCompletedBy())));
        Map<UUID, String> suppliers = supplierNames(rows.stream().map(GoodsReceiptDto::getSupplierId));
        rows.forEach(row -> {
            row.setReceivedByName(users.get(row.getReceivedBy()));
            row.setApprovedByName(users.get(row.getApprovedBy()));
            row.setCompletedByName(users.get(row.getCompletedBy()));
            row.setSupplierName(suppliers.get(row.getSupplierId()));
        });
    }

    public void fillIssues(Collection<GoodsIssueDto> rows) {
        if (rows.isEmpty()) return;
        Map<UUID, String> users = userNames(rows.stream()
                .flatMap(row -> Stream.of(row.getIssuedBy(), row.getApprovedBy(), row.getCompletedBy())));
        Map<UUID, String> suppliers = supplierNames(rows.stream().map(GoodsIssueDto::getSupplierId));
        rows.forEach(row -> {
            row.setIssuedByName(users.get(row.getIssuedBy()));
            row.setApprovedByName(users.get(row.getApprovedBy()));
            row.setCompletedByName(users.get(row.getCompletedBy()));
            row.setSupplierName(suppliers.get(row.getSupplierId()));
        });
    }

    public void fillTransactions(Collection<InventoryTransactionDto> rows) {
        if (rows.isEmpty()) return;
        Map<UUID, String> users = userNames(rows.stream().map(InventoryTransactionDto::getCreatedBy));
        Map<UUID, ProductVariant> variants = byId(variantRepository.findAllById(ids(rows.stream()
                .map(InventoryTransactionDto::getProductVariantId))), ProductVariant::getId);
        Map<UUID, String> products = byId(productRepository.findAllById(ids(variants.values().stream()
                .map(ProductVariant::getProductId))), Product::getId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getName()));
        Map<UUID, String> receiptCodes = new HashMap<>();
        receiptRepository.findAllById(ids(rows.stream().map(InventoryTransactionDto::getImportReceiptId)))
                .forEach(receipt -> receiptCodes.put(receipt.getId(), receipt.getReceiptCode()));
        issueRepository.findAllById(ids(rows.stream().map(InventoryTransactionDto::getExportReceiptId)))
                .forEach(issue -> receiptCodes.put(issue.getId(), issue.getIssueCode()));
        rows.forEach(row -> {
            row.setCreatedByName(users.get(row.getCreatedBy()));
            ProductVariant variant = variants.get(row.getProductVariantId());
            if (variant != null) {
                row.setSku(variant.getSku());
                row.setProductName(products.get(variant.getProductId()));
            }
            row.setReferenceCode(receiptCodes.get(row.getImportReceiptId() != null
                    ? row.getImportReceiptId() : row.getExportReceiptId()));
        });
    }

    private Map<UUID, String> userNames(Stream<UUID> ids) {
        return byId(userRepository.findAllById(ids(ids)), User::getId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> displayName(entry.getValue())));
    }

    private Map<UUID, String> supplierNames(Stream<UUID> ids) {
        return byId(supplierRepository.findAllById(ids(ids)), Supplier::getId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getName()));
    }

    static String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName().trim();
        return user.getUsername();
    }

    private static Set<UUID> ids(Stream<UUID> ids) {
        return ids.filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static <T> Map<UUID, T> byId(List<T> rows, Function<T, UUID> id) {
        return rows.stream().collect(Collectors.toMap(id, Function.identity(), (left, right) -> left));
    }
}
