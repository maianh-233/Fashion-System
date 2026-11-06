package com.fashionsystem.fashion_system.attendance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** DTO của module xếp ca & đổi ca. */
public final class ShiftDtos {
    private ShiftDtos() {
    }

    // ------------------------------------------------------------------ ca mẫu

    /** @param storeId null = ca dùng chung toàn chuỗi */
    public record Template(UUID id, UUID storeId, String storeName, String code, String name, String shiftType, LocalTime startTime,
                           LocalTime endTime, int breakMinutes, String color, int sortOrder, boolean active, int paidMinutes) {
    }

    public record TemplateRequest(UUID storeId,
                                  @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Mã ca chỉ gồm chữ, số, - và _") String code,
                                  @NotBlank @Size(max = 100) String name,
                                  @NotBlank String shiftType,
                                  @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                                  @Min(0) @Max(240) int breakMinutes,
                                  @Size(max = 20) String color,
                                  @Min(0) @Max(999) int sortOrder,
                                  boolean active) {
    }

    // ------------------------------------------------------------------ lịch làm

    /**
     * Một ca trong lịch. {@code actualStatus}: đối chiếu với chấm công — UPCOMING, NOT_CHECKED_IN (đã vào ca chưa
     * check-in), ABSENT, ON_LEAVE, WORKING, ON_TIME, LATE, EARLY, LATE_EARLY, MISSING_CHECK_OUT, MISSING_CHECK_IN.
     */
    public record Entry(UUID id, UUID userId, String employeeCode, String employeeName, UUID storeId, String storeName,
                        LocalDate date, UUID shiftId, String shiftName, String color, LocalTime startTime, LocalTime endTime,
                        int breakMinutes, int paidMinutes, String status, String note, boolean editable, String actualStatus,
                        LocalDateTime checkIn, LocalDateTime checkOut, int lateMinutes, int earlyMinutes, boolean swapPending) {
    }

    public record EntryRequest(@NotNull UUID userId, @NotNull UUID storeId, @NotNull LocalDate date, UUID shiftId,
                               LocalTime startTime, LocalTime endTime, @Min(0) @Max(240) Integer breakMinutes,
                               @Size(max = 500) String note) {
    }

    public record GridEmployee(UUID id, String employeeCode, String fullName, String employeeType, boolean storeMember,
                               int scheduledMinutes, int shiftCount) {
    }

    public record GridDay(LocalDate date, String holiday, boolean weeklyOff, int shiftCount, int paidMinutes) {
    }

    public record Availability(UUID userId, LocalDate date, String kind, UUID preferredShiftId, String preferredShiftName,
                               String note) {
    }

    public record AvailabilityRequest(@NotNull LocalDate date, @NotBlank String kind, UUID preferredShiftId,
                                      @Size(max = 300) String note) {
    }

    public record LeaveDay(UUID userId, LocalDate date, String leaveType, boolean halfDay) {
    }

    public record GridStats(int shifts, int drafts, int published, int paidMinutes, int absent, int late, int early,
                            int pendingSwaps) {
    }

    public record Grid(UUID storeId, String storeName, LocalDate from, LocalDate to, List<GridDay> days,
                       List<GridEmployee> employees, List<Entry> entries, List<Availability> availability,
                       List<LeaveDay> leaves, List<Template> templates, GridStats stats, int weeklyHourLimit) {
    }

    public record PublishRequest(@NotNull UUID storeId, @NotNull LocalDate from, @NotNull LocalDate to) {
    }

    public record CopyWeekRequest(@NotNull UUID storeId, @NotNull LocalDate sourceWeek, @NotNull LocalDate targetWeek) {
    }

    public record BulkResult(int affected, int skipped, List<String> messages) {
    }

    // ------------------------------------------------------------------ đổi ca

    public record SwapCreate(@NotNull UUID scheduleId, @NotNull UUID targetUserId, UUID targetScheduleId,
                             @NotBlank @Size(max = 1000) String reason) {
    }

    public record PeerResponse(@Size(max = 1000) String note) {
    }

    public record Swap(UUID id, String code, UUID storeId, String storeName, String swapType, UUID requesterId,
                       String requesterCode, String requesterName, UUID requesterScheduleId, LocalDate requesterDate,
                       String requesterShift, UUID targetUserId, String targetCode, String targetName, UUID targetScheduleId,
                       LocalDate targetDate, String targetShift, String reason, String status, String peerNote,
                       LocalDateTime peerRespondedAt, String decidedByName, LocalDateTime decidedAt, String rejectionReason,
                       String approverNote, LocalDateTime createdAt) {
    }

    /** Yêu cầu đổi ca của chính mình (gửi đi + được nhờ), kèm id người xem để phân biệt hai chiều. */
    public record MySwaps(UUID userId, List<Swap> swaps) {
    }

    /** Đồng nghiệp có thể nhận / đổi ca: rảnh ngày đó hay không, và các ca đã công bố sắp tới để chọn đổi. */
    public record Colleague(UUID id, String employeeCode, String fullName, boolean freeOnDate, String availability,
                            List<Entry> upcoming) {
    }
}
