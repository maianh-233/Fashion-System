package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.ShipmentStatus;
import com.fashionsystem.fashion_system.entity.SupportTopic;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Bot hỗ trợ đơn hàng theo kịch bản cố định, không dùng AI.
 *
 * <p>Bot chỉ trả lời trạng thái đơn, thông tin vận chuyển và câu hỏi chính sách (nội dung lấy từ bảng
 * support_faqs). Bot không bao giờ hủy đơn hay thao tác thay khách. Tin nhắn tự gõ được nhận diện bằng từ khóa
 * cố định; không nhận ra thì chuyển cho nhân viên.</p>
 */
@Component
public class SupportBot {
    public static final String ORDER_STATUS = "ORDER_STATUS", SHIPPING_INFO = "SHIPPING_INFO",
            TALK_TO_STAFF = "TALK_TO_STAFF";

    public record Option(String code, String label) {
    }

    /** @param escalate chuyển hội thoại cho nhân viên */
    public record Reply(String text, List<Option> options, boolean escalate) {
    }

    /** Một phần đơn (đơn thường có một phần). */
    public record Part(String orderCode, String storeName, OrderStatus status, Shipment shipment) {
    }

    public record Shipment(String provider, ShipmentStatus status, String trackingUrl, String driverName,
                           String driverPlate) {
    }

    public record Faq(SupportTopic topic, String title, String answer, String keywords) {
    }

    /** Dữ liệu bot được phép đọc khi trả lời. */
    public record Context(String orderCode, List<Part> parts, Map<SupportTopic, Faq> faqs) {
    }

    private static final Map<SupportTopic, String> POLICY_CODES = Map.of(
            SupportTopic.RETURN, "POLICY_RETURN", SupportTopic.CANCEL, "POLICY_CANCEL",
            SupportTopic.SHIPPING, "POLICY_SHIPPING", SupportTopic.PAYMENT, "POLICY_PAYMENT");

    private static final Map<ShipmentStatus, String> SHIPMENT_LABELS = Map.of(
            ShipmentStatus.ASSIGNING_DRIVER, "đang tìm tài xế",
            ShipmentStatus.DRIVER_ASSIGNED, "tài xế đã nhận chuyến, đang tới lấy hàng",
            ShipmentStatus.PICKED_UP, "tài xế đã lấy hàng và đang giao tới bạn",
            ShipmentStatus.COMPLETED, "đã giao thành công",
            ShipmentStatus.FAILED, "chuyến giao chưa thành công, cửa hàng sẽ sắp xếp lại",
            ShipmentStatus.CANCELLED, "chuyến giao đã hủy",
            ShipmentStatus.REPLACED, "đơn vị vận chuyển đã đổi chuyến khác");

