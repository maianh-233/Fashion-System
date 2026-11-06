package com.fashionsystem.fashion_system.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PayrollCalculatorTest {
    static final PayrollCalculator.Settings SETTINGS = new PayrollCalculator.Settings(new BigDecimal("1.5"), new BigDecimal("2"),
            new BigDecimal("3"), 1000, "HALF_UP", new BigDecimal("8"));

    static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    static PayrollCalculator.Input input(List<PayrollCalculator.Segment> segments, List<PayrollCalculator.Component> components,
                                         int late, BigDecimal unpaid, BigDecimal otWeekday) {
        return new PayrollCalculator.Input(d("26"), segments, segments.getLast().baseSalary(), true, null, d("24"), late, 0, 0,
                unpaid, BigDecimal.ZERO, otWeekday, BigDecimal.ZERO, BigDecimal.ZERO, components, List.of());
    }

    static PayrollCalculator.Segment seg(String base, String days) {
        return new PayrollCalculator.Segment("2026-03-01", "2026-03-31", d(base), d(days));
    }

    static PayrollCalculator.Component comp(String code, String kind, String method, String amount, String rate) {
        return new PayrollCalculator.Component(null, code, code, kind, method, amount == null ? null : d(amount),
                rate == null ? null : d(rate), 10);
    }

    @Test
    void fullMonthPaysTheFixedSalary() {
        var r = PayrollCalculator.calculate(SETTINGS, input(List.of(seg("10400000", "26")), List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO));
        assertThat(r.basePay()).isEqualByComparingTo("10400000");
        assertThat(r.netPay()).isEqualByComparingTo("10400000");
    }

    @Test
    void unpaidDaysReduceSalaryByWorkdayRatio() {
        var r = PayrollCalculator.calculate(SETTINGS, input(List.of(seg("10400000", "24")), List.of(), 0, d("2"), BigDecimal.ZERO));
        assertThat(r.basePay()).isEqualByComparingTo("9600000");
    }

    @Test
    void payableDaysAboveStandardAreCappedAtFullSalary() {
        var r = PayrollCalculator.calculate(SETTINGS, input(List.of(seg("10400000", "27")), List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO));
        assertThat(r.basePay()).isEqualByComparingTo("10400000");
        assertThat(r.detail()).containsEntry("cappedAtStandardDays", true);
    }

    @Test
    void salaryChangeMidMonthUsesEachVersionForItsOwnDays() {
        var r = PayrollCalculator.calculate(SETTINGS, input(List.of(seg("7800000", "13"), seg("10400000", "13")), List.of(), 0,
                BigDecimal.ZERO, BigDecimal.ZERO));
        // 7.8tr × 13/26 + 10.4tr × 13/26
        assertThat(r.basePay()).isEqualByComparingTo("9100000");
    }

    @Test
    void overtimeUsesHourlyRateFromBaseSalaryAndMultiplier() {
        var r = PayrollCalculator.calculate(SETTINGS, input(List.of(seg("10400000", "26")), List.of(), 0, BigDecimal.ZERO, d("4")));
        // 10.4tr / 26 / 8 = 50.000 ₫/giờ × 4 × 1.5
        assertThat(r.overtimePay()).isEqualByComparingTo("300000");
        assertThat(r.lines()).singleElement().satisfies(l -> assertThat(l.kind()).isEqualTo("OVERTIME"));
    }

    @Test
    void overtimeIsNotPaidWhenEmployeeIsNotEligible() {
        var in = new PayrollCalculator.Input(d("26"), List.of(seg("10400000", "26")), d("10400000"), false, null, d("26"), 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, d("4"), BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
        assertThat(PayrollCalculator.calculate(SETTINGS, in).overtimePay()).isZero();
    }

    @Test
    void componentsFollowTheirCalculationMethod() {
        List<PayrollCalculator.Component> components = List.of(
                comp("PHONE", "ALLOWANCE", "FIXED", "300000", null),
                comp("TRANSPORT", "ALLOWANCE", "PRORATED", "520000", null),
                comp("MEAL", "ALLOWANCE", "PER_WORKED_DAY", "30000", null),
                comp("BONUS", "BONUS", "PERFECT_ATTENDANCE", "300000", null),
                comp("LATE", "DEDUCTION", "PER_LATE_EARLY", "20000", null),
                comp("INSURANCE", "DEDUCTION", "PERCENT_OF_BASE", null, "10.5"));
        var r = PayrollCalculator.calculate(SETTINGS, input(List.of(seg("10000000", "24")), components, 2, d("2"), BigDecimal.ZERO));
        assertThat(r.lines()).extracting(PayrollCalculator.Line::code, PayrollCalculator.Line::amount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("PHONE", d("300000")),
                        org.assertj.core.groups.Tuple.tuple("TRANSPORT", d("480000")),     // 520k × 24/26
                        org.assertj.core.groups.Tuple.tuple("MEAL", d("720000")),          // 30k × 24 ngày đi làm
                        org.assertj.core.groups.Tuple.tuple("BONUS", d("0")),              // có đi trễ / nghỉ không lương
                        org.assertj.core.groups.Tuple.tuple("LATE", d("40000")),
                        org.assertj.core.groups.Tuple.tuple("INSURANCE", d("1050000")));   // 10.5% lương hợp đồng
        assertThat(r.allowanceTotal()).isEqualByComparingTo("1500000");
        assertThat(r.deductionTotal()).isEqualByComparingTo("1090000");
    }

    @Test
    void perfectAttendanceBonusIsPaidOnlyWithoutViolations() {
        var bonus = List.of(comp("BONUS", "BONUS", "PERFECT_ATTENDANCE", "300000", null));
        assertThat(PayrollCalculator.calculate(SETTINGS, input(List.of(seg("10000000", "26")), bonus, 0, BigDecimal.ZERO, BigDecimal.ZERO))
                .bonusTotal()).isEqualByComparingTo("300000");
    }

    @Test
    void manualAndCarriedLinesAreAddedAndNetIsRounded() {
        var in = new PayrollCalculator.Input(d("26"), List.of(seg("10000000", "25")), d("10000000"), true, null, d("25"), 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(),
                List.of(new PayrollCalculator.FixedLine("BONUS", d("500000")), new PayrollCalculator.FixedLine("DEDUCTION", d("123456"))));
        var r = PayrollCalculator.calculate(SETTINGS, in);
        // 10tr × 25/26 = 9.615.385 + 500.000 − 123.456 = 9.991.929 → 9.992.000
        assertThat(r.basePay()).isEqualByComparingTo("9615385");
        assertThat(r.netPay()).isEqualByComparingTo("9992000");
        assertThat(PayrollCalculator.round(d("9991929"), 1000, "DOWN")).isEqualByComparingTo("9991000");
        assertThat(PayrollCalculator.round(d("9991001"), 1000, "UP")).isEqualByComparingTo("9992000");
    }

    @Test
    void deductionsAboveIncomeGiveNegativeNet() {
        var in = new PayrollCalculator.Input(d("26"), List.of(seg("1000000", "1")), d("1000000"), true, null, d("1"), 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(),
                List.of(new PayrollCalculator.FixedLine("DEDUCTION", d("500000"))));
        assertThat(PayrollCalculator.calculate(SETTINGS, in).netPay()).isNegative();
    }

    @Test
    void standardDaysMustBePositive() {
        var in = new PayrollCalculator.Input(BigDecimal.ZERO, List.of(seg("1000000", "1")), d("1000000"), true, null, d("1"), 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
        assertThatThrownBy(() -> PayrollCalculator.calculate(SETTINGS, in)).isInstanceOf(IllegalArgumentException.class);
    }
}
