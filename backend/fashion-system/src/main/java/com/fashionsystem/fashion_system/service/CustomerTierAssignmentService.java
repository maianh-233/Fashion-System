package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CustomerDto;
import com.fashionsystem.fashion_system.dto.CustomerTierAssignmentDto;
import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CustomerMapper;
import com.fashionsystem.fashion_system.mapper.CustomerTierAssignmentMapper;
import com.fashionsystem.fashion_system.mapper.CustomerTierMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ lịch sử và hạng hiện hành của khách hàng. */
@Service
@RequiredArgsConstructor
public class CustomerTierAssignmentService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "customerId", "tierId", "assignedAt", "expiresAt");
    private static final Set<String> CUSTOMER_SORT_FIELDS = Set.of(
            "id", "username", "email", "phone", "fullName", "createdAt");

    private final CustomerRepository customerRepository;
    private final CustomerTierRepository tierRepository;
    private final CustomerTierAssignmentRepository assignmentRepository;
    private final CustomerTierAssignmentMapper assignmentMapper;
    private final CustomerTierMapper tierMapper;
    private final CustomerMapper customerMapper;

    /**
     * Tạo một bản ghi gán hạng sau khi kiểm tra customer, tier và hiệu lực.
     */
    @Transactional
    public CustomerTierAssignmentDto create(CustomerTierAssignmentDto request) {
        if (request.getCustomerId() == null) {
            throw BusinessException.badRequest("customerId là bắt buộc");
        }
        lockCustomer(request.getCustomerId());
        requireTier(request.getTierId());
        LocalDateTime assignedAt = request.getAssignedAt() == null
                ? LocalDateTime.now() : request.getAssignedAt();
        validateValidity(assignedAt, request.getExpiresAt());
        if (request.getExpiresAt() == null
                && assignmentRepository.findByCustomerIdAndExpiresAtIsNull(request.getCustomerId()).isPresent()) {
            throw BusinessException.conflict("Khách hàng đã có hạng hiện hành");
        }
        CustomerTierAssignment entity = assignmentMapper.toEntity(request);
        entity.setAssignedAt(assignedAt);
        return save(entity);
    }

    /**
     * Lấy chi tiết một lần gán hạng.
     */
    @Transactional(readOnly = true)
    public CustomerTierAssignmentDto getById(UUID id) {
        return assignmentMapper.toDto(requireAssignment(id));
    }

    /**
     * Lấy danh sách gán hạng với bộ lọc thời gian, customer, tier và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CustomerTierAssignmentDto> getList(
            UUID customerId,
            UUID tierId,
            LocalDateTime assignedFrom,
            LocalDateTime assignedTo,
            LocalDateTime expiresFrom,
            LocalDateTime expiresTo,
            Pageable pageable) {
        validateRange(assignedFrom, assignedTo, "Khoảng thời gian gán không hợp lệ");
        validateRange(expiresFrom, expiresTo, "Khoảng thời gian hết hạn không hợp lệ");
        validateSort(pageable, ALLOWED_SORT_FIELDS, "Trường sắp xếp lần gán hạng không hợp lệ");
        return assignmentRepository.search(
                        customerId, tierId, assignedFrom, assignedTo, expiresFrom, expiresTo, pageable)
                .map(assignmentMapper::toDto);
    }

    /**
     * Cập nhật thời gian hiệu lực và ghi chú của một lần gán hạng.
     */
    @Transactional
    public CustomerTierAssignmentDto update(UUID id, CustomerTierAssignmentDto request) {
        CustomerTierAssignment entity = requireAssignment(id);
        if (request.getCustomerId() != null && !request.getCustomerId().equals(entity.getCustomerId())) {
            throw BusinessException.badRequest("Không thể thay đổi customer của lịch sử gán hạng");
        }
        if (!request.getTierId().equals(entity.getTierId())) {
            throw BusinessException.badRequest("Không thể thay đổi tier của lịch sử gán hạng");
        }
        lockCustomer(entity.getCustomerId());
        LocalDateTime assignedAt = request.getAssignedAt() == null
                ? entity.getAssignedAt() : request.getAssignedAt();
        validateValidity(assignedAt, request.getExpiresAt());
        if (request.getExpiresAt() == null) {
            assignmentRepository.findByCustomerIdAndExpiresAtIsNull(entity.getCustomerId())
                    .filter(current -> !current.getId().equals(id))
                    .ifPresent(current -> {
                        throw BusinessException.conflict("Khách hàng đã có hạng hiện hành");
                    });
        }
        entity.setAssignedAt(assignedAt);
        entity.setExpiresAt(request.getExpiresAt());
        entity.setNote(request.getNote());
        return save(entity);
    }

    /**
     * Xóa một bản ghi lịch sử gán hạng.
     */
    @Transactional
    public void delete(UUID id) {
        CustomerTierAssignment entity = requireAssignment(id);
        lockCustomer(entity.getCustomerId());
        assignmentRepository.delete(entity);
    }

    /**
     * Lấy hạng hiện hành của khách hàng.
     */
    @Transactional(readOnly = true)
    public CustomerTierDto getCurrentTier(UUID customerId) {
        requireCustomer(customerId);
        return tierMapper.toDto(tierRepository.findCurrentByCustomerId(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng chưa có hạng hiện hành")));
    }

    /**
     * Lấy lịch sử hạng của khách hàng theo phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CustomerTierAssignmentDto> getHistory(UUID customerId, Pageable pageable) {
        requireCustomer(customerId);
        validateSort(pageable, ALLOWED_SORT_FIELDS, "Trường sắp xếp lịch sử hạng không hợp lệ");
        return assignmentRepository.findAllByCustomerId(customerId, pageable)
                .map(assignmentMapper::toDto);
    }

    /**
     * Kết thúc hạng hiện hành và gán hạng mới cho khách hàng trong một transaction.
     */
    @Transactional
    public CustomerTierAssignmentDto changeTier(UUID customerId, CustomerTierAssignmentDto request) {
        lockCustomer(customerId);
        requireTier(request.getTierId());
        LocalDateTime now = LocalDateTime.now();
        assignmentRepository.expireCurrent(customerId, now);
        CustomerTierAssignment entity = CustomerTierAssignment.builder()
                .customerId(customerId)
                .tierId(request.getTierId())
                .assignedAt(now)
                .note(request.getNote())
                .build();
        return save(entity);
    }

    /**
     * Hết hạn hạng hiện hành của khách hàng.
     */
    @Transactional
    public void expireCurrentTier(UUID customerId) {
        lockCustomer(customerId);
        if (assignmentRepository.expireCurrent(customerId, LocalDateTime.now()) == 0) {
            throw BusinessException.notFound("Khách hàng chưa có hạng hiện hành");
        }
    }

    /**
     * Lấy khách hàng đang thuộc một hạng theo phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CustomerDto> getCustomersByTier(UUID tierId, Pageable pageable) {
        requireTier(tierId);
        validateSort(pageable, CUSTOMER_SORT_FIELDS, "Trường sắp xếp khách hàng không hợp lệ");
        return customerRepository.findCustomersByCurrentTier(tierId, pageable).map(customerMapper::toDto);
    }

    private CustomerTierAssignmentDto save(CustomerTierAssignment entity) {
        try {
            return assignmentMapper.toDto(assignmentRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Dữ liệu gán hạng xung đột với hạng hiện hành");
        }
    }

    private CustomerTierAssignment requireAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Lần gán hạng không tồn tại"));
    }

    private void requireCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw BusinessException.notFound("Khách hàng không tồn tại");
        }
    }

    private void lockCustomer(UUID customerId) {
        customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> BusinessException.notFound("Khách hàng không tồn tại"));
    }

    private void requireTier(UUID tierId) {
        if (!tierRepository.existsById(tierId)) {
            throw BusinessException.notFound("Hạng khách hàng không tồn tại");
        }
    }

    private void validateValidity(LocalDateTime assignedAt, LocalDateTime expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(assignedAt)) {
            throw BusinessException.badRequest("Thời gian hết hạn phải sau thời gian gán");
        }
    }

    private void validateRange(LocalDateTime from, LocalDateTime to, String message) {
        if (from != null && to != null && from.isAfter(to)) {
            throw BusinessException.badRequest(message);
        }
    }

    private void validateSort(Pageable pageable, Set<String> allowedFields, String message) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !allowedFields.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest(message);
    }
}
