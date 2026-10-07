package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.auth.RegisterCustomerRequest;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerMembershipStatus;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.util.PhoneNormalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tạo hoặc liên kết Customer Account mà không thay đổi nguồn gốc của Store Member. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("CUSTOMER")
@RequiredArgsConstructor
public class CustomerAccountLinkService {
    private final CustomerRepository customers;
    private final CustomerAccountRepository accounts;
    private final CustomerTierRepository tiers;
    private final CustomerTierAssignmentRepository assignments;
    private final CustomerCodeGenerator codeGenerator;
    private final PhoneNormalizer phoneNormalizer;
    private final PasswordEncoder passwordEncoder;

    public record LinkedAccount(Customer customer, CustomerAccount account, boolean createdCustomer) {}

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CUSTOMER_PHONE_LOOKUP, allEntries = true)
    public LinkedAccount register(RegisterCustomerRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());
        ensureAccountIdentityAvailable(username, email);

        Optional<Customer> phoneMatch = customers.findByNormalizedPhone(phone);
        Optional<Customer> emailMatch = customers.findByEmailIgnoreCase(email);
        if (phoneMatch.isPresent() && emailMatch.isPresent()
                && !phoneMatch.get().getId().equals(emailMatch.get().getId())) {
            throw BusinessException.conflict("Số điện thoại và email thuộc hai khách hàng khác nhau");
        }

        boolean created = phoneMatch.isEmpty();
        Customer customer = phoneMatch.orElseGet(() -> newWebsiteCustomer(
                request.fullName(), request.phone(), phone, email));
        if (!created && accounts.existsByCustomerId(customer.getId())) {
            throw BusinessException.conflict("Khách hàng đã có tài khoản website");
        }
        if (!created && customer.getEmail() == null) customer.setEmail(email);

        try {
            customer = customers.save(customer);
            ensureRegularTier(customer);
            LocalDateTime now = LocalDateTime.now();
            CustomerAccount account = accounts.save(CustomerAccount.builder()
                    .customerId(customer.getId())
                    .username(username)
                    .loginEmail(email)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .failedLoginAttempts(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            return new LinkedAccount(customer, account, created);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Username, email hoặc số điện thoại đã được sử dụng");
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CUSTOMER_PHONE_LOOKUP, allEntries = true)
    public LinkedAccount linkVerifiedSocial(SocialProfile profile) {
        String email = normalizeEmail(profile.email());
        Optional<CustomerAccount> existingAccount = accounts.findByLoginEmailIgnoreCase(email);
        if (existingAccount.isPresent()) {
            Customer customer = customers.findById(existingAccount.get().getCustomerId())
                    .orElseThrow(() -> BusinessException.invalidState("Tài khoản Customer không còn hồ sơ"));
            return new LinkedAccount(customer, existingAccount.get(), false);
        }

        Optional<Customer> emailMatch = customers.findByEmailIgnoreCase(email);
        boolean created = emailMatch.isEmpty();
        Customer customer = emailMatch.orElseGet(() -> newWebsiteCustomer(
                profile.fullName(), null, null, email));
        if (!created && accounts.existsByCustomerId(customer.getId())) {
            throw BusinessException.conflict("Khách hàng đã liên kết tài khoản website khác");
        }

        try {
            customer = customers.save(customer);
            ensureRegularTier(customer);
            LocalDateTime now = LocalDateTime.now();
            String username = socialUsername(profile);
            CustomerAccount account = accounts.save(CustomerAccount.builder()
                    .customerId(customer.getId())
                    .username(username)
                    .loginEmail(email)
                    .failedLoginAttempts(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            return new LinkedAccount(customer, account, created);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Email hoặc tài khoản social đã được liên kết");
        }
    }

    private Customer newWebsiteCustomer(
            String fullName, String phone, String normalizedPhone, String email) {
        LocalDateTime now = LocalDateTime.now();
        return Customer.builder()
                .customerCode(codeGenerator.nextCode())
                .fullName(fullName == null ? null : fullName.trim())
                .phone(phone == null ? null : phone.trim())
                .normalizedPhone(normalizedPhone)
                .email(email)
                .source(CustomerSource.WEBSITE)
                .membershipStatus(CustomerMembershipStatus.MEMBER)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void ensureRegularTier(Customer customer) {
        if (assignments.findByCustomerIdAndExpiresAtIsNull(customer.getId()).isPresent()) return;
        CustomerTier tier = tiers.findByCode("REGULAR")
                .orElseThrow(() -> BusinessException.invalidState("Chưa cấu hình hạng REGULAR"));
        assignments.save(CustomerTierAssignment.builder()
                .customerId(customer.getId())
                .tierId(tier.getId())
                .assignedAt(LocalDateTime.now())
                .note("Default REGULAR tier")
                .build());
    }

    private void ensureAccountIdentityAvailable(String username, String email) {
        if (accounts.existsByUsername(username)) {
            throw BusinessException.conflict("Username đã được sử dụng");
        }
        if (accounts.existsByLoginEmailIgnoreCase(email)) {
            throw BusinessException.conflict("Email đã được sử dụng");
        }
    }

    private String normalizeUsername(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) throw BusinessException.badRequest("Email là bắt buộc");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        try {
            return phoneNormalizer.normalize(value);
        } catch (IllegalArgumentException exception) {
            throw BusinessException.badRequest(exception.getMessage());
        }
    }

    private String socialUsername(SocialProfile profile) {
        String provider = profile.provider().name().toLowerCase(Locale.ROOT);
        String subject = profile.providerUserId().replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        String suffix = subject.length() > 32 ? subject.substring(0, 32) : subject;
        return (provider + "_" + suffix).substring(0, Math.min(50, provider.length() + 1 + suffix.length()));
    }
}
