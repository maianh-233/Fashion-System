package com.fashionsystem.fashion_system.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationStatusTest {
    @Test
    void hiredAndWithdrawnAreFinal() {
        assertThat(ApplicationStatus.HIRED.allowedNext()).isEmpty();
        assertThat(ApplicationStatus.WITHDRAWN.allowedNext()).isEmpty();
        assertThat(ApplicationStatus.REJECTED.allowedNext()).containsExactly(ApplicationStatus.PROCESSING);
    }

    @Test
    void everyOpenStatusCanBeWithdrawnExceptAfterADeclinedOffer() {
        Arrays.stream(ApplicationStatus.values()).filter(s -> !s.closed())
                .forEach(s -> assertThat(s.canMoveTo(ApplicationStatus.WITHDRAWN)).as(s.name()).isTrue());
    }

    @Test
    void offerAndHireStatusesCannotBeSetByHand() {
        Arrays.stream(ApplicationStatus.values()).forEach(from -> from.allowedNext().stream()
                .filter(ApplicationStatus::systemOnly)
                .forEach(to -> assertThat(to).as(from + " → " + to).isNull()));
    }

    @Test
    void stageTypeDecidesTheStatusOnEntry() {
        assertThat(ApplicationWorkflow.statusOnEnter("SCREENING")).isEqualTo(ApplicationStatus.SCREENING_PENDING);
        assertThat(ApplicationWorkflow.statusOnEnter("INTERVIEW")).isEqualTo(ApplicationStatus.INTERVIEW_PENDING);
        assertThat(ApplicationWorkflow.statusOnEnter("OFFER")).isEqualTo(ApplicationStatus.OFFER_PENDING);
        assertThat(ApplicationWorkflow.statusOnEnter("TEST")).isEqualTo(ApplicationStatus.PROCESSING);
    }

    @Test
    void storeAdminOnlyAdministersItsOwnStore() {
        UUID storeA = UUID.randomUUID();
        UUID storeB = UUID.randomUUID();
        Actor storeAdmin = new Actor(UUID.randomUUID(), true, false, false, false, storeA, "A");
        assertThat(storeAdmin.adminOf(storeA)).isTrue();
        assertThat(storeAdmin.adminOf(storeB)).isFalse();
        assertThat(storeAdmin.adminOf(null)).isFalse();
        assertThat(storeAdmin.inScope(storeB)).isFalse();
        assertThat(storeAdmin.inScope(null)).isTrue();
        assertThat(storeAdmin.globalAdmin()).isFalse();

        Actor chainAdmin = new Actor(UUID.randomUUID(), true, false, false, false, null, null);
        assertThat(chainAdmin.adminOf(storeB)).isTrue();
        assertThat(chainAdmin.adminOf(null)).isTrue();
        assertThat(chainAdmin.globalAdmin()).isTrue();
    }

    @Test
    void visibilitySqlAlwaysAppliesTheStoreGuard() {
        assertThat(RecruitmentAccess.campaignVisible("c")).startsWith("((:global or c.store_id is null or c.store_id = :store) and");
        assertThat(RecruitmentAccess.applicationVisible("a", "p", "c"))
                .contains("application_stage_evaluators").contains("recruitment_interview_participants");
        // Mọi nhánh (phân công đánh giá / phỏng vấn) đều nằm sau điều kiện cửa hàng.
        String sql = RecruitmentAccess.applicationVisible("a", "p", "c");
        assertThat(sql.split("\\(:global or ", -1)).hasSize(4);
    }
}
