package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.dto.order.OrderListRow;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Truy vấn chỉ đọc cho trang quản trị đơn hàng. Đơn trả qua cổng chưa thanh toán (AWAITING_PAYMENT, ABANDONED)
 * không hiện cho cửa hàng. Bộ lọc tùy chọn dùng cờ boolean thay vì
 * {@code :param is null} để PostgreSQL luôn suy được kiểu tham số.
 */
public interface OrderAdminQueryRepository extends Repository<Order, UUID> {

    /** Giá trị giữ chỗ khi cờ lọc tắt; vẫn phải nằm trong miền timestamp của PostgreSQL. */
    LocalDateTime UNUSED_DATE = LocalDateTime.of(2000, 1, 1, 0, 0);

    /** Bộ lọc danh sách; chuỗi rỗng / null nghĩa là không lọc. */
    record Filter(UUID storeId, OrderType type, boolean active, OrderStatus status, LocalDateTime from,
                  LocalDateTime to, String keyword, UUID handlerId, String paymentMethod) {
    }

    default Page<OrderListRow> search(Filter f, Pageable pageable) {
        String keyword = f.keyword() == null ? "" : f.keyword().trim().toLowerCase();
        return searchFiltered(f.storeId(), f.type(), f.active(),
                f.status() != null, f.status() == null ? OrderStatus.PENDING : f.status(),
                f.from() != null, f.from() == null ? UNUSED_DATE : f.from(),
                f.to() != null, f.to() == null ? UNUSED_DATE : f.to(),
                keyword, "%" + keyword + "%",
                f.handlerId() != null, f.handlerId() == null ? f.storeId() : f.handlerId(),
                f.paymentMethod() == null ? "" : f.paymentMethod().trim().toUpperCase(), pageable);
    }

    String WHERE = """
            where o.storeId = :storeId and o.orderType = :type and o.active = :active
              and coalesce(o.paymentStatus, '') not in ('AWAITING_PAYMENT', 'ABANDONED')
              and (:filterStatus = false or o.status = :status)
              and (:filterFrom = false or o.createdAt >= :fromDate)
              and (:filterTo = false or o.createdAt < :toDate)
              and (:filterHandler = false or o.handlerUserId = :handlerId)
              and (:paymentMethod = '' or o.paymentMethod = :paymentMethod)
              and (:keyword = '' or lower(o.orderCode) like :pattern
                   or lower(coalesce(c.fullName, '')) like :pattern or coalesce(c.phone, '') like :pattern
                   or lower(coalesce(a.receiverName, '')) like :pattern or coalesce(a.receiverPhone, '') like :pattern)
            """;

    @Query(value = """
            select new com.fashionsystem.fashion_system.dto.order.OrderListRow(
                o.id, o.orderCode, o.orderType, o.status, o.createdAt, o.customerId, c.fullName, c.phone,
                a.receiverName, a.receiverPhone, o.handlerUserId, u.fullName, o.paymentMethod, o.paymentStatus,
                o.totalAmount, (select coalesce(sum(i.quantity), 0) from OrderItem i where i.orderId = o.id),
                o.active, o.parentOrderId)
            from Order o
            left join Customer c on c.id = o.customerId
            left join OrderAddress a on a.orderId = o.id
            left join User u on u.id = o.handlerUserId
            """ + WHERE,
            countQuery = """
            select count(o) from Order o
            left join Customer c on c.id = o.customerId
            left join OrderAddress a on a.orderId = o.id
            """ + WHERE)
    Page<OrderListRow> searchFiltered(
            @Param("storeId") UUID storeId, @Param("type") OrderType type, @Param("active") boolean active,
            @Param("filterStatus") boolean filterStatus, @Param("status") OrderStatus status,
            @Param("filterFrom") boolean filterFrom, @Param("fromDate") LocalDateTime from,
            @Param("filterTo") boolean filterTo, @Param("toDate") LocalDateTime to,
            @Param("keyword") String keyword, @Param("pattern") String pattern,
            @Param("filterHandler") boolean filterHandler, @Param("handlerId") UUID handlerId,
            @Param("paymentMethod") String paymentMethod, Pageable pageable);

    @Query("""
            select o.status, count(o) from Order o
            where o.storeId = :storeId and o.orderType = :type and o.active = true
              and coalesce(o.paymentStatus, '') not in ('AWAITING_PAYMENT', 'ABANDONED')
            group by o.status
            """)
    List<Object[]> countByStatus(@Param("storeId") UUID storeId, @Param("type") OrderType type);

    @Query("select count(o) from Order o where o.storeId = :storeId and o.orderType = :type and o.active = false"
            + " and coalesce(o.paymentStatus, '') not in ('AWAITING_PAYMENT', 'ABANDONED')")
    long countHidden(@Param("storeId") UUID storeId, @Param("type") OrderType type);

    @Query("""
            select count(o) from Order o
            where o.storeId = :storeId and o.orderType = :type and o.createdAt >= :from
              and o.status <> com.fashionsystem.fashion_system.entity.OrderStatus.CANCELLED
              and coalesce(o.paymentStatus, '') not in ('AWAITING_PAYMENT', 'ABANDONED')
            """)
    long countSince(@Param("storeId") UUID storeId, @Param("type") OrderType type, @Param("from") LocalDateTime from);

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from Order o
            where o.storeId = :storeId and o.orderType = :type and o.createdAt >= :from
              and o.status = com.fashionsystem.fashion_system.entity.OrderStatus.COMPLETED
            """)
    BigDecimal revenueSince(@Param("storeId") UUID storeId, @Param("type") OrderType type,
                            @Param("from") LocalDateTime from);

    /** Nhân viên từng xử lý đơn của cửa hàng: nguồn cho bộ lọc "nhân viên xử lý". */
    @Query("""
            select distinct u.id, u.fullName, u.username from Order o join User u on u.id = o.handlerUserId
            where o.storeId = :storeId order by u.fullName
            """)
    List<Object[]> handlers(@Param("storeId") UUID storeId);
}
