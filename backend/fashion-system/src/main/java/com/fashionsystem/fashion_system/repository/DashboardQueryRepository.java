package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse;
import com.fashionsystem.fashion_system.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Truy vấn chỉ đọc cho trang tổng quan. {@code all = true} là toàn hệ thống, khi đó {@code storeId} chỉ là giá trị
 * giữ chỗ (cờ boolean để PostgreSQL luôn suy được kiểu tham số). Chỉ tính đơn cấp cửa hàng: đơn cha của đơn tách
 * ({@code storeId} null) bị bỏ qua để không đếm trùng; đơn trả qua cổng chưa thanh toán cũng không tính.
 */
public interface DashboardQueryRepository extends Repository<Order, UUID> {

    UUID ANY_STORE = new UUID(0, 0);

    String STORE_ORDER = """
            o.storeId is not null and (:all = true or o.storeId = :storeId)
              and coalesce(o.paymentStatus, '') not in ('AWAITING_PAYMENT', 'ABANDONED')
            """;

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from Order o
            where o.status = com.fashionsystem.fashion_system.entity.OrderStatus.COMPLETED
              and o.createdAt >= :from and o.createdAt < :to and
            """ + STORE_ORDER)
    BigDecimal revenue(@Param("all") boolean all, @Param("storeId") UUID storeId,
                       @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select count(o) from Order o
            where o.status <> com.fashionsystem.fashion_system.entity.OrderStatus.CANCELLED
              and o.createdAt >= :from and o.createdAt < :to and
            """ + STORE_ORDER)
    long orderCount(@Param("all") boolean all, @Param("storeId") UUID storeId,
                    @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select coalesce(sum(i.quantity), 0) from OrderItem i join Order o on o.id = i.orderId
            where o.status = com.fashionsystem.fashion_system.entity.OrderStatus.COMPLETED
              and o.createdAt >= :from and o.createdAt < :to and
            """ + STORE_ORDER)
    long itemsSold(@Param("all") boolean all, @Param("storeId") UUID storeId,
                   @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select count(c) from Customer c
            where c.createdAt >= :from and c.createdAt < :to and (:all = true or c.originStoreId = :storeId)
            """)
    long newCustomers(@Param("all") boolean all, @Param("storeId") UUID storeId,
                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse$BestSeller(
                i.productId, i.productName, max(i.imageUrl), sum(i.quantity), sum(i.total))
            from OrderItem i join Order o on o.id = i.orderId
            where o.status = com.fashionsystem.fashion_system.entity.OrderStatus.COMPLETED
              and o.createdAt >= :from and o.createdAt < :to and
            """ + STORE_ORDER + """
            group by i.productId, i.productName
            order by sum(i.quantity) desc, sum(i.total) desc
            """)
    List<DashboardResponse.BestSeller> bestSellers(@Param("all") boolean all, @Param("storeId") UUID storeId,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to, Pageable pageable);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse$RecentOrder(
                o.id, o.orderCode, o.orderType, o.status, o.totalAmount, o.createdAt,
                coalesce(c.fullName, a.receiverName), s.name)
            from Order o
            join Store s on s.id = o.storeId
            left join Customer c on c.id = o.customerId
            left join OrderAddress a on a.orderId = o.id
            where o.active = true and
            """ + STORE_ORDER + """
            order by o.createdAt desc
            """)
    List<DashboardResponse.RecentOrder> recentOrders(@Param("all") boolean all, @Param("storeId") UUID storeId,
                                                     Pageable pageable);

    /** Sắp hết hàng: cùng ngưỡng với bộ lọc LOW ở trang kho (còn 1..threshold); hết hẳn không tính. */
    @Query("""
            select new com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse$LowStockItem(
                v.id, p.name, v.sku, v.color, v.size, s.id, s.name, b.availableQuantity + b.onlineQuantity)
            from InventoryBalance b
            join ProductVariant v on v.id = b.productVariantId
            join Product p on p.id = v.productId
            join Store s on s.id = b.storeId
            where (:all = true or b.storeId = :storeId) and s.active = true and v.active = true
              and b.availableQuantity + b.onlineQuantity between 1 and :threshold
            order by b.availableQuantity + b.onlineQuantity asc, p.name asc
            """)
    List<DashboardResponse.LowStockItem> lowStock(@Param("all") boolean all, @Param("storeId") UUID storeId,
                                                  @Param("threshold") int threshold, Pageable pageable);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse$RecentReceipt(
                r.id, r.receiptCode, sp.name, s.name, r.totalQuantity, r.totalAmount, r.status, r.createdAt)
            from GoodsReceipt r
            join Store s on s.id = r.storeId
            left join Supplier sp on sp.id = r.supplierId
            where (:all = true or r.storeId = :storeId)
            order by r.createdAt desc
            """)
    List<DashboardResponse.RecentReceipt> recentImports(@Param("all") boolean all, @Param("storeId") UUID storeId,
                                                        Pageable pageable);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse$RecentIssue(
                g.id, g.issueCode, g.issueType, s.name, g.totalQuantity, g.status, g.createdAt)
            from GoodsIssue g
            join Store s on s.id = g.storeId
            where (:all = true or g.storeId = :storeId)
            order by g.createdAt desc
            """)
    List<DashboardResponse.RecentIssue> recentExports(@Param("all") boolean all, @Param("storeId") UUID storeId,
                                                      Pageable pageable);
}
