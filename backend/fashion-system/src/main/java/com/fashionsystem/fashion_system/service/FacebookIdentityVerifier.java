package com.fashionsystem.fashion_system.service;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.entity.SocialProvider;

/** Xác minh Facebook user access token bằng debug_token rồi mới đọc hồ sơ /me. */
@Component
public class FacebookIdentityVerifier implements SocialIdentityVerifier {
    private final String appId;
    private final String appSecret;
    private final String graphVersion;
    private final String graphBaseUrl;
    private final RestClient restClient;

    public FacebookIdentityVerifier(
            @Value("${social-auth.facebook.app-id:}") String appId,
            @Value("${social-auth.facebook.app-secret:}") String appSecret,
            @Value("${social-auth.facebook.graph-base-url:}") String graphBaseUrl,
            @Value("${social-auth.facebook.graph-version:}") String graphVersion) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.graphBaseUrl = graphBaseUrl;
        this.graphVersion = graphVersion;
        this.restClient = graphBaseUrl.isBlank() ? null : RestClient.create(graphBaseUrl);
    }

    @Override
    public boolean supports(SocialProvider provider) {
        return provider == SocialProvider.FACEBOOK;
    }

    @Override
    public SocialProfile verify(String token) {
        if (appId.isBlank() || appSecret.isBlank() || graphBaseUrl.isBlank() || graphVersion.isBlank()) {
            throw BusinessException.serviceUnavailable("Facebook Login chưa được cấu hình");
        }
        try {
            Map<String, Object> debug = get("/" + graphVersion + "/debug_token", Map.of(
                    "input_token", token, "access_token", appId + "|" + appSecret));
            Map<?, ?> data = asMap(debug.get("data"));
            String providerUserId = string(data.get("user_id"));
            boolean valid = Boolean.TRUE.equals(data.get("is_valid"));
            boolean correctApp = appId.equals(string(data.get("app_id")));
            long expiresAt = number(data.get("expires_at"));
            if (!valid || !correctApp || providerUserId == null
                    || (expiresAt > 0 && expiresAt <= Instant.now().getEpochSecond())) {
                throw invalidToken();
            }

            Map<String, Object> me = get("/" + graphVersion + "/me", Map.of(
                    "fields", "id,name,email,picture.type(large)", "access_token", token));
            if (!providerUserId.equals(string(me.get("id")))) {
                throw invalidToken();
            }
            Map<?, ?> picture = asMap(me.get("picture"));
            Map<?, ?> pictureData = asMap(picture.get("data"));
            return new SocialProfile(SocialProvider.FACEBOOK, providerUserId, string(me.get("email")),
                    string(me.get("name")), string(pictureData.get("url")));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidToken();
        }
    }

    private Map<String, Object> get(String path, Map<String, String> query) {
        return restClient.get().uri(builder -> {
                    builder.path(path);
                    query.forEach(builder::queryParam);
                    return builder.build();
                }).retrieve().body(new ParameterizedTypeReference<>() {});
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private BusinessException invalidToken() {
        return BusinessException.unauthorized("Facebook access token không hợp lệ");
    }
}
