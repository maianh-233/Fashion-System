package com.fashionsystem.fashion_system.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.RevokedTokenRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.realtime.RealtimeAccountType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.service.JwtService;
import com.fashionsystem.fashion_system.service.RbacService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AccessTokenAuthenticationServiceTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final CustomerAccountRepository customerAccountRepository = mock(CustomerAccountRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RbacService rbacService = mock(RbacService.class);
    private final RevokedTokenRepository revokedTokenRepository = mock(RevokedTokenRepository.class);

    private AccessTokenAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AccessTokenAuthenticationService(
                jwtService,
                customerRepository,
                customerAccountRepository,
                userRepository,
                rbacService,
                revokedTokenRepository);
    }

    @Test
    void authenticatesActiveEmployeeWithCanonicalPrincipal() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("employee")
                .active(true)
                .locked(false)
                .build();
        when(jwtService.extractTokenId("token")).thenReturn(UUID.randomUUID());
        when(jwtService.extractAccountType("token")).thenReturn(JwtService.ACCOUNT_TYPE_USER);
        when(jwtService.extractUserId("token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        when(rbacService.loadAuthorities(userId))
                .thenReturn(List.of(new SimpleGrantedAuthority("IMPORT_RECEIPT_CONFIRM")));

        Optional<Authentication> result = service.authenticate("token");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getName()).isEqualTo("USER:" + userId);
        assertThat(result.orElseThrow().getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(result.orElseThrow().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("IMPORT_RECEIPT_CONFIRM");
    }

    @Test
    void authenticatesActiveCustomerWithCanonicalPrincipal() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).active(true).build();
        CustomerAccount account = CustomerAccount.builder()
                .customerId(customerId)
                .username("customer")
                .build();
        when(jwtService.extractTokenId("token")).thenReturn(UUID.randomUUID());
        when(jwtService.extractAccountType("token")).thenReturn(JwtService.ACCOUNT_TYPE_CUSTOMER);
        when(jwtService.extractCustomerId("token")).thenReturn(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerAccountRepository.findByCustomerId(customerId)).thenReturn(Optional.of(account));
        when(jwtService.isCustomerTokenValid("token", customer, account)).thenReturn(true);

        Optional<Authentication> result = service.authenticate("token");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getName()).isEqualTo("CUSTOMER:" + customerId);
        assertThat(result.orElseThrow().getPrincipal()).isInstanceOf(AuthenticatedCustomer.class);
        assertThat(result.orElseThrow().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void rejectsRevokedInactiveAndMalformedTokens() {
        UUID revokedTokenId = UUID.randomUUID();
        when(jwtService.extractTokenId("revoked")).thenReturn(revokedTokenId);
        when(revokedTokenRepository.existsById(revokedTokenId)).thenReturn(true);

        UUID inactiveUserId = UUID.randomUUID();
        User inactive = User.builder()
                .id(inactiveUserId)
                .username("inactive")
                .active(false)
                .locked(false)
                .build();
        when(jwtService.extractTokenId("inactive")).thenReturn(UUID.randomUUID());
        when(jwtService.extractAccountType("inactive")).thenReturn(JwtService.ACCOUNT_TYPE_USER);
        when(jwtService.extractUserId("inactive")).thenReturn(inactiveUserId);
        when(userRepository.findById(inactiveUserId)).thenReturn(Optional.of(inactive));
        when(jwtService.extractTokenId("malformed")).thenThrow(new IllegalArgumentException("invalid"));

        assertThat(service.authenticate("revoked")).isEmpty();
        assertThat(service.authenticate("inactive")).isEmpty();
        assertThat(service.authenticate("malformed")).isEmpty();
    }

    @Test
    void identitiesUseTypedCanonicalNamespace() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        assertThat(new AuthenticatedUser(userId, "u").realtimeIdentity())
                .isEqualTo(new RealtimeIdentity(RealtimeAccountType.USER, userId));
        assertThat(new AuthenticatedCustomer(customerId, "c").realtimeIdentity())
                .isEqualTo(new RealtimeIdentity(RealtimeAccountType.CUSTOMER, customerId));
    }
}
