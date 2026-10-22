package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.CartDtos;
import com.fashionsystem.fashion_system.payment.MomoGateway;
import com.fashionsystem.fashion_system.payment.PaymentGateway;
import com.fashionsystem.fashion_system.payment.PaymentGatewayRegistry;
import com.fashionsystem.fashion_system.payment.VnpayGateway;
import com.fashionsystem.fashion_system.service.CustomerCartService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Phần công khai của giỏ hàng / thanh toán (khách chưa đăng nhập cũng gọi được). */
@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
public class StorefrontCheckoutController {
    private final CustomerCartService cart;
    private final PaymentGatewayRegistry gateways;

    public record PaymentMethod(String code, String label, boolean available) {
    }

    /** Dựng giỏ trên trình duyệt (chưa đăng nhập) với giá và tồn kho hiện tại; không lưu. */
    @PostMapping("/cart/preview")
    public CartDtos.Cart preview(@Valid @RequestBody CartDtos.GuestCart request) {
        return cart.preview(request);
    }

    /** Phương thức thanh toán; cổng chưa cấu hình khóa thì {@code available = false}. */
    @GetMapping("/payment-methods")
    public List<PaymentMethod> paymentMethods() {
        return List.of(new PaymentMethod("COD", "Thanh toán khi nhận hàng", true),
                gateway(VnpayGateway.CODE, "VNPay"), gateway(MomoGateway.CODE, "Ví MoMo"));
    }

    private PaymentMethod gateway(String code, String label) {
        return new PaymentMethod(code, label, gateways.byCode(code).map(PaymentGateway::configured).orElse(false));
    }
}
