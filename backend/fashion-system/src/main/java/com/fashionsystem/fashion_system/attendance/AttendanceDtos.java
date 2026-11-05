package com.fashionsystem.fashion_system.attendance;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class AttendanceDtos {

    // ========== CHECK-IN/CHECK-OUT ==========

    public record CheckInRequest(
            @JsonProperty("store_id") UUID storeId,
            BigDecimal latitude,
            BigDecimal longitude) {}

    public record CheckOutRequest(
            BigDecimal latitude,
            BigDecimal longitude) {}

    public record AttendanceResponse(
            UUID id,
            @JsonProperty("user_id") UUID userId,
            @JsonProperty("attendance_date") LocalDate attendanceDate,
            @JsonProperty("store_id") UUID storeId,
            @JsonProperty("shift_id") UUID shiftId,
            @JsonProperty("check_in_time") LocalDateTime checkInTime,
            @JsonProperty("check_out_time") LocalDateTime checkOutTime,
            @JsonProperty("check_in_location_lat") BigDecimal checkInLocationLat,
            @JsonProperty("check_in_location_lon") BigDecimal checkInLocationLon,
            @JsonProperty("check_out_location_lat") BigDecimal checkOutLocationLat,
            @JsonProperty("check_out_location_lon") BigDecimal checkOutLocationLon,
            String status,
            String notes,
            @JsonProperty("created_at") LocalDateTime createdAt) {}

    public record TodayAttendanceResponse(
            UUID id,
            @JsonProperty("checked_in") Boolean checkedIn,
            @JsonProperty("checked_out") Boolean checkedOut,
            @JsonProperty("check_in_time") LocalDateTime checkInTime,
            @JsonProperty("check_out_time") LocalDateTime checkOutTime,
            @JsonProperty("location_captured") Boolean locationCaptured,
            @JsonProperty("duration_minutes") Long durationMinutes,
            String status,
            @JsonProperty("shift_time") String shiftTime) {}

    // ========== ATTENDANCE HISTORY ==========

    public record AttendanceHistoryRequest(
            @JsonProperty("from_date") @NotNull LocalDate fromDate,
            @JsonProperty("to_date") @NotNull LocalDate toDate,
            @JsonProperty("store_id") UUID storeId) {}

    public record AttendanceHistoryResponse(
            List<AttendanceResponse> records,
            int total) {}

    // ========== EXCUSE REQUESTS ==========

    public record CreateExcuseRequest(
            @JsonProperty("attendance_date") @NotNull LocalDate attendanceDate,
            @JsonProperty("excuse_type") @NotNull String excuseType,
            @JsonProperty("proposed_check_in") LocalDateTime proposedCheckIn,
            @JsonProperty("proposed_check_out") LocalDateTime proposedCheckOut,
            @NotBlank String reason) {}

    public record ExcuseRequestResponse(
            UUID id,
            String code,
            @JsonProperty("user_id") UUID userId,
            @JsonProperty("attendance_date") LocalDate attendanceDate,
            @JsonProperty("excuse_type") String excuseType,
            @JsonProperty("proposed_check_in") LocalDateTime proposedCheckIn,
            @JsonProperty("proposed_check_out") LocalDateTime proposedCheckOut,
            String reason,
            String status,
            @JsonProperty("approved_by") UUID approvedBy,
            @JsonProperty("approved_at") LocalDateTime approvedAt,
            @JsonProperty("created_at") LocalDateTime createdAt) {}

    public record ApproveExcuseRequest(
            @JsonProperty("proposed_check_in") LocalDateTime proposedCheckIn,
            @JsonProperty("proposed_check_out") LocalDateTime proposedCheckOut) {}

    public record RejectExcuseRequest(
            @NotBlank String reason) {}

    public record ExcuseRequestListResponse(
            List<ExcuseRequestResponse> requests,
            int total) {}

    // ========== LEAVE TYPES ==========

    public record LeaveTypeResponse(
            UUID id,
            String code,
            String name,
            String description,
            @JsonProperty("requires_approval") Boolean requiresApproval,
            @JsonProperty("requires_documents") Boolean requiresDocuments) {}

    public record LeaveTypesResponse(
            List<LeaveTypeResponse> leaveTypes) {}

    // ========== LEAVE REQUESTS ==========

    public record CreateLeaveRequest(
            @JsonProperty("leave_type_id") @NotNull UUID leaveTypeId,
            @JsonProperty("start_date") @NotNull LocalDate startDate,
            @JsonProperty("end_date") @NotNull LocalDate endDate,
            @JsonProperty("is_half_day_start") Boolean isHalfDayStart,
            @JsonProperty("is_half_day_end") Boolean isHalfDayEnd,
            String reason) {}

    public record LeaveRequestResponse(
            UUID id,
            String code,
            @JsonProperty("user_id") UUID userId,
            @JsonProperty("leave_type_id") UUID leaveTypeId,
            @JsonProperty("leave_type_name") String leaveTypeName,
            @JsonProperty("start_date") LocalDate startDate,
            @JsonProperty("end_date") LocalDate endDate,
            @JsonProperty("is_half_day_start") Boolean isHalfDayStart,
            @JsonProperty("is_half_day_end") Boolean isHalfDayEnd,
            @JsonProperty("store_id") UUID storeId,
            String reason,
            String status,
            @JsonProperty("approved_by") UUID approvedBy,
            @JsonProperty("approved_at") LocalDateTime approvedAt,
            @JsonProperty("created_at") LocalDateTime createdAt) {}

    public record ApproveLeaveRequest(
            @JsonProperty("approved_start_date") LocalDate approvedStartDate,
            @JsonProperty("approved_end_date") LocalDate approvedEndDate,
            String notes) {}

    public record RejectLeaveRequest(
            @NotBlank String reason) {}

    public record LeaveRequestListResponse(
            List<LeaveRequestResponse> requests,
            int total) {}

    public record LeaveBalanceResponse(
            @JsonProperty("leave_type_code") String leaveTypeCode,
            @JsonProperty("leave_type_name") String leaveTypeName,
            @JsonProperty("total_days") BigDecimal totalDays,
            @JsonProperty("used_days") BigDecimal usedDays,
            @JsonProperty("remaining_days") BigDecimal remainingDays) {}

    public record LeaveBalancesResponse(
            List<LeaveBalanceResponse> balances) {}

    // ========== HR MANAGEMENT ==========

    public record DashboardSummaryResponse(
            @JsonProperty("total_employees") Integer totalEmployees,
            @JsonProperty("checked_in_today") Integer checkedInToday,
            @JsonProperty("not_checked_in") Integer notCheckedIn,
            @JsonProperty("late_arrivals") Integer lateArrivals,
            @JsonProperty("early_departures") Integer earlyDepartures,
            @JsonProperty("checked_out") Integer checkedOut,
            @JsonProperty("missing_check_out") Integer missingCheckOut,
            @JsonProperty("on_leave_today") Integer onLeaveToday,
            @JsonProperty("pending_approvals") Integer pendingApprovals) {}

    public record AttendanceRecordsRequest(
            @JsonProperty("from_date") @NotNull LocalDate fromDate,
            @JsonProperty("to_date") @NotNull LocalDate toDate,
            @JsonProperty("store_id") UUID storeId,
            @JsonProperty("employee_id") UUID employeeId,
            String status,
            int page,
            int size) {}

    public record AttendanceRecordDetailResponse(
            UUID id,
            @JsonProperty("employee_id") UUID employeeId,
            @JsonProperty("employee_code") String employeeCode,
            @JsonProperty("employee_name") String employeeName,
            @JsonProperty("store_id") UUID storeId,
            @JsonProperty("store_name") String storeName,
            @JsonProperty("attendance_date") LocalDate attendanceDate,
            @JsonProperty("shift_name") String shiftName,
            @JsonProperty("check_in_time") LocalDateTime checkInTime,
            @JsonProperty("check_out_time") LocalDateTime checkOutTime,
            @JsonProperty("total_minutes") Long totalMinutes,
            String status,
            @JsonProperty("is_late") Boolean isLate,
            @JsonProperty("is_early_departure") Boolean isEarlyDeparture) {}

    public record AttendanceRecordsListResponse(
            List<AttendanceRecordDetailResponse> records,
            int total,
            int page,
            int pageSize) {}

    public record AdjustAttendanceRequest(
            @JsonProperty("new_check_in") LocalDateTime newCheckIn,
            @JsonProperty("new_check_out") LocalDateTime newCheckOut,
            @NotBlank String reason) {}

    // ========== SHIFTS & CONFIG ==========

    public record ShiftResponse(
            UUID id,
            String code,
            String name,
            @JsonProperty("shift_type") String shiftType,
            @JsonProperty("start_time") LocalTime startTime,
            @JsonProperty("end_time") LocalTime endTime,
            Boolean active) {}

    public record ShiftsResponse(
            List<ShiftResponse> shifts) {}

    public record CreateShiftRequest(
            @NotBlank String code,
            @NotBlank String name,
            @JsonProperty("shift_type") @NotBlank String shiftType,
            @JsonProperty("start_time") @NotNull LocalTime startTime,
            @JsonProperty("end_time") @NotNull LocalTime endTime) {}

    public record UpdateAttendanceConfigRequest(
            @JsonProperty("check_in_time") @NotNull LocalTime checkInTime,
            @JsonProperty("check_out_time") @NotNull LocalTime checkOutTime,
            @JsonProperty("break_duration_minutes") Integer breakDurationMinutes,
            @JsonProperty("late_threshold_minutes") Integer lateThresholdMinutes,
            @JsonProperty("early_departure_threshold_minutes") Integer earlyDepartureThresholdMinutes) {}
}
