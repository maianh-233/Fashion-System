package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.payroll.PayrollDtos.AfterCloseAdjustment;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Component;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.ComponentRequest;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.ConfirmDays;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.CostRow;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Dashboard;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.EmployeeIssue;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.EmployeeSalary;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.EmployeeSalaryDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.ItemRequest;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Lookups;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Note;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PaymentRequest;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PaymentResult;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodCreate;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodSummary;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Reason;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordRow;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Settings;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Structure;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.StructureRequest;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.SyncResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Tính lương cho HR. Quyền chức năng ở đây; phạm vi cửa hàng, hồ sơ bảo mật, không tự sửa lương của mình và trạng thái
 * kỳ lương kiểm tra trong service.
 */
@RestController
@RequestMapping("/api/admin/payroll")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)"
        + " and hasAnyAuthority('PAYROLL_VIEW', 'SALARY_VIEW', 'SALARY_MANAGE', 'PAYROLL_ADMIN')")
public class PayrollController {
    private static final String VIEW = "hasAnyAuthority('PAYROLL_VIEW', 'PAYROLL_ADMIN')";
    private static final String MANAGE = "hasAnyAuthority('PAYROLL_MANAGE', 'PAYROLL_ADMIN')";
    private static final String APPROVE = "hasAnyAuthority('PAYROLL_APPROVE', 'PAYROLL_ADMIN')";
    private static final String EXPORT = "hasAnyAuthority('PAYROLL_EXPORT', 'PAYROLL_ADMIN')";
    private static final String SALARY_VIEW = "hasAnyAuthority('SALARY_VIEW', 'SALARY_MANAGE', 'PAYROLL_ADMIN')";
    private static final String SALARY_MANAGE = "hasAnyAuthority('SALARY_MANAGE', 'PAYROLL_ADMIN')";
    private static final String ADMIN = "hasAuthority('PAYROLL_ADMIN')";
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PayrollAccess access;
    private final PayrollSettingsService settings;
    private final SalaryStructureService salaries;
    private final PayrollPeriodService periods;
    private final PayrollRecordService records;
    private final PayslipService payslips;
    private final PayrollReportService reports;

    public PayrollController(PayrollAccess access, PayrollSettingsService settings, SalaryStructureService salaries,
                             PayrollPeriodService periods, PayrollRecordService records, PayslipService payslips,
                             PayrollReportService reports) {
        this.access = access;
        this.settings = settings;
        this.salaries = salaries;
        this.periods = periods;
        this.records = records;
        this.payslips = payslips;
        this.reports = reports;
    }

    @GetMapping("/lookups")
    public Lookups lookups() {
        return settings.lookups(access.current());
    }

    // ------------------------------------------------------------------ cấu hình

    @PutMapping("/settings")
    @PreAuthorize(ADMIN)
    public Settings updateSettings(@Valid @RequestBody Settings request) {
        return settings.updateSettings(access.current(), request);
    }

    @PostMapping("/components")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public Component createComponent(@Valid @RequestBody ComponentRequest request) {
        return settings.saveComponent(access.current(), null, request);
    }

    @PutMapping("/components/{id}")
    @PreAuthorize(ADMIN)
    public Component updateComponent(@PathVariable UUID id, @Valid @RequestBody ComponentRequest request) {
        return settings.saveComponent(access.current(), id, request);
    }

    // ------------------------------------------------------------------ hồ sơ lương

    @GetMapping("/salaries")
    @PreAuthorize(SALARY_VIEW)
    public Page<EmployeeSalary> salaries(@RequestParam(required = false) String keyword, @RequestParam(required = false) UUID storeId,
                                         @RequestParam(required = false) String filter, @PageableDefault(size = 20) Pageable pageable) {
        return salaries.list(access.current(), keyword, storeId, filter, pageable);
    }

    @GetMapping("/salaries/{userId}")
    @PreAuthorize(SALARY_VIEW)
    public EmployeeSalaryDetail salary(@PathVariable UUID userId) {
        return salaries.detail(access.current(), userId);
    }

