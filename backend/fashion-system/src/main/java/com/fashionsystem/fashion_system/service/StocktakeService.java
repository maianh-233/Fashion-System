package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.dto.StocktakeDtos;
import com.fashionsystem.fashion_system.dto.StocktakeDtos.Adjustment;
import com.fashionsystem.fashion_system.dto.StocktakeDtos.Item;
import com.fashionsystem.fashion_system.dto.StocktakeDtos.Stocktake;
import com.fashionsystem.fashion_system.dto.StocktakeDtos.Summary;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.PermissionRecipientResolver;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Kiểm kê kho theo cửa hàng (toàn bộ hoặc một danh mục kể cả danh mục con). Tạo phiếu chụp lại tồn hệ thống
 * (tại quầy + online + đang giữ cho đơn) và giá vốn → nhân viên quét mã vạch / SKU hoặc nhập số đếm → gửi duyệt →
 * quản lý duyệt thì sinh phiếu điều chỉnh: chênh lệch (số đếm − số chụp) được cộng / trừ vào tồn hiện tại, nên hàng
 * bán ra trong lúc đếm không bị tính hai lần. Phiếu đếm mù ẩn số hệ thống với người chỉ có quyền đếm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StocktakeService {
    static final String VIEW = "STOCKTAKE_VIEW";
    static final String COUNT = "STOCKTAKE_COUNT";
    static final String APPROVE = "STOCKTAKE_APPROVE";
    static final String IN_PROGRESS = "IN_PROGRESS";
    static final String SUBMITTED = "SUBMITTED";
    static final String APPROVED = "APPROVED";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Set<String> ITEM_FILTERS = Set.of("ALL", "UNCOUNTED", "COUNTED", "VARIANCE", "ADDED");
    private static final int MAX_ITEMS = 20000;

    private final NamedParameterJdbcTemplate jdbc;
    private final StoreAccessService storeAccess;
    private final UserScopeService scopes;
    private final InventoryService inventory;
    private final NotificationPublisher notifications;
    private final PermissionRecipientResolver recipients;

    // ------------------------------------------------------------------ danh mục

    public StocktakeDtos.Lookups lookups(UUID actor) {
        List<StocktakeDtos.Ref> stores = storeAccess.accessibleStores(actor, List.of(VIEW), "Bạn không có quyền xem kiểm kê")
                .stream().map(s -> new StocktakeDtos.Ref(s.id(), s.name())).toList();
        List<StocktakeDtos.CategoryRef> categories = jdbc.query("""
                with recursive tree as (
                    select id, name, parent_id, 0 as depth, lower(name)::text as path from categories where parent_id is null and active
                    union all
                    select c.id, c.name, c.parent_id, t.depth + 1, t.path || '/' || lower(c.name)
                    from categories c join tree t on c.parent_id = t.id where c.active and t.depth < 10
                )
                select id, name, parent_id, depth from tree order by path
                """, new MapSqlParameterSource(), (rs, i) -> new StocktakeDtos.CategoryRef(rs.getObject("id", UUID.class),
                rs.getString("name"), rs.getObject("parent_id", UUID.class), rs.getInt("depth")));
        UserScope scope = scopes.resolve(actor);
        UUID store = scope.isGlobal() ? null : scope.storeId();
        return new StocktakeDtos.Lookups(stores, categories, storeAccess.allowsScope(actor, store, COUNT),
                storeAccess.allowsScope(actor, store, APPROVE), scope.isGlobal(), store);
    }

    // ------------------------------------------------------------------ phiếu

    @Transactional
    @BusinessAudit("STOCKTAKE")
    public Stocktake create(UUID actor, StocktakeDtos.CreateRequest request) {
        UUID store = storeAccess.require(actor, request.storeId(), COUNT);
        if (request.categoryId() != null) {
            Boolean exists = jdbc.queryForObject("select exists (select 1 from categories where id = :id)",
                    new MapSqlParameterSource("id", request.categoryId()), Boolean.class);
            if (!Boolean.TRUE.equals(exists)) throw BusinessException.notFound("Danh mục không tồn tại");
        }
        List<String> open = jdbc.queryForList("select code from stocktakes where store_id = :s and status in ('IN_PROGRESS', 'SUBMITTED')",
                new MapSqlParameterSource("s", store), String.class);
        if (!open.isEmpty()) throw BusinessException.conflict("Cửa hàng đang có phiếu kiểm kê " + open.getFirst() + " chưa hoàn tất");

        UUID id = jdbc.queryForObject("""
                insert into stocktakes(code, store_id, scope_type, category_id, blind_count, status, note, created_by, created_at)
                values (:code, :store, :scope, cast(:cat as uuid), :blind, 'IN_PROGRESS', :note, :actor, now() at time zone 'UTC')
                returning id
                """, new MapSqlParameterSource("code", nextCode("KK", "stocktake_code_seq")).addValue("store", store)
                .addValue("scope", request.categoryId() == null ? "STORE" : "CATEGORY")
                .addValue("cat", request.categoryId(), java.sql.Types.OTHER).addValue("blind", request.blindCount())
                .addValue("note", trim(request.note()), java.sql.Types.VARCHAR).addValue("actor", actor), UUID.class);
        int items = jdbc.update(SNAPSHOT + """
                 select :st, b.product_variant_id, b.available_quantity + b.online_quantity + b.reserved_quantity,
                        coalesce(cost.cost_price, coalesce(v.sale_price, v.price), 0),
                        case when cost.cost_price is not null then 'RECEIPT' when coalesce(v.sale_price, v.price) is not null then 'PRICE'
                             else 'NONE' end, false
                 from inventory_balances b
                 join product_variants v on v.id = b.product_variant_id
                 join products p on p.id = v.product_id
                 left join cost on cost.product_variant_id = b.product_variant_id
                 where b.store_id = :store
                   and (v.active or b.available_quantity + b.online_quantity + b.reserved_quantity > 0)
                   and (cast(:cat as uuid) is null or p.category_id in (select id from cat))
                """, new MapSqlParameterSource("st", id).addValue("store", store)
                .addValue("cat", request.categoryId(), java.sql.Types.OTHER));
        if (items > MAX_ITEMS) throw BusinessException.badRequest("Phạm vi quá lớn (" + items + " mã hàng), hãy kiểm kê theo danh mục");
        return view(actor, id);
    }

    public Page<Stocktake> list(UUID actor, UUID storeId, String status, Pageable pageable) {
        UUID store = storeAccess.requireOrAll(actor, storeId, VIEW);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("store", store, java.sql.Types.OTHER)
                .addValue("status", StringUtils.hasText(status) ? status.trim().toUpperCase() : null, java.sql.Types.VARCHAR);
        String where = " from stocktakes s where (cast(:store as uuid) is null or s.store_id = :store)"
                + " and (cast(:status as varchar) is null or s.status = :status)";
        Long total = jdbc.queryForObject("select count(*)" + where, p, Long.class);
        List<UUID> ids = jdbc.queryForList("select s.id" + where
                        + " order by (s.status in ('IN_PROGRESS', 'SUBMITTED')) desc, s.created_at desc limit :limit offset :offset",
                p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()), UUID.class);
        return new PageImpl<>(ids.stream().map(id -> view(actor, id)).toList(), pageable, total == null ? 0 : total);
    }

    public Stocktake detail(UUID actor, UUID id) {
        Header h = header(id);
        if (!storeAccess.allows(actor, h.storeId(), VIEW)) throw BusinessException.forbidden("Bạn không có quyền xem phiếu kiểm kê này");
        return view(actor, id);
    }

    public Page<Item> items(UUID actor, UUID id, String filter, String keyword, Pageable pageable) {
        Header h = header(id);
        if (!storeAccess.allows(actor, h.storeId(), VIEW)) throw BusinessException.forbidden("Bạn không có quyền xem phiếu kiểm kê này");
        boolean visible = systemVisible(actor, h);
        String f = StringUtils.hasText(filter) ? filter.trim().toUpperCase() : "ALL";
        if (!ITEM_FILTERS.contains(f)) throw BusinessException.badRequest("Bộ lọc không hợp lệ");
        if ("VARIANCE".equals(f) && !visible) f = "COUNTED";
        MapSqlParameterSource p = new MapSqlParameterSource("id", id).addValue("z", h.zeroUncounted())
                .addValue("kw", StringUtils.hasText(keyword) ? "%" + keyword.trim().toLowerCase() + "%" : null, java.sql.Types.VARCHAR);
        String where = " where i.stocktake_id = :id" + switch (f) {
            case "UNCOUNTED" -> " and i.counted_quantity is null";
            case "COUNTED" -> " and i.counted_quantity is not null";
            case "VARIANCE" -> " and " + EFFECTIVE + " is not null and " + EFFECTIVE + " <> i.system_quantity";
            case "ADDED" -> " and i.added_during_count";
            default -> "";
        } + " and (cast(:kw as varchar) is null or lower(v.sku) like :kw or lower(coalesce(v.barcode, '')) like :kw"
                + " or lower(p.name) like :kw)";
        Long total = jdbc.queryForObject("select count(*) from stocktake_items i join product_variants v on v.id = i.product_variant_id"
                + " join products p on p.id = v.product_id" + where, p, Long.class);
        List<Item> rows = jdbc.query(ITEM_SELECT + where + " order by (i.counted_at is null), i.counted_at desc, p.name, v.sku"
                        + " limit :limit offset :offset",
                p.addValue("limit", Math.min(pageable.getPageSize(), 200)).addValue("offset", pageable.getOffset()),
                (rs, i) -> mapItem(rs, visible, h.zeroUncounted()));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    // ------------------------------------------------------------------ đếm

    /** Quét mã vạch hoặc SKU: cộng thêm {@code quantity} (mặc định 1). Mã hàng ngoài danh sách nhưng trong phạm vi được thêm. */
    @Transactional
    @BusinessAudit("STOCKTAKE")
    public StocktakeDtos.ScanResult scan(UUID actor, UUID id, StocktakeDtos.ScanRequest request) {
        Header h = header(id);
        storeAccess.require(actor, h.storeId(), COUNT);
        requireStatus(h, IN_PROGRESS, "Phiếu không còn ở bước đếm");
        String code = request.code().trim();
        int qty = request.quantity() == null ? 1 : request.quantity();
        List<UUID> variants = jdbc.queryForList("""
                select id from product_variants where lower(barcode) = lower(:c) or lower(sku) = lower(:c)
                order by coalesce(lower(barcode) = lower(:c), false) desc, active desc limit 1
                """, new MapSqlParameterSource("c", code), UUID.class);
        if (variants.isEmpty()) throw BusinessException.notFound("Không tìm thấy sản phẩm có mã " + code);
        UUID variant = variants.getFirst();
        boolean added = false;
        UUID itemId = itemId(id, variant);
        if (itemId == null) {
            Boolean inScope = jdbc.queryForObject("""
                    with recursive cat as (select id from categories where id = cast(:cat as uuid)
                                           union all select c.id from categories c join cat on c.parent_id = cat.id)
                    select cast(:cat as uuid) is null or exists (select 1 from product_variants v join products p on p.id = v.product_id
                                                                 where v.id = :v and p.category_id in (select id from cat))
                    """, new MapSqlParameterSource().addValue("cat", h.categoryId(), java.sql.Types.OTHER).addValue("v", variant), Boolean.class);
            if (!Boolean.TRUE.equals(inScope)) throw BusinessException.badRequest("Sản phẩm mã " + code + " không thuộc danh mục đang kiểm kê");
            jdbc.update(SNAPSHOT + """
                     select :st, v.id, coalesce(b.available_quantity + b.online_quantity + b.reserved_quantity, 0),
                            coalesce(cost.cost_price, coalesce(v.sale_price, v.price), 0),
                            case when cost.cost_price is not null then 'RECEIPT' when coalesce(v.sale_price, v.price) is not null then 'PRICE'
                                 else 'NONE' end, true
                     from product_variants v
                     left join inventory_balances b on b.product_variant_id = v.id and b.store_id = :store
                     left join cost on cost.product_variant_id = v.id
                     where v.id = :v
                     on conflict (stocktake_id, product_variant_id) do nothing
                    """, new MapSqlParameterSource("st", id).addValue("store", h.storeId()).addValue("v", variant)
                    .addValue("cat", null, java.sql.Types.OTHER));
            itemId = itemId(id, variant);
            added = true;
        }
        Integer after = jdbc.queryForObject("""
                update stocktake_items set counted_quantity = coalesce(counted_quantity, 0) + :q, counted_by = :actor,
                       counted_at = now() at time zone 'UTC'
                where id = :item returning counted_quantity
                """, new MapSqlParameterSource("q", qty).addValue("actor", actor).addValue("item", itemId), Integer.class);
        logCount(itemId, "SCAN", qty, after, code, actor);
        Item item = item(actor, h, itemId);
        String name = item.productName() + " " + variantText(item);
        return new StocktakeDtos.ScanResult(item, added, (added ? "Đã thêm " : "+" + qty + " ") + name + " → đã đếm " + after);
    }

    /** Nhập / sửa số đếm của một dòng. */
    @Transactional
    @BusinessAudit("STOCKTAKE")
    public Item setCount(UUID actor, UUID id, UUID itemId, StocktakeDtos.CountRequest request) {
        Header h = header(id);
        storeAccess.require(actor, h.storeId(), COUNT);
        requireStatus(h, IN_PROGRESS, "Phiếu không còn ở bước đếm");
        int updated = jdbc.update("""
                update stocktake_items set counted_quantity = :q, note = :note, counted_by = :actor, counted_at = now() at time zone 'UTC'
                where id = :item and stocktake_id = :id
                """, new MapSqlParameterSource("q", request.countedQuantity()).addValue("note", trim(request.note()), java.sql.Types.VARCHAR)
                .addValue("actor", actor).addValue("item", itemId).addValue("id", id));
        if (updated == 0) throw BusinessException.notFound("Dòng kiểm kê không tồn tại");
        logCount(itemId, "SET", request.countedQuantity(), request.countedQuantity(), null, actor);
        return item(actor, h, itemId);
    }

    @Transactional
    @BusinessAudit("STOCKTAKE")
    public Stocktake submit(UUID actor, UUID id, StocktakeDtos.SubmitRequest request) {
        Header h = lockHeader(id);
        storeAccess.require(actor, h.storeId(), COUNT);
        requireStatus(h, IN_PROGRESS, "Chỉ gửi duyệt được phiếu đang đếm");
        Integer counted = jdbc.queryForObject("select count(*) from stocktake_items where stocktake_id = :id and counted_quantity is not null",
                new MapSqlParameterSource("id", id), Integer.class);
        if (counted == null || counted == 0) throw BusinessException.badRequest("Chưa đếm mã hàng nào");
        jdbc.update("""
                update stocktakes set status = 'SUBMITTED', zero_uncounted = :z, submitted_by = :actor,
                       submitted_at = now() at time zone 'UTC', rejection_reason = null, updated_at = now() at time zone 'UTC'
                where id = :id
                """, new MapSqlParameterSource("id", id).addValue("z", request != null && request.zeroUncounted()).addValue("actor", actor));
        afterCommit(() -> notifyStore(h.storeId(), APPROVE, actor, "Kiểm kê chờ duyệt · " + h.code(),
                "Phiếu kiểm kê " + h.code() + " đã đếm xong và chờ duyệt điều chỉnh tồn.", id));
        return view(actor, id);
    }

    /** Trả phiếu về bước đếm (đếm lại các dòng lệch). */
    @Transactional
    @BusinessAudit("STOCKTAKE")
    public Stocktake reject(UUID actor, UUID id, StocktakeDtos.Rejection rejection) {
        Header h = lockHeader(id);
        storeAccess.require(actor, h.storeId(), APPROVE);
        requireStatus(h, SUBMITTED, "Chỉ trả về được phiếu đang chờ duyệt");
        jdbc.update("""
                update stocktakes set status = 'IN_PROGRESS', zero_uncounted = false, rejection_reason = :reason,
                       updated_at = now() at time zone 'UTC' where id = :id
                """, new MapSqlParameterSource("id", id).addValue("reason", rejection.reason().trim()));
        afterCommit(() -> notifyStore(h.storeId(), COUNT, actor, "Kiểm kê cần đếm lại · " + h.code(),
                "Lý do: " + rejection.reason().trim(), id));
        return view(actor, id);
    }

    @Transactional
    @BusinessAudit("STOCKTAKE")
    public Stocktake cancel(UUID actor, UUID id) {
        Header h = lockHeader(id);
        boolean approver = storeAccess.allows(actor, h.storeId(), APPROVE);
        boolean creatorCounting = IN_PROGRESS.equals(h.status()) && actor.equals(h.createdBy()) && storeAccess.allows(actor, h.storeId(), COUNT);
        if (!approver && !creatorCounting) throw BusinessException.forbidden("Bạn không có quyền hủy phiếu kiểm kê này");
        if (!IN_PROGRESS.equals(h.status()) && !SUBMITTED.equals(h.status()))
            throw BusinessException.invalidState("Phiếu đã hoàn tất, không hủy được");
        jdbc.update("update stocktakes set status = 'CANCELLED', decided_by = :actor, decided_at = now() at time zone 'UTC',"
                + " updated_at = now() at time zone 'UTC' where id = :id", new MapSqlParameterSource("id", id).addValue("actor", actor));
        return view(actor, id);
    }

    /**
     * Duyệt: sinh phiếu điều chỉnh, cộng / trừ chênh lệch của từng dòng vào tồn hiện tại (dòng chưa đếm bỏ qua, trừ
     * khi người gửi chọn tính là 0). Lưu số thực điều chỉnh cho từng dòng.
     */
    @Transactional
    @BusinessAudit("STOCKTAKE")
    public Stocktake approve(UUID actor, UUID id, StocktakeDtos.Decision decision) {
        Header h = lockHeader(id);
        storeAccess.require(actor, h.storeId(), APPROVE);
        requireStatus(h, SUBMITTED, "Chỉ duyệt được phiếu đang chờ duyệt");
        UUID adjustment = jdbc.queryForObject("""
                insert into stock_adjustments(code, stocktake_id, store_id, approved_by, created_at)
                values (:code, :st, :store, :actor, now() at time zone 'UTC') returning id
                """, new MapSqlParameterSource("code", nextCode("DCK", "stock_adjustment_code_seq")).addValue("st", id)
                .addValue("store", h.storeId()).addValue("actor", actor), UUID.class);
        record Line(UUID item, UUID variant, int delta, BigDecimal cost) {
        }
        List<Line> lines = jdbc.query("select i.id, i.product_variant_id, " + EFFECTIVE + " - i.system_quantity as delta, i.unit_cost"
                        + " from stocktake_items i where i.stocktake_id = :id and " + EFFECTIVE + " is not null"
                        + " and " + EFFECTIVE + " <> i.system_quantity order by i.product_variant_id",
                new MapSqlParameterSource("id", id).addValue("z", h.zeroUncounted()),
                (rs, i) -> new Line(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getInt(3), rs.getBigDecimal(4)));
        int inc = 0, dec = 0;
        BigDecimal incValue = BigDecimal.ZERO, decValue = BigDecimal.ZERO;
        for (Line line : lines) {
            int applied = inventory.applyStocktake(h.storeId(), line.variant(), line.delta(), adjustment, actor);
            jdbc.update("update stocktake_items set adjusted_quantity = :a where id = :id",
                    new MapSqlParameterSource("a", applied).addValue("id", line.item()));
            BigDecimal value = line.cost().multiply(BigDecimal.valueOf(Math.abs(applied)));
            if (applied > 0) { inc += applied; incValue = incValue.add(value); }
            if (applied < 0) { dec -= applied; decValue = decValue.add(value); }
        }
        jdbc.update("""
                update stock_adjustments set increase_quantity = :inc, decrease_quantity = :dec, increase_value = :incV,
                       decrease_value = :decV, line_count = :lines where id = :id
                """, new MapSqlParameterSource("inc", inc).addValue("dec", dec).addValue("incV", incValue).addValue("decV", decValue)
                .addValue("lines", lines.size()).addValue("id", adjustment));
        jdbc.update("""
                update stocktakes set status = 'APPROVED', decided_by = :actor, decided_at = now() at time zone 'UTC',
                       approver_note = :note, updated_at = now() at time zone 'UTC' where id = :id
                """, new MapSqlParameterSource("id", id).addValue("actor", actor)
                .addValue("note", decision == null ? null : trim(decision.note()), java.sql.Types.VARCHAR));
        int increased = inc, decreased = dec;
        afterCommit(() -> notifyStore(h.storeId(), COUNT, actor, "Kiểm kê đã duyệt · " + h.code(),
                "Đã điều chỉnh tồn: +" + increased + " / −" + decreased + " sản phẩm.", id));
        return view(actor, id);
    }

    // ------------------------------------------------------------------ báo cáo hao hụt

    public StocktakeDtos.Report report(UUID actor, UUID storeId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) throw BusinessException.badRequest("Khoảng ngày không hợp lệ");
        if (ChronoUnit.DAYS.between(from, to) > 731) throw BusinessException.badRequest("Khoảng ngày tối đa 2 năm");
        UUID store = storeAccess.requireOrAll(actor, storeId, VIEW);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("store", store, java.sql.Types.OTHER)
                .addValue("from", DbTimes.toDb(from.atStartOfDay())).addValue("to", DbTimes.toDb(to.plusDays(1).atStartOfDay()));
        String base = """
                with r as (
                    select s.id as stocktake_id, s.store_id, st.name as store_name, s.decided_at, i.product_variant_id, i.unit_cost,
                           i.system_quantity as sys,
                           coalesce(i.counted_quantity, case when s.zero_uncounted then 0 end) as cnt,
                           p.category_id, c.name as category_name, v.sku, p.name as product_name, v.color, v.size
                    from stocktakes s
                    join stores st on st.id = s.store_id
                    join stocktake_items i on i.stocktake_id = s.id
                    join product_variants v on v.id = i.product_variant_id
                    join products p on p.id = v.product_id
                    left join categories c on c.id = p.category_id
                    where s.status = 'APPROVED' and s.decided_at >= :from and s.decided_at < :to
                      and (cast(:store as uuid) is null or s.store_id = :store)
                ), x as (
                    select r.*, greatest(sys - cnt, 0) as short_q, greatest(cnt - sys, 0) as surplus_q from r where cnt is not null
                )
                """;
        StocktakeDtos.ReportTotals totals = jdbc.queryForObject(base + """
                select count(distinct stocktake_id) as n, count(*) as items, coalesce(sum(sys), 0) as sys, coalesce(sum(cnt), 0) as cnt,
                       coalesce(sum(short_q), 0) as sq, coalesce(sum(surplus_q), 0) as pq, coalesce(sum(sys * unit_cost), 0) as sv,
                       coalesce(sum(short_q * unit_cost), 0) as shv, coalesce(sum(surplus_q * unit_cost), 0) as pv
                from x
                """, p, (rs, i) -> new StocktakeDtos.ReportTotals(rs.getInt("n"), rs.getInt("items"), rs.getInt("sys"), rs.getInt("cnt"),
                rs.getInt("sq"), rs.getInt("pq"), rs.getBigDecimal("sv"), rs.getBigDecimal("shv"), rs.getBigDecimal("pv"),
                rs.getBigDecimal("pv").subtract(rs.getBigDecimal("shv")), rate(rs.getBigDecimal("shv"), rs.getBigDecimal("sv"))));
        List<StocktakeDtos.StoreRow> stores = jdbc.query(base + """
                select store_id, store_name, count(distinct stocktake_id) as n, sum(short_q) as sq, sum(surplus_q) as pq,
                       sum(short_q * unit_cost) as shv, sum(surplus_q * unit_cost) as pv, sum(sys * unit_cost) as sv
                from x group by store_id, store_name order by shv desc, store_name
                """, p, (rs, i) -> new StocktakeDtos.StoreRow(rs.getObject("store_id", UUID.class), rs.getString("store_name"),
                rs.getInt("n"), rs.getInt("sq"), rs.getInt("pq"), rs.getBigDecimal("shv"), rs.getBigDecimal("pv"),
                rs.getBigDecimal("pv").subtract(rs.getBigDecimal("shv")), rate(rs.getBigDecimal("shv"), rs.getBigDecimal("sv"))));
        List<StocktakeDtos.CategoryRow> categories = jdbc.query(base + """
                select category_id, coalesce(category_name, 'Chưa phân loại') as name, sum(short_q) as sq, sum(surplus_q) as pq,
                       sum(short_q * unit_cost) as shv, sum(surplus_q * unit_cost) as pv
                from x group by category_id, category_name order by shv desc, name limit 20
                """, p, (rs, i) -> new StocktakeDtos.CategoryRow(rs.getObject("category_id", UUID.class), rs.getString("name"),
                rs.getInt("sq"), rs.getInt("pq"), rs.getBigDecimal("shv"), rs.getBigDecimal("pv"),
                rs.getBigDecimal("pv").subtract(rs.getBigDecimal("shv"))));
        List<StocktakeDtos.VariantRow> top = jdbc.query(base + """
                select product_variant_id, sku, product_name, color, size, sum(short_q) as sq, sum(short_q * unit_cost) as shv,
                       count(*) filter (where short_q > 0) as times
                from x where short_q > 0
                group by product_variant_id, sku, product_name, color, size order by shv desc, sq desc limit 10
                """, p, (rs, i) -> new StocktakeDtos.VariantRow(rs.getObject("product_variant_id", UUID.class), rs.getString("sku"),
                rs.getString("product_name"), rs.getString("color"), rs.getString("size"), rs.getInt("sq"), rs.getBigDecimal("shv"),
                rs.getInt("times")));
        List<StocktakeDtos.MonthRow> months = jdbc.query(base + """
                select to_char(decided_at + interval '7 hours', 'YYYY-MM') as m, sum(short_q * unit_cost) as shv,
                       sum(surplus_q * unit_cost) as pv
                from x group by 1 order by 1
                """, p, (rs, i) -> new StocktakeDtos.MonthRow(rs.getString("m"), rs.getBigDecimal("shv"), rs.getBigDecimal("pv"),
                rs.getBigDecimal("pv").subtract(rs.getBigDecimal("shv"))));
        return new StocktakeDtos.Report(from, to, store, totals, stores, categories, top, months);
    }

    // ------------------------------------------------------------------ helpers

    /** Số đếm hiệu lực: chưa đếm = 0 khi phiếu chọn "tính chưa đếm là 0", ngược lại bỏ qua (null). */
    private static final String EFFECTIVE = "coalesce(i.counted_quantity, case when :z then 0 end)";

    /** Giá vốn = giá nhập của phiếu nhập hoàn tất gần nhất; {@code :cat} = danh mục gốc (null = mọi danh mục). */
    private static final String SNAPSHOT = """
            with recursive cat as (select id from categories where id = cast(:cat as uuid)
                                   union all select c.id from categories c join cat on c.parent_id = cat.id),
            cost as (select distinct on (gi.product_variant_id) gi.product_variant_id, gi.cost_price
                     from goods_receipt_items gi join goods_receipts gr on gr.id = gi.receipt_id
                     where gr.status = 'COMPLETED'
                     order by gi.product_variant_id, coalesce(gr.completed_at, gr.receipt_date) desc nulls last)
            insert into stocktake_items(stocktake_id, product_variant_id, system_quantity, unit_cost, cost_source, added_during_count)
            """;

    private static final String ITEM_SELECT = """
            select i.*, v.sku, v.barcode, v.color, v.size, v.product_id, p.name as product_name, c.name as category_name,
                   u.full_name as counted_name
            from stocktake_items i
            join product_variants v on v.id = i.product_variant_id
            join products p on p.id = v.product_id
            left join categories c on c.id = p.category_id
            left join users u on u.id = i.counted_by
            """;

    record Header(UUID id, String code, UUID storeId, UUID categoryId, String status, boolean blind, boolean zeroUncounted,
                  UUID createdBy) {
    }

    private Header header(UUID id) {
        return headerQuery(id, "");
    }

    private Header lockHeader(UUID id) {
        return headerQuery(id, " for update");
    }

    private Header headerQuery(UUID id, String lock) {
        List<Header> rows = jdbc.query("select id, code, store_id, category_id, status, blind_count, zero_uncounted, created_by"
                        + " from stocktakes where id = :id" + lock, new MapSqlParameterSource("id", id),
                (rs, i) -> new Header(rs.getObject(1, UUID.class), rs.getString(2), rs.getObject(3, UUID.class),
                        rs.getObject(4, UUID.class), rs.getString(5), rs.getBoolean(6), rs.getBoolean(7), rs.getObject(8, UUID.class)));
        if (rows.isEmpty()) throw BusinessException.notFound("Phiếu kiểm kê không tồn tại");
        return rows.getFirst();
    }

    private boolean systemVisible(UUID actor, Header h) {
        return !h.blind() || APPROVED.equals(h.status()) || storeAccess.allows(actor, h.storeId(), APPROVE);
    }

    private Stocktake view(UUID actor, UUID id) {
        Header h = header(id);
        boolean visible = systemVisible(actor, h);
        boolean canApprove = storeAccess.allows(actor, h.storeId(), APPROVE);
        boolean canCount = IN_PROGRESS.equals(h.status()) && storeAccess.allows(actor, h.storeId(), COUNT);
        Summary summary = jdbc.queryForObject("""
                select count(*) as items, count(i.counted_quantity) as counted,
                       count(*) filter (where %1$s is not null and %1$s <> i.system_quantity) as variance,
                       coalesce(sum(i.system_quantity), 0) as sys, coalesce(sum(i.counted_quantity), 0) as cnt,
                       coalesce(sum(greatest(i.system_quantity - %1$s, 0)), 0) as sq,
                       coalesce(sum(greatest(%1$s - i.system_quantity, 0)), 0) as pq,
                       coalesce(sum(greatest(i.system_quantity - %1$s, 0) * i.unit_cost), 0) as shv,
                       coalesce(sum(greatest(%1$s - i.system_quantity, 0) * i.unit_cost), 0) as pv
                from stocktake_items i where i.stocktake_id = :id
                """.formatted(EFFECTIVE), new MapSqlParameterSource("id", id).addValue("z", h.zeroUncounted()),
                (rs, i) -> new Summary(rs.getInt("items"), rs.getInt("counted"), visible ? rs.getInt("variance") : 0,
                        visible ? rs.getInt("sys") : null, rs.getInt("cnt"), visible ? rs.getInt("sq") : null,
                        visible ? rs.getInt("pq") : null, visible ? rs.getBigDecimal("shv") : null, visible ? rs.getBigDecimal("pv") : null));
        List<Adjustment> adjustments = jdbc.query("""
                select a.*, u.full_name from stock_adjustments a left join users u on u.id = a.approved_by where a.stocktake_id = :id
                """, new MapSqlParameterSource("id", id), (rs, i) -> new Adjustment(rs.getObject("id", UUID.class), rs.getString("code"),
                rs.getInt("increase_quantity"), rs.getInt("decrease_quantity"), rs.getBigDecimal("increase_value"),
                rs.getBigDecimal("decrease_value"), rs.getInt("line_count"), rs.getString("full_name"),
                DbTimes.fromDb(rs.getTimestamp("created_at"))));
        return jdbc.queryForObject("""
                select s.*, st.name as store_name, c.name as category_name, cu.full_name as created_name,
                       su.full_name as submitted_name, du.full_name as decided_name
                from stocktakes s join stores st on st.id = s.store_id left join categories c on c.id = s.category_id
                left join users cu on cu.id = s.created_by left join users su on su.id = s.submitted_by
                left join users du on du.id = s.decided_by
                where s.id = :id
                """, new MapSqlParameterSource("id", id), (rs, i) -> new Stocktake(id, rs.getString("code"),
                rs.getObject("store_id", UUID.class), rs.getString("store_name"), rs.getString("scope_type"),
                rs.getObject("category_id", UUID.class), rs.getString("category_name"), rs.getBoolean("blind_count"),
                rs.getString("status"), rs.getString("note"), rs.getBoolean("zero_uncounted"), rs.getString("rejection_reason"),
                rs.getString("approver_note"), rs.getString("created_name"), rs.getString("submitted_name"),
                rs.getString("decided_name"), DbTimes.fromDb(rs.getTimestamp("created_at")),
                DbTimes.fromDb(rs.getTimestamp("submitted_at")), DbTimes.fromDb(rs.getTimestamp("decided_at")), summary,
                adjustments.isEmpty() ? null : adjustments.getFirst(), visible, canCount, canApprove));
    }

    private Item item(UUID actor, Header h, UUID itemId) {
        boolean visible = systemVisible(actor, h);
        return jdbc.queryForObject(ITEM_SELECT + " where i.id = :id", new MapSqlParameterSource("id", itemId),
                (rs, i) -> mapItem(rs, visible, h.zeroUncounted()));
    }

    private static Item mapItem(ResultSet rs, boolean visible, boolean zeroUncounted) throws SQLException {
        Integer counted = (Integer) rs.getObject("counted_quantity");
        int system = rs.getInt("system_quantity");
        Integer effective = counted != null ? counted : zeroUncounted ? Integer.valueOf(0) : null;
        return new Item(rs.getObject("id", UUID.class), rs.getObject("product_variant_id", UUID.class), rs.getObject("product_id", UUID.class),
                rs.getString("sku"), rs.getString("barcode"), rs.getString("product_name"), rs.getString("color"), rs.getString("size"),
                rs.getString("category_name"), visible ? system : null, counted,
                visible && effective != null ? effective - system : null, visible ? rs.getBigDecimal("unit_cost") : null,
                rs.getString("cost_source"), (Integer) rs.getObject("adjusted_quantity"), rs.getBoolean("added_during_count"),
                rs.getString("note"), rs.getString("counted_name"), DbTimes.fromDb(rs.getTimestamp("counted_at")));
    }

    private UUID itemId(UUID stocktake, UUID variant) {
        List<UUID> ids = jdbc.queryForList("select id from stocktake_items where stocktake_id = :s and product_variant_id = :v",
                new MapSqlParameterSource("s", stocktake).addValue("v", variant), UUID.class);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private void logCount(UUID itemId, String mode, int quantity, int after, String code, UUID actor) {
        jdbc.update("""
                insert into stocktake_count_logs(item_id, mode, quantity, counted_after, scanned_code, actor_id, created_at)
                values (:item, :mode, :q, :after, :code, :actor, now() at time zone 'UTC')
                """, new MapSqlParameterSource("item", itemId).addValue("mode", mode).addValue("q", quantity).addValue("after", after)
                .addValue("code", code, java.sql.Types.VARCHAR).addValue("actor", actor));
    }

    private static void requireStatus(Header h, String status, String message) {
        if (!status.equals(h.status())) throw BusinessException.invalidState(message);
    }

    private static BigDecimal rate(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) return BigDecimal.ZERO;
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 2, RoundingMode.HALF_UP);
    }

    private static String variantText(Item item) {
        String text = String.join(" / ", java.util.stream.Stream.of(item.color(), item.size()).filter(StringUtils::hasText).toList());
        return text.isEmpty() ? "(" + item.sku() + ")" : "(" + text + ")";
    }

    private String nextCode(String prefix, String sequence) {
        Long value = jdbc.getJdbcTemplate().queryForObject("select nextval('" + sequence + "')", Long.class);
        return "%s-%s-%05d".formatted(prefix, LocalDate.now(ZONE).format(CODE_DATE), value);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void notifyStore(UUID storeId, String permission, UUID actor, String title, String message, UUID stocktakeId) {
        try {
            Set<RealtimeIdentity> to = new java.util.HashSet<>(recipients.resolveStoreStaff(permission, storeId));
            if (to.isEmpty() && APPROVE.equals(permission)) to.addAll(recipients.resolveGlobalUsers(permission));
            to.remove(RealtimeIdentity.user(actor));
            if (to.isEmpty()) return;
            notifications.publish(new NotificationRequest(List.copyOf(to), actor, "STOCKTAKE", title, message, "STOCKTAKE", stocktakeId,
                    "/admin/stocktakes/" + stocktakeId, Map.of()));
        } catch (RuntimeException failure) {
            log.warn("Stocktake notification failed: {}", title, failure);
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
