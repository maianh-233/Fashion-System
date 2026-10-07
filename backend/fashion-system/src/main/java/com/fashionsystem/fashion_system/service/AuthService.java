package com.fashionsystem.fashion_system.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fashionsystem.fashion_system.config.AuthProperties;
import com.fashionsystem.fashion_system.dto.auth.AuthResponse;
import com.fashionsystem.fashion_system.dto.auth.EmployeeRegistrationResponse;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import com.fashionsystem.fashion_system.dto.auth.MessageResponse;
import com.fashionsystem.fashion_system.dto.auth.RegisterAdminRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterEmployeeRequest;
import com.fashionsystem.fashion_system.entity.EmploymentStatus;
import com.fashionsystem.fashion_system.entity.RevokedToken;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.UserRole;
import com.fashionsystem.fashion_system.entity.UserDepartment;
import com.fashionsystem.fashion_system.repository.RevokedTokenRepository;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.UserRoleRepository;
import com.fashionsystem.fashion_system.repository.UserDepartmentRepository;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.AuthResponseMapper;
import com.fashionsystem.fashion_system.validation.AccountRegistrationValidator;
import com.fashionsystem.fashion_system.validation.AccountRegistrationValidator.EmployeeRelations;

import lombok.RequiredArgsConstructor;

/** Xử lý đăng ký, đăng nhập và phát hành JWT riêng cho Employee/Admin. */
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(reason = "Authentication and login lock state")
@RequiredArgsConstructor
public class AuthService {

    private static final String ADMIN = "ADMIN";
    private static final String INVALID_CREDENTIALS = "Username hoặc mật khẩu không đúng";
    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountRegistrationValidator registrationValidator;
    private final AuthResponseMapper responseMapper;
    private final AuthProperties authProperties;
    private final AuthAuditService authAuditService;

    /** Tạo nhân viên cùng nhiều role và nhiều phòng ban; không cấp token của nhân viên mới. */
    @Transactional
    public EmployeeRegistrationResponse registerEmployee(RegisterEmployeeRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        String employeeCode = normalizeCode(request.employeeCode());
        String phone = normalizeOptional(request.phone());
        Set<String> roleCodes = normalizeCodes(request.roleCodes());
        EmployeeRelations relations = registrationValidator.validateEmployee(
                request, username, email, employeeCode, phone, roleCodes);
        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.save(prepareEmployee(request, username, email, employeeCode, phone, now));
        assignEmployeeRelations(user.getId(), relations, now);
        return responseMapper.toEmployeeRegistration(user, roleCodes, relations.departmentIds());
    }

    /** Thu hồi access token hiện tại tới khi token tự hết hạn. */
    @Transactional
    public MessageResponse logout(String token) {
        if (token == null || token.isBlank()) {
            return new MessageResponse("Đăng xuất thành công; refresh session đã bị thu hồi");
        }
        try {
        UUID tokenId = jwtService.extractTokenId(token);
        String accountType = jwtService.extractAccountType(token);
        if (!JwtService.ACCOUNT_TYPE_USER.equals(accountType)) {
            return new MessageResponse("Đăng xuất tài khoản nội bộ thành công");
        }
        UUID accountId = jwtService.extractUserId(token);
        revokedTokenRepository.save(RevokedToken.builder()
                .tokenId(tokenId).accountType(accountType).accountId(accountId)
                .expiresAt(jwtService.extractExpiration(token)).revokedAt(LocalDateTime.now()).build());
        authAuditService.record(
                JwtService.ACCOUNT_TYPE_USER.equals(accountType) ? accountId : null,
                AuthAuditService.LOGOUT,
                "Access token hiện tại đã được thu hồi");
        } catch (RuntimeException ignored) {
            // Access token hết hạn/không hợp lệ không ngăn việc revoke refresh session và xóa cookie.
        }
        return new MessageResponse("Đăng xuất thành công; JWT hiện tại đã bị thu hồi");
    }

