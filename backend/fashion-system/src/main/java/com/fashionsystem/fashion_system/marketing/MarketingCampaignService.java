package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Actions;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Campaign;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Candidate;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Expense;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Offer;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Chiến dịch marketing: lớp điều phối phía trên Promotion, Voucher Campaign và Flash Sale có sẵn. Module này chỉ liên kết
 * ưu đãi, không tạo / sửa / tắt chúng — hủy chiến dịch chỉ cảnh báo các ưu đãi còn hiệu lực.
 * Trạng thái: Nháp → Chờ duyệt → Đã duyệt / Bị từ chối; hủy được từ mọi trạng thái. Giai đoạn của chiến dịch đã duyệt
 * (sắp diễn ra / đang chạy / đã kết thúc) tính từ thời gian, không lưu.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketingCampaignService {
    static final String DRAFT = "DRAFT";
    static final String PENDING = "PENDING_APPROVAL";
    static final String APPROVED = "APPROVED";
    static final String REJECTED = "REJECTED";
    static final String CANCELLED = "CANCELLED";
    static final String PROMOTION = "PROMOTION";
    static final String VOUCHER_CAMPAIGN = "VOUCHER_CAMPAIGN";
    static final String FLASH_SALE = "FLASH_SALE";

    private static final Set<String> STATUSES = Set.of(DRAFT, PENDING, APPROVED, REJECTED, CANCELLED);
    private static final Set<String> EDITABLE = Set.of(DRAFT, REJECTED);
    private static final Set<String> OBJECTIVES = Set.of("REVENUE", "ACQUISITION", "RETENTION", "CLEARANCE", "AWARENESS");
    private static final Set<String> PHASES = Set.of("UPCOMING", "RUNNING", "ENDED");
    private static final Map<String, String> CATEGORIES = Map.of("ADS", "Quảng cáo", "KOL", "KOL / KOC",
            "PRINTING", "In ấn", "EVENT", "Sự kiện", "OTHER", "Khác");
    private static final Map<String, String> OFFER_LABELS = Map.of(PROMOTION, "Khuyến mãi",
            VOUCHER_CAMPAIGN, "Chiến dịch voucher", FLASH_SALE, "Flash sale");
    private static final long MAX_DAYS = 366;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private static final String SELECT = """
            select c.*, coalesce(ow.full_name, ow.username) as owner_name,
                   coalesce(cb.full_name, cb.username) as created_by_name,
                   coalesce(sb.full_name, sb.username) as submitted_by_name,
                   coalesce(db.full_name, db.username) as decided_by_name,
                   coalesce(xb.full_name, xb.username) as cancelled_by_name, st.name as store_name,
                   (select count(*) from marketing_campaign_offers mo where mo.campaign_id = c.id) as offer_count,
                   (select coalesce(sum(e.amount), 0) from marketing_campaign_expenses e where e.campaign_id = c.id) as expense_total
            """;
    private static final String FROM = """
            from marketing_campaigns c
            left join users ow on ow.id = c.owner_id
            left join users cb on cb.id = c.created_by
            left join users sb on sb.id = c.submitted_by
            left join users db on db.id = c.decided_by
            left join users xb on xb.id = c.cancelled_by
            left join stores st on st.id = c.store_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final MarketingCampaignAccess access;
    private final MarketingCampaignNotifier notifier;
    private final MarketingCampaignHistory historyLog;
    private final MarketingDispatchService dispatches;

    // ------------------------------------------------------------------ danh mục

    public MarketingCampaignDtos.Lookups lookups(UUID actor) {
        boolean create = access.canCreate(actor);
        boolean global = access.isGlobal(actor);
        UUID ownStore = access.ownStore(actor);
        MapSqlParameterSource none = new MapSqlParameterSource();
        List<MarketingCampaignDtos.Ref> tiers = create ? jdbc.query("select id, name from customer_tiers order by min_total_spent nulls first, name",
                none, (rs, i) -> new MarketingCampaignDtos.Ref(rs.getObject("id", UUID.class), rs.getString("name"))) : List.of();
        List<MarketingCampaignDtos.Ref> categories = create ? jdbc.query("select id, name from categories where active order by name",
                none, (rs, i) -> new MarketingCampaignDtos.Ref(rs.getObject("id", UUID.class), rs.getString("name"))) : List.of();
        List<MarketingCampaignDtos.Ref> stores = global && create ? jdbc.query("select id, name from stores where active order by name",
                none, (rs, i) -> new MarketingCampaignDtos.Ref(rs.getObject("id", UUID.class), rs.getString("name"))) : List.of();
        List<MarketingCampaignDtos.Ref> owners = !create ? List.of() : global ? access.globalEmployees() : access.storeEmployees(ownStore);
        String storeName = ownStore == null ? null : jdbc.queryForObject("select name from stores where id = :id",
                new MapSqlParameterSource("id", ownStore), String.class);
        return new MarketingCampaignDtos.Lookups(global, create, access.canApprove(actor),
                access.canLinkOffers(actor, ownStore), owners, tiers, categories, ownStore, storeName, stores);
    }

    // ------------------------------------------------------------------ truy vấn

    /**
     * Nhân viên cửa hàng thấy chiến dịch đã duyệt của toàn chuỗi và của cửa hàng mình; người quản lý cửa hàng thấy thêm
     * mọi trạng thái của chiến dịch cửa hàng mình. Số liệu tài chính chỉ hiện với người xem được tài chính của chiến dịch đó.
     */
    public Page<Campaign> list(UUID actor, String status, String phase, String objective, LocalDate from, LocalDate to,
                               String keyword, Pageable pageable) {
        boolean global = access.isGlobal(actor);
        UUID ownStore = access.ownStore(actor);
        boolean manageOwn = ownStore != null && access.canManageCampaign(actor, ownStore);
        LocalDateTime now = now();
        StringBuilder where = new StringBuilder(" where true");
        MapSqlParameterSource p = new MapSqlParameterSource("now", DbTimes.toDb(now));
        if (!global) {
            p.addValue("ownStore", ownStore);
            where.append(" and ((c.status = 'APPROVED' and (c.store_id is null or c.store_id = :ownStore))")
                    .append(manageOwn ? " or c.store_id = :ownStore)" : ")");
        }
        if (StringUtils.hasText(status)) {
            p.addValue("status", oneOf(status, STATUSES, "Trạng thái không hợp lệ"));
            where.append(" and c.status = :status");
        }
        if (StringUtils.hasText(phase)) {
            String value = oneOf(phase, PHASES, "Giai đoạn không hợp lệ");
            where.append(" and c.status = 'APPROVED' and ").append(switch (value) {
                case "UPCOMING" -> "c.starts_at > :now";
                case "RUNNING" -> "c.starts_at <= :now and c.ends_at > :now";
                default -> "c.ends_at <= :now";
            });
        }
        if (StringUtils.hasText(objective)) {
            p.addValue("objective", oneOf(objective, OBJECTIVES, "Mục tiêu không hợp lệ"));
            where.append(" and c.objective = :objective");
        }
        if (from != null && to != null && to.isBefore(from)) throw BusinessException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        if (from != null) {
            p.addValue("from", DbTimes.toDb(from.atStartOfDay()));
            where.append(" and c.ends_at > :from");
        }
        if (to != null) {
            p.addValue("to", DbTimes.toDb(to.plusDays(1).atStartOfDay()));
            where.append(" and c.starts_at < :to");
        }
        if (StringUtils.hasText(keyword)) {
            p.addValue("keyword", "%" + keyword.trim() + "%");
            where.append(" and (c.name ilike :keyword or c.code ilike :keyword)");
        }
        Long total = jdbc.queryForObject("select count(*) from marketing_campaigns c" + where, p, Long.class);
        p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<Campaign> rows = jdbc.query(SELECT + FROM + where + " order by c.created_at desc, c.id limit :limit offset :offset", p,
                (rs, i) -> {
                    UUID store = rs.getObject("store_id", UUID.class);
                    return mapCampaign(rs, global || (manageOwn && ownStore.equals(store)), now);
                });
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    public MarketingCampaignDtos.Detail detail(UUID actor, UUID id) {
        LocalDateTime now = now();
        Campaign raw = campaign(id, true, now);
        // Nhân viên cửa hàng không được biết chiến dịch họ không được xem tồn tại.
        if (!access.canSee(actor, raw.storeId(), raw.status())) throw BusinessException.notFound("Chiến dịch không tồn tại");
        boolean financial = access.canViewFinancials(actor, raw.storeId());
        Campaign campaign = financial ? raw : campaign(id, false, now);
        List<Offer> offers = offers(campaign, now);
        List<MarketingCampaignDtos.Dispatch> sends = financial ? dispatches.forCampaign(actor, campaign, now) : List.of();
        List<Expense> expenses = financial ? expenses(id) : List.of();
        List<MarketingCampaignDtos.History> history = financial ? history(id) : List.of();
        return new MarketingCampaignDtos.Detail(campaign, offers, sends, expenses, history, actions(actor, campaign, now),
                warnings(campaign, offers, sends, financial));
    }

    /** Chiến dịch kèm số liệu tài chính, cho báo cáo (người gọi đã kiểm tra quyền). */
    Campaign campaignForReport(UUID id) {
        return campaign(id, true, now());
    }

    /** Ưu đãi đã liên kết của chiến dịch, cho báo cáo và lịch. */
    List<Offer> linkedOffers(Campaign campaign) {
        return offers(campaign, now());
    }

    /** Ưu đãi chọn được để liên kết, kèm chiến dịch đang giữ và lý do không chọn được. */
    public Page<Candidate> candidates(UUID actor, String type, String keyword, Pageable pageable) {
        access.requireLinkOffers(actor, access.ownStore(actor));
        String offerType = oneOf(type, OFFER_LABELS.keySet(), "Loại ưu đãi không hợp lệ");
        MapSqlParameterSource p = new MapSqlParameterSource("keyword", StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null);
        String sql = switch (offerType) {
            case PROMOTION -> """
                    select p.id as item_id, p.code, p.name, p.promotion_type, p.discount_type, p.discount_value, p.max_discount,
                           p.start_date as valid_from, p.end_date as valid_to, p.status as item_status, null::int as valid_days,
                           null::boolean as active, mc.id as linked_id, mc.code as linked_code, mc.name as linked_name,
                           p.created_at as sort_at
                    from promotions p
                    left join marketing_campaign_offers mo on mo.promotion_id = p.id
                    left join marketing_campaigns mc on mc.id = mo.campaign_id
                    where p.promotion_type in ('ORDER', 'PRODUCT')
                      and (cast(:keyword as varchar) is null or p.code ilike :keyword or p.name ilike :keyword)
                    """;
            case VOUCHER_CAMPAIGN -> """
                    select vc.id as item_id, vc.code, vc.name, p.promotion_type, p.discount_type, p.discount_value, p.max_discount,
                           coalesce(vc.sent_at, vc.scheduled_at) as valid_from, null::timestamp as valid_to,
                           vc.status as item_status, vc.valid_days, null::boolean as active, mc.id as linked_id,
                           mc.code as linked_code, mc.name as linked_name, vc.created_at as sort_at
                    from voucher_campaigns vc
                    join promotions p on p.id = vc.promotion_id
                    left join marketing_campaign_offers mo on mo.voucher_campaign_id = vc.id
                    left join marketing_campaigns mc on mc.id = mo.campaign_id
                    where (cast(:keyword as varchar) is null or vc.code ilike :keyword or vc.name ilike :keyword)
                    """;
            default -> """
                    select fs.id as item_id, null as code, fs.name, null as promotion_type, null as discount_type,
                           null::numeric as discount_value, null::numeric as max_discount, fs.starts_at as valid_from,
                           fs.ends_at as valid_to, null as item_status, null::int as valid_days, fs.active,
                           mc.id as linked_id, mc.code as linked_code, mc.name as linked_name, fs.created_at as sort_at,
                           (select count(*) from flash_sale_items i where i.flash_sale_id = fs.id) as item_count
                    from flash_sales fs
                    left join marketing_campaign_offers mo on mo.flash_sale_id = fs.id
                    left join marketing_campaigns mc on mc.id = mo.campaign_id
                    where (cast(:keyword as varchar) is null or fs.name ilike :keyword)
                    """;
        };
        Long total = jdbc.queryForObject("select count(*) from (" + sql + ") x", p, Long.class);
        p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        LocalDateTime now = now();
        List<Candidate> rows = jdbc.query("select * from (" + sql + ") x order by (x.linked_id is null) desc, x.sort_at desc"
                + " limit :limit offset :offset", p, (rs, i) -> {
            OfferItem item = offerItem(offerType, rs, now);
            UUID linked = rs.getObject("linked_id", UUID.class);
            String reason = linked != null ? "Đã thuộc chiến dịch " + rs.getString("linked_code") : item.blockedReason();
            return new Candidate(offerType, item.itemId(), item.code(), item.name(), item.detail(), item.validFrom(), item.validTo(),
                    item.status(), linked, rs.getString("linked_code"), rs.getString("linked_name"), reason);
        });
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    // ------------------------------------------------------------------ tạo và sửa

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail create(UUID actor, MarketingCampaignDtos.CampaignRequest request) {
        access.requireCreate(actor);
        Fields f = validate(request, actor, storeFor(actor, request.storeId()));
        UUID id = jdbc.queryForObject("""
                insert into marketing_campaigns(code, name, objective, description, starts_at, ends_at, owner_id, status,
                                                planned_budget, target_revenue, target_orders, target_buyers, store_id, created_by)
                values (:code, :name, :objective, :description, :startsAt, :endsAt, :owner, 'DRAFT',
                        :budget, :revenue, :orders, :buyers, cast(:store as uuid), :actor)
                returning id""", f.params().addValue("code", nextCode()).addValue("actor", actor), UUID.class);
        history(id, "CREATED", null, DRAFT, null, actor);
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail update(UUID actor, UUID id, MarketingCampaignDtos.CampaignRequest request) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        requireEditable(c, "Chỉ sửa được chiến dịch đang Nháp hoặc Bị từ chối");
        Fields f = validate(request, actor, storeFor(actor, request.storeId()));
        jdbc.update("""
                update marketing_campaigns set name = :name, objective = :objective, description = :description,
                       starts_at = :startsAt, ends_at = :endsAt, owner_id = :owner, planned_budget = :budget,
                       target_revenue = :revenue, target_orders = :orders, target_buyers = :buyers,
                       store_id = cast(:store as uuid), updated_at = (now() at time zone 'UTC')
                where id = :id""", f.params().addValue("id", id));
        history(id, "UPDATED", c.status(), c.status(), null, actor);
        return detail(actor, id);
    }

    // ------------------------------------------------------------------ quy trình duyệt

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail submit(UUID actor, UUID id) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        requireEditable(c, "Chỉ gửi duyệt được chiến dịch đang Nháp hoặc Bị từ chối");
        if (!c.endsAt().isAfter(now()))
            throw BusinessException.conflict("Chiến dịch đã qua thời gian kết thúc, hãy sửa lại thời gian trước khi gửi duyệt");
        transition(id, c.status(), PENDING, """
                submitted_by = :actor, submitted_at = (now() at time zone 'UTC'),
                decided_by = null, decided_at = null, decision_note = null""", actor, null);
        history(id, "SUBMITTED", c.status(), PENDING, null, actor);
        notifier.send(access.approvers(actor), actor, MarketingCampaignNotifier.SUBMITTED, "Chiến dịch chờ duyệt: " + c.name(),
                name(actor) + " đã gửi duyệt chiến dịch " + c.code() + ".", id, c.code());
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail withdraw(UUID actor, UUID id) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        requireStatus(c, PENDING, "Chỉ rút lại được chiến dịch đang chờ duyệt");
        transition(id, PENDING, DRAFT, null, actor, null);
        history(id, "WITHDRAWN", PENDING, DRAFT, null, actor);
        return detail(actor, id);
    }

    /** Không tự duyệt chiến dịch mình tạo hoặc gửi duyệt; SUPER_ADMIN được phép và việc tự duyệt được ghi vào lịch sử. */
    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail approve(UUID actor, UUID id, String note) {
        access.requireApprove(actor);
        Locked c = lock(id);
        requireStatus(c, PENDING, "Chỉ duyệt được chiến dịch đang chờ duyệt");
        if (!c.endsAt().isAfter(now()))
            throw BusinessException.conflict("Chiến dịch đã qua thời gian kết thúc nên không duyệt được; hãy từ chối để người tạo sửa lại thời gian");
        boolean self = actor.equals(c.createdBy()) || actor.equals(c.submittedBy());
        if (self && !access.isSuperAdmin(actor))
            throw BusinessException.forbidden("Bạn không thể tự duyệt chiến dịch do mình tạo hoặc gửi duyệt");
        String text = trim(note);
        // Đợt gửi Nháp được duyệt cùng chiến dịch; giờ gửi không hợp lệ thì chặn duyệt.
        dispatches.onCampaignApproved(id, actor, c.startsAt(), c.endsAt(), now());
        transition(id, PENDING, APPROVED, "decided_by = :actor, decided_at = (now() at time zone 'UTC'), decision_note = :note",
                actor, text);
        String historyNote = self ? join("SUPER_ADMIN tự duyệt chiến dịch do mình tạo / gửi duyệt.", text) : text;
        history(id, "APPROVED", PENDING, APPROVED, historyNote, actor);
        notifier.send(people(c), actor, MarketingCampaignNotifier.APPROVED, "Chiến dịch đã được duyệt: " + c.name(),
                text != null ? "Ghi chú: " + text : name(actor) + " đã duyệt chiến dịch " + c.code() + ".", id, c.code());
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail reject(UUID actor, UUID id, String reason) {
        access.requireApprove(actor);
        Locked c = lock(id);
        requireStatus(c, PENDING, "Chỉ từ chối được chiến dịch đang chờ duyệt");
        String text = reason(reason);
        transition(id, PENDING, REJECTED, "decided_by = :actor, decided_at = (now() at time zone 'UTC'), decision_note = :note",
                actor, text);
        history(id, "REJECTED", PENDING, REJECTED, text, actor);
        notifier.send(people(c), actor, MarketingCampaignNotifier.REJECTED, "Chiến dịch bị từ chối: " + c.name(),
                "Lý do: " + text, id, c.code());
        return detail(actor, id);
    }

    /** Đưa chiến dịch đã duyệt về Nháp để sửa; chỉ khi chưa tới thời gian bắt đầu. */
    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail reopen(UUID actor, UUID id, String reason) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        requireStatus(c, APPROVED, "Chỉ mở lại được chiến dịch đã duyệt");
        if (!now().isBefore(c.startsAt())) throw BusinessException.conflict("Chỉ mở lại được trước khi chiến dịch bắt đầu");
        String text = reason(reason);
        dispatches.onCampaignReopened(id);
        transition(id, APPROVED, DRAFT, null, actor, null);
        history(id, "REOPENED", APPROVED, DRAFT, text, actor);
        return detail(actor, id);
    }

    /**
     * Hủy chiến dịch. Promotion, Voucher Campaign và Flash Sale đã liên kết KHÔNG bị tắt; chi tiết trả về cảnh báo kèm
     * danh sách ưu đãi còn hiệu lực để người dùng tự xử lý ở màn hình của từng loại.
     */
    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail cancel(UUID actor, UUID id, String reason) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        if (CANCELLED.equals(c.status())) throw BusinessException.conflict("Chiến dịch đã bị hủy");
        String text = reason(reason);
        transition(id, c.status(), CANCELLED,
                "cancelled_by = :actor, cancelled_at = (now() at time zone 'UTC'), cancel_reason = :note", actor, text);
        int stopped = dispatches.onCampaignCancelled(id, actor, text);
        history(id, "CANCELLED", c.status(), CANCELLED, stopped == 0 ? text : text + " (dừng " + stopped + " đợt gửi chưa xong)", actor);
        return detail(actor, id);
    }

    // ------------------------------------------------------------------ ưu đãi liên kết

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail link(UUID actor, UUID id, MarketingCampaignDtos.LinkRequest request) {
        Locked c = lock(id);
        access.requireLinkOffers(actor, c.storeId());
        requireEditable(c, "Chỉ liên kết ưu đãi khi chiến dịch đang Nháp hoặc Bị từ chối");
        String type = oneOf(request.type(), OFFER_LABELS.keySet(), "Loại ưu đãi không hợp lệ");
        String column = offerColumn(type);
        String label = requireLinkable(type, request.itemId());
        List<Map<String, Object>> holder = jdbc.queryForList("""
                select mc.id, mc.code, mc.name from marketing_campaign_offers mo join marketing_campaigns mc on mc.id = mo.campaign_id
                where mo.""" + column + " = :item", new MapSqlParameterSource("item", request.itemId()));
        if (!holder.isEmpty()) {
            Map<String, Object> other = holder.getFirst();
            if (id.equals(other.get("id"))) throw BusinessException.conflict("Ưu đãi đã được liên kết với chiến dịch này");
            throw BusinessException.conflict("Ưu đãi đã thuộc chiến dịch " + other.get("code") + " – " + other.get("name")
                    + "; hãy gỡ liên kết ở chiến dịch đó trước");
        }
        try {
            jdbc.update("insert into marketing_campaign_offers(campaign_id, offer_type, " + column + ", linked_by)"
                    + " values (:campaign, :type, :item, :actor)", new MapSqlParameterSource("campaign", id).addValue("type", type)
                    .addValue("item", request.itemId()).addValue("actor", actor));
        } catch (DuplicateKeyException race) {
            throw BusinessException.conflict("Ưu đãi vừa được liên kết với chiến dịch khác; hãy tải lại danh sách");
        }
        history(id, "OFFER_LINKED", c.status(), c.status(), label, actor);
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail unlink(UUID actor, UUID id, UUID offerId) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        if (!EDITABLE.contains(c.status()) && !CANCELLED.equals(c.status()))
            throw BusinessException.conflict("Chỉ gỡ ưu đãi khi chiến dịch đang Nháp, Bị từ chối hoặc Đã hủy");
        String label = offers(campaign(id, true, now()), now()).stream().filter(o -> o.id().equals(offerId)).findFirst()
                .map(o -> OFFER_LABELS.get(o.type()) + " " + display(o.code(), o.name()))
                .orElseThrow(() -> BusinessException.notFound("Ưu đãi không thuộc chiến dịch này"));
        jdbc.update("delete from marketing_campaign_offers where id = :offer and campaign_id = :campaign",
                new MapSqlParameterSource("offer", offerId).addValue("campaign", id));
        history(id, "OFFER_UNLINKED", c.status(), c.status(), label, actor);
        return detail(actor, id);
    }

    // ------------------------------------------------------------------ chi phí

    /** Nhập được ở mọi trạng thái, kể cả sau khi kết thúc hoặc bị hủy (hóa đơn thường về sau). */
    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail addExpense(UUID actor, UUID id, MarketingCampaignDtos.ExpenseRequest request) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        String category = oneOf(request.category(), CATEGORIES.keySet(), "Loại chi phí không hợp lệ");
        jdbc.update("""
                insert into marketing_campaign_expenses(campaign_id, category, amount, spent_on, vendor, note, created_by)
                values (:campaign, :category, :amount, :spentOn, :vendor, :note, :actor)""",
                expenseParams(request, category).addValue("campaign", id).addValue("actor", actor));
        history(id, "EXPENSE_ADDED", c.status(), c.status(), expenseLabel(category, request.amount()), actor);
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail updateExpense(UUID actor, UUID id, UUID expenseId, MarketingCampaignDtos.ExpenseRequest request) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        String category = oneOf(request.category(), CATEGORIES.keySet(), "Loại chi phí không hợp lệ");
        int updated = jdbc.update("""
                update marketing_campaign_expenses set category = :category, amount = :amount, spent_on = :spentOn,
                       vendor = :vendor, note = :note, updated_at = (now() at time zone 'UTC')
                where id = :expense and campaign_id = :campaign""",
                expenseParams(request, category).addValue("expense", expenseId).addValue("campaign", id));
        if (updated == 0) throw BusinessException.notFound("Khoản chi không thuộc chiến dịch này");
        history(id, "EXPENSE_UPDATED", c.status(), c.status(), expenseLabel(category, request.amount()), actor);
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("MARKETING_CAMPAIGN")
    public MarketingCampaignDtos.Detail deleteExpense(UUID actor, UUID id, UUID expenseId) {
        Locked c = lock(id);
        access.requireManageCampaign(actor, c.storeId());
        List<Map<String, Object>> removed = jdbc.queryForList("""
                delete from marketing_campaign_expenses where id = :expense and campaign_id = :campaign
                returning category, amount""", new MapSqlParameterSource("expense", expenseId).addValue("campaign", id));
        if (removed.isEmpty()) throw BusinessException.notFound("Khoản chi không thuộc chiến dịch này");
        Map<String, Object> row = removed.getFirst();
        history(id, "EXPENSE_DELETED", c.status(), c.status(),
                expenseLabel((String) row.get("category"), (BigDecimal) row.get("amount")), actor);
        return detail(actor, id);
    }

    // ------------------------------------------------------------------ chi tiết

    private Actions actions(UUID actor, Campaign c, LocalDateTime now) {
        boolean manage = access.canManageCampaign(actor, c.storeId());
        boolean approve = access.canApprove(actor);
        boolean financial = access.canViewFinancials(actor, c.storeId());
        boolean editable = EDITABLE.contains(c.status());
        boolean pending = PENDING.equals(c.status());
        boolean self = actor.equals(c.createdBy()) || actor.equals(submittedBy(c.id()));
        boolean mayApprove = approve && pending && c.endsAt().isAfter(now) && (!self || access.isSuperAdmin(actor));
        return new Actions(manage && editable, manage && editable, manage && pending, mayApprove, approve && pending,
                manage && APPROVED.equals(c.status()) && now.isBefore(c.startsAt()), manage && !CANCELLED.equals(c.status()),
                editable && access.canLinkOffers(actor, c.storeId()), manage && (editable || CANCELLED.equals(c.status())), manage,
                financial, financial, manage && (editable || (APPROVED.equals(c.status()) && now.isBefore(c.endsAt()))));
    }

    private List<String> warnings(Campaign c, List<Offer> offers, List<MarketingCampaignDtos.Dispatch> sends, boolean global) {
        List<String> warnings = new ArrayList<>();
        for (MarketingCampaignDtos.Dispatch d : sends) {
            if (d.outsideWindow()) {
                warnings.add("Đợt gửi " + d.code() + " có giờ gửi nằm ngoài khoảng cho phép (từ " + MarketingDispatchService.TEASER_DAYS
                        + " ngày trước khi bắt đầu tới lúc kết thúc chiến dịch); hãy sửa giờ gửi.");
            }
        }
        for (Offer offer : offers) {
            if (offer.outsideWindow()) {
                warnings.add(OFFER_LABELS.get(offer.type()) + " " + display(offer.code(), offer.name())
                        + " có thời gian hiệu lực nằm ngoài thời gian chiến dịch.");
            }
        }
        if (global && c.plannedBudget() != null && c.expenseTotal() != null && c.expenseTotal().compareTo(c.plannedBudget()) > 0) {
            warnings.add("Chi phí thực tế " + money(c.expenseTotal()) + " đã vượt ngân sách dự kiến " + money(c.plannedBudget()) + ".");
        }
        if (CANCELLED.equals(c.status())) {
            List<String> live = offers.stream().filter(Offer::inEffect)
                    .map(o -> OFFER_LABELS.get(o.type()) + " " + display(o.code(), o.name())).toList();
            if (!live.isEmpty()) {
                warnings.add("Chiến dịch đã hủy nhưng " + live.size() + " ưu đãi vẫn còn hiệu lực: " + String.join(", ", live)
                        + ". Module Marketing không tự tắt ưu đãi; hãy tắt ở màn hình Khuyến mãi / Flash Sale nếu cần.");
            }
        }
        return warnings;
    }

    /** Ưu đãi đã liên kết, gộp ba loại, mới liên kết trước. */
    private List<Offer> offers(Campaign c, LocalDateTime now) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", c.id());
        List<Offer> offers = new ArrayList<>();
        offers.addAll(jdbc.query("""
                select mo.id as offer_id, mo.linked_at, coalesce(u.full_name, u.username) as linked_by_name,
                       p.id as item_id, p.code, p.name, p.promotion_type, p.discount_type, p.discount_value, p.max_discount,
                       p.start_date as valid_from, p.end_date as valid_to, p.status as item_status,
                       null::int as valid_days, null::boolean as active
                from marketing_campaign_offers mo join promotions p on p.id = mo.promotion_id
                left join users u on u.id = mo.linked_by where mo.campaign_id = :id""", p,
                (rs, i) -> linkedOffer(PROMOTION, rs, c, now)));
        offers.addAll(jdbc.query("""
                select mo.id as offer_id, mo.linked_at, coalesce(u.full_name, u.username) as linked_by_name,
                       vc.id as item_id, vc.code, vc.name, p.promotion_type, p.discount_type, p.discount_value, p.max_discount,
                       coalesce(vc.sent_at, vc.scheduled_at) as valid_from, null::timestamp as valid_to,
                       vc.status as item_status, vc.valid_days, null::boolean as active
                from marketing_campaign_offers mo join voucher_campaigns vc on vc.id = mo.voucher_campaign_id
                join promotions p on p.id = vc.promotion_id
                left join users u on u.id = mo.linked_by where mo.campaign_id = :id""", p,
                (rs, i) -> linkedOffer(VOUCHER_CAMPAIGN, rs, c, now)));
        offers.addAll(jdbc.query("""
                select mo.id as offer_id, mo.linked_at, coalesce(u.full_name, u.username) as linked_by_name,
                       fs.id as item_id, null as code, fs.name, null as promotion_type, null as discount_type,
                       null::numeric as discount_value, null::numeric as max_discount, fs.starts_at as valid_from,
                       fs.ends_at as valid_to, null as item_status, null::int as valid_days, fs.active,
                       (select count(*) from flash_sale_items i where i.flash_sale_id = fs.id) as item_count
                from marketing_campaign_offers mo join flash_sales fs on fs.id = mo.flash_sale_id
                left join users u on u.id = mo.linked_by where mo.campaign_id = :id""", p,
                (rs, i) -> linkedOffer(FLASH_SALE, rs, c, now)));
        offers.sort(Comparator.comparing(Offer::linkedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return offers;
    }

    private Offer linkedOffer(String type, ResultSet rs, Campaign c, LocalDateTime now) throws SQLException {
        OfferItem item = offerItem(type, rs, now);
        // "Ngoài thời gian chiến dịch" = không giao nhau với [startsAt, endsAt); ưu đãi kéo dài hơn chiến dịch không bị cảnh báo.
        boolean outside = (item.validTo() != null && !item.validTo().isAfter(c.startsAt()))
                || (item.validFrom() != null && !item.validFrom().isBefore(c.endsAt()));
        return new Offer(rs.getObject("offer_id", UUID.class), type, item.itemId(), item.code(), item.name(), item.detail(),
                item.validFrom(), item.validTo(), item.status(), item.inEffect(), outside, rs.getString("linked_by_name"),
                DbTimes.fromDb(rs.getObject("linked_at")));
    }

    /** Thông tin chung của một ưu đãi (dòng có các cột chuẩn hóa của offers / candidates). */
    private record OfferItem(UUID itemId, String code, String name, String detail, LocalDateTime validFrom,
                             LocalDateTime validTo, String status, boolean inEffect, String blockedReason) {}

    private static OfferItem offerItem(String type, ResultSet rs, LocalDateTime now) throws SQLException {
        UUID itemId = rs.getObject("item_id", UUID.class);
        String code = rs.getString("code");
        String name = rs.getString("name");
        LocalDateTime from = DbTimes.fromDb(rs.getObject("valid_from"));
        LocalDateTime to = DbTimes.fromDb(rs.getObject("valid_to"));
        String status = rs.getString("item_status");
        return switch (type) {
            case PROMOTION -> {
                String detail = benefit(rs) + ("PRODUCT".equals(rs.getString("promotion_type")) ? " theo sản phẩm" : " toàn đơn");
                boolean live = ("ACTIVE".equals(status) || "SCHEDULED".equals(status)) && to != null && to.isAfter(now);
                yield new OfferItem(itemId, code, name, detail, from, to, status, live,
                        "INACTIVE".equals(status) ? "Khuyến mãi đã ngừng hoạt động" : null);
            }
            case VOUCHER_CAMPAIGN -> {
                int validDays = rs.getInt("valid_days");
                // Voucher dùng được tới hết ngày thứ validDays kể từ ngày gửi (cùng quy tắc VoucherCampaignService.expiresAt).
                LocalDateTime until = from == null ? null : from.toLocalDate().plusDays(validDays + 1L).atStartOfDay();
                boolean live = !"CANCELLED".equals(status) && until != null && until.isAfter(now);
                yield new OfferItem(itemId, code, name, benefit(rs) + ", dùng trong " + validDays + " ngày", from, until, status,
                        live, "CANCELLED".equals(status) ? "Chiến dịch voucher đã hủy" : null);
            }
            default -> {
                boolean active = rs.getBoolean("active");
                String state = !active ? "INACTIVE" : from != null && now.isBefore(from) ? "SCHEDULED"
                        : to != null && now.isBefore(to) ? "RUNNING" : "ENDED";
                yield new OfferItem(itemId, code, name, rs.getInt("item_count") + " sản phẩm giá flash", from, to, state,
                        active && to != null && to.isAfter(now), active ? null : "Flash sale đã tắt");
            }
        };
    }

    /** Kiểm tra ưu đãi tồn tại và được phép liên kết; trả về nhãn dùng cho lịch sử. */
    private String requireLinkable(String type, UUID itemId) {
        MapSqlParameterSource p = new MapSqlParameterSource("id", itemId);
        switch (type) {
            case PROMOTION -> {
                Map<String, Object> row = single("select code, name, promotion_type, status from promotions where id = :id", p,
                        "Khuyến mãi không tồn tại");
                String promotionType = (String) row.get("promotion_type");
                if (!"ORDER".equals(promotionType) && !"PRODUCT".equals(promotionType)) {
                    throw BusinessException.badRequest("Chỉ liên kết được khuyến mãi toàn đơn hoặc theo sản phẩm. Voucher sinh nhật, đổi điểm"
                            + " là chương trình thường trực; mẫu voucher của chiến dịch voucher thì hãy liên kết chính chiến dịch voucher đó");
                }
                if ("INACTIVE".equals(row.get("status"))) throw BusinessException.badRequest("Khuyến mãi đã ngừng hoạt động");
                return OFFER_LABELS.get(type) + " " + display((String) row.get("code"), (String) row.get("name"));
            }
            case VOUCHER_CAMPAIGN -> {
                Map<String, Object> row = single("select code, name, status from voucher_campaigns where id = :id", p,
                        "Chiến dịch voucher không tồn tại");
                if ("CANCELLED".equals(row.get("status"))) throw BusinessException.badRequest("Chiến dịch voucher đã hủy");
                return OFFER_LABELS.get(type) + " " + display((String) row.get("code"), (String) row.get("name"));
            }
            default -> {
                Map<String, Object> row = single("select name, active from flash_sales where id = :id", p, "Flash sale không tồn tại");
                if (!Boolean.TRUE.equals(row.get("active"))) throw BusinessException.badRequest("Flash sale đã tắt");
                return OFFER_LABELS.get(type) + " " + row.get("name");
            }
        }
    }

    private static String offerColumn(String type) {
        return switch (type) {
            case PROMOTION -> "promotion_id";
            case VOUCHER_CAMPAIGN -> "voucher_campaign_id";
            default -> "flash_sale_id";
        };
    }

    private List<Expense> expenses(UUID id) {
        return jdbc.query("""
                select e.*, coalesce(u.full_name, u.username) as created_by_name from marketing_campaign_expenses e
                left join users u on u.id = e.created_by where e.campaign_id = :id order by e.spent_on desc, e.created_at desc""",
                new MapSqlParameterSource("id", id), (rs, i) -> new Expense(rs.getObject("id", UUID.class), rs.getString("category"),
                        rs.getBigDecimal("amount"), rs.getObject("spent_on", LocalDate.class), rs.getString("vendor"), rs.getString("note"),
                        rs.getString("created_by_name"), DbTimes.fromDb(rs.getObject("created_at")), DbTimes.fromDb(rs.getObject("updated_at"))));
    }

    private List<MarketingCampaignDtos.History> history(UUID id) {
        return jdbc.query("""
                select h.*, coalesce(u.full_name, u.username) as actor_name from marketing_campaign_history h
                left join users u on u.id = h.actor_id where h.campaign_id = :id order by h.created_at desc, h.id""",
                new MapSqlParameterSource("id", id), (rs, i) -> new MarketingCampaignDtos.History(rs.getObject("id", UUID.class),
                        rs.getString("action"), rs.getString("from_status"), rs.getString("to_status"), rs.getString("note"),
                        rs.getString("actor_name"), DbTimes.fromDb(rs.getObject("created_at"))));
    }

    // ------------------------------------------------------------------ hỗ trợ

    /** Các cột cần cho kiểm tra trạng thái, đọc sau khi đã khóa dòng. */
    private record Locked(UUID id, String code, String name, String status, LocalDateTime startsAt, LocalDateTime endsAt,
                          UUID createdBy, UUID submittedBy, UUID ownerId, UUID storeId) {}

    /** Khóa dòng chiến dịch: hai người thao tác cùng lúc thì người sau thấy trạng thái mới nhất. */
    private Locked lock(UUID id) {
        List<Locked> rows = jdbc.query("""
                select id, code, name, status, starts_at, ends_at, created_by, submitted_by, owner_id, store_id
                from marketing_campaigns where id = :id for update""", new MapSqlParameterSource("id", id),
                (rs, i) -> new Locked(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getString("status"),
                        DbTimes.fromDb(rs.getObject("starts_at")), DbTimes.fromDb(rs.getObject("ends_at")),
                        rs.getObject("created_by", UUID.class), rs.getObject("submitted_by", UUID.class), rs.getObject("owner_id", UUID.class),
                        rs.getObject("store_id", UUID.class)));
        if (rows.isEmpty()) throw BusinessException.notFound("Chiến dịch không tồn tại");
        return rows.getFirst();
    }

    /** Chuyển trạng thái có điều kiện (WHERE status = from): nếu trạng thái đã đổi thì báo xung đột thay vì ghi đè. */
    private void transition(UUID id, String from, String to, String extraSet, UUID actor, String note) {
        String sql = "update marketing_campaigns set status = :to, updated_at = (now() at time zone 'UTC')"
                + (extraSet == null ? "" : ", " + extraSet) + " where id = :id and status = :from";
        int updated = jdbc.update(sql, new MapSqlParameterSource("id", id).addValue("from", from).addValue("to", to)
                .addValue("actor", actor).addValue("note", note, Types.VARCHAR));
        if (updated == 0) throw BusinessException.conflict("Chiến dịch vừa được người khác cập nhật; hãy tải lại trang");
    }

    private void history(UUID campaign, String action, String from, String to, String note, UUID actor) {
        historyLog.record(campaign, action, from, to, note, actor);
    }

    private Campaign campaign(UUID id, boolean financial, LocalDateTime now) {
        try {
            return jdbc.queryForObject(SELECT + FROM + " where c.id = :id", new MapSqlParameterSource("id", id),
                    (rs, i) -> mapCampaign(rs, financial, now));
        } catch (EmptyResultDataAccessException missing) {
            throw BusinessException.notFound("Chiến dịch không tồn tại");
        }
    }

    private static Campaign mapCampaign(ResultSet rs, boolean financial, LocalDateTime now) throws SQLException {
        String status = rs.getString("status");
        LocalDateTime starts = DbTimes.fromDb(rs.getObject("starts_at"));
        LocalDateTime ends = DbTimes.fromDb(rs.getObject("ends_at"));
        return new Campaign(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getString("objective"),
                rs.getString("description"), starts, ends, rs.getObject("owner_id", UUID.class), rs.getString("owner_name"), status,
                phase(status, starts, ends, now),
                financial ? rs.getBigDecimal("planned_budget") : null, financial ? rs.getBigDecimal("target_revenue") : null,
                financial ? (Integer) rs.getObject("target_orders") : null, financial ? (Integer) rs.getObject("target_buyers") : null,
                rs.getInt("offer_count"), financial ? rs.getBigDecimal("expense_total") : null,
                rs.getObject("created_by", UUID.class), rs.getString("created_by_name"), DbTimes.fromDb(rs.getObject("submitted_at")),
                rs.getString("submitted_by_name"), DbTimes.fromDb(rs.getObject("decided_at")), rs.getString("decided_by_name"),
                rs.getString("decision_note"), DbTimes.fromDb(rs.getObject("cancelled_at")), rs.getString("cancelled_by_name"),
                rs.getString("cancel_reason"), DbTimes.fromDb(rs.getObject("created_at")), DbTimes.fromDb(rs.getObject("updated_at")),
                rs.getObject("store_id", UUID.class), rs.getString("store_name"));
    }

    /**
     * Phạm vi cửa hàng của chiến dịch theo người thao tác: nhân viên cấp chuỗi chọn toàn chuỗi (null) hoặc một cửa hàng
     * đang hoạt động; nhân viên cửa hàng luôn là cửa hàng của mình.
     */
    private UUID storeFor(UUID actor, UUID requested) {
        UUID own = access.ownStore(actor);
        if (own != null) {
            if (requested != null && !requested.equals(own)) throw BusinessException.forbidden("Bạn chỉ lập được chiến dịch cho cửa hàng của mình");
            return own;
        }
        if (requested != null && !Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from stores where id = :id and active)",
                new MapSqlParameterSource("id", requested), Boolean.class))) {
            throw BusinessException.notFound("Cửa hàng không tồn tại hoặc ngừng hoạt động");
        }
        return requested;
    }

    /** Giai đoạn của chiến dịch đã duyệt, tính từ thời gian; null với các trạng thái khác. */
    static String phase(String status, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime now) {
        if (!APPROVED.equals(status)) return null;
        if (now.isBefore(startsAt)) return "UPCOMING";
        return now.isBefore(endsAt) ? "RUNNING" : "ENDED";
    }

    private record Fields(MapSqlParameterSource params) {}

    private Fields validate(MarketingCampaignDtos.CampaignRequest r, UUID actor, UUID store) {
        String name = r.name().trim();
        if (name.length() < 3) throw BusinessException.badRequest("Tên chiến dịch cần ít nhất 3 ký tự");
        String objective = oneOf(r.objective(), OBJECTIVES, "Mục tiêu không hợp lệ");
        if (!r.endsAt().isAfter(r.startsAt())) throw BusinessException.badRequest("Thời gian kết thúc phải sau thời gian bắt đầu");
        if (Duration.between(r.startsAt(), r.endsAt()).toDays() > MAX_DAYS)
            throw BusinessException.badRequest("Một chiến dịch kéo dài tối đa " + MAX_DAYS + " ngày");
        UUID owner = r.ownerId() != null ? r.ownerId() : actor;
        if (!access.isValidOwner(owner, store)) {
            throw BusinessException.badRequest(store == null ? "Người phụ trách phải là nhân viên cấp chuỗi đang hoạt động"
                    : "Người phụ trách phải là nhân viên cấp chuỗi hoặc nhân viên của cửa hàng này");
        }
        return new Fields(new MapSqlParameterSource("name", name).addValue("objective", objective).addValue("store", store, Types.OTHER)
                .addValue("description", trim(r.description()), Types.VARCHAR)
                .addValue("startsAt", DbTimes.toDb(r.startsAt())).addValue("endsAt", DbTimes.toDb(r.endsAt()))
                .addValue("owner", owner).addValue("budget", r.plannedBudget(), Types.NUMERIC)
                .addValue("revenue", r.targetRevenue(), Types.NUMERIC).addValue("orders", r.targetOrders(), Types.INTEGER)
                .addValue("buyers", r.targetBuyers(), Types.INTEGER));
    }

    private static MapSqlParameterSource expenseParams(MarketingCampaignDtos.ExpenseRequest r, String category) {
        return new MapSqlParameterSource("category", category).addValue("amount", r.amount().setScale(2, RoundingMode.HALF_UP))
                .addValue("spentOn", r.spentOn()).addValue("vendor", trim(r.vendor()), Types.VARCHAR)
                .addValue("note", trim(r.note()), Types.VARCHAR);
    }

    private static void requireEditable(Locked c, String message) {
        if (!EDITABLE.contains(c.status())) throw BusinessException.conflict(message);
    }

    private static void requireStatus(Locked c, String status, String message) {
        if (!status.equals(c.status())) throw BusinessException.conflict(message);
    }

    /** Người tạo, người gửi duyệt và người phụ trách: những người cần biết kết quả duyệt. */
    private static Set<UUID> people(Locked c) {
        Set<UUID> to = new HashSet<>();
        if (c.createdBy() != null) to.add(c.createdBy());
        if (c.submittedBy() != null) to.add(c.submittedBy());
        if (c.ownerId() != null) to.add(c.ownerId());
        return to;
    }

    private UUID submittedBy(UUID id) {
        return jdbc.queryForObject("select submitted_by from marketing_campaigns where id = :id", new MapSqlParameterSource("id", id), UUID.class);
    }

    private Map<String, Object> single(String sql, MapSqlParameterSource p, String notFound) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) throw BusinessException.notFound(notFound);
        return rows.getFirst();
    }

    private static String oneOf(String value, Set<String> allowed, String message) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw BusinessException.badRequest(message);
        return normalized;
    }

    private static String reason(String value) {
        String text = trim(value);
        if (text == null || text.length() < 5) throw BusinessException.badRequest("Lý do cần ít nhất 5 ký tự");
        if (text.length() > 500) throw BusinessException.badRequest("Lý do tối đa 500 ký tự");
        return text;
    }

    private String nextCode() {
        Long value = jdbc.getJdbcTemplate().queryForObject("select nextval('marketing_campaign_code_seq')", Long.class);
        return "MC-%s-%04d".formatted(LocalDate.now(ZONE).format(CODE_DATE), value);
    }

    private String name(UUID user) {
        List<String> names = jdbc.queryForList("select coalesce(full_name, username) from users where id = :id",
                new MapSqlParameterSource("id", user), String.class);
        return names.isEmpty() ? "Hệ thống" : names.getFirst();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now();
    }

    private static String benefit(ResultSet rs) throws SQLException {
        BigDecimal value = rs.getBigDecimal("discount_value");
        if (value == null) return "Ưu đãi";
        BigDecimal max = rs.getBigDecimal("max_discount");
        return "PERCENT".equals(rs.getString("discount_type"))
                ? "Giảm " + value.stripTrailingZeros().toPlainString() + "%" + (max != null ? " (tối đa " + money(max) + ")" : "")
                : "Giảm " + money(value);
    }

    static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount.setScale(0, RoundingMode.HALF_UP)) + "đ";
    }

    private static String expenseLabel(String category, BigDecimal amount) {
        return CATEGORIES.getOrDefault(category, category) + " · " + money(amount);
    }

    private static String display(String code, String name) {
        return code == null ? name : code + " – " + name;
    }

    private static String join(String first, String second) {
        return second == null ? first : first + " " + second;
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
