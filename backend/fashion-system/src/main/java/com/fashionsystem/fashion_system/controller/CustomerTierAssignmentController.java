package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CustomerDto;
import com.fashionsystem.fashion_system.dto.CustomerTierAssignmentDto;
import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.service.CustomerTierAssignmentService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API quản lý lịch sử và hạng hiện hành của khách hàng. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerTierAssignmentController {
    private final CustomerTierAssignmentService assignmentService;

    /**
     * Tạo một bản ghi gán hạng.
     */
    @PostMapping("/customer-tier-assignments")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerTierAssignmentDto create(
            @Valid @RequestBody CustomerTierAssignmentDto request) {
        return assignmentService.create(request);
    }

    /**
     * Lấy chi tiết một bản ghi gán hạng.
     */
    @GetMapping("/customer-tier-assignments/{id}")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public CustomerTierAssignmentDto getById(@PathVariable UUID id) {
        return assignmentService.getById(id);
    }

    /**
     * Lấy danh sách gán hạng với bộ lọc và phân trang.
     */
    @GetMapping("/customer-tier-assignments")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public Page<CustomerTierAssignmentDto> getList(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID tierId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime assignedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime assignedTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresTo,
            @PageableDefault(size = 20, sort = "assignedAt") Pageable pageable) {
        return assignmentService.getList(
                customerId, tierId, assignedFrom, assignedTo, expiresFrom, expiresTo, pageable);
    }

    /**
     * Cập nhật thời gian hiệu lực và ghi chú của một lần gán hạng.
     */
    @PutMapping("/customer-tier-assignments/{id}")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public CustomerTierAssignmentDto update(
            @PathVariable UUID id, @Valid @RequestBody CustomerTierAssignmentDto request) {
        return assignmentService.update(id, request);
    }

    /**
     * Xóa một bản ghi lịch sử gán hạng.
     */
    @DeleteMapping("/customer-tier-assignments/{id}")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        assignmentService.delete(id);
    }

    /**
     * Lấy hạng hiện hành của khách hàng.
     */
    @GetMapping("/customers/{customerId}/tier")
    @PreAuthorize("@ownershipSecurity.isCustomerOrEmployee(#customerId)")
    public CustomerTierDto getCurrentTier(@PathVariable UUID customerId) {
        return assignmentService.getCurrentTier(customerId);
    }

    /**
     * Lấy lịch sử hạng của khách hàng theo phân trang.
     */
    @GetMapping("/customers/{customerId}/tier-history")
    @PreAuthorize("@ownershipSecurity.isCustomerOrEmployee(#customerId)")
    public Page<CustomerTierAssignmentDto> getHistory(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20, sort = "assignedAt") Pageable pageable) {
        return assignmentService.getHistory(customerId, pageable);
    }

    /**
     * Kết thúc hạng cũ và gán hạng mới cho khách hàng.
     */
    @PostMapping("/customers/{customerId}/tier")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerTierAssignmentDto changeTier(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerTierAssignmentDto request) {
        return assignmentService.changeTier(customerId, request);
    }

    /**
     * Hết hạn hạng hiện hành của khách hàng.
     */
    @DeleteMapping("/customers/{customerId}/tier")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void expireCurrentTier(@PathVariable UUID customerId) {
        assignmentService.expireCurrentTier(customerId);
    }

    /**
     * Lấy khách hàng đang thuộc một hạng theo phân trang.
     */
    @GetMapping("/customer-tiers/{tierId}/customers")
    @PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
    public Page<CustomerDto> getCustomersByTier(
            @PathVariable UUID tierId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return assignmentService.getCustomersByTier(tierId, pageable);
    }
}
