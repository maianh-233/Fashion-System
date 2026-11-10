package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Audience;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.AudienceCriteria;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Campaign;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Dispatch;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.DispatchActions;
import com.fashionsystem.fashion_system.service.MarketingAudienceSql;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * Đợt gửi thông báo của chiến dịch qua thông báo trong app hoặc email. Trạng thái đợt tách khỏi trạng thái chiến dịch:
 * Nháp (duyệt cùng chiến dịch) / Chờ duyệt (đợt thêm sau khi chiến dịch đã duyệt) → Đã hẹn giờ → Đang gửi →
 * Hoàn tất / Hoàn tất có lỗi; hủy được khi chưa xong. Việc gửi thật do {@link MarketingDispatchWorker} làm.
 * <p>
 * Email chỉ tới khách có email và đã chủ động đồng ý nhận email khuyến mãi (mặc định là không); thông báo trong app chỉ
 * tới khách có tài khoản online và không tắt tin marketing. Một tỷ lệ người nhận đủ điều kiện có thể được giữ lại làm
 * nhóm đối chứng (không gửi) để đo mức tăng thêm. Chiến dịch của một cửa hàng chỉ gửi cho khách của cửa hàng đó.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketingDispatchService {
    static final String DRAFT = "DRAFT";
    static final String PENDING = "PENDING_APPROVAL";
    static final String SCHEDULED = "SCHEDULED";
    static final String SENDING = "SENDING";
    static final String COMPLETED = "COMPLETED";
    static final String COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS";
    static final String CANCELLED = "CANCELLED";
    static final String IN_APP = "IN_APP";
    static final String EMAIL = "EMAIL";
    /** Đợt chưa xong — hủy chiến dịch thì hủy theo. */
    static final String OPEN = "('DRAFT', 'PENDING_APPROVAL', 'SCHEDULED', 'SENDING')";
    static final int MAX_PER_CAMPAIGN = 20;
    static final int MAX_HOLDOUT_PERCENT = 50;
    /** Được gửi "teaser" trước khi chiến dịch bắt đầu tối đa chừng này ngày. */
    static final int TEASER_DAYS = 7;
    private static final Set<String> RECIPIENT_STATUSES = Set.of("PENDING", "SENDING", "SENT", "FAILED", "SKIPPED", "HOLDOUT");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int SAMPLE_SIZE = 10;

    // ------------------------------------------------------------------ điều kiện nhóm khách (alias khách: c)

    /** Khách có tài khoản online mới thấy được thông báo trong app. */
    static final String HAS_ACCOUNT = "exists (select 1 from customer_accounts ca where ca.customer_id = c.id)";
    static final String IN_APP_OPTED_OUT =
            "exists (select 1 from marketing_customer_preferences mp where mp.customer_id = c.id and mp.in_app_opt_out)";
    static final String HAS_EMAIL = "(c.email is not null and btrim(c.email) <> '')";
    /** Đồng ý nhận email khuyến mãi là opt-in: không có dòng tùy chọn nghĩa là chưa đồng ý. */
    static final String EMAIL_CONSENT =
            "exists (select 1 from marketing_customer_preferences mp where mp.customer_id = c.id and mp.email_opt_in)";
    /** Giới hạn tần suất theo từng kênh: đã nhận tin marketing cùng kênh sau :capSince. */
    static final String RECENTLY_SENT = "exists (select 1 from marketing_dispatch_recipients x join marketing_dispatches xd"
            + " on xd.id = x.dispatch_id where x.customer_id = c.id and x.status = 'SENT' and x.sent_at > :capSince"
            + " and xd.channel = :channel)";
    /** Khách của một cửa hàng: tạo tại cửa hàng đó hoặc đã có đơn (chưa hủy) ở cửa hàng đó. */
    static final String STORE_AUDIENCE = " and (c.origin_store_id = :audienceStore or exists (select 1 from orders so"
            + " where so.customer_id = c.id and so.store_id = :audienceStore and so.status <> 'CANCELLED'))";
    /** Chia nhóm đối chứng ổn định theo (đợt, khách): cùng đợt chạy lại vẫn ra cùng kết quả. */
    static final String IN_HOLDOUT = "((('x' || substr(md5(cast(:dispatch as text) || c.id::text), 1, 7))::bit(28)::int % 100) < :holdout)";

    /** Nhận được qua kênh (trước giới hạn tần suất). */
    static String reachable(String channel) {
        return EMAIL.equals(channel) ? HAS_EMAIL + " and " + EMAIL_CONSENT : HAS_ACCOUNT + " and not " + IN_APP_OPTED_OUT;
    }

    /** Lý do bỏ qua theo thứ tự kiểm tra: {điều kiện, mã lý do}. */
    static List<String[]> skipRules(String channel) {
        return EMAIL.equals(channel)
                ? List.of(new String[] {"not " + HAS_EMAIL, "NO_EMAIL"}, new String[] {"not " + EMAIL_CONSENT, "NO_CONSENT"},
                        new String[] {RECENTLY_SENT, "FREQUENCY_CAP"})
                : List.of(new String[] {"not " + HAS_ACCOUNT, "NO_ACCOUNT"}, new String[] {IN_APP_OPTED_OUT, "OPTED_OUT"},
                        new String[] {RECENTLY_SENT, "FREQUENCY_CAP"});
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final MarketingCampaignAccess access;
    private final MarketingCampaignHistory history;
    private final ObjectMapper json;

    @Value("${marketing-dispatch.frequency-cap-hours:24}")
    private int frequencyCapHours = 24;

    /** Tiêu chí lưu trong cột JSONB; {@code version} để đọc được dữ liệu cũ khi đổi cấu trúc. */
    record StoredCriteria(int version, List<UUID> tierIds, Integer inactiveDays, UUID categoryId, Labels labels) {
        record Labels(List<String> tiers, String category) {}
    }

    // ------------------------------------------------------------------ đọc

    /** Đợt gửi của chiến dịch kèm số đếm lấy trực tiếp từ danh sách người nhận. */
    List<Dispatch> forCampaign(UUID actor, Campaign campaign, LocalDateTime now) {
        boolean manage = access.canManageCampaign(actor, campaign.storeId());
        boolean approve = access.canApprove(actor);
        boolean superAdmin = approve && access.isSuperAdmin(actor);
        return jdbc.query("""
                select d.*, coalesce(cb.full_name, cb.username) as created_by_name,
                       coalesce(ab.full_name, ab.username) as approved_by_name,
                       (select count(*) from marketing_dispatch_recipients r where r.dispatch_id = d.id
                        and r.status in ('PENDING', 'SENDING')) as pending_count,
                       (select count(*) from marketing_dispatch_recipients r where r.dispatch_id = d.id and r.status = 'SENT') as sent_now,
                       (select count(*) from marketing_dispatch_recipients r where r.dispatch_id = d.id and r.status = 'SKIPPED') as skipped_now,
                       (select count(*) from marketing_dispatch_recipients r where r.dispatch_id = d.id and r.status = 'FAILED') as failed_now,
                       (select count(*) from marketing_dispatch_recipients r where r.dispatch_id = d.id and r.status = 'HOLDOUT') as holdout_now,
                       (select count(*) from marketing_dispatch_recipients r
                        join notifications n on n.event_id = r.event_id and n.recipient_type = 'CUSTOMER' and n.recipient_id = r.customer_id
                        where r.dispatch_id = d.id and r.status = 'SENT' and n.read_at is not null) as read_count
                from marketing_dispatches d
                left join users cb on cb.id = d.created_by
                left join users ab on ab.id = d.approved_by
                where d.campaign_id = :campaign order by d.scheduled_at, d.created_at""",
                new MapSqlParameterSource("campaign", campaign.id()), (rs, i) -> {
                    String status = rs.getString("status");
                    LocalDateTime scheduled = DbTimes.fromDb(rs.getObject("scheduled_at"));
                    UUID createdBy = rs.getObject("created_by", UUID.class);
                    boolean campaignLive = MarketingCampaignService.APPROVED.equals(campaign.status()) && now.isBefore(campaign.endsAt());
                    DispatchActions actions = new DispatchActions(
                            manage && (DRAFT.equals(status) || PENDING.equals(status)),
                            approve && PENDING.equals(status) && campaignLive && scheduled.isAfter(now)
                                    && (!actor.equals(createdBy) || superAdmin),
                            approve && PENDING.equals(status),
                            manage && (DRAFT.equals(status) || PENDING.equals(status) || SCHEDULED.equals(status) || SENDING.equals(status)),
                            manage && COMPLETED_WITH_ERRORS.equals(status) && campaignLive);
                    boolean open = DRAFT.equals(status) || PENDING.equals(status) || SCHEDULED.equals(status);
                    return new Dispatch(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("channel"),
                            rs.getString("title"), rs.getString("message"), rs.getString("action_url"),
                            audience(rs.getString("criteria")), scheduled, status, (Integer) rs.getObject("audience_count"),
                            rs.getInt("pending_count"), rs.getInt("sent_now"), rs.getInt("skipped_now"), rs.getInt("failed_now"),
                            rs.getInt("read_count"), DbTimes.fromDb(rs.getObject("snapshot_at")), DbTimes.fromDb(rs.getObject("started_at")),
                            DbTimes.fromDb(rs.getObject("finished_at")), rs.getString("created_by_name"), rs.getString("approved_by_name"),
                            DbTimes.fromDb(rs.getObject("approved_at")), rs.getString("cancel_reason"),
                            DbTimes.fromDb(rs.getObject("cancelled_at")),
                            open && !insideWindow(scheduled, campaign.startsAt(), campaign.endsAt()), actions,
                            rs.getInt("holdout_percent"), rs.getInt("holdout_now"));
                });
    }

    /** Danh sách người nhận đã chốt của một đợt; số điện thoại che bớt. */
    public Page<MarketingCampaignDtos.Recipient> recipients(UUID actor, UUID campaignId, UUID dispatchId, String status, Pageable pageable) {
        access.requireFinancials(actor, campaignStore(campaignId));
        requireBelongs(campaignId, dispatchId);
        MapSqlParameterSource p = new MapSqlParameterSource("dispatch", dispatchId);
        String where = " where r.dispatch_id = :dispatch";
        if (StringUtils.hasText(status)) {
            String value = status.trim().toUpperCase(Locale.ROOT);
            if (!RECIPIENT_STATUSES.contains(value)) throw BusinessException.badRequest("Trạng thái người nhận không hợp lệ");
            p.addValue("status", value);
            where += " and r.status = :status";
        }
        Long total = jdbc.queryForObject("select count(*) from marketing_dispatch_recipients r" + where, p, Long.class);
        p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<MarketingCampaignDtos.Recipient> rows = jdbc.query("""
                select r.*, c.customer_code, c.full_name, c.phone,
                       (select n.read_at from notifications n where n.event_id = r.event_id and n.recipient_type = 'CUSTOMER'
                        and n.recipient_id = r.customer_id) as read_at
                from marketing_dispatch_recipients r join customers c on c.id = r.customer_id""" + where
                + " order by (r.status = 'FAILED') desc, c.full_name, r.id limit :limit offset :offset", p,
                (rs, i) -> new MarketingCampaignDtos.Recipient(rs.getObject("customer_id", UUID.class), rs.getString("customer_code"),
                        rs.getString("full_name"), maskPhone(rs.getString("phone")), rs.getString("status"), rs.getString("skip_reason"),
                        rs.getInt("attempt_count"), DbTimes.fromDb(rs.getObject("sent_at")), DbTimes.fromDb(rs.getObject("read_at")),
                        rs.getString("last_error_code"), rs.getString("last_error")));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    /**
     * Ước lượng nhóm khách cho một kênh; danh sách thật được chốt đúng lúc bắt đầu gửi nên con số có thể khác. Nhân viên
     * cửa hàng phải chọn chiến dịch (của cửa hàng mình) để nhóm khách được giới hạn theo cửa hàng.
     */
    public MarketingCampaignDtos.Preview preview(UUID actor, MarketingCampaignDtos.PreviewRequest request) {
        UUID store = null;
        if (request.campaignId() != null) {
            store = campaignStore(request.campaignId());
            access.requireManageCampaign(actor, store);
        } else if (!access.isGlobal(actor)) {
            throw BusinessException.badRequest("Chọn chiến dịch để xem trước nhóm khách của cửa hàng");
        } else {
            access.requireManage(actor);
        }
        String channel = channel(request.channel());
        AudienceCriteria criteria = request.criteria();
        checkCriteria(criteria);
        LocalDateTime now = LocalDateTime.now();
        MapSqlParameterSource p = MarketingAudienceSql.params(criteria.tierIdsOrEmpty(), criteria.inactiveDays(), criteria.categoryId(), now)
                .addValue("capSince", DbTimes.toDb(now.minusHours(frequencyCapHours))).addValue("channel", channel)
                .addValue("audienceStore", store, Types.OTHER);
        String where = MarketingAudienceSql.where(criteria.tierIdsOrEmpty(), criteria.inactiveDays(), criteria.categoryId())
                + (store == null ? "" : STORE_AUDIENCE);
        String reach = reachable(channel);
        // Nối chuỗi thường (không dùng text block) để giữ khoảng trắng giữa các phần SQL.
        Map<String, Object> counts = jdbc.queryForMap(MarketingAudienceSql.CATEGORY_TREE
                + " select count(*) as matched, count(*) filter (where " + reach + ") as reachable,"
                + " count(*) filter (where " + reach + " and not " + RECENTLY_SENT + ") as after_cap"
                + " from customers c" + where, p);
        List<MarketingCampaignDtos.PreviewCustomer> sample = jdbc.query(MarketingAudienceSql.CATEGORY_TREE
                + " select c.customer_code, c.full_name, c.phone, " + reach + " as reachable"
                + " from customers c" + where + " order by c.full_name, c.id limit " + SAMPLE_SIZE, p,
                (rs, i) -> new MarketingCampaignDtos.PreviewCustomer(rs.getString("customer_code"), rs.getString("full_name"),
                        maskPhone(rs.getString("phone")), rs.getBoolean("reachable")));
        return new MarketingCampaignDtos.Preview(((Number) counts.get("matched")).longValue(),
                ((Number) counts.get("reachable")).longValue(), ((Number) counts.get("after_cap")).longValue(), frequencyCapHours,
                sample, channel);
    }

    // ------------------------------------------------------------------ ghi

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public UUID create(UUID actor, UUID campaignId, MarketingCampaignDtos.DispatchRequest request) {
        CampaignRow c = lockCampaign(campaignId);
        access.requireManageCampaign(actor, c.storeId());
        LocalDateTime now = LocalDateTime.now();
        String status;
        if (MarketingCampaignService.DRAFT.equals(c.status()) || MarketingCampaignService.REJECTED.equals(c.status())) {
            status = DRAFT;
        } else if (MarketingCampaignService.APPROVED.equals(c.status()) && now.isBefore(c.endsAt())) {
            status = PENDING;
        } else {
            throw BusinessException.conflict("Chỉ thêm đợt gửi khi chiến dịch đang Nháp, Bị từ chối hoặc Đã duyệt và chưa kết thúc");
        }
        Integer count = jdbc.queryForObject("select count(*) from marketing_dispatches where campaign_id = :id and status <> 'CANCELLED'",
                new MapSqlParameterSource("id", campaignId), Integer.class);
        if (count != null && count >= MAX_PER_CAMPAIGN)
            throw BusinessException.conflict("Mỗi chiến dịch có tối đa " + MAX_PER_CAMPAIGN + " đợt gửi");
        MapSqlParameterSource p = fields(request, c, now);
        String code = nextCode();
        UUID id = jdbc.queryForObject("""
                insert into marketing_dispatches(campaign_id, code, channel, title, message, action_url, criteria, scheduled_at,
                                                 status, holdout_percent, created_by)
                values (:campaign, :code, :channel, :title, :message, :actionUrl, cast(:criteria as jsonb), :scheduledAt,
                        :status, :holdout, :actor)
                returning id""", p.addValue("campaign", campaignId).addValue("code", code).addValue("status", status)
                .addValue("actor", actor), UUID.class);
        history.record(campaignId, "DISPATCH_CREATED", c.status(), c.status(), code + " · " + channelLabel((String) p.getValue("channel"))
                + " · " + request.title().trim() + (PENDING.equals(status) ? " (chờ duyệt riêng)" : ""), actor);
        return id;
    }

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public void update(UUID actor, UUID campaignId, UUID dispatchId, MarketingCampaignDtos.DispatchRequest request) {
        CampaignRow c = lockCampaign(campaignId);
        access.requireManageCampaign(actor, c.storeId());
        DispatchRow d = lockDispatch(campaignId, dispatchId);
        if (!DRAFT.equals(d.status()) && !PENDING.equals(d.status()))
            throw BusinessException.conflict("Chỉ sửa được đợt gửi đang Nháp hoặc Chờ duyệt");
        jdbc.update("""
                update marketing_dispatches set channel = :channel, title = :title, message = :message, action_url = :actionUrl,
                       criteria = cast(:criteria as jsonb), scheduled_at = :scheduledAt, holdout_percent = :holdout,
                       updated_at = (now() at time zone 'UTC')
                where id = :id""", fields(request, c, LocalDateTime.now()).addValue("id", dispatchId));
        history.record(campaignId, "DISPATCH_UPDATED", c.status(), c.status(), d.code() + " · " + request.title().trim(), actor);
    }

    /** Duyệt đợt thêm sau khi chiến dịch đã duyệt; cùng luật không tự duyệt như chiến dịch. */
    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public void approve(UUID actor, UUID campaignId, UUID dispatchId) {
        access.requireApprove(actor);
        CampaignRow c = lockCampaign(campaignId);
        DispatchRow d = lockDispatch(campaignId, dispatchId);
        if (!PENDING.equals(d.status())) throw BusinessException.conflict("Chỉ duyệt được đợt gửi đang chờ duyệt");
        LocalDateTime now = LocalDateTime.now();
        if (!MarketingCampaignService.APPROVED.equals(c.status()) || !now.isBefore(c.endsAt()))
            throw BusinessException.conflict("Chiến dịch không còn ở trạng thái Đã duyệt hoặc đã kết thúc");
        if (!d.scheduledAt().isAfter(now)) throw BusinessException.conflict("Giờ gửi đã qua; hãy sửa giờ gửi trước khi duyệt");
        boolean self = actor.equals(d.createdBy());
        if (self && !access.isSuperAdmin(actor)) throw BusinessException.forbidden("Bạn không thể tự duyệt đợt gửi do mình tạo");
        jdbc.update("""
                update marketing_dispatches set status = 'SCHEDULED', approved_by = :actor, approved_at = (now() at time zone 'UTC'),
                       updated_at = (now() at time zone 'UTC') where id = :id and status = 'PENDING_APPROVAL'""",
                new MapSqlParameterSource("id", dispatchId).addValue("actor", actor));
        history.record(campaignId, "DISPATCH_APPROVED", c.status(), c.status(),
                d.code() + (self ? " · SUPER_ADMIN tự duyệt đợt gửi do mình tạo." : ""), actor);
    }

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public void reject(UUID actor, UUID campaignId, UUID dispatchId, String reason) {
        access.requireApprove(actor);
        CampaignRow c = lockCampaign(campaignId);
        DispatchRow d = lockDispatch(campaignId, dispatchId);
        if (!PENDING.equals(d.status())) throw BusinessException.conflict("Chỉ từ chối được đợt gửi đang chờ duyệt");
        String text = reason(reason);
        markCancelled(dispatchId, actor, text);
        history.record(campaignId, "DISPATCH_REJECTED", c.status(), c.status(), d.code() + " · " + text, actor);
    }

    /** Hủy đợt chưa xong; người đã nhận giữ thông báo, người chưa gửi chuyển SKIPPED (CANCELLED). */
    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public void cancel(UUID actor, UUID campaignId, UUID dispatchId, String reason) {
        CampaignRow c = lockCampaign(campaignId);
        access.requireManageCampaign(actor, c.storeId());
        DispatchRow d = lockDispatch(campaignId, dispatchId);
        if (!Set.of(DRAFT, PENDING, SCHEDULED, SENDING).contains(d.status()))
            throw BusinessException.conflict("Đợt gửi đã kết thúc, không hủy được");
        String text = reason(reason);
        markCancelled(dispatchId, actor, text);
        history.record(campaignId, "DISPATCH_CANCELLED", c.status(), c.status(), d.code() + " · " + text, actor);
    }

    /** Gửi lại cho người nhận lỗi của đợt đã hoàn tất có lỗi; chỉ khi chiến dịch còn đang được duyệt và chưa kết thúc. */
    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public void retryFailed(UUID actor, UUID campaignId, UUID dispatchId) {
        CampaignRow c = lockCampaign(campaignId);
        access.requireManageCampaign(actor, c.storeId());
        DispatchRow d = lockDispatch(campaignId, dispatchId);
        if (!COMPLETED_WITH_ERRORS.equals(d.status())) throw BusinessException.conflict("Chỉ thử lại được đợt gửi hoàn tất có lỗi");
        if (!MarketingCampaignService.APPROVED.equals(c.status()) || !LocalDateTime.now().isBefore(c.endsAt()))
            throw BusinessException.conflict("Chiến dịch không còn ở trạng thái Đã duyệt hoặc đã kết thúc");
        int reset = jdbc.update("""
                update marketing_dispatch_recipients set status = 'PENDING', attempt_count = 0,
                       next_attempt_at = (now() at time zone 'UTC'), lease_until = null
                where dispatch_id = :id and status = 'FAILED'""", new MapSqlParameterSource("id", dispatchId));
        jdbc.update("""
                update marketing_dispatches set status = 'SENDING', finished_at = null, stall_notified_at = null,
                       updated_at = (now() at time zone 'UTC') where id = :id""", new MapSqlParameterSource("id", dispatchId));
        history.record(campaignId, "DISPATCH_RETRIED", c.status(), c.status(), d.code() + " · thử lại " + reset + " người nhận lỗi", actor);
    }

    // ------------------------------------------------------------------ theo vòng đời chiến dịch (gọi trong giao dịch của chiến dịch)

    /** Chiến dịch được duyệt: các đợt Nháp phải có giờ gửi hợp lệ, rồi chuyển sang Đã hẹn giờ. */
    void onCampaignApproved(UUID campaignId, UUID actor, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime now) {
        List<DispatchRow> drafts = jdbc.query("select * from marketing_dispatches where campaign_id = :id and status = 'DRAFT'",
                new MapSqlParameterSource("id", campaignId), MarketingDispatchService::dispatchRow);
        List<String> bad = new ArrayList<>();
        for (DispatchRow d : drafts) {
            if (!d.scheduledAt().isAfter(now) || !insideWindow(d.scheduledAt(), startsAt, endsAt)) bad.add(d.code());
        }
        if (!bad.isEmpty()) {
            throw BusinessException.conflict("Giờ gửi của đợt " + String.join(", ", bad) + " đã qua hoặc nằm ngoài khoảng cho phép"
                    + " (từ " + TEASER_DAYS + " ngày trước khi bắt đầu tới lúc kết thúc chiến dịch); hãy từ chối để người tạo sửa lại");
        }
        jdbc.update("""
                update marketing_dispatches set status = 'SCHEDULED', approved_by = :actor, approved_at = (now() at time zone 'UTC'),
                       updated_at = (now() at time zone 'UTC') where campaign_id = :id and status = 'DRAFT'""",
                new MapSqlParameterSource("id", campaignId).addValue("actor", actor));
    }

    /** Mở lại chiến dịch chỉ khi chưa có đợt nào bắt đầu gửi; các đợt đã hẹn giờ / chờ duyệt quay về Nháp. */
    void onCampaignReopened(UUID campaignId) {
        Integer started = jdbc.queryForObject("""
                select count(*) from marketing_dispatches where campaign_id = :id
                  and status in ('SENDING', 'COMPLETED', 'COMPLETED_WITH_ERRORS')""", new MapSqlParameterSource("id", campaignId), Integer.class);
        if (started != null && started > 0) throw BusinessException.conflict("Đã có đợt gửi bắt đầu gửi nên không mở lại được chiến dịch");
        jdbc.update("""
                update marketing_dispatches set status = 'DRAFT', approved_by = null, approved_at = null,
                       updated_at = (now() at time zone 'UTC')
                where campaign_id = :id and status in ('SCHEDULED', 'PENDING_APPROVAL')""", new MapSqlParameterSource("id", campaignId));
    }

    /** Hủy chiến dịch: mọi đợt chưa xong bị hủy theo. */
    int onCampaignCancelled(UUID campaignId, UUID actor, String reason) {
        List<UUID> open = jdbc.queryForList("select id from marketing_dispatches where campaign_id = :id and status in " + OPEN
                + " for update", new MapSqlParameterSource("id", campaignId), UUID.class);
        for (UUID id : open) markCancelled(id, actor, "Chiến dịch bị hủy: " + reason);
        return open.size();
    }

    // ------------------------------------------------------------------ hỗ trợ

    /**
     * Người đang SENDING trong lô hiện tại cũng chuyển SKIPPED; nếu worker đã kịp gửi, nó ghi đè lại thành SENT
     * (xem {@link MarketingDispatchWorker#recordResults}) nên số đếm vẫn đúng với thực tế.
     */
    private void markCancelled(UUID dispatchId, UUID actor, String reason) {
        jdbc.update("""
                update marketing_dispatches set status = 'CANCELLED', cancelled_by = cast(:actor as uuid),
                       cancelled_at = (now() at time zone 'UTC'), cancel_reason = :reason, updated_at = (now() at time zone 'UTC')
                where id = :id""", new MapSqlParameterSource("id", dispatchId).addValue("actor", actor, Types.OTHER)
                .addValue("reason", cut(reason, 500)));
        jdbc.update("""
                update marketing_dispatch_recipients set status = 'SKIPPED', skip_reason = 'CANCELLED', lease_until = null
                where dispatch_id = :id and status in ('PENDING', 'SENDING')""", new MapSqlParameterSource("id", dispatchId));
    }

    /** Kiểm tra yêu cầu và trả về tham số dùng chung cho insert / update. */
    private MapSqlParameterSource fields(MarketingCampaignDtos.DispatchRequest r, CampaignRow c, LocalDateTime now) {
        String channel = channel(r.channel());
        String title = r.title().trim();
        String message = r.message().trim();
        if (title.length() < 3) throw BusinessException.badRequest("Tiêu đề cần ít nhất 3 ký tự");
        String actionUrl = actionUrl(r.actionUrl());
        int holdout = r.holdoutPercent() == null ? 0 : r.holdoutPercent();
        if (holdout < 0 || holdout > MAX_HOLDOUT_PERCENT)
            throw BusinessException.badRequest("Tỷ lệ nhóm đối chứng từ 0 đến " + MAX_HOLDOUT_PERCENT + "%");
        if (!r.scheduledAt().isAfter(now)) throw BusinessException.badRequest("Giờ gửi phải sau thời điểm hiện tại");
        if (!insideWindow(r.scheduledAt(), c.startsAt(), c.endsAt()))
            throw BusinessException.badRequest("Giờ gửi phải từ " + TEASER_DAYS + " ngày trước khi chiến dịch bắt đầu tới lúc chiến dịch kết thúc");
        StoredCriteria criteria = checkCriteria(r.criteria());
        return new MapSqlParameterSource("title", title).addValue("message", message).addValue("actionUrl", actionUrl, Types.VARCHAR)
                .addValue("criteria", json.writeValueAsString(criteria)).addValue("scheduledAt", DbTimes.toDb(r.scheduledAt()))
                .addValue("channel", channel).addValue("holdout", holdout);
    }

    static String channel(String value) {
        String channel = value == null || value.isBlank() ? IN_APP : value.trim().toUpperCase(Locale.ROOT);
        if (!IN_APP.equals(channel) && !EMAIL.equals(channel)) throw BusinessException.badRequest("Kênh gửi không hợp lệ");
        return channel;
    }

    static String channelLabel(String channel) {
        return EMAIL.equals(channel) ? "Email" : "Thông báo trong app";
    }

    /** Hạng và danh mục phải tồn tại; trả về tiêu chí kèm nhãn để lưu. */
    private StoredCriteria checkCriteria(AudienceCriteria criteria) {
        List<UUID> tiers = criteria.tierIdsOrEmpty();
        List<String> tierNames = List.of();
        if (!tiers.isEmpty()) {
            tierNames = jdbc.queryForList("select name from customer_tiers where id in (:ids) order by min_total_spent nulls first, name",
                    new MapSqlParameterSource("ids", tiers), String.class);
            if (tierNames.size() != tiers.size()) throw BusinessException.notFound("Có hạng khách hàng không tồn tại");
        }
        String category = null;
        if (criteria.categoryId() != null) {
            category = jdbc.queryForList("select name from categories where id = :id", new MapSqlParameterSource("id", criteria.categoryId()),
                    String.class).stream().findFirst().orElseThrow(() -> BusinessException.notFound("Danh mục không tồn tại"));
        }
        return new StoredCriteria(1, tiers, criteria.inactiveDays(), criteria.categoryId(), new StoredCriteria.Labels(tierNames, category));
    }

    StoredCriteria readCriteria(String value) {
        return json.readValue(value, StoredCriteria.class);
    }

    private Audience audience(String value) {
        StoredCriteria c = readCriteria(value);
        StoredCriteria.Labels labels = c.labels() == null ? new StoredCriteria.Labels(List.of(), null) : c.labels();
        return new Audience(c.tierIds() == null ? List.of() : c.tierIds(), c.inactiveDays(), c.categoryId(),
                labels.tiers() == null ? List.of() : labels.tiers(), labels.category());
    }

    /** Giờ gửi hợp lệ: từ {@value #TEASER_DAYS} ngày trước khi bắt đầu tới lúc kết thúc chiến dịch. */
    static boolean insideWindow(LocalDateTime at, LocalDateTime startsAt, LocalDateTime endsAt) {
        return !at.isBefore(startsAt.minusDays(TEASER_DAYS)) && !at.isAfter(endsAt);
    }

    /** Đường dẫn nội bộ của trang khách (khớp customerNotificationLogic.js): bắt đầu bằng "/", không "//", không "/admin". */
    static String actionUrl(String value) {
        if (!StringUtils.hasText(value)) return null;
        String url = value.trim();
        if (!url.startsWith("/") || url.startsWith("//") || url.startsWith("/admin") || url.chars().anyMatch(Character::isWhitespace))
            throw BusinessException.badRequest("Liên kết phải là đường dẫn nội bộ của trang khách, ví dụ /promotions");
        return url;
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    record CampaignRow(UUID id, String status, LocalDateTime startsAt, LocalDateTime endsAt, UUID storeId) {}

    record DispatchRow(UUID id, UUID campaignId, String code, String status, LocalDateTime scheduledAt, UUID createdBy) {}

    private CampaignRow lockCampaign(UUID id) {
        List<CampaignRow> rows = jdbc.query("select id, status, starts_at, ends_at, store_id from marketing_campaigns where id = :id for update",
                new MapSqlParameterSource("id", id), (rs, i) -> new CampaignRow(rs.getObject("id", UUID.class), rs.getString("status"),
                        DbTimes.fromDb(rs.getObject("starts_at")), DbTimes.fromDb(rs.getObject("ends_at")), rs.getObject("store_id", UUID.class)));
        if (rows.isEmpty()) throw BusinessException.notFound("Chiến dịch không tồn tại");
        return rows.getFirst();
    }

    private UUID campaignStore(UUID campaignId) {
        List<Map<String, Object>> rows = jdbc.queryForList("select store_id from marketing_campaigns where id = :id",
                new MapSqlParameterSource("id", campaignId));
        if (rows.isEmpty()) throw BusinessException.notFound("Chiến dịch không tồn tại");
        return (UUID) rows.getFirst().get("store_id");
    }

    private DispatchRow lockDispatch(UUID campaignId, UUID dispatchId) {
        List<DispatchRow> rows = jdbc.query("select * from marketing_dispatches where id = :id and campaign_id = :campaign for update",
                new MapSqlParameterSource("id", dispatchId).addValue("campaign", campaignId), MarketingDispatchService::dispatchRow);
        if (rows.isEmpty()) throw BusinessException.notFound("Đợt gửi không thuộc chiến dịch này");
        return rows.getFirst();
    }

    private void requireBelongs(UUID campaignId, UUID dispatchId) {
        Boolean found = jdbc.queryForObject("select exists (select 1 from marketing_dispatches where id = :id and campaign_id = :campaign)",
                new MapSqlParameterSource("id", dispatchId).addValue("campaign", campaignId), Boolean.class);
        if (!Boolean.TRUE.equals(found)) throw BusinessException.notFound("Đợt gửi không thuộc chiến dịch này");
    }

    private static DispatchRow dispatchRow(ResultSet rs, int row) throws SQLException {
        return new DispatchRow(rs.getObject("id", UUID.class), rs.getObject("campaign_id", UUID.class), rs.getString("code"),
                rs.getString("status"), DbTimes.fromDb(rs.getObject("scheduled_at")), rs.getObject("created_by", UUID.class));
    }

    private String nextCode() {
        Long value = jdbc.getJdbcTemplate().queryForObject("select nextval('marketing_dispatch_code_seq')", Long.class);
        return "MD-%s-%04d".formatted(LocalDate.now(ZONE).format(CODE_DATE), value);
    }

    private static String reason(String value) {
        String text = StringUtils.hasText(value) ? value.trim() : null;
        if (text == null || text.length() < 5) throw BusinessException.badRequest("Lý do cần ít nhất 5 ký tự");
        if (text.length() > 500) throw BusinessException.badRequest("Lý do tối đa 500 ký tự");
        return text;
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
