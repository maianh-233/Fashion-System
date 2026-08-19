package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.User;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho User.
 */
public interface UserRepository extends BaseRepository<User, UUID> {
}
