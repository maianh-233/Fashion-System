package com.fashionsystem.fashion_system.shipping;

import com.fashionsystem.fashion_system.entity.ShipmentStatus;
import com.fashionsystem.fashion_system.shipping.ShippingModels.CreatedShipment;
import com.fashionsystem.fashion_system.shipping.ShippingModels.Driver;
import com.fashionsystem.fashion_system.shipping.ShippingModels.ProviderShipment;
import com.fashionsystem.fashion_system.shipping.ShippingModels.Quote;
import com.fashionsystem.fashion_system.shipping.ShippingModels.ShipmentRequest;
import com.fashionsystem.fashion_system.shipping.ShippingModels.Stop;
import com.fashionsystem.fashion_system.shipping.ShippingModels.WebhookEvent;
import com.fashionsystem.fashion_system.shipping.ShippingModels.WebhookType;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lalamove API v3 (sandbox/production theo SHIPPING_API_BASE_URL).
 *
 * <p>Luồng: báo giá ({@code POST /v3/quotations}) → đặt chuyến ({@code POST /v3/orders}) với stopId của báo
 * giá; hủy {@code DELETE /v3/orders/{id}}; tra cứu {@code GET /v3/orders/{id}}. Body gửi/nhận bọc trong
 * {@code {"data": ...}}. Webhook ký bằng cùng API secret (tài liệu "Tutorial on Lalamove Webhook", 04/2026).</p>
 */
@Slf4j
@Component
public class LalamoveShippingProvider implements ShippingProvider {
    public static final String CODE = "LALAMOVE";
    private static final java.util.Set<String> FAILURE_STATUSES = java.util.Set.of("CANCELED", "EXPIRED", "REJECTED");

    private final ShippingProperties properties;
    private final JsonMapper json;
    private final Clock clock;
    private RestClient client;

    @Autowired
    public LalamoveShippingProvider(ShippingProperties properties) {
        this(properties, Clock.systemUTC(), null);
    }

    LalamoveShippingProvider(ShippingProperties properties, Clock clock, RestClient client) {
        this.properties = properties;
        this.clock = clock;
        this.client = client;
        // BigDecimal giữ nguyên chữ số của tọa độ/tiền khi tái tạo JSON.stringify(data) để kiểm tra chữ ký webhook.
        this.json = JsonMapper.builder()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                .build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public Quote quote(ShipmentRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("serviceType", lalamove().serviceType());
        data.put("language", lalamove().language());
        data.put("stops", List.of(stop(request.pickup()), stop(request.dropoff())));
        JsonNode body = call("POST", "/v3/quotations", Map.of("data", data)).path("data");
        JsonNode stops = body.path("stops");
        if (stops.size() < 2) throw new ShippingException("Lalamove không trả về điểm dừng trong báo giá");
        return new Quote(text(body, "quotationId"), money(body.path("priceBreakdown").path("total")),
                text(body.path("priceBreakdown"), "currency"), instant(text(body, "expiresAt")),
                text(stops.get(0), "stopId"), text(stops.get(1), "stopId"), body.toString());
    }

    @Override
    public CreatedShipment createShipment(ShipmentRequest request, Quote quote) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("quotationId", quote.quotationId());
        data.put("sender", Map.of("stopId", quote.pickupStopId(), "name", request.pickup().name(),
                "phone", PhoneNumbers.toE164Vietnam(request.pickup().phone())));
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("stopId", quote.dropoffStopId());
        recipient.put("name", request.dropoff().name());
        recipient.put("phone", PhoneNumbers.toE164Vietnam(request.dropoff().phone()));
        if (request.remarks() != null && !request.remarks().isBlank()) recipient.put("remarks", request.remarks());
        data.put("recipients", List.of(recipient));
        data.put("isPODEnabled", false);
        data.put("metadata", Map.of("orderCode", request.orderCode()));
        JsonNode body = call("POST", "/v3/orders", Map.of("data", data)).path("data");
        String status = text(body, "status");
        JsonNode price = body.path("priceBreakdown");
        return new CreatedShipment(text(body, "orderId"), status,
                LalamoveStatusMapping.toInternal(status).orElse(ShipmentStatus.ASSIGNING_DRIVER), text(body, "shareLink"),
                price.isMissingNode() ? quote.fee() : money(price.path("total")),
                price.isMissingNode() ? quote.currency() : text(price, "currency"), body.toString());
    }

