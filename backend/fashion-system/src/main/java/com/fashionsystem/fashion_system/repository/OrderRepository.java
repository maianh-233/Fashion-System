package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Order.
 */
public interface OrderRepository extends BaseRepository<Order, UUID> {
    boolean existsByOrderCode(String orderCode);

    java.util.List<Order> findAllByParentOrderId(UUID parentOrderId);

    /** Đơn của khách (đơn thường hoặc đơn tổng) đang chờ thanh toán qua cổng và đã quá hạn. */
    @Query("""
            select o.id from Order o
            where o.parentOrderId is null and o.paymentStatus = 'AWAITING_PAYMENT' and o.paymentDueAt < :now
            order by o.paymentDueAt
            """)
    java.util.List<UUID> findOverduePaymentOrderIds(@Param("now") java.time.LocalDateTime now);

    java.util.Optional<Order> findByCustomerIdAndClientRequestId(UUID customerId, String clientRequestId);

    org.springframework.data.domain.Page<Order> findAllByCustomerIdAndParentOrderIdIsNullAndStatus(
            UUID customerId, OrderStatus status, Pageable pageable);

    org.springframework.data.domain.Page<Order> findAllByCustomerIdAndParentOrderIdIsNull(
            UUID customerId, org.springframework.data.domain.Pageable pageable);
    boolean existsByOrderCodeAndIdNot(String orderCode, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select o from Order o
            where (:keyword = '' or lower(o.orderCode) like lower(concat('%', :keyword, '%')))
              and (:customerId is null or o.customerId = :customerId)
              and (:storeId is null or o.storeId = :storeId)
              and (:orderType is null or o.orderType = :orderType)
              and (:status is null or o.status = :status)
              and (:paymentStatus = '' or upper(coalesce(o.paymentStatus, '')) = :paymentStatus)
              and (:fromDate is null or o.createdAt >= :fromDate)
              and (:toDate is null or o.createdAt <= :toDate)
            """)
    Page<Order> search(
            @Param("keyword") String keyword, @Param("customerId") UUID customerId,
            @Param("storeId") UUID storeId, @Param("orderType") OrderType orderType,
            @Param("status") OrderStatus status, @Param("paymentStatus") String paymentStatus,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate, Pageable pageable);
}
