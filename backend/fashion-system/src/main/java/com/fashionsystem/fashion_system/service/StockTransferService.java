package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.dto.StockTransferDtos;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.entity.StockTransfer;
import com.fashionsystem.fashion_system.entity.StockTransferItem;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.notification.PermissionRecipientResolver;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StockTransferItemRepository;
import com.fashionsystem.fashion_system.repository.StockTransferRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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

/**
 * Điều chuyển tồn tại quầy giữa cửa hàng: cửa hàng nhận tạo yêu cầu → cửa hàng gửi duyệt và xuất (trừ tồn, hàng
 * đang đi đường) hoặc từ chối → cửa hàng nhận xác nhận đã nhận (cộng tồn). Nhân viên chỉ thao tác ở phía cửa
 * hàng của mình; nhân viên toàn hệ thống thao tác được cả hai phía.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTransferService {
    static final String VIEW = "TRANSFER_VIEW";
    static final String MANAGE = "TRANSFER_MANAGE";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final StockTransferRepository transfers;
    private final StockTransferItemRepository transferItems;
    private final ProductVariantRepository variants;
    private final StoreRepository stores;
    private final InventoryService inventory;
    private final StoreAccessService storeAccess;
    private final NotificationPublisher notifications;
    private final PermissionRecipientResolver recipients;
    private final NamedParameterJdbcTemplate jdbc;

    // ------------------------------------------------------------------ workflow

    @Transactional
    @BusinessAudit("STOCK_TRANSFER")
    public StockTransferDtos.Transfer create(UUID actor, StockTransferDtos.CreateRequest request) {
        UUID toStore = storeAccess.require(actor, request.toStoreId(), MANAGE);
        if (toStore.equals(request.fromStoreId())) throw BusinessException.badRequest("Cửa hàng gửi phải khác cửa hàng nhận");
        stores.findById(request.fromStoreId()).filter(s -> Boolean.TRUE.equals(s.getActive()))
                .orElseThrow(() -> BusinessException.notFound("Cửa hàng gửi không hoạt động hoặc không tồn tại"));
        Set<UUID> seen = new HashSet<>();
        List<StockTransferItem> items = new ArrayList<>();
        for (StockTransferDtos.CreateItem item : request.items()) {
            if (!seen.add(item.variantId())) throw BusinessException.badRequest("Mỗi sản phẩm chỉ chọn một lần");
            ProductVariant variant = variants.findById(item.variantId())
                    .orElseThrow(() -> BusinessException.notFound("Sản phẩm không tồn tại"));
            if (!Boolean.TRUE.equals(variant.getActive())) throw BusinessException.badRequest("Sản phẩm " + variant.getSku() + " đã ngừng bán");
            items.add(StockTransferItem.builder().productVariantId(item.variantId()).requestedQuantity(item.quantity()).build());
        }
        LocalDateTime now = LocalDateTime.now();
        StockTransfer saved = transfers.saveAndFlush(StockTransfer.builder().transferCode(nextCode())
                .fromStoreId(request.fromStoreId()).toStoreId(toStore).status("REQUESTED")
                .note(request.note() == null || request.note().isBlank() ? null : request.note().trim())
                .requestedBy(actor).createdAt(now).build());
        items.forEach(item -> item.setTransferId(saved.getId()));
        transferItems.saveAllAndFlush(items);
        String to = storeName(toStore);
        afterCommit(() -> notifyStore(saved, saved.getFromStoreId(), NotificationType.STOCK_TRANSFER_REQUESTED,
                "Yêu cầu chuyển hàng " + saved.getTransferCode(), to + " xin " + items.size() + " sản phẩm. Vui lòng duyệt và xuất hàng."));
        return view(saved);
    }

    @Transactional
    @BusinessAudit("STOCK_TRANSFER")
    public StockTransferDtos.Transfer cancel(UUID actor, UUID id) {
        StockTransfer transfer = lock(id);
        storeAccess.require(actor, transfer.getToStoreId(), MANAGE);
        requireStatus(transfer, "REQUESTED", "Chỉ hủy được yêu cầu chưa được duyệt");
        transfer.setStatus("CANCELLED");
        transfer.setUpdatedAt(LocalDateTime.now());
        return view(transfers.saveAndFlush(transfer));
    }

    @Transactional
    @BusinessAudit("STOCK_TRANSFER")
    public StockTransferDtos.Transfer reject(UUID actor, UUID id, String reason) {
        StockTransfer transfer = lock(id);
        storeAccess.require(actor, transfer.getFromStoreId(), MANAGE);
        requireStatus(transfer, "REQUESTED", "Chỉ từ chối được yêu cầu chưa được duyệt");
        transfer.setStatus("REJECTED");
        transfer.setRejectReason(reason.trim());
        transfer.setUpdatedAt(LocalDateTime.now());
        StockTransfer saved = transfers.saveAndFlush(transfer);
        afterCommit(() -> notifyStore(saved, saved.getToStoreId(), NotificationType.STOCK_TRANSFER_UPDATED,
                "Yêu cầu chuyển hàng " + saved.getTransferCode() + " bị từ chối", "Lý do: " + saved.getRejectReason()));
        return view(saved);
    }

    /** Nơi gửi duyệt và xuất: số thực xuất ≤ số yêu cầu và ≤ tồn tại quầy; phải xuất ít nhất một sản phẩm. */
    @Transactional
    @BusinessAudit("STOCK_TRANSFER")
    public StockTransferDtos.Transfer ship(UUID actor, UUID id, Map<UUID, Integer> quantities) {
        StockTransfer transfer = lock(id);
        storeAccess.require(actor, transfer.getFromStoreId(), MANAGE);
        requireStatus(transfer, "REQUESTED", "Chỉ xuất được yêu cầu đang chờ duyệt");
        List<StockTransferItem> items = transferItems.findAllByTransferIdIn(List.of(transfer.getId()));
        int total = 0;
        for (StockTransferItem item : items) {
            Integer wanted = quantities == null ? null : quantities.get(item.getProductVariantId());
            int qty = wanted == null ? item.getRequestedQuantity() : wanted;
            if (qty < 0 || qty > item.getRequestedQuantity())
                throw BusinessException.badRequest("Số xuất phải từ 0 đến số yêu cầu (" + item.getRequestedQuantity() + ")");
            if (qty > 0) inventory.transferOut(transfer.getFromStoreId(), item.getProductVariantId(), qty, transfer.getId(), actor);
            item.setShippedQuantity(qty);
            total += qty;
        }
        if (total == 0) throw BusinessException.badRequest("Phải xuất ít nhất một sản phẩm; nếu không có hàng hãy từ chối yêu cầu");
        transferItems.saveAllAndFlush(items);
        LocalDateTime now = LocalDateTime.now();
        transfer.setStatus("SHIPPED");
        transfer.setShippedBy(actor);
        transfer.setShippedAt(now);
        transfer.setUpdatedAt(now);
        StockTransfer saved = transfers.saveAndFlush(transfer);
        int shipped = total;
        String from = storeName(saved.getFromStoreId());
        afterCommit(() -> notifyStore(saved, saved.getToStoreId(), NotificationType.STOCK_TRANSFER_UPDATED,
                "Hàng đang chuyển tới · " + saved.getTransferCode(), from + " đã xuất " + shipped + " sản phẩm. Xác nhận khi nhận được hàng."));
        return view(saved);
    }

    @Transactional
    @BusinessAudit("STOCK_TRANSFER")
    public StockTransferDtos.Transfer receive(UUID actor, UUID id) {
        StockTransfer transfer = lock(id);
        storeAccess.require(actor, transfer.getToStoreId(), MANAGE);
        requireStatus(transfer, "SHIPPED", "Chỉ xác nhận nhận được phiếu đang chuyển");
        for (StockTransferItem item : transferItems.findAllByTransferIdIn(List.of(transfer.getId()))) {
            if (item.getShippedQuantity() != null && item.getShippedQuantity() > 0)
                inventory.transferIn(transfer.getToStoreId(), item.getProductVariantId(), item.getShippedQuantity(), transfer.getId(), actor);
        }
        LocalDateTime now = LocalDateTime.now();
        transfer.setStatus("RECEIVED");
        transfer.setReceivedBy(actor);
        transfer.setReceivedAt(now);
        transfer.setUpdatedAt(now);
        StockTransfer saved = transfers.saveAndFlush(transfer);
        String to = storeName(saved.getToStoreId());
        afterCommit(() -> notifyStore(saved, saved.getFromStoreId(), NotificationType.STOCK_TRANSFER_UPDATED,
                "Đã nhận hàng chuyển · " + saved.getTransferCode(), to + " đã xác nhận nhận hàng."));
        return view(saved);
    }

    // ------------------------------------------------------------------ queries

    /** Phiếu liên quan tới cửa hàng; {@code direction} IN (nhận về) / OUT (gửi đi) / null (cả hai). */
    public Page<StockTransferDtos.Transfer> list(UUID actor, UUID storeId, String direction, String status, Pageable pageable) {
        UUID store = storeAccess.requireOrAll(actor, storeId, VIEW);
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" from stock_transfers t where 1 = 1");
        if (store != null) {
            params.addValue("store", store);
            where.append("IN".equalsIgnoreCase(direction) ? " and t.to_store_id = :store"
                    : "OUT".equalsIgnoreCase(direction) ? " and t.from_store_id = :store"
                    : " and (t.to_store_id = :store or t.from_store_id = :store)");
        }
        if (status != null && !status.isBlank()) {
            where.append(" and t.status = :status");
            params.addValue("status", status.trim().toUpperCase());
        }
        Long total = jdbc.queryForObject("select count(*)" + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<UUID> ids = jdbc.queryForList("select t.id" + where + " order by t.created_at desc, t.id limit :limit offset :offset",
                params, UUID.class);
        Map<UUID, StockTransfer> byId = transfers.findAllById(ids).stream().collect(Collectors.toMap(StockTransfer::getId, Function.identity()));
        return new PageImpl<>(views(ids.stream().map(byId::get).filter(Objects::nonNull).toList()), pageable, total == null ? 0 : total);
    }

    public StockTransferDtos.Transfer detail(UUID actor, UUID id) {
        StockTransfer transfer = transfers.findById(id).orElseThrow(() -> BusinessException.notFound("Phiếu điều chuyển không tồn tại"));
        if (!storeAccess.allows(actor, transfer.getFromStoreId(), VIEW) && !storeAccess.allows(actor, transfer.getToStoreId(), VIEW))
            throw BusinessException.forbidden("Bạn không có quyền xem phiếu điều chuyển này");
        return view(transfer);
    }

    /**
     * Biến thể đang bán của cửa hàng có tổng tồn (tại quầy + online) ≤ {@link DashboardService#LOW_STOCK_THRESHOLD},
     * kể cả đã hết, ít tồn nhất trước; gợi ý cửa hàng khác đang có tồn tại quầy nhiều nhất vượt ngưỡng.
     */
    public Page<StockTransferDtos.LowStock> lowStock(UUID actor, UUID storeId, String keyword, Pageable pageable) {
        UUID store = storeAccess.require(actor, storeId, VIEW);
        MapSqlParameterSource params = new MapSqlParameterSource("store", store)
                .addValue("threshold", DashboardService.LOW_STOCK_THRESHOLD);
        String joins = """
                 from inventory_balances b
                join product_variants v on v.id = b.product_variant_id
                join products p on p.id = v.product_id""";
        StringBuilder where = new StringBuilder("""

                where b.store_id = :store and v.active and p.status = 'ACTIVE'
                  and b.available_quantity + b.online_quantity <= :threshold""");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and (p.name ilike :kw or v.sku ilike :kw)");
            params.addValue("kw", "%" + keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        Long total = jdbc.queryForObject("select count(*)" + joins + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<StockTransferDtos.LowStock> rows = jdbc.query("""
                select v.id as variant_id, p.id as product_id, v.sku, p.name, v.color, v.size,
                       b.available_quantity as counter, b.online_quantity as online,
                       coalesce((select sum(coalesce(i.shipped_quantity, i.requested_quantity))
                                 from stock_transfer_items i join stock_transfers t on t.id = i.transfer_id
                                 where t.to_store_id = :store and t.status in ('REQUESTED', 'SHIPPED')
                                   and i.product_variant_id = v.id), 0) as incoming,
                       best.store_id as best_store, best.name as best_name, best.qty as best_qty
                """ + joins + """

                left join lateral (
                    select ob.store_id, s.name, ob.available_quantity as qty
                    from inventory_balances ob join stores s on s.id = ob.store_id
                    where ob.product_variant_id = v.id and ob.store_id <> :store and s.active
                      and ob.available_quantity > :threshold
                    order by ob.available_quantity desc, s.name limit 1
                ) best on true""" + where + """

                order by b.available_quantity + b.online_quantity, p.name, v.sku
                limit :limit offset :offset""",
                params, (rs, i) -> new StockTransferDtos.LowStock(rs.getObject("variant_id", UUID.class),
                        rs.getObject("product_id", UUID.class), rs.getString("sku"), rs.getString("name"),
                        rs.getString("color"), rs.getString("size"), rs.getInt("counter"), rs.getInt("online"),
                        rs.getInt("counter") + rs.getInt("online"), rs.getInt("incoming"),
                        rs.getObject("best_store", UUID.class) == null ? null
                                : new StockTransferDtos.StoreRef(rs.getObject("best_store", UUID.class), rs.getString("best_name")),
                        rs.getObject("best_qty", Integer.class)));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    // ------------------------------------------------------------------ helpers

    private StockTransfer lock(UUID id) {
        return transfers.findByIdForUpdate(id).orElseThrow(() -> BusinessException.notFound("Phiếu điều chuyển không tồn tại"));
    }

    private static void requireStatus(StockTransfer transfer, String status, String message) {
        if (!status.equals(transfer.getStatus())) throw BusinessException.invalidState(message);
    }

    private String nextCode() {
        Long value = jdbc.getJdbcTemplate().queryForObject("select nextval('stock_transfer_code_seq')", Long.class);
        return "CK-%s-%05d".formatted(LocalDate.now(ZONE).format(CODE_DATE), value);
    }

    private String storeName(UUID storeId) {
        return stores.findById(storeId).map(Store::getName).orElse("Cửa hàng");
    }

    private StockTransferDtos.Transfer view(StockTransfer transfer) {
        return views(List.of(transfer)).getFirst();
    }

    private List<StockTransferDtos.Transfer> views(List<StockTransfer> list) {
        if (list.isEmpty()) return List.of();
        Set<UUID> storeIds = new HashSet<>();
        list.forEach(t -> { storeIds.add(t.getFromStoreId()); storeIds.add(t.getToStoreId()); });
        Map<UUID, String> names = stores.findAllById(storeIds).stream().collect(Collectors.toMap(Store::getId, Store::getName));
        Map<UUID, List<StockTransferItem>> itemsByTransfer = transferItems.findAllByTransferIdIn(list.stream().map(StockTransfer::getId).toList())
                .stream().collect(Collectors.groupingBy(StockTransferItem::getTransferId));
        Map<String, Map<String, Object>> info = new HashMap<>();
        List<UUID> variantIds = itemsByTransfer.values().stream().flatMap(List::stream).map(StockTransferItem::getProductVariantId).distinct().toList();
        if (!variantIds.isEmpty()) {
            jdbc.queryForList("""
                    select v.id::text as variant_id, s.id::text as store_id, v.sku, p.name, v.color, v.size,
                           coalesce(b.available_quantity, 0) as stock
                    from product_variants v join products p on p.id = v.product_id
                    cross join stores s
                    left join inventory_balances b on b.product_variant_id = v.id and b.store_id = s.id
                    where v.id in (:variants) and s.id in (:stores)""",
                    new MapSqlParameterSource("variants", variantIds).addValue("stores", storeIds))
                    .forEach(row -> info.put(row.get("variant_id") + ":" + row.get("store_id"), row));
        }
        return list.stream().map(t -> new StockTransferDtos.Transfer(t.getId(), t.getTransferCode(),
                new StockTransferDtos.StoreRef(t.getFromStoreId(), names.get(t.getFromStoreId())),
                new StockTransferDtos.StoreRef(t.getToStoreId(), names.get(t.getToStoreId())), t.getStatus(),
                StockTransferDtos.STATUS_LABELS.getOrDefault(t.getStatus(), t.getStatus()), t.getNote(), t.getRejectReason(),
                itemsByTransfer.getOrDefault(t.getId(), List.of()).stream().map(item -> {
                    Map<String, Object> row = info.getOrDefault(item.getProductVariantId() + ":" + t.getFromStoreId(), Map.of());
                    return new StockTransferDtos.Item(item.getProductVariantId(), (String) row.get("sku"), (String) row.get("name"),
                            (String) row.get("color"), (String) row.get("size"), item.getRequestedQuantity(), item.getShippedQuantity(),
                            row.get("stock") == null ? 0 : ((Number) row.get("stock")).intValue());
                }).toList(),
                t.getCreatedAt(), t.getShippedAt(), t.getReceivedAt())).toList();
    }

    private void notifyStore(StockTransfer transfer, UUID storeId, String type, String title, String message) {
        Set<RealtimeIdentity> to = recipients.resolveStoreStaff(MANAGE, storeId);
        if (to.isEmpty()) return;
        try {
            notifications.publish(new NotificationRequest(List.copyOf(to), null, type, title, message, "STOCK_TRANSFER",
                    transfer.getId(), "/admin/transfers?id=" + transfer.getId(), Map.of("transferCode", transfer.getTransferCode())));
        } catch (RuntimeException failure) {
            log.warn("Transfer notification {} for {} failed: {}", type, transfer.getTransferCode(), failure.getMessage());
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                action.run();
            }
        });
    }
}
