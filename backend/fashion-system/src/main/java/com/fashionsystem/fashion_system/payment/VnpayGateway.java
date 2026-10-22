package com.fashionsystem.fashion_system.payment;

import com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput;
import com.fashionsystem.fashion_system.payment.PaymentModels.GatewayCallback;
import com.fashionsystem.fashion_system.payment.PaymentModels.Outcome;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentLink;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentRequest;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * VNPay 2.1.0 (tài liệu "Thanh toán PAY", sandbox.vnpayment.vn/apis).
 *
 * <p>Link thanh toán là URL GET tới vnp_Url với các tham số {@code vnp_*} đã sắp xếp theo tên; chuỗi ký là
 * {@code key=URLEncode(value)} nối bằng {@code &}, ký HMAC-SHA512 bằng vnp_HashSecret (hex) và gửi trong
 * {@code vnp_SecureHash}. IPN và return URL nhận cùng bộ tham số, kiểm tra lại theo đúng cách đó sau khi bỏ
 * {@code vnp_SecureHash} và {@code vnp_SecureHashType}. Giao dịch thành công khi vnp_ResponseCode và
 * vnp_TransactionStatus cùng là "00". Số tiền gửi/nhận nhân 100.</p>
 */
@Component
public class VnpayGateway implements PaymentGateway {
    public static final String CODE = "VNPAY";
    static final String VERSION = "2.1.0";
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final PaymentProperties properties;

    public VnpayGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean configured() {
        return properties.vnpay() != null && properties.vnpay().configured();
    }

    @Override
    public PaymentLink createPayment(PaymentRequest request) {
        if (!configured()) throw new PaymentGatewayException("VNPay chưa được cấu hình");
        var config = properties.vnpay();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.tmnCode().trim());
        params.put("vnp_Amount", String.valueOf(Math.multiplyExact(request.amount(), 100L)));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", request.reference());
        params.put("vnp_OrderInfo", request.orderInfo());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", request.returnUrl());
        params.put("vnp_IpAddr", request.clientIp());
        params.put("vnp_CreateDate", VNPAY_TIME.format(request.createdAt()));
        params.put("vnp_ExpireDate", VNPAY_TIME.format(request.expiresAt()));
        String query = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        String hash = PaymentSigning.hmacSha512Hex(config.hashSecret().trim(), hashData(params));
        return new PaymentLink(config.payUrl().trim() + "?" + query + "&vnp_SecureHash=" + hash, null);
    }

    @Override
    public GatewayCallback parseCallback(CallbackInput input) {
        if (!configured()) throw new InvalidPaymentCallbackException("VNPay chưa được cấu hình");
        Map<String, String> received = input.params() == null ? Map.of() : input.params();
        String secureHash = received.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) throw new InvalidPaymentCallbackException("Thiếu vnp_SecureHash");
        Map<String, String> signed = new TreeMap<>();
        received.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")
                    && value != null && !value.isEmpty()) signed.put(key, value);
        });
        String expected = PaymentSigning.hmacSha512Hex(properties.vnpay().hashSecret().trim(), hashData(signed));
        if (!PaymentSigning.constantTimeEqualsIgnoreCase(expected, secureHash.trim()))
            throw new InvalidPaymentCallbackException("Sai chữ ký VNPay");
        if (!properties.vnpay().tmnCode().trim().equals(signed.get("vnp_TmnCode")))
            throw new InvalidPaymentCallbackException("vnp_TmnCode không khớp");

        String reference = require(signed, "vnp_TxnRef");
        BigDecimal amount;
        try {
            amount = new BigDecimal(require(signed, "vnp_Amount")).divide(HUNDRED);
        } catch (NumberFormatException | ArithmeticException invalid) {
            throw new InvalidPaymentCallbackException("vnp_Amount không hợp lệ");
        }
        String responseCode = signed.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = signed.getOrDefault("vnp_TransactionStatus", "");
        Outcome outcome = "00".equals(responseCode) && "00".equals(transactionStatus) ? Outcome.SUCCESS : Outcome.FAILED;
        String transactionNo = signed.get("vnp_TransactionNo");
        String code = responseCode + "/" + transactionStatus;
        return new GatewayCallback(reference, blankToNull(transactionNo), amount, outcome, code, message(responseCode),
                reference + ":" + (transactionNo == null ? "-" : transactionNo) + ":" + code, JSON.writeValueAsString(received));
    }

    /** Chuỗi ký: tham số đã sắp xếp theo tên, giá trị URL-encode (dấu cách thành '+') như code mẫu Java của VNPay. */
    static String hashData(Map<String, String> sortedParams) {
        return sortedParams.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private static String require(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) throw new InvalidPaymentCallbackException("Thiếu " + key);
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() || "0".equals(value) ? null : value;
    }

    /** Mã lỗi thường gặp (bảng vnp_ResponseCode trong tài liệu VNPay). */
    static String message(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Giao dịch bị nghi ngờ gian lận";
            case "09" -> "Thẻ/tài khoản chưa đăng ký Internet Banking";
            case "10" -> "Xác thực thông tin thẻ/tài khoản sai quá 3 lần";
            case "11" -> "Hết hạn chờ thanh toán";
            case "12" -> "Thẻ/tài khoản bị khóa";
            case "13" -> "Nhập sai mật khẩu OTP";
            case "24" -> "Khách hàng hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Tài khoản vượt hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Giao dịch không thành công (mã " + responseCode + ")";
        };
    }
}
