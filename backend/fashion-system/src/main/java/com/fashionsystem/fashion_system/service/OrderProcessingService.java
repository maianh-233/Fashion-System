package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.OrderActionRequests;
import com.fashionsystem.fashion_system.dto.order.OrderDetailResponse;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderCustomerContact;
import com.fashionsystem.fashion_system.entity.OrderCustomerContact.DeliveryStatus;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderCustomerContactRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Thao tác của nhân viên trên đơn online: xác nhận, hủy, xác nhận nhận hàng hoàn và liên hệ khách. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("ORDER")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderProcessingService {
    private final OrderAccessService access;
    private final OrderStatusService statusService;
    private final OnlineOrderService onlineOrders;
    private final OrderAdminService adminService;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderCustomerContactRepository contactRepository;
    private final UserRepository userRepository;
    private final com.fashionsystem.fashion_system.repository.PaymentRepository paymentRepository;
    private final com.fashionsystem.fashion_system.repository.RefundRepository refundRepository;
    private final OrderAmountCalculator amounts;
    private final ApplicationEventPublisher events;

    /** PENDING → CONFIRMED; nhân viên xác nhận trở thành người xử lý đơn. */
    @Transactional
    public OrderDetailResponse confirm(UUID actor, UUID orderId) {
        Order order = access.requireOrderForUpdate(actor, orderId, OrderPermissions.CONFIRM);
        if (order.getOrderType() != OrderType.ONLINE) throw BusinessException.invalidState("Chỉ xác nhận đơn online");
        order.setHandlerUserId(actor);
        statusService.transition(order, OrderStatus.CONFIRMED, OrderActor.user(actor), null);
        return adminService.detail(actor, orderId);
    }

    /**
     * Hủy khi đơn còn PENDING/CONFIRMED (bảng chuyển trạng thái chặn từ khi đã giao vận chuyển), lý do bắt buộc,
     * chặn đơn đã thanh toán qua cổng. Tồn kho được cộng lại; voucher không được hoàn.
     */
    @Transactional
    public OrderDetailResponse cancel(UUID actor, UUID orderId, String reason) {
        Order order = access.requireOrderForUpdate(actor, orderId, OrderPermissions.CANCEL);
        boolean gatewayPaid = adminService.isGatewayPaid(order);
        if (gatewayPaid && !adminService.hasFailedDelivery(order))
            throw BusinessException.invalidState("Đơn đã thanh toán qua cổng thanh toán nên không thể hủy");
        if (order.getHandlerUserId() == null) order.setHandlerUserId(actor);
        statusService.transition(order, OrderStatus.CANCELLED, OrderActor.user(actor), reason);
        onlineOrders.restock(order, actor);
        if (gatewayPaid) requestRefund(order, actor, reason);
        return adminService.detail(actor, orderId);
    }

    /**
     * Đơn đã thanh toán qua cổng bị hủy sau khi giao hàng thất bại: tạo yêu cầu hoàn tiền PENDING để kế toán
     * xử lý thủ công. Số tiền là phần khách đã trả cho đơn cửa hàng này (xem {@link OrderAmountCalculator}).
     */
    private void requestRefund(Order order, UUID actor, String reason) {
        UUID paymentOwner = order.getParentOrderId() == null ? order.getId() : order.getParentOrderId();
        var payment = paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(paymentOwner).stream()
                .filter(p -> "SUCCESS".equals(p.getStatus())).reduce((first, second) -> second)
                .orElseThrow(() -> BusinessException.invalidState("Không tìm thấy giao dịch thanh toán thành công để hoàn tiền"));
        refundRepository.save(Refund.builder().paymentId(payment.getId()).orderId(order.getId())
                .amount(amounts.amountDue(order)).status("PENDING").requestedBy(actor).requestedAt(LocalDateTime.now())
                .reason("Hủy đơn " + order.getOrderCode() + " sau khi giao hàng thất bại: " + reason).build());
    }

    /** DELIVERY_FAILED → RETURNED khi hàng hoàn về cửa hàng; chỉ người xử lý đơn (hoặc quyền thay thế). */
    @Transactional
    public OrderDetailResponse confirmReturn(UUID actor, UUID orderId) {
        Order order = access.requireOrderForUpdate(actor, orderId, OrderPermissions.VIEW);
        access.requireReturnConfirmer(actor, order);
        statusService.transition(order, OrderStatus.RETURNED, OrderActor.user(actor), null);
        onlineOrders.restock(order, actor);
        return adminService.detail(actor, orderId);
    }

    /** Lưu lần liên hệ rồi gửi email/thông báo sau khi commit (xem {@link OrderContactDeliveryListener}). */
    @Transactional
    public OrderActionRequests.ContactView contact(UUID actor, UUID orderId, OrderActionRequests.Contact request) {
        if (!request.viaEmail() && !request.viaNotification())
            throw BusinessException.badRequest("Vui lòng chọn ít nhất một kênh liên hệ");
        Order order = access.requireOrder(actor, orderId, OrderPermissions.CONTACT_CUSTOMER);
        UUID customerId = order.getCustomerId();
        if (customerId == null) throw BusinessException.invalidState("Đơn của khách vãng lai, không có thông tin liên hệ");
        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        String email = customer.getEmail() == null || customer.getEmail().isBlank() ? null : customer.getEmail().trim();
        if (request.viaEmail() && email == null) throw BusinessException.invalidState("Khách chưa có email");
        OrderCustomerContact contact = contactRepository.save(OrderCustomerContact.builder()
                .orderId(orderId).customerId(customerId).sentBy(actor)
                .subject(request.subject().trim()).content(request.content().trim())
                .viaEmail(request.viaEmail()).viaNotification(request.viaNotification())
                .emailTo(request.viaEmail() ? email : null)
                .emailStatus(request.viaEmail() ? DeliveryStatus.PENDING : DeliveryStatus.SKIPPED)
                .notificationStatus(request.viaNotification() ? DeliveryStatus.PENDING : DeliveryStatus.SKIPPED)
                .createdAt(LocalDateTime.now()).build());
        events.publishEvent(new OrderEvents.CustomerContactRequested(contact.getId()));
        return view(contact, names(List.of(contact)));
    }

    public List<OrderActionRequests.ContactView> contacts(UUID actor, UUID orderId) {
        access.requireOrder(actor, orderId, OrderPermissions.VIEW);
        List<OrderCustomerContact> contacts = contactRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId);
        Map<UUID, String> names = names(contacts);
        return contacts.stream().map(c -> view(c, names)).toList();
    }

    private Map<UUID, String> names(List<OrderCustomerContact> contacts) {
        return userRepository.findAllById(contacts.stream().map(OrderCustomerContact::getSentBy).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId,
                        u -> u.getFullName() == null || u.getFullName().isBlank() ? u.getUsername() : u.getFullName()));
    }

    private static OrderActionRequests.ContactView view(OrderCustomerContact c, Map<UUID, String> names) {
        return new OrderActionRequests.ContactView(c.getId(), c.getSubject(), c.getContent(), c.isViaEmail(),
                c.isViaNotification(), c.getEmailTo(), c.getEmailStatus(), c.getEmailError(), c.getNotificationStatus(),
                names.getOrDefault(c.getSentBy(), "Nhân viên"), c.getCreatedAt());
    }
}
