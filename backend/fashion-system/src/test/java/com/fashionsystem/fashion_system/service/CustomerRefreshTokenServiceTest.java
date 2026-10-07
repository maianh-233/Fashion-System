package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.CustomerRefreshTokenProperties;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerRefreshToken;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.AuthResponseMapper;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRefreshTokenRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerRefreshTokenServiceTest {
    private CustomerRefreshTokenRepository tokens;
    private CustomerAccountRepository accounts;
    private CustomerRepository customers;
    private JwtService jwt;
    private CustomerRefreshTokenService service;

    @BeforeEach
    void setUp() {
        tokens = mock(CustomerRefreshTokenRepository.class);
        accounts = mock(CustomerAccountRepository.class);
        customers = mock(CustomerRepository.class);
        jwt = mock(JwtService.class);
        CustomerRefreshTokenProperties properties = new CustomerRefreshTokenProperties();
        properties.setExpiration(Duration.ofDays(7));
        service = new CustomerRefreshTokenService(
                tokens, accounts, customers, jwt, new AuthResponseMapper(), properties);
        when(tokens.save(any(CustomerRefreshToken.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void issueAndRotateUseOnlyCustomerAccountPersistence() {
        Customer customer = Customer.builder().id(UUID.randomUUID()).fullName("Customer").active(true).build();
        CustomerAccount account = CustomerAccount.builder()
                .id(UUID.randomUUID()).customerId(customer.getId()).username("customer").build();
        CustomerRefreshToken current = CustomerRefreshToken.builder()
                .id(UUID.randomUUID()).customerAccountId(account.getId()).tokenHash("hash")
                .refreshTokenFamily(UUID.randomUUID()).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(tokens.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));
        when(accounts.findByCustomerId(customer.getId())).thenReturn(Optional.of(account));
        when(accounts.findById(account.getId())).thenReturn(Optional.of(account));
        when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(jwt.generateCustomerToken(customer, account)).thenReturn("access");
        when(jwt.getExpirationMs()).thenReturn(1_800_000L);

        CustomerRefreshTokenService.IssuedToken issued = service.issueForCustomer(customer.getId());
        CustomerRefreshTokenService.RefreshedSession refreshed = service.refresh(issued.value());

        assertThat(issued.value()).isNotBlank();
        assertThat(refreshed.response().token()).isEqualTo("access");
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(current.getRevokedAt()).isNotNull();
        verify(tokens, org.mockito.Mockito.times(2)).save(any(CustomerRefreshToken.class));
    }

    @Test
    void replayedCustomerTokenRevokesOnlyItsCustomerFamily() {
        UUID family = UUID.randomUUID();
        CustomerRefreshToken replayed = CustomerRefreshToken.builder()
                .tokenHash("hash").refreshTokenFamily(family)
                .expiresAt(LocalDateTime.now().plusDays(1)).revokedAt(LocalDateTime.now()).build();
        when(tokens.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(replayed));

        assertThatThrownBy(() -> service.refresh("replayed"))
                .isInstanceOf(BusinessException.class);

        verify(tokens).revokeFamily(any(UUID.class), any(LocalDateTime.class));
    }

    @Test
    void inactiveBusinessCustomerCannotRefresh() {
        Customer customer = Customer.builder().id(UUID.randomUUID()).active(false).build();
        CustomerAccount account = CustomerAccount.builder()
                .id(UUID.randomUUID()).customerId(customer.getId()).username("customer").build();
        CustomerRefreshToken current = CustomerRefreshToken.builder()
                .customerAccountId(account.getId()).tokenHash("hash")
                .refreshTokenFamily(UUID.randomUUID()).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(tokens.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));
        when(accounts.findById(account.getId())).thenReturn(Optional.of(account));
        when(customers.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.refresh("token")).isInstanceOf(BusinessException.class);
    }
}
