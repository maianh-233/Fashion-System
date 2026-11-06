package com.fashionsystem.fashion_system.attendance;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** DTO của module chấm công & nghỉ phép. */
public final class AttendanceDtos {
    private AttendanceDtos() {
    }

    // ------------------------------------------------------------------ quyền / danh mục

    public record Permissions(boolean view, boolean manage, boolean approveLeave, boolean export, boolean admin,
                              boolean globalAdmin) {
    }

    public record StoreRef(UUID id, String name) {
    }

    public record EmployeeRef(UUID id, String employeeCode, String fullName, UUID storeId, String storeName) {
    }

    public record Lookups(Permissions permissions, UUID scopeStoreId, String scopeStoreName, List<StoreRef> stores,
                          List<LeaveType> leaveTypes) {
    }

    // ------------------------------------------------------------------ chấm công

    public record CheckRequest(
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            /** Nhân viên Global chọn cửa hàng đang làm (không bắt buộc); nhân viên cửa hàng luôn là cửa hàng của mình. */
            UUID storeId) {
    }

    public record Record(UUID id, UUID userId, String employeeCode, String employeeName, String employeeType, UUID storeId,
                         String storeName, LocalDate date, LocalTime expectedStart, LocalTime expectedEnd,
                         LocalDateTime checkIn, LocalDateTime checkOut, BigDecimal checkInLatitude, BigDecimal checkInLongitude,
                         BigDecimal checkOutLatitude, BigDecimal checkOutLongitude, Integer checkInDistanceM,
                         Integer checkOutDistanceM, Long workedMinutes, int lateMinutes, int earlyMinutes, String status,
                         String source, String notes, boolean locked) {
    }

    public record Today(LocalDate date, String employeeType, String storeName, LocalTime expectedStart, LocalTime expectedEnd,
                        String holiday, boolean weeklyOff, String leave, Record record, boolean requireLocation) {
    }

    public record AdjustRequest(LocalDateTime checkIn, LocalDateTime checkOut, @NotBlank @Size(max = 1000) String reason) {
    }

    /** HR bổ sung ngày công cho nhân viên chưa có bản ghi (ví dụ máy chấm công hỏng). */
    public record RecordCreate(@NotNull UUID userId, @NotNull LocalDate date, @NotNull LocalDateTime checkIn,
                               @NotNull LocalDateTime checkOut, @NotBlank @Size(max = 1000) String reason) {
    }

    public record AuditEntry(String action, String reason, String actorName, LocalDateTime createdAt, String oldData,
                             String newData) {
    }

    public record RecordDetail(Record record, List<AuditEntry> history) {
    }

    // ------------------------------------------------------------------ đơn giải trình

    public record ExcuseCreate(@NotNull LocalDate date, @NotBlank String type, LocalDateTime proposedCheckIn,
                               LocalDateTime proposedCheckOut, @NotBlank @Size(max = 1000) String reason) {
    }

    public record Excuse(UUID id, String code, UUID userId, String employeeCode, String employeeName, String storeName,
                         LocalDate date, String type, LocalDateTime proposedCheckIn, LocalDateTime proposedCheckOut,
                         String reason, String status, String decidedByName, LocalDateTime decidedAt, String rejectionReason,
                         String approverNote, LocalDateTime createdAt) {
    }

    public record Decision(@Size(max = 1000) String note) {
    }

    public record Rejection(@NotBlank @Size(max = 1000) String reason) {
    }

    // ------------------------------------------------------------------ nghỉ phép

    public record LeaveType(UUID id, String code, String name, String description, boolean paid, BigDecimal annualQuota,
                            boolean requiresDocuments, boolean active) {
    }

    public record LeaveTypeRequest(@NotBlank @Size(max = 30) String code, @NotBlank @Size(max = 100) String name,
                                   @Size(max = 1000) String description, boolean paid,
                                   @DecimalMin("0") @DecimalMax("366") BigDecimal annualQuota, boolean requiresDocuments,
                                   boolean active) {
    }

    public record LeaveCreate(@NotNull UUID leaveTypeId, @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                              boolean halfDayStart, boolean halfDayEnd, @NotBlank @Size(max = 1000) String reason) {
    }

    public record Leave(UUID id, String code, UUID userId, String employeeCode, String employeeName, String storeName,
                        UUID leaveTypeId, String leaveTypeName, boolean paid, LocalDate startDate, LocalDate endDate,
                        boolean halfDayStart, boolean halfDayEnd, BigDecimal totalDays, String reason, String status,
                        String decidedByName, LocalDateTime decidedAt, String rejectionReason, String approverNote,
                        LocalDateTime createdAt) {
    }

    public record LeaveBalance(UUID leaveTypeId, String code, String name, boolean paid, BigDecimal quota, BigDecimal used,
                               BigDecimal pending, BigDecimal remaining) {
    }

    // ------------------------------------------------------------------ tăng ca

    public record OvertimeCreate(@NotNull LocalDate workDate, @NotNull @DecimalMin("0.5") @DecimalMax("12") BigDecimal hours,
                                 @NotBlank @Size(max = 1000) String reason) {
    }

    public record OvertimeApprove(@DecimalMin("0.5") @DecimalMax("12") BigDecimal approvedHours, @Size(max = 1000) String note) {
    }

    public record Overtime(UUID id, String code, UUID userId, String employeeCode, String employeeName, String storeName,
                           LocalDate workDate, BigDecimal hours, BigDecimal approvedHours, String type, String reason,
                           String status, String decidedByName, LocalDateTime decidedAt, String rejectionReason,
                           String approverNote, LocalDateTime createdAt) {
    }

    // ------------------------------------------------------------------ dashboard / cấu hình

    public record Dashboard(LocalDate date, int totalEmployees, int checkedIn, int notCheckedIn, int late, int earlyLeave,
                            int checkedOut, int missingCheckOut, int onLeave, int pendingExcuses, int pendingLeaves,
                            int pendingOvertime, int scheduledShifts, int absentShifts, int notYetCheckedIn,
                            int pendingSwaps) {
    }

    public record Settings(@NotEmpty List<@Min(1) @Max(7) Integer> weeklyOffDays,
                           @NotNull @DecimalMin("1") @DecimalMax("24") BigDecimal standardHoursPerDay,
                           @Min(20) @Max(5000) int storeRadiusMeters, boolean requireStoreLocation,
                           @NotNull LocalTime defaultCheckIn, @NotNull LocalTime defaultCheckOut,
                           @Min(0) @Max(240) int lateThresholdMinutes, @Min(0) @Max(240) int earlyThresholdMinutes) {
    }

    public record Holiday(@NotNull LocalDate date, @NotBlank @Size(max = 150) String name) {
    }
}
