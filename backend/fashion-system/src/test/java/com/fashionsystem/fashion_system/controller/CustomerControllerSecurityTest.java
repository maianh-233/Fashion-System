package com.fashionsystem.fashion_system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.customer.CreateStoreCustomerRequest;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerMembershipStatus;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.service.CustomerAccessService;
import com.fashionsystem.fashion_system.service.CustomerCodeGenerator;
import com.fashionsystem.fashion_system.service.CustomerService;
import com.fashionsystem.fashion_system.util.PhoneNormalizer;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerControllerSecurityTest {
    @Test
    void staffCreateIgnoresAuthFieldsAndAssignsStoreBusinessDefaults() {
        UUID actor = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerTierRepository tiers = mock(CustomerTierRepository.class);
        CustomerTierAssignmentRepository assignments = mock(CustomerTierAssignmentRepository.class);
        StoreRepository stores = mock(StoreRepository.class);
        CustomerAccessService access = mock(CustomerAccessService.class);
        CustomerCodeGenerator codes = mock(CustomerCodeGenerator.class);
        when(access.resolveCreateStore(actor, null)).thenReturn(storeId);
        when(codes.nextCode()).thenReturn("CUS000001");
        when(stores.existsById(storeId)).thenReturn(true);
        when(customers.save(any(Customer.class))).thenAnswer(call -> {
            Customer customer = call.getArgument(0); customer.setId(UUID.randomUUID()); return customer;
        });
        when(tiers.findByCode("REGULAR")).thenReturn(Optional.of(
                CustomerTier.builder().id(UUID.randomUUID()).code("REGULAR").build()));
        CustomerService service = new CustomerService(
                customers, mock(CustomerAccountRepository.class), tiers, assignments,
                stores, access, codes, new PhoneNormalizer());
        CreateStoreCustomerRequest request = new CreateStoreCustomerRequest(
                "Store Member", "0901234567", "member@example.com", LocalDate.of(1995, 1, 1),
                "FEMALE", "Member note", null);

        var result = service.create(actor, request);

        assertThat(result.source()).isEqualTo(CustomerSource.STORE);
        assertThat(result.originStoreId()).isEqualTo(storeId);
        assertThat(result.membershipStatus()).isEqualTo(CustomerMembershipStatus.MEMBER);
        assertThat(result.tier()).isEqualTo("REGULAR");
        assertThat(result.hasWebAccount()).isFalse();
        verify(assignments).save(any());
    }
}
