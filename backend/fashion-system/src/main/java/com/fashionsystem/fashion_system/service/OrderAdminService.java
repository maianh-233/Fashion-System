package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.OrderStatusHistoryDto;
import com.fashionsystem.fashion_system.dto.order.OrderAdminMetaResponse;
import com.fashionsystem.fashion_system.dto.order.OrderDetailResponse;
import com.fashionsystem.fashion_system.dto.order.OrderListRow;
import com.fashionsystem.fashion_system.dto.order.OrderSummaryResponse;
import com.fashionsystem.fashion_system.entity.CustomerAddress;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderAddress;
import com.fashionsystem.fashion_system.entity.OrderItem;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderStatusHistory;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.OrderStatusHistoryMapper;
import com.fashionsystem.fashion_system.repository.CustomerAddressRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderAddressRepository;
import com.fashionsystem.fashion_system.repository.OrderAdminQueryRepository;
import com.fashionsystem.fashion_system.repository.OrderItemRepository;
import com.fashionsystem.fashion_system.repository.OrderPromotionRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.PaymentTransactionRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.ShipmentRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Danh sách, chi tiết và ẩn/khôi phục đơn trong trang quản trị; mọi truy vấn đều kiểm tra phạm vi cửa hàng. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("ORDER")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderAdminService {
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "totalAmount", "orderCode", "status");
    private static final int MAX_PAGE_SIZE = 100;

    /** Mã thao tác trả về cho frontend; các giai đoạn sau bổ sung xác nhận, hủy, giao vận chuyển... */
    public static final String ACTION_HIDE = "HIDE";
    public static final String ACTION_RESTORE = "RESTORE";
    public static final String ACTION_CONFIRM = "CONFIRM";
    public static final String ACTION_CANCEL = "CANCEL";
    public static final String ACTION_CONFIRM_RETURN = "CONFIRM_RETURN";
    public static final String ACTION_CONTACT_CUSTOMER = "CONTACT_CUSTOMER";
    /** Giao cho vận chuyển (lần đầu hoặc gửi lại yêu cầu giao hàng sau khi chuyến trước thất bại). */
    public static final String ACTION_HANDOVER = "HANDOVER_SHIPPING";
    /** Cổng thanh toán online; đơn đã thanh toán qua các cổng này không được hủy. */
    static final Set<String> GATEWAY_METHODS = Set.of("VNPAY", "MOMO");

    private final OrderAccessService access;
    private final OrderStatusService statusService;
    private final OrderStateMachine stateMachine;
    private final OrderStatusHistoryMapper historyMapper;
    private final UserScopeService userScopes;
    private final OrderAdminQueryRepository queries;
    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderAddressRepository addressRepository;
    private final OrderPromotionRepository orderPromotionRepository;
    private final PromotionRepository promotionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ShipmentRepository shipmentRepository;
    private final com.fashionsystem.fashion_system.repository.RefundRepository refundRepository;
    private final com.fashionsystem.fashion_system.repository.OrderChatRoomRepository chatRooms;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public OrderAdminMetaResponse meta(UUID actor) {
        var stores = access.accessibleStores(actor);
        UserScope scope = userScopes.resolve(actor);
        return new OrderAdminMetaResponse(scope.isGlobal(), scope.storeId(), stores,
                OrderAdminMetaResponse.StatusOption.all());
    }

    public List<StoreAccessService.StoreOption> stores(UUID actor) {
        return access.accessibleStores(actor);
    }

    /** Bộ lọc danh sách nhận từ controller (storeId có thể null với nhân viên Store). */
    public record ListQuery(UUID storeId, OrderType type, boolean hidden, OrderStatus status, LocalDate fromDate,
                            LocalDate toDate, String keyword, UUID handlerId, String paymentMethod) {
    }

    public Page<OrderListRow> list(UUID actor, ListQuery query, Pageable pageable) {
        UUID store = access.resolveStore(actor, query.storeId(), OrderPermissions.VIEW);
        if (query.type() == null) throw BusinessException.badRequest("Vui lòng chọn loại đơn");
        if (query.fromDate() != null && query.toDate() != null && query.fromDate().isAfter(query.toDate()))
            throw BusinessException.badRequest("Khoảng ngày không hợp lệ");
        var filter = new OrderAdminQueryRepository.Filter(store, query.type(), !query.hidden(), query.status(),
                query.fromDate() == null ? null : query.fromDate().atStartOfDay(),
                // Ngày kết thúc tính trọn ngày.
                query.toDate() == null ? null : query.toDate().plusDays(1).atStartOfDay(),
                query.keyword(), query.handlerId(), query.paymentMethod());
        return queries.search(filter, page(pageable));
    }

    public OrderSummaryResponse summary(UUID actor, UUID storeId, OrderType type) {
        UUID store = access.resolveStore(actor, storeId, OrderPermissions.VIEW);
        if (type == null) throw BusinessException.badRequest("Vui lòng chọn loại đơn");
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) counts.put(status, 0L);
        for (Object[] row : queries.countByStatus(store, type)) counts.put((OrderStatus) row[0], (Long) row[1]);
        LocalDateTime today = LocalDate.now().atStartOfDay();
        return new OrderSummaryResponse(counts, queries.countHidden(store, type), queries.countSince(store, type, today),
                queries.revenueSince(store, type, today));
    }

    public List<OrderDetailResponse.Person> handlers(UUID actor, UUID storeId) {
        UUID store = access.resolveStore(actor, storeId, OrderPermissions.VIEW);
        return queries.handlers(store).stream()
                .map(row -> new OrderDetailResponse.Person((UUID) row[0], displayName((String) row[1], (String) row[2])))
                .toList();
    }

    public List<OrderStatusHistoryDto> statusHistory(UUID actor, UUID orderId) {
        access.requireOrder(actor, orderId, OrderPermissions.VIEW);
        return statusService.history(orderId).stream().map(historyMapper::toDto).toList();
    }

    public OrderDetailResponse detail(UUID actor, UUID orderId) {
        return toDetail(actor, access.requireOrder(actor, orderId, OrderPermissions.VIEW));
    }

    /** Ẩn (xóa mềm) đơn đã kết thúc: không xóa dữ liệu, chỉ đưa ra khỏi danh sách mặc định. */
    @Transactional
    public OrderDetailResponse hide(UUID actor, UUID orderId) {
        Order order = access.requireOrderForUpdate(actor, orderId, OrderPermissions.HIDE);
        if (!Boolean.TRUE.equals(order.getActive())) throw BusinessException.invalidState("Đơn hàng đã được ẩn");
        if (!order.getStatus().isTerminal())
            throw BusinessException.invalidState("Chỉ được ẩn đơn đã hoàn thành, đã hủy hoặc đã hoàn hàng");
        LocalDateTime now = LocalDateTime.now();
        order.setActive(false);
        order.setHiddenBy(actor);
        order.setHiddenAt(now);
        order.setUpdatedAt(now);
        return toDetail(actor, orderRepository.save(order));
    }

    @Transactional
    public OrderDetailResponse restore(UUID actor, UUID orderId) {
        Order order = access.requireOrderForUpdate(actor, orderId, OrderPermissions.RESTORE);
        if (Boolean.TRUE.equals(order.getActive())) throw BusinessException.invalidState("Đơn hàng đang hiển thị");
        LocalDateTime now = LocalDateTime.now();
        order.setActive(true);
        order.setRestoredBy(actor);
        order.setRestoredAt(now);
        order.setUpdatedAt(now);
        return toDetail(actor, orderRepository.save(order));
    }

    /** Thao tác hợp lệ với trạng thái đơn và quyền của nhân viên. */
    List<String> actions(UUID actor, Order order) {
        List<String> actions = new ArrayList<>();
        boolean active = Boolean.TRUE.equals(order.getActive());
        OrderStatus status = order.getStatus();
        if (active && order.getOrderType() == OrderType.ONLINE) {
            if (stateMachine.canTransition(status, OrderStatus.CONFIRMED, OrderActorType.USER)
                    && access.can(actor, order, OrderPermissions.CONFIRM)) actions.add(ACTION_CONFIRM);
            if (stateMachine.canTransition(status, OrderStatus.HANDED_TO_SHIPPER, OrderActorType.USER)
                    && shipmentRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                    .noneMatch(s -> s.getStatus().isActive())
                    && access.can(actor, order, OrderPermissions.HANDOVER_SHIPPING)) actions.add(ACTION_HANDOVER);
            if (stateMachine.canTransition(status, OrderStatus.CANCELLED, OrderActorType.USER)
                    && (!isGatewayPaid(order) || hasFailedDelivery(order))
                    && access.can(actor, order, OrderPermissions.CANCEL)) actions.add(ACTION_CANCEL);
            if (stateMachine.canTransition(status, OrderStatus.RETURNED, OrderActorType.USER)
                    && (actor.equals(order.getHandlerUserId()) || access.can(actor, order, OrderPermissions.RETURN_OVERRIDE)))
                actions.add(ACTION_CONFIRM_RETURN);
        }
        if (order.getCustomerId() != null && access.can(actor, order, OrderPermissions.CONTACT_CUSTOMER))
            actions.add(ACTION_CONTACT_CUSTOMER);
        if (active && order.getStatus().isTerminal() && access.can(actor, order, OrderPermissions.HIDE))
            actions.add(ACTION_HIDE);
        if (!active && access.can(actor, order, OrderPermissions.RESTORE)) actions.add(ACTION_RESTORE);
        return actions;
    }

    /**
     * Có chuyến giao thất bại (hãng không tìm được tài xế / từ chối / hủy). Đơn đã thanh toán qua cổng chỉ được hủy
     * trong trường hợp này, kèm yêu cầu hoàn tiền thủ công.
     */
    public boolean hasFailedDelivery(Order order) {
        return shipmentRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .anyMatch(s -> s.getStatus() == com.fashionsystem.fashion_system.entity.ShipmentStatus.FAILED);
    }

    /** Đơn (hoặc đơn tổng của đơn con) đã thanh toán qua VNPay/MoMo. */
    public boolean isGatewayPaid(Order order) {
        Order paymentOwner = order.getParentOrderId() == null ? order
                : orderRepository.findById(order.getParentOrderId()).orElse(order);
        return GATEWAY_METHODS.contains(paymentOwner.getPaymentMethod()) && "PAID".equals(paymentOwner.getPaymentStatus());
    }

    private OrderDetailResponse toDetail(UUID actor, Order order) {
        UUID id = order.getId();
        List<OrderItem> items = itemRepository.findAllByOrderIdOrderByCreatedAtAsc(id);
        List<OrderStatusHistory> history = statusService.history(id);

        Set<UUID> userIds = new HashSet<>();
        userIds.add(order.getHandlerUserId());
        userIds.add(order.getCreatedBy());
        userIds.add(order.getHiddenBy());
        userIds.add(order.getRestoredBy());
        history.stream().filter(h -> h.getActorType() == OrderActorType.USER).map(OrderStatusHistory::getChangedBy)
                .forEach(userIds::add);
        userIds.remove(null);
        Map<UUID, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> displayName(u.getFullName(), u.getUsername())));

        var customer = order.getCustomerId() == null ? null : customerRepository.findById(order.getCustomerId())
                .map(c -> new OrderDetailResponse.Customer(c.getId(), c.getCustomerCode(), c.getFullName(), c.getPhone(),
                        c.getEmail()))
                .orElse(null);

        var store = order.getStoreId() == null ? null : storeRepository.findById(order.getStoreId())
                .map(s -> new OrderDetailResponse.Ref(s.getId(), s.getCode(), s.getName())).orElse(null);
        var parent = order.getParentOrderId() == null ? null : orderRepository.findById(order.getParentOrderId())
                .map(p -> new OrderDetailResponse.Ref(p.getId(), p.getOrderCode(), null)).orElse(null);

        BigDecimal productDiscount = items.stream().map(i -> orZero(i.getDiscountAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountTotal = orZero(order.getDiscountTotal());
        var totals = new OrderDetailResponse.Totals(orZero(order.getSubtotal()), productDiscount,
                discountTotal.subtract(productDiscount).max(BigDecimal.ZERO), discountTotal,
                orZero(order.getShippingFee()), orZero(order.getTotalAmount()), order.getCashReceived(),
                order.getChangeAmount());

        var orderPromotions = orderPromotionRepository.findAllByOrderIdOrderByCreatedAtAsc(id);
        Map<UUID, Promotion> promotions = promotionRepository.findAllById(orderPromotions.stream()
                .map(p -> p.getPromotionId()).toList()).stream().collect(Collectors.toMap(Promotion::getId, Function.identity()));
        var applied = orderPromotions.stream().map(p -> {
            Promotion promotion = promotions.get(p.getPromotionId());
            return new OrderDetailResponse.AppliedPromotion(p.getPromotionId(), p.getPromotionCode(),
                    promotion == null ? null : promotion.getName(),
                    promotion == null ? null : promotion.getPromotionType(), p.getDiscountAmount());
        }).toList();

        List<OrderDetailResponse.History> timeline = history.stream().map(h -> new OrderDetailResponse.History(
                h.getFromStatus(), h.getToStatus(), h.getToStatus().label(), h.getActorType(),
                actorName(h, userNames, customer), h.getReason(), h.getNote(), h.getChangedAt())).toList();

        var cancellation = order.getStatus() == OrderStatus.CANCELLED
                ? new OrderDetailResponse.Cancellation(order.getCancelReason(), order.getCancelledByType()) : null;
        var visibility = new OrderDetailResponse.Visibility(Boolean.TRUE.equals(order.getActive()),
                person(order.getHiddenBy(), userNames), order.getHiddenAt(),
                person(order.getRestoredBy(), userNames), order.getRestoredAt());

        return new OrderDetailResponse(id, order.getOrderCode(), order.getOrderType(), order.getStatus(),
                order.getStatus().label(), order.getCreatedAt(), order.getUpdatedAt(), order.getNote(), store, parent,
                customer, address(order), items.stream().map(OrderAdminService::item).toList(), totals, applied,
                payment(order), shipments(id), person(order.getHandlerUserId(), userNames),
                person(order.getCreatedBy(), userNames), cancellation, visibility, timeline, actions(actor, order),
                chatRooms.findByOrderId(order.getParentOrderId() == null ? order.getId() : order.getParentOrderId())
                        .map(com.fashionsystem.fashion_system.entity.OrderChatRoom::getId).orElse(null));
    }

    private OrderDetailResponse.Address address(Order order) {
        OrderAddress address = addressRepository.findByOrderId(order.getId()).orElse(null);
        if (address == null) return null;
        boolean different = false;
        if (order.getCustomerId() != null) {
            CustomerAddress def = customerAddressRepository.findDefaults(order.getCustomerId()).stream().findFirst()
                    .orElse(null);
            different = def == null || !sameAddress(def, address);
        }
        return new OrderDetailResponse.Address(address.getReceiverName(), address.getReceiverPhone(),
                address.getAddressLine(), address.getWard(), address.getDistrict(), address.getProvince(), different);
    }

    private static boolean sameAddress(CustomerAddress def, OrderAddress address) {
        return Objects.equals(normalize(def.getAddressLine()), normalize(address.getAddressLine()))
                && Objects.equals(normalize(def.getWard()), normalize(address.getWard()))
                && Objects.equals(normalize(def.getDistrict()), normalize(address.getDistrict()))
                && Objects.equals(normalize(def.getProvince()), normalize(address.getProvince()))
                && Objects.equals(normalize(def.getReceiverPhone()), normalize(address.getReceiverPhone()));
    }

    /** Thanh toán của đơn; phần của đơn bị tách hiển thị thanh toán của đơn tổng. */
    private OrderDetailResponse.PaymentInfo payment(Order order) {
        Order owner = order.getParentOrderId() == null ? order
                : orderRepository.findById(order.getParentOrderId()).orElse(order);
        List<Payment> payments = paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(owner.getId());
        var transactions = payments.isEmpty() ? List.<com.fashionsystem.fashion_system.entity.PaymentTransaction>of()
                : paymentTransactionRepository.findAllByPaymentIdInOrderByCreatedAtAsc(
                        payments.stream().map(Payment::getId).toList());
        var records = payments.stream().map(p -> new OrderDetailResponse.PaymentRecord(p.getId(), p.getMethod(),
                p.getAmount(), p.getStatus(), p.getTransactionCode(), p.getPaidAt(), p.getCreatedAt(),
                transactions.stream().filter(t -> t.getPaymentId().equals(p.getId()))
                        .map(t -> new OrderDetailResponse.GatewayTransaction(t.getGatewayTransactionId(),
                                t.getTransactionType(), t.getAmount(), t.getStatus(), t.getCreatedAt()))
                        .toList())).toList();
        // Hoàn tiền ghi trên đơn cửa hàng (hủy sau giao thất bại) hoặc trên đơn tổng (tiền về sau khi đơn đã hủy).
        var refunds = refundRepository.findAllByOrderIdInOrderByRequestedAtDesc(
                        new java.util.HashSet<>(List.of(order.getId(), owner.getId()))).stream()
                .map(r -> new OrderDetailResponse.RefundInfo(r.getId(), r.getAmount(), r.getStatus(), r.getReason(),
                        r.getRequestedAt())).toList();
        return new OrderDetailResponse.PaymentInfo(owner.getPaymentMethod(), owner.getPaymentStatus(), records, refunds);
    }

    private List<OrderDetailResponse.ShipmentInfo> shipments(UUID orderId) {
        return shipmentRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(s -> new OrderDetailResponse.ShipmentInfo(s.getId(), s.getAttemptNo() == null ? 1 : s.getAttemptNo(),
                        s.getShippingProvider(), s.getTrackingCode(), s.getTrackingUrl(), s.getFee(), s.getCurrency(),
                        s.getStatus(), s.getProviderStatus(), s.getFailureReason(), s.getDriverName(), s.getDriverPhone(),
                        s.getDriverPlate(), s.getShippedAt(), s.getDeliveredAt(), s.getCreatedAt()))
                .toList();
    }

    private static OrderDetailResponse.Item item(OrderItem item) {
        return new OrderDetailResponse.Item(item.getId(), item.getProductId(), item.getProductVariantId(),
                item.getProductName(), item.getSku(), item.getColor(), item.getSize(), item.getImageUrl(),
                item.getQuantity(), item.getPrice(), orZero(item.getDiscountAmount()), item.getTotal(),
                item.getPromotionCode());
    }

    private static String actorName(OrderStatusHistory history, Map<UUID, String> users,
                                    OrderDetailResponse.Customer customer) {
        return switch (history.getActorType()) {
            case USER -> users.getOrDefault(history.getChangedBy(), "Nhân viên");
            case CUSTOMER -> customer != null && customer.fullName() != null ? customer.fullName() : "Khách hàng";
            case SYSTEM -> "Hệ thống";
            case SHIPPER -> "Đơn vị vận chuyển";
        };
    }

    private static OrderDetailResponse.Person person(UUID id, Map<UUID, String> names) {
        return id == null ? null : new OrderDetailResponse.Person(id, names.getOrDefault(id, "Nhân viên"));
    }

    private static String displayName(String fullName, String username) {
        return fullName == null || fullName.isBlank() ? username : fullName;
    }

    private Pageable page(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE)
            throw BusinessException.badRequest("Mỗi trang tối đa " + MAX_PAGE_SIZE + " đơn");
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp đơn hàng không hợp lệ");
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
