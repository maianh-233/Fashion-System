package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Câu trả lời chính sách của bot; quản trị viên sửa nội dung. */
@Entity
@Table(name = "support_faqs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportFaq {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 30, unique = true)
    private SupportTopic topic;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    /** Từ khóa (phân tách bằng dấu phẩy) để bot nhận ra câu hỏi khách tự gõ. */
    @Column(name = "keywords", length = 500)
    private String keywords;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
