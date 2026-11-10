package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.service.MarketingAudienceSql;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Các bước có giao dịch của job gửi (mỗi bước một giao dịch ngắn, gọi từ {@link MarketingDispatchScheduler}):
 * <ol>
 *   <li>{@link #startDue}: nhận đợt đã tới giờ, chốt danh sách người nhận (kèm nhóm đối chứng) trong cùng giao dịch
 *       chuyển sang SENDING;</li>
 *   <li>{@link #claimBatch}: giữ một lô người nhận (lease) để gửi;</li>
 *   <li>{@link #recordResults}: ghi kết quả (SENT / thử lại có backoff / FAILED / SKIPPED);</li>
 *   <li>{@link #finishDone}: kết thúc đợt không còn ai chờ gửi.</li>
 * </ol>
 * Mọi bước nhận việc dùng FOR UPDATE SKIP LOCKED nên chạy nhiều instance cùng lúc không gửi trùng; dòng giữ quá lease
 * (instance chết giữa chừng) được nhận lại. Với thông báo trong app, event_id cố định khiến lần ghi thứ hai bị bỏ qua;
 * với email, SMTP không chống trùng được nên kết quả được ghi ngay sau từng email để thu hẹp khoảng có thể gửi lặp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingDispatchWorker {
    /** Chờ trước lần thử thứ 2, 3, 4 (phút). */
    static final int[] BACKOFF_MINUTES = {1, 5, 30};

    private final NamedParameterJdbcTemplate jdbc;
    private final MarketingCampaignHistory history;
    private final MarketingDispatchService dispatches;
    private final MarketingCampaignNotifier notifier;

    /**
     * Một người nhận đã được giữ để gửi; các cờ đồng ý / tắt tin được đọc lại lúc giữ (khách có thể đổi ý sau khi chốt).
     */
    record Work(UUID recipientId, UUID customerId, UUID eventId, int attempt, UUID dispatchId, UUID campaignId,
                String channel, String title, String message, String actionUrl, boolean customerActive, String email,
                boolean emailConsent, boolean inAppOptedOut, UUID unsubscribeToken) {}

    enum Outcome { SENT, SKIPPED, RETRY, FAILED }

    /** @param code mã lỗi, hoặc lý do bỏ qua khi {@code outcome} là SKIPPED */
    record Result(UUID recipientId, int attempt, Outcome outcome, UUID notificationId, String code, String error) {}

    // ------------------------------------------------------------------ 1. bắt đầu đợt

    /** @return số đợt đã bắt đầu gửi */
    @Transactional
    @BusinessAudit("MARKETING_DISPATCH")
    public int startDue(LocalDateTime now, int frequencyCapHours) {
        List<UUID> due = jdbc.queryForList("""
                select id from marketing_dispatches where status = 'SCHEDULED' and scheduled_at <= :now
                order by scheduled_at limit 5 for update skip locked""", new MapSqlParameterSource("now", DbTimes.toDb(now)), UUID.class);
        int started = 0;
        for (UUID id : due) {
            if (start(id, now, frequencyCapHours)) started++;
        }
        return started;
    }

    private boolean start(UUID id, LocalDateTime now, int frequencyCapHours) {
        Map<String, Object> row = jdbc.queryForMap("""
                select d.code, d.channel, d.holdout_percent, d.criteria::text as criteria, d.campaign_id,
                       c.status as campaign_status, c.ends_at, c.store_id
                from marketing_dispatches d join marketing_campaigns c on c.id = d.campaign_id where d.id = :id""",
                new MapSqlParameterSource("id", id));
        UUID campaignId = (UUID) row.get("campaign_id");
        String code = (String) row.get("code");
        String channel = (String) row.get("channel");
        int holdout = ((Number) row.get("holdout_percent")).intValue();
        UUID store = (UUID) row.get("store_id");
        String campaignStatus = (String) row.get("campaign_status");
        LocalDateTime endsAt = DbTimes.fromDb(row.get("ends_at"));
        if (!MarketingCampaignService.APPROVED.equals(campaignStatus) || !now.isBefore(endsAt)) {
            cancel(id, campaignId, campaignStatus, code, "Chiến dịch không còn ở trạng thái Đã duyệt hoặc đã kết thúc trước giờ gửi");
            return false;
        }
        MarketingDispatchService.StoredCriteria criteria = dispatches.readCriteria((String) row.get("criteria"));
        List<UUID> tiers = criteria.tierIds() == null ? List.of() : criteria.tierIds();
        if (!referencesExist(tiers, criteria.categoryId())) {
            // Không gửi cho một nhóm rộng hơn dự định khi hạng / danh mục trong tiêu chí đã bị xóa.
            cancel(id, campaignId, campaignStatus, code, "Hạng khách hàng hoặc danh mục trong tiêu chí đã bị xóa");
            return false;
        }
        MapSqlParameterSource p = MarketingAudienceSql.params(tiers, criteria.inactiveDays(), criteria.categoryId(), now)
                .addValue("dispatch", id).addValue("now", DbTimes.toDb(now)).addValue("channel", channel).addValue("holdout", holdout)
                .addValue("capSince", DbTimes.toDb(now.minusHours(frequencyCapHours))).addValue("audienceStore", store, Types.OTHER);
        // Thứ tự: không nhận được qua kênh / đã tắt / vừa nhận tin → SKIPPED kèm lý do; còn lại chia nhóm đối chứng (HOLDOUT)
        // hoặc chờ gửi (PENDING). event_id cố định theo (đợt, khách) — md5 của chuỗi định danh, đổi sang uuid.
        StringBuilder status = new StringBuilder("case");
        StringBuilder reason = new StringBuilder("case");
        for (String[] rule : MarketingDispatchService.skipRules(channel)) {
            status.append(" when ").append(rule[0]).append(" then 'SKIPPED'");
            reason.append(" when ").append(rule[0]).append(" then '").append(rule[1]).append("'");
        }
        status.append(" when ").append(MarketingDispatchService.IN_HOLDOUT).append(" then 'HOLDOUT' else 'PENDING' end");
        reason.append(" end");
        String sql = MarketingAudienceSql.CATEGORY_TREE
                + " insert into marketing_dispatch_recipients(dispatch_id, customer_id, event_id, status, skip_reason, next_attempt_at, email)"
                + " select :dispatch, c.id, md5('marketing:' || cast(:dispatch as text) || ':' || c.id::text)::uuid, "
                + status + ", " + reason + ", :now, " + (MarketingDispatchService.EMAIL.equals(channel) ? "nullif(btrim(c.email), '')" : "null")
                + " from customers c"
                + MarketingAudienceSql.where(tiers, criteria.inactiveDays(), criteria.categoryId())
                + (store == null ? "" : MarketingDispatchService.STORE_AUDIENCE)
                + " on conflict (dispatch_id, customer_id) do nothing";
        int audience = jdbc.update(sql, p);
        Integer heldOut = jdbc.queryForObject("select count(*) from marketing_dispatch_recipients where dispatch_id = :id and status = 'HOLDOUT'",
                new MapSqlParameterSource("id", id), Integer.class);
        jdbc.update("""
                update marketing_dispatches set status = 'SENDING', snapshot_at = :now, started_at = :now,
                       audience_count = :audience, holdout_count = :holdout, updated_at = (now() at time zone 'UTC') where id = :id""",
                new MapSqlParameterSource("id", id).addValue("now", DbTimes.toDb(now)).addValue("audience", audience)
                        .addValue("holdout", heldOut));
        history.record(campaignId, "DISPATCH_STARTED", campaignStatus, campaignStatus, code + " · chốt " + audience + " khách"
                + (heldOut != null && heldOut > 0 ? ", giữ lại " + heldOut + " làm nhóm đối chứng" : ""), null);
        log.info("Marketing dispatch {} ({}) started with {} recipient(s), {} held out", code, channel, audience, heldOut);
        return true;
    }

    private boolean referencesExist(List<UUID> tiers, UUID categoryId) {
        if (!tiers.isEmpty()) {
            Integer found = jdbc.queryForObject("select count(*) from customer_tiers where id in (:ids)",
                    new MapSqlParameterSource("ids", tiers), Integer.class);
            if (found == null || found != tiers.size()) return false;
        }
        return categoryId == null || Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from categories where id = :id)",
                new MapSqlParameterSource("id", categoryId), Boolean.class));
    }

    private void cancel(UUID id, UUID campaignId, String campaignStatus, String code, String reason) {
        jdbc.update("""
                update marketing_dispatches set status = 'CANCELLED', cancelled_at = (now() at time zone 'UTC'), cancel_reason = :reason,
                       updated_at = (now() at time zone 'UTC') where id = :id""", new MapSqlParameterSource("id", id).addValue("reason", reason));
        history.record(campaignId, "DISPATCH_CANCELLED", campaignStatus, campaignStatus, code + " · " + reason, null);
        log.warn("Marketing dispatch {} cancelled at send time: {}", code, reason);
    }

    // ------------------------------------------------------------------ 2. giữ một lô

    /** Giữ tối đa {@code size} người nhận tới hạn gửi (hoặc giữ quá lease) và tăng số lần thử. */
    @Transactional
    @BusinessAudit("MARKETING_DISPATCH")
    public List<Work> claimBatch(LocalDateTime now, int size, int leaseMinutes) {
        MapSqlParameterSource p = new MapSqlParameterSource("now", DbTimes.toDb(now)).addValue("size", size);
        List<UUID> ids = jdbc.queryForList("""
                select r.id from marketing_dispatch_recipients r join marketing_dispatches d on d.id = r.dispatch_id
                where d.status = 'SENDING'
                  and ((r.status = 'PENDING' and r.next_attempt_at <= :now) or (r.status = 'SENDING' and r.lease_until < :now))
                order by r.next_attempt_at, r.id limit :size
                for update of r skip locked""", p, UUID.class);
        if (ids.isEmpty()) return List.of();
        p.addValue("ids", ids).addValue("lease", DbTimes.toDb(now.plusMinutes(leaseMinutes)));
        jdbc.update("""
                update marketing_dispatch_recipients set status = 'SENDING', lease_until = :lease, attempt_count = attempt_count + 1
                where id in (:ids)""", p);
        return jdbc.query("""
                select r.id, r.customer_id, r.event_id, r.attempt_count, r.email, d.id as dispatch_id, d.campaign_id, d.channel,
                       d.title, d.message, d.action_url, coalesce(c.active, false) as customer_active,
                       coalesce(mp.email_opt_in, false) as email_consent, coalesce(mp.in_app_opt_out, false) as in_app_opted_out,
                       mp.unsubscribe_token
                from marketing_dispatch_recipients r join marketing_dispatches d on d.id = r.dispatch_id
                join customers c on c.id = r.customer_id
                left join marketing_customer_preferences mp on mp.customer_id = r.customer_id
                where r.id in (:ids)""", p,
                (rs, i) -> new Work(rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
                        rs.getObject("event_id", UUID.class), rs.getInt("attempt_count"), rs.getObject("dispatch_id", UUID.class),
                        rs.getObject("campaign_id", UUID.class), rs.getString("channel"), rs.getString("title"), rs.getString("message"),
                        rs.getString("action_url"), rs.getBoolean("customer_active"), rs.getString("email"), rs.getBoolean("email_consent"),
                        rs.getBoolean("in_app_opted_out"), rs.getObject("unsubscribe_token", UUID.class)));
    }

    // ------------------------------------------------------------------ 3. ghi kết quả

    /**
     * Ghi kết quả. SENT được ghi cả khi dòng vừa bị hủy (SKIPPED) trong lúc gửi: tin đã tới khách nên số đếm phải phản
     * ánh đúng. Các kết quả khác chỉ ghi khi dòng vẫn đang SENDING.
     */
    @Transactional
    @BusinessAudit("MARKETING_DISPATCH")
    public void recordResults(List<Result> results, LocalDateTime now, int maxAttempts) {
        for (Result r : results) {
            MapSqlParameterSource p = new MapSqlParameterSource("id", r.recipientId()).addValue("now", DbTimes.toDb(now))
                    .addValue("code", r.code()).addValue("error", r.error() == null ? null : cut(r.error()));
            switch (r.outcome()) {
                case SENT -> jdbc.update("""
                        update marketing_dispatch_recipients set status = 'SENT', skip_reason = null, sent_at = :now,
                               notification_id = :notification, lease_until = null, last_error_code = null, last_error = null
                        where id = :id and status in ('SENDING', 'SKIPPED')""", p.addValue("notification", r.notificationId()));
                case SKIPPED -> jdbc.update("""
                        update marketing_dispatch_recipients set status = 'SKIPPED', skip_reason = :code, lease_until = null
                        where id = :id and status = 'SENDING'""", p);
                case RETRY -> {
                    if (r.attempt() >= maxAttempts) {
                        fail(p);
                    } else {
                        int wait = BACKOFF_MINUTES[Math.min(r.attempt(), BACKOFF_MINUTES.length) - 1];
                        jdbc.update("""
                                update marketing_dispatch_recipients set status = 'PENDING', next_attempt_at = :next, lease_until = null,
                                       last_error_code = :code, last_error = :error
                                where id = :id and status = 'SENDING'""", p.addValue("next", DbTimes.toDb(now.plusMinutes(wait))));
                    }
                }
                case FAILED -> fail(p);
            }
        }
    }

    private void fail(MapSqlParameterSource p) {
        jdbc.update("""
                update marketing_dispatch_recipients set status = 'FAILED', lease_until = null, last_error_code = :code, last_error = :error
                where id = :id and status = 'SENDING'""", p);
    }

    // ------------------------------------------------------------------ 4. kết thúc đợt

    /** Kết thúc các đợt đang gửi không còn ai chờ; ghi số đếm cuối cùng từ danh sách người nhận. @return số đợt kết thúc */
    @Transactional
    @BusinessAudit("MARKETING_DISPATCH")
    public int finishDone() {
        List<Map<String, Object>> done = jdbc.queryForList("""
                select d.id, d.code, d.campaign_id, c.status as campaign_status,
                       count(*) filter (where r.status = 'SENT') as sent,
                       count(*) filter (where r.status = 'SKIPPED') as skipped,
                       count(*) filter (where r.status = 'FAILED') as failed
                from marketing_dispatches d
                join marketing_campaigns c on c.id = d.campaign_id
                left join marketing_dispatch_recipients r on r.dispatch_id = d.id
                where d.status = 'SENDING'
                  and not exists (select 1 from marketing_dispatch_recipients x where x.dispatch_id = d.id
                                  and x.status in ('PENDING', 'SENDING'))
                group by d.id, d.code, d.campaign_id, c.status""", new MapSqlParameterSource());
        int finished = 0;
        for (Map<String, Object> d : done) {
            long failed = ((Number) d.get("failed")).longValue();
            int updated = jdbc.update("""
                    update marketing_dispatches set status = :status, finished_at = (now() at time zone 'UTC'), sent_count = :sent,
                           skipped_count = :skipped, failed_count = :failed, updated_at = (now() at time zone 'UTC')
                    where id = :id and status = 'SENDING'""",
                    new MapSqlParameterSource("id", d.get("id")).addValue("status", failed > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED")
                            .addValue("sent", ((Number) d.get("sent")).intValue()).addValue("skipped", ((Number) d.get("skipped")).intValue())
                            .addValue("failed", (int) failed));
            if (updated == 0) continue;
            finished++;
            String status = (String) d.get("campaign_status");
            history.record((UUID) d.get("campaign_id"), "DISPATCH_FINISHED", status, status, d.get("code") + " · gửi " + d.get("sent")
                    + ", bỏ qua " + d.get("skipped") + ", lỗi " + failed, null);
            log.info("Marketing dispatch {} finished: sent {}, skipped {}, failed {}", d.get("code"), d.get("sent"), d.get("skipped"), failed);
        }
        return finished;
    }

    /** Báo người tạo một lần khi đợt gửi quá lâu chưa xong (thường do lỗi kéo dài). */
    @Transactional
    @BusinessAudit("MARKETING_DISPATCH")
    public int reportStalled(LocalDateTime now, int stallMinutes) {
        List<Map<String, Object>> stalled = jdbc.queryForList("""
                update marketing_dispatches d set stall_notified_at = (now() at time zone 'UTC')
                where d.status = 'SENDING' and d.stall_notified_at is null and d.started_at < :since
                returning d.id, d.code, d.campaign_id, d.created_by""",
                new MapSqlParameterSource("since", DbTimes.toDb(now.minusMinutes(stallMinutes))));
        for (Map<String, Object> d : stalled) {
            UUID createdBy = (UUID) d.get("created_by");
            if (createdBy == null) continue;
            notifier.send(List.of(createdBy), null, MarketingCampaignNotifier.DISPATCH_STALLED,
                    "Đợt gửi " + d.get("code") + " chạy quá " + stallMinutes + " phút",
                    "Còn người nhận chưa gửi được; hãy xem danh sách người nhận để biết lỗi.", (UUID) d.get("campaign_id"),
                    (String) d.get("code"));
        }
        return stalled.size();
    }

    private static String cut(String text) {
        return text.length() <= 500 ? text : text.substring(0, 499) + "…";
    }
}
