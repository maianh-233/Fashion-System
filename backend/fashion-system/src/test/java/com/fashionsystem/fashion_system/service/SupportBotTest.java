package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.ShipmentStatus;
import com.fashionsystem.fashion_system.entity.SupportTopic;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportBotTest {
    final SupportBot bot = new SupportBot();
    final Map<SupportTopic, SupportBot.Faq> faqs = Map.of(
            SupportTopic.CANCEL, new SupportBot.Faq(SupportTopic.CANCEL, "Chính sách hủy đơn", "Bạn tự hủy khi chưa xác nhận.", "hủy,huỷ,cancel"),
            SupportTopic.RETURN, new SupportBot.Faq(SupportTopic.RETURN, "Chính sách đổi trả", "Đổi trong 7 ngày.", "đổi,trả,hoàn"),
            SupportTopic.PAYMENT, new SupportBot.Faq(SupportTopic.PAYMENT, "Thanh toán", "COD.", "thanh toán,cod"));
    final SupportBot.Context single = new SupportBot.Context("LB-1", List.of(new SupportBot.Part("LB-1", "Lunaria Q1",
            OrderStatus.SHIPPING, new SupportBot.Shipment("LALAMOVE", ShipmentStatus.PICKED_UP, "https://share/1", "Tài xế A", "59X1"))), faqs);

    @Test void welcomeOffersMenuEndingWithTalkToStaff() {
        var reply = bot.welcome(single);
        assertThat(reply.text()).contains("LB-1");
        assertThat(reply.options()).extracting(SupportBot.Option::code)
                .startsWith(SupportBot.ORDER_STATUS, SupportBot.SHIPPING_INFO).endsWith(SupportBot.TALK_TO_STAFF)
                .contains("POLICY_CANCEL", "POLICY_RETURN", "POLICY_PAYMENT").doesNotContain("POLICY_SHIPPING");
        assertThat(reply.escalate()).isFalse();
    }

    @Test void answersOrderStatusAndShippingFromRealData() {
        var status = bot.answer(SupportBot.ORDER_STATUS, null, single);
        assertThat(status.text()).contains("Đang giao").contains("https://share/1");
        var shipping = bot.answer(SupportBot.SHIPPING_INFO, null, single);
        assertThat(shipping.text()).contains("Lalamove").contains("Tài xế A (59X1)").contains("chưa cung cấp");
    }

    @Test void splitOrderListsEveryPart() {
        var split = new SupportBot.Context("LB-10", List.of(
                new SupportBot.Part("LB-11", "Lunaria Q1", OrderStatus.CONFIRMED, null),
                new SupportBot.Part("LB-12", "Lunaria Thủ Đức", OrderStatus.COMPLETED, null)), faqs);
        assertThat(bot.answer(SupportBot.ORDER_STATUS, null, split).text())
                .contains("2 phần").contains("LB-11 từ Lunaria Q1: Đã xác nhận").contains("LB-12 từ Lunaria Thủ Đức: Hoàn thành");
    }

    @Test void policyAnswersComeFromConfiguredFaq() {
        assertThat(bot.answer("POLICY_RETURN", null, single).text()).isEqualTo("Đổi trong 7 ngày.");
    }

    @Test void freeTextIsRecognizedByFixedKeywordsWithoutDiacritics() {
        assertThat(bot.answer(null, "Đơn của tôi đang ở đâu vậy shop?", single).text()).contains("Đang giao");
        assertThat(bot.answer(null, "cho mình hỏi muốn huy don thì sao", single).text()).isEqualTo("Bạn tự hủy khi chưa xác nhận.");
        assertThat(bot.answer(null, "Tôi muốn ĐỔI size", single).text()).isEqualTo("Đổi trong 7 ngày.");
        // "tra" không được khớp nhầm bên trong "trang thai".
        assertThat(bot.recognize("cho xem trang thai", single)).isEqualTo(SupportBot.ORDER_STATUS);
    }

    @Test void talkToStaffOrUnknownQuestionEscalates() {
        assertThat(bot.answer(SupportBot.TALK_TO_STAFF, null, single).escalate()).isTrue();
        assertThat(bot.answer(null, "cho mình gặp nhân viên", single).escalate()).isTrue();
        var unknown = bot.answer(null, "áo này có hợp với da ngăm không", single);
        assertThat(unknown.escalate()).isTrue();
        assertThat(unknown.text()).contains("chuyển cuộc trò chuyện cho nhân viên");
    }
}
