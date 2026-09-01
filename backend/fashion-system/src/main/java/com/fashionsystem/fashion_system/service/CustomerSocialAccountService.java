package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CustomerSocialAccountDto;
import com.fashionsystem.fashion_system.dto.auth.SocialLoginRequest;
import com.fashionsystem.fashion_system.entity.CustomerSocialAccount;
import com.fashionsystem.fashion_system.entity.SocialProvider;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CustomerSocialAccountMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerSocialAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ liên kết tài khoản social đã được xác minh. */
@Service
@RequiredArgsConstructor
public class CustomerSocialAccountService {
    private final CustomerRepository customerRepository;
    private final CustomerSocialAccountRepository socialAccountRepository;
    private final List<SocialIdentityVerifier> identityVerifiers;
    private final CustomerSocialAccountMapper socialAccountMapper;

    /**
     * Lấy các tài khoản social đã liên kết với khách hàng.
     */
    @Transactional(readOnly = true)
    public List<CustomerSocialAccountDto> getList(UUID customerId) {
        requireCustomer(customerId);
        return socialAccountRepository.findAllByCustomerIdOrderByProviderAsc(customerId).stream()
                .map(socialAccountMapper::toDto)
                .toList();
    }

    /**
     * Lấy liên kết social của khách hàng theo provider.
     */
    @Transactional(readOnly = true)
    public CustomerSocialAccountDto getByProvider(UUID customerId, SocialProvider provider) {
        requireCustomer(customerId);
        return socialAccountMapper.toDto(requireSocialAccount(customerId, provider));
    }

    /**
     * Xác minh token rồi liên kết tài khoản social với khách hàng.
     */
    @Transactional
    public CustomerSocialAccountDto link(UUID customerId, SocialLoginRequest request) {
        customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        SocialProfile profile = findVerifier(request.provider()).verify(request.token());
        String provider = request.provider().name();
        var providerLink = socialAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId());
        if (providerLink.isPresent()) {
            if (providerLink.get().getCustomerId().equals(customerId)) {
                return socialAccountMapper.toDto(providerLink.get());
            }
            throw BusinessException.conflict("Tài khoản social đã được liên kết với khách hàng khác");
        }
        if (socialAccountRepository.existsByCustomerIdAndProvider(customerId, provider)) {
            throw BusinessException.conflict("Khách hàng đã liên kết provider này");
        }
        CustomerSocialAccount entity = CustomerSocialAccount.builder()
                .customerId(customerId)
                .provider(provider)
                .providerUserId(profile.providerUserId())
                .providerEmail(normalizeEmail(profile.email()))
                .createdAt(LocalDateTime.now())
                .build();
        try {
            return socialAccountMapper.toDto(socialAccountRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Liên kết social đã tồn tại");
        }
    }

    /**
     * Gỡ liên kết social của khách hàng theo provider.
     */
    @Transactional
    public void unlink(UUID customerId, SocialProvider provider) {
        customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        socialAccountRepository.delete(requireSocialAccount(customerId, provider));
    }

    private SocialIdentityVerifier findVerifier(SocialProvider provider) {
        return identityVerifiers.stream()
                .filter(candidate -> candidate.supports(provider))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("Provider không hỗ trợ"));
    }

    private CustomerSocialAccount requireSocialAccount(UUID customerId, SocialProvider provider) {
        return socialAccountRepository.findByCustomerIdAndProvider(customerId, provider.name())
                .orElseThrow(() -> BusinessException.notFound("Liên kết social không tồn tại"));
    }

    private void requireCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw BusinessException.notFound("Khách hàng không tồn tại");
        }
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
