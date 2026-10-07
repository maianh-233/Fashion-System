package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CustomerAddressDto;
import com.fashionsystem.fashion_system.service.CustomerAddressService;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API địa chỉ lồng dưới khách hàng sở hữu. */
@RestController
@RequestMapping("/api/customers/{customerId}/addresses")
@PreAuthorize("@ownershipSecurity.isCustomerOrEmployee(#customerId)")
@RequiredArgsConstructor
public class CustomerAddressController {
    private final CustomerAddressService addressService;

    /**
     * Tạo địa chỉ cho khách hàng.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAddressDto create(
            Authentication authentication, @PathVariable UUID customerId,
            @Valid @RequestBody CustomerAddressDto request) {
        return addressService.create(customerId, request, access(authentication, customerId));
    }

    /**
     * Lấy chi tiết địa chỉ của khách hàng.
     */
    @GetMapping("/{addressId}")
    public CustomerAddressDto getById(
            Authentication authentication, @PathVariable UUID customerId, @PathVariable UUID addressId) {
        return addressService.getById(customerId, addressId, access(authentication, customerId));
    }

    /**
     * Lấy danh sách địa chỉ của khách hàng theo phân trang.
     */
    @GetMapping
    public Page<CustomerAddressDto> getList(
            Authentication authentication,
            @PathVariable UUID customerId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return addressService.getList(customerId, pageable, access(authentication, customerId));
    }

    /**
     * Cập nhật địa chỉ của khách hàng.
     */
    @PutMapping("/{addressId}")
    public CustomerAddressDto update(
            Authentication authentication,
            @PathVariable UUID customerId,
            @PathVariable UUID addressId,
            @Valid @RequestBody CustomerAddressDto request) {
        return addressService.update(customerId, addressId, request, access(authentication, customerId));
    }

    /**
     * Xóa địa chỉ của khách hàng.
     */
    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID customerId, @PathVariable UUID addressId) {
        addressService.delete(customerId, addressId, access(authentication, customerId));
    }

    /**
     * Lấy địa chỉ mặc định của khách hàng.
     */
    @GetMapping("/default")
    public CustomerAddressDto getDefault(Authentication authentication, @PathVariable UUID customerId) {
        return addressService.getDefault(customerId, access(authentication, customerId));
    }

    /**
     * Thiết lập địa chỉ mặc định duy nhất của khách hàng.
     */
    @PutMapping("/{addressId}/default")
    public CustomerAddressDto setDefault(
            Authentication authentication, @PathVariable UUID customerId, @PathVariable UUID addressId) {
        return addressService.setDefault(customerId, addressId, access(authentication, customerId));
    }

    /**
     * Gỡ trạng thái mặc định khỏi địa chỉ của khách hàng.
     */
    @DeleteMapping("/{addressId}/default")
    public CustomerAddressDto removeDefault(
            Authentication authentication, @PathVariable UUID customerId, @PathVariable UUID addressId) {
        return addressService.removeDefault(customerId, addressId, access(authentication, customerId));
    }

    private CustomerAddressService.Access access(Authentication authentication, UUID customerId) {
        if (authentication.getPrincipal() instanceof AuthenticatedCustomer customer
                && customer.customerId().equals(customerId)) {
            return CustomerAddressService.Access.customer();
        }
        return CustomerAddressService.Access.staff(
                ((AuthenticatedUser) authentication.getPrincipal()).userId());
    }
}
