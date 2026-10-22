package com.fashionsystem.fashion_system.payment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Tra cứu cổng thanh toán theo mã phương thức trên đơn (VNPAY, MOMO). */
@Component
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentGatewayRegistry {
    private final List<PaymentGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.gateways = List.copyOf(gateways);
    }

    public Optional<PaymentGateway> byCode(String code) {
        if (code == null) return Optional.empty();
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return gateways.stream().filter(g -> g.code().equals(normalized)).findFirst();
    }

    public boolean isGateway(String method) {
        return byCode(method).isPresent();
    }
}
