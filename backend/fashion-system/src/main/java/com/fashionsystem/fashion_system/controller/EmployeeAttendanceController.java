package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.attendance.AttendanceRecordService;
import com.fashionsystem.fashion_system.attendance.ExcuseRequestService;
import com.fashionsystem.fashion_system.attendance.LeaveRequestService;
import com.fashionsystem.fashion_system.attendance.LeaveBalanceService;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.*;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class EmployeeAttendanceController {
    private final AttendanceRecordService attendanceRecordService;
    private final ExcuseRequestService excuseRequestService;
    private final LeaveRequestService leaveRequestService;
    private final LeaveBalanceService leaveBalanceService;

    @PostMapping("/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkIn(
            Authentication authentication,
            @Valid @RequestBody CheckInRequest request) {
        UUID userId = extractUserId(authentication);
        AttendanceResponse response = attendanceRecordService.checkIn(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/check-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkOut(
            Authentication authentication,
            @Valid @RequestBody CheckOutRequest request) {
        UUID userId = extractUserId(authentication);
        AttendanceResponse response = attendanceRecordService.checkOut(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TodayAttendanceResponse> getTodayStatus(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        TodayAttendanceResponse response = attendanceRecordService.getTodayStatus(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceHistoryResponse> getHistory(
            Authentication authentication,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        UUID userId = extractUserId(authentication);
        AttendanceHistoryRequest request = new AttendanceHistoryRequest(fromDate, toDate, null);
        AttendanceHistoryResponse response = attendanceRecordService.getHistory(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/excuse")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExcuseRequestResponse> createExcuse(
            Authentication authentication,
            @Valid @RequestBody CreateExcuseRequest request) {
        UUID userId = extractUserId(authentication);
        ExcuseRequestResponse response = excuseRequestService.createExcuse(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/excuses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExcuseRequestListResponse> getMyExcuses(Authentication authentication) {
        return ResponseEntity.ok(new ExcuseRequestListResponse(
                new java.util.ArrayList<>(), 0));
    }

    @PostMapping("/leave-request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
            Authentication authentication,
            @Valid @RequestBody CreateLeaveRequest request) {
        UUID userId = extractUserId(authentication);
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/leave/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveBalancesResponse> getLeaveBalance(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        LeaveBalancesResponse response = leaveBalanceService.getLeaveBalance(userId);
        return ResponseEntity.ok(response);
    }

    private UUID extractUserId(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return user.userId();
    }
}
