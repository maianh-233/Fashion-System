package com.fashionsystem.fashion_system.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RecruitmentMailerTest {
    @Test
    void placeholdersAreReplacedAndUnknownOnesRemoved() {
        String out = RecruitmentMailer.render("Chào {{candidateName}}, vị trí {{ positionTitle }}.\n{{offerNote}}\n\n\nHẹn gặp {{nope}}",
                Map.of("candidateName", "An", "positionTitle", "Thu ngân $1"));
        assertThat(out).isEqualTo("Chào An, vị trí Thu ngân $1.\n\nHẹn gặp");
    }

    @Test
    void htmlIsEscapedSoCandidateInputCannotInjectMarkup() {
        assertThat(RecruitmentMailer.toHtml("<b>Hi</b>\nDòng 2")).isEqualTo("&lt;b&gt;Hi&lt;/b&gt;<br>Dòng 2");
    }
}
