package com.fashionsystem.fashion_system.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput;
import com.fashionsystem.fashion_system.payment.PaymentModels.Outcome;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentRequest;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class VnpayGatewayTest {
    static final String SECRET = "SECRETKEYFORTEST";
    final PaymentProperties properties = new PaymentProperties(15, "http://localhost:8080", null, 15,
            new PaymentProperties.Vnpay("TESTCODE", SECRET, "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"), null);
    final VnpayGateway gateway = new VnpayGateway(properties);

    final PaymentRequest request = new PaymentRequest("LBP261019150000000123", 125_000,
            "Thanh toan don hang LB-261019-000001", "127.0.0.1", LocalDateTime.of(2026, 10, 19, 15, 0),
            LocalDateTime.of(2026, 10, 19, 15, 15), "http://localhost:8080/api/webhooks/payments/vnpay/return",
            "http://localhost:8080/api/webhooks/payments/vnpay/ipn");

    @Test
    void signsSortedUrlEncodedParametersWithHmacSha512() {
        String url = gateway.createPayment(request).payUrl();
        Map<String, String> query = query(url);

        assertThat(url).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=12500000&");
        assertThat(query).containsEntry("vnp_Amount", "12500000").containsEntry("vnp_Version", "2.1.0")
                .containsEntry("vnp_CreateDate", "20261019150000").containsEntry("vnp_ExpireDate", "20261019151500")
                .containsEntry("vnp_TxnRef", "LBP261019150000000123");
        // Giá trị tham chiếu tính độc lập bằng Python: quote_plus từng giá trị, sắp xếp theo tên, HMAC-SHA512.
        assertThat(query.get("vnp_SecureHash")).isEqualTo("768897f4337750542be7fa7090f87d9cb4adeac4d00de74ede6b3d9acf48"
                + "0910c3a173b8a44c0e2289266864e1c088f7120105cd0644daac264d78d5c37d6d6f");
    }

    @Test
    void verifiesIpnAndReadsSuccess() {
        Map<String, String> ipn = signedIpn("00", "00", "12500000");
        var callback = gateway.parseCallback(new CallbackInput(ipn, null));

        assertThat(callback.reference()).isEqualTo("LBP261019150000000123");
        assertThat(callback.amount()).isEqualByComparingTo(new BigDecimal("125000"));
        assertThat(callback.outcome()).isEqualTo(Outcome.SUCCESS);
        assertThat(callback.gatewayTransactionId()).isEqualTo("14567890");
        assertThat(callback.eventKey()).isEqualTo("LBP261019150000000123:14567890:00/00");
    }

    @Test
    void customerCancelledIsAFailure() {
        var callback = gateway.parseCallback(new CallbackInput(signedIpn("24", "02", "12500000"), null));
        assertThat(callback.outcome()).isEqualTo(Outcome.FAILED);
        assertThat(callback.message()).isEqualTo("Khách hàng hủy giao dịch");
    }

    @Test
    void rejectsTamperedAmountOrMissingHash() {
        Map<String, String> ipn = signedIpn("00", "00", "12500000");
        ipn.put("vnp_Amount", "100");
        assertThatThrownBy(() -> gateway.parseCallback(new CallbackInput(ipn, null)))
                .isInstanceOf(InvalidPaymentCallbackException.class);

        Map<String, String> unsigned = signedIpn("00", "00", "12500000");
        unsigned.remove("vnp_SecureHash");
        assertThatThrownBy(() -> gateway.parseCallback(new CallbackInput(unsigned, null)))
                .isInstanceOf(InvalidPaymentCallbackException.class);
    }

    @Test
    void secureHashTypeIsNotPartOfTheSignature() {
        Map<String, String> ipn = signedIpn("00", "00", "12500000");
        ipn.put("vnp_SecureHashType", "HmacSHA512");
        assertThat(gateway.parseCallback(new CallbackInput(ipn, null)).outcome()).isEqualTo(Outcome.SUCCESS);
    }

    static Map<String, String> signedIpn(String responseCode, String transactionStatus, String amount) {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", amount);
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_OrderInfo", "Thanh toan don hang LB-261019-000001");
        params.put("vnp_PayDate", "20261019150512");
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TmnCode", "TESTCODE");
        params.put("vnp_TransactionNo", "14567890");
        params.put("vnp_TransactionStatus", transactionStatus);
        params.put("vnp_TxnRef", "LBP261019150000000123");
        Map<String, String> signed = new LinkedHashMap<>(params);
        signed.put("vnp_SecureHash", PaymentSigning.hmacSha512Hex(SECRET, VnpayGateway.hashData(params)));
        return signed;
    }

    private static Map<String, String> query(String url) {
        Map<String, String> query = new LinkedHashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            query.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return query;
    }
}
