package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerProfile;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho CustomerProfile.
 */
public interface CustomerProfileRepository extends BaseRepository<CustomerProfile, UUID> {
}
