package com.fashionsystem.fashion_system.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Công thức lương (thuần, không truy cập DB):
 * <pre>
 * Lương theo công  = Σ (lương cơ bản phiên bản × ngày được trả lương trong phiên bản) / ngày công chuẩn
 *                    (tổng ngày được trả lương vượt ngày công chuẩn thì quy về đúng ngày công chuẩn)
 * Tiền tăng ca     = đơn giá giờ × Σ (giờ đã duyệt × hệ số ngày thường / nghỉ tuần / lễ)
 *                    đơn giá giờ = đơn giá riêng hoặc lương cơ bản / ngày công chuẩn / giờ công chuẩn
 * Thực nhận        = Lương theo công + Phụ cấp + Thưởng + Tăng ca − Khấu trừ   (làm tròn theo cấu hình)
 * </pre>
 * Mỗi dòng tiền làm tròn tới đồng; riêng thực nhận làm tròn theo đơn vị cấu hình.
 */
public final class PayrollCalculator {
    private PayrollCalculator() {
    }

    public record Settings(BigDecimal otWeekdayRate, BigDecimal otWeekendRate, BigDecimal otHolidayRate, int roundingUnit,
                           String roundingMode, BigDecimal standardHoursPerDay) {
    }

    /** Một phiên bản lương áp dụng trong kỳ và số ngày được trả lương thuộc phiên bản đó. */
    public record Segment(String from, String to, BigDecimal baseSalary, BigDecimal payableDays) {
    }

    public record Component(UUID componentId, String code, String name, String kind, String calcMethod, BigDecimal amount,
                            BigDecimal rate, int sortOrder) {
    }

    /** Dòng đã có số tiền (HR nhập trong kỳ / điều chỉnh từ kỳ trước): chỉ cộng vào tổng. */
    public record FixedLine(String kind, BigDecimal amount) {
    }

    public record Input(BigDecimal standardDays, List<Segment> segments, BigDecimal latestBase, boolean overtimeEligible,
                        BigDecimal overtimeHourlyRate, BigDecimal workedDays, int lateCount, int earlyCount, int missingCount,
                        BigDecimal unpaidLeaveDays, BigDecimal confirmedAbsentDays, BigDecimal otWeekdayHours,
                        BigDecimal otWeekendHours, BigDecimal otHolidayHours, List<Component> components, List<FixedLine> fixedLines) {
    }

    public record Line(UUID componentId, String code, String name, String kind, String calcMethod, BigDecimal quantity,
                       BigDecimal rate, BigDecimal amount, String note, int sortOrder) {
    }

    public record Result(BigDecimal payableDays, BigDecimal basePay, List<Line> lines, BigDecimal allowanceTotal,
                         BigDecimal bonusTotal, BigDecimal overtimePay, BigDecimal deductionTotal, BigDecimal grossPay,
                         BigDecimal netPay, Map<String, Object> detail) {
    }

