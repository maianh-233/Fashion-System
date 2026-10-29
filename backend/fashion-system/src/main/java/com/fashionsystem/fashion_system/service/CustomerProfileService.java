package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.CustomerProfileDtos;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.LoyaltyAccountRepository;
import com.fashionsystem.fashion_system.util.PhoneNormalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ của khách đang đăng nhập: xem (kèm hạng và điểm), tự sửa thông tin, đổi mật khẩu.
 *
 * <p>Số điện thoại và email phải là duy nhất giữa các khách (như khi nhân viên sửa). Email hồ sơ cũng là email đăng
 * nhập nhận mã quên mật khẩu, nên đổi email phải nhập mật khẩu hiện tại (tài khoản có mật khẩu).</p>
 */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("CUSTOMER")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerProfileService {
    private final CustomerRepository customers;
    private final CustomerAccountRepository accounts;
    private final CustomerTierRepository tiers;
    private final LoyaltyAccountRepository loyalty;
    private final PasswordEncoder passwordEncoder;
    private final PhoneNormalizer phoneNormalizer;

    public CustomerProfileDtos.Profile profile(UUID customerId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        CustomerAccount account = accounts.findByCustomerId(customerId).orElse(null);
        var tier = tiers.findCurrentByCustomerId(customerId).orElse(null);
        int points = loyalty.findById(customerId).map(a -> a.getPointsBalance() == null ? 0 : a.getPointsBalance()).orElse(0);
        return new CustomerProfileDtos.Profile(customer.getId(), customer.getCustomerCode(),
                account == null ? null : account.getUsername(), customer.getFullName(), customer.getEmail(),
                customer.getPhone(), customer.getDateOfBirth(), customer.getGender(), customer.getAvatar(),
                customer.getCreatedAt(), tier == null ? null : tier.getCode(), tier == null ? null : tier.getName(), points,
                account != null && account.getPasswordHash() != null);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CUSTOMER_PHONE_LOOKUP, allEntries = true)
    public CustomerProfileDtos.Profile update(UUID customerId, CustomerProfileDtos.UpdateProfile request) {
        Customer customer = customers.findByIdForUpdate(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        CustomerAccount account = accounts.findByCustomerIdForUpdate(customerId).orElse(null);

        String normalizedPhone;
        try {
            normalizedPhone = phoneNormalizer.normalize(request.phone());
        } catch (IllegalArgumentException invalid) {
            throw BusinessException.badRequest(invalid.getMessage());
        }
        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim().toLowerCase(Locale.ROOT);
        if (customers.existsByNormalizedPhoneAndIdNot(normalizedPhone, customerId))
            throw BusinessException.conflict("Số điện thoại đã được dùng cho tài khoản khác");

        boolean emailChanged = !Objects.equals(email, customer.getEmail());
        if (emailChanged) {
            if (email != null && (customers.existsByEmailIgnoreCaseAndIdNot(email, customerId)
                    || accounts.findByLoginEmailIgnoreCase(email).filter(a -> !a.getCustomerId().equals(customerId)).isPresent()))
                throw BusinessException.conflict("Email đã được dùng cho tài khoản khác");
            if (account != null && account.getPasswordHash() != null && (request.currentPassword() == null
                    || !passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())))
                throw BusinessException.badRequest("Vui lòng nhập đúng mật khẩu hiện tại để đổi email");
        }

        // Ngày sinh quyết định voucher sinh nhật: khách tự nhập một lần, đổi sau đó phải nhờ cửa hàng.
        if (customer.getDateOfBirth() != null && !customer.getDateOfBirth().equals(request.dateOfBirth()))
            throw BusinessException.badRequest("Ngày sinh đã được lưu. Vui lòng liên hệ cửa hàng nếu cần sửa");

        LocalDateTime now = LocalDateTime.now();
        customer.setFullName(request.fullName().trim());
        customer.setPhone(request.phone().trim());
        customer.setNormalizedPhone(normalizedPhone);
        customer.setEmail(email);
        customer.setDateOfBirth(request.dateOfBirth());
        customer.setGender(request.gender());
        customer.setUpdatedAt(now);
        customers.save(customer);
        if (emailChanged && account != null) {
            // Mã quên mật khẩu gửi tới email đăng nhập: giữ trùng với email hồ sơ.
            account.setLoginEmail(email);
            account.setUpdatedAt(now);
            accounts.save(account);
        }
        return profile(customerId);
    }

    /** Đổi mật khẩu (hoặc đặt mật khẩu lần đầu cho tài khoản đăng nhập Google). */
    @Transactional
    public void changePassword(UUID customerId, CustomerProfileDtos.ChangePassword request) {
        CustomerAccount account = accounts.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> BusinessException.notFound("Tài khoản không tồn tại"));
        if (account.getPasswordHash() != null) {
            if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), account.getPasswordHash()))
                throw BusinessException.badRequest("Mật khẩu hiện tại không chính xác");
            if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash()))
                throw BusinessException.badRequest("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        account.setFailedLoginAttempts(0);
        account.setLoginLockedUntil(null);
        account.setUpdatedAt(LocalDateTime.now());
        accounts.save(account);
    }
}
