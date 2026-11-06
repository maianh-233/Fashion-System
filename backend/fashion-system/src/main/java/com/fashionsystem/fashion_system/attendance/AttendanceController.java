package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceDtos.CheckRequest;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Excuse;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.ExcuseCreate;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Leave;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveBalance;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveCreate;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveType;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Overtime;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.OvertimeCreate;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Record;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Today;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Chấm công cá nhân: mọi nhân viên đăng nhập, chỉ thao tác trên dữ liệu của chính mình. */
@RestController
@RequestMapping("/api/attendance")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AttendanceController {
    private final AttendanceAccess access;
    private final AttendanceRecordService records;
    private final ExcuseRequestService excuses;
    private final LeaveRequestService leaves;
    private final OvertimeRequestService overtimes;

    public AttendanceController(AttendanceAccess access, AttendanceRecordService records, ExcuseRequestService excuses,
                                LeaveRequestService leaves, OvertimeRequestService overtimes) {
        this.access = access;
        this.records = records;
        this.excuses = excuses;
        this.leaves = leaves;
        this.overtimes = overtimes;
    }

    @GetMapping("/today")
    public Today today() {
        return records.today(access.currentUserId());
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public Record checkIn(@Valid @RequestBody(required = false) CheckRequest request) {
        return records.checkIn(access.currentUserId(), request);
    }

    @PostMapping("/check-out")
    public Record checkOut(@Valid @RequestBody(required = false) CheckRequest request) {
        return records.checkOut(access.currentUserId(), request);
    }

    @GetMapping("/history")
    public List<Record> history(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return records.history(access.currentUserId(), from, to);
    }

    @GetMapping("/leave-types")
    public List<LeaveType> leaveTypes() {
        return leaves.types(true);
    }

    @GetMapping("/leave-balance")
    public List<LeaveBalance> leaveBalance(@RequestParam(required = false) Integer year) {
        return leaves.balance(access.currentUserId(), year == null ? LocalDate.now().getYear() : year);
    }

    @GetMapping("/excuses")
    public List<Excuse> myExcuses() {
        return excuses.mine(access.currentUserId());
    }

    @PostMapping("/excuses")
    @ResponseStatus(HttpStatus.CREATED)
    public Excuse createExcuse(@Valid @RequestBody ExcuseCreate request) {
        return excuses.create(access.currentUserId(), request);
    }

    @PostMapping("/excuses/{id}/cancel")
    public Excuse cancelExcuse(@PathVariable UUID id) {
        return excuses.cancel(access.currentUserId(), id);
    }

    @GetMapping("/leaves")
    public List<Leave> myLeaves() {
        return leaves.mine(access.currentUserId());
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public Leave createLeave(@Valid @RequestBody LeaveCreate request) {
        return leaves.create(access.currentUserId(), request);
    }

    @PostMapping("/leaves/{id}/cancel")
    public Leave cancelLeave(@PathVariable UUID id) {
        return leaves.cancel(access.currentUserId(), id);
    }

    @GetMapping("/overtime")
    public List<Overtime> myOvertime() {
        return overtimes.mine(access.currentUserId());
    }

    @PostMapping("/overtime")
    @ResponseStatus(HttpStatus.CREATED)
    public Overtime createOvertime(@Valid @RequestBody OvertimeCreate request) {
        return overtimes.create(access.currentUserId(), request);
    }

    @PostMapping("/overtime/{id}/cancel")
    public Overtime cancelOvertime(@PathVariable UUID id) {
        return overtimes.cancel(access.currentUserId(), id);
    }
}
