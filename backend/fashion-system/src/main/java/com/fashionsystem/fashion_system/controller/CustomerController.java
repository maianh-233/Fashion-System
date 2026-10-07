package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CustomerDto;
import com.fashionsystem.fashion_system.dto.customer.CreateStoreCustomerRequest;
import com.fashionsystem.fashion_system.dto.customer.CustomerDetailResponse;
import com.fashionsystem.fashion_system.dto.customer.CustomerListResponse;
import com.fashionsystem.fashion_system.dto.customer.CustomerLookupResponse;
import com.fashionsystem.fashion_system.dto.customer.UpdateCustomerRequest;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.CustomerService;
import com.fashionsystem.fashion_system.service.StoreAccessService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService service;

    @GetMapping
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public Page<CustomerListResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "false") boolean noStore,
            @RequestParam(required = false) CustomerSource source,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) Boolean hasWebAccount,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.getList(actor(authentication), search, storeId, noStore, source,
                tier, hasWebAccount, active, pageable);
    }

    @GetMapping("/store-options")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public List<StoreAccessService.StoreOption> stores(Authentication authentication) {
        return service.storeOptions(actor(authentication));
    }

    @GetMapping("/lookup")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public CustomerLookupResponse lookup(Authentication authentication, @RequestParam String value) {
        return service.lookup(actor(authentication), value);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerDto profile(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return service.getProfile(customer.customerId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public CustomerDetailResponse detail(Authentication authentication, @PathVariable UUID id) {
        return service.getById(actor(authentication), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public CustomerDetailResponse create(
            Authentication authentication, @Valid @RequestBody CreateStoreCustomerRequest request) {
        return service.create(actor(authentication), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public CustomerDetailResponse update(
            Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return service.update(actor(authentication), id, request);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public CustomerDetailResponse activate(Authentication authentication, @PathVariable UUID id) {
        return service.setActive(actor(authentication), id, true);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public CustomerDetailResponse deactivate(Authentication authentication, @PathVariable UUID id) {
        return service.setActive(actor(authentication), id, false);
    }

    private UUID actor(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
