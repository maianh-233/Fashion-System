package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CustomerRefreshTokenProperties;
import com.fashionsystem.fashion_system.dto.auth.CustomerAuthResponse;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerRefreshToken;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.AuthResponseMapper;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRefreshTokenRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer-only refresh session rotation and replay protection. */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(reason = "Customer session token lifecycle")
@RequiredArgsConstructor
public class CustomerRefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVALID = "Phiên đăng nhập khách hàng đã hết hạn hoặc không hợp lệ";
    private final CustomerRefreshTokenRepository tokens;
    private final CustomerAccountRepository accounts;
    private final CustomerRepository customers;
    private final JwtService jwtService;
    private final AuthResponseMapper responseMapper;
    private final CustomerRefreshTokenProperties properties;

    public record IssuedToken(String value) {}
    public record RefreshedSession(CustomerAuthResponse response, String refreshToken) {}

    @Transactional
    public IssuedToken issueForCustomer(UUID customerId) {
        CustomerAccount account = accounts.findByCustomerId(customerId).orElseThrow(this::unauthorized);
        return issue(account.getId(), UUID.randomUUID(), null,
                LocalDateTime.now().plus(properties.getExpiration()));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RefreshedSession refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw unauthorized();
        CustomerRefreshToken current = tokens.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(this::unauthorized);
        LocalDateTime now = LocalDateTime.now();
        if (current.getRevokedAt() != null) {
            tokens.revokeFamily(current.getRefreshTokenFamily(), now);
            throw unauthorized();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.setRevokedAt(now);
            throw unauthorized();
        }
        CustomerAccount account = accounts.findById(current.getCustomerAccountId())
                .orElseThrow(this::unauthorized);
        Customer customer = customers.findById(account.getCustomerId()).orElseThrow(this::unauthorized);
        if (!Boolean.TRUE.equals(customer.getActive())) throw unauthorized();

        current.setRevokedAt(now);
        IssuedToken replacement = issue(account.getId(), current.getRefreshTokenFamily(), current.getId(),
                current.getExpiresAt());
        CustomerAuthResponse response = new CustomerAuthResponse(
                jwtService.generateCustomerToken(customer, account), JwtService.TOKEN_TYPE,
                jwtService.getExpirationMs(), responseMapper.toCustomerInfo(customer, account));
        return new RefreshedSession(response, replacement.value());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        tokens.findByTokenHashForUpdate(hash(rawToken)).ifPresent(token ->
                tokens.revokeFamily(token.getRefreshTokenFamily(), LocalDateTime.now()));
    }

    @Transactional
    public void revokeAll(UUID customerAccountId) {
        tokens.revokeAllForAccount(customerAccountId, LocalDateTime.now());
    }

    private IssuedToken issue(UUID accountId, UUID family, UUID parentId, LocalDateTime expiresAt) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(CustomerRefreshToken.builder()
                .customerAccountId(accountId).tokenHash(hash(raw)).refreshTokenFamily(family)
                .parentTokenId(parentId).expiresAt(expiresAt).createdAt(LocalDateTime.now()).build());
        return new IssuedToken(raw);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }

    private BusinessException unauthorized() { return BusinessException.unauthorized(INVALID); }
}

