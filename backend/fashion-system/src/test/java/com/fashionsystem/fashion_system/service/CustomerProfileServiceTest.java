package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.CustomerProfileDtos;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.entity.LoyaltyAccount;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.LoyaltyAccountRepository;
import com.fashionsystem.fashion_system.util.PhoneNormalizer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class CustomerProfileServiceTest {
    final CustomerRepository customers = mock(CustomerRepository.class);
    final CustomerAccountRepository accounts = mock(CustomerAccountRepository.class);
    final CustomerTierRepository tiers = mock(CustomerTierRepository.class);
    final LoyaltyAccountRepository loyalty = mock(LoyaltyAccountRepository.class);
    final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final CustomerProfileService service = new CustomerProfileService(customers, accounts, tiers, loyalty, encoder, new PhoneNormalizer());
    final UUID id = UUID.randomUUID();
    Customer customer;
    CustomerAccount account;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(id).customerCode("CUS000001").fullName("Lan").phone("0900000001")
                .normalizedPhone("+84900000001").email("lan@example.invalid").build();
        account = CustomerAccount.builder().id(UUID.randomUUID()).customerId(id).username("lan")
                .loginEmail("lan@example.invalid").passwordHash(encoder.encode("Matkhau@123")).failedLoginAttempts(2).build();
        when(customers.findById(id)).thenReturn(Optional.of(customer));
        when(customers.findByIdForUpdate(id)).thenReturn(Optional.of(customer));
        when(accounts.findByCustomerId(id)).thenReturn(Optional.of(account));
        when(accounts.findByCustomerIdForUpdate(id)).thenReturn(Optional.of(account));
        when(accounts.findByLoginEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(tiers.findCurrentByCustomerId(id)).thenReturn(Optional.of(CustomerTier.builder().code("REGULAR").name("Regular").build()));
        when(loyalty.findById(id)).thenReturn(Optional.of(LoyaltyAccount.builder().customerId(id).pointsBalance(350).build()));
    }

    CustomerProfileDtos.UpdateProfile update(String phone, String email, String password) {
        return new CustomerProfileDtos.UpdateProfile("Trần Thị Lan", phone, email, null, "FEMALE", password);
    }

    @Test
    void profileShowsTierPointsAndWhetherAPasswordIsSet() {
        var profile = service.profile(id);
        assertThat(profile.tierName()).isEqualTo("Regular");
        assertThat(profile.points()).isEqualTo(350);
        assertThat(profile.hasPassword()).isTrue();
        assertThat(profile.username()).isEqualTo("lan");
    }

    @Test
    void updatingNameAndPhoneDoesNotNeedThePassword() {
        service.update(id, update("0912 345 678", "lan@example.invalid", null));
        assertThat(customer.getFullName()).isEqualTo("Trần Thị Lan");
        assertThat(customer.getNormalizedPhone()).isEqualTo("+84912345678");
        verify(accounts, never()).save(any());
    }

    @Test
    void phoneUsedByAnotherCustomerIsRejected() {
        when(customers.existsByNormalizedPhoneAndIdNot("+84912345678", id)).thenReturn(true);
        assertThatThrownBy(() -> service.update(id, update("0912345678", "lan@example.invalid", null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Số điện thoại");
    }

    @Test
    void changingEmailNeedsTheCurrentPasswordAndUpdatesTheLoginEmail() {
        assertThatThrownBy(() -> service.update(id, update("0900000001", "new@example.invalid", null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("mật khẩu");
        assertThatThrownBy(() -> service.update(id, update("0900000001", "new@example.invalid", "sai")))
                .isInstanceOf(BusinessException.class);

        service.update(id, update("0900000001", "NEW@example.invalid", "Matkhau@123"));
        assertThat(customer.getEmail()).isEqualTo("new@example.invalid");
        assertThat(account.getLoginEmail()).isEqualTo("new@example.invalid");
    }

    @Test
    void emailUsedByAnotherAccountIsRejected() {
        when(accounts.findByLoginEmailIgnoreCase("taken@example.invalid"))
                .thenReturn(Optional.of(CustomerAccount.builder().customerId(UUID.randomUUID()).build()));
        assertThatThrownBy(() -> service.update(id, update("0900000001", "taken@example.invalid", "Matkhau@123")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Email");
    }

    @Test
    void changingPasswordChecksTheCurrentOneAndClearsLockout() {
        assertThatThrownBy(() -> service.changePassword(id, new CustomerProfileDtos.ChangePassword("sai", "MoiHon@123")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("hiện tại");
        assertThatThrownBy(() -> service.changePassword(id, new CustomerProfileDtos.ChangePassword("Matkhau@123", "Matkhau@123")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("khác");

        service.changePassword(id, new CustomerProfileDtos.ChangePassword("Matkhau@123", "MoiHon@123"));
        assertThat(encoder.matches("MoiHon@123", account.getPasswordHash())).isTrue();
        assertThat(account.getFailedLoginAttempts()).isZero();
    }

    @Test
    void socialAccountWithoutPasswordCanSetOneAndChangeEmailFreely() {
        account.setPasswordHash(null);
        service.update(id, update("0900000001", "google@example.invalid", null));
        assertThat(account.getLoginEmail()).isEqualTo("google@example.invalid");
        service.changePassword(id, new CustomerProfileDtos.ChangePassword(null, "DatLanDau@1"));
        assertThat(encoder.matches("DatLanDau@1", account.getPasswordHash())).isTrue();
    }
}
