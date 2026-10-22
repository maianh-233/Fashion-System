package com.fashionsystem.fashion_system.shipping;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Chọn hãng vận chuyển đang dùng theo SHIPPING_PROVIDER; webhook tìm hãng theo mã trên URL. */
@Component
@EnableConfigurationProperties(ShippingProperties.class)
public class ShippingProviderRegistry {
    private final ShippingProperties properties;
    private final Map<String, ShippingProvider> providers;

    public ShippingProviderRegistry(ShippingProperties properties, List<ShippingProvider> providers) {
        this.properties = properties;
        this.providers = providers.stream().collect(Collectors.toMap(ShippingProvider::code, Function.identity()));
    }

    /** Hãng dùng để tạo chuyến mới. */
    public ShippingProvider current() {
        if (!properties.enabled()) throw new ShippingException("Chưa bật tích hợp vận chuyển (SHIPPING_ENABLED=true)");
        String code = properties.provider() == null || properties.provider().isBlank()
                ? LalamoveShippingProvider.CODE : properties.provider().trim().toUpperCase(Locale.ROOT);
        return byCode(code).orElseThrow(() -> new ShippingException("Hãng vận chuyển " + code + " chưa được hỗ trợ"));
    }

    public Optional<ShippingProvider> byCode(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(providers.get(code.trim().toUpperCase(Locale.ROOT)));
    }
}
