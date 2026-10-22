package com.fashionsystem.fashion_system.shipping;

import com.fashionsystem.fashion_system.entity.ShipmentStatus;
import com.fashionsystem.fashion_system.shipping.ShippingModels.ShipmentRequest;
import com.fashionsystem.fashion_system.shipping.ShippingModels.Stop;
import com.fashionsystem.fashion_system.shipping.ShippingModels.WebhookType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LalamoveShippingProviderTest {
    static final String KEY = "pk_test_lunaria", SECRET = "sk_test_lunaria_secret";
    static final String PATH = "/api/webhooks/shipping/lalamove";
    final ShippingProperties properties = new ShippingProperties(true, "LALAMOVE", "https://rest.sandbox.lalamove.com",
            KEY, SECRET, null, 10, new ShippingProperties.Lalamove("VN", "vi_VN", "MOTORCYCLE"));
    final Clock clock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC);

    @Test void signatureMatchesLalamoveFormulaComputedIndependently() {
        // Giá trị tham chiếu tính bằng Python hmac theo công thức trong tài liệu Lalamove.
        assertThat(LalamoveSigner.sign("1700000000000", "POST", "/v3/quotations",
                "{\"data\":{\"serviceType\":\"MOTORCYCLE\"}}", SECRET))
                .isEqualTo("d937164f18adbd2b0817aef5708aa6f709391363e75372c467676d10fdc1e7cb");
        assertThat(LalamoveSigner.sign("1700000000000", "GET", "/v3/orders/123", "", SECRET))
                .isEqualTo("5bab3bf214e8d424a920af3395c6308ff41f5ecb934799f2ca11fe00f2f58872");
        assertThat(LalamoveSigner.authorization(KEY, "1700000000000", "abc")).isEqualTo("hmac pk_test_lunaria:1700000000000:abc");
    }

    /** Body được định dạng xuống dòng nhưng chữ ký tính trên JSON.stringify(data) dạng gọn, đúng như mẫu của Lalamove. */
    static final String DRIVER_ASSIGNED = """
            {
              "apiKey": "pk_test_lunaria",
              "timestamp": 1775027622,
              "signature": "32dd4aea837df249e982b53e238153d46c4f25ec35674e9544ed5faf29a6d805",
              "eventId": "EFA4A3AA-22DD-4E61-B7C4-459D890BE1E1",
              "eventType": "DRIVER_ASSIGNED",
              "eventVersion": "v3",
              "data": {
                "driver": { "driverId": "79973", "phone": "+85288888888", "name": "Tài xế Test", "photo": "", "plateNumber": "VP7815702" },
                "location": { "lng": 114.05575560999996, "lat": 22.575277319999994 },
                "order": { "orderId": "3463513590991397204" },
                "updatedAt": "2026-04-01T15:13:00Z"
              }
            }
            """;

    @Test void verifiesWebhookSignatureOverStringifiedData() {
        var provider = new LalamoveShippingProvider(properties, clock, null);
        var event = provider.parseWebhook(PATH, DRIVER_ASSIGNED).orElseThrow();
        assertThat(event.type()).isEqualTo(WebhookType.DRIVER_ASSIGNED);
        assertThat(event.providerOrderId()).isEqualTo("3463513590991397204");
        assertThat(event.driver().name()).isEqualTo("Tài xế Test");
        assertThat(event.driver().plateNumber()).isEqualTo("VP7815702");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-04-01T15:13:00Z"));
    }

    @Test void rejectsTamperedBodyWrongPathAndForeignApiKey() {
        var provider = new LalamoveShippingProvider(properties, clock, null);
        assertThatThrownBy(() -> provider.parseWebhook(PATH, DRIVER_ASSIGNED.replace("VP7815702", "VP0000000")))
                .isInstanceOf(InvalidWebhookException.class);
        assertThatThrownBy(() -> provider.parseWebhook("/other/path", DRIVER_ASSIGNED))
                .isInstanceOf(InvalidWebhookException.class);
        assertThatThrownBy(() -> provider.parseWebhook(PATH, DRIVER_ASSIGNED.replace("pk_test_lunaria", "pk_test_other")))
                .isInstanceOf(InvalidWebhookException.class);
        assertThatThrownBy(() -> provider.parseWebhook(PATH, "not json")).isInstanceOf(InvalidWebhookException.class);
    }

    @Test void emptyBodyIsTheConnectivityCheck() {
        var provider = new LalamoveShippingProvider(properties, clock, null);
        assertThat(provider.parseWebhook(PATH, "")).isEmpty();
        assertThat(provider.parseWebhook(PATH, null)).isEmpty();
    }

    static String signedWebhook(String eventType, String dataJson) {
        String signature = LalamoveSigner.sign("1775026716", "POST", PATH, dataJson, SECRET);
        return "{\"apiKey\":\"" + KEY + "\",\"timestamp\":1775026716,\"signature\":\"" + signature
                + "\",\"eventId\":\"E-" + signature.substring(0, 8) + "\",\"eventType\":\"" + eventType
                + "\",\"eventVersion\":\"v3\",\"data\":" + dataJson + "}";
    }

    @Test void mapsStatusesAndCarriesCancelReason() {
        var provider = new LalamoveShippingProvider(properties, clock, null);
        var expired = provider.parseWebhook(PATH, signedWebhook("ORDER_STATUS_CHANGED",
                "{\"order\":{\"orderId\":\"111\",\"previousStatus\":\"ASSIGNING_DRIVER\",\"status\":\"EXPIRED\"},\"updatedAt\":\"2026-04-01T14:58:00Z\"}")).orElseThrow();
        assertThat(expired.status()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(expired.reason()).isEqualTo("Lalamove không tìm được tài xế");

        var canceled = provider.parseWebhook(PATH, signedWebhook("ORDER_STATUS_CHANGED",
                "{\"order\":{\"orderId\":\"111\",\"cancelParty\":\"LALAMOVE_CUSTOMER_SUPPORT\",\"cancelReason\":\"Driver asked to cancel\",\"previousStatus\":\"PICKED_UP\",\"status\":\"CANCELED\"},\"updatedAt\":\"2026-04-01T15:36:00Z\"}")).orElseThrow();
        assertThat(canceled.status()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(canceled.previousProviderStatus()).isEqualTo("PICKED_UP");
        assertThat(canceled.reason()).contains("Driver asked to cancel");

        var pickedUp = provider.parseWebhook(PATH, signedWebhook("ORDER_STATUS_CHANGED",
                "{\"order\":{\"orderId\":\"111\",\"status\":\"PICKED_UP\"},\"updatedAt\":\"2026-04-01T15:00:00Z\"}")).orElseThrow();
        assertThat(pickedUp.status()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(pickedUp.reason()).isNull();

        var unknown = provider.parseWebhook(PATH, signedWebhook("ORDER_STATUS_CHANGED",
                "{\"order\":{\"orderId\":\"111\",\"status\":\"SOMETHING_NEW\"}}")).orElseThrow();
        assertThat(unknown.status()).isNull();

        var replaced = provider.parseWebhook(PATH, signedWebhook("ORDER_REPLACED",
                "{\"order\":{\"orderId\":\"222\"},\"prevOrderId\":\"111\",\"updatedAt\":\"2026-04-01T15:39:00Z\"}")).orElseThrow();
        assertThat(replaced.type()).isEqualTo(WebhookType.ORDER_REPLACED);
        assertThat(replaced.previousProviderOrderId()).isEqualTo("111");
    }

    final ShipmentRequest request = new ShipmentRequest("LB-1",
            new Stop("Lunaria Q1", "0283 822 1234", "1 Lê Lợi, Q1", 10.7731, 106.7030),
            new Stop("Nguyễn An", "0900000999", "2 Nguyễn Huệ, Q1", 10.7769, 106.7009), 2, "Đơn LB-1");

    @Test void quotesThenPlacesOrderWithSignedHeadersAndE164Phones() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rest.sandbox.lalamove.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var provider = new LalamoveShippingProvider(properties, clock, builder.build());

        server.expect(requestTo("https://rest.sandbox.lalamove.com/v3/quotations")).andExpect(method(HttpMethod.POST))
                .andExpect(header("Market", "VN"))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("hmac pk_test_lunaria:1700000000000:")))
                .andExpect(header("Request-ID", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                .andExpect(jsonPath("$.data.serviceType").value("MOTORCYCLE"))
                .andExpect(jsonPath("$.data.language").value("vi_VN"))
                .andExpect(jsonPath("$.data.stops[0].coordinates.lat").value("10.7731"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"data":{"quotationId":"Q1","expiresAt":"2026-10-14T03:05:00.00Z",
                          "priceBreakdown":{"total":"32000","currency":"VND"},
                          "stops":[{"stopId":"S1"},{"stopId":"S2"}]}}"""));
        server.expect(requestTo("https://rest.sandbox.lalamove.com/v3/orders")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.data.quotationId").value("Q1"))
                .andExpect(jsonPath("$.data.sender.stopId").value("S1"))
                .andExpect(jsonPath("$.data.sender.phone").value("+842838221234"))
                .andExpect(jsonPath("$.data.recipients[0].phone").value("+84900000999"))
                .andExpect(jsonPath("$.data.metadata.orderCode").value("LB-1"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"data":{"orderId":"O1","status":"ASSIGNING_DRIVER","shareLink":"https://share.sandbox.lalamove.com/x",
                          "priceBreakdown":{"total":"32000","currency":"VND"}}}"""));

        var quote = provider.quote(request);
        assertThat(quote.fee()).isEqualByComparingTo(new BigDecimal("32000"));
        assertThat(quote.dropoffStopId()).isEqualTo("S2");
        var created = provider.createShipment(request, quote);
        assertThat(created.providerOrderId()).isEqualTo("O1");
        assertThat(created.status()).isEqualTo(ShipmentStatus.ASSIGNING_DRIVER);
        assertThat(created.trackingUrl()).contains("share.sandbox");
        server.verify();
    }

    @Test void providerErrorsBecomeReadableShippingExceptions() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rest.sandbox.lalamove.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var provider = new LalamoveShippingProvider(properties, clock, builder.build());
        server.expect(requestTo("https://rest.sandbox.lalamove.com/v3/orders/O1")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errors\":[{\"id\":\"ERR_CANCELLATION_FORBIDDEN\",\"message\":\"cannot cancel\"}]}"));
        assertThatThrownBy(() -> provider.cancelShipment("O1"))
                .isInstanceOf(ShippingException.class).hasMessageContaining("409").hasMessageContaining("ERR_CANCELLATION_FORBIDDEN");
    }

    @Test void missingCredentialsAreReportedBeforeCallingLalamove() {
        var empty = new ShippingProperties(true, "LALAMOVE", "https://rest.sandbox.lalamove.com", "", "", null, 10, null);
        assertThatThrownBy(() -> new LalamoveShippingProvider(empty, clock, null).quote(request))
                .isInstanceOf(ShippingException.class).hasMessageContaining("SHIPPING_CLIENT_ID");
    }

    @Test void phoneNumbersAreNormalizedToE164() {
        assertThat(PhoneNumbers.toE164Vietnam("0900 000 999")).isEqualTo("+84900000999");
        assertThat(PhoneNumbers.toE164Vietnam("84900000999")).isEqualTo("+84900000999");
        assertThat(PhoneNumbers.toE164Vietnam("+84900000999")).isEqualTo("+84900000999");
        assertThat(PhoneNumbers.toE164Vietnam("(028) 3822-1234")).isEqualTo("+842838221234");
    }
}
