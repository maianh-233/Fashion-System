package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CustomerAddressDto;
import com.fashionsystem.fashion_system.entity.CustomerAddress;
import com.fashionsystem.fashion_system.entity.CustomerAddressSource;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CustomerAddressMapper;
import com.fashionsystem.fashion_system.repository.CustomerAddressRepository;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ địa chỉ thuộc sở hữu khách hàng. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("CUSTOMER")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerAddressService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "receiverName", "province", "district", "ward", "isDefault",
            "addressType", "createdAt", "updatedAt");

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerAddressMapper addressMapper;
    private final CustomerAccessService customerAccess;

    public record Access(UUID staffId, boolean customerSelf) {
        public static Access staff(UUID staffId) { return new Access(staffId, false); }
        public static Access customer() { return new Access(null, true); }
    }

    /**
     * Tạo địa chỉ mới cho khách hàng.
     */
    @Transactional
    public CustomerAddressDto create(UUID customerId, CustomerAddressDto request, Access access) {
        Customer customer = lockCustomer(customerId);
        authorizeEdit(access, customer);
        CustomerAddress entity = addressMapper.toEntity(request);
        entity.setCustomerId(customerId);
        entity.setManagementSource(access.customerSelf()
                ? CustomerAddressSource.WEB : CustomerAddressSource.STORE);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefault(customerId, LocalDateTime.now());
            entity.setIsDefault(Boolean.TRUE);
        }
        return addressMapper.toDto(addressRepository.save(entity));
    }

    /**
     * Lấy chi tiết một địa chỉ thuộc khách hàng.
     */
    @Transactional(readOnly = true)
    public CustomerAddressDto getById(UUID customerId, UUID addressId, Access access) {
        authorizeView(access, requireCustomer(customerId));
        return addressMapper.toDto(requireAddress(customerId, addressId));
    }

    /**
     * Lấy danh sách địa chỉ của khách hàng theo phân trang và sắp xếp.
     */
    @Transactional(readOnly = true)
    public Page<CustomerAddressDto> getList(UUID customerId, Pageable pageable, Access access) {
        authorizeView(access, requireCustomer(customerId));
        validateSort(pageable);
        return addressRepository.findAllByCustomerId(customerId, pageable).map(addressMapper::toDto);
    }

    /**
     * Cập nhật một địa chỉ thuộc khách hàng.
     */
    @Transactional
    public CustomerAddressDto update(UUID customerId, UUID addressId, CustomerAddressDto request, Access access) {
        Customer customer = lockCustomer(customerId);
        authorizeEdit(access, customer);
        CustomerAddress entity = requireAddress(customerId, addressId);
        requireManagedByCaller(access, entity);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefault(customerId, LocalDateTime.now());
            entity = requireAddress(customerId, addressId);
            entity.setIsDefault(Boolean.TRUE);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            entity.setIsDefault(Boolean.FALSE);
        }
        addressMapper.updateEntity(request, entity);
        return addressMapper.toDto(addressRepository.save(entity));
    }

    /**
     * Xóa một địa chỉ thuộc khách hàng.
     */
    @Transactional
    public void delete(UUID customerId, UUID addressId, Access access) {
        Customer customer = lockCustomer(customerId);
        authorizeEdit(access, customer);
        CustomerAddress entity = requireAddress(customerId, addressId);
        requireManagedByCaller(access, entity);
        addressRepository.delete(entity);
    }

    /**
     * Lấy địa chỉ mặc định của khách hàng.
     */
    @Transactional(readOnly = true)
    public CustomerAddressDto getDefault(UUID customerId, Access access) {
        authorizeView(access, requireCustomer(customerId));
        return addressMapper.toDto(addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng chưa có địa chỉ mặc định")));
    }

    /**
     * Thiết lập một địa chỉ làm địa chỉ mặc định duy nhất của khách hàng.
     */
    @Transactional
    public CustomerAddressDto setDefault(UUID customerId, UUID addressId, Access access) {
        Customer customer = lockCustomer(customerId);
        authorizeEdit(access, customer);
        requireManagedByCaller(access, requireAddress(customerId, addressId));
        LocalDateTime now = LocalDateTime.now();
        addressRepository.clearDefault(customerId, now);
        CustomerAddress entity = requireAddress(customerId, addressId);
        entity.setIsDefault(Boolean.TRUE);
        entity.setUpdatedAt(now);
        return addressMapper.toDto(addressRepository.save(entity));
    }

    /**
     * Gỡ trạng thái mặc định khỏi một địa chỉ của khách hàng.
     */
    @Transactional
    public CustomerAddressDto removeDefault(UUID customerId, UUID addressId, Access access) {
        Customer customer = lockCustomer(customerId);
        authorizeEdit(access, customer);
        CustomerAddress entity = requireAddress(customerId, addressId);
        requireManagedByCaller(access, entity);
        entity.setIsDefault(Boolean.FALSE);
        entity.setUpdatedAt(LocalDateTime.now());
        return addressMapper.toDto(addressRepository.save(entity));
    }

    private Customer lockCustomer(UUID customerId) {
        return customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
    }

    private Customer requireCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
    }

    private CustomerAddress requireAddress(UUID customerId, UUID addressId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> BusinessException.notFound("Địa chỉ khách hàng không tồn tại"));
    }

    private void authorizeView(Access caller, Customer customer) {
        if (!caller.customerSelf()) customerAccess.requireView(caller.staffId(), customer);
    }

    private void authorizeEdit(Access caller, Customer customer) {
        if (!caller.customerSelf()) customerAccess.requireEdit(caller.staffId(), customer);
    }

    private void requireManagedByCaller(Access caller, CustomerAddress address) {
        CustomerAddressSource allowed = caller.customerSelf()
                ? CustomerAddressSource.WEB : CustomerAddressSource.STORE;
        if (address.getManagementSource() != allowed) {
            throw BusinessException.forbidden(caller.customerSelf()
                    ? "Không thể sửa địa chỉ do cửa hàng quản lý"
                    : "Nhân viên chỉ được xem địa chỉ do tài khoản website quản lý");
        }
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp địa chỉ không hợp lệ");
    }
}