    /** Từ khóa cố định (đã bỏ dấu) cho các câu hỏi không thuộc chính sách. */
    private static final Map<String, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put(TALK_TO_STAFF, List.of("nhan vien", "gap nguoi", "tu van", "nguoi that", "admin", "shop oi"));
        KEYWORDS.put(SHIPPING_INFO, List.of("tai xe", "van don", "theo doi", "shipper", "link", "dang giao"));
        KEYWORDS.put(ORDER_STATUS, List.of("o dau", "trang thai", "den chua", "xac nhan chua", "tinh trang", "don hang cua toi"));
    }

    public List<Option> menu(Context context) {
        List<Option> options = new ArrayList<>(List.of(new Option(ORDER_STATUS, "Đơn hàng của tôi đang ở đâu?"),
                new Option(SHIPPING_INFO, "Thông tin vận chuyển")));
        for (SupportTopic topic : SupportTopic.values()) {
            Faq faq = context.faqs().get(topic);
            if (faq != null) options.add(new Option(POLICY_CODES.get(topic), faq.title()));
        }
        options.add(new Option(TALK_TO_STAFF, "Gặp nhân viên"));
        return options;
    }

    public Reply welcome(Context context) {
        return new Reply("Xin chào! Mình là trợ lý tự động của Lunaria, hỗ trợ bạn về đơn " + context.orderCode()
                + ". Bạn cần hỗ trợ gì?", menu(context), false);
    }

    /** Trả lời một nút khách bấm, hoặc tin nhắn tự gõ khi {@code optionCode} null. */
    public Reply answer(String optionCode, String text, Context context) {
        String code = optionCode != null ? optionCode : recognize(text, context);
        if (code == null)
            return new Reply("Mình chưa trả lời được câu hỏi này. Mình đã chuyển cuộc trò chuyện cho nhân viên cửa hàng, "
                    + "bạn vui lòng chờ trong giây lát nhé.", List.of(), true);
        if (TALK_TO_STAFF.equals(code))
            return new Reply("Mình đã chuyển cuộc trò chuyện cho nhân viên cửa hàng. Bạn vui lòng chờ trong giây lát nhé.",
                    List.of(), true);
        if (ORDER_STATUS.equals(code)) return new Reply(orderStatus(context), menu(context), false);
        if (SHIPPING_INFO.equals(code)) return new Reply(shippingInfo(context), menu(context), false);
        for (Map.Entry<SupportTopic, String> policy : POLICY_CODES.entrySet())
            if (policy.getValue().equals(code)) {
                Faq faq = context.faqs().get(policy.getKey());
                if (faq == null) break;
                return new Reply(faq.answer(), menu(context), false);
            }
        return new Reply("Lựa chọn không hợp lệ, bạn chọn một mục bên dưới nhé.", menu(context), false);
    }

    private String orderStatus(Context context) {
        if (context.parts().size() == 1) {
            Part part = context.parts().getFirst();
            return "Đơn " + context.orderCode() + " đang ở trạng thái: " + part.status().label() + "."
                    + shipmentSentence(part.shipment());
        }
        StringBuilder text = new StringBuilder("Đơn " + context.orderCode() + " được giao thành "
                + context.parts().size() + " phần:");
        for (Part part : context.parts())
            text.append("\n• ").append(part.orderCode()).append(" từ ").append(part.storeName()).append(": ")
                    .append(part.status().label()).append('.').append(shipmentSentence(part.shipment()));
        return text.toString();
    }

    private String shippingInfo(Context context) {
        StringBuilder text = new StringBuilder();
        for (Part part : context.parts()) {
            if (!text.isEmpty()) text.append('\n');
            String prefix = context.parts().size() > 1 ? "• " + part.orderCode() + " (" + part.storeName() + "): " : "";
            Shipment shipment = part.shipment();
            if (shipment == null) {
                text.append(prefix).append(part.status() == OrderStatus.PENDING || part.status() == OrderStatus.CONFIRMED
                        ? "Cửa hàng đang chuẩn bị hàng, chưa bàn giao cho đơn vị vận chuyển."
                        : "Chưa có thông tin chuyến giao.");
                continue;
            }
            text.append(prefix).append("Đơn vị vận chuyển ").append(providerName(shipment.provider())).append(": ")
                    .append(SHIPMENT_LABELS.getOrDefault(shipment.status(), "đang cập nhật")).append('.');
            if (shipment.driverName() != null)
                text.append(" Tài xế: ").append(shipment.driverName())
                        .append(shipment.driverPlate() == null ? "" : " (" + shipment.driverPlate() + ")").append('.');
            if (shipment.trackingUrl() != null) text.append(" Theo dõi chuyến giao: ").append(shipment.trackingUrl());
        }
        // Hãng chưa trả về thời gian dự kiến nên bot không tự ước lượng.
        text.append("\nThời gian giao dự kiến: đơn vị vận chuyển chưa cung cấp, bạn theo dõi trực tiếp qua đường link nhé.");
        return text.toString();
    }

    private static String shipmentSentence(Shipment shipment) {
        if (shipment == null) return "";
        return " Chuyến giao: " + SHIPMENT_LABELS.getOrDefault(shipment.status(), "đang cập nhật") + "."
                + (shipment.trackingUrl() == null ? "" : " Theo dõi: " + shipment.trackingUrl());
    }

    private static String providerName(String code) {
        return "LALAMOVE".equals(code) ? "Lalamove" : code;
    }

    /** Nhận diện câu hỏi tự gõ bằng từ khóa cố định và từ khóa của từng chính sách. */
    String recognize(String text, Context context) {
        if (text == null || text.isBlank()) return null;
        String normalized = normalize(text);
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet())
            if (entry.getValue().stream().anyMatch(normalized::contains)) return entry.getKey();
        for (SupportTopic topic : SupportTopic.values()) {
            Faq faq = context.faqs().get(topic);
            if (faq == null || faq.keywords() == null) continue;
            boolean matched = Arrays.stream(faq.keywords().split(",")).map(SupportBot::normalize)
                    .filter(k -> !k.isBlank()).anyMatch(k -> containsWord(normalized, k));
            if (matched) return POLICY_CODES.get(topic);
        }
        return null;
    }

    /** Khớp nguyên từ để "tra" không khớp nhầm trong "trang thai". */
    private static boolean containsWord(String text, String keyword) {
        return (" " + text + " ").contains(" " + keyword + " ");
    }

    static String normalize(String value) {
        String noAccent = Normalizer.normalize(value.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
