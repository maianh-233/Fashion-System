package com.fashionsystem.fashion_system.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput;
import com.fashionsystem.fashion_system.payment.PaymentModels.Outcome;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class MomoGatewayTest {
    // Khóa test công khai trong tài liệu MoMo (môi trường test), chỉ dùng cho unit test.
    static final String ACCESS_KEY = "F8BBA842ECF85";
    static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    final PaymentProperties properties = new PaymentProperties(15, "http://localhost:8080", null, 15, null,
            new PaymentProperties.Momo("MOMO", ACCESS_KEY, SECRET_KEY, "https://test-payment.momo.vn"));
    final JsonMapper json = JsonMapper.builder().build();

    final PaymentRequest request = new PaymentRequest("LBP261019150000000123", 125_000,
            "Thanh toan don hang LB-261019-000001", "127.0.0.1", LocalDateTime.of(2026, 10, 19, 15, 0),
            LocalDateTime.of(2026, 10, 19, 15, 15), "http://localhost:8080/api/webhooks/payments/momo/return",
            "http://localhost:8080/api/webhooks/payments/momo/ipn");

    @Test
    void createsCaptureWalletPaymentSignedInDocumentedFieldOrder() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://test-payment.momo.vn");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AtomicReference<String> sent = new AtomicReference<>();
        server.expect(requestTo("https://test-payment.momo.vn/v2/gateway/api/create")).andExpect(method(HttpMethod.POST))
                .andExpect(req -> sent.set(((MockClientHttpRequest) req).getBodyAsString()))
                .andRespond(withSuccess("""
                        {"partnerCode":"MOMO","orderId":"LBP261019150000000123","amount":125000,"resultCode":0,
                         "message":"Thành công.","payUrl":"https://test-payment.momo.vn/v2/gateway/pay?t=abc"}
                        """, MediaType.APPLICATION_JSON));

        var link = new MomoGateway(properties, builder.build()).createPayment(request);

        assertThat(link.payUrl()).isEqualTo("https://test-payment.momo.vn/v2/gateway/pay?t=abc");
        JsonNode body = json.readTree(sent.get());
        assertThat(body.path("requestType").asString()).isEqualTo("captureWallet");
        assertThat(body.path("amount").asLong()).isEqualTo(125_000L);
        String raw = "accessKey=" + ACCESS_KEY + "&amount=125000&extraData=&ipnUrl=" + request.ipnUrl()
                + "&orderId=LBP261019150000000123&orderInfo=Thanh toan don hang LB-261019-000001&partnerCode=MOMO"
                + "&redirectUrl=" + request.returnUrl() + "&requestId=" + body.path("requestId").asString()
                + "&requestType=captureWallet";
        assertThat(body.path("signature").asString()).isEqualTo(PaymentSigning.hmacSha256Hex(SECRET_KEY, raw));
        server.verify();
    }

    @Test
    void rejectedCreateRequestRaises() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://test-payment.momo.vn");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://test-payment.momo.vn/v2/gateway/api/create"))
                .andRespond(withSuccess("{\"resultCode\":22,\"message\":\"Số tiền không hợp lệ\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> new MomoGateway(properties, builder.build()).createPayment(request))
                .isInstanceOf(PaymentGatewayException.class).hasMessageContaining("Số tiền không hợp lệ");
    }

    @Test
    void verifiesIpnSignature() {
        // Chữ ký tham chiếu tính độc lập bằng Python theo chuỗi rawSignature của tài liệu IPN.
        String body = """
                {"partnerCode":"MOMO","orderId":"LBP261019150000000123","requestId":"req-1","amount":125000,
                 "orderInfo":"Thanh toan don hang LB-261019-000001","orderType":"momo_wallet","transId":4088878653,
                 "resultCode":0,"message":"Successful.","payType":"qr","responseTime":1760860800000,"extraData":"",
                 "signature":"cdc84aec001ce7f117fa95e452537fec5de91d8c25034fd6cdf8b481e4ce84bb"}
                """;
        var callback = new MomoGateway(properties).parseCallback(new CallbackInput(Map.of(), body));

        assertThat(callback.outcome()).isEqualTo(Outcome.SUCCESS);
        assertThat(callback.amount()).isEqualByComparingTo(new BigDecimal("125000"));
        assertThat(callback.gatewayTransactionId()).isEqualTo("4088878653");
        assertThat(callback.eventKey()).isEqualTo("LBP261019150000000123:4088878653:0");

        assertThatThrownBy(() -> new MomoGateway(properties).parseCallback(
                new CallbackInput(Map.of(), body.replace("\"amount\":125000", "\"amount\":1000"))))
                .isInstanceOf(InvalidPaymentCallbackException.class);
    }

    @Test
    void mapsResultCodes() {
        var gateway = new MomoGateway(properties);
        assertThat(gateway.parseCallback(new CallbackInput(signed("1006"), null)).outcome()).isEqualTo(Outcome.FAILED);
        assertThat(gateway.parseCallback(new CallbackInput(signed("7000"), null)).outcome()).isEqualTo(Outcome.PENDING);
        assertThat(gateway.parseCallback(new CallbackInput(signed("9000"), null)).outcome()).isEqualTo(Outcome.SUCCESS);
    }

    /** Tham số return URL (query) đã ký. */
    private static Map<String, String> signed(String resultCode) {
        var params = new java.util.LinkedHashMap<String, String>();
        params.put("partnerCode", "MOMO");
        params.put("orderId", "LBP1");
        params.put("requestId", "req-2");
        params.put("amount", "125000");
        params.put("orderInfo", "x");
        params.put("orderType", "momo_wallet");
        params.put("transId", "0");
        params.put("resultCode", resultCode);
        params.put("message", "m");
        params.put("payType", "");
        params.put("responseTime", "1");
        params.put("extraData", "");
        String raw = "accessKey=" + ACCESS_KEY + "&amount=125000&extraData=&message=m&orderId=LBP1&orderInfo=x"
                + "&orderType=momo_wallet&partnerCode=MOMO&payType=&requestId=req-2&responseTime=1&resultCode="
                + resultCode + "&transId=0";
        params.put("signature", PaymentSigning.hmacSha256Hex(SECRET_KEY, raw));
        return params;
    }
}
