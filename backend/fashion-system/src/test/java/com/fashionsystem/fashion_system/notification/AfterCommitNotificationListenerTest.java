package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class AfterCommitNotificationListenerTest {

    private final PermissionRecipientResolver resolver = mock(PermissionRecipientResolver.class);
    private final NotificationPublisher publisher = mock(NotificationPublisher.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AfterCommitNotificationListener listener = new AfterCommitNotificationListener(resolver, publisher, users);
    private final UUID actorId = UUID.randomUUID();
    private final UUID creatorId = UUID.randomUUID();
    private final UUID approverId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final UUID receiptId = UUID.randomUUID();

    @BeforeEach
    void actorHasName() {
        when(users.findById(actorId)).thenReturn(Optional.of(User.builder().id(actorId).fullName("Nhân viên A").build()));
    }

    private NotificationIntent intent(NotificationIntent.Workflow workflow, NotificationIntent.Action action,
            UUID creator, UUID approver, String previousStatus) {
        return new NotificationIntent(workflow, action, receiptId, "PN000123", storeId, actorId, creator, approver, previousStatus);
    }

    private NotificationIntent importIntent(NotificationIntent.Action action, UUID creator, UUID approver, String previousStatus) {
        return intent(NotificationIntent.Workflow.IMPORT_RECEIPT, action, creator, approver, previousStatus);
    }

    private List<NotificationRequest> published(int count) {
        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(publisher, times(count)).publish(request.capture());
        return request.getAllValues();
    }

    private static NotificationRequest to(List<NotificationRequest> requests, UUID userId) {
        return requests.stream()
                .filter(request -> request.recipientIdentities().contains(RealtimeIdentity.user(userId)))
                .findFirst().orElseThrow();
    }

    @Test
    void listenerRunsOnlyAfterTheBusinessTransactionCommits() throws Exception {
        TransactionalEventListener annotation = AfterCommitNotificationListener.class
                .getMethod("handle", NotificationIntent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void submitByCreatorNotifiesStoreApproversWithDeepLink() {
        when(resolver.resolveUsers("IMPORT_RECEIPT_CONFIRM", storeId, actorId))
                .thenReturn(Set.of(RealtimeIdentity.user(approverId)));

        listener.handle(importIntent(NotificationIntent.Action.SUBMITTED, actorId, null, "DRAFT"));

        NotificationRequest request = published(1).get(0);
        assertThat(request.recipientIdentities()).containsExactly(RealtimeIdentity.user(approverId));
        assertThat(request.type()).isEqualTo(NotificationType.PURCHASE_RECEIPT_SUBMITTED);
        assertThat(request.message()).contains("Nhân viên A", "PN000123");
        assertThat(request.actionUrl()).isEqualTo("/admin/imports?storeId=" + storeId + "&receipt=" + receiptId);
    }

    @Test
    void exportSubmitUsesExportConfirmPermission() {
        when(resolver.resolveUsers("EXPORT_RECEIPT_CONFIRM", storeId, actorId))
                .thenReturn(Set.of(RealtimeIdentity.user(approverId)));

        listener.handle(intent(NotificationIntent.Workflow.EXPORT_RECEIPT, NotificationIntent.Action.SUBMITTED,
                actorId, null, "DRAFT"));

        NotificationRequest request = published(1).get(0);
        assertThat(request.type()).isEqualTo(NotificationType.STOCK_EXPORT_SUBMITTED);
        assertThat(request.actionUrl()).startsWith("/admin/exports?storeId=");
    }

    @Test
    void submitBySomeoneElseAlsoTellsCreatorOnceWithCreatorWording() {
        when(resolver.resolveUsers("IMPORT_RECEIPT_CONFIRM", storeId, actorId))
                .thenReturn(Set.of(RealtimeIdentity.user(approverId), RealtimeIdentity.user(creatorId)));

        listener.handle(importIntent(NotificationIntent.Action.SUBMITTED, creatorId, null, "DRAFT"));

        List<NotificationRequest> requests = published(2);
        assertThat(to(requests, approverId).recipientIdentities()).containsExactly(RealtimeIdentity.user(approverId));
        assertThat(to(requests, creatorId).recipientIdentities()).containsExactly(RealtimeIdentity.user(creatorId));
        assertThat(to(requests, creatorId).message()).contains("của bạn");
    }

    @Test
    void approveNotifiesCreatorAndSuppressesSelfNotification() {
        listener.handle(importIntent(NotificationIntent.Action.APPROVED, creatorId, actorId, "PENDING_CONFIRMATION"));

        NotificationRequest request = published(1).get(0);
        assertThat(request.recipientIdentities()).containsExactly(RealtimeIdentity.user(creatorId));
        assertThat(request.type()).isEqualTo(NotificationType.PURCHASE_RECEIPT_APPROVED);
        assertThat(request.message()).contains("Nhân viên A");
        verify(resolver, never()).resolveUsers(any(), any(), any());

        listener.handle(importIntent(NotificationIntent.Action.APPROVED, actorId, actorId, "PENDING_CONFIRMATION"));
        verify(publisher, times(1)).publish(any());
    }

    @Test
    void completeNotifiesCreatorAndTheApproverWhoConfirmed() {
        listener.handle(importIntent(NotificationIntent.Action.COMPLETED, creatorId, approverId, "CONFIRMED"));

        List<NotificationRequest> requests = published(2);
        assertThat(requests).allSatisfy(request ->
                assertThat(request.type()).isEqualTo(NotificationType.PURCHASE_RECEIPT_COMPLETED));
        assertThat(to(requests, creatorId).message()).contains("của bạn", "Tồn kho");
        assertThat(to(requests, approverId).message()).contains("bạn đã duyệt");
        verify(resolver, never()).resolveUsers(any(), any(), any());
    }

    @Test
    void completeByTheApproverOnlyTellsCreator() {
        listener.handle(importIntent(NotificationIntent.Action.COMPLETED, creatorId, actorId, "CONFIRMED"));

        assertThat(published(1).get(0).recipientIdentities()).containsExactly(RealtimeIdentity.user(creatorId));
    }

    @Test
    void cancellingPendingReceiptTellsWaitingApproversAndCreator() {
        when(resolver.resolveUsers("EXPORT_RECEIPT_CONFIRM", storeId, actorId))
                .thenReturn(Set.of(RealtimeIdentity.user(approverId)));

        listener.handle(intent(NotificationIntent.Workflow.EXPORT_RECEIPT, NotificationIntent.Action.CANCELLED,
                creatorId, null, "PENDING_CONFIRMATION"));

        List<NotificationRequest> requests = published(2);
        assertThat(requests).allSatisfy(request ->
                assertThat(request.type()).isEqualTo(NotificationType.STOCK_EXPORT_CANCELLED));
        assertThat(to(requests, approverId).message()).contains("không cần duyệt");
        assertThat(to(requests, creatorId).message()).contains("của bạn");
    }

    @Test
    void creatorCancellingOwnDraftNotifiesNobody() {
        listener.handle(importIntent(NotificationIntent.Action.CANCELLED, actorId, null, "DRAFT"));

        verify(publisher, never()).publish(any());
        verify(resolver, never()).resolveUsers(any(), any(), any());
    }

    @Test
    void resolverOrPublisherFailureIsContainedAfterBusinessCommit() {
        when(resolver.resolveUsers(any(), any(), any())).thenThrow(new IllegalStateException("db unavailable"));

        assertThatCode(() -> listener.handle(importIntent(NotificationIntent.Action.SUBMITTED, creatorId, null, "DRAFT")))
                .doesNotThrowAnyException();
    }
}
