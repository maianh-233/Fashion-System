package com.fashionsystem.fashion_system.marketing;

import java.sql.Types;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/** Ghi lịch sử chiến dịch; dùng chung cho thao tác trên chiến dịch, đợt gửi và job gửi (actor null = hệ thống). */
@Component
@RequiredArgsConstructor
class MarketingCampaignHistory {
    private final NamedParameterJdbcTemplate jdbc;

    /** clock_timestamp() thay vì now(): nhiều dòng ghi trong cùng giao dịch vẫn giữ đúng thứ tự. */
    void record(UUID campaign, String action, String from, String to, String note, UUID actor) {
        jdbc.update("""
                insert into marketing_campaign_history(campaign_id, action, from_status, to_status, note, actor_id, created_at)
                values (:campaign, :action, :from, :to, :note, cast(:actor as uuid), (clock_timestamp() at time zone 'UTC'))""",
                new MapSqlParameterSource("campaign", campaign).addValue("action", action).addValue("from", from, Types.VARCHAR)
                        .addValue("to", to, Types.VARCHAR).addValue("note", cut(note), Types.VARCHAR).addValue("actor", actor, Types.OTHER));
    }

    private static String cut(String note) {
        return note == null || note.length() <= 1000 ? note : note.substring(0, 999) + "…";
    }
}
