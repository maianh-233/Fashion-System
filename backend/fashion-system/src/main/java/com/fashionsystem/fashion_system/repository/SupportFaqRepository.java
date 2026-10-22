package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.SupportFaq;
import java.util.List;
import java.util.UUID;

public interface SupportFaqRepository extends BaseRepository<SupportFaq, UUID> {
    List<SupportFaq> findAllByOrderBySortOrderAsc();
}
