package com.fashionsystem.fashion_system.realtime;

import com.fashionsystem.fashion_system.security.AccessTokenAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Authenticates STOMP CONNECT and enforces the phase-one inbound destination policy. */
@Component
@RequiredArgsConstructor
public class RealtimeStompInterceptor implements ChannelInterceptor {

    public static final String CLIENT_EVENTS_DESTINATION = "/user/queue/events";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenAuthenticationService authenticationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            requireAuthenticated(accessor);
            if (!CLIENT_EVENTS_DESTINATION.equals(accessor.getDestination())) {
                throw new AccessDeniedException("STOMP subscription destination is not allowed");
            }
        } else if (StompCommand.SEND.equals(command)) {
            throw new AccessDeniedException("Client STOMP SEND is not enabled");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("Missing STOMP bearer token");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        var authentication = authenticationService.authenticate(token)
                .orElseThrow(() -> new AccessDeniedException("Invalid STOMP bearer token"));
        accessor.setUser(authentication);
    }

    private void requireAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new AccessDeniedException("STOMP session is not authenticated");
        }
    }
}
