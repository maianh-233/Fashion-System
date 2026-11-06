package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.pdf.PdfAlignment;
import com.fashionsystem.fashion_system.helper.pdf.PdfDocumentData;
import com.fashionsystem.fashion_system.helper.pdf.PdfElement;
import com.fashionsystem.fashion_system.helper.pdf.PdfHelper;
import com.fashionsystem.fashion_system.helper.pdf.PdfTable;
import com.fashionsystem.fashion_system.helper.pdf.PdfText;
import com.fashionsystem.fashion_system.payroll.PayrollAccess.Actor;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Item;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordRow;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu lương: nhân viên chỉ xem phiếu của chính mình và chỉ khi kỳ lương đã chốt (phát hành). Dữ liệu phiếu là bản
 * chụp lúc chốt nên không đổi theo hồ sơ nhân viên / chấm công về sau.
 */
@Service
@Transactional(readOnly = true)
public class PayslipService {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> KIND = Map.of("ALLOWANCE", "Phụ cấp", "BONUS", "Thưởng", "OVERTIME", "Tăng ca",
            "DEDUCTION", "Khấu trừ");

    private final NamedParameterJdbcTemplate jdbc;
    private final PayrollRecordService records;
    private final PdfHelper pdf;

    public PayslipService(NamedParameterJdbcTemplate jdbc, PayrollRecordService records, PdfHelper pdf) {
        this.jdbc = jdbc;
        this.records = records;
        this.pdf = pdf;
    }

    public List<RecordRow> mine(UUID userId) {
        return jdbc.query("""
                select r.* from payroll_records r join payroll_periods p on p.id = r.period_id
                where r.user_id = :u and r.active and p.status in ('APPROVED', 'PAID') order by r.period_month desc
                """, new MapSqlParameterSource("u", userId), PayrollPeriodService::mapRow);
    }

    public RecordDetail mineDetail(UUID userId, UUID recordId) {
        Boolean own = jdbc.queryForObject("""
                select exists (select 1 from payroll_records r join payroll_periods p on p.id = r.period_id
                               where r.id = :id and r.user_id = :u and r.active and p.status in ('APPROVED', 'PAID'))
                """, new MapSqlParameterSource("id", recordId).addValue("u", userId), Boolean.class);
        // Không phân biệt "không tồn tại" và "của người khác" để không lộ thông tin.
        if (!Boolean.TRUE.equals(own)) throw BusinessException.notFound("Phiếu lương không tồn tại");
        return withoutInternal(records.load(recordId, false));
    }

    public byte[] minePdf(UUID userId, UUID recordId) {
        return render(mineDetail(userId, recordId));
    }

    /** HR xuất phiếu lương PDF (trong phạm vi, mọi trạng thái — bản nháp có ghi chú). */
    public byte[] hrPdf(Actor actor, UUID recordId) {
        PayrollAccess.require(actor.permissions().export(), "Bạn không có quyền xuất phiếu lương");
        return render(records.detail(actor, recordId));
    }

    /** Nhân viên không cần thấy lịch sử điều chỉnh nội bộ (lý do / người sửa). */
    private static RecordDetail withoutInternal(RecordDetail d) {
        return new RecordDetail(d.row(), d.periodCode(), d.periodMonth(), d.periodStatus(), d.windowStart(), d.windowEnd(),
                d.workdaysInWindow(), d.offDayWorkedDays(), d.workedMinutes(), d.lateCount(), d.earlyCount(), d.lateMinutes(),
                d.earlyMinutes(), d.missingCheckOutCount(), d.missingCheckInCount(), d.approvedExcuseCount(), d.pendingRequestCount(),
                d.otWeekdayHours(), d.otWeekendHours(), d.otHolidayHours(), d.payMethod(), d.bankName(), maskAccount(d.bankAccount()),
                d.syncedAt(), d.calculatedAt(), d.items(), List.of(), d.payment(), d.days(), d.calculationDetail(), false, d.closed());
    }

    private static String maskAccount(String account) {
        if (account == null || account.length() <= 4) return account;
        return "•".repeat(account.length() - 4) + account.substring(account.length() - 4);
    }

