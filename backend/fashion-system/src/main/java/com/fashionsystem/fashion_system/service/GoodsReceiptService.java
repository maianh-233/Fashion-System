package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.GoodsReceiptDto;
import com.fashionsystem.fashion_system.dto.GoodsReceiptItemDto;
import com.fashionsystem.fashion_system.entity.GoodsReceipt;
import com.fashionsystem.fashion_system.entity.GoodsReceiptItem;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.GoodsReceiptItemMapper;
import com.fashionsystem.fashion_system.mapper.GoodsReceiptMapper;
import com.fashionsystem.fashion_system.repository.GoodsReceiptItemRepository;
import com.fashionsystem.fashion_system.repository.GoodsReceiptRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.SupplierRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý phiếu nhập và ghi tăng tồn kho khi phiếu được duyệt. */
@Service
@RequiredArgsConstructor
public class GoodsReceiptService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "receiptCode", "supplierId", "storeId", "receiptDate", "status",
            "totalQuantity", "totalAmount", "createdAt", "updatedAt");
    private final GoodsReceiptRepository receiptRepository;
    private final GoodsReceiptItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final GoodsReceiptMapper receiptMapper;
    private final GoodsReceiptItemMapper itemMapper;
    private final InventoryService inventoryService;

    /** Tạo phiếu nhập ở trạng thái chờ duyệt. */
    @Transactional
    public GoodsReceiptDto create(GoodsReceiptDto request) {
        validateHeader(request);
        ensureCodeAvailable(request.getReceiptCode(), null);
        return receiptMapper.toDto(receiptRepository.save(receiptMapper.toEntity(request)));
    }

    /** Lấy chi tiết phiếu nhập. */
    @Transactional(readOnly = true)
    public GoodsReceiptDto getById(UUID id) { return receiptMapper.toDto(requireReceipt(id)); }

    /** Lấy danh sách phiếu nhập với tìm kiếm, lọc thời gian và phân trang. */
    @Transactional(readOnly = true)
    public Page<GoodsReceiptDto> getList(
            String keyword, UUID storeId, UUID supplierId, String status,
            LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        validatePeriod(fromDate, toDate);
        validateSort(pageable);
        return receiptRepository.search(trimToEmpty(keyword), storeId, supplierId, normalize(status),
                fromDate, toDate, pageable).map(receiptMapper::toDto);
    }

    /** Cập nhật phần thông tin chung của phiếu nhập đang chờ duyệt. */
    @Transactional
    public GoodsReceiptDto update(UUID id, GoodsReceiptDto request) {
        GoodsReceipt receipt = requirePendingReceiptForUpdate(id);
        validateHeader(request);
        ensureCodeAvailable(request.getReceiptCode(), id);
        receiptMapper.updateDraft(request, receipt);
        return receiptMapper.toDto(receiptRepository.save(receipt));
    }

    /** Xóa phiếu nhập đang chờ duyệt cùng các dòng hàng của phiếu. */
    @Transactional
    public void delete(UUID id) { receiptRepository.delete(requirePendingReceiptForUpdate(id)); }

    /** Thêm dòng hàng vào phiếu nhập đang chờ duyệt. */
    @Transactional
    public GoodsReceiptItemDto addItem(UUID receiptId, GoodsReceiptItemDto request) {
        requirePendingReceiptForUpdate(receiptId);
        ProductVariant variant = requireVariant(request.getProductVariantId());
        validateItem(request);
        GoodsReceiptItem item = itemMapper.toEntity(request);
        item.setId(null);
        item.setReceiptId(receiptId);
        applyItemValues(item, request, variant);
        item.setCreatedAt(LocalDateTime.now());
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(receiptId);
        return itemMapper.toDto(item);
    }

    /** Cập nhật dòng hàng thuộc phiếu nhập đang chờ duyệt. */
    @Transactional
    public GoodsReceiptItemDto updateItem(UUID receiptId, UUID itemId, GoodsReceiptItemDto request) {
        requirePendingReceiptForUpdate(receiptId);
        GoodsReceiptItem item = requireItem(receiptId, itemId);
        ProductVariant variant = requireVariant(request.getProductVariantId());
        validateItem(request);
        item.setProductVariantId(request.getProductVariantId());
        applyItemValues(item, request, variant);
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(receiptId);
        return itemMapper.toDto(item);
    }

    /** Xóa dòng hàng khỏi phiếu nhập đang chờ duyệt. */
    @Transactional
    public void removeItem(UUID receiptId, UUID itemId) {
        requirePendingReceiptForUpdate(receiptId);
        itemRepository.delete(requireItem(receiptId, itemId));
        itemRepository.flush();
        recalculate(receiptId);
    }

    /** Lấy các dòng hàng của phiếu nhập theo thứ tự tạo. */
    @Transactional(readOnly = true)
    public List<GoodsReceiptItemDto> getItems(UUID receiptId) {
        requireReceipt(receiptId);
        return itemRepository.findAllByReceiptIdOrderByCreatedAtAsc(receiptId).stream().map(itemMapper::toDto).toList();
    }

    /** Duyệt phiếu nhập và ghi tăng tồn kho trong cùng transaction. */
    @Transactional
    public GoodsReceiptDto approve(UUID receiptId, UUID approvedBy) {
        GoodsReceipt receipt = requirePendingReceiptForUpdate(receiptId);
        requireUser(approvedBy);
        List<GoodsReceiptItem> items = itemRepository.findAllByReceiptIdOrderByCreatedAtAsc(receiptId);
        if (items.isEmpty()) throw BusinessException.invalidState("Phiếu nhập phải có ít nhất một dòng hàng");
        for (GoodsReceiptItem item : items) {
            inventoryService.receive(receipt.getStoreId(), item.getProductVariantId(), item.getQuantity(), receiptId, approvedBy);
        }
        receipt.setStatus("APPROVED");
        receipt.setApprovedBy(approvedBy);
        receipt.setUpdatedAt(LocalDateTime.now());
        return receiptMapper.toDto(receiptRepository.save(receipt));
    }

    private GoodsReceipt requireReceipt(UUID id) {
        return receiptRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Phiếu nhập không tồn tại"));
    }
    private GoodsReceipt requirePendingReceiptForUpdate(UUID id) {
        GoodsReceipt receipt = receiptRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Phiếu nhập không tồn tại"));
        if (!"PENDING".equalsIgnoreCase(receipt.getStatus()))
            throw BusinessException.invalidState("Chỉ được thay đổi phiếu nhập đang chờ duyệt");
        return receipt;
    }
    private GoodsReceiptItem requireItem(UUID receiptId, UUID itemId) {
        return itemRepository.findByIdAndReceiptId(itemId, receiptId)
                .orElseThrow(() -> BusinessException.notFound("Dòng hàng phiếu nhập không tồn tại"));
    }
    private ProductVariant requireVariant(UUID id) {
        return variantRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Biến thể sản phẩm không tồn tại"));
    }
    private void applyItemValues(GoodsReceiptItem item, GoodsReceiptItemDto request, ProductVariant variant) {
        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm của biến thể không tồn tại"));
        item.setProductVariantId(variant.getId());
        item.setSku(variant.getSku());
        item.setProductName(product.getName());
        item.setCostPrice(request.getCostPrice());
        item.setQuantity(request.getQuantity());
        item.setTotal(request.getCostPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
    }
    private void recalculate(UUID receiptId) {
        GoodsReceipt receipt = requireReceipt(receiptId);
        receipt.setTotalQuantity(itemRepository.sumQuantity(receiptId));
        receipt.setTotalAmount(itemRepository.sumTotal(receiptId));
        receipt.setUpdatedAt(LocalDateTime.now());
        receiptRepository.save(receipt);
    }
    private void validateHeader(GoodsReceiptDto request) {
        if (!storeRepository.existsById(request.getStoreId())) throw BusinessException.notFound("Cửa hàng không tồn tại");
        if (request.getSupplierId() != null && !supplierRepository.existsById(request.getSupplierId()))
            throw BusinessException.notFound("Nhà cung cấp không tồn tại");
        if (request.getReceivedBy() != null) requireUser(request.getReceivedBy());
    }
    private void validateItem(GoodsReceiptItemDto request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0)
            throw BusinessException.badRequest("Số lượng nhập phải lớn hơn 0");
        if (request.getCostPrice() == null || request.getCostPrice().signum() < 0)
            throw BusinessException.badRequest("Giá nhập không được âm");
    }
    private void requireUser(UUID id) {
        if (id == null || !userRepository.existsById(id)) throw BusinessException.notFound("Nhân viên không tồn tại");
    }
    private void ensureCodeAvailable(String code, UUID excludedId) {
        String value = code.trim().toUpperCase(Locale.ROOT);
        boolean exists = excludedId == null ? receiptRepository.existsByReceiptCode(value)
                : receiptRepository.existsByReceiptCodeAndIdNot(value, excludedId);
        if (exists) throw BusinessException.conflict("Mã phiếu nhập đã tồn tại");
    }
    private void validatePeriod(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) throw BusinessException.badRequest("Khoảng ngày không hợp lệ");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp phiếu nhập không hợp lệ");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
