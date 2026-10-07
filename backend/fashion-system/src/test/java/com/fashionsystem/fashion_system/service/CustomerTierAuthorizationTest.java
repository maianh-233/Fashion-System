package com.fashionsystem.fashion_system.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import com.fashionsystem.fashion_system.dto.CustomerTierAssignmentDto;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.mapper.CustomerMapper;
import com.fashionsystem.fashion_system.mapper.CustomerTierAssignmentMapper;
import com.fashionsystem.fashion_system.mapper.CustomerTierMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTierAuthorizationTest {
    @Test
    void tierMutationRequiresGlobalCustomerTierPermission() {
        UUID actor = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).build();
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerTierRepository tiers = mock(CustomerTierRepository.class);
        CustomerTierAssignmentRepository assignments = mock(CustomerTierAssignmentRepository.class);
        CustomerAccessService access = mock(CustomerAccessService.class);
        when(customers.findByIdForUpdate(customerId)).thenReturn(Optional.of(customer));
        when(assignments.expireCurrent(eq(customerId), any())).thenReturn(1);
        CustomerTierAssignmentService service = new CustomerTierAssignmentService(
                customers, tiers, assignments, mock(CustomerTierAssignmentMapper.class),
                mock(CustomerTierMapper.class), mock(CustomerMapper.class), access);

        service.expireCurrentTier(actor, customerId);

        verify(access).requireTier(actor, customer);
    }
}
