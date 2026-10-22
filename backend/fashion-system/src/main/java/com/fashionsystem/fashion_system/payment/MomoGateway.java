package com.fashionsystem.fashion_system.payment;

import com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput;
import com.fashionsystem.fashion_system.payment.PaymentModels.GatewayCallback;
import com.fashionsystem.fashion_system.payment.PaymentModels.Outcome;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentLink;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentRequest;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * MoMo AIO v2, thanh toán một lần qua ví ({@code requestType=captureWallet}; developers.momo.vn, "One-Time Payment").
 *
 * <p>Tạo link: {@code POST /v2/gateway/api/create}, ký HMAC-SHA256 (hex) bằng secretKey trên chuỗi
 * {@code accessKey=..&amount=..&extraData=..&ipnUrl=..&orderId=..&orderInfo=..&partnerCode=..&redirectUrl=..&requestId=..&requestType=..}.
 * IPN gửi POST JSON, ký trên {@code accessKey, amount, extraData, message, orderId, orderInfo, orderType,
 * partnerCode, payType, requestId, responseTime, resultCode, transId} theo đúng thứ tự đó; đối tác phải trả HTTP 204.
 * Return URL nhận cùng các trường dưới dạng query. resultCode 0 là thành công; 9000 (đã cấp quyền) với thanh toán
 * một bước cũng được coi là thành công; 1000/7000/7002 là đang xử lý; mã khác là thất bại.</p>
 */
@Slf4j
@Component
public class MomoGateway implements PaymentGateway {
    public static final String CODE = "MOMO";
    static final String REQUEST_TYPE = "captureWallet";
    private static final List<String> IPN_FIELDS = List.of("accessKey", "amount", "extraData", "message", "orderId",
            "orderInfo", "orderType", "partnerCode", "payType", "requestId", "responseTime", "resultCode", "transId");
    private static final List<String> PENDING_CODES = List.of("1000", "7000", "7002");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final PaymentProperties properties;
    private RestClient client;

    @Autowired
    public MomoGateway(PaymentProperties properties) {
        this(properties, null);
    }

