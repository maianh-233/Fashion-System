package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ReturnRequestItem;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRequestItemRepository extends BaseRepository<ReturnRequestItem, UUID> {
    List<ReturnRequestItem> findAllByRequestIdIn(Collection<UUID> requestIds);

    /** Số lượng của dòng đơn đã nằm trong các yêu cầu còn hiệu lực (chưa bị từ chối / hủy). */
    @Query("""
            select coalesce(sum(i.quantity), 0) from ReturnRequestItem i, ReturnRequest r
            where r.id = i.requestId and i.orderItemId = :orderItemId and r.status not in ('REJECTED', 'CANCELLED')""")
    long sumActiveQuantity(@Param("orderItemId") UUID orderItemId);
}
