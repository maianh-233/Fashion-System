package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.AuditTransactionConfig;
import com.fashionsystem.fashion_system.entity.AuditOutbox;
import com.fashionsystem.fashion_system.repository.AuditOutboxRepository;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.doAnswer;

class BusinessAuditAspectIntegrationTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void start() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthenticatedUser(UUID.randomUUID(), "alice"), null, List.of()));
        context = new AnnotationConfigApplicationContext(Fixture.class);
    }

    @AfterEach
    void close() { context.close(); SecurityContextHolder.clearContext(); }

    @Test
    void committedMultiTableCallWritesOneEventAfterFlush() {
        context.getBean(OuterService.class).changeUserAndRole();
        List<AuditOutbox> rows = context.getBean(FixtureStore.class).rows;
        assertThat(rows).hasSize(1);
        AuditEvent event = new ObjectMapper().treeToValue(rows.getFirst().getPayload(), AuditEvent.class);
        assertThat(event.rowCount()).isEqualTo(2);
        assertThat(event.action()).isEqualTo("EMPLOYEE_CHANGE_USER_AND_ROLE");
    }

    @Test
    void flushCapturesPendingChangeBeforeOutboxIsBuilt() {
        context.getBean(OuterService.class).changeOnFlush();
        AuditEvent event = new ObjectMapper().treeToValue(
                context.getBean(FixtureStore.class).rows.getFirst().getPayload(), AuditEvent.class);
        assertThat(event.rowCount()).isEqualTo(1);
        assertThat(event.changes().getFirst().table()).isEqualTo("users");
    }

    @Test
    void failureDoesNotWriteOutbox() {
        assertThatThrownBy(() -> context.getBean(OuterService.class).changeThenFail())
                .isInstanceOf(IllegalStateException.class);
        assertThat(context.getBean(FixtureStore.class).rows).isEmpty();
    }

    @Test
    void nestedRequiredCallSharesOuterEvent() {
        context.getBean(OuterService.class).outerWithRequiredChild();
        assertThat(context.getBean(FixtureStore.class).rows).hasSize(1);
        AuditEvent event = new ObjectMapper().treeToValue(
                context.getBean(FixtureStore.class).rows.getFirst().getPayload(), AuditEvent.class);
        assertThat(event.rowCount()).isEqualTo(2);
    }

    @Test
    void requiresNewChildWritesSeparateEvent() {
        context.getBean(OuterService.class).outerWithRequiresNewChild();
        assertThat(context.getBean(FixtureStore.class).rows).hasSize(2);
    }

    @Test
    void auditedCallWithoutTransactionIsRejected() {
        assertThatThrownBy(() -> context.getBean(OuterService.class).withoutTransaction())
                .isInstanceOf(IllegalStateException.class);
    }

    @Configuration
    @Import(AuditTransactionConfig.class)
    static class Fixture {
        @Bean EntityManagerFactory entityManagerFactory() { return mock(EntityManagerFactory.class); }
        @Bean EntityManager entityManager(FixtureStore store) {
            EntityManager em = mock(EntityManager.class);
            doAnswer(invocation -> {
                for (String[] change : store.pending) change(change[0], change[1]);
                store.pending.clear();
                return null;
            }).when(em).flush();
            return em;
        }
        @Bean FixtureStore fixtureStore() { return new FixtureStore(); }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory emf) { return new FixtureTransactionManager(emf); }
        @Bean AuditOutboxRepository auditOutboxRepository(FixtureStore store) {
            AuditOutboxRepository repo = mock(AuditOutboxRepository.class);
            when(repo.save(org.mockito.ArgumentMatchers.any(AuditOutbox.class))).thenAnswer(invocation -> {
                AuditOutbox row = invocation.getArgument(0);
                store.rows.add(row);
                return row;
            });
            return repo;
        }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean BusinessAuditContextFactory contextFactory() { return new BusinessAuditContextFactory(); }
        @Bean BusinessAuditAspect businessAuditAspect(EntityManager em, AuditOutboxRepository repo,
                ObjectMapper mapper, BusinessAuditContextFactory factory) {
            return new BusinessAuditAspect(em, repo, mapper, factory);
        }
        @Bean OuterService outerService(ChildService child, FixtureStore store) { return new OuterService(child, store); }
        @Bean ChildService childService() { return new ChildService(); }
    }

    static class FixtureStore {
        final List<AuditOutbox> rows = new ArrayList<>();
        final List<String[]> pending = new ArrayList<>();
    }

    static class FixtureTransactionManager implements PlatformTransactionManager {
        private final EntityManagerFactory emf;
        private final java.util.Deque<EntityManagerHolder> suspended = new java.util.ArrayDeque<>();
        FixtureTransactionManager(EntityManagerFactory emf) { this.emf = emf; }
        @Override public TransactionStatus getTransaction(TransactionDefinition definition) {
            boolean fresh = !TransactionSynchronizationManager.isActualTransactionActive()
                    || definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            if (fresh) {
                if (TransactionSynchronizationManager.hasResource(emf))
                    suspended.push((EntityManagerHolder) TransactionSynchronizationManager.unbindResource(emf));
                TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(mock(EntityManager.class)));
                TransactionSynchronizationManager.setActualTransactionActive(true);
            }
            return new SimpleTransactionStatus(fresh);
        }
        @Override public void commit(TransactionStatus status) { finish(status); }
        @Override public void rollback(TransactionStatus status) { finish(status); }
        private void finish(TransactionStatus status) {
            if (!status.isNewTransaction()) return;
            TransactionSynchronizationManager.unbindResource(emf);
            if (suspended.isEmpty()) TransactionSynchronizationManager.setActualTransactionActive(false);
            else TransactionSynchronizationManager.bindResource(emf, suspended.pop());
        }
    }

    @BusinessAudit("EMPLOYEE")
    static class OuterService {
        private final ChildService child;
        private final FixtureStore store;
        OuterService(ChildService child, FixtureStore store) { this.child = child; this.store = store; }
        @Transactional public void changeUserAndRole() { change("users", "1"); change("roles", "2"); }
        @Transactional public void changeOnFlush() { store.pending.add(new String[]{"users", "1"}); }
        @Transactional public void changeThenFail() { change("users", "1"); throw new IllegalStateException("fail"); }
        @Transactional public void outerWithRequiredChild() { change("users", "1"); child.required(); }
        @Transactional public void outerWithRequiresNewChild() { change("users", "1"); child.requiresNew(); }
        public void withoutTransaction() { change("users", "1"); }
    }

    @BusinessAudit("ROLE")
    static class ChildService {
        @Transactional public void required() { change("roles", "2"); }
        @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
        public void requiresNew() { change("roles", "2"); }
    }

    private static void change(String table, String id) {
        AuditCaptureScope.current().orElseThrow().recordInsert(table, id,
                new ObjectMapper().createObjectNode().put("name", table));
    }
}
