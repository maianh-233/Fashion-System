package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.CustomerAddressDto;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerAddress;
import com.fashionsystem.fashion_system.entity.CustomerAddressSource;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CustomerAddressMapper;
import com.fashionsystem.fashion_system.repository.CustomerAddressRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CustomerAddressAuthorizationTest {
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CustomerAddressRepository addresses = mock(CustomerAddressRepository.class);
    private final CustomerAccessService access = mock(CustomerAccessService.class);
    private CustomerAddressService service;

    @BeforeEach
    void setUp() {
        service = new CustomerAddressService(customers, addresses, new CustomerAddressMapper(), access);
    }

    @Test
    void staffCreatesStoreManagedAddressOnlyAfterCustomerEditCheck() {
        UUID actor = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).build();
        when(customers.findByIdForUpdate(customerId)).thenReturn(Optional.of(customer));
        when(addresses.save(any())).thenAnswer(call -> call.getArgument(0));

        CustomerAddressDto result = service.create(customerId, validRequest(),
                CustomerAddressService.Access.staff(actor));

        verify(access).requireEdit(actor, customer);
        assertThat(result.getManagementSource()).isEqualTo(CustomerAddressSource.STORE);
    }

    @Test
    void staffCannotUpdateWebManagedAddress() {
        UUID actor = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        Customer customer = Customer.builder().id(customerId).build();
        CustomerAddress address = CustomerAddress.builder().id(addressId).customerId(customerId)
                .managementSource(CustomerAddressSource.WEB).build();
        when(customers.findByIdForUpdate(customerId)).thenReturn(Optional.of(customer));
        when(addresses.findByIdAndCustomerId(addressId, customerId)).thenReturn(Optional.of(address));

        assertThatThrownBy(() -> service.update(customerId, addressId, validRequest(),
                CustomerAddressService.Access.staff(actor)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private CustomerAddressDto validRequest() {
        return CustomerAddressDto.builder().receiverName("Customer").receiverPhone("0901234567")
                .addressLine("1 Main Street").addressType("HOME").build();
    }
}