    @PostMapping("/salaries/{userId}/structures")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(SALARY_MANAGE)
    public Structure createStructure(@PathVariable UUID userId, @Valid @RequestBody StructureRequest request) {
        return salaries.create(access.current(), userId, request);
    }

    @PutMapping("/structures/{id}")
    @PreAuthorize(SALARY_MANAGE)
    public Structure updateStructure(@PathVariable UUID id, @Valid @RequestBody StructureRequest request) {
        return salaries.update(access.current(), id, request);
    }

    // ------------------------------------------------------------------ kỳ lương

    @GetMapping("/dashboard")
    @PreAuthorize(VIEW)
    public Dashboard dashboard(@RequestParam(required = false) String month, @RequestParam(required = false) UUID storeId) {
        return reports.dashboard(access.current(), month, storeId);
    }

    @GetMapping("/periods")
    @PreAuthorize(VIEW)
    public Page<PeriodSummary> periods(@RequestParam(required = false) String month, @RequestParam(required = false) UUID storeId,
                                       @RequestParam(required = false) String status, @PageableDefault(size = 20) Pageable pageable) {
        return periods.list(access.current(), month, storeId, status, pageable);
    }

    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public PeriodDetail createPeriod(@Valid @RequestBody PeriodCreate request) {
        return periods.create(access.current(), request);
    }

    @GetMapping("/periods/{id}")
    @PreAuthorize(VIEW)
    public PeriodDetail period(@PathVariable UUID id) {
        return periods.detail(access.current(), id);
    }

    @GetMapping("/periods/{id}/records")
    @PreAuthorize(VIEW)
    public Page<RecordRow> periodRecords(@PathVariable UUID id, @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String reviewStatus, @PageableDefault(size = 50) Pageable pageable) {
        return periods.records(access.current(), id, keyword, reviewStatus, pageable);
    }

    @PostMapping("/periods/{id}/sync")
    @PreAuthorize(MANAGE)
    public SyncResult sync(@PathVariable UUID id) {
        return periods.sync(access.current(), id);
    }

    @PostMapping("/periods/{id}/calculate")
    @PreAuthorize(MANAGE)
    public PeriodDetail calculate(@PathVariable UUID id) {
        return periods.calculate(access.current(), id);
    }

    @PostMapping("/periods/{id}/submit")
    @PreAuthorize(MANAGE)
    public PeriodDetail submit(@PathVariable UUID id, @Valid @RequestBody(required = false) Note note) {
        return periods.submit(access.current(), id, note == null ? null : note.note());
    }

    @PostMapping("/periods/{id}/approve")
    @PreAuthorize(APPROVE)
    public PeriodDetail approve(@PathVariable UUID id, @Valid @RequestBody(required = false) Note note) {
        return periods.approve(access.current(), id, note == null ? null : note.note());
    }

    @PostMapping("/periods/{id}/reject")
    @PreAuthorize(APPROVE)
    public PeriodDetail reject(@PathVariable UUID id, @Valid @RequestBody Reason reason) {
        return periods.reject(access.current(), id, reason.reason());
    }

    @PostMapping("/periods/{id}/cancel")
    @PreAuthorize(MANAGE)
    public PeriodDetail cancel(@PathVariable UUID id, @Valid @RequestBody Reason reason) {
        return periods.cancel(access.current(), id, reason.reason());
    }

    @PostMapping("/periods/{id}/pay")
    @PreAuthorize("hasAnyAuthority('PAYROLL_PAY', 'PAYROLL_ADMIN')")
    public PaymentResult pay(@PathVariable UUID id, @Valid @RequestBody(required = false) PaymentRequest request) {
        return periods.pay(access.current(), id, request);
    }

    @GetMapping("/periods/{id}/export")
    @PreAuthorize(EXPORT)
    public ResponseEntity<byte[]> exportPeriod(@PathVariable UUID id) {
        return file(reports.exportPeriod(access.current(), id), "bang-luong_" + id + ".xlsx", XLSX);
    }

