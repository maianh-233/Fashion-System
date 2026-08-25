package com.fashionsystem.fashion_system.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Component;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.entity.SocialProvider;

/** Xác minh Google ID token: chữ ký, issuer, hạn dùng và audience của ứng dụng. */
@Component
public class GoogleIdentityVerifier implements SocialIdentityVerifier {
    private final String clientId;
    private final String issuerUri;
    private volatile JwtDecoder decoder;

    public GoogleIdentityVerifier(
            @Value("${social-auth.google.client-id:}") String clientId,
            @Value("${social-auth.google.issuer-uri:}") String issuerUri) {
        this.clientId = clientId;
        this.issuerUri = issuerUri;
    }

    @Override
    public boolean supports(SocialProvider provider) {
        return provider == SocialProvider.GOOGLE;
    }

    @Override
    public SocialProfile verify(String token) {
        if (clientId.isBlank() || issuerUri.isBlank()) {
            throw BusinessException.serviceUnavailable("Google Login chưa được cấu hình");
        }
        try {
            Jwt jwt = decoder().decode(token);
            List<String> audience = jwt.getAudience();
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            if (!audience.contains(clientId) || !Boolean.TRUE.equals(emailVerified) || jwt.getSubject() == null) {
                throw invalidToken();
            }
            return new SocialProfile(SocialProvider.GOOGLE, jwt.getSubject(), jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidToken();
        }
    }

    private JwtDecoder decoder() {
        JwtDecoder current = decoder;
        if (current == null) {
            synchronized (this) {
                current = decoder;
                if (current == null) {
                    current = JwtDecoders.fromIssuerLocation(issuerUri);
                    decoder = current;
                }
            }
        }
        return current;
    }

    private BusinessException invalidToken() {
        return BusinessException.unauthorized("Google ID token không hợp lệ");
    }
}
