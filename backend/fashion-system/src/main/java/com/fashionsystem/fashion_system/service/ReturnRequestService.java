package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.dto.RefundDto;
import com.fashionsystem.fashion_system.dto.order.ReturnDtos;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderItem;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderStatusHistory;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.entity.ReturnRequest;
import com.fashionsystem.fashion_system.entity.ReturnRequestItem;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.notification.PermissionRecipientResolver;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderItemRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.OrderStatusHistoryRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.RefundRepository;
import com.fashionsystem.fashion_system.repository.ReturnRequestItemRepository;
import com.fashionsystem.fashion_system.repository.ReturnRequestRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Đổi / trả hàng. Khách gửi yêu cầu cho đơn đã hoàn thành trong {@value ReturnDtos#WINDOW_DAYS} ngày; cửa hàng
 * đã bán duyệt; khách mang hàng tới, nhân viên chọn tình trạng từng món và hoàn tất: nhận hàng vào kho (tốt →
 * tồn tại quầy, lỗi → tồn hỏng), đổi hàng thì xuất phân loại mới từ tồn tại quầy, trả hàng thì hoàn tiền
 * (tiền mặt / COD hoàn ngay tại quầy, VNPay / MoMo tạo yêu cầu hoàn PENDING để xác nhận sau).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnRequestService {
    static final String VIEW = "RETURN_VIEW";
    static final String MANAGE = "RETURN_MANAGE";
    private static final Set<String> COUNTER_REFUND_METHODS = Set.of("CASH", "COD");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final ReturnRequestRepository requests;
    private final ReturnRequestItemRepository requestItems;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository histories;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final ProductVariantRepository variants;
    private final StoreRepository stores;
    private final CustomerRepository customers;
    private final InventoryService inventory;
    private final RefundService refundService;
    private final StoreAccessService storeAccess;
    private final NotificationPublisher notifications;
    private final PermissionRecipientResolver recipients;
    private final NamedParameterJdbcTemplate jdbc;

    // ------------------------------------------------------------------ customer

    public ReturnDtos.Eligibility eligibility(UUID customerId, UUID orderId) {
        Order order = customerOrder(customerId, orderId);
        String blocked = blockReason(order);
        LocalDateTime deadline = order.getStatus() == OrderStatus.COMPLETED ? completedAt(order).plusDays(ReturnDtos.WINDOW_DAYS) : null;
        List<ReturnDtos.EligibleItem> items = blocked != null ? List.of() : orderItems.findAllByOrderIdOrderByCreatedAtAsc(orderId)
                .stream().map(item -> eligibleItem(order, item)).toList();
        return new ReturnDtos.Eligibility(order.getId(), order.getOrderCode(), blocked == null, blocked, deadline, items,
                ReturnDtos.REASONS);
    }

    @Transactional
    @BusinessAudit("RETURN_REQUEST")
    public ReturnDtos.ReturnRequestView create(UUID customerId, ReturnDtos.CreateRequest request) {
        Order order = orders.findByIdForUpdate(request.orderId())
                .filter(o -> customerId.equals(o.getCustomerId()))
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        String blocked = blockReason(order);
        if (blocked != null) throw BusinessException.invalidState(blocked);
        if (ReturnDtos.REASONS.stream().noneMatch(r -> r.code().equals(request.reasonCode())))
            throw BusinessException.badRequest("Lý do đổi / trả không hợp lệ");
        boolean exchange = "EXCHANGE".equals(request.type());

        Map<UUID, OrderItem> lines = orderItems.findAllByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Set<UUID> seen = new HashSet<>();
        List<ReturnRequestItem> items = new ArrayList<>();
        for (ReturnDtos.CreateItem wanted : request.items()) {
            OrderItem line = lines.get(wanted.orderItemId());
            if (line == null) throw BusinessException.badRequest("Sản phẩm không thuộc đơn hàng này");
            if (!seen.add(line.getId())) throw BusinessException.badRequest("Mỗi sản phẩm chỉ chọn một lần");
            int requestable = line.getQuantity() - (int) requestItems.sumActiveQuantity(line.getId());
            if (wanted.quantity() > requestable)
                throw BusinessException.badRequest(line.getProductName() + " chỉ còn " + Math.max(0, requestable) + " sản phẩm có thể đổi / trả");
            if (exchange) requireExchangeTarget(line, wanted.exchangeVariantId());
            else if (wanted.exchangeVariantId() != null) throw BusinessException.badRequest("Trả hàng không chọn phân loại đổi");
            items.add(ReturnRequestItem.builder().orderItemId(line.getId()).productVariantId(line.getProductVariantId())
                    .quantity(wanted.quantity()).unitPrice(unitPrice(line))
                    .exchangeVariantId(exchange ? wanted.exchangeVariantId() : null).build());
        }

        LocalDateTime now = LocalDateTime.now();
        ReturnRequest saved = requests.saveAndFlush(ReturnRequest.builder().requestCode(nextCode()).orderId(order.getId())
                .customerId(customerId).storeId(order.getStoreId()).requestType(request.type()).status("REQUESTED")
                .reasonCode(request.reasonCode()).note(trimToNull(request.note())).createdAt(now).build());
        items.forEach(item -> item.setRequestId(saved.getId()));
        requestItems.saveAllAndFlush(items);
        afterCommit(() -> notifyStore(saved, "Yêu cầu " + (exchange ? "đổi" : "trả") + " hàng mới · " + saved.getRequestCode(),
                "Đơn " + order.getOrderCode() + ": " + ReturnDtos.reasonLabel(saved.getReasonCode()) + ". Vui lòng xem và duyệt."));
        return view(saved);
    }

    /** Yêu cầu của khách, mới nhất trước; {@code orderId} để lọc theo một đơn (hoặc đơn tổng và các đơn con). */
    public List<ReturnDtos.ReturnRequestView> mine(UUID customerId, UUID orderId) {
        List<ReturnRequest> found = requests.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        if (orderId != null) {
            Set<UUID> scope = new HashSet<>(Set.of(orderId));
            orders.findAllByParentOrderId(orderId).forEach(child -> scope.add(child.getId()));
            found = found.stream().filter(r -> scope.contains(r.getOrderId())).toList();
        }
        return views(found);
    }

    @Transactional
    @BusinessAudit("RETURN_REQUEST")
    public ReturnDtos.ReturnRequestView cancel(UUID customerId, UUID requestId) {
        ReturnRequest request = requests.findByIdForUpdate(requestId)
                .filter(r -> customerId.equals(r.getCustomerId()))
                .orElseThrow(() -> BusinessException.notFound("Yêu cầu đổi / trả không tồn tại"));
        if (!"REQUESTED".equals(request.getStatus()))
            throw BusinessException.invalidState("Chỉ hủy được yêu cầu đang chờ cửa hàng duyệt");
        request.setStatus("CANCELLED");
        request.setUpdatedAt(LocalDateTime.now());
        return view(requests.saveAndFlush(request));
    }

    // ------------------------------------------------------------------ staff

    /** Yêu cầu của các cửa hàng nhân viên được xem; {@code keyword} khớp mã yêu cầu, mã đơn, tên / SĐT khách. */
    public Page<ReturnDtos.ReturnRequestView> list(UUID actor, UUID storeId, String status, String keyword, Pageable pageable) {
        UUID store = storeAccess.requireOrAll(actor, storeId, VIEW);
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder("""
                 from return_requests r join orders o on o.id = r.order_id join customers c on c.id = r.customer_id
                where 1 = 1""");
        if (store != null) {
            where.append(" and r.store_id = :store");
            params.addValue("store", store);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and r.status = :status");
            params.addValue("status", status.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (r.request_code ilike :kw or o.order_code ilike :kw or c.full_name ilike :kw or c.phone ilike :kw)");
            params.addValue("kw", "%" + keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        Long total = jdbc.queryForObject("select count(*)" + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<UUID> ids = jdbc.queryForList("select r.id" + where + " order by r.created_at desc, r.id limit :limit offset :offset",
                params, UUID.class);
        Map<UUID, ReturnRequest> byId = requests.findAllById(ids).stream()
                .collect(Collectors.toMap(ReturnRequest::getId, Function.identity()));
        List<ReturnRequest> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new PageImpl<>(views(ordered), pageable, total == null ? 0 : total);
    }

    public ReturnDtos.ReturnRequestView detail(UUID actor, UUID requestId) {
        ReturnRequest request = requests.findById(requestId)
                .orElseThrow(() -> BusinessException.notFound("Yêu cầu đổi / trả không tồn tại"));
        storeAccess.require(actor, request.getStoreId(), VIEW);
        return view(request);
    }

    @Transactional
    @BusinessAudit("RETURN_REQUEST")
    public ReturnDtos.ReturnRequestView approve(UUID actor, UUID requestId) {
        ReturnRequest request = lockForStaff(actor, requestId);
        requireStatus(request, "REQUESTED", "Chỉ duyệt được yêu cầu đang chờ duyệt");
        LocalDateTime now = LocalDateTime.now();
        request.setStatus("APPROVED");
        request.setApprovedAt(now);
        request.setHandledBy(actor);
        request.setUpdatedAt(now);
        ReturnRequest saved = requests.saveAndFlush(request);
        String store = stores.findById(saved.getStoreId()).map(Store::getName).orElse("cửa hàng");
        afterCommit(() -> notifyCustomer(saved, "Yêu cầu " + saved.getRequestCode() + " đã được duyệt",
                "Vui lòng mang sản phẩm (kèm tem mác) tới " + store + " để hoàn tất " + typeWord(saved) + "."));
        return view(saved);
    }

    @Transactional
    @BusinessAudit("RETURN_REQUEST")
    public ReturnDtos.ReturnRequestView reject(UUID actor, UUID requestId, String reason) {
        ReturnRequest request = lockForStaff(actor, requestId);
        if (!Set.of("REQUESTED", "APPROVED").contains(request.getStatus()))
            throw BusinessException.invalidState("Chỉ từ chối được yêu cầu chưa hoàn tất");
        LocalDateTime now = LocalDateTime.now();
        request.setStatus("REJECTED");
        request.setRejectReason(reason.trim());
        request.setHandledBy(actor);
        request.setUpdatedAt(now);
        ReturnRequest saved = requests.saveAndFlush(request);
        afterCommit(() -> notifyCustomer(saved, "Yêu cầu " + saved.getRequestCode() + " bị từ chối", "Lý do: " + saved.getRejectReason()));
        return view(saved);
    }

    /**
     * Nhận hàng tại quầy và hoàn tất: mỗi món phải có tình trạng. Đổi hàng xuất phân loại mới (đủ tồn tại quầy mới
     * hoàn tất được); trả hàng hoàn tiền theo giá thực trả của từng món.
     */
    @Transactional
    @BusinessAudit("RETURN_REQUEST")
    public ReturnDtos.ReturnRequestView complete(UUID actor, UUID requestId, ReturnDtos.CompleteRequest body) {
        ReturnRequest request = lockForStaff(actor, requestId);
        requireStatus(request, "APPROVED", "Chỉ hoàn tất được yêu cầu đã duyệt");
        List<ReturnRequestItem> items = requestItems.findAllByRequestIdIn(List.of(request.getId()));
        for (ReturnRequestItem item : items) {
            String condition = body.conditions().get(item.getId());
            if (condition == null) throw BusinessException.badRequest("Vui lòng chọn tình trạng cho từng sản phẩm");
            if (item.getProductVariantId() == null)
                throw BusinessException.invalidState("Phân loại của một sản phẩm đã bị xóa, không thể nhận lại kho");
            item.setItemCondition(condition);
            inventory.receiveCustomerReturn(request.getStoreId(), item.getProductVariantId(), item.getQuantity(),
                    "GOOD".equals(condition), request.getId(), actor);
            if ("EXCHANGE".equals(request.getRequestType()))
                inventory.issueForExchange(request.getStoreId(), item.getExchangeVariantId(), item.getQuantity(), request.getId(), actor);
        }
        requestItems.saveAllAndFlush(items);

        LocalDateTime now = LocalDateTime.now();
        String refundNote = "";
        if ("RETURN".equals(request.getRequestType())) {
            BigDecimal amount = items.stream().map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (amount.signum() > 0) {
                Payment payment = paymentFor(request.getOrderId());
                RefundDto refund = refundService.request(RefundDto.builder().paymentId(payment.getId())
                        .refundCode(request.getRequestCode()).amount(amount)
                        .reason("Trả hàng " + request.getRequestCode() + ": " + ReturnDtos.reasonLabel(request.getReasonCode()))
                        .requestedBy(actor).build());
                refunds.findById(refund.getId()).ifPresent(row -> {
                    row.setOrderId(request.getOrderId());
                    refunds.save(row);
                });
                boolean atCounter = COUNTER_REFUND_METHODS.contains(payment.getMethod());
                if (atCounter) refundService.complete(refund.getId(), null, null);
                request.setRefundId(refund.getId());
                refundNote = atCounter ? " Đã hoàn " + money(amount) + " tại quầy."
                        : " Số tiền " + money(amount) + " sẽ được hoàn về tài khoản " + payment.getMethod() + " của bạn.";
            }
            request.setRefundAmount(amount);
        }
        request.setStatus("COMPLETED");
        request.setCompletedAt(now);
        request.setHandledBy(actor);
        request.setUpdatedAt(now);
        ReturnRequest saved = requests.saveAndFlush(request);
        String message = ("EXCHANGE".equals(saved.getRequestType()) ? "Cửa hàng đã đổi sản phẩm cho bạn." : "Cửa hàng đã nhận lại sản phẩm.") + refundNote;
        afterCommit(() -> notifyCustomer(saved, "Yêu cầu " + saved.getRequestCode() + " đã hoàn tất", message));
        return view(saved);
    }

    /** Xác nhận đã hoàn tiền qua cổng (VNPay / MoMo) cho yêu cầu trả hàng đã hoàn tất. */
    @Transactional
    @BusinessAudit("RETURN_REQUEST")
    public ReturnDtos.ReturnRequestView confirmRefund(UUID actor, UUID requestId, String transactionCode) {
        ReturnRequest request = lockForStaff(actor, requestId);
        Refund refund = request.getRefundId() == null ? null : refunds.findById(request.getRefundId()).orElse(null);
        if (refund == null || !"PENDING".equals(refund.getStatus()))
            throw BusinessException.invalidState("Yêu cầu này không có khoản hoàn tiền đang chờ");
        refundService.complete(refund.getId(), trimToNull(transactionCode), null);
        request.setUpdatedAt(LocalDateTime.now());
        ReturnRequest saved = requests.saveAndFlush(request);
        afterCommit(() -> notifyCustomer(saved, "Đã hoàn tiền cho yêu cầu " + saved.getRequestCode(),
                "Lunaria đã hoàn " + money(refund.getAmount()) + " về tài khoản thanh toán của bạn."));
        return view(saved);
    }

    // ------------------------------------------------------------------ rules

    private Order customerOrder(UUID customerId, UUID orderId) {
        return orders.findById(orderId).filter(o -> customerId.equals(o.getCustomerId()))
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
    }

    /** null nếu đơn còn đổi / trả được, ngược lại là lý do. */
    private String blockReason(Order order) {
        if (order.getStoreId() == null) return "Đơn được giao từ nhiều cửa hàng: vui lòng yêu cầu theo từng phần đơn";
        if (order.getStatus() != OrderStatus.COMPLETED) return "Chỉ đổi / trả được đơn hàng đã hoàn thành";
        if (LocalDateTime.now().isAfter(completedAt(order).plusDays(ReturnDtos.WINDOW_DAYS)))
            return "Đơn đã quá " + ReturnDtos.WINDOW_DAYS + " ngày kể từ khi hoàn thành";
        return null;
    }

    private LocalDateTime completedAt(Order order) {
        return histories.findAllByOrderIdOrderByChangedAtAsc(order.getId()).stream()
                .filter(h -> h.getToStatus() == OrderStatus.COMPLETED).map(OrderStatusHistory::getChangedAt)
                .filter(Objects::nonNull).max(Comparator.naturalOrder())
                .orElse(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt());
    }

    private ReturnDtos.EligibleItem eligibleItem(Order order, OrderItem item) {
        int requestable = Math.max(0, item.getQuantity() - (int) requestItems.sumActiveQuantity(item.getId()));
        List<ReturnDtos.ExchangeOption> options = jdbc.query("""
                select v.id, v.color, v.size, coalesce(b.available_quantity, 0) > 0 as in_stock
                from product_variants v
                left join inventory_balances b on b.product_variant_id = v.id and b.store_id = :store
                where v.product_id = :product and v.active and v.id <> :current
                order by v.color, v.size""",
                new MapSqlParameterSource("store", order.getStoreId()).addValue("product", item.getProductId())
                        .addValue("current", item.getProductVariantId()),
                (rs, i) -> new ReturnDtos.ExchangeOption(rs.getObject("id", UUID.class), rs.getString("color"),
                        rs.getString("size"), rs.getBoolean("in_stock")));
        return new ReturnDtos.EligibleItem(item.getId(), item.getProductId(), item.getProductVariantId(), item.getProductName(),
                item.getColor(), item.getSize(), item.getImageUrl(), item.getQuantity(), requestable, unitPrice(item), options);
    }

    private void requireExchangeTarget(OrderItem line, UUID target) {
        if (target == null) throw BusinessException.badRequest("Vui lòng chọn phân loại muốn đổi cho " + line.getProductName());
        ProductVariant variant = variants.findById(target)
                .orElseThrow(() -> BusinessException.notFound("Phân loại muốn đổi không tồn tại"));
        if (!Objects.equals(variant.getProductId(), line.getProductId()))
            throw BusinessException.badRequest("Chỉ đổi sang màu / size khác của cùng sản phẩm");
        if (target.equals(line.getProductVariantId())) throw BusinessException.badRequest("Phân loại đổi phải khác phân loại đã mua");
        if (!Boolean.TRUE.equals(variant.getActive())) throw BusinessException.badRequest("Phân loại muốn đổi đã ngừng bán");
    }

    private static BigDecimal unitPrice(OrderItem item) {
        BigDecimal total = item.getTotal() != null ? item.getTotal() : item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return total.divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);
    }

    /** Thanh toán thành công của đơn (hoặc đơn tổng nếu đơn bị tách, nơi khách thực sự trả tiền). */
    private Payment paymentFor(UUID orderId) {
        Order order = orders.findById(orderId).orElseThrow();
        List<UUID> candidates = new ArrayList<>(List.of(orderId));
        if (order.getParentOrderId() != null) candidates.add(order.getParentOrderId());
        return candidates.stream().flatMap(id -> payments.findAllByOrderIdOrderByCreatedAtAsc(id).stream())
                .filter(p -> Set.of("SUCCESS", "REFUNDED").contains(p.getStatus())).findFirst()
                .orElseThrow(() -> BusinessException.invalidState("Đơn chưa ghi nhận thanh toán thành công, không thể hoàn tiền"));
    }

    private ReturnRequest lockForStaff(UUID actor, UUID requestId) {
        ReturnRequest request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> BusinessException.notFound("Yêu cầu đổi / trả không tồn tại"));
        storeAccess.require(actor, request.getStoreId(), MANAGE);
        return request;
    }

    private static void requireStatus(ReturnRequest request, String status, String message) {
        if (!status.equals(request.getStatus())) throw BusinessException.invalidState(message);
    }

    private String nextCode() {
        Long value = jdbc.getJdbcTemplate().queryForObject("select nextval('return_request_code_seq')", Long.class);
        return "RT-%s-%05d".formatted(LocalDate.now(ZONE).format(CODE_DATE), value);
    }

    // ------------------------------------------------------------------ views & notifications

    private ReturnDtos.ReturnRequestView view(ReturnRequest request) {
        return views(List.of(request)).getFirst();
    }

    private List<ReturnDtos.ReturnRequestView> views(List<ReturnRequest> list) {
        if (list.isEmpty()) return List.of();
        Map<UUID, Order> orderById = orders.findAllById(list.stream().map(ReturnRequest::getOrderId).toList()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        Map<UUID, Store> storeById = stores.findAllById(list.stream().map(ReturnRequest::getStoreId).toList()).stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        Map<UUID, Customer> customerById = customers.findAllById(list.stream().map(ReturnRequest::getCustomerId).toList()).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<UUID, Refund> refundById = refunds.findAllById(list.stream().map(ReturnRequest::getRefundId).filter(Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Refund::getId, Function.identity()));
        Map<UUID, List<ReturnRequestItem>> itemsByRequest = requestItems.findAllByRequestIdIn(list.stream().map(ReturnRequest::getId).toList())
                .stream().collect(Collectors.groupingBy(ReturnRequestItem::getRequestId));
        List<ReturnRequestItem> allItems = itemsByRequest.values().stream().flatMap(List::stream).toList();
        Map<UUID, OrderItem> lineById = orderItems.findAllById(allItems.stream().map(ReturnRequestItem::getOrderItemId).toList()).stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<UUID, ProductVariant> exchangeById = variants.findAllById(allItems.stream().map(ReturnRequestItem::getExchangeVariantId)
                .filter(Objects::nonNull).toList()).stream().collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        return list.stream().map(r -> {
            Order order = orderById.get(r.getOrderId());
            Customer customer = customerById.get(r.getCustomerId());
            Refund refund = r.getRefundId() == null ? null : refundById.get(r.getRefundId());
            List<ReturnDtos.RequestItem> items = itemsByRequest.getOrDefault(r.getId(), List.of()).stream().map(i -> {
                OrderItem line = lineById.get(i.getOrderItemId());
                ProductVariant target = i.getExchangeVariantId() == null ? null : exchangeById.get(i.getExchangeVariantId());
                return new ReturnDtos.RequestItem(i.getId(), i.getOrderItemId(), line == null ? "" : line.getProductName(),
                        line == null ? null : line.getColor(), line == null ? null : line.getSize(),
                        line == null ? null : line.getImageUrl(), i.getQuantity(), i.getUnitPrice(), i.getExchangeVariantId(),
                        target == null ? null : target.getColor(), target == null ? null : target.getSize(), i.getItemCondition());
            }).toList();
            BigDecimal estimated = "RETURN".equals(r.getRequestType())
                    ? items.stream().map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : null;
            return new ReturnDtos.ReturnRequestView(r.getId(), r.getRequestCode(), r.getOrderId(),
                    order == null ? null : order.getOrderCode(), r.getStoreId(),
                    storeById.containsKey(r.getStoreId()) ? storeById.get(r.getStoreId()).getName() : null,
                    r.getCustomerId(), customer == null ? null : customer.getFullName(), customer == null ? null : customer.getPhone(),
                    r.getRequestType(), r.getStatus(), ReturnDtos.STATUS_LABELS.getOrDefault(r.getStatus(), r.getStatus()),
                    r.getReasonCode(), ReturnDtos.reasonLabel(r.getReasonCode()), r.getNote(), r.getRejectReason(), estimated,
                    r.getRefundAmount(), order == null ? null : order.getPaymentMethod(), refund == null ? null : refund.getStatus(),
                    items, r.getCreatedAt(), r.getApprovedAt(), r.getCompletedAt());
        }).toList();
    }

    private void notifyCustomer(ReturnRequest request, String title, String message) {
        UUID orderId = orders.findById(request.getOrderId()).map(o -> o.getParentOrderId() != null ? o.getParentOrderId() : o.getId())
                .orElse(request.getOrderId());
        publish(Set.of(RealtimeIdentity.customer(request.getCustomerId())), NotificationType.RETURN_STATUS_CHANGED, title, message,
                request, "/orderdetail?id=" + orderId);
    }

    private void notifyStore(ReturnRequest request, String title, String message) {
        publish(recipients.resolveStoreStaff(MANAGE, request.getStoreId()), NotificationType.RETURN_REQUESTED, title, message,
                request, "/admin/returns?id=" + request.getId());
    }

    private void publish(Set<RealtimeIdentity> to, String type, String title, String message, ReturnRequest request, String url) {
        if (to.isEmpty()) return;
        try {
            notifications.publish(new NotificationRequest(List.copyOf(to), null, type, title, message, "RETURN_REQUEST",
                    request.getId(), url, Map.of("requestCode", request.getRequestCode())));
        } catch (RuntimeException failure) {
            log.warn("Return notification {} for {} failed: {}", type, request.getRequestCode(), failure.getMessage());
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                action.run();
            }
        });
    }

    private static String typeWord(ReturnRequest request) {
        return "EXCHANGE".equals(request.getRequestType()) ? "đổi hàng" : "trả hàng";
    }

    private static String money(BigDecimal amount) {
        return String.format("%,.0f", amount).replace(',', '.') + "đ";
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
