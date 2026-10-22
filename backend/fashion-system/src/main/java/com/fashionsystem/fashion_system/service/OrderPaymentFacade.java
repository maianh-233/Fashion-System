package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.PaymentStartResponse;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payment.InvalidPaymentCallbackException;
import com.fashionsystem.fashion_system.payment.PaymentGateway;
import com.fashionsystem.fashion_system.payment.PaymentGatewayRegistry;
import com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput;
import com.fashionsystem.fashion_system.payment.PaymentModels.GatewayCallback;
import com.fashionsystem.fashion_system.payment.PaymentModels.Outcome;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentLink;
import com.fashionsystem.fashion_system.payment.PaymentProperties;
import com.fashionsystem.fashion_system.service.OrderPaymentService.CallbackResult;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Điều phối thanh toán qua cổng: các bước ghi dữ liệu ở {@link OrderPaymentService} (mỗi bước một transaction),
 * còn gọi HTTP tới cổng chạy ngoài transaction. Người gọi phải mở ngữ cảnh audit (khách / webhook / job).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentFacade {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final OrderPaymentService payments;
    private final PaymentGatewayRegistry gateways;
    private final PaymentProperties properties;

    /** Tạo (hoặc dùng lại) link thanh toán cho đơn đang chờ thanh toán. */
    public PaymentStartResponse start(UUID customerId, UUID orderId, String method, String clientIp) {
        OrderPaymentService.Prepared prepared = payments.prepare(customerId, orderId, method, clientIp);
        if (prepared.existing() != null) return prepared.existing();
        PaymentGateway gateway = gateways.byCode(prepared.gatewayCode()).orElseThrow();
        PaymentLink link;
        try {
            link = gateway.createPayment(prepared.request());
        } catch (RuntimeException failure) {
            log.warn("{} create payment {} failed: {}", gateway.code(), prepared.request().reference(), failure.getMessage());
            payments.markLinkFailed(prepared.paymentId(), failure.getMessage());
            throw BusinessException.serviceUnavailable("Chưa tạo được link thanh toán " + gateway.code()
                    + ", vui lòng thử lại hoặc đổi sang thanh toán khi nhận hàng", failure);
        }
        return payments.attachLink(prepared.paymentId(), link);
    }

    /** IPN của cổng; {@code empty} khi không có cổng mang mã này. */
    public Optional<CallbackResult> handleIpn(String gatewayCode, CallbackInput input) {
        Optional<PaymentGateway> gateway = gateways.byCode(gatewayCode);
        if (gateway.isEmpty()) return Optional.empty();
        GatewayCallback callback;
        try {
            callback = gateway.get().parseCallback(input);
        } catch (InvalidPaymentCallbackException invalid) {
            log.warn("Rejected {} IPN: {}", gateway.get().code(), invalid.getMessage());
            payments.logRejected(gateway.get().code(), raw(input), invalid.getMessage());
            return Optional.of(CallbackResult.INVALID_SIGNATURE);
        }
        return Optional.of(payments.applyCallback(gateway.get().code(), callback));
    }

    /**
     * Khách quay lại từ cổng: chỉ xác minh chữ ký để hiển thị kết quả (cập nhật đơn là việc của IPN) rồi chuyển
     * tới trang kết quả của frontend. Trả {@code empty} khi chưa cấu hình PAYMENT_RESULT_URL.
     */
    public Map<String, String> returnResult(String gatewayCode, CallbackInput input) {
        Map<String, String> result = new LinkedHashMap<>();
        PaymentGateway gateway = gateways.byCode(gatewayCode).orElse(null);
        GatewayCallback callback = null;
        try {
            if (gateway != null) callback = gateway.parseCallback(input);
        } catch (InvalidPaymentCallbackException invalid) {
            log.warn("Rejected {} return: {}", gatewayCode, invalid.getMessage());
        }
        if (callback == null) {
            result.put("status", "invalid");
            return result;
        }
        result.put("status", callback.outcome() == Outcome.SUCCESS ? "success"
                : callback.outcome() == Outcome.PENDING ? "pending" : "failed");
        payments.returnView(callback.reference()).ifPresent(view -> {
            result.put("orderId", view.orderId().toString());
            result.put("orderCode", view.orderCode());
        });
        if (callback.outcome() != Outcome.SUCCESS && callback.message() != null) result.put("message", callback.message());
        return result;
    }

    /** URL trang kết quả của frontend kèm tham số; null nếu chưa cấu hình. */
    public String resultRedirect(Map<String, String> result) {
        String base = properties.resultUrl();
        if (base == null || base.isBlank()) return null;
        StringBuilder url = new StringBuilder(base.trim()).append(base.contains("?") ? '&' : '?');
        result.forEach((key, value) -> url.append(key).append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append('&'));
        return url.substring(0, url.length() - 1);
    }

    private static String raw(CallbackInput input) {
        if (input.body() != null && !input.body().isBlank()) {
            try {
                JSON.readTree(input.body());
                return input.body();
            } catch (RuntimeException notJson) {
                return JSON.writeValueAsString(Map.of("body", input.body()));
            }
        }
        return JSON.writeValueAsString(input.params() == null ? Map.of() : input.params());
    }
}
