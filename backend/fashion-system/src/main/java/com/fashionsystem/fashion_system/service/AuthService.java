
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
import com.fashionsystem.fashion_system.dto.auth.CustomerAuthResponse;
import com.fashionsystem.fashion_system.dto.auth.EmployeeRegistrationResponse;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import com.fashionsystem.fashion_system.dto.auth.MessageResponse;
import com.fashionsystem.fashion_system.dto.auth.RegisterAdminRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterCustomerRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterEmployeeRequest;
import com.fashionsystem.fashion_system.dto.auth.SocialLoginRequest;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerSocialAccount;
import com.fashionsystem.fashion_system.entity.EmploymentStatus;
import com.fashionsystem.fashion_system.entity.RevokedToken;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.UserRole;
import com.fashionsystem.fashion_system.entity.UserDepartment;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerSocialAccountRepository;
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

/** Xử lý đăng ký, đăng nhập và phát hành JWT cho admin/customer. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ADMIN = "ADMIN";
    private static final String INVALID_CREDENTIALS = "Username hoặc mật khẩu không đúng";
    private static final String CUSTOMER_LOGIN_FORBIDDEN = "Tài khoản khách hàng không thể đăng nhập";
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CustomerSocialAccountRepository customerSocialAccountRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final List<SocialIdentityVerifier> socialIdentityVerifiers;
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

    /** Đăng ký hoặc đăng nhập khách hàng sau khi Google ID token đã được xác minh. */
    @Transactional
    public CustomerAuthResponse loginSocialCustomer(SocialLoginRequest request) {
        String provider = request.provider().name();
        SocialProfile profile = findVerifier(request).verify(request.token());
        Customer customer = findOrCreateSocialCustomer(provider, profile, LocalDateTime.now());
        validateCustomerCanLogin(customer);
        return createCustomerAuthResponse(customer);
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
        UUID accountId = JwtService.ACCOUNT_TYPE_CUSTOMER.equals(accountType)
                ? jwtService.extractCustomerId(token) : jwtService.extractUserId(token);
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
     * Đăng ký customer trong bảng customers độc lập, không tạo User hoặc UserRole.
     *
     * @param request thông tin tài khoản và tên customer
     * @return JWT cùng thông tin customer vừa tạo
     */
    @Transactional
    public CustomerAuthResponse registerCustomer(RegisterCustomerRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        registrationValidator.validateCustomer(username, email);
        Customer customer = customerRepository.save(Customer.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .active(true)
                .locked(false)
                .fullName(request.fullName().trim())
                .createdAt(LocalDateTime.now())
                .build());
        return createCustomerAuthResponse(customer);
    }

    /** Đăng nhập vào không gian khách hàng, không tra cứu bảng users. */
    @Transactional(readOnly = true)
    public CustomerAuthResponse loginCustomer(LoginRequest request) {
        Customer customer = customerRepository.findByUsername(normalizeUsername(request.username()))
                .orElseThrow(() -> BusinessException.unauthorized(INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            throw BusinessException.unauthorized(INVALID_CREDENTIALS);
        }
        validateCustomerCanLogin(customer);
        return createCustomerAuthResponse(customer);
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

    private CustomerAuthResponse createCustomerAuthResponse(Customer customer) {
        String token = jwtService.generateCustomerToken(customer);
        return new CustomerAuthResponse(
                token,
                JwtService.TOKEN_TYPE,
                jwtService.getExpirationMs(),
                responseMapper.toCustomerInfo(customer));
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

    private SocialIdentityVerifier findVerifier(SocialLoginRequest request) {
        return socialIdentityVerifiers.stream()
                .filter(candidate -> candidate.supports(request.provider()))
                .findFirst()
                .orElseThrow(() -> BusinessException.badRequest("Provider không hỗ trợ"));
    }

    private Customer findOrCreateSocialCustomer(String provider, SocialProfile profile, LocalDateTime now) {
        return customerSocialAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId())
                .map(social -> loadSocialCustomer(social, now))
                .orElseGet(() -> createSocialCustomer(provider, profile, now));
    }

    private Customer loadSocialCustomer(CustomerSocialAccount social, LocalDateTime now) {
        Customer customer = customerRepository.findById(social.getCustomerId())
                .orElseThrow(() -> BusinessException.conflict("Liên kết social bị lỗi"));
        social.setLastLoginAt(now);
        customerSocialAccountRepository.save(social);
        return customer;
    }

    private Customer createSocialCustomer(String provider, SocialProfile profile, LocalDateTime now) {
        String email = normalizeOptionalEmail(profile.email());
        if (email != null && customerRepository.existsByEmail(email)) {
            throw BusinessException.conflict(
                    "Email đã có tài khoản; hãy đăng nhập tài khoản hiện tại rồi liên kết social");
        }
        Customer customer = customerRepository.save(Customer.builder()
                .username(provider.toLowerCase(Locale.ROOT) + "_" + UUID.randomUUID().toString().replace("-", ""))
                .email(email).fullName(profile.fullName()).avatar(profile.avatar())
                .active(true).locked(false).createdAt(now).build());
        customerSocialAccountRepository.save(CustomerSocialAccount.builder()
                .customerId(customer.getId()).provider(provider).providerUserId(profile.providerUserId())
                .providerEmail(email).createdAt(now).lastLoginAt(now).build());
        return customer;
    }

    private void validateCustomerCanLogin(Customer customer) {
        if (!Boolean.TRUE.equals(customer.getActive()) || Boolean.TRUE.equals(customer.getLocked())) {
            throw BusinessException.forbidden(CUSTOMER_LOGIN_FORBIDDEN);
        }
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
    private String normalizeOptionalEmail(String email) {
        return email == null || email.isBlank() ? null : normalizeEmail(email);
    }
    private String normalizeCode(String code) { return code.trim().toUpperCase(Locale.ROOT); }
    private String normalizeOptional(String value) { return value == null ? null : value.trim(); }
    private String enumName(Enum<?> value) { return value == null ? null : value.name(); }
    private Set<String> normalizeCodes(Set<String> codes) {
        return codes.stream().map(this::normalizeCode).collect(Collectors.toSet());
    }
}
