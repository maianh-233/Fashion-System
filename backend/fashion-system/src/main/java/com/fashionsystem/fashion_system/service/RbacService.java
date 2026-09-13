package com.fashionsystem.fashion_system.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nạp permission hiệu lực động từ cơ sở dữ liệu cho Spring Security. */
@Service
@RequiredArgsConstructor
public class RbacService {

    private final AuthorizationService authorizationService;
    private final RoleRepository roleRepository;

    /**
     * Xây dựng authorities từ cả role và permission hiệu lực của user.
     *
     * @param userId mã user lấy từ JWT đã xác minh
     * @return tập authority dùng bởi SecurityContext và @PreAuthorize; DENY đã được loại bỏ
     */
    @Transactional(readOnly = true)
    public List<GrantedAuthority> loadAuthorities(UUID userId) {
        Stream<String> roles = roleRepository.findCodesByUserId(userId).stream()
                .map(code -> "ROLE_" + code.trim().toUpperCase(Locale.ROOT));
        Stream<String> permissions = authorizationService.getEffectivePermissions(userId).stream()
                .map(permission -> permission.permissionCode());

        return Stream.concat(roles, permissions)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(authority -> (GrantedAuthority) authority)
                .toList();
    }
}
