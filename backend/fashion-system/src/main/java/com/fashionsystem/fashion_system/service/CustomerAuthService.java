package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.AuthProperties;
import com.fashionsystem.fashion_system.dto.auth.CustomerAuthResponse;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterCustomerRequest;
import com.fashionsystem.fashion_system.dto.auth.SocialLoginRequest;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerSocialAccount;
import com.fashionsystem.fashion_system.entity.RevokedToken;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.AuthResponseMapper;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerSocialAccountRepository;
import com.fashionsystem.fashion_system.repository.RevokedTokenRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ xác thực độc lập dành riêng cho Customer website. */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(reason = "Customer authentication")
@RequiredArgsConstructor
public class CustomerAuthService {
    private static final String INVALID_CREDENTIALS = "Username hoặc mật khẩu không đúng";
    private final CustomerRepository customers;
    private final CustomerAccountRepository accounts;
    private final CustomerSocialAccountRepository socialAccounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final List<SocialIdentityVerifier> socialIdentityVerifiers;
    private final CustomerAccountLinkService accountLinkService;
    private final AuthResponseMapper responseMapper;
    private final AuthProperties authProperties;
    private final RevokedTokenRepository revokedTokens;

    @Transactional
    public CustomerAuthResponse register(RegisterCustomerRequest request) {
        CustomerAccountLinkService.LinkedAccount linked = accountLinkService.register(request);
        return response(linked.customer(), linked.account());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public CustomerAuthResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        CustomerAccount account = accounts.findByUsernameForUpdate(username)
                .orElseThrow(() -> BusinessException.unauthorized(INVALID_CREDENTIALS));
        Customer customer = requireActiveCustomer(account.getCustomerId());
        LocalDateTime now = LocalDateTime.now();
        if (account.getLoginLockedUntil() != null && account.getLoginLockedUntil().isAfter(now)) {
            throw temporaryLoginLock(account.getLoginLockedUntil(), now);
        }
        if (account.getLoginLockedUntil() != null) {
            account.setLoginLockedUntil(null);
            account.setFailedLoginAttempts(0);
        }
        if (account.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            int attempts = (account.getFailedLoginAttempts() == null ? 0 : account.getFailedLoginAttempts()) + 1;
            if (attempts >= authProperties.getMaxFailedAttempts()) {
                long lockSeconds = Math.max(1, authProperties.getLoginLockDurationSeconds());
                account.setFailedLoginAttempts(0);
                account.setLoginLockedUntil(now.plusSeconds(lockSeconds));
                account.setUpdatedAt(now);
                accounts.save(account);
                throw BusinessException.tooManyRequests(
                        "Bạn đã nhập sai quá số lần cho phép. Vui lòng thử lại sau " + lockSeconds + " giây.",
                        lockSeconds);
            }
            account.setFailedLoginAttempts(attempts);
            account.setUpdatedAt(now);
            accounts.save(account);
            throw BusinessException.unauthorized(INVALID_CREDENTIALS);
        }
        account.setFailedLoginAttempts(0);
        account.setLoginLockedUntil(null);
        account.setUpdatedAt(now);
        accounts.save(account);
        return response(customer, account);
    }

    @Transactional
    public CustomerAuthResponse loginSocial(SocialLoginRequest request) {
        SocialProfile profile = verifier(request).verify(request.token());
        String provider = request.provider().name();
        LocalDateTime now = LocalDateTime.now();
        var existing = socialAccounts.findByProviderAndProviderUserId(provider, profile.providerUserId());
        Customer customer;
        CustomerAccount account;
        if (existing.isPresent()) {
            CustomerSocialAccount social = existing.get();
            customer = requireActiveCustomer(social.getCustomerId());
            account = accounts.findByCustomerId(customer.getId())
                    .orElseThrow(() -> BusinessException.invalidState("Liên kết social không có Customer Account"));
            social.setLastLoginAt(now);
            socialAccounts.save(social);
        } else {
            CustomerAccountLinkService.LinkedAccount linked = accountLinkService.linkVerifiedSocial(profile);
            customer = requireActiveCustomer(linked.customer().getId());
            account = linked.account();
            try {
                socialAccounts.saveAndFlush(CustomerSocialAccount.builder()
                        .customerId(customer.getId())
                        .provider(provider)
                        .providerUserId(profile.providerUserId())
                        .providerEmail(normalizeEmail(profile.email()))
                        .createdAt(now)
                        .lastLoginAt(now)
                        .build());
            } catch (DataIntegrityViolationException exception) {
                throw BusinessException.conflict("Tài khoản social đã được liên kết");
            }
        }
        return response(customer, account);
    }

    @Transactional
    public com.fashionsystem.fashion_system.dto.auth.MessageResponse logout(String token) {
        if (token != null && !token.isBlank()) {
            try {
                if (JwtService.ACCOUNT_TYPE_CUSTOMER.equals(jwtService.extractAccountType(token))) {
                    revokedTokens.save(RevokedToken.builder()
                            .tokenId(jwtService.extractTokenId(token))
                            .accountType(JwtService.ACCOUNT_TYPE_CUSTOMER)
                            .accountId(jwtService.extractCustomerId(token))
                            .expiresAt(jwtService.extractExpiration(token))
                            .revokedAt(LocalDateTime.now())
                            .build());
                }
            } catch (RuntimeException ignored) {
                // Refresh revocation remains authoritative when the access token is expired or malformed.
            }
        }
        return new com.fashionsystem.fashion_system.dto.auth.MessageResponse(
                "Đăng xuất khách hàng thành công");
    }

    private Customer requireActiveCustomer(java.util.UUID customerId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> BusinessException.unauthorized("Tài khoản khách hàng không hợp lệ"));
        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw BusinessException.forbidden("Tài khoản khách hàng không thể đăng nhập");
        }
        return customer;
    }

    private SocialIdentityVerifier verifier(SocialLoginRequest request) {
        return socialIdentityVerifiers.stream()
                .filter(candidate -> candidate.supports(request.provider()))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("Provider không hỗ trợ"));
    }

    private CustomerAuthResponse response(Customer customer, CustomerAccount account) {
        return new CustomerAuthResponse(
                jwtService.generateCustomerToken(customer, account),
                JwtService.TOKEN_TYPE,
                jwtService.getExpirationMs(),
                responseMapper.toCustomerInfo(customer, account));
    }

    private BusinessException temporaryLoginLock(LocalDateTime lockedUntil, LocalDateTime now) {
        long remainingMillis = Math.max(1, Duration.between(now, lockedUntil).toMillis());
        long seconds = Math.max(1, (remainingMillis + 999) / 1000);
        return BusinessException.tooManyRequests(
                "Tài khoản đang tạm khóa. Vui lòng thử lại sau " + seconds + " giây.", seconds);
    }

    private String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
