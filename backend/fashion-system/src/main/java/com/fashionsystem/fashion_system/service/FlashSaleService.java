package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.dto.FlashSaleDtos;
import com.fashionsystem.fashion_system.entity.FlashSale;
import com.fashionsystem.fashion_system.entity.FlashSaleItem;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.FlashSaleItemRepository;
import com.fashionsystem.fashion_system.repository.FlashSaleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flash sale cho đơn online. Một biến thể chỉ thuộc một chương trình đang bật tại một thời điểm (khung giờ không
 * chồng nhau). Suất đã dùng tính từ {@code flash_sale_usages} của đơn chưa hủy / chưa bỏ dở thanh toán, nên đơn
 * bị hủy tự trả suất. Khi đặt đơn, dòng flash sale bị khóa để hai người không cùng lấy suất cuối.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashSaleService {
    /** Chương trình đang chạy (giờ lưu UTC). */
    private static final String RUNNING = """
            fs.active and fs.starts_at <= now() at time zone 'UTC' and fs.ends_at > now() at time zone 'UTC'""";
    /** Lượt dùng còn hiệu lực: đơn chưa hủy, không phải đơn qua cổng đã bỏ dở. */
    private static final String COUNTED_USAGE = """
            from flash_sale_usages u join orders o on o.id = u.order_id
            where o.status <> 'CANCELLED' and coalesce(o.payment_status, '') <> 'ABANDONED'""";

    private final FlashSaleRepository sales;
    private final FlashSaleItemRepository items;
    private final NamedParameterJdbcTemplate jdbc;

    /** Phần giá flash của một dòng giỏ: {@code quantity} món được giảm {@code unitDiscount} mỗi món. */
    public record Allocation(UUID itemId, int quantity, BigDecimal unitDiscount) {
    }

    // ------------------------------------------------------------------ pricing (online checkout)

    /**
     * Số món giá flash cho từng dòng: tối thiểu của số mua, suất còn lại và hạn mức còn lại của khách.
     *
     * @param unitPrices giá bán thường hiện tại theo biến thể
     * @param lock       khóa dòng flash sale (khi tạo đơn)
     */
    public Map<UUID, Allocation> allocate(UUID customerId, Map<UUID, Integer> demand, Map<UUID, BigDecimal> unitPrices, boolean lock) {
        if (demand.isEmpty()) return Map.of();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select i.id, i.product_variant_id, i.sale_price, i.quota
                from flash_sale_items i join flash_sales fs on fs.id = i.flash_sale_id
                where i.product_variant_id in (:variants) and """ + " " + RUNNING
                + (lock ? " order by i.id for update of i" : ""), new MapSqlParameterSource("variants", demand.keySet()));
        Map<UUID, Allocation> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            UUID itemId = (UUID) row.get("id");
            UUID variant = (UUID) row.get("product_variant_id");
            BigDecimal unitDiscount = unitPrices.getOrDefault(variant, BigDecimal.ZERO).subtract((BigDecimal) row.get("sale_price"));
            if (unitDiscount.signum() <= 0) continue;
            int remaining = ((Number) row.get("quota")).intValue() - used(itemId, null);
            int customerLeft = FlashSaleDtos.PER_CUSTOMER - used(itemId, customerId);
            int quantity = Math.min(demand.get(variant), Math.min(remaining, customerLeft));
            if (quantity > 0) result.put(variant, new Allocation(itemId, quantity, unitDiscount));
        }
        return result;
    }

    /** Ghi lượt dùng suất cho đơn vừa tạo (trong transaction tạo đơn). */
    @Transactional
    @com.fashionsystem.fashion_system.audit.AuditInfrastructure(
            reason = "Quota usage rows belong to the audited order placement that calls this")
    public void recordUsage(UUID orderId, UUID customerId, Collection<Allocation> allocations) {
        for (Allocation allocation : allocations) {
            jdbc.update("""
                    insert into flash_sale_usages (flash_sale_item_id, order_id, customer_id, quantity)
                    values (:item, :order, :customer, :quantity)""",
                    new MapSqlParameterSource("item", allocation.itemId()).addValue("order", orderId)
                            .addValue("customer", customerId).addValue("quantity", allocation.quantity()));
        }
    }

    private int used(UUID itemId, UUID customerId) {
        MapSqlParameterSource params = new MapSqlParameterSource("item", itemId);
        String sql = "select coalesce(sum(u.quantity), 0) " + COUNTED_USAGE + " and u.flash_sale_item_id = :item";
        if (customerId != null) {
            sql += " and u.customer_id = :customer";
            params.addValue("customer", customerId);
        }
        Integer value = jdbc.queryForObject(sql, params, Integer.class);
        return value == null ? 0 : value;
    }

    // ------------------------------------------------------------------ storefront

    /** Chương trình đang chạy, kết thúc sớm nhất trước; chỉ biến thể / sản phẩm còn bán. */
    public List<FlashSaleDtos.LiveSale> running() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select fs.id as sale_id, fs.name as sale_name, fs.starts_at, fs.ends_at,
                       i.id as item_id, i.product_variant_id, i.sale_price, i.quota, p.id as product_id, p.name,
                       v.color, v.size, coalesce(v.sale_price, v.price) as regular_price,
                       coalesce(nullif(p.image_url, ''), (select pi.image_url from product_images pi
                           join product_variants iv on iv.id = pi.product_variant_id where iv.product_id = p.id
                           order by pi.is_primary desc nulls last, pi.sort_order nulls last limit 1)) as image_url
                from flash_sales fs
                join flash_sale_items i on i.flash_sale_id = fs.id
                join product_variants v on v.id = i.product_variant_id
                join products p on p.id = v.product_id
                where v.active and p.status = 'ACTIVE' and """ + " " + RUNNING + """

                order by fs.ends_at, fs.id, p.name, v.size""", new MapSqlParameterSource());
        Map<UUID, Integer> usedByItem = usedByItems(rows.stream().map(r -> (UUID) r.get("item_id")).toList());
        Map<UUID, FlashSaleDtos.LiveSale> bySale = new LinkedHashMap<>();
        Map<UUID, List<FlashSaleDtos.LiveItem>> itemsBySale = new HashMap<>();
        for (Map<String, Object> row : rows) {
            UUID saleId = (UUID) row.get("sale_id");
            List<FlashSaleDtos.LiveItem> list = itemsBySale.computeIfAbsent(saleId, ignored -> new ArrayList<>());
            bySale.putIfAbsent(saleId, new FlashSaleDtos.LiveSale(saleId, (String) row.get("sale_name"),
                    com.fashionsystem.fashion_system.util.DbTimes.fromDb(row.get("starts_at")),
                    com.fashionsystem.fashion_system.util.DbTimes.fromDb(row.get("ends_at")), list));
            int quota = ((Number) row.get("quota")).intValue();
            list.add(new FlashSaleDtos.LiveItem((UUID) row.get("product_variant_id"), (UUID) row.get("product_id"),
                    (String) row.get("name"), (String) row.get("color"), (String) row.get("size"), (String) row.get("image_url"),
                    (BigDecimal) row.get("regular_price"), (BigDecimal) row.get("sale_price"), quota,
                    Math.max(0, quota - usedByItem.getOrDefault((UUID) row.get("item_id"), 0))));
        }
        return List.copyOf(bySale.values());
    }

    private Map<UUID, Integer> usedByItems(List<UUID> itemIds) {
        Map<UUID, Integer> used = new HashMap<>();
        if (itemIds.isEmpty()) return used;
        jdbc.query("select u.flash_sale_item_id, sum(u.quantity) as qty " + COUNTED_USAGE
                        + " and u.flash_sale_item_id in (:items) group by u.flash_sale_item_id",
                new MapSqlParameterSource("items", itemIds),
                rs -> { used.put(rs.getObject("flash_sale_item_id", UUID.class), rs.getInt("qty")); });
        return used;
    }

    // ------------------------------------------------------------------ admin

    public Page<FlashSaleDtos.AdminSale> list(Pageable pageable) {
        Long total = jdbc.queryForObject("select count(*) from flash_sales", new MapSqlParameterSource(), Long.class);
        List<UUID> ids = jdbc.queryForList("select id from flash_sales order by starts_at desc, id limit :limit offset :offset",
                new MapSqlParameterSource("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()), UUID.class);
        Map<UUID, FlashSale> byId = sales.findAllById(ids).stream().collect(Collectors.toMap(FlashSale::getId, Function.identity()));
        return new PageImpl<>(views(ids.stream().map(byId::get).toList()), pageable, total == null ? 0 : total);
    }

    public FlashSaleDtos.AdminSale get(UUID id) {
        return views(List.of(find(id))).getFirst();
    }

    @Transactional
    @BusinessAudit("FLASH_SALE")
    public FlashSaleDtos.AdminSale create(UUID actor, FlashSaleDtos.SaleRequest request) {
        validate(null, request);
        LocalDateTime now = LocalDateTime.now();
        FlashSale sale = sales.saveAndFlush(FlashSale.builder().name(request.name().trim()).startsAt(request.startsAt())
                .endsAt(request.endsAt()).active(true).createdBy(actor).createdAt(now).build());
        request.items().forEach(item -> items.save(FlashSaleItem.builder().flashSaleId(sale.getId())
                .productVariantId(item.variantId()).salePrice(item.salePrice()).quota(item.quota()).build()));
        items.flush();
        return get(sale.getId());
    }

    /**
     * Sửa chương trình chưa kết thúc. Biến thể đã có người mua không được bỏ ra và không được giảm suất dưới số đã bán.
     */
    @Transactional
    @BusinessAudit("FLASH_SALE")
    public FlashSaleDtos.AdminSale update(UUID id, FlashSaleDtos.SaleRequest request) {
        FlashSale sale = find(id);
        if (!sale.getEndsAt().isAfter(LocalDateTime.now())) throw BusinessException.invalidState("Chương trình đã kết thúc, không sửa được");
        validate(id, request);
        Map<UUID, FlashSaleItem> current = items.findAllByFlashSaleIdIn(List.of(id)).stream()
                .collect(Collectors.toMap(FlashSaleItem::getProductVariantId, Function.identity()));
        Map<UUID, Integer> sold = usedByItems(current.values().stream().map(FlashSaleItem::getId).toList());
        Set<UUID> kept = new HashSet<>();
        for (FlashSaleDtos.ItemRequest wanted : request.items()) {
            kept.add(wanted.variantId());
            FlashSaleItem existing = current.get(wanted.variantId());
            if (existing == null) {
                items.save(FlashSaleItem.builder().flashSaleId(id).productVariantId(wanted.variantId())
                        .salePrice(wanted.salePrice()).quota(wanted.quota()).build());
                continue;
            }
            int alreadySold = sold.getOrDefault(existing.getId(), 0);
            if (wanted.quota() < alreadySold)
                throw BusinessException.badRequest("Số suất không được nhỏ hơn số đã bán (" + alreadySold + ")");
            existing.setSalePrice(wanted.salePrice());
            existing.setQuota(wanted.quota());
            items.save(existing);
        }
        for (FlashSaleItem existing : current.values()) {
            if (kept.contains(existing.getProductVariantId())) continue;
            if (sold.getOrDefault(existing.getId(), 0) > 0)
                throw BusinessException.badRequest("Không bỏ được sản phẩm đã có người mua khỏi chương trình");
            items.delete(existing);
        }
        sale.setName(request.name().trim());
        sale.setStartsAt(request.startsAt());
        sale.setEndsAt(request.endsAt());
        sale.setUpdatedAt(LocalDateTime.now());
        sales.saveAndFlush(sale);
        items.flush();
        return get(id);
    }

    @Transactional
    @BusinessAudit("FLASH_SALE")
    public FlashSaleDtos.AdminSale setActive(UUID id, boolean active) {
        FlashSale sale = find(id);
        if (active) validateOverlap(id, sale.getStartsAt(), sale.getEndsAt(),
                items.findAllByFlashSaleIdIn(List.of(id)).stream().map(FlashSaleItem::getProductVariantId).toList());
        sale.setActive(active);
        sale.setUpdatedAt(LocalDateTime.now());
        sales.saveAndFlush(sale);
        return get(id);
    }

    private void validate(UUID id, FlashSaleDtos.SaleRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) throw BusinessException.badRequest("Giờ kết thúc phải sau giờ bắt đầu");
        if (id == null && !request.endsAt().isAfter(LocalDateTime.now())) throw BusinessException.badRequest("Khung giờ đã qua");
        Set<UUID> seen = new HashSet<>();
        Map<UUID, Map<String, Object>> variants = new HashMap<>();
        jdbc.queryForList("""
                select v.id, v.sku, v.active and p.status = 'ACTIVE' as sellable, coalesce(v.sale_price, v.price) as price
                from product_variants v join products p on p.id = v.product_id where v.id in (:ids)""",
                new MapSqlParameterSource("ids", request.items().stream().map(FlashSaleDtos.ItemRequest::variantId).toList()))
                .forEach(row -> variants.put((UUID) row.get("id"), row));
        for (FlashSaleDtos.ItemRequest item : request.items()) {
            if (!seen.add(item.variantId())) throw BusinessException.badRequest("Mỗi biến thể chỉ thêm một lần");
            Map<String, Object> variant = variants.get(item.variantId());
            if (variant == null) throw BusinessException.notFound("Biến thể không tồn tại");
            if (!Boolean.TRUE.equals(variant.get("sellable"))) throw BusinessException.badRequest(variant.get("sku") + " đã ngừng bán");
            if (item.salePrice().compareTo((BigDecimal) variant.get("price")) >= 0)
                throw BusinessException.badRequest("Giá flash của " + variant.get("sku") + " phải thấp hơn giá bán hiện tại");
        }
        validateOverlap(id, request.startsAt(), request.endsAt(), new ArrayList<>(seen));
    }

    /** Một biến thể không được nằm trong hai chương trình đang bật có khung giờ chồng nhau. */
    private void validateOverlap(UUID id, LocalDateTime startsAt, LocalDateTime endsAt, List<UUID> variantIds) {
        if (variantIds.isEmpty()) return;
        List<String> clashes = jdbc.queryForList("""
                select distinct v.sku from flash_sale_items i
                join flash_sales fs on fs.id = i.flash_sale_id
                join product_variants v on v.id = i.product_variant_id
                where fs.active and fs.id <> coalesce(:id, '00000000-0000-0000-0000-000000000000'::uuid)
                  and i.product_variant_id in (:variants)
                  and fs.starts_at < :ends and fs.ends_at > :starts""",
                new MapSqlParameterSource("id", id).addValue("variants", variantIds)
                        .addValue("starts", com.fashionsystem.fashion_system.util.DbTimes.toDb(startsAt))
                        .addValue("ends", com.fashionsystem.fashion_system.util.DbTimes.toDb(endsAt)), String.class);
        if (!clashes.isEmpty())
            throw BusinessException.conflict("Biến thể " + String.join(", ", clashes) + " đã thuộc flash sale khác trong khung giờ này");
    }

    private FlashSale find(UUID id) {
        return sales.findById(id).orElseThrow(() -> BusinessException.notFound("Flash sale không tồn tại"));
    }

    private List<FlashSaleDtos.AdminSale> views(List<FlashSale> list) {
        if (list.isEmpty()) return List.of();
        List<FlashSaleItem> all = items.findAllByFlashSaleIdIn(list.stream().map(FlashSale::getId).toList());
        Map<UUID, Integer> sold = usedByItems(all.stream().map(FlashSaleItem::getId).toList());
        Map<UUID, Map<String, Object>> info = new HashMap<>();
        if (!all.isEmpty()) jdbc.queryForList("""
                select v.id, p.id as product_id, v.sku, p.name, v.color, v.size, coalesce(v.sale_price, v.price) as price
                from product_variants v join products p on p.id = v.product_id where v.id in (:ids)""",
                new MapSqlParameterSource("ids", all.stream().map(FlashSaleItem::getProductVariantId).distinct().toList()))
                .forEach(row -> info.put((UUID) row.get("id"), row));
        Map<UUID, List<FlashSaleItem>> bySale = all.stream().collect(Collectors.groupingBy(FlashSaleItem::getFlashSaleId));
        LocalDateTime now = LocalDateTime.now();
        return list.stream().map(sale -> new FlashSaleDtos.AdminSale(sale.getId(), sale.getName(), sale.getStartsAt(),
                sale.getEndsAt(), Boolean.TRUE.equals(sale.getActive()), status(sale, now),
                bySale.getOrDefault(sale.getId(), List.of()).stream().map(item -> {
                    Map<String, Object> row = info.getOrDefault(item.getProductVariantId(), Map.of());
                    return new FlashSaleDtos.AdminItem(item.getProductVariantId(), (UUID) row.get("product_id"),
                            (String) row.get("sku"), (String) row.get("name"), (String) row.get("color"), (String) row.get("size"),
                            (BigDecimal) row.get("price"), item.getSalePrice(), item.getQuota(), sold.getOrDefault(item.getId(), 0));
                }).toList())).toList();
    }

    static String status(FlashSale sale, LocalDateTime now) {
        if (!Boolean.TRUE.equals(sale.getActive())) return "STOPPED";
        if (now.isBefore(sale.getStartsAt())) return "UPCOMING";
        return now.isBefore(sale.getEndsAt()) ? "RUNNING" : "ENDED";
    }
}
