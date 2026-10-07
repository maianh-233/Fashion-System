package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Notification;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.NotificationRepository;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class NotificationPersistenceTest {

    @Test
    void entityDeclaresEventRecipientIdempotencyConstraint() {
        Table table = Notification.class.getAnnotation(Table.class);

        assertThat(table.uniqueConstraints()).anySatisfy(constraint ->
                assertThat(constraint.columnNames())
                        .containsExactly("event_id", "recipient_type", "recipient_id"));
    }

    @Test
    void queryIsIdentityScopedNewestFirstAndPageSizeIsBounded() {
        NotificationRepository repository = mock(NotificationRepository.class);
        UUID userId = UUID.randomUUID();
        Notification entity = Notification.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .recipientType("USER")
                .recipientId(userId)
                .channel("NOTIFICATION")
                .type("SYSTEM")
                .title("Title")
                .content("Message")
                .createdAt(LocalDateTime.now())
                .build();
        when(repository.findByRecipientTypeAndRecipientId(
                        org.mockito.ArgumentMatchers.eq("USER"),
                        org.mockito.ArgumentMatchers.eq(userId),
                        org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        NotificationQueryService service = new NotificationQueryService(repository);

        var result = service.history(RealtimeIdentity.user(userId), 0, 500);

        assertThat(result.getContent()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(entity.getId());
            assertThat(item.eventId()).isEqualTo(entity.getEventId());
            assertThat(item.message()).isEqualTo("Message");
        });
        ArgumentCaptor<Pageable> pageable = forClass(Pageable.class);
        verify(repository).findByRecipientTypeAndRecipientId(
                org.mockito.ArgumentMatchers.eq("USER"),
                org.mockito.ArgumentMatchers.eq(userId),
                pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }
}
