package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceDtos.AdjustRequest;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Dashboard;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Decision;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Excuse;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Holiday;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Leave;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveType;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveTypeRequest;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Lookups;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Overtime;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.OvertimeApprove;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Record;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.RecordCreate;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.RecordDetail;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Rejection;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Settings;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý chấm công cho HR / quản lý cửa hàng. Quyền chức năng kiểm tra tại đây; phạm vi cửa hàng, không tự duyệt
 * đơn của mình và khóa kỳ lương kiểm tra trong service.
 */
@RestController
@RequestMapping("/api/admin/attendance")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)"
        + " and hasAnyAuthority('ATTENDANCE_VIEW', 'LEAVE_VIEW', 'ATTENDANCE_ADMIN')")
public class AttendanceAdminController {
    private static final String MANAGE = "hasAnyAuthority('ATTENDANCE_MANAGE', 'ATTENDANCE_ADMIN')";
    private static final String APPROVE_LEAVE = "hasAnyAuthority('LEAVE_APPROVE', 'ATTENDANCE_ADMIN')";
    private static final String ADMIN = "hasAuthority('ATTENDANCE_ADMIN')";

    private final AttendanceAccess access;
    private final AttendanceAdminService admin;
    private final AttendanceRecordService records;
    private final ExcuseRequestService excuses;
    private final LeaveRequestService leaves;
    private final OvertimeRequestService overtimes;
    private final AttendanceExportService exports;

    public AttendanceAdminController(AttendanceAccess access, AttendanceAdminService admin, AttendanceRecordService records,
                                     ExcuseRequestService excuses, LeaveRequestService leaves, OvertimeRequestService overtimes,
                                     AttendanceExportService exports) {
        this.access = access;
        this.admin = admin;
        this.records = records;
        this.excuses = excuses;
        this.leaves = leaves;
        this.overtimes = overtimes;
        this.exports = exports;
    }

    @GetMapping("/lookups")
    public Lookups lookups() {
        return admin.lookups(access.current());
    }

