package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.AuthProperties;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterCustomerRequest;
import com.fashionsystem.fashion_system.dto.auth.SocialLoginRequest;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerMembershipStatus;
import com.fashionsystem.fashion_system.entity.CustomerSocialAccount;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import com.fashionsystem.fashion_system.entity.SocialProvider;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.AuthResponseMapper;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerSocialAccountRepository;
import com.fashionsystem.fashion_system.repository.RevokedTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class CustomerAuthServiceTest {
    private CustomerRepository customers;
    private CustomerAccountRepository accounts;
    private CustomerSocialAccountRepository socialAccounts;
    private PasswordEncoder encoder;
    private JwtService jwt;
    private CustomerAccountLinkService linker;
    private RevokedTokenRepository revokedTokens;
    private CustomerAuthService service;

    @BeforeEach
    void setUp() {
        customers = mock(CustomerRepository.class);
        accounts = mock(CustomerAccountRepository.class);
        socialAccounts = mock(CustomerSocialAccountRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);
        linker = mock(CustomerAccountLinkService.class);
        revokedTokens = mock(RevokedTokenRepository.class);
        AuthProperties properties = new AuthProperties();
        properties.setMaxFailedAttempts(5);
        properties.setLoginLockDurationSeconds(60);
        service = new CustomerAuthService(
                customers, accounts, socialAccounts, encoder, jwt, List.of(), linker,
                new AuthResponseMapper(), properties, revokedTokens);
        when(jwt.getExpirationMs()).thenReturn(1_800_000L);
    }

    @Test
    void successfulPasswordLoginUsesCustomerAccountAndBusinessCustomer() {
        Customer customer = activeCustomer();
        CustomerAccount account = passwordAccount(customer.getId());
        account.setFailedLoginAttempts(2);
        when(accounts.findByUsernameForUpdate("customer")).thenReturn(Optional.of(account));
        when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(encoder.matches("correct", "hash")).thenReturn(true);
        when(jwt.generateCustomerToken(customer, account)).thenReturn("customer-jwt");

        var response = service.login(new LoginRequest(" CUSTOMER ", "correct"));

        assertThat(response.token()).isEqualTo("customer-jwt");
        assertThat(response.customer().id()).isEqualTo(customer.getId());
        assertThat(response.customer().username()).isEqualTo(account.getUsername());
        assertThat(account.getFailedLoginAttempts()).isZero();
        verify(accounts).save(account);
    }

    @Test
    void fifthWrongPasswordLocksOnlyCustomerAccount() {
        Customer customer = activeCustomer();
        CustomerAccount account = passwordAccount(customer.getId());
        account.setFailedLoginAttempts(4);
        when(accounts.findByUsernameForUpdate("customer")).thenReturn(Optional.of(account));
        when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(encoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("customer", "wrong")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getHeaders().getFirst("Retry-After")).isEqualTo("60");
                });

        assertThat(account.getLoginLockedUntil()).isAfter(LocalDateTime.now().plusSeconds(58));
        verify(accounts).save(account);
        verify(customers, never()).save(any(Customer.class));
    }

    @Test
    void registerDelegatesToTransactionalCustomerAccountLinker() {
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "customer", "web@example.com", "0901234567", "Password1!", "Web Customer");
        Customer customer = activeCustomer();
        CustomerAccount account = passwordAccount(customer.getId());
        when(linker.register(request)).thenReturn(
                new CustomerAccountLinkService.LinkedAccount(customer, account, true));
        when(jwt.generateCustomerToken(customer, account)).thenReturn("customer-jwt");

        var response = service.register(request);

        assertThat(response.customer().id()).isEqualTo(customer.getId());
        verify(linker).register(request);
    }

    @Test
    void repeatedSocialLoginReusesProviderSubjectWithoutCreatingCustomer() {
        Customer customer = activeCustomer();
        CustomerAccount account = passwordAccount(customer.getId());
        CustomerSocialAccount social = CustomerSocialAccount.builder()
                .customerId(customer.getId()).provider("GOOGLE").providerUserId("subject").build();
        SocialIdentityVerifier verifier = mock(SocialIdentityVerifier.class);
        SocialLoginRequest request = new SocialLoginRequest(SocialProvider.GOOGLE, "id-token");
        SocialProfile profile = new SocialProfile(
                SocialProvider.GOOGLE, "subject", "web@example.com", "Web Customer", null);
        when(verifier.supports(SocialProvider.GOOGLE)).thenReturn(true);
        when(verifier.verify("id-token")).thenReturn(profile);
        when(socialAccounts.findByProviderAndProviderUserId("GOOGLE", "subject"))
                .thenReturn(Optional.of(social));
        when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(accounts.findByCustomerId(customer.getId())).thenReturn(Optional.of(account));
        when(jwt.generateCustomerToken(customer, account)).thenReturn("customer-jwt");
        service = new CustomerAuthService(
                customers, accounts, socialAccounts, encoder, jwt, List.of(verifier), linker,
                new AuthResponseMapper(), new AuthProperties(), revokedTokens);

        service.loginSocial(request);

        verify(linker, never()).linkVerifiedSocial(any());
        verify(socialAccounts).save(social);
    }

    @Test
    void customerLogoutRevokesOnlyCustomerAccessToken() {
        UUID tokenId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(jwt.extractAccountType("customer-token")).thenReturn(JwtService.ACCOUNT_TYPE_CUSTOMER);
        when(jwt.extractTokenId("customer-token")).thenReturn(tokenId);
        when(jwt.extractCustomerId("customer-token")).thenReturn(customerId);
        when(jwt.extractExpiration("customer-token")).thenReturn(LocalDateTime.now().plusMinutes(5));

        service.logout("customer-token");

        verify(revokedTokens).save(org.mockito.ArgumentMatchers.argThat(token ->
                token.getTokenId().equals(tokenId)
                        && token.getAccountId().equals(customerId)
                        && JwtService.ACCOUNT_TYPE_CUSTOMER.equals(token.getAccountType())));
    }

    private Customer activeCustomer() {
        return Customer.builder()
                .id(UUID.randomUUID()).customerCode("CUS000001").fullName("Customer")
                .email("web@example.com").source(CustomerSource.WEBSITE)
                .membershipStatus(CustomerMembershipStatus.MEMBER).active(true).build();
    }

    private CustomerAccount passwordAccount(UUID customerId) {
        return CustomerAccount.builder()
                .id(UUID.randomUUID()).customerId(customerId).username("customer")
                .loginEmail("web@example.com").passwordHash("hash")
                .failedLoginAttempts(0).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
