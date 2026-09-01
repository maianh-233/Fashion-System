package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerSocialAccount;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface CustomerSocialAccountRepository extends BaseRepository<CustomerSocialAccount, UUID> {
    Optional<CustomerSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<CustomerSocialAccount> findByCustomerIdAndProvider(UUID customerId, String provider);

    List<CustomerSocialAccount> findAllByCustomerIdOrderByProviderAsc(UUID customerId);

    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByCustomerIdAndProvider(UUID customerId, String provider);
}
