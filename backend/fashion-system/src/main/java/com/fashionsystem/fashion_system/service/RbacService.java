package com.fashionsystem.fashion_system.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nạp permission hiệu lực động từ cơ sở dữ liệu cho Spring Security. */
@Service
@RequiredArgsConstructor
public class RbacService {

    private final AuthorizationService authorizationService;

    /**
     * Xây dựng authorities chỉ từ permission code hiệu lực của user.
     *
     * @param userId mã user lấy từ JWT đã xác minh
     * @return tập authority dùng bởi SecurityContext và @PreAuthorize; DENY đã được loại bỏ
     */
    @Transactional(readOnly = true)
    public List<GrantedAuthority> loadAuthorities(UUID userId) {
        return authorizationService.getEffectivePermissions(userId).stream()
                .map(permission -> permission.permissionCode())
                .map(SimpleGrantedAuthority::new)
                .map(authority -> (GrantedAuthority) authority)
                .toList();
    }
}
