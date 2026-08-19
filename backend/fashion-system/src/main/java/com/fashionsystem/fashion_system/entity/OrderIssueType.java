package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code order_issue_types}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "order_issue_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderIssueType {
    /** Lưu mã nghiệp vụ của bản ghi. */
    @Id
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /** Lưu tên hiển thị của bản ghi. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

}

