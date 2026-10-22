package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.OrderCustomerContact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/** Request/response của các thao tác nhân viên trên đơn. */
public final class OrderActionRequests {
    private OrderActionRequests() {
    }

    /** Nhân viên hủy đơn: lý do bắt buộc và được gửi cho khách. */
    public record Cancel(@NotBlank(message = "Vui lòng nhập lý do hủy đơn") @Size(max = 500) String reason) {
    }

    /** Liên hệ khách: chọn ít nhất một kênh. */
    public record Contact(
            @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 200) String subject,
            @NotBlank(message = "Vui lòng nhập nội dung") @Size(max = 5000) String content,
            boolean viaEmail,
            boolean viaNotification) {
    }

    public record ContactView(UUID id, String subject, String content, boolean viaEmail, boolean viaNotification,
                              String emailTo, OrderCustomerContact.DeliveryStatus emailStatus, String emailError,
                              OrderCustomerContact.DeliveryStatus notificationStatus, String sentByName,
                              LocalDateTime createdAt) {
    }
}
