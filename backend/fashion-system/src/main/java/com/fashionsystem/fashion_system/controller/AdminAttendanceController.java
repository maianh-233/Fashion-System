package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.attendance.AttendanceRecordService;
import com.fashionsystem.fashion_system.attendance.ExcuseRequestService;
import com.fashionsystem.fashion_system.attendance.LeaveRequestService;
import com.fashionsystem.fashion_system.attendance.AttendanceExportService;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.*;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/admin/attendance")
@RequiredArgsConstructor
public class AdminAttendanceController {
    private final AttendanceRecordService attendanceRecordService;
    private final ExcuseRequestService excuseRequestService;
    private final LeaveRequestService leaveRequestService;
    private final AttendanceExportService exportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasPermission(authentication, 'ATTENDANCE_VIEW')")
    public ResponseEntity<DashboardSummaryResponse> getDashboard(
            Authentication authentication,
            @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok(new DashboardSummaryResponse(0, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    @GetMapping("/records")
    @PreAuthorize("hasPermission(authentication, 'ATTENDANCE_VIEW')")
    public ResponseEntity<AttendanceRecordsListResponse> getRecords(
            Authentication authentication,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AttendanceRecordsListResponse response = attendanceRecordService.searchRecords(
                storeId, fromDate, toDate, status, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/records/{recordId}/adjust")
    @PreAuthorize("hasPermission(authentication, 'ATTENDANCE_MANAGE')")
    public ResponseEntity<AttendanceResponse> adjustAttendance(
            Authentication authentication,
            @PathVariable UUID recordId,
            @Valid @RequestBody AdjustAttendanceRequest request) {
        UUID userId = extractUserId(authentication);
        AttendanceResponse response = attendanceRecordService.adjustAttendance(recordId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/excuses/{excuseId}/approve")
    @PreAuthorize("hasPermission(authentication, 'ATTENDANCE_MANAGE')")
    public ResponseEntity<ExcuseRequestResponse> approveExcuse(
            Authentication authentication,
            @PathVariable UUID excuseId,
            @Valid @RequestBody ApproveExcuseRequest request) {
        UUID userId = extractUserId(authentication);
        ExcuseRequestResponse response = excuseRequestService.approveExcuse(excuseId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/excuses/{excuseId}/reject")
    @PreAuthorize("hasPermission(authentication, 'ATTENDANCE_MANAGE')")
    public ResponseEntity<ExcuseRequestResponse> rejectExcuse(
            Authentication authentication,
            @PathVariable UUID excuseId,
            @Valid @RequestBody RejectExcuseRequest request) {
        UUID userId = extractUserId(authentication);
        ExcuseRequestResponse response = excuseRequestService.rejectExcuse(excuseId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/leave/{leaveId}/approve")
    @PreAuthorize("hasPermission(authentication, 'LEAVE_APPROVE')")
    public ResponseEntity<LeaveRequestResponse> approveLeave(
            Authentication authentication,
            @PathVariable UUID leaveId,
            @Valid @RequestBody ApproveLeaveRequest request) {
        UUID userId = extractUserId(authentication);
        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(leaveId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/leave/{leaveId}/reject")
    @PreAuthorize("hasPermission(authentication, 'LEAVE_APPROVE')")
    public ResponseEntity<LeaveRequestResponse> rejectLeave(
            Authentication authentication,
            @PathVariable UUID leaveId,
            @Valid @RequestBody RejectLeaveRequest request) {
        UUID userId = extractUserId(authentication);
        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(leaveId, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/records")
    @PreAuthorize("hasPermission(authentication, 'ATTENDANCE_EXPORT')")
    public ResponseEntity<byte[]> exportAttendanceRecords(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String status) {
        try {
            byte[] data = exportService.exportAttendanceToExcel(storeId, fromDate, toDate, status);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"attendance_" + fromDate + ".xlsx\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/export/leave-report")
    @PreAuthorize("hasPermission(authentication, 'ATTENDANCE_EXPORT')")
    public ResponseEntity<byte[]> exportLeaveReport(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        try {
            byte[] data = exportService.exportLeaveReportToExcel(fromDate, toDate);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"leave_report_" + fromDate + ".xlsx\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    private UUID extractUserId(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return user.userId();
    }
}
