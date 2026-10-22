package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.notification.NotificationType;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Nội dung thông báo đơn hàng gửi khách và nhân viên; tách riêng để kiểm thử không cần hạ tầng. */
@Component
public class OrderNotificationComposer {

    public record Message(String type, String title, String body) {
    }

    /**
     * Ngữ cảnh hiển thị của một lần đổi trạng thái.
     *
     * @param customerOrderCode mã đơn khách nhìn thấy (đơn tổng nếu đơn bị tách)
     * @param partCode          mã phần đơn của cửa hàng khi đơn bị tách, ngược lại null
     */
    public record StatusContext(OrderEvents.StatusChanged event, String customerOrderCode, String partCode,
                                String storeName) {
    }

    /** Thông báo khách khi đơn đổi trạng thái; không gửi khi chính khách thao tác hoặc đổi trạng thái nội bộ. */
    public Optional<Message> forCustomer(StatusContext context) {
        var event = context.event();
        if (event.actor().type() == OrderActorType.CUSTOMER) return Optional.empty();
        // Hãng vận chuyển không nhận chuyến, đơn quay lại "Đã xác nhận": nhân viên liên hệ khách trước.
        if (event.from() == OrderStatus.HANDED_TO_SHIPPER && event.to() == OrderStatus.CONFIRMED) return Optional.empty();
        String part = context.partCode() == null ? "" : " (phần giao từ " + context.storeName() + ")";
        String body = switch (event.to()) {
            case CONFIRMED -> "Cửa hàng " + context.storeName()
                    + (context.partCode() == null ? " đã xác nhận đơn" : " đã xác nhận phần đơn của bạn")
                    + " và đang chuẩn bị hàng.";
            case HANDED_TO_SHIPPER -> "Đơn hàng" + part + " đã được bàn giao cho đơn vị vận chuyển.";
            case SHIPPING -> "Đơn hàng" + part + " đang được giao tới bạn.";
            case COMPLETED -> "Đơn hàng" + part + " đã giao thành công. Cảm ơn bạn đã mua sắm tại Lunaria!";
            case DELIVERY_FAILED -> "Giao hàng" + part + " chưa thành công. Cửa hàng sẽ liên hệ với bạn.";
            case RETURNED -> "Hàng" + part + " đã được hoàn về cửa hàng.";
            case CANCELLED -> "Đơn hàng" + part + " đã bị hủy."
                    + (event.reason() == null ? "" : " Lý do: " + event.reason());
            case PENDING -> null;
        };
        if (body == null) return Optional.empty();
        String type = event.to() == OrderStatus.CANCELLED ? NotificationType.ORDER_CANCELLED : NotificationType.ORDER_STATUS_CHANGED;
        return Optional.of(new Message(type, "Đơn " + context.customerOrderCode() + ": " + event.to().label(), body));
    }

    /** Thông báo nhân viên cửa hàng khi khách tự hủy đơn. */
    public Optional<Message> forStaff(StatusContext context) {
        var event = context.event();
        if (event.to() != OrderStatus.CANCELLED || event.actor().type() != OrderActorType.CUSTOMER) return Optional.empty();
        return Optional.of(new Message(NotificationType.ORDER_CANCELLED_BY_CUSTOMER,
                "Khách đã hủy đơn " + event.orderCode(),
                event.reason() == null ? "Khách không nêu lý do." : "Lý do: " + event.reason()));
    }

    /** Báo nhân viên xử lý khi chuyến giao không thành công, kèm việc cần làm tiếp. */
    public Message shipmentFailed(OrderEvents.ShipmentFailed event) {
        String reason = event.reason() == null ? "Chuyến giao không thành công" : event.reason();
        String next = event.goodsWithDriver()
                ? " Hàng đang được hoàn về cửa hàng; xác nhận khi đã nhận lại hàng."
                : " Đơn đã quay về \"Đã xác nhận\": liên hệ khách để chờ giao lại (gửi lại yêu cầu giao hàng) hoặc hủy đơn.";
        return new Message(NotificationType.ORDER_SHIPMENT_FAILED,
                "Giao hàng chưa thành công · " + event.orderCode(), reason + "." + next);
    }

    /** Nhân viên cửa hàng được phân một phần của đơn online mới. */
    public Message newOrderForStaff(OrderEvents.OnlineOrderPlaced.Part part, boolean split, String customerOrderCode) {
        String items = String.join(", ", part.items());
        String suffix = split ? " (một phần của đơn " + customerOrderCode + ")" : "";
        return new Message(NotificationType.ORDER_PLACED, "Có đơn online mới " + part.orderCode(),
                "Sản phẩm: " + items + suffix + ". Vui lòng xác nhận đơn.");
    }

    /** Khách vừa đặt đơn; nếu bị tách thì báo đơn gồm mấy phần, mỗi phần có gì và giao từ đâu. */
    public Message placedForCustomer(OrderEvents.OnlineOrderPlaced event) {
        if (!event.split()) {
            var part = event.parts().getFirst();
            return new Message(NotificationType.ORDER_PLACED, "Đặt hàng thành công · " + event.orderCode(),
                    "Đơn đang chờ cửa hàng " + part.storeName() + " xác nhận.");
        }
        StringBuilder body = new StringBuilder("Để giao nhanh nhất, đơn được chia thành ")
                .append(event.parts().size()).append(" phần: ");
        for (int i = 0; i < event.parts().size(); i++) {
            var part = event.parts().get(i);
            body.append(i == 0 ? "" : "; ").append("Phần ").append(i + 1).append(" giao từ ").append(part.storeName())
                    .append(": ").append(part.items().stream().collect(Collectors.joining(", ")));
        }
        return new Message(NotificationType.ORDER_SPLIT, "Đơn " + event.orderCode() + " được giao thành "
                + event.parts().size() + " phần", body.append('.').toString());
    }
}
