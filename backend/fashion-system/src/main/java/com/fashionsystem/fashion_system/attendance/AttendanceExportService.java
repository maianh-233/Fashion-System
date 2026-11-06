package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Leave;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Overtime;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Record;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.excel.ExcelAlignment;
import com.fashionsystem.fashion_system.helper.excel.ExcelCellFormat;
import com.fashionsystem.fashion_system.helper.excel.ExcelColumn;
import com.fashionsystem.fashion_system.helper.excel.ExcelHelper;
import com.fashionsystem.fashion_system.helper.excel.ExcelSheet;
import com.fashionsystem.fashion_system.helper.excel.ExcelWorkbookData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Xuất Excel bảng công + đơn nghỉ / tăng ca trong phạm vi người xem. */
@Service
@Transactional(readOnly = true)
public class AttendanceExportService {
    private static final Map<String, String> STATUS = Map.of("COMPLETED", "Đủ công", "MISSING_CHECK_OUT", "Thiếu check-out",
            "MISSING_CHECK_IN", "Thiếu check-in", "WORKING", "Đang làm");
    private static final Map<String, String> REQUEST_STATUS = Map.of("PENDING", "Chờ duyệt", "APPROVED", "Đã duyệt",
            "REJECTED", "Từ chối", "CANCELLED", "Đã hủy");
    private static final Map<String, String> OT_TYPE = Map.of("WEEKDAY", "Ngày thường", "WEEKEND", "Ngày nghỉ tuần", "HOLIDAY", "Ngày lễ");
    private static final int MAX_ROWS = 20_000;

    private final AttendanceRecordService records;
    private final LeaveRequestService leaves;
    private final OvertimeRequestService overtimes;
    private final ExcelHelper excel;

    public AttendanceExportService(AttendanceRecordService records, LeaveRequestService leaves, OvertimeRequestService overtimes,
                                   ExcelHelper excel) {
        this.records = records;
        this.leaves = leaves;
        this.overtimes = overtimes;
        this.excel = excel;
    }

    public byte[] export(Actor actor, AttendanceRecordService.Filter filter) {
        if (!actor.export() && !actor.admin()) throw BusinessException.forbidden("Bạn không có quyền xuất dữ liệu chấm công");
        List<Record> rows = records.all(actor, filter);
        if (rows.size() > MAX_ROWS) throw BusinessException.badRequest("Quá nhiều dòng, hãy thu hẹp khoảng ngày hoặc chọn cửa hàng");
        RequestQueries.Filter requestFilter = new RequestQueries.Filter(null, filter.storeId(), filter.employeeType(), filter.keyword(),
                filter.from(), filter.to());
        Pageable all = Pageable.ofSize(MAX_ROWS);
        List<Leave> leaveRows = leaves.list(actor, requestFilter, all).getContent();
        List<Overtime> otRows = overtimes.list(actor, requestFilter, all).getContent();
        return excel.export(new ExcelWorkbookData(List.of(
                new ExcelSheet<>("Bảng công", List.of(
                        text("Ngày", Record::date), text("Mã NV", Record::employeeCode), text("Họ tên", Record::employeeName),
                        text("Loại NV", r -> "STORE".equals(r.employeeType()) ? "Cửa hàng" : "Global"),
                        text("Cửa hàng", Record::storeName), text("Giờ dự kiến", r -> r.expectedStart() + " - " + r.expectedEnd()),
                        dateTime("Giờ vào", Record::checkIn), dateTime("Giờ ra", Record::checkOut),
                        text("Vị trí vào", r -> point(r.checkInLatitude(), r.checkInLongitude())),
                        text("Vị trí ra", r -> point(r.checkOutLatitude(), r.checkOutLongitude())),
                        number("Phút làm", Record::workedMinutes), number("Phút trễ", Record::lateMinutes),
                        number("Phút về sớm", Record::earlyMinutes), text("Trạng thái", r -> STATUS.getOrDefault(r.status(), r.status())),
                        text("Nguồn", Record::source)), rows),
                new ExcelSheet<>("Nghỉ phép", List.of(
                        text("Mã đơn", Leave::code), text("Mã NV", Leave::employeeCode), text("Họ tên", Leave::employeeName),
                        text("Cửa hàng", Leave::storeName), text("Loại nghỉ", Leave::leaveTypeName),
                        text("Hưởng lương", l -> l.paid() ? "Có" : "Không"), text("Từ ngày", Leave::startDate),
                        text("Đến ngày", Leave::endDate), number("Số ngày", Leave::totalDays),
                        text("Trạng thái", l -> REQUEST_STATUS.getOrDefault(l.status(), l.status())), text("Lý do", Leave::reason)),
                        leaveRows),
                new ExcelSheet<>("Tăng ca", List.of(
                        text("Mã đơn", Overtime::code), text("Mã NV", Overtime::employeeCode), text("Họ tên", Overtime::employeeName),
                        text("Cửa hàng", Overtime::storeName), text("Ngày", Overtime::workDate),
                        text("Loại", o -> OT_TYPE.getOrDefault(o.type(), o.type())), number("Giờ đăng ký", Overtime::hours),
                        number("Giờ duyệt", Overtime::approvedHours),
                        text("Trạng thái", o -> REQUEST_STATUS.getOrDefault(o.status(), o.status())), text("Lý do", Overtime::reason)),
                        otRows))));
    }

    private static String point(BigDecimal lat, BigDecimal lon) {
        return lat == null || lon == null ? "" : lat.toPlainString() + ", " + lon.toPlainString();
    }

    private static <T> ExcelColumn<T> text(String header, Function<T, ?> value) {
        return new ExcelColumn<>(header, t -> {
            Object v = value.apply(t);
            return v instanceof LocalDate d ? d.toString() : v;
        });
    }

    private static <T> ExcelColumn<T> number(String header, Function<T, ?> value) {
        return new ExcelColumn<>(header, value, ExcelCellFormat.NUMBER, null, ExcelAlignment.RIGHT);
    }

    private static <T> ExcelColumn<T> dateTime(String header, Function<T, ?> value) {
        return new ExcelColumn<>(header, value, ExcelCellFormat.DATE_TIME, "dd/MM/yyyy HH:mm", ExcelAlignment.LEFT);
    }
}
