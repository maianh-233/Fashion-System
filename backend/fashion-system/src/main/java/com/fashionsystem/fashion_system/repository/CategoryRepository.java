package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Category;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Category.
 */
public interface CategoryRepository extends BaseRepository<Category, UUID> {
}