    @Override
    public void cancelShipment(String providerOrderId) {
        call("DELETE", "/v3/orders/" + providerOrderId, null);
    }

    @Override
    public ProviderShipment getStatus(String providerOrderId) {
        JsonNode body = call("GET", "/v3/orders/" + providerOrderId, null).path("data");
        String status = text(body, "status");
        return new ProviderShipment(providerOrderId, status, LalamoveStatusMapping.toInternal(status).orElse(null),
                text(body, "shareLink"), body.toString());
    }

    @Override
    public Optional<WebhookEvent> parseWebhook(String path, String rawBody) {
        // Lalamove gọi thử URL bằng body rỗng khi đăng ký: phải trả 200.
        if (rawBody == null || rawBody.isBlank()) return Optional.empty();
        JsonNode root;
        try {
            root = json.readTree(rawBody);
        } catch (RuntimeException invalid) {
            throw new InvalidWebhookException("Webhook không phải JSON hợp lệ");
        }
        if (!requireConfigured().clientId().equals(text(root, "apiKey")))
            throw new InvalidWebhookException("apiKey của webhook không khớp");
        JsonNode data = root.path("data");
        String timestamp = root.path("timestamp").asString("");
        String signature = text(root, "signature");
        // Mẫu của Lalamove ký JSON.stringify(data); thử thêm đúng chuỗi data trong body phòng khi định dạng số khác.
        boolean valid = LalamoveSigner.matches(LalamoveSigner.sign(timestamp, "POST", path, json.writeValueAsString(data),
                properties.clientSecret()), signature)
                || rawDataSlice(rawBody).map(raw -> LalamoveSigner.matches(
                LalamoveSigner.sign(timestamp, "POST", path, raw, properties.clientSecret()), signature)).orElse(false);
        if (!valid) throw new InvalidWebhookException("Chữ ký webhook không hợp lệ");

        String rawType = text(root, "eventType");
        JsonNode order = data.path("order");
        WebhookType type = switch (rawType == null ? "" : rawType) {
            case "ORDER_STATUS_CHANGED" -> WebhookType.STATUS_CHANGED;
            case "DRIVER_ASSIGNED" -> WebhookType.DRIVER_ASSIGNED;
            case "ORDER_REPLACED" -> WebhookType.ORDER_REPLACED;
            default -> WebhookType.OTHER;
        };
        String status = text(order, "status");
        JsonNode driver = data.path("driver");
        Instant occurredAt = instant(text(data, "updatedAt"));
        if (occurredAt == null && !timestamp.isBlank()) occurredAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
        return Optional.of(new WebhookEvent(text(root, "eventId"), type, rawType, text(order, "orderId"), status,
                text(order, "previousStatus"),
                type == WebhookType.STATUS_CHANGED ? LalamoveStatusMapping.toInternal(status).orElse(null) : null,
                type == WebhookType.STATUS_CHANGED && FAILURE_STATUSES.contains(status)
                        ? LalamoveStatusMapping.failureLabel(status, text(order, "cancelReason")) : null,
                text(data, "prevOrderId"),
                driver.isMissingNode() ? null : new Driver(text(driver, "name"), text(driver, "phone"), text(driver, "plateNumber")),
                text(order, "shareLink"), occurredAt, rawBody));
    }

