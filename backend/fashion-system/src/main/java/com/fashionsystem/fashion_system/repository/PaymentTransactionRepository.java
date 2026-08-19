package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PaymentTransaction;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PaymentTransaction.
 */
public interface PaymentTransactionRepository extends BaseRepository<PaymentTransaction, UUID> {
}
