package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lần nhân viên liên hệ khách về một đơn hàng qua email và/hoặc thông báo trong hệ thống. */
@Entity
@Table(name = "order_customer_contacts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCustomerContact {
    /** Trạng thái gửi của từng kênh. */
    public enum DeliveryStatus { SKIPPED, PENDING, SENT, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "sent_by", nullable = false)
    private UUID sentBy;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "via_email", nullable = false)
    private boolean viaEmail;

    @Column(name = "via_notification", nullable = false)
    private boolean viaNotification;

    @Column(name = "email_to", length = 255)
    private String emailTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    private DeliveryStatus emailStatus;

    @Column(name = "email_error", columnDefinition = "TEXT")
    private String emailError;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    private DeliveryStatus notificationStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