    private JsonNode call(String method, String path, Object body) {
        ShippingProperties config = requireConfigured();
        String payload = body == null ? "" : json.writeValueAsString(body);
        String timestamp = String.valueOf(clock.millis());
        String signature = LalamoveSigner.sign(timestamp, method, path, payload, config.clientSecret());
        try {
            var spec = client().method(org.springframework.http.HttpMethod.valueOf(method)).uri(path)
                    .header("Authorization", LalamoveSigner.authorization(config.clientId(), timestamp, signature))
                    .header("Market", lalamove().market())
                    .header("Request-ID", UUID.randomUUID().toString())
                    .accept(MediaType.APPLICATION_JSON);
            if (body != null) spec = spec.contentType(MediaType.APPLICATION_JSON).body(payload);
            String response = spec.retrieve()
                    .onStatus(HttpStatusCode::isError, (request, failure) -> {
                        String detail = new String(failure.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        throw new ShippingException("Lalamove từ chối yêu cầu (" + failure.getStatusCode().value() + "): "
                                + errorMessage(detail));
                    })
                    .body(String.class);
            return response == null || response.isBlank() ? json.createObjectNode() : json.readTree(response);
        } catch (ShippingException failure) {
            throw failure;
        } catch (RestClientException failure) {
            log.warn("Lalamove {} {} failed", method, path, failure);
            throw new ShippingException("Không kết nối được Lalamove, vui lòng thử lại", failure);
        }
    }

    private String errorMessage(String body) {
        try {
            JsonNode root = json.readTree(body);
            JsonNode first = root.path("errors").path(0);
            if (!first.isMissingNode()) {
                String id = text(first, "id"), message = text(first, "message");
                return (id == null ? "" : id + " ") + (message == null ? "" : message);
            }
            String message = text(root, "message");
            return message == null ? body : message;
        } catch (RuntimeException notJson) {
            return body;
        }
    }

    private RestClient client() {
        if (client == null) {
            var http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.timeoutSecondsOrDefault())).build();
            var factory = new JdkClientHttpRequestFactory(http);
            factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSecondsOrDefault()));
            client = RestClient.builder().baseUrl(properties.apiBaseUrl()).requestFactory(factory).build();
        }
        return client;
    }

    private ShippingProperties requireConfigured() {
        if (properties.clientId() == null || properties.clientId().isBlank()
                || properties.clientSecret() == null || properties.clientSecret().isBlank()
                || properties.apiBaseUrl() == null || properties.apiBaseUrl().isBlank())
            throw new ShippingException("Chưa cấu hình API key/secret Lalamove (SHIPPING_CLIENT_ID, SHIPPING_CLIENT_SECRET)");
        return properties;
    }

    private ShippingProperties.Lalamove lalamove() {
        var config = properties.lalamove();
        return config == null ? new ShippingProperties.Lalamove("VN", "vi_VN", "MOTORCYCLE") : config;
    }

    private static Map<String, Object> stop(Stop stop) {
        // Lalamove nhận tọa độ dạng chuỗi.
        return Map.of("coordinates", Map.of("lat", String.valueOf(stop.latitude()), "lng", String.valueOf(stop.longitude())),
                "address", stop.address());
    }

    /** Chuỗi JSON nguyên văn của trường "data" trong body (đếm ngoặc, bỏ qua ngoặc nằm trong chuỗi). */
    static Optional<String> rawDataSlice(String body) {
        int key = body.indexOf("\"data\"");
        if (key < 0) return Optional.empty();
        int start = body.indexOf('{', key);
        if (start < 0) return Optional.empty();
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (inString) {
                if (c == '\\') i++;
                else if (c == '"') inString = false;
            } else if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return Optional.of(body.substring(start, i + 1));
            }
        }
        return Optional.empty();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        String text = value.asString("");
        return text.isEmpty() ? null : text;
    }

    private static BigDecimal money(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        return new BigDecimal(node.asString("0"));
    }

    private static Instant instant(String value) {
        if (value == null) return null;
        try {
            return Instant.parse(value);
        } catch (RuntimeException invalid) {
            return null;
        }
    }
}
