package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.OrderAddressDto;
import com.fashionsystem.fashion_system.dto.OrderDto;
import com.fashionsystem.fashion_system.dto.OrderItemDto;
import com.fashionsystem.fashion_system.dto.OrderPromotionDto;
import com.fashionsystem.fashion_system.dto.OrderStatusHistoryDto;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderAddress;
import com.fashionsystem.fashion_system.entity.OrderItem;
import com.fashionsystem.fashion_system.entity.OrderPromotion;
import com.fashionsystem.fashion_system.entity.OrderStatusHistory;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.OrderAddressMapper;
import com.fashionsystem.fashion_system.mapper.OrderItemMapper;
import com.fashionsystem.fashion_system.mapper.OrderMapper;
import com.fashionsystem.fashion_system.mapper.OrderPromotionMapper;
import com.fashionsystem.fashion_system.mapper.OrderStatusHistoryMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderAddressRepository;
import com.fashionsystem.fashion_system.repository.OrderItemRepository;
import com.fashionsystem.fashion_system.repository.OrderPromotionRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.OrderStatusHistoryRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý aggregate đơn hàng, dòng hàng, địa chỉ, khuyến mãi và lịch sử trạng thái. */
@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "orderCode", "customerId", "storeId", "orderType", "status",
            "subtotal", "discountTotal", "totalAmount", "paymentStatus", "createdAt", "updatedAt");
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PROCESSING", "CANCELLED"),
            "PROCESSING", Set.of("SHIPPING", "CANCELLED"),
            "SHIPPING", Set.of("DELIVERED", "RETURNED"),
            "DELIVERED", Set.of("RETURNED"));

    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderAddressRepository addressRepository;
    private final OrderPromotionRepository orderPromotionRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final PromotionRepository promotionRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final OrderAddressMapper addressMapper;
    private final OrderPromotionMapper orderPromotionMapper;
    private final OrderStatusHistoryMapper historyMapper;
    private final PromotionService promotionService;

    /** Tạo đơn hàng chờ xác nhận và ghi lịch sử trạng thái đầu tiên. */
    @Transactional
    public OrderDto create(OrderDto request, UUID createdBy) {
        validateHeader(request);
        ensureCodeAvailable(request.getOrderCode(), null);
        Order order = orderRepository.save(orderMapper.toEntity(request));
        appendHistory(order.getId(), null, "PENDING", createdBy, "Tạo đơn hàng");
        return orderMapper.toDto(order);
    }

    /** Lấy chi tiết đơn hàng. */
    @Transactional(readOnly = true)
    public OrderDto getById(UUID id) { return orderMapper.toDto(requireOrder(id)); }

    /** Lấy danh sách đơn hàng với tìm kiếm, lọc thời gian và phân trang. */
    @Transactional(readOnly = true)
    public Page<OrderDto> getList(
            String keyword, UUID customerId, UUID storeId, String orderType, String status,
            String paymentStatus, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate))
            throw BusinessException.badRequest("Khoảng ngày đơn hàng không hợp lệ");
        validateSort(pageable);
        return orderRepository.search(trimToEmpty(keyword), customerId, storeId, normalize(orderType),
                normalize(status), normalize(paymentStatus), fromDate, toDate, pageable).map(orderMapper::toDto);
    }

    /** Cập nhật thông tin đơn hàng khi còn chờ xác nhận. */
    @Transactional
    public OrderDto update(UUID id, OrderDto request) {
        Order order = requirePendingForUpdate(id);
        validateHeader(request);
        ensureCodeAvailable(request.getOrderCode(), id);
        orderMapper.updateDraft(request, order);
        recalculate(order);
        return orderMapper.toDto(orderRepository.save(order));
    }

    /** Xóa đơn hàng khi còn chờ xác nhận. */
    @Transactional
    public void delete(UUID id) { orderRepository.delete(requirePendingForUpdate(id)); }

    /** Thêm sản phẩm vào đơn hàng đang chờ xác nhận. */
    @Transactional
    public OrderItemDto addItem(UUID orderId, OrderItemDto request) {
        Order order = requirePendingForUpdate(orderId);
        ProductVariant variant = requireVariant(request.getProductVariantId());
        validateQuantity(request.getQuantity());
        OrderItem item = itemMapper.toEntity(request);
        item.setId(null);
        item.setOrderId(orderId);
        applyItemSnapshot(item, variant, request.getQuantity());
        item.setCreatedAt(LocalDateTime.now());
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(order);
        orderRepository.save(order);
        return itemMapper.toDto(item);
    }

    /** Cập nhật sản phẩm trong đơn hàng đang chờ xác nhận. */
    @Transactional
    public OrderItemDto updateItem(UUID orderId, UUID itemId, OrderItemDto request) {
        Order order = requirePendingForUpdate(orderId);
        OrderItem item = requireItem(orderId, itemId);
        ProductVariant variant = requireVariant(request.getProductVariantId());
        validateQuantity(request.getQuantity());
        applyItemSnapshot(item, variant, request.getQuantity());
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(order);
        orderRepository.save(order);
        return itemMapper.toDto(item);
    }

    /** Xóa sản phẩm khỏi đơn hàng đang chờ xác nhận. */
    @Transactional
    public void removeItem(UUID orderId, UUID itemId) {
        Order order = requirePendingForUpdate(orderId);
        itemRepository.delete(requireItem(orderId, itemId));
        itemRepository.flush();
        recalculate(order);
        orderRepository.save(order);
    }

    /** Lấy các dòng sản phẩm của đơn hàng. */
    @Transactional(readOnly = true)
    public List<OrderItemDto> getItems(UUID orderId) {
        requireOrder(orderId);
        return itemRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId).stream().map(itemMapper::toDto).toList();
    }

    /** Tạo hoặc thay thế địa chỉ giao hàng khi đơn còn chờ xác nhận. */
    @Transactional
    public OrderAddressDto upsertAddress(UUID orderId, OrderAddressDto request) {
        requirePendingForUpdate(orderId);
        OrderAddress address = addressRepository.findByOrderId(orderId).orElseGet(() -> {
            OrderAddress created = new OrderAddress();
            created.setOrderId(orderId);
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });
        copyAddress(request, address);
        return addressMapper.toDto(addressRepository.save(address));
    }

    /** Lấy địa chỉ giao hàng của đơn hàng. */
    @Transactional(readOnly = true)
    public OrderAddressDto getAddress(UUID orderId) {
        requireOrder(orderId);
        return addressMapper.toDto(addressRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng chưa có địa chỉ giao hàng")));
    }

    /** Áp dụng khuyến mãi đã được xác định đủ điều kiện vào đơn đang chờ xác nhận. */
    @Transactional
    public OrderPromotionDto applyPromotion(UUID orderId, UUID promotionId) {
        Order order = requirePendingForUpdate(orderId);
        if (orderPromotionRepository.existsByOrderIdAndPromotionId(orderId, promotionId))
            throw BusinessException.conflict("Khuyến mãi đã được áp dụng cho đơn hàng");
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> BusinessException.notFound("Khuyến mãi không tồn tại"));
        BigDecimal amount = promotionService.calculateDiscount(promotionId, order.getSubtotal(), LocalDateTime.now());
        OrderPromotion applied = OrderPromotion.builder().orderId(orderId).promotionId(promotionId)
                .promotionCode(promotion.getCode()).discountAmount(amount).createdAt(LocalDateTime.now()).build();
        applied = orderPromotionRepository.save(applied);
        orderPromotionRepository.flush();
        recalculate(order);
        orderRepository.save(order);
        return orderPromotionMapper.toDto(applied);
    }

    /** Gỡ khuyến mãi khỏi đơn hàng đang chờ xác nhận. */
    @Transactional
    public void removePromotion(UUID orderId, UUID appliedPromotionId) {
        Order order = requirePendingForUpdate(orderId);
        OrderPromotion applied = orderPromotionRepository.findByIdAndOrderId(appliedPromotionId, orderId)
                .orElseThrow(() -> BusinessException.notFound("Khuyến mãi của đơn hàng không tồn tại"));
        orderPromotionRepository.delete(applied);
        orderPromotionRepository.flush();
        recalculate(order);
        orderRepository.save(order);
    }

    /** Lấy các khuyến mãi đã áp dụng cho đơn hàng. */
    @Transactional(readOnly = true)
    public List<OrderPromotionDto> getPromotions(UUID orderId) {
        requireOrder(orderId);
        return orderPromotionRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(orderPromotionMapper::toDto).toList();
    }

    /** Chuyển trạng thái đơn hàng theo state machine và ghi lịch sử append-only. */
    @Transactional
    public OrderDto changeStatus(UUID orderId, String targetStatus, UUID changedBy, String note) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        String from = normalize(order.getStatus());
        String target = normalize(targetStatus);
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(target))
            throw BusinessException.invalidState("Chuyển trạng thái đơn hàng không hợp lệ: " + from + " → " + target);
        if (changedBy != null && !userRepository.existsById(changedBy))
            throw BusinessException.notFound("Nhân viên thay đổi trạng thái không tồn tại");
        order.setStatus(target);
        order.setUpdatedAt(LocalDateTime.now());
        appendHistory(orderId, from, target, changedBy, note);
        return orderMapper.toDto(orderRepository.save(order));
    }

    /** Lấy lịch sử trạng thái đơn hàng theo phân trang. */
    @Transactional(readOnly = true)
    public Page<OrderStatusHistoryDto> getStatusHistory(UUID orderId, Pageable pageable) {
        requireOrder(orderId);
        return historyRepository.findAllByOrderId(orderId, pageable).map(historyMapper::toDto);
    }

    private Order requireOrder(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
    }
    private Order requirePendingForUpdate(UUID id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        if (!"PENDING".equalsIgnoreCase(order.getStatus()))
            throw BusinessException.invalidState("Chỉ được sửa đơn hàng đang chờ xác nhận");
        return order;
    }
    private OrderItem requireItem(UUID orderId, UUID itemId) {
        return itemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> BusinessException.notFound("Dòng sản phẩm đơn hàng không tồn tại"));
    }
    private ProductVariant requireVariant(UUID id) {
        return variantRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Biến thể sản phẩm không tồn tại"));
    }
    private void applyItemSnapshot(OrderItem item, ProductVariant variant, int quantity) {
        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm của biến thể không tồn tại"));
        BigDecimal price = variant.getSalePrice() == null ? variant.getPrice() : variant.getSalePrice();
        item.setProductId(product.getId()); item.setProductVariantId(variant.getId());
        item.setProductName(product.getName()); item.setSku(variant.getSku()); item.setColor(variant.getColor());
        item.setSize(variant.getSize()); item.setImageUrl(product.getImageUrl()); item.setPrice(price);
        item.setQuantity(quantity); item.setTotal(price.multiply(BigDecimal.valueOf(quantity)));
    }
    private void recalculate(Order order) {
        BigDecimal subtotal = itemRepository.sumTotal(order.getId());
        BigDecimal discount = orderPromotionRepository.sumDiscount(order.getId()).min(subtotal);
        BigDecimal total = subtotal.subtract(discount).add(orZero(order.getTax())).add(orZero(order.getShippingFee()));
        order.setSubtotal(subtotal); order.setDiscountTotal(discount); order.setTotalAmount(total.max(BigDecimal.ZERO));
        order.setUpdatedAt(LocalDateTime.now());
    }
    private void copyAddress(OrderAddressDto source, OrderAddress target) {
        target.setReceiverName(source.getReceiverName().trim()); target.setReceiverPhone(source.getReceiverPhone().trim());
        target.setProvince(source.getProvince()); target.setDistrict(source.getDistrict()); target.setWard(source.getWard());
        target.setAddressLine(source.getAddressLine()); target.setPostalCode(source.getPostalCode());
        target.setLatitude(source.getLatitude()); target.setLongitude(source.getLongitude());
        target.setAddressType(source.getAddressType() == null || source.getAddressType().isBlank()
                ? "SHIPPING" : normalize(source.getAddressType()));
    }
    private void appendHistory(UUID orderId, String from, String to, UUID changedBy, String note) {
        historyRepository.save(OrderStatusHistory.builder().orderId(orderId).fromStatus(from).toStatus(to)
                .changedBy(changedBy).note(note).changedAt(LocalDateTime.now()).build());
    }
    private void validateHeader(OrderDto request) {
        if (request.getCustomerId() != null && !customerRepository.existsById(request.getCustomerId()))
            throw BusinessException.notFound("Khách hàng không tồn tại");
        if (request.getStoreId() != null && !storeRepository.existsById(request.getStoreId()))
            throw BusinessException.notFound("Cửa hàng không tồn tại");
        if (orZero(request.getTax()).signum() < 0 || orZero(request.getShippingFee()).signum() < 0)
            throw BusinessException.badRequest("Thuế và phí vận chuyển không được âm");
    }
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) throw BusinessException.badRequest("Số lượng sản phẩm phải lớn hơn 0");
    }
    private void ensureCodeAvailable(String code, UUID excludedId) {
        String value = code.trim().toUpperCase(Locale.ROOT);
        boolean exists = excludedId == null ? orderRepository.existsByOrderCode(value)
                : orderRepository.existsByOrderCodeAndIdNot(value, excludedId);
        if (exists) throw BusinessException.conflict("Mã đơn hàng đã tồn tại");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp đơn hàng không hợp lệ");
    }
    private BigDecimal orZero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
