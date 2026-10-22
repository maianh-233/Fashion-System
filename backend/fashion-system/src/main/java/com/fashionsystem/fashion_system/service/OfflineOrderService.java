package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.OfflineCartRequest;
import com.fashionsystem.fashion_system.dto.order.OfflineOrderCreatedResponse;
import com.fashionsystem.fashion_system.dto.order.OfflineQuoteResponse;
import com.fashionsystem.fashion_system.dto.order.PosProductRow;
import com.fashionsystem.fashion_system.dto.order.PosVariantRow;
import com.fashionsystem.fashion_system.dto.order.VoucherOptionResponse;
import com.fashionsystem.fashion_system.entity.CustomerPromotion;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderItem;
import com.fashionsystem.fashion_system.entity.OrderPromotion;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.entity.PromotionUsage;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerPromotionRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderItemRepository;
import com.fashionsystem.fashion_system.repository.OrderPromotionRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.PosCatalogRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionUsageRepository;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.LineInput;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.LineResult;
import com.fashionsystem.fashion_system.service.OrderPricingCalculator.ProductPromotion;
import com.fashionsystem.fashion_system.service.OrderVoucherService.CustomerContext;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bán hàng tại quầy: tra cứu biến thể còn hàng, gợi ý voucher, tính tiền realtime và tạo đơn offline.
 *
 * <p>Tạo đơn tính lại toàn bộ từ dữ liệu trong DB (không dùng số tiền từ frontend) và chạy trong một
 * transaction: lưu đơn, trừ tồn kho offline, ghi lượt dùng voucher và phiếu thu tiền mặt. Đơn offline
 * hoàn thành ngay khi tạo.</p>
 */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("ORDER")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfflineOrderService {
    static final String CHANNEL = "OFFLINE";
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderAccessService access;
    private final PosCatalogRepository catalog;
    private final OrderCartPricer pricer;
    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final OrderCodeGenerator codeGenerator;
    private final OrderStatusService statusService;
    private final LoyaltyEarningService loyalty;

    /** Tìm đúng một biến thể theo SKU hoặc mã vạch (nhập tay hoặc máy quét). */
    public PosVariantRow findByCode(UUID actor, UUID storeId, String code) {
        UUID store = access.resolveStore(actor, storeId, OrderPermissions.CREATE_OFFLINE);
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) throw BusinessException.badRequest("Vui lòng nhập mã sản phẩm");
        PosVariantRow row = catalog.findByCode(store, normalized).stream()
                // Mã trùng SKU được ưu tiên hơn mã vạch.
                .min(Comparator.comparing((PosVariantRow r) -> !normalized.equalsIgnoreCase(r.sku())))
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy sản phẩm có mã " + normalized));
        if (!row.sellable()) throw BusinessException.invalidState("Sản phẩm " + row.sku() + " đã ngừng bán");
        if (row.stock() <= 0)
            throw BusinessException.invalidState("Sản phẩm " + row.sku() + " đã hết hàng tại cửa hàng");
        return row;
    }

    /** Sản phẩm còn ít nhất một biến thể đang bán và còn hàng tại cửa hàng. */
    public Page<PosProductRow> searchProducts(UUID actor, UUID storeId, String keyword, Pageable pageable) {
        UUID store = access.resolveStore(actor, storeId, OrderPermissions.CREATE_OFFLINE);
        if (pageable.getPageSize() > MAX_PAGE_SIZE)
            throw BusinessException.badRequest("Mỗi trang tối đa " + MAX_PAGE_SIZE + " sản phẩm");
        return catalog.searchSellableProducts(store, keyword == null ? "" : keyword.trim(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
    }

    public List<PosVariantRow> productVariants(UUID actor, UUID storeId, UUID productId) {
        UUID store = access.resolveStore(actor, storeId, OrderPermissions.CREATE_OFFLINE);
        return catalog.findSellableByProduct(store, productId);
    }

    public OfflineQuoteResponse quote(UUID actor, OfflineCartRequest request) {
        UUID store = access.resolveStore(actor, request.storeId(), OrderPermissions.CREATE_OFFLINE);
        return toResponse(evaluate(store, request, false));
    }

    /**
     * Voucher cho giỏ hiện tại, sắp xếp theo số tiền giảm thực tế giảm dần. Voucher toàn đơn được tính
     * trên tạm tính sau các khuyến mãi sản phẩm đang chọn; khuyến mãi sản phẩm tính trên các dòng phù hợp.
     */
    public List<VoucherOptionResponse> vouchers(UUID actor, OfflineCartRequest request) {
        UUID store = access.resolveStore(actor, request.storeId(), OrderPermissions.CREATE_OFFLINE);
        Evaluation current = evaluate(store, request, false);
        return pricer.voucherOptions(current.cart(), current.lineInputs(), current.customer());
    }

    @Transactional
    public OfflineOrderCreatedResponse create(UUID actor, OfflineCartRequest request) {
        UUID store = access.resolveStore(actor, request.storeId(), OrderPermissions.CREATE_OFFLINE);
        Evaluation evaluation = evaluate(store, request, true);
        if (!evaluation.warnings().isEmpty())
            throw BusinessException.invalidState(String.join("; ", evaluation.warnings()));
        BigDecimal total = evaluation.pricing().total();
        if (request.cashReceived() == null) throw BusinessException.badRequest("Vui lòng nhập tiền khách đưa");
        if (request.cashReceived().compareTo(total) < 0)
            throw BusinessException.badRequest("Tiền khách đưa còn thiếu " + OrderCartPricer.vnd(total.subtract(request.cashReceived())));

        LocalDateTime now = LocalDateTime.now();
        UUID customerId = evaluation.customer().customerId();
        Order order = Order.builder()
                .orderCode(codeGenerator.nextCode()).orderType(OrderType.OFFLINE).storeId(store).customerId(customerId)
                .createdBy(actor).handlerUserId(actor)
                .subtotal(evaluation.pricing().subtotal()).discountTotal(evaluation.pricing().discountTotal())
                .tax(BigDecimal.ZERO).shippingFee(BigDecimal.ZERO).totalAmount(total)
                .paymentMethod("CASH").paymentStatus("PAID")
                .cashReceived(request.cashReceived()).changeAmount(request.cashReceived().subtract(total))
                .note(trimToNull(request.note())).active(true).createdAt(now).build();
        statusService.initialize(order);
        order = orderRepository.save(order);
        UUID orderId = order.getId();

        for (int i = 0; i < evaluation.pricing().lines().size(); i++) {
            LineResult line = evaluation.pricing().lines().get(i);
            PosVariantRow row = evaluation.rows().get(line.input().variantId());
            itemRepository.save(OrderItem.builder().orderId(orderId).productId(row.productId())
                    .productVariantId(row.variantId()).productName(row.productName()).sku(row.sku())
                    .color(row.color()).size(row.size()).imageUrl(row.imageUrl())
                    .price(line.input().unitPrice()).quantity(line.input().quantity())
                    .discountAmount(line.discount()).total(line.total())
                    .promotionId(line.promotion() == null ? null : line.promotion().getId())
                    .promotionCode(line.promotion() == null ? null : line.promotion().getCode())
                    // Lệch vài micro giây để giữ đúng thứ tự dòng khi đọc lại theo created_at.
                    .createdAt(now.plusNanos(i * 1000L)).build());
        }

        pricer.recordApplied(evaluation.appliedPromotions(), orderId, customerId, now);

        // Khóa và trừ tồn theo thứ tự variant id để hai đơn đồng thời không khóa chéo nhau.
        evaluation.pricing().lines().stream().map(LineResult::input)
                .sorted(Comparator.comparing(LineInput::variantId))
                .forEach(line -> inventoryService.deductForOrder(store, line.variantId(), line.quantity(),
                        CHANNEL, orderId, actor));

        paymentRepository.save(Payment.builder().orderId(orderId).method("CASH").amount(total).status("SUCCESS")
                .paidAt(now).createdAt(now).build());
        statusService.recordCreated(order, OrderActor.user(actor), "Tạo đơn bán tại quầy");
        // Đơn tại quầy hoàn tất ngay khi thanh toán nên tích điểm luôn (không đi qua bước đổi trạng thái).
        loyalty.earnForCompletedOrder(order);
        return new OfflineOrderCreatedResponse(orderId, order.getOrderCode(), total, order.getCashReceived(),
                order.getChangeAmount(), now);
    }

    /**
     * Tính tiền giỏ hàng. Lỗi dữ liệu (biến thể/khách không tồn tại) ném ngay; vấn đề nghiệp vụ được
     * gom thành cảnh báo để màn hình quote hiển thị, còn tạo đơn sẽ chặn nếu có cảnh báo.
     *
     * @param lockPromotions khóa các khuyến mãi đã chọn trước khi đếm lượt dùng (khi tạo đơn)
     */
    private Evaluation evaluate(UUID store, OfflineCartRequest request, boolean lockPromotions) {
        List<String> warnings = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        CustomerContext customer = pricer.customer(request.customerId());

        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (OfflineCartRequest.Line line : request.lines())
            quantities.merge(line.variantId(), line.quantity(), Integer::sum);
        if (quantities.isEmpty()) warnings.add("Giỏ hàng đang trống");

        Map<UUID, PosVariantRow> rows = quantities.isEmpty() ? Map.of()
                : catalog.findByIds(store, quantities.keySet()).stream()
                .collect(Collectors.toMap(PosVariantRow::variantId, Function.identity()));
        List<LineInput> lineInputs = new ArrayList<>();
        Map<UUID, String> lineWarnings = new LinkedHashMap<>();
        quantities.forEach((variantId, quantity) -> {
            PosVariantRow row = rows.get(variantId);
            if (row == null) throw BusinessException.notFound("Biến thể sản phẩm không tồn tại");
            String warning = null;
            if (!row.sellable()) warning = "Sản phẩm " + row.sku() + " đã ngừng bán";
            else if (quantity > row.stock())
                warning = "Sản phẩm " + row.sku() + " chỉ còn " + row.stock() + " tại cửa hàng";
            if (warning != null) {
                lineWarnings.put(variantId, warning);
                warnings.add(warning);
            }
            lineInputs.add(new LineInput(variantId, row.productId(), row.unitPrice(), quantity));
        });

        OrderCartPricer.PricedCart cart = pricer.price(lineInputs, request.promotionCodesOrEmpty(), customer, lockPromotions);
        OrderPricingCalculator.Result pricing = cart.pricing();
        warnings.addAll(cart.warnings());
        notices.addAll(cart.notices());

        if (request.cashReceived() != null && request.cashReceived().compareTo(pricing.total()) < 0)
            warnings.add("Tiền khách đưa còn thiếu " + OrderCartPricer.vnd(pricing.total().subtract(request.cashReceived())));

        return new Evaluation(store, customer, rows, lineInputs, lineWarnings, cart, warnings, notices,
                request.cashReceived());
    }

    private OfflineQuoteResponse toResponse(Evaluation evaluation) {
        OrderPricingCalculator.Result pricing = evaluation.pricing();
        List<OfflineQuoteResponse.Line> lines = pricing.lines().stream().map(line -> {
            PosVariantRow row = evaluation.rows().get(line.input().variantId());
            return new OfflineQuoteResponse.Line(row.variantId(), row.productId(), row.productName(), row.sku(),
                    row.color(), row.size(), row.imageUrl(), line.input().unitPrice(), line.input().quantity(),
                    row.stock(), line.subtotal(), line.discount(), line.total(),
                    line.promotion() == null ? null : line.promotion().getCode(),
                    evaluation.lineWarnings().get(row.variantId()));
        }).toList();
        List<OfflineQuoteResponse.AppliedPromotion> promotions = evaluation.appliedPromotions().entrySet().stream()
                .map(e -> new OfflineQuoteResponse.AppliedPromotion(e.getKey().getId(), e.getKey().getCode(),
                        e.getKey().getName(), e.getKey().getPromotionType(), e.getValue()))
                .toList();
        BigDecimal cash = evaluation.cashReceived();
        BigDecimal change = cash == null || cash.compareTo(pricing.total()) < 0 ? null : cash.subtract(pricing.total());
        boolean canCreate = evaluation.warnings().isEmpty() && !lines.isEmpty() && change != null;
        return new OfflineQuoteResponse(evaluation.store(), evaluation.customer().customerId(), lines,
                pricing.subtotal(), pricing.productDiscount(), pricing.orderDiscount(), pricing.discountTotal(),
                pricing.total(), cash, change, promotions, List.copyOf(evaluation.warnings()),
                List.copyOf(evaluation.notices()), canCreate);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Evaluation(
            UUID store, CustomerContext customer, Map<UUID, PosVariantRow> rows, List<LineInput> lineInputs,
            Map<UUID, String> lineWarnings, OrderPricingCalculator.Result pricing, OrderCartPricer.PricedCart cart,
            List<String> warnings, List<String> notices, BigDecimal cashReceived) {
        Evaluation(UUID store, CustomerContext customer, Map<UUID, PosVariantRow> rows, List<LineInput> lineInputs,
                   Map<UUID, String> lineWarnings, OrderCartPricer.PricedCart cart, List<String> warnings,
                   List<String> notices, BigDecimal cashReceived) {
            this(store, customer, rows, lineInputs, lineWarnings, cart.pricing(), cart, warnings, notices, cashReceived);
        }

        Map<Promotion, BigDecimal> appliedPromotions() {
            return cart.applied();
        }
    }
}
