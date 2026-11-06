package com.fashionsystem.fashion_system.payroll;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** DTO của module tính lương. Số tiền là VND nguyên. */
public final class PayrollDtos {
    private PayrollDtos() {
    }

    // ------------------------------------------------------------------ quyền / danh mục / cấu hình

    public record Permissions(boolean view, boolean manage, boolean approve, boolean pay, boolean export, boolean salaryView,
                              boolean salaryManage, boolean admin, boolean globalAdmin) {
    }

    public record StoreRef(UUID id, String name) {
    }

    public record Lookups(Permissions permissions, UUID scopeStoreId, String scopeStoreName, List<StoreRef> stores,
                          List<Component> components, Settings settings) {
    }

    public record Settings(@NotBlank @Pattern(regexp = "CALENDAR|FIXED") String standardDaysMode,
                           @NotNull @DecimalMin("1") @DecimalMax("31") BigDecimal fixedStandardDays,
                           @NotNull @DecimalMin("1") @DecimalMax("10") BigDecimal otWeekdayRate,
                           @NotNull @DecimalMin("1") @DecimalMax("10") BigDecimal otWeekendRate,
                           @NotNull @DecimalMin("1") @DecimalMax("10") BigDecimal otHolidayRate,
                           int roundingUnit, @NotBlank @Pattern(regexp = "HALF_UP|DOWN|UP") String roundingMode) {
    }

    public record Component(UUID id, String code, String name, String kind, String calcMethod, BigDecimal defaultAmount,
                            BigDecimal defaultRate, boolean active, String note, int sortOrder) {
    }

