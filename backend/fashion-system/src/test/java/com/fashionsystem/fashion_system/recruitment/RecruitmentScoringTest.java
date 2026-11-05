package com.fashionsystem.fashion_system.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.Criterion;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.Mark;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.Outcome;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.PassRule;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.Recommendation;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.ScoringMode;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.SubmittedSheet;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecruitmentScoringTest {
    private static final Criterion SKILL = new Criterion(BigDecimal.TEN, BigDecimal.valueOf(2), false);
    private static final Criterion COMM = new Criterion(BigDecimal.TEN, BigDecimal.ONE, false);
    private static final Criterion MUST = new Criterion(BigDecimal.valueOf(5), BigDecimal.ONE, true);

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void weightedScoreIsScaledToTheStageScale() {
        // (8/10*2 + 5/10*1) / 3 = 0.7 → 7.00 / 10 ; 70.00 / 100
        var marks = List.of(new Mark(SKILL, d("8"), null), new Mark(COMM, d("5"), null));
        assertThat(RecruitmentScoring.sheet(ScoringMode.SCORE, BigDecimal.TEN, marks).total()).isEqualByComparingTo("7.00");
        assertThat(RecruitmentScoring.sheet(ScoringMode.SCORE, d("100"), marks).total()).isEqualByComparingTo("70.00");
    }

    @Test
    void criticalCriterionBelowHalfIsAKnockout() {
        var result = RecruitmentScoring.sheet(ScoringMode.SCORE, BigDecimal.TEN,
                List.of(new Mark(SKILL, d("10"), null), new Mark(MUST, d("2"), null)));
        assertThat(result.criticalFailed()).isTrue();
        assertThat(RecruitmentScoring.sheet(ScoringMode.SCORE, BigDecimal.TEN,
                List.of(new Mark(MUST, d("2.5"), null))).criticalFailed()).isFalse();
    }

    @Test
    void passFailModeCountsPassedCriteria() {
        var result = RecruitmentScoring.sheet(ScoringMode.PASS_FAIL, BigDecimal.TEN,
                List.of(new Mark(SKILL, null, true), new Mark(COMM, null, false)));
        assertThat(result.total()).isEqualByComparingTo("6.67");
        assertThat(RecruitmentScoring.sheet(ScoringMode.PASS_FAIL, BigDecimal.TEN,
                List.of(new Mark(MUST, null, false))).criticalFailed()).isTrue();
    }

    @Test
    void outOfRangeOrMissingScoresAreRejected() {
        assertThatThrownBy(() -> RecruitmentScoring.sheet(ScoringMode.SCORE, BigDecimal.TEN, List.of(new Mark(SKILL, d("11"), null))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RecruitmentScoring.sheet(ScoringMode.PASS_FAIL, BigDecimal.TEN, List.of(new Mark(SKILL, null, null))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SubmittedSheet sheet(String total, Recommendation r) {
        return new SubmittedSheet(total == null ? null : d(total), r, false);
    }

    @Test
    void averageRuleWaitsForEveryAssignedEvaluatorAndTheMinimum() {
        var one = List.of(sheet("8", Recommendation.PASS));
        assertThat(RecruitmentScoring.decide(PassRule.AVERAGE_SCORE, d("7"), 2, 0, one).outcome()).isEqualTo(Outcome.UNDECIDED);
        assertThat(RecruitmentScoring.decide(PassRule.AVERAGE_SCORE, d("7"), 1, 1, one).outcome()).isEqualTo(Outcome.UNDECIDED);
        var two = List.of(sheet("8", Recommendation.PASS), sheet("6", Recommendation.FAIL));
        var decision = RecruitmentScoring.decide(PassRule.AVERAGE_SCORE, d("7"), 2, 0, two);
        assertThat(decision.outcome()).isEqualTo(Outcome.PASSED);
        assertThat(decision.averageScore()).isEqualByComparingTo("7.00");
        assertThat(RecruitmentScoring.decide(PassRule.AVERAGE_SCORE, d("7.5"), 2, 0, two).outcome()).isEqualTo(Outcome.FAILED);
    }

    @Test
    void recommendationRules() {
        var mixed = List.of(sheet(null, Recommendation.PASS), sheet(null, Recommendation.PASS), sheet(null, Recommendation.FAIL));
        assertThat(RecruitmentScoring.decide(PassRule.ALL_PASS, null, 1, 0, mixed).outcome()).isEqualTo(Outcome.FAILED);
        assertThat(RecruitmentScoring.decide(PassRule.MAJORITY_PASS, null, 1, 0, mixed).outcome()).isEqualTo(Outcome.PASSED);
        var tie = List.of(sheet(null, Recommendation.PASS), sheet(null, Recommendation.CONSIDER));
        assertThat(RecruitmentScoring.decide(PassRule.MAJORITY_PASS, null, 1, 0, tie).outcome()).isEqualTo(Outcome.FAILED);
        assertThat(RecruitmentScoring.decide(PassRule.MANUAL, null, 1, 0, mixed).outcome()).isEqualTo(Outcome.UNDECIDED);
    }

    @Test
    void anyKnockoutFailsTheRoundEvenWithAHighAverage() {
        var sheets = List.of(sheet("9", Recommendation.PASS), new SubmittedSheet(d("8"), Recommendation.PASS, true));
        assertThat(RecruitmentScoring.decide(PassRule.AVERAGE_SCORE, d("5"), 1, 0, sheets).outcome()).isEqualTo(Outcome.FAILED);
    }
}
