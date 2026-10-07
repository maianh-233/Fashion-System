package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.CustomerDto;
import com.fashionsystem.fashion_system.dto.customer.CreateStoreCustomerRequest;
import com.fashionsystem.fashion_system.dto.customer.CustomerDetailResponse;
import com.fashionsystem.fashion_system.dto.customer.CustomerListResponse;
import com.fashionsystem.fashion_system.dto.customer.CustomerLookupResponse;
import com.fashionsystem.fashion_system.dto.customer.UpdateCustomerRequest;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerMembershipStatus;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CustomerAccountRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.util.PhoneNormalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("CUSTOMER")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerService {
    private static final Set<String> SORTS = Set.of(
            "id", "customerCode", "fullName", "phone", "email", "source", "active", "createdAt");
    private final CustomerRepository customers;
    private final CustomerAccountRepository accounts;
    private final CustomerTierRepository tiers;
    private final CustomerTierAssignmentRepository assignments;
    private final StoreRepository stores;
    private final CustomerAccessService access;
    private final CustomerCodeGenerator codeGenerator;
    private final PhoneNormalizer phoneNormalizer;

    public Page<CustomerListResponse> getList(
            UUID actor, String search, UUID storeId, boolean noStore, CustomerSource source,
            String tier, Boolean hasWebAccount, Boolean active, Pageable pageable) {
        validateSort(pageable);
        CustomerAccessService.QueryScope scope = access.resolveList(actor, storeId, noStore);
        return customers.searchManagement(trim(search), scope.storeId(), scope.noStore(), source,
                tier == null ? "" : tier.trim().toUpperCase(Locale.ROOT), hasWebAccount, active, pageable);
    }

    public CustomerDetailResponse getById(UUID actor, UUID id) {
        Customer customer = require(id);
        access.requireView(actor, customer);
        return detail(customer);
    }

    public CustomerDto getProfile(UUID customerId) {
        Customer c = require(customerId);
        return CustomerDto.builder().id(c.getId()).customerCode(c.getCustomerCode())
                .email(c.getEmail()).phone(c.getPhone()).fullName(c.getFullName())
                .dateOfBirth(c.getDateOfBirth()).gender(c.getGender()).avatar(c.getAvatar())
                .active(c.getActive()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CUSTOMER_PHONE_LOOKUP, allEntries = true)
    public CustomerDetailResponse create(UUID actor, CreateStoreCustomerRequest request) {
        UUID storeId = access.resolveCreateStore(actor, request.originStoreId());
        String normalizedPhone = normalizePhone(request.phone(), true);
        ensureIdentityAvailable(normalizedPhone, normalizeEmail(request.email()), null);
        if (!stores.existsById(storeId)) throw BusinessException.notFound("Cửa hàng không tồn tại");
        LocalDateTime now = LocalDateTime.now();
        Customer customer = customers.save(Customer.builder()
                .customerCode(codeGenerator.nextCode()).fullName(request.fullName().trim())
                .phone(request.phone().trim()).normalizedPhone(normalizedPhone)
                .email(normalizeEmail(request.email())).dateOfBirth(request.birthday())
                .gender(request.gender()).note(trimNullable(request.note()))
                .source(CustomerSource.STORE).membershipStatus(CustomerMembershipStatus.MEMBER)
                .originStoreId(storeId).active(true).createdAt(now).updatedAt(now).build());
        CustomerTier regular = tiers.findByCode("REGULAR")
                .orElseThrow(() -> BusinessException.invalidState("Chưa cấu hình hạng REGULAR"));
        assignments.save(CustomerTierAssignment.builder().customerId(customer.getId())
                .tierId(regular.getId()).assignedAt(now).note("Default REGULAR tier").build());
        return detail(customer, regular.getCode());
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CUSTOMER_PHONE_LOOKUP, allEntries = true)
    public CustomerDetailResponse update(UUID actor, UUID id, UpdateCustomerRequest request) {
        Customer customer = customers.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        access.requireEdit(actor, customer);
        String normalizedPhone = normalizePhone(request.phone(), false);
        String email = normalizeEmail(request.email());
        ensureIdentityAvailable(normalizedPhone, email, id);
        customer.setFullName(request.fullName().trim());
        customer.setPhone(trimNullable(request.phone()));
        customer.setNormalizedPhone(normalizedPhone);
        customer.setEmail(email);
        customer.setDateOfBirth(request.birthday());
        customer.setGender(request.gender());
        customer.setNote(trimNullable(request.note()));
        customer.setUpdatedAt(LocalDateTime.now());
        return detail(customers.save(customer));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CUSTOMER_PHONE_LOOKUP, allEntries = true)
    public CustomerDetailResponse setActive(UUID actor, UUID id, boolean active) {
        Customer customer = customers.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        access.requireStatus(actor, customer);
        customer.setActive(active);
        customer.setUpdatedAt(LocalDateTime.now());
        return detail(customers.save(customer));
    }

    public CustomerLookupResponse lookup(UUID actor, String value) {
        UserScope identity = access.requireLookup(actor);
        String normalized = normalizeLookup(value);
        Customer customer = customers.findExactLookup(normalized)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        boolean editable = identity.isGlobal()
                || customer.getSource() == CustomerSource.STORE
                && identity.storeId().equals(customer.getOriginStoreId());
        return lookupResponse(customer, editable);
    }

    @Cacheable(cacheNames = CacheNames.CUSTOMER_PHONE_LOOKUP, key = "#normalizedPhone")
    public CustomerLookupResponse lookupByNormalizedPhone(String normalizedPhone) {
        Customer customer = customers.findByNormalizedPhone(normalizedPhone)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
        return lookupResponse(customer, false);
    }

    public List<StoreAccessService.StoreOption> storeOptions(UUID actor) {
        UserScope identity = access.requireLookup(actor);
        return stores.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(store -> identity.isGlobal() || store.getId().equals(identity.storeId()))
                .map(store -> new StoreAccessService.StoreOption(store.getId(), store.getName()))
                .toList();
    }

    private CustomerDetailResponse detail(Customer customer) {
        String tier = tiers.findCurrentByCustomerId(customer.getId()).map(CustomerTier::getCode).orElse(null);
        return detail(customer, tier);
    }

    private CustomerDetailResponse detail(Customer customer, String tier) {
        String storeName = customer.getOriginStoreId() == null ? null
                : stores.findById(customer.getOriginStoreId()).map(value -> value.getName()).orElse(null);
        return new CustomerDetailResponse(customer.getId(), customer.getCustomerCode(), customer.getFullName(),
                customer.getPhone(), customer.getEmail(), customer.getDateOfBirth(), customer.getGender(),
                customer.getNote(), customer.getSource(), customer.getMembershipStatus(),
                customer.getOriginStoreId(), storeName, tier, accounts.existsByCustomerId(customer.getId()),
                customer.getActive(), customer.getCreatedAt(), customer.getUpdatedAt());
    }

    private CustomerLookupResponse lookupResponse(Customer customer, boolean editable) {
        return new CustomerLookupResponse(customer.getId(), customer.getCustomerCode(), customer.getFullName(),
                customer.getPhone(), customer.getEmail(), customer.getSource(), customer.getOriginStoreId(), editable);
    }

    private Customer require(UUID id) {
        return customers.findById(id).orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
    }

    private void ensureIdentityAvailable(String phone, String email, UUID excludedId) {
        if ((phone != null && excludedId != null && customers.existsByNormalizedPhoneAndIdNot(phone, excludedId))
                || (phone != null && excludedId == null && customers.findByNormalizedPhone(phone).isPresent())) {
            throw BusinessException.conflict("Số điện thoại khách hàng đã tồn tại");
        }
        if ((email != null && excludedId != null && customers.existsByEmailIgnoreCaseAndIdNot(email, excludedId))
                || (email != null && excludedId == null && customers.findByEmailIgnoreCase(email).isPresent())) {
            throw BusinessException.conflict("Email khách hàng đã tồn tại");
        }
    }

    private String normalizePhone(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) throw BusinessException.badRequest("Số điện thoại là bắt buộc");
            return null;
        }
        try { return phoneNormalizer.normalize(value); }
        catch (IllegalArgumentException exception) { throw BusinessException.badRequest(exception.getMessage()); }
    }
    private String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
    private String normalizeLookup(String value) {
        if (value == null || value.isBlank()) throw BusinessException.badRequest("Giá trị tra cứu là bắt buộc");
        String trimmed = value.trim();
        if (trimmed.matches("[+0-9 .-]+")) return normalizePhone(trimmed, true);
        return trimmed.toLowerCase(Locale.ROOT);
    }
    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String trimNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void validateSort(Pageable page) {
        if (page.getSort().stream().anyMatch(order -> !SORTS.contains(order.getProperty()))) {
            throw BusinessException.badRequest("Trường sắp xếp khách hàng không hợp lệ");
        }
    }
}
