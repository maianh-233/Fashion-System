package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.payment.MomoGateway;
import com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput;
import com.fashionsystem.fashion_system.payment.VnpayGateway;
import com.fashionsystem.fashion_system.service.OrderPaymentFacade;
import com.fashionsystem.fashion_system.service.OrderPaymentService.CallbackResult;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Callback của cổng thanh toán (không cần đăng nhập; tính xác thực dựa vào chữ ký của cổng).
 *
 * <ul>
 *   <li>VNPay IPN: GET, trả JSON {@code {"RspCode","Message"}}. 00/02 thì VNPay dừng; 01/04/97/99 thì VNPay gửi lại.</li>
 *   <li>MoMo IPN: POST JSON, trả HTTP 204 trong 15 giây.</li>
 *   <li>Return URL (cả hai): chỉ hiển thị kết quả rồi chuyển tới trang kết quả của frontend.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final OrderPaymentFacade payments;

    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        try (var ignored = BusinessAuditContextFactory.systemJob("PAYMENT_IPN:" + VnpayGateway.CODE)) {
            CallbackResult result = payments.handleIpn(VnpayGateway.CODE, new CallbackInput(params, null)).orElseThrow();
            return switch (result) {
                case CONFIRMED -> vnpay("00", "Confirm Success");
                case ALREADY_PROCESSED -> vnpay("02", "Order already confirmed");
                case NOT_FOUND -> vnpay("01", "Order not found");
                case AMOUNT_MISMATCH -> vnpay("04", "invalid amount");
                case INVALID_SIGNATURE -> vnpay("97", "Invalid signature");
            };
        } catch (RuntimeException failure) {
            log.error("VNPay IPN failed", failure);
            return vnpay("99", "Unknow error");
        }
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody(required = false) String body) {
        try (var ignored = BusinessAuditContextFactory.systemJob("PAYMENT_IPN:" + MomoGateway.CODE)) {
            CallbackResult result = payments.handleIpn(MomoGateway.CODE, new CallbackInput(Map.of(), body)).orElseThrow();
            return switch (result) {
                case CONFIRMED, ALREADY_PROCESSED -> ResponseEntity.noContent().build();
                case INVALID_SIGNATURE -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                case AMOUNT_MISMATCH -> ResponseEntity.badRequest().build();
            };
        }
    }

    @GetMapping("/{gateway}/return")
    public ResponseEntity<Map<String, String>> returnPage(@PathVariable String gateway,
                                                          @RequestParam Map<String, String> params) {
        Map<String, String> result = payments.returnResult(gateway, new CallbackInput(params, null));
        String redirect = payments.resultRedirect(result);
        if (redirect == null) return ResponseEntity.ok(result);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirect)).build();
    }

    private static Map<String, String> vnpay(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