    public static Result calculate(Settings settings, Input in) {
        BigDecimal standard = in.standardDays();
        if (standard == null || standard.signum() <= 0) throw new IllegalArgumentException("Ngày công chuẩn phải lớn hơn 0");
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("standardDays", standard);

        // ---- lương theo công
        BigDecimal payable = in.segments().stream().map(Segment::payableDays).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal raw = BigDecimal.ZERO;
        List<Map<String, Object>> segmentDetail = new ArrayList<>();
        for (Segment s : in.segments()) {
            BigDecimal pay = s.baseSalary().multiply(s.payableDays()).divide(standard, 6, RoundingMode.HALF_UP);
            raw = raw.add(pay);
            segmentDetail.add(Map.of("from", s.from(), "to", s.to(), "baseSalary", s.baseSalary(), "payableDays", s.payableDays(),
                    "amount", money(pay)));
        }
        if (payable.compareTo(standard) > 0) {
            raw = raw.multiply(standard).divide(payable, 6, RoundingMode.HALF_UP);
            detail.put("cappedAtStandardDays", true);
        }
        BigDecimal basePay = money(raw);
        detail.put("segments", segmentDetail);
        detail.put("payableDays", payable);
        BigDecimal ratio = payable.min(standard).divide(standard, 6, RoundingMode.HALF_UP);

        // ---- tăng ca
        List<Line> lines = new ArrayList<>();
        BigDecimal overtimePay = BigDecimal.ZERO;
        BigDecimal otHours = in.otWeekdayHours().add(in.otWeekendHours()).add(in.otHolidayHours());
        if (otHours.signum() > 0) {
            if (!in.overtimeEligible()) {
                detail.put("overtimeNote", "Nhân viên không thuộc diện hưởng tăng ca: " + otHours.stripTrailingZeros().toPlainString() + " giờ không tính tiền");
            } else {
                BigDecimal hourly = in.overtimeHourlyRate() != null ? in.overtimeHourlyRate()
                        : in.latestBase().divide(standard, 6, RoundingMode.HALF_UP).divide(settings.standardHoursPerDay(), 6, RoundingMode.HALF_UP);
                detail.put("overtimeHourlyRate", money(hourly));
                overtimePay = overtimePay.add(otLine(lines, "OT_WEEKDAY", "Tăng ca ngày thường", in.otWeekdayHours(), hourly, settings.otWeekdayRate(), 300));
                overtimePay = overtimePay.add(otLine(lines, "OT_WEEKEND", "Tăng ca ngày nghỉ tuần", in.otWeekendHours(), hourly, settings.otWeekendRate(), 301));
                overtimePay = overtimePay.add(otLine(lines, "OT_HOLIDAY", "Tăng ca ngày lễ", in.otHolidayHours(), hourly, settings.otHolidayRate(), 302));
            }
        }

        // ---- phụ cấp / thưởng / khấu trừ từ hồ sơ lương
        boolean perfect = in.lateCount() == 0 && in.earlyCount() == 0 && in.missingCount() == 0
                && in.unpaidLeaveDays().signum() == 0 && in.confirmedAbsentDays().signum() == 0;
        for (Component c : in.components()) {
            BigDecimal amount = c.amount() == null ? BigDecimal.ZERO : c.amount();
            BigDecimal quantity = null;
            BigDecimal rate = null;
            BigDecimal value;
            String note = null;
            switch (c.calcMethod()) {
                case "PRORATED" -> {
                    quantity = ratio;
                    rate = amount;
                    value = amount.multiply(ratio);
                    note = "Theo tỷ lệ ngày công " + payable.min(standard).stripTrailingZeros().toPlainString() + "/" + standard.stripTrailingZeros().toPlainString();
                }
                case "PER_WORKED_DAY" -> {
                    quantity = in.workedDays();
                    rate = amount;
                    value = amount.multiply(in.workedDays());
                }
                case "PERCENT_OF_BASE" -> {
                    BigDecimal percent = c.rate() == null ? BigDecimal.ZERO : c.rate();
                    quantity = percent;
                    rate = in.latestBase();
                    value = in.latestBase().multiply(percent).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                    note = percent.stripTrailingZeros().toPlainString() + "% lương cơ bản";
                }
                case "PER_LATE_EARLY" -> {
                    quantity = BigDecimal.valueOf(in.lateCount() + in.earlyCount());
                    rate = amount;
                    value = amount.multiply(quantity);
                }
                case "PERFECT_ATTENDANCE" -> {
                    value = perfect ? amount : BigDecimal.ZERO;
                    note = perfect ? "Đạt chuyên cần" : "Không đạt chuyên cần";
                }
                default -> value = amount; // FIXED, MANUAL có mức trong hồ sơ
            }
            lines.add(new Line(c.componentId(), c.code(), c.name(), c.kind(), c.calcMethod(), quantity, rate, money(value), note, c.sortOrder()));
        }

        BigDecimal allowance = sum(lines, "ALLOWANCE").add(sumFixed(in.fixedLines(), "ALLOWANCE"));
        BigDecimal bonus = sum(lines, "BONUS").add(sumFixed(in.fixedLines(), "BONUS"));
        BigDecimal deduction = sum(lines, "DEDUCTION").add(sumFixed(in.fixedLines(), "DEDUCTION"));
        BigDecimal gross = basePay.add(allowance).add(bonus).add(overtimePay);
        BigDecimal exactNet = gross.subtract(deduction);
        BigDecimal net = round(exactNet, settings.roundingUnit(), settings.roundingMode());
        detail.put("roundingDifference", net.subtract(exactNet));
        detail.put("perfectAttendance", perfect);
        return new Result(payable, basePay, lines, allowance, bonus, overtimePay, deduction, gross, net, detail);
    }

    private static BigDecimal otLine(List<Line> lines, String code, String name, BigDecimal hours, BigDecimal hourly,
                                     BigDecimal multiplier, int order) {
        if (hours.signum() == 0) return BigDecimal.ZERO;
        BigDecimal amount = money(hourly.multiply(hours).multiply(multiplier));
        lines.add(new Line(null, code, name, "OVERTIME", "OVERTIME", hours, money(hourly), amount,
                "Hệ số " + multiplier.stripTrailingZeros().toPlainString(), order));
        return amount;
    }

    private static BigDecimal sum(List<Line> lines, String kind) {
        return lines.stream().filter(l -> l.kind().equals(kind)).map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumFixed(List<FixedLine> lines, String kind) {
        return lines.stream().filter(l -> l.kind().equals(kind)).map(FixedLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal money(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    static BigDecimal round(BigDecimal value, int unit, String mode) {
        BigDecimal step = BigDecimal.valueOf(unit);
        RoundingMode rm = switch (mode) {
            case "DOWN" -> RoundingMode.FLOOR;
            case "UP" -> RoundingMode.CEILING;
            default -> RoundingMode.HALF_UP;
        };
        return value.divide(step, 0, rm).multiply(step);
    }
}
