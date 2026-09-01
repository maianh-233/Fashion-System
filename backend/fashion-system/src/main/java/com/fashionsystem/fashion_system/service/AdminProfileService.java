package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.profile.AdminProfileResponse;
import com.fashionsystem.fashion_system.dto.profile.AdminProfileResponse.RoleSummary;
import com.fashionsystem.fashion_system.dto.profile.ChangeMyPasswordRequest;
import com.fashionsystem.fashion_system.dto.profile.UpdateAdminProfileRequest;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc và cập nhật hồ sơ của chính tài khoản nội bộ đang đăng nhập. */
@Service
@RequiredArgsConstructor
public class AdminProfileService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AdminProfileResponse getProfile(UUID userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public AdminProfileResponse updateProfile(UUID userId, UpdateAdminProfileRequest request) {
        User user = requireUser(userId);
        String phone = normalize(request.phone());
        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw BusinessException.conflict("Số điện thoại đã được sử dụng");
        }

        if (!Objects.equals(user.getPhone(), phone)) {
            user.setPhoneVerified(Boolean.FALSE);
        }
        user.setPhone(phone);
        user.setDateOfBirth(request.dateOfBirth());
        user.setGender(request.gender() == null ? null : request.gender().name());
        user.setAvatar(normalize(request.avatar()));
        user.setUpdatedAt(LocalDateTime.now());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangeMyPasswordRequest request) {
        User user = requireUser(userId);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("Mật khẩu hiện tại không chính xác");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Tài khoản không tồn tại"));
    }

    private AdminProfileResponse toResponse(User user) {
        List<String> roleCodes = roleRepository.findCodesByUserId(user.getId());
        List<RoleSummary> roles = roleRepository.findAllByCodeIn(roleCodes).stream()
                .sorted(java.util.Comparator.comparing(Role::getCode))
                .map(role -> new RoleSummary(role.getCode(), role.getName()))
                .toList();
        return new AdminProfileResponse(
                user.getId(), user.getUsername(), user.getEmployeeCode(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getDateOfBirth(), user.getGender(), user.getAvatar(), user.getJobTitle(),
                user.getEmploymentType(), user.getEmploymentStatus(), user.getHireDate(), user.getWorkLocation(),
                user.getActive(), user.getLocked(), user.getEmailVerified(), user.getPhoneVerified(),
                user.getLastPasswordChange(), user.getLastLogin(), user.getCreatedAt(), roles);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