    // ------------------------------------------------------------------ dòng lương

    @GetMapping("/records/{id}")
    @PreAuthorize(VIEW)
    public RecordDetail record(@PathVariable UUID id) {
        return records.detail(access.current(), id);
    }

    @PostMapping("/records/{id}/items")
    @PreAuthorize(MANAGE)
    public RecordDetail addItem(@PathVariable UUID id, @Valid @RequestBody ItemRequest request) {
        return records.addItem(access.current(), id, request);
    }

    @PostMapping("/records/{id}/items/{itemId}/remove")
    @PreAuthorize(MANAGE)
    public RecordDetail removeItem(@PathVariable UUID id, @PathVariable UUID itemId, @Valid @RequestBody Reason reason) {
        return records.removeItem(access.current(), id, itemId, reason.reason());
    }

    @PostMapping("/records/{id}/confirm-days")
    @PreAuthorize(MANAGE)
    public RecordDetail confirmDays(@PathVariable UUID id, @Valid @RequestBody ConfirmDays request) {
        return records.confirmDays(access.current(), id, request);
    }

    @PostMapping("/records/{id}/adjustments")
    @PreAuthorize(MANAGE)
    public RecordDetail afterClose(@PathVariable UUID id, @Valid @RequestBody AfterCloseAdjustment request) {
        return records.afterClose(access.current(), id, request);
    }

    @PostMapping("/records/{id}/adjustments/{adjustmentId}/cancel")
    @PreAuthorize(MANAGE)
    public RecordDetail cancelAdjustment(@PathVariable UUID id, @PathVariable UUID adjustmentId) {
        return records.cancelAdjustment(access.current(), id, adjustmentId);
    }

    @GetMapping("/records/{id}/payslip.pdf")
    @PreAuthorize(EXPORT)
    public ResponseEntity<byte[]> payslipPdf(@PathVariable UUID id) {
        return file(payslips.hrPdf(access.current(), id), "phieu-luong_" + id + ".pdf", MediaType.APPLICATION_PDF);
    }

    // ------------------------------------------------------------------ báo cáo

    @GetMapping("/reports/by-month")
    @PreAuthorize(VIEW)
    public List<CostRow> byMonth(@RequestParam(required = false) String month, @RequestParam(required = false) UUID storeId) {
        return reports.byMonth(access.current(), month, storeId);
    }

    @GetMapping("/reports/by-store")
    @PreAuthorize(VIEW)
    public List<CostRow> byStore(@RequestParam String month, @RequestParam(required = false) UUID storeId) {
        return reports.byStore(access.current(), month, storeId);
    }

    @GetMapping("/reports/by-department")
    @PreAuthorize(VIEW)
    public List<CostRow> byDepartment(@RequestParam String month, @RequestParam(required = false) UUID storeId) {
        return reports.byDepartment(access.current(), month, storeId);
    }

    @GetMapping("/reports/missing-salary")
    @PreAuthorize(VIEW)
    public List<EmployeeIssue> missingSalary(@RequestParam(required = false) UUID storeId) {
        return reports.missingSalary(access.current(), storeId);
    }

    @GetMapping("/reports/incomplete")
    @PreAuthorize(VIEW)
    public List<EmployeeIssue> incomplete(@RequestParam String month, @RequestParam(required = false) UUID storeId) {
        return reports.incomplete(access.current(), month, storeId);
    }

    @GetMapping("/reports/export")
    @PreAuthorize(EXPORT)
    public ResponseEntity<byte[]> exportReports(@RequestParam String month, @RequestParam(required = false) UUID storeId) {
        return file(reports.exportReports(access.current(), month, storeId), "bao-cao-luong_" + month + ".xlsx", XLSX);
    }

    static ResponseEntity<byte[]> file(byte[] body, String name, MediaType type) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .contentType(type).body(body);
    }
}
