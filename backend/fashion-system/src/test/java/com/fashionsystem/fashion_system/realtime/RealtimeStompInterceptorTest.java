package com.fashionsystem.fashion_system.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.security.AccessTokenAuthenticationService;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

class RealtimeStompInterceptorTest {

    private final AccessTokenAuthenticationService authenticationService =
            mock(AccessTokenAuthenticationService.class);
    private RealtimeStompInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RealtimeStompInterceptor(authenticationService);
    }

    @Test
    void authenticatesConnectFromBearerHeaderAndSetsCanonicalPrincipal() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "employee"), null, List.of());
        when(authenticationService.authenticate("valid-token")).thenReturn(Optional.of(authentication));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class));

        assertThat(accessor.getUser()).isEqualTo(authentication);
        assertThat(accessor.getUser().getName()).isEqualTo("USER:" + userId);
    }

    @Test
    void rejectsInvalidConnect() {
        when(authenticationService.authenticate("bad-token")).thenReturn(Optional.empty());
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer bad-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(
                        message, mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void onlyAllowsTheSharedPrivateSubscriptionAndRejectsClientSend() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID(), "employee"), null, List.of());

        assertThat(interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/user/queue/events", authentication),
                mock(org.springframework.messaging.MessageChannel.class))).isNotNull();
        assertThatThrownBy(() -> interceptor.preSend(
                        frame(StompCommand.SUBSCRIBE, "/topic/events", authentication),
                        mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> interceptor.preSend(
                        frame(StompCommand.SEND, "/app/chat", authentication),
                        mock(org.springframework.messaging.MessageChannel.class)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<byte[]> frame(StompCommand command, String destination, Authentication authentication) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(authentication);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