    byte[] render(RecordDetail d) {
        NumberFormat money = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        RecordRow r = d.row();
        List<PdfElement> el = new ArrayList<>();
        if (!d.closed()) el.add(new PdfText("BẢN NHÁP — bảng lương chưa được phê duyệt", 10, true, PdfAlignment.CENTER, 8));
        el.add(new PdfText("Kỳ lương: " + d.periodMonth().format(MONTH) + "   (" + d.periodCode() + ")", 11, false, PdfAlignment.LEFT, 4));
        el.add(new PdfText("Nhân viên: " + r.fullName() + "   Mã NV: " + nz(r.employeeCode()), 11, true, PdfAlignment.LEFT, 4));
        el.add(new PdfText("Cửa hàng / phòng ban: " + (r.storeName() != null ? r.storeName() : "Văn phòng")
                + (r.departmentName() != null ? " · " + r.departmentName() : "") + "   Chức vụ: " + nz(r.positionName()),
                10, false, PdfAlignment.LEFT, 4));
        el.add(new PdfText("Thời gian tính lương: " + d.windowStart().format(DAY) + " – " + d.windowEnd().format(DAY), 10, false,
                PdfAlignment.LEFT, 10));

        el.add(new PdfText("NGÀY CÔNG", 11, true, PdfAlignment.LEFT, 4));
        el.add(new PdfTable(List.of("Ngày công chuẩn", "Ngày được trả lương", "Đi làm", "Nghỉ có lương", "Nghỉ không lương", "Ngày lễ"),
                List.of(List.of(days(r.standardDays()), days(r.payableDays()), days(r.workedDays()), days(r.paidLeaveDays()),
                        days(r.unpaidLeaveDays().add(r.confirmedAbsentDays())), days(r.holidayDays()))), List.of(), 9));
        el.add(new PdfText("Đi trễ: " + d.lateCount() + " lần · Về sớm: " + d.earlyCount() + " lần · Tăng ca: "
                + days(d.otWeekdayHours().add(d.otWeekendHours()).add(d.otHolidayHours())) + " giờ", 9, false, PdfAlignment.LEFT, 10));

        List<List<String>> income = new ArrayList<>();
        income.add(List.of("Lương cơ bản (hợp đồng)", "", money.format(r.baseSalary())));
        income.add(List.of("Lương theo ngày công", days(r.payableDays()) + "/" + days(r.standardDays()) + " ngày", money.format(r.basePay())));
        List<List<String>> deductions = new ArrayList<>();
        for (Item item : d.items()) {
            List<String> line = List.of(KIND.getOrDefault(item.kind(), "") + ": " + item.name(), item.note() == null ? "" : item.note(),
                    money.format(item.amount()));
            if ("DEDUCTION".equals(item.kind())) deductions.add(line);
            else income.add(line);
        }
        el.add(new PdfText("THU NHẬP", 11, true, PdfAlignment.LEFT, 4));
        el.add(new PdfTable(List.of("Khoản", "Ghi chú", "Số tiền (₫)"), income, List.of(0.42f, 0.36f, 0.22f), 9));
        el.add(new PdfText("Tổng thu nhập: " + money.format(r.grossPay()) + " ₫", 11, true, PdfAlignment.RIGHT, 8));
        el.add(new PdfText("KHẤU TRỪ", 11, true, PdfAlignment.LEFT, 4));
        if (deductions.isEmpty()) el.add(new PdfText("Không có khoản khấu trừ", 9, false, PdfAlignment.LEFT, 4));
        else el.add(new PdfTable(List.of("Khoản", "Ghi chú", "Số tiền (₫)"), deductions, List.of(0.42f, 0.36f, 0.22f), 9));
        el.add(new PdfText("Tổng khấu trừ: " + money.format(r.deductionTotal()) + " ₫", 11, true, PdfAlignment.RIGHT, 10));
        el.add(new PdfText("THỰC NHẬN: " + money.format(r.netPay()) + " ₫", 14, true, PdfAlignment.RIGHT, 8));
        String payment = "PAID".equals(r.paymentStatus()) && d.payment() != null
                ? "Đã thanh toán ngày " + d.payment().paidAt().format(DAY) + (d.payment().reference() != null ? " · " + d.payment().reference() : "")
                : "Chưa thanh toán";
        el.add(new PdfText("Hình thức: " + ("CASH".equals(d.payMethod()) ? "Tiền mặt" : "Chuyển khoản"
                + (d.bankAccount() != null ? " · " + nz(d.bankName()) + " " + d.bankAccount() : "")) + " · " + payment, 9, false, PdfAlignment.LEFT, 4));
        return pdf.generatePdf(new PdfDocumentData("PHIẾU LƯƠNG THÁNG " + d.periodMonth().format(MONTH), "Lunaria Fashion · Phiếu lương",
                "Tài liệu bảo mật — chỉ dành cho người nhận", el, 48, 48, 54, 48));
    }

    private static String days(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String nz(String value) {
        return value == null ? "—" : value;
    }
}