    MomoGateway(PaymentProperties properties, RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean configured() {
        return properties.momo() != null && properties.momo().configured();
    }

    @Override
    public PaymentLink createPayment(PaymentRequest request) {
        if (!configured()) throw new PaymentGatewayException("MoMo chưa được cấu hình");
        var config = properties.momo();
        String requestId = UUID.randomUUID().toString();
        String extraData = "";
        String raw = "accessKey=" + config.accessKey().trim() + "&amount=" + request.amount() + "&extraData=" + extraData
                + "&ipnUrl=" + request.ipnUrl() + "&orderId=" + request.reference() + "&orderInfo=" + request.orderInfo()
                + "&partnerCode=" + config.partnerCode().trim() + "&redirectUrl=" + request.returnUrl()
                + "&requestId=" + requestId + "&requestType=" + REQUEST_TYPE;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", config.partnerCode().trim());
        body.put("requestId", requestId);
        body.put("amount", request.amount());
        body.put("orderId", request.reference());
        body.put("orderInfo", request.orderInfo());
        body.put("redirectUrl", request.returnUrl());
        body.put("ipnUrl", request.ipnUrl());
        body.put("requestType", REQUEST_TYPE);
        body.put("extraData", extraData);
        body.put("lang", "vi");
        body.put("signature", PaymentSigning.hmacSha256Hex(config.secretKey().trim(), raw));

        String response;
        try {
            response = client().post().uri("/v2/gateway/api/create").contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.writeValueAsString(body))
                    .exchange((req, res) -> {
                        String text = new String(res.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        HttpStatusCode status = res.getStatusCode();
                        if (status.is5xxServerError())
                            throw new PaymentGatewayException("MoMo đang lỗi (HTTP " + status.value() + ")");
                        return text;
                    });
        } catch (RestClientException failure) {
            throw new PaymentGatewayException("Không kết nối được MoMo", failure);
        }
        JsonNode node;
        try {
            node = JSON.readTree(response == null ? "" : response);
        } catch (RuntimeException invalid) {
            throw new PaymentGatewayException("MoMo trả về dữ liệu không đọc được");
        }
        int resultCode = node.path("resultCode").asInt(-1);
        String payUrl = node.path("payUrl").asString("");
        if (resultCode != 0 || payUrl.isBlank()) {
            log.warn("MoMo create payment {} rejected: {} {}", request.reference(), resultCode, node.path("message").asString(""));
            throw new PaymentGatewayException("MoMo từ chối tạo thanh toán: " + node.path("message").asString("mã " + resultCode));
        }
        return new PaymentLink(payUrl, response);
    }

    @Override
    public GatewayCallback parseCallback(CallbackInput input) {
        if (!configured()) throw new InvalidPaymentCallbackException("MoMo chưa được cấu hình");
        Map<String, String> fields = fields(input);
        String signature = fields.get("signature");
        if (signature == null || signature.isBlank()) throw new InvalidPaymentCallbackException("Thiếu signature");
        var config = properties.momo();
        StringBuilder raw = new StringBuilder();
        for (String field : IPN_FIELDS) {
            if (!raw.isEmpty()) raw.append('&');
            String value = field.equals("accessKey") ? config.accessKey().trim() : fields.getOrDefault(field, "");
            raw.append(field).append('=').append(value);
        }
        String expected = PaymentSigning.hmacSha256Hex(config.secretKey().trim(), raw.toString());
        if (!PaymentSigning.constantTimeEqualsIgnoreCase(expected, signature.trim()))
            throw new InvalidPaymentCallbackException("Sai chữ ký MoMo");
        if (!config.partnerCode().trim().equals(fields.get("partnerCode")))
            throw new InvalidPaymentCallbackException("partnerCode không khớp");

        String reference = fields.get("orderId");
        if (reference == null || reference.isBlank()) throw new InvalidPaymentCallbackException("Thiếu orderId");
        BigDecimal amount;
        try {
            amount = new BigDecimal(fields.getOrDefault("amount", ""));
        } catch (NumberFormatException invalid) {
            throw new InvalidPaymentCallbackException("amount không hợp lệ");
        }
        String resultCode = fields.getOrDefault("resultCode", "");
        Outcome outcome = "0".equals(resultCode) || "9000".equals(resultCode) ? Outcome.SUCCESS
                : PENDING_CODES.contains(resultCode) ? Outcome.PENDING : Outcome.FAILED;
        String transId = fields.get("transId");
        transId = transId == null || transId.isBlank() || "0".equals(transId) ? null : transId;
        return new GatewayCallback(reference, transId, amount, outcome, resultCode, fields.getOrDefault("message", ""),
                reference + ":" + (transId == null ? "-" : transId) + ":" + resultCode, JSON.writeValueAsString(fields));
    }

    /** Trường của IPN (body JSON) hoặc của return URL (query) dưới dạng chuỗi như khi MoMo ký. */
    private static Map<String, String> fields(CallbackInput input) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (input.body() != null && !input.body().isBlank()) {
            JsonNode node;
            try {
                node = JSON.readTree(input.body());
            } catch (RuntimeException invalid) {
                throw new InvalidPaymentCallbackException("Body IPN không phải JSON");
            }
            if (!node.isObject()) throw new InvalidPaymentCallbackException("Body IPN không hợp lệ");
            node.properties().forEach(e -> {
                JsonNode value = e.getValue();
                if (value.isValueNode()) fields.put(e.getKey(), value.isNull() ? "" : value.asString(""));
            });
        } else if (input.params() != null) {
            fields.putAll(input.params());
        }
        return fields;
    }

    private RestClient client() {
        if (client == null) {
            var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(properties.timeoutSecondsOrDefault())).build();
            var factory = new JdkClientHttpRequestFactory(http);
            factory.setReadTimeout(Duration.ofSeconds(properties.timeoutSecondsOrDefault()));
            client = RestClient.builder().baseUrl(properties.momo().apiBaseUrl().trim()).requestFactory(factory).build();
        }
        return client;
    }
}
