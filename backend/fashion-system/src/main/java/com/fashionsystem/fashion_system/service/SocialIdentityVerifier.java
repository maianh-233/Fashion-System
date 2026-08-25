package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.SocialProvider;

public interface SocialIdentityVerifier {
    boolean supports(SocialProvider provider);
    SocialProfile verify(String token);
}
