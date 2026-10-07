package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class AfterCommitNotificationListenerTest {

    @Test
    void listenerRunsOnlyAfterTheBusinessTransactionCommits() throws Exception {
        TransactionalEventListener annotation = AfterCommitNotificationListener.class
                .getMethod("handle", NotificationIntent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void importSubmitResolvesAllPermissionRecipientsInStoreExcludingActor() {
        PermissionRecipientResolver resolver = mock(PermissionRecipientResolver.class);
        NotificationPublisher publisher = mock(NotificationPublisher.class);
        UserRepository users = mock(UserRepository.class);
        AfterCommitNotificationListener listener = new AfterCommitNotificationListener(resolver, publisher, users);
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();
        when(resolver.resolveUsers("IMPORT_RECEIPT_CONFIRM", storeId, actorId))
                .thenReturn(Set.of(RealtimeIdentity.user(approverId)));
        when(users.findById(actorId)).thenReturn(Optional.of(User.builder().id(actorId).fullName("Nhân viên A").build()));
        NotificationIntent intent = new NotificationIntent(
                NotificationIntent.Workflow.IMPORT_RECEIPT,
                NotificationIntent.Action.SUBMITTED,
                UUID.randomUUID(), "PN000123", storeId, actorId, actorId);

        listener.handle(intent);

        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(publisher).publish(request.capture());
        verify(resolver).resolveUsers("IMPORT_RECEIPT_CONFIRM", storeId, actorId);
        assertThat(request.getValue().recipientIdentities()).containsExactly(RealtimeIdentity.user(approverId));
        assertThat(request.getValue().message()).contains("Nhân viên A", "PN000123");
        assertThat(request.getValue().actionUrl()).isEqualTo("/admin/imports");
    }

    @Test
    void exportSubmitUsesExportConfirmPermission() {
        PermissionRecipientResolver resolver = mock(PermissionRecipientResolver.class);
        NotificationPublisher publisher = mock(NotificationPublisher.class);
        UserRepository users = mock(UserRepository.class);
        AfterCommitNotificationListener listener = new AfterCommitNotificationListener(resolver, publisher, users);
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        when(resolver.resolveUsers("EXPORT_RECEIPT_CONFIRM", storeId, actorId))
                .thenReturn(Set.of(RealtimeIdentity.user(UUID.randomUUID())));

        listener.handle(new NotificationIntent(
                NotificationIntent.Workflow.EXPORT_RECEIPT,
                NotificationIntent.Action.SUBMITTED,
                UUID.randomUUID(), "PX000123", storeId, actorId, actorId));

        verify(resolver).resolveUsers("EXPORT_RECEIPT_CONFIRM", storeId, actorId);
        verify(publisher).publish(any());
    }

    @Test
    void approveNotifiesOnlyCreatorAndSuppressesSelfNotification() {
        PermissionRecipientResolver resolver = mock(PermissionRecipientResolver.class);
        NotificationPublisher publisher = mock(NotificationPublisher.class);
        UserRepository users = mock(UserRepository.class);
        AfterCommitNotificationListener listener = new AfterCommitNotificationListener(resolver, publisher, users);
        UUID actorId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        NotificationIntent approved = new NotificationIntent(
                NotificationIntent.Workflow.EXPORT_RECEIPT,
                NotificationIntent.Action.APPROVED,
                UUID.randomUUID(), "PX000123", UUID.randomUUID(), actorId, creatorId);

        listener.handle(approved);

        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(publisher).publish(request.capture());
        assertThat(request.getValue().recipientIdentities()).containsExactly(RealtimeIdentity.user(creatorId));
        verify(resolver, never()).resolveUsers(any(), any(), any());

        listener.handle(new NotificationIntent(
                NotificationIntent.Workflow.EXPORT_RECEIPT,
                NotificationIntent.Action.APPROVED,
                UUID.randomUUID(), "PX000124", UUID.randomUUID(), actorId, actorId));
        verify(publisher, org.mockito.Mockito.times(1)).publish(any());
    }

    @Test
    void resolverOrPublisherFailureIsContainedAfterBusinessCommit() {
        PermissionRecipientResolver resolver = mock(PermissionRecipientResolver.class);
        NotificationPublisher publisher = mock(NotificationPublisher.class);
        UserRepository users = mock(UserRepository.class);
        AfterCommitNotificationListener listener = new AfterCommitNotificationListener(resolver, publisher, users);
        NotificationIntent intent = new NotificationIntent(
                NotificationIntent.Workflow.IMPORT_RECEIPT,
                NotificationIntent.Action.SUBMITTED,
                UUID.randomUUID(), "PN000123", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(resolver.resolveUsers(any(), any(), any())).thenThrow(new IllegalStateException("db unavailable"));

        assertThatCode(() -> listener.handle(intent)).doesNotThrowAnyException();
    }
}
