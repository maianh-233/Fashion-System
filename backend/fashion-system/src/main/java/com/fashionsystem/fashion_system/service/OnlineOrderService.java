package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.CustomerCancelRequest;
import com.fashionsystem.fashion_system.dto.order.CustomerOrderResponse;
import com.fashionsystem.fashion_system.dto.order.OfflineCartRequest;
import com.fashionsystem.fashion_system.dto.order.OfflineQuoteResponse;
import com.fashionsystem.fashion_system.dto.order.OnlineCheckoutRequest;
import com.fashionsystem.fashion_system.dto.order.OnlineOrderPlacedResponse;
import com.fashionsystem.fashion_system.dto.order.OnlineQuoteResponse;
import com.fashionsystem.fashion_system.dto.order.OrderDetailResponse;
import com.fashionsystem.fashion_system.dto.order.PosVariantRow;
import com.fashionsystem.fashion_system.dto.order.VoucherOptionResponse;
import com.fashionsystem.fashion_system.entity.CustomerAddress;
import com.fashionsystem.fashion_system.entity.InventoryBalance;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderAddress;
import com.fashionsystem.fashion_system.entity.OrderItem;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payment.PaymentGateway;
import com.fashionsystem.fashion_system.payment.PaymentGatewayRegistry;
import com.fashionsystem.fashion_system.payment.PaymentProperties;
import com.fashionsystem.fashion_system.repository.CustomerAddressRepository;
import com.fashionsystem.fashion_system.repository.InventoryBalanceRepository;
import com.fashionsystem.fashion_system.repository.OrderAddressRepository;
import com.fashionsystem.fashion_system.repository.OrderItemRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.OrderPromotionRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.ShipmentRepository;
import com.fashionsystem.fashion_system.repository.PosCatalogRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.LineInput;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.LineResult;
import com.fashionsystem.fashion_system.service.OrderVoucherService.CustomerContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đơn online phía khách: tính tiền, phân cửa hàng theo tọa độ, đặt đơn (trừ tồn online ngay), xem và hủy.
 *
 * <p>Đơn được một cửa hàng nhận trọn là đơn thường. Khi phải tách, đơn của khách là <b>đơn tổng</b>
 * (không thuộc cửa hàng nào, giữ tổng tiền, voucher toàn đơn, phí ship gộp và thanh toán), mỗi cửa hàng
 * xử lý một <b>đơn con</b> chứa sản phẩm của mình với mã, trạng thái, nhân viên xử lý và vận chuyển riêng.</p>
 */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("ORDER")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnlineOrderService {
    static final String CHANNEL = "ONLINE";
    private static final java.time.format.DateTimeFormatter PAYMENT_DEADLINE =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    /** Thanh toán khi nhận hàng; VNPAY / MOMO đi qua cổng thanh toán và phải trả trước khi cửa hàng nhận đơn. */
    static final String DEFAULT_PAYMENT = "COD";

    private final OrderCartPricer pricer;
    private final PosCatalogRepository catalog;
    private final InventoryBalanceRepository balanceRepository;
    private final StoreRepository storeRepository;
    private final StoreAllocator allocator;
    private final CustomerAddressRepository customerAddressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final InventoryService inventoryService;
    private final OrderCodeGenerator codeGenerator;
    private final OrderStatusService statusService;
    private final ApplicationEventPublisher events;
    private final PaymentGatewayRegistry gateways;
    private final PaymentProperties paymentProperties;
    private final PaymentRepository paymentRepository;
    private final CustomerCartService cart;
    private final ShipmentRepository shipmentRepository;
    private final OrderPromotionRepository orderPromotionRepository;
    private final FlashSaleService flashSales;

    /** Trạng thái chuyến giao hiển thị cho khách; chuyến thất bại/hủy/thay thế không hiện. */
    private static final Map<com.fashionsystem.fashion_system.entity.ShipmentStatus, String> DELIVERY_LABELS = Map.of(
            com.fashionsystem.fashion_system.entity.ShipmentStatus.ASSIGNING_DRIVER, "Đang tìm tài xế",
            com.fashionsystem.fashion_system.entity.ShipmentStatus.DRIVER_ASSIGNED, "Tài xế đang đến lấy hàng",
            com.fashionsystem.fashion_system.entity.ShipmentStatus.PICKED_UP, "Đang giao hàng",
            com.fashionsystem.fashion_system.entity.ShipmentStatus.COMPLETED, "Đã giao hàng");

    public OnlineQuoteResponse quote(UUID customerId, OnlineCheckoutRequest request) {
        return toQuote(evaluate(customerId, request, false));
    }

    public List<VoucherOptionResponse> vouchers(UUID customerId, OnlineCheckoutRequest request) {
        Checkout checkout = evaluate(customerId, request, false);
        return pricer.voucherOptions(checkout.cart(), checkout.lines(), checkout.customer());
    }

    @Transactional
    public OnlineOrderPlacedResponse place(UUID customerId, OnlineCheckoutRequest request) {
        String requestId = request.clientRequestId() == null || request.clientRequestId().isBlank() ? null
                : request.clientRequestId().trim();
        if (requestId != null) {
            var existing = orderRepository.findByCustomerIdAndClientRequestId(customerId, requestId);
            if (existing.isPresent()) return placedResponse(existing.get());
        }
        String paymentMethod = request.paymentMethod() == null ? DEFAULT_PAYMENT : request.paymentMethod();
        boolean gatewayPayment = !DEFAULT_PAYMENT.equals(paymentMethod);
        if (gatewayPayment && gateways.byCode(paymentMethod).filter(PaymentGateway::configured).isEmpty())
            throw BusinessException.badRequest("Cổng thanh toán " + paymentMethod
                    + " chưa sẵn sàng, vui lòng chọn phương thức khác");

        Checkout checkout = evaluate(customerId, request, true);
        if (!checkout.warnings().isEmpty()) throw BusinessException.invalidState(String.join("; ", checkout.warnings()));

        LocalDateTime now = LocalDateTime.now();
        // Đơn trả qua cổng giữ hàng tới hạn thanh toán; cửa hàng chỉ thấy đơn khi khách đã trả (hoặc đổi sang COD).
        InitialPayment payment = gatewayPayment
                ? new InitialPayment(OrderPaymentStatus.AWAITING_PAYMENT, now.plusMinutes(paymentProperties.holdMinutesOrDefault()))
                : new InitialPayment(OrderPaymentStatus.UNPAID, null);
        var pricing = checkout.cart().pricing();
        boolean split = checkout.allocation().split();
        Order top = orderRepository.save(newOrder(split ? null : checkout.allocation().parts().getFirst().storeId(),
                customerId, null, pricing.subtotal(), pricing.discountTotal(), pricing.total(), paymentMethod, payment,
                request.note(), requestId, now));
        saveAddress(top.getId(), checkout.address(), now);
        pricer.recordApplied(checkout.cart().applied(), top.getId(), customerId, now);
        if (!checkout.flash().isEmpty()) {
            orderRepository.flush(); // lượt dùng suất ghi bằng JDBC tham chiếu đơn vừa tạo
            flashSales.recordUsage(top.getId(), customerId, checkout.flash().values());
        }

        Map<UUID, LineResult> lineByVariant = pricing.lines().stream()
                .collect(Collectors.toMap(line -> line.input().variantId(), Function.identity()));
        List<OrderEvents.OnlineOrderPlaced.Part> eventParts = new ArrayList<>();
        List<Order> storeOrders = new ArrayList<>();
        Map<UUID, BigDecimal> discountLeft = new HashMap<>();
        Map<UUID, Integer> quantityLeft = new HashMap<>();
        for (StoreAllocator.Part part : checkout.allocation().parts()) {
            Order storeOrder = top;
            List<OrderItem> items = items(part, lineByVariant, checkout.rows(), discountLeft, quantityLeft);
            if (split) {
                BigDecimal subtotal = items.stream().map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal discount = items.stream().map(OrderItem::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                // Đơn con chỉ mang giảm giá theo sản phẩm của mình; voucher toàn đơn, phí ship và thanh toán ở đơn tổng.
                storeOrder = orderRepository.save(newOrder(part.storeId(), customerId, top.getId(), subtotal, discount,
                        subtotal.subtract(discount), paymentMethod, payment, request.note(), null, now));
                saveAddress(storeOrder.getId(), checkout.address(), now);
            }
            for (int i = 0; i < items.size(); i++) {
                OrderItem item = items.get(i);
                item.setOrderId(storeOrder.getId());
                item.setCreatedAt(now.plusNanos(i * 1000L));
                itemRepository.save(item);
            }
            storeOrders.add(storeOrder);
            eventParts.add(new OrderEvents.OnlineOrderPlaced.Part(storeOrder.getId(), storeOrder.getOrderCode(),
                    part.storeId(), checkout.stores().get(part.storeId()).getName(),
                    items.stream().map(OnlineOrderService::itemLabel).toList()));
        }

        // Trừ tồn online dưới khóa dòng theo thứ tự (cửa hàng, biến thể) để các đơn đồng thời không khóa chéo.
        record Deduction(UUID orderId, UUID storeId, UUID variantId, int quantity) {
        }
        List<Deduction> deductions = new ArrayList<>();
        for (int i = 0; i < storeOrders.size(); i++) {
            UUID orderId = storeOrders.get(i).getId();
            StoreAllocator.Part part = checkout.allocation().parts().get(i);
            part.quantities().forEach((variant, quantity) -> deductions.add(new Deduction(orderId, part.storeId(), variant, quantity)));
        }
        deductions.stream().sorted(Comparator.comparing(Deduction::storeId).thenComparing(Deduction::variantId))
                .forEach(d -> {
                    try {
                        inventoryService.deductForOrder(d.storeId(), d.variantId(), d.quantity(), CHANNEL, d.orderId(), null);
                    } catch (BusinessException soldOut) {
                        throw BusinessException.conflict("Sản phẩm vừa được người khác mua hết, vui lòng tải lại giỏ hàng");
                    }
                });

        // Sản phẩm đã đặt rời khỏi giỏ cùng transaction; đặt lỗi thì giỏ giữ nguyên.
        cart.removeOrdered(customerId, request.lines().stream().map(OfflineCartRequest.Line::variantId).toList());

        OrderActor actor = OrderActor.customer(customerId);
        statusService.recordCreated(top, actor, split ? "Khách đặt đơn online (tách " + storeOrders.size() + " phần)" : "Khách đặt đơn online");
        if (split) storeOrders.forEach(child -> statusService.recordCreated(child, actor, "Phần của đơn " + top.getOrderCode()));
        if (!gatewayPayment)
            events.publishEvent(new OrderEvents.OnlineOrderPlaced(top.getId(), top.getOrderCode(), customerId, split, eventParts));
        return placedResponse(top);
    }

    /**
     * Sự kiện "đơn mới" dựng lại từ dữ liệu đã lưu: đơn trả qua cổng chỉ báo cửa hàng và khách khi đã thanh toán
     * hoặc khi khách đổi sang COD.
     */
    OrderEvents.OnlineOrderPlaced placedEvent(Order top) {
        List<Order> storeOrders = storeOrders(top);
        Map<UUID, String> storeNames = storeRepository.findAllById(storeOrders.stream().map(Order::getStoreId).toList())
                .stream().collect(Collectors.toMap(Store::getId, Store::getName));
        List<OrderEvents.OnlineOrderPlaced.Part> parts = storeOrders.stream().map(o -> new OrderEvents.OnlineOrderPlaced.Part(
                o.getId(), o.getOrderCode(), o.getStoreId(), storeNames.getOrDefault(o.getStoreId(), ""),
                itemRepository.findAllByOrderIdOrderByCreatedAtAsc(o.getId()).stream().map(OnlineOrderService::itemLabel).toList()))
                .toList();
        return new OrderEvents.OnlineOrderPlaced(top.getId(), top.getOrderCode(), top.getCustomerId(),
                top.getStoreId() == null, parts);
    }

    /**
     * Đơn trả qua cổng bị hủy khi chưa thanh toán: đánh dấu "bỏ dở" (cửa hàng không thấy) và vô hiệu các link
     * thanh toán còn treo. Đơn đã được khóa bởi nghiệp vụ gọi tới.
     */
    void markAbandoned(Order top, List<Order> storeOrders, LocalDateTime now) {
        java.util.LinkedHashSet<Order> all = new java.util.LinkedHashSet<>(storeOrders);
        all.add(top);
        for (Order order : all) {
            order.setPaymentStatus(OrderPaymentStatus.ABANDONED);
            order.setPaymentDueAt(null);
            order.setUpdatedAt(now);
            orderRepository.save(order);
        }
        paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(top.getId()).stream()
                .filter(p -> OrderPaymentStatus.ATTEMPT_PENDING.equals(p.getStatus()))
                .forEach(p -> {
                    p.setStatus(OrderPaymentStatus.ATTEMPT_CANCELLED);
                    p.setFailureReason("Đơn đã hủy trước khi thanh toán");
                    p.setUpdatedAt(now);
                    paymentRepository.save(p);
                });
    }

    /** Đơn của khách, mới nhất trước; {@code status} null là mọi trạng thái. */
    public Page<CustomerOrderResponse> myOrders(UUID customerId, OrderStatus status, Pageable pageable) {
        Page<Order> page = status == null
                ? orderRepository.findAllByCustomerIdAndParentOrderIdIsNull(customerId, pageable)
                : orderRepository.findAllByCustomerIdAndParentOrderIdIsNullAndStatus(customerId, status, pageable);
        return page.map(order -> toCustomerView(order, false));
    }

    public CustomerOrderResponse myOrder(UUID customerId, UUID orderId) {
        return toCustomerView(requireOwnTopOrder(customerId, orderId));
    }

    public List<CustomerCancelRequest.ReasonOption> cancelReasons() {
        return Arrays.stream(CustomerCancelRequest.CancelReason.values())
                .map(r -> new CustomerCancelRequest.ReasonOption(r.name(), r.label())).toList();
    }

    /**
     * Khách hủy khi nhân viên chưa xác nhận (mọi phần còn PENDING) và đơn chưa thanh toán. Tồn kho online được
     * cộng lại; voucher đã dùng không được hoàn. Nhân viên cửa hàng được báo qua sự kiện đổi trạng thái.
     */
    @Transactional
    public CustomerOrderResponse cancel(UUID customerId, UUID orderId, CustomerCancelRequest request) {
        Order top = orderRepository.findByIdForUpdate(orderId)
                .filter(o -> customerId.equals(o.getCustomerId()) && o.getParentOrderId() == null)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        if ("PAID".equals(top.getPaymentStatus()))
            throw BusinessException.invalidState("Đơn đã thanh toán nên không thể tự hủy, vui lòng liên hệ cửa hàng");
        List<Order> storeOrders = storeOrders(top);
        if (storeOrders.stream().anyMatch(o -> o.getStatus() != OrderStatus.PENDING))
            throw BusinessException.invalidState("Cửa hàng đã xác nhận đơn nên bạn không thể tự hủy. Vui lòng liên hệ cửa hàng");
        String reason = reasonText(request);
        boolean unpaidGatewayOrder = OrderPaymentStatus.AWAITING_PAYMENT.equals(top.getPaymentStatus());
        List<Order> locked = new ArrayList<>();
        for (Order storeOrder : storeOrders) {
            Order lockedOrder = storeOrder == top ? top : orderRepository.findByIdForUpdate(storeOrder.getId()).orElseThrow();
            statusService.transition(lockedOrder, OrderStatus.CANCELLED, OrderActor.customer(customerId), reason);
            restock(lockedOrder, null);
            locked.add(lockedOrder);
        }
        if (unpaidGatewayOrder) markAbandoned(top, locked, LocalDateTime.now());
        return toCustomerView(orderRepository.findById(top.getId()).orElseThrow());
    }

    /** Cộng lại tồn online của các dòng trong đơn cửa hàng (hủy hoặc hoàn hàng). */
    void restock(Order storeOrder, UUID actor) {
        String channel = storeOrder.getOrderType() == OrderType.OFFLINE ? OfflineOrderService.CHANNEL : CHANNEL;
        itemRepository.findAllByOrderIdOrderByCreatedAtAsc(storeOrder.getId()).stream()
                .sorted(Comparator.comparing(OrderItem::getProductVariantId))
                .forEach(item -> inventoryService.restockForOrder(storeOrder.getStoreId(), item.getProductVariantId(),
                        item.getQuantity(), channel, storeOrder.getId(), actor));
    }

    private Checkout evaluate(UUID customerId, OnlineCheckoutRequest request, boolean lock) {
        CustomerContext customer = pricer.customer(customerId);
        CustomerAddress address = customerAddressRepository.findByIdAndCustomerId(request.addressId(), customerId)
                .orElseThrow(() -> BusinessException.notFound("Địa chỉ giao hàng không tồn tại"));
        if (address.getLatitude() == null || address.getLongitude() == null)
            throw BusinessException.badRequest("Địa chỉ giao hàng chưa có tọa độ, vui lòng chọn lại vị trí trên bản đồ");

        Map<UUID, Integer> demand = new LinkedHashMap<>();
        request.lines().forEach(line -> demand.merge(line.variantId(), line.quantity(), Integer::sum));
        Map<UUID, PosVariantRow> rows = catalog.findCatalogByIds(demand.keySet()).stream()
                .collect(Collectors.toMap(PosVariantRow::variantId, Function.identity()));
        List<String> warnings = new ArrayList<>();
        Map<UUID, String> lineWarnings = new LinkedHashMap<>();
        List<LineInput> lines = new ArrayList<>();
        demand.forEach((variantId, quantity) -> {
            PosVariantRow row = rows.get(variantId);
            if (row == null) throw BusinessException.notFound("Sản phẩm không tồn tại");
            if (!row.sellable()) {
                String warning = "Sản phẩm " + row.productName() + " (" + row.sku() + ") đã ngừng bán";
                lineWarnings.put(variantId, warning);
                warnings.add(warning);
            }
            lines.add(new LineInput(variantId, row.productId(), row.unitPrice(), quantity));
        });

        // Flash sale chỉ cho đơn online: món giá flash không nhận khuyến mãi sản phẩm, voucher đơn vẫn áp dụng.
        Map<UUID, BigDecimal> unitPrices = lines.stream().collect(Collectors.toMap(LineInput::variantId, LineInput::unitPrice));
        Map<UUID, Integer> sellableDemand = new LinkedHashMap<>(demand);
        sellableDemand.keySet().removeAll(lineWarnings.keySet());
        Map<UUID, FlashSaleService.Allocation> flash = flashSales.allocate(customerId, sellableDemand, unitPrices, lock);
        Map<UUID, OrderPricingCalculator.FlashLine> flashLines = flash.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> new OrderPricingCalculator.FlashLine(e.getValue().quantity(), e.getValue().unitDiscount())));
        OrderCartPricer.PricedCart cart = pricer.price(lines, request.promotionCodesOrEmpty(), customer, lock, flashLines);
        warnings.addAll(cart.warnings());

        Map<UUID, Store> stores = storeRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        Map<UUID, Map<UUID, Integer>> stock = new HashMap<>();
        for (InventoryBalance balance : balanceRepository.findAllByProductVariantIdInAndOnlineQuantityGreaterThan(demand.keySet(), 0))
            if (stores.containsKey(balance.getStoreId()))
                stock.computeIfAbsent(balance.getStoreId(), ignored -> new HashMap<>())
                        .put(balance.getProductVariantId(), balance.getOnlineQuantity());
        StoreAllocator.Result allocation = allocator.allocate(address.getLatitude().doubleValue(),
                address.getLongitude().doubleValue(), demand,
                stores.values().stream().map(s -> new StoreAllocator.StoreLocation(s.getId(), s.getCode(),
                        s.getLatitude().doubleValue(), s.getLongitude().doubleValue())).toList(), stock);
        allocation.shortages().forEach((variantId, missing) -> {
            PosVariantRow row = rows.get(variantId);
            int available = demand.get(variantId) - missing;
            String warning = available <= 0 ? "Sản phẩm " + row.productName() + " (" + row.sku() + ") đã hết hàng"
                    : "Sản phẩm " + row.productName() + " (" + row.sku() + ") chỉ còn " + available;
            lineWarnings.put(variantId, warning);
            warnings.add(warning);
        });
        return new Checkout(customer, address, rows, lines, lineWarnings, cart, allocation, stores, warnings, flash);
    }

    private OnlineQuoteResponse toQuote(Checkout checkout) {
        var pricing = checkout.cart().pricing();
        List<OfflineQuoteResponse.Line> lines = pricing.lines().stream().map(line -> {
            PosVariantRow row = checkout.rows().get(line.input().variantId());
            return new OfflineQuoteResponse.Line(row.variantId(), row.productId(), row.productName(), row.sku(),
                    row.color(), row.size(), row.imageUrl(), line.input().unitPrice(), line.input().quantity(), 0,
                    line.subtotal(), line.discount(), line.total(), lineLabel(line), checkout.lineWarnings().get(row.variantId()));
        }).toList();
        var promotions = checkout.cart().applied().entrySet().stream()
                .map(e -> new OfflineQuoteResponse.AppliedPromotion(e.getKey().getId(), e.getKey().getCode(),
                        e.getKey().getName(), e.getKey().getPromotionType(), e.getValue())).toList();
        List<OnlineQuoteResponse.Part> parts = checkout.allocation().parts().stream()
                .map(part -> quotePart(part, checkout)).toList();
        return new OnlineQuoteResponse(lines, pricing.subtotal(), pricing.productDiscount(), pricing.orderDiscount(),
                BigDecimal.ZERO, pricing.total(), promotions, parts, checkout.allocation().split(),
                List.copyOf(checkout.warnings()), checkout.cart().notices(), checkout.warnings().isEmpty());
    }

    private OnlineQuoteResponse.Part quotePart(StoreAllocator.Part part, Checkout checkout) {
        Store store = checkout.stores().get(part.storeId());
        return new OnlineQuoteResponse.Part(part.storeId(), store.getName(), store.getAddress(),
                Math.round(part.distanceKm() * 10) / 10.0,
                part.quantities().entrySet().stream().map(e -> {
                    PosVariantRow row = checkout.rows().get(e.getKey());
                    return new OnlineQuoteResponse.PartItem(row.variantId(), row.productName(), row.sku(), row.color(),
                            row.size(), e.getValue());
                }).toList());
    }

    /** Mã hiển thị cho phần giảm của dòng: mã khuyến mãi sản phẩm, hoặc nhãn flash sale khi chỉ có giảm flash. */
    private static String lineLabel(LineResult line) {
        if (line.promotion() != null) return line.promotion().getCode();
        return line.flashDiscount().signum() > 0 ? com.fashionsystem.fashion_system.dto.FlashSaleDtos.LINE_LABEL : null;
    }

    /**
     * Dòng của một phần đơn; một dòng giỏ có thể chia cho nhiều cửa hàng. Giảm giá chia theo tỷ lệ số lượng, làm tròn
     * tới đồng; phần lẻ dồn cho phần cuối của dòng (giảm có thể không đều từng món khi dòng có cả món giá flash).
     */
    private static List<OrderItem> items(StoreAllocator.Part part, Map<UUID, LineResult> lines, Map<UUID, PosVariantRow> rows,
                                         Map<UUID, BigDecimal> discountLeft, Map<UUID, Integer> quantityLeft) {
        List<OrderItem> items = new ArrayList<>();
        part.quantities().forEach((variantId, quantity) -> {
            LineResult line = lines.get(variantId);
            PosVariantRow row = rows.get(variantId);
            int lineQuantity = line.input().quantity();
            BigDecimal remaining = discountLeft.getOrDefault(variantId, line.discount());
            int remainingQty = quantityLeft.getOrDefault(variantId, lineQuantity);
            BigDecimal discount = quantity >= remainingQty ? remaining
                    : line.discount().multiply(BigDecimal.valueOf(quantity)).divide(BigDecimal.valueOf(lineQuantity), 0, java.math.RoundingMode.HALF_UP);
            discountLeft.put(variantId, remaining.subtract(discount));
            quantityLeft.put(variantId, remainingQty - quantity);
            BigDecimal subtotal = line.input().unitPrice().multiply(BigDecimal.valueOf(quantity));
            items.add(OrderItem.builder().productId(row.productId()).productVariantId(variantId)
                    .productName(row.productName()).sku(row.sku()).color(row.color()).size(row.size())
                    .imageUrl(row.imageUrl()).price(line.input().unitPrice()).quantity(quantity)
                    .discountAmount(discount).total(subtotal.subtract(discount))
                    .promotionId(line.promotion() == null ? null : line.promotion().getId())
                    .promotionCode(lineLabel(line)).build());
        });
        return items;
    }

    /** Trạng thái thanh toán ban đầu của đơn (và các đơn con). */
    private record InitialPayment(String status, LocalDateTime dueAt) {
    }

    private Order newOrder(UUID storeId, UUID customerId, UUID parentId, BigDecimal subtotal, BigDecimal discount,
                           BigDecimal total, String paymentMethod, InitialPayment payment, String note, String requestId,
                           LocalDateTime now) {
        Order order = Order.builder().orderCode(codeGenerator.nextCode()).orderType(OrderType.ONLINE).storeId(storeId)
                .customerId(customerId).parentOrderId(parentId).subtotal(subtotal).discountTotal(discount)
                .tax(BigDecimal.ZERO).shippingFee(BigDecimal.ZERO).totalAmount(total).paymentMethod(paymentMethod)
                .paymentStatus(payment.status()).paymentDueAt(payment.dueAt())
                .note(note == null || note.isBlank() ? null : note.trim())
                .clientRequestId(requestId).active(true).createdAt(now).build();
        statusService.initialize(order);
        return order;
    }

    private void saveAddress(UUID orderId, CustomerAddress address, LocalDateTime now) {
        orderAddressRepository.save(OrderAddress.builder().orderId(orderId).receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone()).province(address.getProvince())
                .district(address.getDistrict()).ward(address.getWard()).addressLine(address.getAddressLine())
                .postalCode(address.getPostalCode()).latitude(address.getLatitude()).longitude(address.getLongitude())
                .addressType("SHIPPING").createdAt(now).build());
    }

    private OnlineOrderPlacedResponse placedResponse(Order top) {
        CustomerOrderResponse view = toCustomerView(top);
        List<OnlineQuoteResponse.Part> parts = view.parts().stream().map(p -> new OnlineQuoteResponse.Part(null,
                p.storeName(), null, 0, p.items().stream().map(i -> new OnlineQuoteResponse.PartItem(i.variantId(),
                i.productName(), i.sku(), i.color(), i.size(), i.quantity())).toList())).toList();
        String message = view.split()
                ? "Đơn " + top.getOrderCode() + " được giao thành " + parts.size() + " phần từ các cửa hàng: "
                + parts.stream().map(OnlineQuoteResponse.Part::storeName).collect(Collectors.joining(", "))
                : "Đặt hàng thành công, đơn " + top.getOrderCode() + " đang chờ cửa hàng xác nhận";
        if (OrderPaymentStatus.AWAITING_PAYMENT.equals(top.getPaymentStatus()))
            message = "Đã giữ hàng cho đơn " + top.getOrderCode() + ". Vui lòng thanh toán trước "
                    + PAYMENT_DEADLINE.format(top.getPaymentDueAt()) + ", quá hạn đơn sẽ tự hủy";
        return new OnlineOrderPlacedResponse(top.getId(), top.getOrderCode(), top.getTotalAmount(), view.split(), parts,
                message, top.getPaymentMethod(), top.getPaymentStatus(), top.getPaymentDueAt(), null, null);
    }

    private CustomerOrderResponse toCustomerView(Order top) {
        return toCustomerView(top, true);
    }

    /** {@code detailed}: kèm lịch sử trạng thái, thanh toán, voucher và theo dõi vận chuyển (trang chi tiết). */
    private CustomerOrderResponse toCustomerView(Order top, boolean detailed) {
        List<Order> storeOrders = storeOrders(top);
        boolean split = top.getStoreId() == null;
        Map<UUID, String> storeNames = storeRepository.findAllById(storeOrders.stream().map(Order::getStoreId).toList())
                .stream().collect(Collectors.toMap(Store::getId, Store::getName));
        List<CustomerOrderResponse.Part> parts = storeOrders.stream().map(o -> {
            var shipment = detailed ? shipmentRepository.findAllByOrderIdOrderByCreatedAtDesc(o.getId()).stream()
                    .filter(sh -> DELIVERY_LABELS.containsKey(sh.getStatus())).findFirst().orElse(null) : null;
            return new CustomerOrderResponse.Part(o.getId(), o.getOrderCode(), storeNames.get(o.getStoreId()),
                    o.getStatus(), o.getStatus().label(),
                    itemRepository.findAllByOrderIdOrderByCreatedAtAsc(o.getId()).stream().map(OnlineOrderService::itemView).toList(),
                    shipment == null ? null : DELIVERY_LABELS.get(shipment.getStatus()),
                    shipment == null ? null : shipment.getTrackingUrl());
        }).toList();
        int itemCount = parts.stream().flatMap(p -> p.items().stream()).mapToInt(OrderDetailResponse.Item::quantity).sum();
        boolean allPending = storeOrders.stream().allMatch(o -> o.getStatus() == OrderStatus.PENDING);
        boolean canCancel = !"PAID".equals(top.getPaymentStatus()) && allPending;
        boolean awaitingPayment = OrderPaymentStatus.AWAITING_PAYMENT.equals(top.getPaymentStatus()) && allPending
                && top.getPaymentDueAt() != null && top.getPaymentDueAt().isAfter(LocalDateTime.now());
        var address = orderAddressRepository.findByOrderId(top.getId())
                .map(a -> new OrderDetailResponse.Address(a.getReceiverName(), a.getReceiverPhone(), a.getAddressLine(),
                        a.getWard(), a.getDistrict(), a.getProvince(), false)).orElse(null);
        String cancelReason = storeOrders.stream().map(Order::getCancelReason).filter(r -> r != null).findFirst().orElse(null);
        return new CustomerOrderResponse(top.getId(), top.getOrderCode(), top.getCreatedAt(), top.getStatus(),
                top.getStatus().label(), top.getSubtotal(), top.getDiscountTotal(), top.getShippingFee(),
                top.getTotalAmount(), top.getPaymentMethod(), top.getPaymentStatus(), split, parts, address,
                cancelReason, canCancel, top.getPaymentDueAt(), awaitingPayment, awaitingPayment, itemCount, top.getNote(),
                detailed ? history(top, storeOrders, split) : List.of(),
                detailed ? payments(top) : List.of(),
                detailed ? orderPromotionRepository.findAllByOrderIdOrderByCreatedAtAsc(top.getId()).stream()
                        .map(p -> new CustomerOrderResponse.AppliedPromotion(p.getPromotionCode(), p.getDiscountAmount())).toList()
                        : List.of());
    }

    /** Mốc trạng thái của các phần đơn (đơn tách: ghi mã phần), cũ nhất trước. */
    private List<CustomerOrderResponse.History> history(Order top, List<Order> storeOrders, boolean split) {
        List<CustomerOrderResponse.History> entries = new ArrayList<>();
        for (Order order : storeOrders)
            statusService.history(order.getId()).forEach(h -> entries.add(new CustomerOrderResponse.History(h.getToStatus(),
                    h.getToStatus().label(), h.getChangedAt(), h.getReason() != null ? h.getReason() : h.getNote(),
                    split ? order.getOrderCode() : null)));
        entries.sort(Comparator.comparing(CustomerOrderResponse.History::at, Comparator.nullsFirst(Comparator.naturalOrder())));
        return entries;
    }

    /** Các lần thanh toán (bỏ link đã bị hủy vì khách đổi cổng / đổi COD). */
    private List<CustomerOrderResponse.PaymentLine> payments(Order top) {
        return paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(top.getId()).stream()
                .filter(p -> !OrderPaymentStatus.ATTEMPT_CANCELLED.equals(p.getStatus()))
                .map(p -> new CustomerOrderResponse.PaymentLine(p.getMethod(), p.getAmount(), p.getStatus(), p.getPaidAt(),
                        p.getCreatedAt(), p.getFailureReason())).toList();
    }

    List<Order> storeOrders(Order top) {
        return top.getStoreId() != null ? List.of(top) : orderRepository.findAllByParentOrderId(top.getId()).stream()
                .sorted(Comparator.comparing(Order::getOrderCode)).toList();
    }

    private Order requireOwnTopOrder(UUID customerId, UUID orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> customerId.equals(o.getCustomerId()) && o.getParentOrderId() == null)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
    }

    private static OrderDetailResponse.Item itemView(OrderItem item) {
        return new OrderDetailResponse.Item(item.getId(), item.getProductId(), item.getProductVariantId(),
                item.getProductName(), item.getSku(), item.getColor(), item.getSize(), item.getImageUrl(),
                item.getQuantity(), item.getPrice(), item.getDiscountAmount(), item.getTotal(), item.getPromotionCode());
    }

    private static String itemLabel(OrderItem item) {
        String variant = java.util.stream.Stream.of(item.getColor(), item.getSize()).filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining("/"));
        return item.getProductName() + (variant.isEmpty() ? "" : " (" + variant + ")") + " × " + item.getQuantity();
    }

    private static String reasonText(CustomerCancelRequest request) {
        if (request == null) return null;
        String label = null;
        if (request.reasonCode() != null && !request.reasonCode().isBlank()) {
            try {
                label = CustomerCancelRequest.CancelReason.valueOf(request.reasonCode().trim()).label();
            } catch (IllegalArgumentException unknown) {
                throw BusinessException.badRequest("Lý do hủy không hợp lệ");
            }
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        if (label == null) return note;
        return note == null ? label : label + ": " + note;
    }

    private record Checkout(CustomerContext customer, CustomerAddress address, Map<UUID, PosVariantRow> rows,
                            List<LineInput> lines, Map<UUID, String> lineWarnings, OrderCartPricer.PricedCart cart,
                            StoreAllocator.Result allocation, Map<UUID, Store> stores, List<String> warnings,
                            Map<UUID, FlashSaleService.Allocation> flash) {
    }
}
