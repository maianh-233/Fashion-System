package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

class BulkAuditRecorderTest {
    @Test
    void nativeInventoryInitializationRecordsOnlyTheInsertedCompositeRow() {
        var factory = mock(EntityManagerFactory.class);
        TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
        var collector = new AuditChangeCollector();
        var storeId = java.util.UUID.randomUUID();
        var variantId = java.util.UUID.randomUUID();
        var repository = mock(com.fashionsystem.fashion_system.repository.InventoryBalanceRepository.class, CALLS_REAL_METHODS);
        var row = com.fashionsystem.fashion_system.entity.InventoryBalance.builder()
                .storeId(storeId).productVariantId(variantId).availableQuantity(0)
                .reservedQuantity(0).damagedQuantity(0).build();
        when(repository.initializeNative(storeId, variantId)).thenReturn(1, 0);
        when(repository.findForUpdate(storeId, variantId)).thenReturn(java.util.Optional.of(row));
        try (var scope = AuditCaptureScope.open(collector)) {
            assertThat(repository.initialize(storeId, variantId)).isEqualTo(1);
            assertThat(repository.initialize(storeId, variantId)).isZero();
            assertThat(collector.finish()).hasSize(1);
            var change = collector.finish().getFirst();
            assertThat(change.table()).isEqualTo("inventory_balances");
            assertThat(change.rowId()).contains(storeId.toString(), variantId.toString());
            assertThat(change.operation()).isEqualTo(AuditOperation.INSERT);
        } finally { TransactionSynchronizationManager.unbindResource(factory); }
    }

    @Test
    void recordsEveryAffectedIdAndCoalescesWithHibernateUpdates() {
        var factory = mock(EntityManagerFactory.class);
        TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
        var collector = new AuditChangeCollector();
        var mapper = new ObjectMapper();
        try (var scope = AuditCaptureScope.open(collector)) {
            for (String id : new String[]{"1", "2", "3"}) {
                BulkAuditRecorder.record("products", id, AuditOperation.UPDATE,
                        mapper.readTree("{\"status\":\"ACTIVE\"}"), mapper.readTree("{\"status\":\"ARCHIVE\"}"));
            }
            BulkAuditRecorder.record("inventory_balances", "composite-id", AuditOperation.INSERT,
                    null, mapper.readTree("{\"availableQuantity\":0}"));
            collector.recordUpdate("inventory_balances", "composite-id", mapper.readTree("{\"availableQuantity\":0}"),
                    mapper.readTree("{\"availableQuantity\":10}"));
            assertThat(collector.finish()).extracting(AuditChange::rowId).containsExactly("1", "2", "3", "composite-id");
            assertThat(collector.finish().getLast().operation()).isEqualTo(AuditOperation.INSERT);
            assertThat(collector.finish().getLast().newValues().get("availableQuantity").asInt()).isEqualTo(10);
        } finally { TransactionSynchronizationManager.unbindResource(factory); }
    }

    @Test
    void refusesSilentLossOutsideAnAuditScope() {
        assertThatThrownBy(() -> BulkAuditRecorder.record("products", "1", AuditOperation.DELETE,
                new ObjectMapper().createObjectNode(), null)).isInstanceOf(IllegalStateException.class);
    }
}
