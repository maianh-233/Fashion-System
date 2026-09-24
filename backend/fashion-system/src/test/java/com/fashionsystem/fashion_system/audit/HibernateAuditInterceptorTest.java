package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.entity.AuditOutbox;
import com.fashionsystem.fashion_system.entity.AuthAuditLog;
import com.fashionsystem.fashion_system.entity.PasswordResetOtp;
import com.fashionsystem.fashion_system.entity.PaymentWebhookLog;
import com.fashionsystem.fashion_system.entity.RevokedToken;
import com.fashionsystem.fashion_system.entity.RolePermission;
import com.fashionsystem.fashion_system.entity.RolePermissionId;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.UserToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

class HibernateAuditInterceptorTest {
    private final EntityManagerFactory factory = mock(EntityManagerFactory.class);
    private final HibernateAuditInterceptor interceptor = new HibernateAuditInterceptor(new ObjectMapper());

    @AfterEach
    void clearResource() {
        if (TransactionSynchronizationManager.hasResource(factory)) {
            TransactionSynchronizationManager.unbindResource(factory);
        }
    }

    @Test
    void capturesInsertUpdateAndDeleteUsingTableAndIdentifier() {
        TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
        AuditChangeCollector collector = new AuditChangeCollector();
        UUID id = UUID.randomUUID();
        try (AuditCaptureScope ignored = AuditCaptureScope.open(collector)) {
            interceptor.onPersist(new User(), id, new Object[]{"A"}, new String[]{"fullName"}, null);
            interceptor.onFlushDirty(new User(), id, new Object[]{"B"}, new Object[]{"A"}, new String[]{"fullName"}, null);
        }
        AuditChange change = collector.finish().getFirst();
        assertThat(change.table()).isEqualTo("users");
        assertThat(change.rowId()).isEqualTo(id.toString());
        assertThat(change.operation()).isEqualTo(AuditOperation.INSERT);
        assertThat(change.newValues().get("fullName").asText()).isEqualTo("B");
    }

    @Test
    void serializesCompositeIdentifierAndCapturesDelete() {
        TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
        AuditChangeCollector collector = new AuditChangeCollector();
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        try (AuditCaptureScope ignored = AuditCaptureScope.open(collector)) {
            interceptor.onRemove(new RolePermission(), new RolePermissionId(roleId, permissionId),
                    new Object[]{"ALL"}, new String[]{"scope"}, null);
        }
        AuditChange change = collector.finish().getFirst();
        assertThat(change.table()).isEqualTo("role_permissions");
        JsonNodeAssertions.assertCompositeId(change.rowId(), roleId, permissionId);
        assertThat(change.operation()).isEqualTo(AuditOperation.DELETE);
        assertThat(change.oldValues().get("scope").asText()).isEqualTo("ALL");
    }

    @Test
    void excludesAuditAndAuthenticationEntities() {
        TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
        AuditChangeCollector collector = new AuditChangeCollector();
        try (AuditCaptureScope ignored = AuditCaptureScope.open(collector)) {
            for (Object entity : List.of(new AuditLog(), new AuthAuditLog(), new AuditOutbox(),
                    new RevokedToken(), new UserToken(), new PasswordResetOtp(), new PaymentWebhookLog())) {
                interceptor.onPersist(entity, UUID.randomUUID(), new Object[]{"x"}, new String[]{"action"}, null);
            }
        }
        assertThat(collector.finish()).isEmpty();
    }

    @Test
    void capturesTemporalPropertyValuesFromHibernateState() {
        TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
        AuditChangeCollector collector = new AuditChangeCollector();
        try (AuditCaptureScope ignored = AuditCaptureScope.open(collector)) {
            interceptor.onPersist(new User(), UUID.randomUUID(), new Object[]{LocalDateTime.of(2026, 9, 24, 10, 30)},
                    new String[]{"createdAt"}, null);
        }
        assertThat(collector.finish().getFirst().newValues().has("createdAt")).isTrue();
    }

    @Test
    void restoresPreviousCollectorForNestedScopeOnSameResource() {
        TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
        AuditChangeCollector outer = new AuditChangeCollector();
        AuditChangeCollector inner = new AuditChangeCollector();
        try (AuditCaptureScope ignored = AuditCaptureScope.open(outer)) {
            try (AuditCaptureScope nested = AuditCaptureScope.open(inner)) {
                assertThat(AuditCaptureScope.current()).contains(inner);
            }
            assertThat(AuditCaptureScope.current()).contains(outer);
        }
        assertThat(AuditCaptureScope.current()).isEmpty();
    }

    @Test
    void refusesOutOfOrderCloseWithoutLosingNestedCollector() {
        TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
        AuditChangeCollector outer = new AuditChangeCollector();
        AuditChangeCollector inner = new AuditChangeCollector();
        AuditCaptureScope outerScope = AuditCaptureScope.open(outer);
        AuditCaptureScope innerScope = AuditCaptureScope.open(inner);
        try {
            assertThatThrownBy(outerScope::close).isInstanceOf(IllegalStateException.class);
            assertThat(AuditCaptureScope.current()).contains(inner);
        } finally {
            innerScope.close();
            outerScope.close();
        }
        assertThat(AuditCaptureScope.current()).isEmpty();
    }

    @Test
    void restoresOuterCollectorAfterSuspendedInnerResource() {
        EntityManagerHolder outerHolder = new EntityManagerHolder(mock(EntityManager.class));
        TransactionSynchronizationManager.bindResource(factory, outerHolder);
        AuditChangeCollector outer = new AuditChangeCollector();
        AuditChangeCollector inner = new AuditChangeCollector();
        try (AuditCaptureScope ignored = AuditCaptureScope.open(outer)) {
            assertThat(AuditCaptureScope.current()).contains(outer);
            TransactionSynchronizationManager.unbindResource(factory);
            TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(mock(EntityManager.class)));
            try (AuditCaptureScope innerScope = AuditCaptureScope.open(inner)) {
                assertThat(AuditCaptureScope.current()).contains(inner);
            }
            TransactionSynchronizationManager.unbindResource(factory);
            TransactionSynchronizationManager.bindResource(factory, outerHolder);
            assertThat(AuditCaptureScope.current()).contains(outer);
        }
        assertThat(AuditCaptureScope.current()).isEmpty();
    }

    private static final class JsonNodeAssertions {
        static void assertCompositeId(String rowId, UUID roleId, UUID permissionId) {
            var id = new ObjectMapper().readTree(rowId);
            assertThat(id.get("roleId").asText()).isEqualTo(roleId.toString());
            assertThat(id.get("permissionId").asText()).isEqualTo(permissionId.toString());
        }
    }
}
