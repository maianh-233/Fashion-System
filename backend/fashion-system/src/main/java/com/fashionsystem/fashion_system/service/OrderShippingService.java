package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.OrderDetailResponse;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderAddress;
import com.fashionsystem.fashion_system.entity.OrderItem;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.Shipment;
import com.fashionsystem.fashion_system.entity.ShipmentStatus;
import com.fashionsystem.fashion_system.entity.ShippingWebhookEvent;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.OrderAddressRepository;
import com.fashionsystem.fashion_system.repository.OrderItemRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.ShipmentRepository;
import com.fashionsystem.fashion_system.repository.ShippingWebhookEventRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.shipping.ShippingException;
import com.fashionsystem.fashion_system.shipping.ShippingModels;
import com.fashionsystem.fashion_system.shipping.ShippingModels.WebhookEvent;
import com.fashionsystem.fashion_system.shipping.ShippingProvider;
import com.fashionsystem.fashion_system.shipping.ShippingProviderRegistry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giao đơn cho bên vận chuyển và xử lý trạng thái hãng báo về. Chỉ phụ thuộc {@link ShippingProvider}.
 *
 * <ul>
 *   <li>Mỗi lần tạo chuyến là một dòng shipments (attempt 1, 2, ...), không ghi đè chuyến cũ.</li>
 *   <li>Webhook: (hãng, eventId) duy nhất nên hãng gửi lại không bị xử lý hai lần; webhook cũ hơn lần cập nhật
 *       đã ghi nhận bị bỏ qua vì hãng có thể gửi sai thứ tự.</li>
 *   <li>Hãng không tìm được tài xế / từ chối / hủy khi chưa lấy hàng: chuyến FAILED, đơn quay về CONFIRMED và
 *       nhân viên xử lý được báo để liên hệ khách (chờ → gửi lại yêu cầu giao, hủy → hủy đơn).</li>
 *   <li>Hãng hủy sau khi đã lấy hàng: đơn sang DELIVERY_FAILED, chờ nhân viên xác nhận nhận hàng hoàn.</li>
 * </ul>
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("ORDER")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderShippingService {
    private final OrderAccessService access;
    private final OrderStateMachine stateMachine;
    private final OrderStatusService statusService;
    private final OrderAdminService adminService;
    private final ShippingProviderRegistry providers;
    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderAddressRepository addressRepository;
    private final StoreRepository storeRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShippingWebhookEventRepository webhookEvents;
    private final ApplicationEventPublisher events;
    private final CodCollectionService codCollection;

    /**
     * CONFIRMED → HANDED_TO_SHIPPER: báo giá và đặt chuyến với hãng, lưu chuyến mới. Dùng cho cả lần đầu và
     * "gửi lại yêu cầu giao hàng" sau khi chuyến trước thất bại.
     *
     * <p>Gọi hãng khi đang giữ khóa dòng đơn để hai nhân viên không cùng tạo hai chuyến. Nếu lưu dữ liệu thất bại
     * sau khi hãng đã nhận chuyến thì chuyến bên hãng được hủy lại.</p>
     */
    @Transactional
    public OrderDetailResponse handover(UUID actor, UUID orderId) {
        Order order = access.requireOrderForUpdate(actor, orderId, OrderPermissions.HANDOVER_SHIPPING);
        if (order.getOrderType() != OrderType.ONLINE) throw BusinessException.invalidState("Chỉ giao vận chuyển cho đơn online");
        List<Shipment> previous = shipmentRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId);
        if (previous.stream().anyMatch(s -> s.getStatus().isActive()))
            throw BusinessException.conflict("Đơn đang có chuyến giao chưa kết thúc");
        stateMachine.requireTransition(order.getStatus(), OrderStatus.HANDED_TO_SHIPPER, OrderActorType.USER);

        ShippingProvider provider = provider();
        ShippingModels.ShipmentRequest request = shipmentRequest(order);
        ShippingModels.CreatedShipment created;
        ShippingModels.Quote quote;
        try {
            quote = provider.quote(request);
            created = provider.createShipment(request, quote);
        } catch (ShippingException failure) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, failure.getMessage(), failure);
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            shipmentRepository.save(Shipment.builder().orderId(orderId).attemptNo(previous.size() + 1)
                    .shippingProvider(provider.code()).providerOrderId(created.providerOrderId())
                    .quotationId(quote.quotationId()).trackingCode(created.providerOrderId())
                    .trackingUrl(created.trackingUrl()).fee(created.fee()).currency(created.currency())
                    .status(created.status()).providerStatus(created.providerStatus()).createdBy(actor)
                    .rawResponse(created.rawJson()).createdAt(now).updatedAt(now).build());
            if (order.getHandlerUserId() == null) order.setHandlerUserId(actor);
            statusService.transition(order, OrderStatus.HANDED_TO_SHIPPER, OrderActor.user(actor), null);
        } catch (RuntimeException failure) {
            cancelQuietly(provider, created.providerOrderId());
            throw failure;
        }
        return adminService.detail(actor, orderId);
    }

    /** Kết quả xử lý webhook, lưu vào shipping_webhook_events.result. */
    public static final String APPLIED = "APPLIED", DUPLICATE = "DUPLICATE", STALE = "IGNORED_STALE",
            UNKNOWN_SHIPMENT = "UNKNOWN_SHIPMENT", UNKNOWN_STATUS = "UNKNOWN_STATUS", IGNORED_TYPE = "IGNORED_TYPE",
            INACTIVE_SHIPMENT = "IGNORED_INACTIVE", STATE_MISMATCH = "ORDER_STATE_MISMATCH";

    /**
     * Webhook đã xác minh chữ ký. Ghi sự kiện trước (unique theo hãng + eventId) để lần gửi lại hoặc gửi đồng thời
     * của cùng sự kiện không áp dụng hai lần.
     */
    @Transactional
    public String applyWebhook(String providerCode, WebhookEvent event) {
        if (webhookEvents.existsByProviderAndEventId(providerCode, event.eventId())) return DUPLICATE;
        ShippingWebhookEvent record = webhookEvents.saveAndFlush(ShippingWebhookEvent.builder().provider(providerCode)
                .eventId(event.eventId()).eventType(event.rawType() == null ? "UNKNOWN" : event.rawType())
                .providerOrderId(event.providerOrderId()).payload(event.rawJson()).result("PROCESSING")
                .receivedAt(LocalDateTime.now()).build());
        String result = switch (event.type()) {
            case STATUS_CHANGED -> statusChanged(providerCode, event);
            case DRIVER_ASSIGNED -> driverAssigned(providerCode, event);
            case ORDER_REPLACED -> replaced(providerCode, event);
            case OTHER -> IGNORED_TYPE;
        };
        if (!APPLIED.equals(result))
            log.info("Shipping webhook {} {} for {}: {}", providerCode, event.rawType(), event.providerOrderId(), result);
        record.setResult(result);
        webhookEvents.save(record);
        return result;
    }

    private String statusChanged(String providerCode, WebhookEvent event) {
        Shipment shipment = shipmentRepository.findByProviderOrderForUpdate(providerCode, event.providerOrderId()).orElse(null);
        if (shipment == null) return UNKNOWN_SHIPMENT;
        if (event.status() == null) return UNKNOWN_STATUS;
        if (shipment.getStatus() == ShipmentStatus.CANCELLED || shipment.getStatus() == ShipmentStatus.REPLACED)
            return INACTIVE_SHIPMENT;
        LocalDateTime occurredAt = toLocal(event);
        if (shipment.getProviderUpdatedAt() != null && occurredAt != null && occurredAt.isBefore(shipment.getProviderUpdatedAt()))
            return STALE;

        LocalDateTime now = LocalDateTime.now();
        shipment.setProviderStatus(event.providerStatus());
        if (occurredAt != null) shipment.setProviderUpdatedAt(occurredAt);
        if (event.trackingUrl() != null) shipment.setTrackingUrl(event.trackingUrl());
        shipment.setUpdatedAt(now);
        Order order = orderRepository.findByIdForUpdate(shipment.getOrderId()).orElseThrow();
        String result = APPLIED;
        switch (event.status()) {
            case ASSIGNING_DRIVER, DRIVER_ASSIGNED -> shipment.setStatus(event.status());
            case PICKED_UP -> {
                shipment.setStatus(ShipmentStatus.PICKED_UP);
                shipment.setShippedAt(now);
                result = moveOrder(order, OrderStatus.SHIPPING, null);
            }
            case COMPLETED -> {
                shipment.setStatus(ShipmentStatus.COMPLETED);
                shipment.setDeliveredAt(now);
                result = moveOrder(order, OrderStatus.COMPLETED, null);
                // Đơn COD: tài xế đã thu tiền khi giao thành công.
                if (order.getStatus() == OrderStatus.COMPLETED)
                    codCollection.recordCollected(order, shipment.getProviderOrderId());
            }
            case FAILED -> {
                shipment.setStatus(ShipmentStatus.FAILED);
                shipment.setFailureReason(event.reason());
                // Hàng đã ở chỗ tài xế: chờ hoàn về cửa hàng. Chưa lấy hàng: đơn quay lại để nhân viên xử lý tiếp.
                boolean goodsWithDriver = "PICKED_UP".equals(event.previousProviderStatus())
                        || order.getStatus() == OrderStatus.SHIPPING;
                result = moveOrder(order, goodsWithDriver ? OrderStatus.DELIVERY_FAILED : OrderStatus.CONFIRMED, event.reason());
                events.publishEvent(new OrderEvents.ShipmentFailed(order.getId(), order.getOrderCode(), order.getStoreId(),
                        order.getHandlerUserId(), shipment.getAttemptNo(), event.reason(), goodsWithDriver));
            }
            default -> result = UNKNOWN_STATUS;
        }
        shipmentRepository.save(shipment);
        return result;
    }

    private String driverAssigned(String providerCode, WebhookEvent event) {
        Shipment shipment = shipmentRepository.findByProviderOrderForUpdate(providerCode, event.providerOrderId()).orElse(null);
        if (shipment == null) return UNKNOWN_SHIPMENT;
        if (event.driver() != null) {
            shipment.setDriverName(event.driver().name());
            shipment.setDriverPhone(event.driver().phone());
            shipment.setDriverPlate(event.driver().plateNumber());
        }
        if (shipment.getStatus() == ShipmentStatus.ASSIGNING_DRIVER) shipment.setStatus(ShipmentStatus.DRIVER_ASSIGNED);
        shipment.setUpdatedAt(LocalDateTime.now());
        shipmentRepository.save(shipment);
        return APPLIED;
    }

    /**
     * Hãng hủy chuyến cũ và tạo chuyến thay thế (cancel-and-clone, gửi CANCELED rồi ORDER_REPLACED): ghi chuyến mới
     * và đưa đơn về lại HANDED_TO_SHIPPER nếu đơn đang chờ vì lần hủy đó.
     */
    private String replaced(String providerCode, WebhookEvent event) {
        Shipment old = shipmentRepository.findByProviderOrderForUpdate(providerCode, event.previousProviderOrderId()).orElse(null);
        if (old == null) return UNKNOWN_SHIPMENT;
        Order order = orderRepository.findByIdForUpdate(old.getOrderId()).orElseThrow();
        List<Shipment> all = shipmentRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId());
        if (all.stream().anyMatch(s -> s.getStatus().isActive() && !s.getId().equals(old.getId())))
            return STATE_MISMATCH; // Nhân viên đã tạo chuyến khác trong lúc chờ; không nhận chuyến thay thế.
        LocalDateTime now = LocalDateTime.now();
        Shipment replacement = shipmentRepository.save(Shipment.builder().orderId(order.getId())
                .attemptNo(all.size() + 1).shippingProvider(providerCode).providerOrderId(event.providerOrderId())
                .quotationId(old.getQuotationId()).trackingCode(event.providerOrderId()).trackingUrl(old.getTrackingUrl())
                .fee(old.getFee()).currency(old.getCurrency()).status(ShipmentStatus.ASSIGNING_DRIVER)
                .providerStatus("ASSIGNING_DRIVER").providerUpdatedAt(toLocal(event)).createdBy(old.getCreatedBy())
                .rawResponse(event.rawJson()).createdAt(now).updatedAt(now).build());
        old.setStatus(ShipmentStatus.REPLACED);
        old.setReplacedByShipmentId(replacement.getId());
        old.setUpdatedAt(now);
        shipmentRepository.save(old);
        return order.getStatus() == OrderStatus.CONFIRMED
                ? moveOrder(order, OrderStatus.HANDED_TO_SHIPPER, "Đơn vị vận chuyển thay thế chuyến giao")
                : APPLIED;
    }

    /** Chuyển đơn theo báo cáo của hãng nếu bảng trạng thái cho phép; không thì chỉ ghi nhận chuyến. */
    private String moveOrder(Order order, OrderStatus target, String reason) {
        if (order.getStatus() == target) return APPLIED;
        if (!stateMachine.canTransition(order.getStatus(), target, OrderActorType.SHIPPER)) {
            log.warn("Shipping update cannot move order {} from {} to {}", order.getOrderCode(), order.getStatus(), target);
            return STATE_MISMATCH;
        }
        statusService.transition(order, target, OrderActor.shipper(), reason);
        return APPLIED;
    }

    private ShippingModels.ShipmentRequest shipmentRequest(Order order) {
        Store store = storeRepository.findById(order.getStoreId())
                .orElseThrow(() -> BusinessException.notFound("Cửa hàng không tồn tại"));
        if (store.getLatitude() == null || store.getLongitude() == null || isBlank(store.getAddress()) || isBlank(store.getPhone()))
            throw BusinessException.invalidState("Cửa hàng chưa có đủ địa chỉ, tọa độ và số điện thoại để gọi vận chuyển");
        // Phần của đơn bị tách có bản sao địa chỉ riêng; đơn cũ có thể chỉ lưu ở đơn tổng.
        OrderAddress address = addressRepository.findByOrderId(order.getId())
                .or(() -> order.getParentOrderId() == null ? java.util.Optional.empty()
                        : addressRepository.findByOrderId(order.getParentOrderId()))
                .orElseThrow(() -> BusinessException.invalidState("Đơn chưa có địa chỉ giao hàng"));
        if (address.getLatitude() == null || address.getLongitude() == null)
            throw BusinessException.invalidState("Địa chỉ giao hàng chưa có tọa độ");
        int quantity = itemRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .mapToInt(OrderItem::getQuantity).sum();
        String dropoffAddress = Stream.of(address.getAddressLine(), address.getWard(), address.getDistrict(), address.getProvince())
                .filter(Objects::nonNull).filter(v -> !v.isBlank()).collect(Collectors.joining(", "));
        String remarks = "Đơn " + order.getOrderCode() + (isBlank(order.getNote()) ? "" : " · " + order.getNote().trim());
        return new ShippingModels.ShipmentRequest(order.getOrderCode(),
                new ShippingModels.Stop(store.getName(), store.getPhone(), store.getAddress(),
                        store.getLatitude().doubleValue(), store.getLongitude().doubleValue()),
                new ShippingModels.Stop(address.getReceiverName(), address.getReceiverPhone(), dropoffAddress,
                        address.getLatitude().doubleValue(), address.getLongitude().doubleValue()),
                quantity, remarks);
    }

    private ShippingProvider provider() {
        try {
            return providers.current();
        } catch (ShippingException notConfigured) {
            throw BusinessException.invalidState(notConfigured.getMessage());
        }
    }

    private static void cancelQuietly(ShippingProvider provider, String providerOrderId) {
        try {
            provider.cancelShipment(providerOrderId);
        } catch (RuntimeException failure) {
            log.error("Could not cancel orphan shipment {} at {}", providerOrderId, provider.code(), failure);
        }
    }

    private static LocalDateTime toLocal(WebhookEvent event) {
        // Cột timestamp lưu UTC (hibernate.jdbc.time_zone=UTC) nhưng ứng dụng làm việc theo giờ VN.
        return event.occurredAt() == null ? null
                : LocalDateTime.ofInstant(event.occurredAt(), PromotionService.ZONE);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
