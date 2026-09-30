package com.fashionsystem.fashion_system.config;

import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.UserRole;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.UserRoleRepository;
import com.fashionsystem.fashion_system.service.JwtService;
import com.fashionsystem.fashion_system.service.AuthAuditService;
import com.fashionsystem.fashion_system.service.AuthService;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.HttpCookie;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real application/filter chain, signed JWTs and database accounts; fixtures roll back. */
@SpringBootTest
@Import(JwtSecurityIntegrationTest.ProbeController.class)
@Transactional
class JwtSecurityIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JwtService jwtService;
    @Autowired UserRepository users;
    @Autowired CustomerRepository customers;
    @Autowired RoleRepository roles;
    @Autowired UserRoleRepository userRoles;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;
    // Audit uses REQUIRES_NEW and cannot see uncommitted, rollback-only account fixtures.
    // Keep credential verification, repositories, token/session services and filters real.
    @MockitoBean AuthAuditService authAuditService;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicLoginRoutesReachValidationWithoutBearerToken() throws Exception {
        for (String path : List.of("/api/auth/login", "/api/auth/login/employee", "/api/auth/login/customer",
                "/api/auth/login/customer/social", "/api/auth/register/customer")) {
            mvc.perform(post(path).contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void refreshIsPublicButRequiresValidRefreshSession() throws Exception {
        mvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void productControllerStillRequiresPermissionDespitePublicFilterMatcher() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void employeeDatabaseLoginAndRefreshIssueUsableJwt() throws Exception {
        String password = UUID.randomUUID().toString();
        String username = "security-" + UUID.randomUUID();
        User user = users.saveAndFlush(User.builder().username(username)
                .passwordHash(passwordEncoder.encode(password)).active(true).locked(false)
                .createdAt(LocalDateTime.now()).build());
        Role role = roles.saveAndFlush(Role.builder().code("SEC_" + UUID.randomUUID())
                .name("Security regression fixture").createdAt(LocalDateTime.now()).build());
        userRoles.saveAndFlush(UserRole.builder().userId(user.getId()).roleId(role.getId())
                .assignedAt(LocalDateTime.now()).build());
        String credentials = objectMapper.writeValueAsString(java.util.Map.of(
                "username", username, "password", password));
        var login = mvc.perform(post("/api/auth/login/employee")
                        .contentType("application/json").content(credentials))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse();
        String token = objectMapper.readTree(login.getContentAsString()).get("token").asString();
        mvc.perform(get("/api/security-regression/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        HttpCookie cookie = HttpCookie.parse(login.getHeader("Set-Cookie")).getFirst();
        var refreshed = mvc.perform(post("/api/auth/refresh")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .cookie(new Cookie(cookie.getName(), cookie.getValue())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse();
        String newToken = objectMapper.readTree(refreshed.getContentAsString()).get("token").asString();
        mvc.perform(get("/api/security-regression/read").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    void customerDatabaseCredentialsIssueUsableJwtIndependentlyOfEmployeeAccounts() throws Exception {
        String password = UUID.randomUUID().toString();
        String username = "security-" + UUID.randomUUID();
        users.saveAndFlush(User.builder().username(username)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .active(true).locked(false).createdAt(LocalDateTime.now()).build());
        customers.saveAndFlush(Customer.builder().username(username)
                .passwordHash(passwordEncoder.encode(password)).active(true).locked(false)
                .createdAt(LocalDateTime.now()).build());
        String token = authService.loginCustomer(new LoginRequest(username, password)).token();
        mvc.perform(get("/api/security-regression/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRouteRejectsMissingAndInvalidTokens() throws Exception {
        mvc.perform(get("/api/security-regression/read")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/security-regression/read").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeJwtAuthenticatesDatabaseAccountAndRejectsLockedAccount() throws Exception {
        User user = users.saveAndFlush(User.builder().username("security-" + UUID.randomUUID())
                .active(true).locked(false).createdAt(LocalDateTime.now()).build());
        String token = jwtService.generateToken(user, List.of());
        mvc.perform(get("/api/security-regression/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        user.setLocked(true);
        users.saveAndFlush(user);
        mvc.perform(get("/api/security-regression/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerJwtAuthenticatesDatabaseAccountButCannotAccessEmployeePermission() throws Exception {
        Customer customer = customers.saveAndFlush(Customer.builder().username("security-" + UUID.randomUUID())
                .active(true).locked(false).createdAt(LocalDateTime.now()).build());
        String token = jwtService.generateCustomerToken(customer);
        mvc.perform(get("/api/security-regression/read").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/security-regression/privileged").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/security-regression/read")
        String read() { return "ok"; }

        @PreAuthorize("hasAuthority('USER_CREATE')")
        @GetMapping("/api/security-regression/privileged")
        String privileged() { return "ok"; }
    }
}