    @GetMapping("/employees")
    public List<AttendanceDtos.EmployeeRef> employees(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) UUID storeId) {
        return admin.employees(access.current(), keyword, storeId);
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false) UUID storeId) {
        return admin.dashboard(access.current(), date, storeId);
    }

    // ------------------------------------------------------------------ bảng công

    @GetMapping("/records")
    public Page<Record> records(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                @RequestParam(required = false) UUID storeId, @RequestParam(required = false) String employeeType,
                                @RequestParam(required = false) String status, @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) UUID userId, @PageableDefault(size = 20) Pageable pageable) {
        return records.list(access.current(),
                new AttendanceRecordService.Filter(from, to, storeId, employeeType, status, keyword, userId), pageable);
    }

    @GetMapping("/records/{id}")
    public RecordDetail record(@PathVariable UUID id) {
        return records.detail(access.current(), id);
    }

    @PutMapping("/records/{id}")
    @PreAuthorize(MANAGE)
    public Record adjust(@PathVariable UUID id, @Valid @RequestBody AdjustRequest request) {
        return records.adjust(access.current(), id, request);
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public Record create(@Valid @RequestBody RecordCreate request) {
        return records.create(access.current(), request);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_EXPORT', 'ATTENDANCE_ADMIN')")
    public ResponseEntity<byte[]> export(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) UUID storeId,
                                         @RequestParam(required = false) String employeeType,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String keyword) {
        byte[] file = exports.export(access.current(),
                new AttendanceRecordService.Filter(from, to, storeId, employeeType, status, keyword, null));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("bang-cong_" + from + "_" + to + ".xlsx").build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    // ------------------------------------------------------------------ đơn

    @GetMapping("/excuses")
    public Page<Excuse> excuses(@RequestParam(required = false) String status, @RequestParam(required = false) UUID storeId,
                                @RequestParam(required = false) String employeeType, @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                @PageableDefault(size = 20) Pageable pageable) {
        return excuses.list(access.current(), new RequestQueries.Filter(status, storeId, employeeType, keyword, from, to), pageable);
    }

    @PostMapping("/excuses/{id}/approve")
    @PreAuthorize(MANAGE)
    public Excuse approveExcuse(@PathVariable UUID id, @Valid @RequestBody(required = false) Decision decision) {
        return excuses.approve(access.current(), id, decision);
    }

    @PostMapping("/excuses/{id}/reject")
    @PreAuthorize(MANAGE)
    public Excuse rejectExcuse(@PathVariable UUID id, @Valid @RequestBody Rejection rejection) {
        return excuses.reject(access.current(), id, rejection);
    }

    @GetMapping("/leaves")
    public Page<Leave> leaves(@RequestParam(required = false) String status, @RequestParam(required = false) UUID storeId,
                              @RequestParam(required = false) String employeeType, @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                              @PageableDefault(size = 20) Pageable pageable) {
        return leaves.list(access.current(), new RequestQueries.Filter(status, storeId, employeeType, keyword, from, to), pageable);
    }

    @PostMapping("/leaves/{id}/approve")
    @PreAuthorize(APPROVE_LEAVE)
    public Leave approveLeave(@PathVariable UUID id, @Valid @RequestBody(required = false) Decision decision) {
        return leaves.approve(access.current(), id, decision);
    }

    @PostMapping("/leaves/{id}/reject")
    @PreAuthorize(APPROVE_LEAVE)
    public Leave rejectLeave(@PathVariable UUID id, @Valid @RequestBody Rejection rejection) {
        return leaves.reject(access.current(), id, rejection);
    }

    @GetMapping("/overtime")
    public Page<Overtime> overtime(@RequestParam(required = false) String status, @RequestParam(required = false) UUID storeId,
                                   @RequestParam(required = false) String employeeType, @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @PageableDefault(size = 20) Pageable pageable) {
        return overtimes.list(access.current(), new RequestQueries.Filter(status, storeId, employeeType, keyword, from, to), pageable);
    }

    @PostMapping("/overtime/{id}/approve")
    @PreAuthorize(MANAGE)
    public Overtime approveOvertime(@PathVariable UUID id, @Valid @RequestBody(required = false) OvertimeApprove request) {
        return overtimes.approve(access.current(), id, request);
    }

    @PostMapping("/overtime/{id}/reject")
    @PreAuthorize(MANAGE)
    public Overtime rejectOvertime(@PathVariable UUID id, @Valid @RequestBody Rejection rejection) {
        return overtimes.reject(access.current(), id, rejection);
    }

    // ------------------------------------------------------------------ cấu hình

    @GetMapping("/settings")
    public Settings settings() {
        access.current();
        return admin.settings();
    }

    @PutMapping("/settings")
    @PreAuthorize(ADMIN)
    public Settings updateSettings(@Valid @RequestBody Settings request) {
        return admin.updateSettings(access.current(), request);
    }

    @GetMapping("/holidays")
    public List<Holiday> holidays(@RequestParam(required = false) Integer year) {
        access.current();
        return admin.holidays(year == null ? LocalDate.now().getYear() : year);
    }

    @PostMapping("/holidays")
    @PreAuthorize(ADMIN)
    public List<Holiday> addHoliday(@Valid @RequestBody Holiday holiday) {
        return admin.addHoliday(access.current(), holiday);
    }

    @DeleteMapping("/holidays/{date}")
    @PreAuthorize(ADMIN)
    public List<Holiday> deleteHoliday(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return admin.deleteHoliday(access.current(), date);
    }

    @PostMapping("/leave-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public LeaveType createLeaveType(@Valid @RequestBody LeaveTypeRequest request) {
        return leaves.saveType(access.current(), null, request);
    }

    @PutMapping("/leave-types/{id}")
    @PreAuthorize(ADMIN)
    public LeaveType updateLeaveType(@PathVariable UUID id, @Valid @RequestBody LeaveTypeRequest request) {
        return leaves.saveType(access.current(), id, request);
    }
}
