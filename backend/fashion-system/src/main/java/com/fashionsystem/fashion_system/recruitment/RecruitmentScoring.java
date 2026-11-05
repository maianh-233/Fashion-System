package com.fashionsystem.fashion_system.recruitment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Quy tắc tính điểm và kết luận vòng, tách riêng để kiểm thử.
 *
 * <p>Điểm một phiếu = Σ(điểm tiêu chí / điểm tối đa × trọng số) / Σ trọng số × thang điểm vòng. Ở chế độ Đạt/Không đạt,
 * tiêu chí Đạt tính đủ điểm, Không đạt tính 0. Tiêu chí CRITICAL dưới 50% điểm tối đa (hoặc Không đạt) là "trượt tiêu
 * chí loại trực tiếp".
 *
 * <p>Kết luận vòng chỉ có khi đã đủ {@code minEvaluations} phiếu nộp và mọi người được phân công đã nộp:
 * AVERAGE_SCORE: điểm TB ≥ điểm đạt; ALL_PASS: mọi phiếu đề xuất Đạt; MAJORITY_PASS: quá nửa phiếu Đạt; MANUAL: không
 * tự kết luận. Bất kỳ phiếu nào trượt tiêu chí loại trực tiếp → Không đạt (trừ MANUAL).
 */
public final class RecruitmentScoring {
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private RecruitmentScoring() {
    }

    public enum ScoringMode { SCORE, PASS_FAIL }

    public enum PassRule { AVERAGE_SCORE, ALL_PASS, MAJORITY_PASS, MANUAL }

    public enum Recommendation { PASS, FAIL, CONSIDER }

    public record Criterion(BigDecimal maxScore, BigDecimal weight, boolean critical) {
    }

    /** Một ô chấm: {@code score} cho chế độ SCORE, {@code passed} cho chế độ PASS_FAIL. */
    public record Mark(Criterion criterion, BigDecimal score, Boolean passed) {
    }

    public record SheetResult(BigDecimal total, boolean criticalFailed) {
    }

    public record SubmittedSheet(BigDecimal total, Recommendation recommendation, boolean criticalFailed) {
    }

    public enum Outcome { PASSED, FAILED, UNDECIDED }

    public record Decision(Outcome outcome, BigDecimal averageScore) {
    }

    /** Điểm tổng của một phiếu theo thang của vòng. Thiếu ô chấm → lỗi (phiếu nộp phải chấm đủ). */
    public static SheetResult sheet(ScoringMode mode, BigDecimal scaleMax, List<Mark> marks) {
        if (marks.isEmpty()) return new SheetResult(null, false);
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        boolean criticalFailed = false;
        for (Mark mark : marks) {
            Criterion c = mark.criterion();
            BigDecimal ratio;
            if (mode == ScoringMode.PASS_FAIL) {
                if (mark.passed() == null) throw new IllegalArgumentException("Chưa chấm Đạt/Không đạt cho mọi tiêu chí");
                ratio = mark.passed() ? BigDecimal.ONE : BigDecimal.ZERO;
            } else {
                if (mark.score() == null) throw new IllegalArgumentException("Chưa chấm điểm cho mọi tiêu chí");
                if (mark.score().signum() < 0 || mark.score().compareTo(c.maxScore()) > 0)
                    throw new IllegalArgumentException("Điểm phải từ 0 đến " + c.maxScore().stripTrailingZeros().toPlainString());
                ratio = mark.score().divide(c.maxScore(), 6, RoundingMode.HALF_UP);
            }
            if (c.critical() && ratio.compareTo(HALF) < 0) criticalFailed = true;
            weighted = weighted.add(ratio.multiply(c.weight()));
            weights = weights.add(c.weight());
        }
        BigDecimal total = weighted.divide(weights, 6, RoundingMode.HALF_UP).multiply(scaleMax).setScale(2, RoundingMode.HALF_UP);
        return new SheetResult(total, criticalFailed);
    }

    /**
     * Kết luận vòng từ các phiếu đã nộp.
     *
     * @param pendingAssigned số người được phân công chưa nộp phiếu
     */
    public static Decision decide(PassRule rule, BigDecimal passScore, int minEvaluations, int pendingAssigned,
                                  List<SubmittedSheet> sheets) {
        BigDecimal average = average(sheets);
        if (rule == PassRule.MANUAL || sheets.size() < Math.max(1, minEvaluations) || pendingAssigned > 0)
            return new Decision(Outcome.UNDECIDED, average);
        if (sheets.stream().anyMatch(SubmittedSheet::criticalFailed)) return new Decision(Outcome.FAILED, average);
        boolean pass = switch (rule) {
            case AVERAGE_SCORE -> {
                if (passScore == null) {
                    // Không đặt điểm đạt: dựa vào đề xuất như MAJORITY_PASS.
                    yield majority(sheets);
                }
                yield average != null && average.compareTo(passScore) >= 0;
            }
            case ALL_PASS -> sheets.stream().allMatch(s -> s.recommendation() == Recommendation.PASS);
            case MAJORITY_PASS -> majority(sheets);
            case MANUAL -> false;
        };
        return new Decision(pass ? Outcome.PASSED : Outcome.FAILED, average);
    }

    private static boolean majority(List<SubmittedSheet> sheets) {
        long passes = sheets.stream().filter(s -> s.recommendation() == Recommendation.PASS).count();
        return passes * 2 > sheets.size();
    }

    static BigDecimal average(List<SubmittedSheet> sheets) {
        List<BigDecimal> totals = sheets.stream().map(SubmittedSheet::total).filter(t -> t != null).toList();
        if (totals.isEmpty()) return null;
        return totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totals.size()), 2, RoundingMode.HALF_UP);
    }
}
