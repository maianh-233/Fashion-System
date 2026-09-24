package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CustomerDto;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.Gender;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CustomerMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý khách hàng. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("CUSTOMER")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "username", "email", "phone", "fullName", "dateOfBirth",
            "gender", "active", "locked", "createdAt", "updatedAt");

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    /**
     * Lấy danh sách khách hàng với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CustomerDto> getList(
            String keyword,
            Boolean active,
            Boolean locked,
            Gender gender,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            Pageable pageable) {
        validateDateRange(createdFrom, createdTo);
        validateSort(pageable);
        return customerRepository.search(
                        trimToEmpty(keyword), active, locked,
                        gender == null ? "" : gender.name(), createdFrom, createdTo, pageable)
                .map(customerMapper::toDto);
    }

    /**
     * Lấy chi tiết khách hàng theo ID mà không lộ passwordHash.
     */
    @Transactional(readOnly = true)
    public CustomerDto getById(UUID id) {
        return customerMapper.toDto(requireCustomer(id));
    }

    /**
     * Lấy hồ sơ của khách hàng đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public CustomerDto getProfile(UUID customerId) {
        return customerMapper.toDto(requireCustomer(customerId));
    }

    /**
     * Cập nhật thông tin khách hàng theo ID.
     */
    @Transactional
    public CustomerDto update(UUID id, CustomerDto request) {
        Customer entity = requireCustomer(id);
        ensureUniqueFields(request, id);
        customerMapper.updateEntity(request, entity);
        return customerMapper.toDto(customerRepository.save(entity));
    }

    /**
     * Xóa khách hàng chưa được dữ liệu nghiệp vụ khác tham chiếu.
     */
    @Transactional
    public void delete(UUID id) {
        Customer entity = requireCustomer(id);
        try {
            customerRepository.delete(entity);
            customerRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa khách hàng đang có dữ liệu liên quan");
        }
    }

    /**
     * Kích hoạt tài khoản khách hàng.
     */
    @Transactional
    public CustomerDto activate(UUID id) {
        return updateState(id, true, null);
    }

    /**
     * Vô hiệu hóa tài khoản khách hàng.
     */
    @Transactional
    public CustomerDto deactivate(UUID id) {
        return updateState(id, false, null);
    }

    /**
     * Khóa tài khoản khách hàng.
     */
    @Transactional
    public CustomerDto lock(UUID id) {
        return updateState(id, null, true);
    }

    /**
     * Mở khóa tài khoản khách hàng.
     */
    @Transactional
    public CustomerDto unlock(UUID id) {
        return updateState(id, null, false);
    }

    private CustomerDto updateState(UUID id, Boolean active, Boolean locked) {
        Customer entity = requireCustomer(id);
        if (active != null) entity.setActive(active);
        if (locked != null) entity.setLocked(locked);
        entity.setUpdatedAt(LocalDateTime.now());
        return customerMapper.toDto(customerRepository.save(entity));
    }

    private Customer requireCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
    }

    private void ensureUniqueFields(CustomerDto request, UUID excludedId) {
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = normalizeOptional(request.getEmail(), true);
        String phone = normalizeOptional(request.getPhone(), false);
        if (customerRepository.existsByUsernameAndIdNot(username, excludedId)) {
            throw BusinessException.conflict("Username khách hàng đã tồn tại");
        }
        if (email != null && customerRepository.existsByEmailAndIdNot(email, excludedId)) {
            throw BusinessException.conflict("Email khách hàng đã tồn tại");
        }
        if (phone != null && customerRepository.existsByPhoneAndIdNot(phone, excludedId)) {
            throw BusinessException.conflict("Số điện thoại khách hàng đã tồn tại");
        }
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw BusinessException.badRequest("Khoảng ngày tạo không hợp lệ");
        }
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp khách hàng không hợp lệ");
    }

    private String normalizeOptional(String value, boolean lowercase) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return lowercase ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
