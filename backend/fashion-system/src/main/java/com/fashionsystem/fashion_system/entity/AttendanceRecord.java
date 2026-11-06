package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attendance_records", uniqueConstraints = {
    @UniqueConstraint(name = "uq_attendance_records", columnNames = {"user_id", "attendance_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "shift_id")
    private UUID shiftId;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "check_in_location_lat", precision = 9, scale = 6)
    private BigDecimal checkInLocationLat;

    @Column(name = "check_in_location_lon", precision = 9, scale = 6)
    private BigDecimal checkInLocationLon;

    @Column(name = "check_out_location_lat", precision = 9, scale = 6)
    private BigDecimal checkOutLocationLat;

    @Column(name = "check_out_location_lon", precision = 9, scale = 6)
    private BigDecimal checkOutLocationLon;

    // expected_start / expected_end (TIME) đọc ghi bằng JDBC: Hibernate jdbc.time_zone=UTC dịch cả giá trị TIME.

    @Column(name = "late_minutes", nullable = false)
    private int lateMinutes;

    @Column(name = "early_minutes", nullable = false)
    private int earlyMinutes;

    @Column(name = "check_in_distance_m")
    private Integer checkInDistanceM;

    @Column(name = "check_out_distance_m")
    private Integer checkOutDistanceM;

    /** SELF (tự chấm), EXCUSE (từ đơn giải trình đã duyệt), ADJUSTED (HR điều chỉnh). */
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private String source = "SELF";

    /** COMPLETED, MISSING_CHECK_OUT, MISSING_CHECK_IN. */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
