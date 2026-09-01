package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CustomerAddressDto;
import com.fashionsystem.fashion_system.entity.CustomerAddress;
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
@RequiredArgsConstructor
public class CustomerAddressService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "receiverName", "province", "district", "ward", "isDefault",
            "addressType", "createdAt", "updatedAt");

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerAddressMapper addressMapper;

    /**
     * Tạo địa chỉ mới cho khách hàng.
     */
    @Transactional
    public CustomerAddressDto create(UUID customerId, CustomerAddressDto request) {
        lockCustomer(customerId);
        CustomerAddress entity = addressMapper.toEntity(request);
        entity.setCustomerId(customerId);
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
    public CustomerAddressDto getById(UUID customerId, UUID addressId) {
        requireCustomer(customerId);
        return addressMapper.toDto(requireAddress(customerId, addressId));
    }

    /**
     * Lấy danh sách địa chỉ của khách hàng theo phân trang và sắp xếp.
     */
    @Transactional(readOnly = true)
    public Page<CustomerAddressDto> getList(UUID customerId, Pageable pageable) {
        requireCustomer(customerId);
        validateSort(pageable);
        return addressRepository.findAllByCustomerId(customerId, pageable).map(addressMapper::toDto);
    }

    /**
     * Cập nhật một địa chỉ thuộc khách hàng.
     */
    @Transactional
    public CustomerAddressDto update(UUID customerId, UUID addressId, CustomerAddressDto request) {
        lockCustomer(customerId);
        CustomerAddress entity = requireAddress(customerId, addressId);
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
    public void delete(UUID customerId, UUID addressId) {
        lockCustomer(customerId);
        addressRepository.delete(requireAddress(customerId, addressId));
    }

    /**
     * Lấy địa chỉ mặc định của khách hàng.
     */
    @Transactional(readOnly = true)
    public CustomerAddressDto getDefault(UUID customerId) {
        requireCustomer(customerId);
        return addressMapper.toDto(addressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng chưa có địa chỉ mặc định")));
    }

    /**
     * Thiết lập một địa chỉ làm địa chỉ mặc định duy nhất của khách hàng.
     */
    @Transactional
    public CustomerAddressDto setDefault(UUID customerId, UUID addressId) {
        lockCustomer(customerId);
        requireAddress(customerId, addressId);
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
    public CustomerAddressDto removeDefault(UUID customerId, UUID addressId) {
        lockCustomer(customerId);
        CustomerAddress entity = requireAddress(customerId, addressId);
        entity.setIsDefault(Boolean.FALSE);
        entity.setUpdatedAt(LocalDateTime.now());
        return addressMapper.toDto(addressRepository.save(entity));
    }

    private void lockCustomer(UUID customerId) {
        customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
    }

    private void requireCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw BusinessException.notFound("Khách hàng không tồn tại");
        }
    }

    private CustomerAddress requireAddress(UUID customerId, UUID addressId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> BusinessException.notFound("Địa chỉ khách hàng không tồn tại"));
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp địa chỉ không hợp lệ");
    }
}
