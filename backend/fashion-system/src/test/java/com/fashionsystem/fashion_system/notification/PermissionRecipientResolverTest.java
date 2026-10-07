package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.realtime.RealtimeAccountType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class PermissionRecipientResolverTest {

    @Test
    void resolvesDistinctUsersAndDefensivelyExcludesTheActor() {
        PermissionRecipientRepository repository = mock(PermissionRecipientRepository.class);
        PermissionRecipientResolver resolver = new PermissionRecipientResolver(repository);
        UUID storeId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        when(repository.findEligibleUserIds("IMPORT_RECEIPT_CONFIRM", storeId, actorId))
                .thenReturn(List.of(approverId, approverId, actorId));

        var result = resolver.resolveUsers(" import_receipt_confirm ", storeId, actorId);

        assertThat(result).singleElement().satisfies(identity -> {
            assertThat(identity.accountType()).isEqualTo(RealtimeAccountType.USER);
            assertThat(identity.accountId()).isEqualTo(approverId);
        });
        verify(repository).findEligibleUserIds("IMPORT_RECEIPT_CONFIRM", storeId, actorId);
    }

    @Test
    void nativeQueryEncodesEffectivePermissionAndIdentityScopeRules() throws Exception {
        Method method = PermissionRecipientRepository.class.getMethod(
                "findEligibleUserIds", String.class, UUID.class, UUID.class);
        String sql = method.getAnnotation(Query.class).value().toUpperCase();

        assertThat(sql)
                .contains("UO.EFFECT = 'DENY'")
                .contains("COUNT(DISTINCT SS.STORE_ID)")
                .contains("U.ACTIVE = TRUE")
                .contains("U.LOCKED = FALSE")
                .contains("U.DELETED_AT IS NULL")
                .contains("ACTIVE_STORE_COUNT = 0")
                .contains("EFFECTIVE_SCOPE = 5")
                .contains("ACTIVE_STORE_COUNT = 1")
                .contains("SS.STORE_ID = :STOREID")
                .contains("EFFECTIVE_SCOPE >= 4");
    }
}
