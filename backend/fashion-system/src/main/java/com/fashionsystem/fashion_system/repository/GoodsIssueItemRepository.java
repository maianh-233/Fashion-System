package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.GoodsIssueItem;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho GoodsIssueItem.
 */
public interface GoodsIssueItemRepository extends BaseRepository<GoodsIssueItem, UUID> {
    Optional<GoodsIssueItem> findByIdAndIssueId(UUID id, UUID issueId);
    List<GoodsIssueItem> findAllByIssueIdOrderByCreatedAtAsc(UUID issueId);

    @Query("select coalesce(sum(i.quantity), 0) from GoodsIssueItem i where i.issueId = :issueId")
    Integer sumQuantity(@Param("issueId") UUID issueId);
}
