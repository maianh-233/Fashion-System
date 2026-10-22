package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OrderChatRoom;
import com.fashionsystem.fashion_system.entity.SupportChatStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Hội thoại hỗ trợ đơn hàng. */
public interface OrderChatRoomRepository extends BaseRepository<OrderChatRoom, UUID> {
    Optional<OrderChatRoom> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from OrderChatRoom r where r.id = :id")
    Optional<OrderChatRoom> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Hội thoại mà cửa hàng phụ trách: đơn của cửa hàng, hoặc đơn tổng có một phần do cửa hàng giao.
     * Chờ nhân viên lâu nhất lên đầu, sau đó là tin nhắn mới nhất.
     */
    @Query(value = """
            select r from OrderChatRoom r
            where r.status in :statuses
              and (exists (select 1 from Order o where o.id = r.orderId and o.storeId = :storeId)
                   or exists (select 1 from Order c where c.parentOrderId = r.orderId and c.storeId = :storeId))
            order by case when r.status = com.fashionsystem.fashion_system.entity.SupportChatStatus.WAITING_STAFF then 0 else 1 end,
                     r.escalatedAt asc, r.lastMessageAt desc
            """,
            countQuery = """
            select count(r) from OrderChatRoom r
            where r.status in :statuses
              and (exists (select 1 from Order o where o.id = r.orderId and o.storeId = :storeId)
                   or exists (select 1 from Order c where c.parentOrderId = r.orderId and c.storeId = :storeId))
            """)
    Page<OrderChatRoom> findForStore(@Param("storeId") UUID storeId,
                                     @Param("statuses") Collection<SupportChatStatus> statuses, Pageable pageable);
}
