package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.service.OrderShippingService;
import com.fashionsystem.fashion_system.shipping.InvalidWebhookException;
import com.fashionsystem.fashion_system.shipping.ShippingModels;
import com.fashionsystem.fashion_system.shipping.ShippingProperties;
import com.fashionsystem.fashion_system.shipping.ShippingProvider;
import com.fashionsystem.fashion_system.shipping.ShippingProviderRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook của hãng vận chuyển (không cần đăng nhập; tính xác thực dựa vào chữ ký của hãng).
 * Lalamove yêu cầu trả 200 nhanh và sẽ gửi lại tối đa 10 lần trong 24 giờ nếu không nhận được 200.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/shipping")
@RequiredArgsConstructor
public class ShippingWebhookController {
    private final ShippingProviderRegistry providers;
    private final ShippingProperties properties;
    private final OrderShippingService shippingService;

    @PostMapping("/{providerCode}")
    public ResponseEntity<Map<String, Object>> receive(@PathVariable String providerCode,
                                                       @RequestBody(required = false) String body,
                                                       HttpServletRequest request) {
        ShippingProvider provider = providers.byCode(providerCode).orElse(null);
        if (provider == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Unknown provider"));
        Optional<ShippingModels.WebhookEvent> event;
        try {
            event = provider.parseWebhook(signedPath(request), body);
        } catch (InvalidWebhookException invalid) {
            log.warn("Rejected {} webhook: {}", provider.code(), invalid.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", invalid.getMessage()));
        }
        if (event.isEmpty()) return ResponseEntity.ok(Map.of("received", true));
        // Nhật ký audit cần tác nhân: webhook được ghi như tác vụ hệ thống của hãng.
        try (var ignored = BusinessAuditContextFactory.systemJob("SHIPPING_WEBHOOK:" + provider.code())) {
            String result = shippingService.applyWebhook(provider.code(), event.get());
            return ResponseEntity.ok(Map.of("received", true, "result", result));
        }
    }

    /** Path của URL đã đăng ký với hãng (SHIPPING_WEBHOOK_URL) vì nó nằm trong chuỗi ký; mặc định là path thực nhận. */
    private String signedPath(HttpServletRequest request) {
        String configured = properties.webhookUrl();
        if (configured != null && !configured.isBlank()) {
            String path = URI.create(configured.trim()).getPath();
            if (path != null && !path.isBlank()) return path;
        }
        return request.getRequestURI();
    }
}
