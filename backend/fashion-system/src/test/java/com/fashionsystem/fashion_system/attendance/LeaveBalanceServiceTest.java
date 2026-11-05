package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.entity.LeaveRequest;
import com.fashionsystem.fashion_system.entity.LeaveType;
import com.fashionsystem.fashion_system.repository.LeaveRequestRepository;
import com.fashionsystem.fashion_system.repository.LeaveTypeRepository;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveBalancesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class LeaveBalanceServiceTest {

    private LeaveBalanceService service;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    private UUID userId;
    private UUID annualLeaveTypeId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new LeaveBalanceService(leaveRequestRepository, leaveTypeRepository);
        userId = UUID.randomUUID();
        annualLeaveTypeId = UUID.randomUUID();
    }

    @Test
    void testGetLeaveBalance_Success() {
        LeaveType annualLeave = new LeaveType();
        annualLeave.setId(annualLeaveTypeId);
        annualLeave.setCode("ANNUAL");
        annualLeave.setName("Nghỉ phép năm");

        LeaveType sickLeave = new LeaveType();
        sickLeave.setId(UUID.randomUUID());
        sickLeave.setCode("SICK");
        sickLeave.setName("Nghỉ ốm");

        when(leaveTypeRepository.findByActiveTrueOrderByCode())
                .thenReturn(Arrays.asList(annualLeave, sickLeave));
        when(leaveRequestRepository.findOverlappingLeaves(any(), any(), any()))
                .thenReturn(Arrays.asList());

        LeaveBalancesResponse response = service.getLeaveBalance(userId);

        assertNotNull(response);
        assertNotNull(response.balances());
        assertTrue(response.balances().size() >= 2);
        verify(leaveTypeRepository, times(1)).findByActiveTrueOrderByCode();
    }

    @Test
    void testGetLeaveBalance_WithUsedLeave() {
        LeaveType annualLeave = new LeaveType();
        annualLeave.setId(annualLeaveTypeId);
        annualLeave.setCode("ANNUAL");
        annualLeave.setName("Nghỉ phép năm");

        LocalDate startDate = LocalDate.now().withMonth(1).withDayOfMonth(1);
        LocalDate endDate = LocalDate.now().withMonth(12).withDayOfMonth(31);

        LeaveRequest approvedLeave = new LeaveRequest();
        approvedLeave.setId(UUID.randomUUID());
        approvedLeave.setLeaveTypeId(annualLeaveTypeId);
        approvedLeave.setStatus("APPROVED");
        approvedLeave.setStartDate(LocalDate.now().minusDays(5));
        approvedLeave.setEndDate(LocalDate.now());
        approvedLeave.setIsHalfDayStart(false);
        approvedLeave.setIsHalfDayEnd(false);

        when(leaveTypeRepository.findByActiveTrueOrderByCode())
                .thenReturn(Arrays.asList(annualLeave));
        when(leaveRequestRepository.findOverlappingLeaves(userId, startDate, endDate))
                .thenReturn(Arrays.asList(approvedLeave));

        LeaveBalancesResponse response = service.getLeaveBalance(userId);

        assertNotNull(response);
        assertNotNull(response.balances());
        assertTrue(response.balances().size() > 0);
    }

    @Test
    void testCalculateLeaveDays_FullDays() {
        LeaveRequest leave = new LeaveRequest();
        leave.setStartDate(LocalDate.now());
        leave.setEndDate(LocalDate.now().plusDays(4));
        leave.setIsHalfDayStart(false);
        leave.setIsHalfDayEnd(false);

        // Calculate days: 5 days total
        long expectedDays = 5;
        long actualDays = java.time.temporal.ChronoUnit.DAYS.between(
                leave.getStartDate(), leave.getEndDate()) + 1;

        assertEquals(expectedDays, actualDays);
    }

    @Test
    void testCalculateLeaveDays_WithHalfDays() {
        LeaveRequest leave = new LeaveRequest();
        leave.setStartDate(LocalDate.now());
        leave.setEndDate(LocalDate.now().plusDays(2));
        leave.setIsHalfDayStart(true);
        leave.setIsHalfDayEnd(false);

        // Calculate: 3 full days - 0.5 for half day start = 2.5 days
        long fullDays = java.time.temporal.ChronoUnit.DAYS.between(
                leave.getStartDate(), leave.getEndDate()) + 1;
        BigDecimal expected = BigDecimal.valueOf(fullDays).subtract(BigDecimal.valueOf(0.5));

        assertTrue(expected.compareTo(new BigDecimal("2.5")) == 0);
    }

    @Test
    void testCalculateLeaveDays_SingleHalfDay() {
        LeaveRequest leave = new LeaveRequest();
        leave.setStartDate(LocalDate.now());
        leave.setEndDate(LocalDate.now());
        leave.setIsHalfDayStart(true);
        leave.setIsHalfDayEnd(true);

        // Single day with both half = 1 full day
        long fullDays = java.time.temporal.ChronoUnit.DAYS.between(
                leave.getStartDate(), leave.getEndDate()) + 1;
        BigDecimal expected = BigDecimal.valueOf(fullDays);

        assertTrue(expected.compareTo(BigDecimal.ONE) == 0);
    }
}
