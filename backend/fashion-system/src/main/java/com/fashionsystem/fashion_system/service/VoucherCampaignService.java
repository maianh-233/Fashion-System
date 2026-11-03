package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.dto.promotion.VoucherCampaignDtos;
import com.fashionsystem.fashion_system.dto.promotion.VoucherCampaignDtos.Criteria;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Chiến dịch voucher theo nhóm khách. Admin chọn nhóm (hạng, lâu chưa mua, từng mua danh mục; thỏa tất cả tiêu chí),
 * nhập mức giảm và số ngày được dùng; hệ thống tạo một khuyến mãi loại CAMPAIGN làm mẫu. Khi gửi (ngay hoặc đúng giờ
 * hẹn, {@link VoucherCampaignScheduler}) nhóm được tính lại, mỗi khách nhận một voucher riêng giảm trên toàn đơn, dùng
 * một lần, online lẫn tại quầy, tới hết ngày thứ {@code validDays} kể từ ngày gửi; thông báo và email đi trên luồng nền
 * sau khi commit. Mặc định bỏ qua khách đã nhận voucher chiến dịch trong
 * {@value VoucherCampaignDtos#DEFAULT_SKIP_RECENT_DAYS} ngày.
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("VOUCHER_CAMPAIGN")
@Transactional(readOnly = true)
public class VoucherCampaignService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int SAMPLE_SIZE = 10;
    /** Hẹn giờ lùi về quá khứ quá chừng này phút thì báo lỗi (cho phép lệch đồng hồ khi bấm "gửi ngay"). */
    private static final int PAST_TOLERANCE_MINUTES = 5;

    /** Danh mục được chọn và mọi danh mục con. */
    private static final String CATEGORY_TREE = """
            with recursive cat_tree as (
                select id from categories where id = :categoryId
                union all
                select ch.id from categories ch join cat_tree t on ch.parent_id = t.id
            )""";

    private final NamedParameterJdbcTemplate jdbc;
    private final PromotionService promotions;
    private final NotificationPublisher notifications;
    private final MailHelper mail;
    /** Kafka/SMTP chậm không được giữ luồng request hay scheduler. */
    private final TaskExecutor delivery;

    public VoucherCampaignService(NamedParameterJdbcTemplate jdbc, PromotionService promotions,
            NotificationPublisher notifications, MailHelper mail,
            @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.jdbc = jdbc;
        this.promotions = promotions;
        this.notifications = notifications;
        this.mail = mail;
        this.delivery = delivery;
    }

    record Issued(UUID id, UUID customerId, String fullName, String email) {
    }

    record Sending(UUID id, String name, String message, UUID promotionId, String promotionCode,
                   BirthdayVoucherService.Template template, int validDays, Criteria criteria) {
    }

    // ------------------------------------------------------------------ đọc

    /** Số khách khớp nhóm lúc này và vài khách mẫu. */
    public VoucherCampaignDtos.Preview preview(Criteria criteria) {
        LocalDateTime now = PromotionService.now();
        MapSqlParameterSource params = segmentParams(criteria, now);
        String where = segment(criteria);
        Long count = jdbc.queryForObject(CATEGORY_TREE + " select count(*) from customers c" + where, params, Long.class);
        List<VoucherCampaignDtos.Recipient> sample = jdbc.query(CATEGORY_TREE + """
                 select c.id, c.customer_code, c.full_name, c.phone,
                        (select t.name from customer_tier_assignments a join customer_tiers t on t.id = a.tier_id
                         where a.customer_id = c.id and a.expires_at is null limit 1) as tier_name
                 from customers c""" + where + " order by c.full_name, c.id limit " + SAMPLE_SIZE, params,
                (rs, i) -> new VoucherCampaignDtos.Recipient(rs.getObject("id", UUID.class), rs.getString("customer_code"),
                        rs.getString("full_name"), rs.getString("phone"), rs.getString("tier_name")));
        return new VoucherCampaignDtos.Preview(count == null ? 0 : count, sample);
    }

    public Page<VoucherCampaignDtos.Campaign> list(Pageable pageable) {
        Long total = jdbc.queryForObject("select count(*) from voucher_campaigns", new MapSqlParameterSource(), Long.class);
        List<UUID> ids = jdbc.queryForList("select id from voucher_campaigns order by created_at desc, id limit :limit offset :offset",
                new MapSqlParameterSource("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()), UUID.class);
        return new PageImpl<>(views(ids), pageable, total == null ? 0 : total);
    }

    public VoucherCampaignDtos.Campaign detail(UUID id) {
        List<VoucherCampaignDtos.Campaign> found = views(List.of(id));
        if (found.isEmpty()) throw BusinessException.notFound("Chiến dịch không tồn tại");
        return found.getFirst();
    }

    // ------------------------------------------------------------------ ghi

    /** Tạo chiến dịch (và mẫu voucher); không hẹn giờ thì gửi luôn trong cùng giao dịch. */
    @Transactional
    public VoucherCampaignDtos.Campaign create(UUID actor, VoucherCampaignDtos.CreateRequest request) {
        LocalDateTime now = PromotionService.now();
        validate(request, now);
        boolean sendNow = request.scheduledAt() == null || !request.scheduledAt().isAfter(now);
        LocalDateTime scheduledAt = sendNow ? now : request.scheduledAt();
        Criteria criteria = request.criteria();
        List<UUID> tierIds = criteria.tierIdsOrEmpty();
        if (!tierIds.isEmpty()) {
            Integer found = jdbc.queryForObject("select count(*) from customer_tiers where id in (:ids)",
                    new MapSqlParameterSource("ids", tierIds), Integer.class);
            if (found == null || found != tierIds.size()) throw BusinessException.notFound("Có hạng khách hàng không tồn tại");
        }
        if (criteria.categoryId() != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from categories where id = :id)",
                new MapSqlParameterSource("id", criteria.categoryId()), Boolean.class)))
            throw BusinessException.notFound("Danh mục không tồn tại");

        Promotion promotion = promotions.createCampaignVoucher(request.name(), request.discountType(),
                request.discountValue(), request.maxDiscount(), request.minOrderValue(), scheduledAt,
                expiresAt(scheduledAt, request.validDays()));
        UUID id = jdbc.queryForObject("""
                insert into voucher_campaigns (code, name, status, promotion_id, inactive_days, category_id,
                                               skip_recent_days, valid_days, message, scheduled_at, created_by, created_at)
                values (:code, :name, 'SCHEDULED', :promotionId, :inactiveDays, :categoryId, :skipRecentDays, :validDays,
                        :message, :scheduledAt, :actor, now() at time zone 'UTC')
                returning id""",
                new MapSqlParameterSource("code", nextCode(now)).addValue("name", request.name().trim())
                        .addValue("promotionId", promotion.getId()).addValue("inactiveDays", criteria.inactiveDays())
                        .addValue("categoryId", criteria.categoryId())
                        .addValue("skipRecentDays", criteria.skipRecentOrDefault() ? VoucherCampaignDtos.DEFAULT_SKIP_RECENT_DAYS : null)
                        .addValue("validDays", request.validDays())
                        .addValue("message", StringUtils.hasText(request.message()) ? request.message().trim() : null)
                        .addValue("scheduledAt", DbTimes.toDb(scheduledAt)).addValue("actor", actor),
                UUID.class);
        for (UUID tierId : tierIds)
            jdbc.update("insert into voucher_campaign_tiers (campaign_id, tier_id) values (:id, :tierId)",
                    new MapSqlParameterSource("id", id).addValue("tierId", tierId));
        if (sendNow) send(id, now);
        return detail(id);
    }

    /** Hủy chiến dịch chưa gửi; mẫu voucher chuyển ngừng hoạt động. */
    @Transactional
    public VoucherCampaignDtos.Campaign cancel(UUID actor, UUID id) {
        UUID promotionId = jdbc.queryForList("""
                update voucher_campaigns set status = 'CANCELLED', cancelled_by = :actor,
                       cancelled_at = now() at time zone 'UTC'
                where id = :id and status = 'SCHEDULED' returning promotion_id""",
                new MapSqlParameterSource("id", id).addValue("actor", actor), UUID.class).stream().findFirst().orElse(null);
        if (promotionId == null) {
            detail(id); // 404 nếu không tồn tại
            throw BusinessException.invalidState("Chỉ hủy được chiến dịch chưa gửi");
        }
        promotions.delete(promotionId);
        return detail(id);
    }

    /**
     * Gửi các chiến dịch đã tới giờ hẹn (gọi từ scheduler trong ngữ cảnh audit hệ thống).
     *
     * @return số chiến dịch đã gửi
     */
    @Transactional
    public int sendDue(LocalDateTime now) {
        List<UUID> due = jdbc.queryForList("""
                select id from voucher_campaigns where status = 'SCHEDULED' and scheduled_at <= :now
                order by scheduled_at limit 20 for update skip locked""",
                new MapSqlParameterSource("now", DbTimes.toDb(now)), UUID.class);
        due.forEach(id -> send(id, now));
        return due.size();
    }

    // ------------------------------------------------------------------ gửi

    /** Phát voucher cho nhóm khách tính tại thời điểm gửi, đánh dấu đã gửi và hẹn gửi thông báo sau commit. */
    private void send(UUID id, LocalDateTime now) {
        Sending campaign = sending(id);
        LocalDateTime expiresAt = expiresAt(now, campaign.validDays());
        MapSqlParameterSource params = segmentParams(campaign.criteria(), now)
                .addValue("promotionId", campaign.promotionId()).addValue("campaignId", id)
                .addValue("now", DbTimes.toDb(now)).addValue("expiresAt", DbTimes.toDb(expiresAt));
        List<Issued> issued = jdbc.query(CATEGORY_TREE + """
                , inserted as (
                    insert into customer_promotions (customer_id, promotion_id, points_spent, redeemed_at, source,
                                                     valid_from, expires_at, campaign_id)
                    select c.id, :promotionId, 0, :now, 'CAMPAIGN', :now, :expiresAt, :campaignId
                    from customers c""" + segment(campaign.criteria()) + """

                    on conflict (campaign_id, customer_id) where source = 'CAMPAIGN' do nothing
                    returning id, customer_id
                )
                select i.id, i.customer_id, c.full_name, c.email
                from inserted i join customers c on c.id = i.customer_id""", params,
                (rs, i) -> new Issued(rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
                        rs.getString("full_name"), rs.getString("email")));
        jdbc.update("""
                update voucher_campaigns set status = 'SENT', sent_at = :now, recipient_count = :count where id = :id""",
                new MapSqlParameterSource("now", DbTimes.toDb(now)).addValue("count", issued.size()).addValue("id", id));
        if (issued.isEmpty()) return;
        LocalDateTime lastDay = expiresAt.minusDays(1);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                delivery.execute(() -> issued.forEach(voucher -> deliver(campaign, voucher, lastDay)));
            }
        });
    }

    private void deliver(Sending campaign, Issued voucher, LocalDateTime lastDay) {
        String message = message(campaign, lastDay);
        try {
            notifications.publish(new NotificationRequest(List.of(RealtimeIdentity.customer(voucher.customerId())), null,
                    NotificationType.VOUCHER_CAMPAIGN, "Lunaria tặng bạn voucher", message,
                    "PROMOTION", campaign.promotionId(), "/vouchers?tab=mine",
                    Map.of("voucherId", voucher.id().toString(), "promotionCode", campaign.promotionCode(),
                            "campaignId", campaign.id().toString())));
        } catch (RuntimeException failure) {
            log.warn("Campaign voucher notification {} failed: {}", voucher.id(), failure.getMessage());
        }
        if (!StringUtils.hasText(voucher.email())) return;
        try {
            String name = StringUtils.hasText(voucher.fullName()) ? voucher.fullName().trim() : "bạn";
            mail.sendText(voucher.email(), campaign.name() + " - quà tặng từ Lunaria",
                    "Chào " + name + ",\n\n" + message
                            + "\n\nVoucher đã có sẵn trong tài khoản của bạn (mục Voucher của tôi), chỉ cần chọn khi"
                            + " thanh toán hoặc báo nhân viên tại quầy.\n\n— Lunaria Boutique");
        } catch (RuntimeException failure) {
            log.warn("Campaign voucher email {} failed: {}", voucher.id(), failure.getMessage());
        }
    }

    /** "{lời nhắn} Lunaria tặng bạn voucher KM_ABC giảm 15% …, dùng được tới hết ngày dd/MM/yyyy …" */
    static String message(Sending campaign, LocalDateTime lastDay) {
        String intro = StringUtils.hasText(campaign.message()) ? campaign.message().trim() + " " : "";
        return intro + "Lunaria tặng bạn voucher " + campaign.promotionCode() + " "
                + BirthdayVoucherService.benefit(campaign.template()) + ", dùng được tới hết ngày "
                + lastDay.format(DATE) + " khi mua online hoặc tại cửa hàng.";
    }

    // ------------------------------------------------------------------ nhóm khách

    /** Điều kiện lọc khách {@code c} theo tiêu chí; dùng sau {@link #CATEGORY_TREE}. */
    static String segment(Criteria criteria) {
        StringBuilder where = new StringBuilder(" where c.active");
        if (!criteria.tierIdsOrEmpty().isEmpty())
            where.append(" ").append("""
                     and exists (select 1 from customer_tier_assignments a where a.customer_id = c.id
                                 and a.expires_at is null and a.tier_id in (:tierIds))""");
        if (criteria.inactiveDays() != null)
            where.append(" ").append("""
                     and exists (select 1 from orders o where o.customer_id = c.id and o.status = 'COMPLETED')
                     and not exists (select 1 from orders o where o.customer_id = c.id and o.status <> 'CANCELLED'
                                     and o.created_at >= :inactiveSince)""");
        if (criteria.categoryId() != null)
            where.append(" ").append("""
                     and exists (select 1 from orders o join order_items oi on oi.order_id = o.id
                                 join products p on p.id = oi.product_id
                                 where o.customer_id = c.id and o.status = 'COMPLETED'
                                   and p.category_id in (select id from cat_tree))""");
        if (criteria.skipRecentOrDefault())
            where.append(" ").append("""
                     and not exists (select 1 from customer_promotions x where x.customer_id = c.id
                                     and x.source = 'CAMPAIGN' and x.redeemed_at >= :recentSince)""");
        return where.toString();
    }

    private static MapSqlParameterSource segmentParams(Criteria criteria, LocalDateTime now) {
        return new MapSqlParameterSource("categoryId", criteria.categoryId())
                .addValue("tierIds", criteria.tierIdsOrEmpty())
                .addValue("inactiveSince", criteria.inactiveDays() == null ? null
                        : DbTimes.toDb(now.minusDays(criteria.inactiveDays())))
                .addValue("recentSince", DbTimes.toDb(now.minusDays(VoucherCampaignDtos.DEFAULT_SKIP_RECENT_DAYS)));
    }

    // ------------------------------------------------------------------ tiện ích

    /** Voucher dùng được tới hết ngày thứ {@code validDays} kể từ ngày gửi. */
    static LocalDateTime expiresAt(LocalDateTime sentAt, int validDays) {
        return sentAt.toLocalDate().plusDays(validDays + 1L).atStartOfDay();
    }

    private static void validate(VoucherCampaignDtos.CreateRequest request, LocalDateTime now) {
        if ("PERCENT".equals(request.discountType()) && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            throw BusinessException.badRequest("Phần trăm giảm giá không được vượt quá 100");
        if (request.scheduledAt() != null && request.scheduledAt().isBefore(now.minusMinutes(PAST_TOLERANCE_MINUTES)))
            throw BusinessException.badRequest("Giờ hẹn gửi đã qua");
        if (request.scheduledAt() != null && request.scheduledAt().isAfter(now.plusDays(180)))
            throw BusinessException.badRequest("Chỉ hẹn gửi trong vòng 180 ngày");
    }

    private String nextCode(LocalDateTime now) {
        Long value = jdbc.getJdbcTemplate().queryForObject("select nextval('voucher_campaign_code_seq')", Long.class);
        return "CD-" + now.format(CODE_DATE) + "-" + String.format("%04d", value == null ? 0 : value);
    }

    private Sending sending(UUID id) {
        Sending campaign = jdbc.query("""
                select vc.id, vc.name, vc.message, vc.valid_days, vc.inactive_days, vc.category_id, vc.skip_recent_days,
                       p.id as promotion_id, p.code, p.name as promotion_name, p.discount_type, p.discount_value,
                       p.max_discount, p.min_order_value
                from voucher_campaigns vc join promotions p on p.id = vc.promotion_id where vc.id = :id""",
                new MapSqlParameterSource("id", id), (rs, i) -> new Sending(rs.getObject("id", UUID.class),
                        rs.getString("name"), rs.getString("message"), rs.getObject("promotion_id", UUID.class),
                        rs.getString("code"),
                        new BirthdayVoucherService.Template(rs.getObject("promotion_id", UUID.class), rs.getString("code"),
                                rs.getString("promotion_name"), rs.getString("discount_type"),
                                rs.getBigDecimal("discount_value"), rs.getBigDecimal("max_discount"),
                                rs.getBigDecimal("min_order_value")),
                        rs.getInt("valid_days"),
                        new Criteria(null, (Integer) rs.getObject("inactive_days"), rs.getObject("category_id", UUID.class),
                                rs.getObject("skip_recent_days") != null)))
                .stream().findFirst().orElseThrow(() -> BusinessException.notFound("Chiến dịch không tồn tại"));
        List<UUID> tiers = jdbc.queryForList("select tier_id from voucher_campaign_tiers where campaign_id = :id",
                new MapSqlParameterSource("id", id), UUID.class);
        Criteria criteria = campaign.criteria();
        return new Sending(campaign.id(), campaign.name(), campaign.message(), campaign.promotionId(),
                campaign.promotionCode(), campaign.template(), campaign.validDays(),
                new Criteria(tiers, criteria.inactiveDays(), criteria.categoryId(), criteria.skipRecent()));
    }

    private List<VoucherCampaignDtos.Campaign> views(List<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        Map<UUID, List<String>> tierNames = new HashMap<>();
        jdbc.query("""
                select ct.campaign_id, t.name from voucher_campaign_tiers ct join customer_tiers t on t.id = ct.tier_id
                where ct.campaign_id in (:ids) order by t.min_total_spent nulls first, t.name""", params, rs -> {
            tierNames.computeIfAbsent(rs.getObject("campaign_id", UUID.class), ignored -> new ArrayList<>())
                    .add(rs.getString("name"));
        });
        Map<UUID, VoucherCampaignDtos.Campaign> byId = new HashMap<>();
        jdbc.query("""
                select vc.*, p.code as promotion_code, p.discount_type, p.discount_value, p.max_discount, p.min_order_value,
                       cat.name as category_name, u.full_name as created_by_name,
                       (select count(*) from customer_promotions cp where cp.campaign_id = vc.id
                        and cp.used_order_id is not null) as used_count,
                       (select coalesce(sum(o.total_amount), 0) from customer_promotions cp join orders o on o.id = cp.used_order_id
                        where cp.campaign_id = vc.id and o.status <> 'CANCELLED') as used_revenue
                from voucher_campaigns vc
                join promotions p on p.id = vc.promotion_id
                left join categories cat on cat.id = vc.category_id
                left join users u on u.id = vc.created_by
                where vc.id in (:ids)""", params, rs -> {
            UUID id = rs.getObject("id", UUID.class);
            String status = rs.getString("status");
            byId.put(id, new VoucherCampaignDtos.Campaign(id, rs.getString("code"), rs.getString("name"), status,
                    VoucherCampaignDtos.STATUS_LABELS.getOrDefault(status, status), rs.getObject("promotion_id", UUID.class),
                    rs.getString("promotion_code"), rs.getString("discount_type"), rs.getBigDecimal("discount_value"),
                    rs.getBigDecimal("max_discount"), rs.getBigDecimal("min_order_value"), rs.getInt("valid_days"),
                    rs.getString("message"), tierNames.getOrDefault(id, List.of()), (Integer) rs.getObject("inactive_days"),
                    rs.getObject("category_id", UUID.class), rs.getString("category_name"),
                    (Integer) rs.getObject("skip_recent_days"), time(rs.getTimestamp("scheduled_at")),
                    time(rs.getTimestamp("sent_at")), (Integer) rs.getObject("recipient_count"), rs.getLong("used_count"),
                    rs.getBigDecimal("used_revenue"), rs.getString("created_by_name"), time(rs.getTimestamp("created_at")),
                    time(rs.getTimestamp("cancelled_at"))));
        });
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
    }

    private static LocalDateTime time(Timestamp value) {
        return DbTimes.fromDb(value);
    }
}
