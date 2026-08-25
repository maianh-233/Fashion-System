package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerSocialAccount;
import java.util.Optional;
import java.util.UUID;

public interface CustomerSocialAccountRepository extends BaseRepository<CustomerSocialAccount, UUID> {
    Optional<CustomerSocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
