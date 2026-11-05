package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.entity.LeaveRequest;
import com.fashionsystem.fashion_system.entity.LeaveType;
import com.fashionsystem.fashion_system.repository.LeaveRequestRepository;
import com.fashionsystem.fashion_system.repository.LeaveTypeRepository;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveBalanceResponse;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveBalancesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LeaveBalanceService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    private static final Map<String, BigDecimal> DEFAULT_LEAVE_ALLOCATION = new HashMap<>();

    static {
        DEFAULT_LEAVE_ALLOCATION.put("ANNUAL", new BigDecimal("12"));
        DEFAULT_LEAVE_ALLOCATION.put("SICK", new BigDecimal("5"));
        DEFAULT_LEAVE_ALLOCATION.put("PERSONAL", new BigDecimal("3"));
        DEFAULT_LEAVE_ALLOCATION.put("UNPAID", new BigDecimal("0"));
        DEFAULT_LEAVE_ALLOCATION.put("MATERNITY", new BigDecimal("0"));
        DEFAULT_LEAVE_ALLOCATION.put("FUNERAL", new BigDecimal("0"));
    }

    public LeaveBalancesResponse getLeaveBalance(UUID userId) {
        LocalDate currentYear = LocalDate.now();
        LocalDate yearStart = LocalDate.of(currentYear.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(currentYear.getYear(), 12, 31);

        List<LeaveType> leaveTypes = leaveTypeRepository.findByActiveTrueOrderByCode();
        List<LeaveBalanceResponse> balances = new java.util.ArrayList<>();

        for (LeaveType leaveType : leaveTypes) {
            BigDecimal totalDays = DEFAULT_LEAVE_ALLOCATION.getOrDefault(leaveType.getCode(), BigDecimal.ZERO);
            BigDecimal usedDays = calculateUsedLeave(userId, leaveType.getId(), yearStart, yearEnd);
            BigDecimal remainingDays = totalDays.subtract(usedDays);

            LeaveBalanceResponse balance = new LeaveBalanceResponse(
                    leaveType.getCode(),
                    leaveType.getName(),
                    totalDays,
                    usedDays,
                    remainingDays
            );
            balances.add(balance);
        }

        return new LeaveBalancesResponse(balances);
    }

    private BigDecimal calculateUsedLeave(UUID userId, UUID leaveTypeId, LocalDate fromDate, LocalDate toDate) {
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findOverlappingLeaves(
                userId, fromDate, toDate);

        return approvedLeaves.stream()
                .filter(lr -> lr.getLeaveTypeId().equals(leaveTypeId) && "APPROVED".equals(lr.getStatus()))
                .map(this::calculateLeaveDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateLeaveDays(LeaveRequest leave) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                leave.getStartDate(), leave.getEndDate()) + 1;

        BigDecimal totalDays = BigDecimal.valueOf(daysBetween);

        if (leave.getIsHalfDayStart() && leave.getIsHalfDayEnd() && leave.getStartDate().equals(leave.getEndDate())) {
            return BigDecimal.ONE;
        } else if (leave.getIsHalfDayStart() || leave.getIsHalfDayEnd()) {
            return totalDays.subtract(BigDecimal.valueOf(0.5));
        }

        return totalDays;
    }

    public BigDecimal getLeaveBalanceForType(UUID userId, String leaveTypeCode) {
        LeaveType leaveType = leaveTypeRepository.findByCode(leaveTypeCode)
                .orElse(null);

        if (leaveType == null) {
            return BigDecimal.ZERO;
        }

        LocalDate currentYear = LocalDate.now();
        LocalDate yearStart = LocalDate.of(currentYear.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(currentYear.getYear(), 12, 31);

        BigDecimal totalDays = DEFAULT_LEAVE_ALLOCATION.getOrDefault(leaveTypeCode, BigDecimal.ZERO);
        BigDecimal usedDays = calculateUsedLeave(userId, leaveType.getId(), yearStart, yearEnd);

        return totalDays.subtract(usedDays);
    }
}
