package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.order.PaymentStartResponse;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.service.OrderPaymentFacade;
import com.fashionsystem.fashion_system.service.OrderPaymentService;
import jakarta.servlet.http.HttpServletRequest;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.order.CustomerCancelRequest;
import com.fashionsystem.fashion_system.dto.order.CustomerOrderResponse;
import com.fashionsystem.fashion_system.dto.order.OnlineCheckoutRequest;
import com.fashionsystem.fashion_system.dto.order.OnlineOrderPlacedResponse;
import com.fashionsystem.fashion_system.dto.order.OnlineQuoteResponse;
import com.fashionsystem.fashion_system.dto.order.VoucherOptionResponse;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.OnlineOrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Đơn online của khách: tính tiền, đặt hàng, thanh toán qua cổng (VNPay/MoMo), xem và hủy. Giao diện phía khách
 * sẽ nối sau.
 */
@RestController
@RequestMapping("/api/customers/me/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {
    private final OnlineOrderService service;
    private final OrderPaymentFacade payments;
    private final OrderPaymentService paymentService;

    /** Tính tiền và xem trước đơn sẽ giao từ cửa hàng nào (có tách đơn hay không). */
    @PostMapping("/quote")
    public OnlineQuoteResponse quote(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                     @Valid @RequestBody OnlineCheckoutRequest request) {
        return service.quote(customer.customerId(), request);
    }

    @PostMapping("/vouchers")
    public List<VoucherOptionResponse> vouchers(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                                @Valid @RequestBody OnlineCheckoutRequest request) {
        return service.vouchers(customer.customerId(), request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OnlineOrderPlacedResponse place(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                           @Valid @RequestBody OnlineCheckoutRequest request,
                                           HttpServletRequest http) {
        try (var ignored = customerScope(customer)) {
            OnlineOrderPlacedResponse placed = service.place(customer.customerId(), request);
            if (!"AWAITING_PAYMENT".equals(placed.paymentStatus())) return placed;
            // Đơn đã được giữ hàng; tạo link lỗi thì khách vẫn có đơn và thử lại / đổi sang COD.
            try {
                return placed.withPayment(payments.start(customer.customerId(), placed.id(), null, clientIp(http)), null);
            } catch (BusinessException failure) {
                return placed.withPayment(null, failure.getReason());
            }
        }
    }

    /**
     * Tạo link thanh toán cho đơn đang chờ thanh toán (lần đầu lỗi, khách trả thất bại, hoặc đổi cổng).
     * Body tùy chọn {@code {"method": "VNPAY" | "MOMO"}}.
     */
    @PostMapping("/{id}/payments")
    public PaymentStartResponse pay(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID id,
                                    @RequestBody(required = false) java.util.Map<String, String> body,
                                    HttpServletRequest http) {
        String method = body == null ? null : body.get("method");
        if (method != null && !method.isBlank() && !method.trim().toUpperCase().matches("VNPAY|MOMO"))
            throw BusinessException.badRequest("Phương thức thanh toán không hợp lệ");
        try (var ignored = customerScope(customer)) {
            return payments.start(customer.customerId(), id, method, clientIp(http));
        }
    }

    /** Khách đổi sang thanh toán khi nhận hàng khi chưa trả được qua cổng. */
    @PostMapping("/{id}/payment-method/cod")
    public CustomerOrderResponse switchToCod(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID id) {
        try (var ignored = customerScope(customer)) {
            return paymentService.switchToCod(customer.customerId(), id);
        }
    }

    @GetMapping
    public Page<CustomerOrderResponse> list(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                            @RequestParam(required = false) com.fashionsystem.fashion_system.entity.OrderStatus status,
                                            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                                            Pageable pageable) {
        return service.myOrders(customer.customerId(), status, pageable);
    }

    @GetMapping("/cancel-reasons")
    public List<CustomerCancelRequest.ReasonOption> cancelReasons() {
        return service.cancelReasons();
    }

    @GetMapping("/{id}")
    public CustomerOrderResponse detail(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID id) {
        return service.myOrder(customer.customerId(), id);
    }

    /** Khách hủy khi cửa hàng chưa xác nhận và đơn chưa thanh toán; lý do không bắt buộc. */
    @PostMapping("/{id}/cancel")
    public CustomerOrderResponse cancel(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID id,
                                        @Valid @RequestBody(required = false) CustomerCancelRequest request) {
        try (var ignored = customerScope(customer)) {
            return service.cancel(customer.customerId(), id, request);
        }
    }

    /** IP của khách gửi cho cổng (vnp_IpAddr); sau proxy lấy địa chỉ đầu tiên của X-Forwarded-For. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    /**
     * Nhật ký audit nghiệp vụ chỉ có tác nhân nhân viên hoặc hệ thống; thao tác tự phục vụ của khách được ghi
     * như một tác vụ hệ thống mang mã khách. Lịch sử trạng thái đơn vẫn ghi tác nhân là khách hàng.
     */
    private static BusinessAuditContextFactory.JobScope customerScope(AuthenticatedCustomer customer) {
        return BusinessAuditContextFactory.systemJob("CUSTOMER_ORDER:" + customer.customerId());
    }
}
