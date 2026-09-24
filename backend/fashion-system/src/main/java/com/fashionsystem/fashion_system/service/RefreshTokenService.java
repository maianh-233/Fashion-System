package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.RefreshTokenProperties;
import com.fashionsystem.fashion_system.dto.auth.AuthResponse;
import com.fashionsystem.fashion_system.dto.auth.CustomerAuthResponse;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.UserToken;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.AuthResponseMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.UserTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý refresh session có trạng thái; DB chỉ lưu SHA-256 của token. */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(reason = "Session token lifecycle")
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String REFRESH = "REFRESH";
    private static final String INVALID_REFRESH = "Phiên đăng nhập đã hết hạn hoặc không hợp lệ";

    private final UserTokenRepository userTokenRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthResponseMapper responseMapper;
    private final RefreshTokenProperties properties;

    public record IssuedToken(String value) {}
    public record RefreshedSession(Object response, String refreshToken) {}

    @Transactional
    public IssuedToken issueForUser(UUID userId) {
        return issue(userId, null, UUID.randomUUID(), null,
                LocalDateTime.now().plus(properties.getExpiration()));
    }

    @Transactional
    public IssuedToken issueForCustomer(UUID customerId) {
        return issue(null, customerId, UUID.randomUUID(), null,
                LocalDateTime.now().plus(properties.getExpiration()));
    }

    /** Khóa token cũ, rotate một lần và cấp access JWT từ role/trạng thái hiện tại trong DB. */
    @Transactional(noRollbackFor = BusinessException.class)
    public RefreshedSession refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw unauthorized();

        UserToken current = userTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(this::unauthorized);
        LocalDateTime now = LocalDateTime.now();
        if (current.getRevokedAt() != null) {
            userTokenRepository.revokeFamily(current.getRefreshTokenFamily(), now);
            throw unauthorized();
        }
        if (current.getExpiresAt().isBefore(now) || current.getExpiresAt().isEqual(now)) {
            current.setRevokedAt(now);
            throw unauthorized();
        }

        Object response;
        if (current.getUserId() != null) {
            User user = userRepository.findById(current.getUserId()).orElseThrow(this::unauthorized);
            validateUser(user);
            List<String> roles = roleRepository.findCodesByUserId(user.getId());
            if (roles.isEmpty()) throw unauthorized();
            String accessToken = jwtService.generateToken(user, roles);
            response = new AuthResponse(accessToken, JwtService.TOKEN_TYPE, jwtService.getExpirationMs(),
                    responseMapper.toUserInfo(user, roles));
        } else if (current.getCustomerId() != null) {
            Customer customer = customerRepository.findById(current.getCustomerId()).orElseThrow(this::unauthorized);
            validateCustomer(customer);
            String accessToken = jwtService.generateCustomerToken(customer);
            response = new CustomerAuthResponse(accessToken, JwtService.TOKEN_TYPE, jwtService.getExpirationMs(),
                    responseMapper.toCustomerInfo(customer));
        } else {
            throw unauthorized();
        }

        current.setRevokedAt(now);
        IssuedToken replacement = issue(
                current.getUserId(), current.getCustomerId(), current.getRefreshTokenFamily(), current.getId(),
                current.getExpiresAt());
        return new RefreshedSession(response, replacement.value());
    }

    /** Revoke toàn bộ family để mọi refresh token của phiên logout đều mất hiệu lực. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        userTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> userTokenRepository.revokeFamily(
                        token.getRefreshTokenFamily(), LocalDateTime.now()));
    }

    private IssuedToken issue(
            UUID userId, UUID customerId, UUID family, UUID parentId, LocalDateTime expiresAt) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        userTokenRepository.save(UserToken.builder()
                .userId(userId)
                .customerId(customerId)
                .tokenHash(hash(rawToken))
                .tokenType(REFRESH)
                .refreshTokenFamily(family)
                .parentTokenId(parentId)
                .expiresAt(expiresAt)
                .createdAt(now)
                .build());
        return new IssuedToken(rawToken);
    }

    private void validateUser(User user) {
        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getLocked())
                || user.getDeletedAt() != null) throw unauthorized();
    }

    private void validateCustomer(Customer customer) {
        if (!Boolean.TRUE.equals(customer.getActive()) || Boolean.TRUE.equals(customer.getLocked())) {
            throw unauthorized();
        }
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }

    private BusinessException unauthorized() {
        return BusinessException.unauthorized(INVALID_REFRESH);
    }
}
