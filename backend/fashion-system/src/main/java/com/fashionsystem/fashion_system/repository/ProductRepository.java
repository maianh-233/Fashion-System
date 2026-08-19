package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Product;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Product.
 */
public interface ProductRepository extends BaseRepository<Product, UUID> {
}
