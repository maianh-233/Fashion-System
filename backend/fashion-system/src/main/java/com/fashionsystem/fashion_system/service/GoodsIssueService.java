package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.GoodsIssueDto;
import com.fashionsystem.fashion_system.dto.GoodsIssueItemDto;
import com.fashionsystem.fashion_system.entity.GoodsIssue;
import com.fashionsystem.fashion_system.entity.GoodsIssueItem;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.entity.StockReservation;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.GoodsIssueItemMapper;
import com.fashionsystem.fashion_system.mapper.GoodsIssueMapper;
import com.fashionsystem.fashion_system.repository.GoodsIssueItemRepository;
import com.fashionsystem.fashion_system.repository.GoodsIssueRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StockReservationRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
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

/** Quản lý phiếu xuất và ghi giảm tồn kho khi phiếu được duyệt. */
@Service
@RequiredArgsConstructor
public class GoodsIssueService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "issueCode", "storeId", "orderId", "issueType", "issueDate", "status",
            "totalQuantity", "createdAt", "updatedAt");
    private final GoodsIssueRepository issueRepository;
    private final GoodsIssueItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;
    private final GoodsIssueMapper issueMapper;
    private final GoodsIssueItemMapper itemMapper;
    private final InventoryService inventoryService;

    /** Tạo phiếu xuất ở trạng thái chờ duyệt. */
    @Transactional
    public GoodsIssueDto create(GoodsIssueDto request) {
        validateHeader(request);
        ensureCodeAvailable(request.getIssueCode(), null);
        return issueMapper.toDto(issueRepository.save(issueMapper.toEntity(request)));
    }

    /** Lấy chi tiết phiếu xuất. */
    @Transactional(readOnly = true)
    public GoodsIssueDto getById(UUID id) { return issueMapper.toDto(requireIssue(id)); }

    /** Lấy danh sách phiếu xuất với tìm kiếm, lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<GoodsIssueDto> getList(
            String keyword, UUID storeId, UUID orderId, String issueType, String status,
            LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw BusinessException.badRequest("Khoảng ngày không hợp lệ");
        validateSort(pageable);
        return issueRepository.search(trimToEmpty(keyword), storeId, orderId, normalize(issueType),
                normalize(status), fromDate, toDate, pageable).map(issueMapper::toDto);
    }

    /** Cập nhật thông tin chung của phiếu xuất đang chờ duyệt. */
    @Transactional
    public GoodsIssueDto update(UUID id, GoodsIssueDto request) {
        GoodsIssue issue = requirePendingIssueForUpdate(id);
        validateHeader(request);
        ensureCodeAvailable(request.getIssueCode(), id);
        issueMapper.updateDraft(request, issue);
        return issueMapper.toDto(issueRepository.save(issue));
    }

    /** Xóa phiếu xuất đang chờ duyệt cùng các dòng hàng. */
    @Transactional
    public void delete(UUID id) { issueRepository.delete(requirePendingIssueForUpdate(id)); }

    /** Thêm dòng hàng vào phiếu xuất đang chờ duyệt. */
    @Transactional
    public GoodsIssueItemDto addItem(UUID issueId, GoodsIssueItemDto request) {
        requirePendingIssueForUpdate(issueId);
        ProductVariant variant = requireVariant(request.getProductVariantId());
        validateItem(request);
        GoodsIssueItem item = itemMapper.toEntity(request);
        item.setId(null);
        item.setIssueId(issueId);
        applyItemValues(item, request, variant);
        item.setCreatedAt(LocalDateTime.now());
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(issueId);
        return itemMapper.toDto(item);
    }

    /** Cập nhật dòng hàng thuộc phiếu xuất đang chờ duyệt. */
    @Transactional
    public GoodsIssueItemDto updateItem(UUID issueId, UUID itemId, GoodsIssueItemDto request) {
        requirePendingIssueForUpdate(issueId);
        GoodsIssueItem item = requireItem(issueId, itemId);
        ProductVariant variant = requireVariant(request.getProductVariantId());
        validateItem(request);
        applyItemValues(item, request, variant);
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(issueId);
        return itemMapper.toDto(item);
    }

    /** Xóa dòng hàng khỏi phiếu xuất đang chờ duyệt. */
    @Transactional
    public void removeItem(UUID issueId, UUID itemId) {
        requirePendingIssueForUpdate(issueId);
        itemRepository.delete(requireItem(issueId, itemId));
        itemRepository.flush();
        recalculate(issueId);
    }

    /** Lấy các dòng hàng của phiếu xuất theo thứ tự tạo. */
    @Transactional(readOnly = true)
    public List<GoodsIssueItemDto> getItems(UUID issueId) {
        requireIssue(issueId);
        return itemRepository.findAllByIssueIdOrderByCreatedAtAsc(issueId).stream().map(itemMapper::toDto).toList();
    }

    /** Duyệt phiếu xuất và ghi giảm tồn khả dụng hoặc tiêu thụ phần đã giữ chỗ. */
    @Transactional
    public GoodsIssueDto approve(UUID issueId, UUID approvedBy) {
        GoodsIssue issue = requirePendingIssueForUpdate(issueId);
        requireUser(approvedBy);
        List<GoodsIssueItem> items = itemRepository.findAllByIssueIdOrderByCreatedAtAsc(issueId);
        if (items.isEmpty()) throw BusinessException.invalidState("Phiếu xuất phải có ít nhất một dòng hàng");
        for (GoodsIssueItem item : items) applyInventoryIssue(issue, item, approvedBy);
        issue.setStatus("APPROVED");
        issue.setApprovedBy(approvedBy);
        issue.setUpdatedAt(LocalDateTime.now());
        return issueMapper.toDto(issueRepository.save(issue));
    }

    private void applyInventoryIssue(GoodsIssue issue, GoodsIssueItem item, UUID approvedBy) {
        if (issue.getOrderId() != null) {
            var reservation = reservationRepository
                    .findByOrderIdAndStoreIdAndProductVariantIdAndStatus(
                            issue.getOrderId(), issue.getStoreId(), item.getProductVariantId(), "ACTIVE");
            if (reservation.isPresent()) {
                StockReservation active = reservation.get();
                if (!active.getQuantity().equals(item.getQuantity()))
                    throw BusinessException.invalidState("Số lượng xuất phải khớp số lượng đang giữ chỗ");
                inventoryService.consumeReservation(issue.getStoreId(), item.getProductVariantId(),
                        item.getQuantity(), active.getId(), approvedBy);
                active.setStatus("FULFILLED");
                active.setUpdatedAt(LocalDateTime.now());
                reservationRepository.save(active);
                return;
            }
        }
        inventoryService.issue(issue.getStoreId(), item.getProductVariantId(), item.getQuantity(), issue.getId(), approvedBy);
    }

    private GoodsIssue requireIssue(UUID id) {
        return issueRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Phiếu xuất không tồn tại"));
    }
    private GoodsIssue requirePendingIssueForUpdate(UUID id) {
        GoodsIssue issue = issueRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Phiếu xuất không tồn tại"));
        if (!"PENDING".equalsIgnoreCase(issue.getStatus()))
            throw BusinessException.invalidState("Chỉ được thay đổi phiếu xuất đang chờ duyệt");
        return issue;
    }
    private GoodsIssueItem requireItem(UUID issueId, UUID itemId) {
        return itemRepository.findByIdAndIssueId(itemId, issueId)
                .orElseThrow(() -> BusinessException.notFound("Dòng hàng phiếu xuất không tồn tại"));
    }
    private ProductVariant requireVariant(UUID id) {
        return variantRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Biến thể sản phẩm không tồn tại"));
    }
    private void applyItemValues(GoodsIssueItem item, GoodsIssueItemDto request, ProductVariant variant) {
        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm của biến thể không tồn tại"));
        item.setProductVariantId(variant.getId());
        item.setSku(variant.getSku());
        item.setProductName(product.getName());
        item.setQuantity(request.getQuantity());
    }
    private void recalculate(UUID issueId) {
        GoodsIssue issue = requireIssue(issueId);
        issue.setTotalQuantity(itemRepository.sumQuantity(issueId));
        issue.setUpdatedAt(LocalDateTime.now());
        issueRepository.save(issue);
    }
    private void validateHeader(GoodsIssueDto request) {
        if (!storeRepository.existsById(request.getStoreId())) throw BusinessException.notFound("Cửa hàng không tồn tại");
        if (request.getOrderId() != null && !orderRepository.existsById(request.getOrderId()))
            throw BusinessException.notFound("Đơn hàng không tồn tại");
        if (request.getIssuedBy() != null) requireUser(request.getIssuedBy());
    }
    private void validateItem(GoodsIssueItemDto request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0)
            throw BusinessException.badRequest("Số lượng xuất phải lớn hơn 0");
    }
    private void requireUser(UUID id) {
        if (id == null || !userRepository.existsById(id)) throw BusinessException.notFound("Nhân viên không tồn tại");
    }
    private void ensureCodeAvailable(String code, UUID excludedId) {
        String value = code.trim().toUpperCase(Locale.ROOT);
        boolean exists = excludedId == null ? issueRepository.existsByIssueCode(value)
                : issueRepository.existsByIssueCodeAndIdNot(value, excludedId);
        if (exists) throw BusinessException.conflict("Mã phiếu xuất đã tồn tại");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp phiếu xuất không hợp lệ");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
