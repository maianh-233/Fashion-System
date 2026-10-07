package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.auth.RegisterCustomerRequest;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAccount;
import com.fashionsystem.fashion_system.entity.CustomerMembershipStatus;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import com.fashionsystem.fashion_system.entity.SocialProvider;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.util.PhoneNormalizer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;

class CustomerAccountLinkServiceTest {
    private CustomerRepository customers;
    private CustomerAccountRepository accounts;
    private CustomerTierRepository tiers;
    private CustomerTierAssignmentRepository assignments;
    private PasswordEncoder encoder;
    private CustomerCodeGenerator codeGenerator;
    private CustomerAccountLinkService service;

    @BeforeEach
    void setUp() {
        customers = mock(CustomerRepository.class);
        accounts = mock(CustomerAccountRepository.class);
        tiers = mock(CustomerTierRepository.class);
        assignments = mock(CustomerTierAssignmentRepository.class);
        encoder = mock(PasswordEncoder.class);
        codeGenerator = mock(CustomerCodeGenerator.class);
        when(tiers.findByCode("REGULAR")).thenReturn(Optional.of(
                CustomerTier.builder().id(UUID.randomUUID()).code("REGULAR").name("Regular").build()));
        when(customers.save(any(Customer.class))).thenAnswer(call -> {
            Customer customer = call.getArgument(0);
            if (customer.getId() == null) customer.setId(UUID.randomUUID());
            return customer;
        });
        when(accounts.save(any(CustomerAccount.class))).thenAnswer(call -> {
            CustomerAccount account = call.getArgument(0);
            if (account.getId() == null) account.setId(UUID.randomUUID());
            return account;
        });
        when(encoder.encode("Password1!")).thenReturn("hash");
        service = new CustomerAccountLinkService(
                customers, accounts, tiers, assignments,
                codeGenerator, new PhoneNormalizer(), encoder);
        when(codeGenerator.nextCode()).thenReturn("CUS000001");
    }

    @Test
    void accountLinkingInvalidatesCachedPhoneLookups() throws Exception {
        CacheEvict register = CustomerAccountLinkService.class
                .getMethod("register", RegisterCustomerRequest.class)
                .getAnnotation(CacheEvict.class);
        CacheEvict social = CustomerAccountLinkService.class
                .getMethod("linkVerifiedSocial", SocialProfile.class)
                .getAnnotation(CacheEvict.class);

        assertThat(register).isNotNull();
        assertThat(register.cacheNames()).containsExactly(CacheNames.CUSTOMER_PHONE_LOOKUP);
        assertThat(register.allEntries()).isTrue();
        assertThat(social).isNotNull();
        assertThat(social.cacheNames()).containsExactly(CacheNames.CUSTOMER_PHONE_LOOKUP);
        assertThat(social.allEntries()).isTrue();
    }

    @Test
    void createsWebsiteCustomerAndRegularTierWhenPhoneDoesNotExist() {
        RegisterCustomerRequest request = request("0901234567", "web@example.com");

        CustomerAccountLinkService.LinkedAccount linked = service.register(request);

        assertThat(linked.customer().getSource()).isEqualTo(CustomerSource.WEBSITE);
        assertThat(linked.customer().getOriginStoreId()).isNull();
        assertThat(linked.customer().getMembershipStatus()).isEqualTo(CustomerMembershipStatus.MEMBER);
        assertThat(linked.customer().getNormalizedPhone()).isEqualTo("+84901234567");
        assertThat(linked.account().getCustomerId()).isEqualTo(linked.customer().getId());
        assertThat(linked.createdCustomer()).isTrue();
        verify(assignments).save(any(CustomerTierAssignment.class));
    }

    @Test
    void linksExistingStoreMemberWithoutChangingOrigin() {
        UUID storeId = UUID.randomUUID();
        Customer member = Customer.builder()
                .id(UUID.randomUUID()).customerCode("CUS000099").fullName("Store Member")
                .phone("0901234567").normalizedPhone("+84901234567")
                .source(CustomerSource.STORE).membershipStatus(CustomerMembershipStatus.MEMBER)
                .originStoreId(storeId).active(true).build();
        when(customers.findByNormalizedPhone("+84901234567")).thenReturn(Optional.of(member));

        CustomerAccountLinkService.LinkedAccount linked = service.register(request("0901234567", "web@example.com"));

        assertThat(linked.customer()).isSameAs(member);
        assertThat(member.getSource()).isEqualTo(CustomerSource.STORE);
        assertThat(member.getOriginStoreId()).isEqualTo(storeId);
        assertThat(linked.createdCustomer()).isFalse();
    }

    @Test
    void rejectsWhenPhoneAndEmailBelongToDifferentCustomers() {
        Customer byPhone = Customer.builder().id(UUID.randomUUID()).normalizedPhone("+84901234567").build();
        Customer byEmail = Customer.builder().id(UUID.randomUUID()).email("web@example.com").build();
        when(customers.findByNormalizedPhone("+84901234567")).thenReturn(Optional.of(byPhone));
        when(customers.findByEmailIgnoreCase("web@example.com")).thenReturn(Optional.of(byEmail));

        assertThatThrownBy(() -> service.register(request("0901234567", "web@example.com")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rejectsAlreadyLinkedCustomer() {
        Customer member = Customer.builder().id(UUID.randomUUID()).normalizedPhone("+84901234567").build();
        when(customers.findByNormalizedPhone("+84901234567")).thenReturn(Optional.of(member));
        when(accounts.existsByCustomerId(member.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.register(request("0901234567", "web@example.com")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void verifiedSocialEmailLinksUnlinkedStoreMember() {
        Customer member = Customer.builder()
                .id(UUID.randomUUID()).source(CustomerSource.STORE)
                .originStoreId(UUID.randomUUID()).email("social@example.com").active(true).build();
        when(customers.findByEmailIgnoreCase("social@example.com")).thenReturn(Optional.of(member));

        CustomerAccountLinkService.LinkedAccount linked = service.linkVerifiedSocial(
                new SocialProfile(SocialProvider.GOOGLE, "subject", "social@example.com", "Social User", null));

        assertThat(linked.customer()).isSameAs(member);
        assertThat(linked.account().getPasswordHash()).isNull();
        assertThat(linked.customer().getSource()).isEqualTo(CustomerSource.STORE);
    }

    private RegisterCustomerRequest request(String phone, String email) {
        return new RegisterCustomerRequest("customer", email, phone, "Password1!", "Customer Name");
    }
}
