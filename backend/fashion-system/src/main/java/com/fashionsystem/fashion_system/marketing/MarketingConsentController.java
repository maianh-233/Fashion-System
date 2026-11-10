package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tùy chọn nhận tin marketing: khách tự quản lý trong hồ sơ, và trang hủy đăng ký công khai (xác thực bằng token trong
 * email, không cần đăng nhập). Hủy đăng ký dùng POST để trình quét link của hộp thư không tự hủy hộ khách.
 */
@RestController
@RequiredArgsConstructor
public class MarketingConsentController {
    private final MarketingConsentService consents;

    @GetMapping("/api/customers/me/marketing-preferences")
    @PreAuthorize("hasRole('CUSTOMER')")
    public MarketingConsentService.Preferences preferences(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return consents.preferences(customer.customerId());
    }

    @PutMapping("/api/customers/me/marketing-preferences")
    @PreAuthorize("hasRole('CUSTOMER')")
    public MarketingConsentService.Preferences update(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                                      @RequestBody MarketingConsentService.PreferencesRequest request) {
        // Audit nghiệp vụ chỉ có tác nhân nhân viên / hệ thống: thao tác tự phục vụ của khách ghi như tác vụ hệ thống.
        try (var ignored = BusinessAuditContextFactory.systemJob("CUSTOMER_MARKETING_CONSENT:" + customer.customerId())) {
            return consents.update(customer.customerId(), request);
        }
    }

    @GetMapping("/api/public/marketing/unsubscribe")
    public MarketingConsentService.UnsubscribeInfo unsubscribeInfo(@RequestParam UUID token) {
        return consents.unsubscribeInfo(token);
    }

    public record UnsubscribeRequest(UUID token) {}

    @PostMapping("/api/public/marketing/unsubscribe")
    public MarketingConsentService.UnsubscribeInfo unsubscribe(@RequestBody UnsubscribeRequest request) {
        try (var ignored = BusinessAuditContextFactory.systemJob("MARKETING_UNSUBSCRIBE")) {
            return consents.unsubscribe(request.token());
        }
    }
}