    /**
     * Đăng ký admin và gán ROLE_ADMIN.
     *
     * @param request thông tin tài khoản admin
     * @return JWT cùng thông tin admin vừa tạo
     */
    @Transactional
    public AuthResponse registerAdmin(RegisterAdminRequest request) {
        User user = registerUser(request.username(), request.email(), request.password(), ADMIN);
        return createAuthResponse(user, ADMIN);
    }

    /**
     * Kiểm tra username/password và phát hành JWT khi tài khoản hợp lệ.
     *
     * @param request thông tin đăng nhập
     * @return JWT cùng thông tin người dùng cơ bản
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameForUpdate(normalizeUsername(request.username())).orElse(null);
        if (user == null) {
            authAuditService.record(null, AuthAuditService.LOGIN_FAILED, "Không tìm thấy tài khoản nội bộ");
            throw BusinessException.unauthorized(INVALID_CREDENTIALS);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(user.getActive()) || Boolean.TRUE.equals(user.getLocked())
                || user.getDeletedAt() != null) {
            authAuditService.record(user.getId(), AuthAuditService.LOGIN_FAILED, "Trạng thái tài khoản không cho phép đăng nhập");
            throw BusinessException.forbidden("Tài khoản không thể đăng nhập");
        }

        if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(now)) {
            throw temporaryLoginLock(user.getLoginLockedUntil(), now);
        }
        if (user.getLoginLockedUntil() != null) {
            user.setLoginLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            if (attempts >= authProperties.getMaxFailedAttempts()) {
                long lockSeconds = Math.max(1, authProperties.getLoginLockDurationSeconds());
                user.setFailedLoginAttempts(0);
                user.setLoginLockedUntil(now.plusSeconds(lockSeconds));
                userRepository.save(user);
                authAuditService.record(user.getId(), AuthAuditService.LOGIN_FAILED,
                        "Tài khoản tạm khóa do đăng nhập sai nhiều lần");
                throw BusinessException.tooManyRequests(
                        "Bạn đã nhập sai quá số lần cho phép. Vui lòng thử lại sau " + lockSeconds + " giây.",
                        lockSeconds);
            }
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);
            authAuditService.record(user.getId(), AuthAuditService.LOGIN_FAILED, "Thông tin đăng nhập không hợp lệ");
            throw BusinessException.unauthorized(INVALID_CREDENTIALS);
        }

        List<String> roles = findRoles(user);
        user.setFailedLoginAttempts(0);
        user.setLoginLockedUntil(null);
        user.setLastLogin(now);
        userRepository.save(user);
        AuthResponse response = createAuthResponse(user, roles);
        authAuditService.record(user.getId(), AuthAuditService.LOGIN_SUCCESS, "Đăng nhập tài khoản nội bộ thành công");
        return response;
    }

    private BusinessException temporaryLoginLock(LocalDateTime lockedUntil, LocalDateTime now) {
        long remainingMillis = Math.max(1, Duration.between(now, lockedUntil).toMillis());
        long retryAfterSeconds = Math.max(1, (remainingMillis + 999) / 1000);
        return BusinessException.tooManyRequests(
                "Tài khoản đang tạm khóa. Vui lòng thử lại sau " + retryAfterSeconds + " giây.",
                retryAfterSeconds);
    }

    /**
     * Tạo user mới sau khi chuẩn hóa và kiểm tra trùng username/email, rồi gán role.
     *
     * @param username tên đăng nhập
     * @param email địa chỉ email
     * @param rawPassword mật khẩu chưa băm
     * @param roleCode mã vai trò cần gán
     * @return user đã được lưu
     */
    private User registerUser(String username, String email, String rawPassword, String roleCode) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        registrationValidator.validateUser(normalizedUsername, normalizedEmail);

        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.save(User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .active(true)
                .locked(false)
                .employmentStatus(EmploymentStatus.ACTIVE.name())
                .failedLoginAttempts(0)
                .emailVerified(false)
                .phoneVerified(false)
                .lastPasswordChange(now)
                .createdAt(now)
                .build());
        Role role = getOrCreateRole(roleCode);
        userRoleRepository.save(UserRole.builder()
                .userId(user.getId())
                .roleId(role.getId())
                .assignedAt(now)
                .build());
        return user;
    }

    /**
     * Lấy role theo mã hoặc khởi tạo role chuẩn khi hệ thống chưa seed dữ liệu.
     *
     * @param roleCode mã role nội bộ, hiện dùng cho ADMIN
     * @return role có thể dùng để gán cho user
     */
    private Role getOrCreateRole(String roleCode) {
        return roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code(roleCode)
                        .name(roleCode)
                        .description("System role " + roleCode)
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    /**
     * Lấy toàn bộ role của user để JWT và response hỗ trợ RBAC nhiều vai trò.
     *
     * @param user user cần đọc quyền
     * @return danh sách mã role, không kèm tiền tố ROLE_
     */
    private List<String> findRoles(User user) {
        return roleRepository.findCodesByUserId(user.getId());
    }

    /**
     * Phát hành token và ánh xạ user sang DTO an toàn.
     *
     * @param user user đã xác thực
     * @param role vai trò vừa được gán cho user mới
     * @return response chứa token và thông tin không nhạy cảm
     */
    private AuthResponse createAuthResponse(User user, String role) {
        return createAuthResponse(user, List.of(role));
    }

    /**
     * Phát hành token chứa nhiều role và ánh xạ user sang DTO an toàn.
     *
     * @param user user đã xác thực
     * @param roles các vai trò đưa vào JWT
     * @return response chứa token và thông tin không nhạy cảm
     */
    private AuthResponse createAuthResponse(User user, List<String> roles) {
        if (roles.isEmpty()) {
            throw BusinessException.forbidden("Tài khoản chưa được gán vai trò");
        }
        String token = jwtService.generateToken(user, roles);
        return new AuthResponse(token, JwtService.TOKEN_TYPE, jwtService.getExpirationMs(),
                responseMapper.toUserInfo(user, roles));
    }

    private User prepareEmployee(
            RegisterEmployeeRequest request,
            String username,
            String email,
            String employeeCode,
            String phone,
            LocalDateTime now) {
        return User.builder()
                .username(username).email(email).phone(phone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .employeeCode(employeeCode).fullName(request.fullName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(enumName(request.gender()))
                .jobTitle(request.jobTitle())
                .employmentType(enumName(request.employmentType()))
                .employmentStatus(EmploymentStatus.ACTIVE.name())
                .hireDate(request.hireDate()).workLocation(request.workLocation()).managerId(request.managerId())
                .active(true).locked(false).failedLoginAttempts(0)
                .emailVerified(false).phoneVerified(false).lastPasswordChange(now).createdAt(now)
                .build();
    }

    private void assignEmployeeRelations(UUID userId, EmployeeRelations relations, LocalDateTime assignedAt) {
        userRoleRepository.saveAll(relations.roles().stream()
                .map(role -> UserRole.builder()
                        .userId(userId).roleId(role.getId()).assignedAt(assignedAt).build())
                .toList());
        userDepartmentRepository.saveAll(relations.departmentIds().stream()
                .map(departmentId -> UserDepartment.builder()
                        .userId(userId).departmentId(departmentId).assignedAt(assignedAt).build())
                .toList());
    }

    /**
     * Chuẩn hóa username để việc kiểm tra trùng và đăng nhập nhất quán.
     *
     * @param username username từ request
     * @return username đã bỏ khoảng trắng và chuyển chữ thường
     */
    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String normalizeCode(String code) { return code.trim().toUpperCase(Locale.ROOT); }
    private String normalizeOptional(String value) { return value == null ? null : value.trim(); }
    private String enumName(Enum<?> value) { return value == null ? null : value.name(); }
    private Set<String> normalizeCodes(Set<String> codes) {
        return codes.stream().map(this::normalizeCode).collect(Collectors.toSet());
    }
}
