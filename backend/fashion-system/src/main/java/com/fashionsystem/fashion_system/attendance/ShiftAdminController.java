package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Decision;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Rejection;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.BulkResult;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.CopyWeekRequest;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Entry;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.EntryRequest;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Grid;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.PublishRequest;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Swap;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Template;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.TemplateRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
 * Xếp ca cho quản lý cửa hàng / HR. Quyền chức năng (xem / xếp ca) kiểm tra tại đây; phạm vi cửa hàng và không tự
 * duyệt đổi ca của mình kiểm tra trong service.
 */
@RestController
@RequestMapping("/api/admin/shifts")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)"
        + " and hasAnyAuthority('SHIFT_VIEW', 'SHIFT_MANAGE', 'ATTENDANCE_ADMIN')")
public class ShiftAdminController {
    private static final String MANAGE = "hasAnyAuthority('SHIFT_MANAGE', 'ATTENDANCE_ADMIN')";

    private final AttendanceAccess access;
    private final ShiftTemplateService templates;
    private final ShiftScheduleService schedules;
    private final ShiftSwapService swaps;

    public ShiftAdminController(AttendanceAccess access, ShiftTemplateService templates, ShiftScheduleService schedules,
                                ShiftSwapService swaps) {
        this.access = access;
        this.templates = templates;
        this.schedules = schedules;
        this.swaps = swaps;
    }

    // ------------------------------------------------------------------ ca mẫu

    @GetMapping("/templates")
    public List<Template> templates(@RequestParam(required = false) UUID storeId,
                                    @RequestParam(defaultValue = "false") boolean includeInactive) {
        return templates.list(access.current(), storeId, includeInactive);
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public Template createTemplate(@Valid @RequestBody TemplateRequest request) {
        return templates.save(access.current(), null, request);
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize(MANAGE)
    public Template updateTemplate(@PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        return templates.save(access.current(), id, request);
    }

    // ------------------------------------------------------------------ lịch tuần

    @GetMapping("/schedule")
    public Grid schedule(@RequestParam(required = false) UUID storeId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        return schedules.grid(access.current(), storeId, week);
    }

    @PostMapping("/schedule")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public Entry create(@Valid @RequestBody EntryRequest request) {
        return schedules.create(access.current(), request);
    }

    @PutMapping("/schedule/{id}")
    @PreAuthorize(MANAGE)
    public Entry update(@PathVariable UUID id, @Valid @RequestBody EntryRequest request) {
        return schedules.update(access.current(), id, request);
    }

    @DeleteMapping("/schedule/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void delete(@PathVariable UUID id) {
        schedules.delete(access.current(), id);
    }

    @PostMapping("/schedule/publish")
    @PreAuthorize(MANAGE)
    public BulkResult publish(@Valid @RequestBody PublishRequest request) {
        return schedules.publish(access.current(), request);
    }

    @PostMapping("/schedule/copy-week")
    @PreAuthorize(MANAGE)
    public BulkResult copyWeek(@Valid @RequestBody CopyWeekRequest request) {
        return schedules.copyWeek(access.current(), request);
    }

    // ------------------------------------------------------------------ đổi ca

    @GetMapping("/swaps")
    public Page<Swap> swaps(@RequestParam(required = false) String status, @RequestParam(required = false) UUID storeId,
                            @RequestParam(required = false) String keyword, @PageableDefault(size = 20) Pageable pageable) {
        return swaps.list(access.current(), new ShiftSwapService.Filter(status, storeId, keyword), pageable);
    }

    @PostMapping("/swaps/{id}/approve")
    @PreAuthorize(MANAGE)
    public Swap approve(@PathVariable UUID id, @Valid @RequestBody(required = false) Decision decision) {
        return swaps.approve(access.current(), id, decision);
    }

    @PostMapping("/swaps/{id}/reject")
    @PreAuthorize(MANAGE)
    public Swap reject(@PathVariable UUID id, @Valid @RequestBody Rejection rejection) {
        return swaps.reject(access.current(), id, rejection);
    }
}
