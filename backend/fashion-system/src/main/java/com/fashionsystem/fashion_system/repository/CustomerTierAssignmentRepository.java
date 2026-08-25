package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import java.util.List;
import java.util.UUID;

public interface CustomerTierAssignmentRepository extends BaseRepository<CustomerTierAssignment, UUID> {
    List<CustomerTierAssignment> findAllByCustomerId(UUID customerId);
}