    public record ComponentRequest(@NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9_]+") String code,
                                   @NotBlank @Size(max = 150) String name,
                                   @NotBlank @Pattern(regexp = "ALLOWANCE|BONUS|DEDUCTION") String kind,
                                   @NotBlank @Pattern(regexp = "FIXED|PRORATED|PER_WORKED_DAY|PERCENT_OF_BASE|PER_LATE_EARLY|PERFECT_ATTENDANCE|MANUAL")
                                   String calcMethod,
                                   @DecimalMin("0") BigDecimal defaultAmount, @DecimalMin("0") @DecimalMax("100") BigDecimal defaultRate,
                                   boolean active, @Size(max = 1000) String note, int sortOrder) {
    }

    // ------------------------------------------------------------------ hồ sơ lương

    public record StructureBrief(UUID id, BigDecimal baseSalary, LocalDate effectiveFrom, LocalDate effectiveTo, String payMethod,
                                 boolean confidential) {
    }

    public record EmployeeSalary(UUID userId, String employeeCode, String fullName, String employeeType, UUID storeId,
                                 String storeName, String departmentName, String positionName, LocalDate hireDate,
                                 LocalDate terminationDate, String employmentStatus, StructureBrief current, int versions) {
    }

    public record StructureItem(UUID componentId, String code, String name, String kind, String calcMethod, BigDecimal amount,
                                BigDecimal rate, String note) {
    }

    public record Structure(UUID id, UUID userId, BigDecimal baseSalary, LocalDate effectiveFrom, LocalDate effectiveTo,
                            String payMethod, String bankName, String bankAccount, boolean overtimeEligible,
                            BigDecimal overtimeHourlyRate, boolean confidential, String note, List<StructureItem> items,
                            String createdByName, LocalDateTime createdAt, boolean locked) {
    }

    public record StructureItemRequest(@NotNull UUID componentId, @DecimalMin("0") BigDecimal amount,
                                       @DecimalMin("0") @DecimalMax("100") BigDecimal rate, @Size(max = 500) String note) {
    }

    public record StructureRequest(@NotNull @DecimalMin("1") @DecimalMax("9999999999") BigDecimal baseSalary,
                                   @NotNull LocalDate effectiveFrom, LocalDate effectiveTo,
                                   @NotBlank @Pattern(regexp = "BANK_TRANSFER|CASH") String payMethod,
                                   @Size(max = 100) String bankName, @Size(max = 50) String bankAccount,
                                   boolean overtimeEligible, @DecimalMin("1") BigDecimal overtimeHourlyRate, boolean confidential,
                                   @Size(max = 1000) String note, @NotBlank @Size(max = 1000) String reason,
                                   @Valid List<StructureItemRequest> items) {
    }

    public record HistoryEntry(String action, String reason, String changedByName, LocalDateTime changedAt, JsonNode oldData,
                               JsonNode newData) {
    }

    public record EmployeeSalaryDetail(EmployeeSalary employee, List<Structure> structures, List<HistoryEntry> history) {
    }

    // ------------------------------------------------------------------ kỳ lương

    public record PeriodCreate(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String month, UUID storeId, @Size(max = 1000) String note) {
    }

    public record PeriodSummary(UUID id, String code, LocalDate periodMonth, LocalDate startDate, LocalDate endDate, UUID storeId,
                                String storeName, String status, BigDecimal standardDays, int employeeCount, int needsReviewCount,
                                int paidCount, BigDecimal totalGross, BigDecimal totalDeduction, BigDecimal totalNet,
                                LocalDateTime attendanceSyncedAt, LocalDateTime calculatedAt, LocalDateTime submittedAt,
                                String submittedByName, LocalDateTime approvedAt, String approvedByName, LocalDateTime paidAt,
                                String cancelReason, String lastRejectionReason, String note, String createdByName,
                                LocalDateTime createdAt) {
    }

    public record Approval(String action, String fromStatus, String toStatus, String note, String actorName, LocalDateTime createdAt) {
    }

    public record Actions(boolean sync, boolean calculate, boolean submit, boolean approve, boolean pay, boolean cancel,
                          boolean editRecords) {
    }

    public record PeriodDetail(PeriodSummary period, List<Approval> approvals, Actions actions, List<String> blockers) {
    }

    public record SkippedEmployee(UUID userId, String employeeCode, String fullName, String reason) {
    }

    public record SyncResult(int added, int updated, int removed, List<SkippedEmployee> skipped) {
    }

    // ------------------------------------------------------------------ bảng lương nhân viên

    public record RecordRow(UUID id, UUID periodId, LocalDate periodMonth, UUID userId, String employeeCode, String fullName, String employeeType,
                            String storeName, String departmentName, String positionName, BigDecimal baseSalary,
                            BigDecimal standardDays, BigDecimal payableDays, BigDecimal workedDays, BigDecimal paidLeaveDays,
                            BigDecimal unpaidLeaveDays, BigDecimal holidayDays, BigDecimal unverifiedDays,
                            BigDecimal confirmedAbsentDays, BigDecimal basePay, BigDecimal allowanceTotal, BigDecimal bonusTotal,
                            BigDecimal overtimePay, BigDecimal deductionTotal, BigDecimal grossPay, BigDecimal netPay,
                            String reviewStatus, String reviewNote, String paymentStatus, boolean calculated) {
    }

    public record Item(UUID id, UUID componentId, String code, String name, String kind, String calcMethod, BigDecimal quantity,
                       BigDecimal rate, BigDecimal amount, String source, String note) {
    }

    public record Adjustment(UUID id, String phase, String action, String kind, String name, BigDecimal amount, String reason,
                             String status, String appliedPeriodCode, String createdByName, LocalDateTime createdAt,
                             JsonNode oldValue, JsonNode newValue) {
    }

    public record Payment(BigDecimal amount, String method, String reference, LocalDateTime paidAt, String confirmedByName,
                          String note) {
    }

    public record RecordDetail(RecordRow row, String periodCode, LocalDate periodMonth, String periodStatus, LocalDate windowStart,
                               LocalDate windowEnd, BigDecimal workdaysInWindow, BigDecimal offDayWorkedDays, int workedMinutes,
                               int lateCount, int earlyCount, int lateMinutes, int earlyMinutes, int missingCheckOutCount,
                               int missingCheckInCount, int approvedExcuseCount, int pendingRequestCount,
                               BigDecimal otWeekdayHours, BigDecimal otWeekendHours, BigDecimal otHolidayHours, String payMethod,
                               String bankName, String bankAccount, LocalDateTime syncedAt, LocalDateTime calculatedAt,
                               List<Item> items, List<Adjustment> adjustments, Payment payment, JsonNode days,
                               JsonNode calculationDetail, boolean editable, boolean closed) {
    }

    public record ItemRequest(UUID componentId, @NotBlank @Size(max = 150) String name,
                              @NotBlank @Pattern(regexp = "ALLOWANCE|BONUS|DEDUCTION") String kind,
                              @NotNull @DecimalMin("0") @DecimalMax("9999999999") BigDecimal amount, @Size(max = 500) String note,
                              @NotBlank @Size(max = 1000) String reason) {
    }

    public record ConfirmDays(@NotNull @DecimalMin("0") @DecimalMax("31") BigDecimal confirmedAbsentDays,
                              @NotBlank @Size(max = 1000) String note) {
    }

    public record AfterCloseAdjustment(@NotBlank @Pattern(regexp = "ALLOWANCE|BONUS|DEDUCTION") String kind,
                                       @NotBlank @Size(max = 150) String name,
                                       @NotNull @DecimalMin("1") @DecimalMax("9999999999") BigDecimal amount,
                                       @NotBlank @Size(max = 1000) String reason) {
    }

    public record Reason(@NotBlank @Size(max = 1000) String reason) {
    }

    public record Note(@Size(max = 1000) String note) {
    }

    public record PaymentRequest(List<UUID> recordIds, @Size(max = 100) String reference, LocalDate paidDate,
                                 @Size(max = 500) String note) {
    }

    public record PaymentResult(int paid, int alreadyPaid, String periodStatus) {
    }

    // ------------------------------------------------------------------ dashboard / báo cáo

    public record PeriodBrief(UUID id, String code, String storeName, String status, int employeeCount, BigDecimal totalNet) {
    }

    public record Dashboard(LocalDate month, int employeeCount, BigDecimal totalGross, BigDecimal totalBaseSalary,
                            BigDecimal totalBasePay, BigDecimal totalAllowance, BigDecimal totalBonus, BigDecimal totalOvertime,
                            BigDecimal totalDeduction, BigDecimal totalNet, int pendingApproval, int paidPeriods, int needsReview,
                            int missingSalaryCount, List<PeriodBrief> periods) {
    }

    public record CostRow(String label, int employeeCount, BigDecimal totalBasePay, BigDecimal totalAllowance, BigDecimal totalBonus,
                          BigDecimal totalOvertime, BigDecimal totalDeduction, BigDecimal totalGross, BigDecimal totalNet) {
    }

    public record EmployeeIssue(UUID userId, String employeeCode, String fullName, String storeName, String issue) {
    }
}
