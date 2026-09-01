package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CustomerDto;
import com.fashionsystem.fashion_system.entity.Gender;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.CustomerService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API quản lý và đọc hồ sơ khách hàng. */
@RestController
@RequestMapping("/api/customers")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    /**
     * Lấy danh sách khách hàng với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
    public Page<CustomerDto> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return customerService.getList(
                keyword, active, locked, gender, createdFrom, createdTo, pageable);
    }

    /**
     * Lấy chi tiết khách hàng theo ID.
     */
    @GetMapping("/{id}")
    public CustomerDto getById(@PathVariable UUID id) {
        return customerService.getById(id);
    }

    /**
     * Lấy hồ sơ của khách hàng đang đăng nhập.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerDto getMyProfile(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return customerService.getProfile(customer.customerId());
    }

    /**
     * Cập nhật thông tin khách hàng theo ID.
     */
    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable UUID id, @Valid @RequestBody CustomerDto request) {
        return customerService.update(id, request);
    }

    /**
     * Xóa khách hàng chưa có dữ liệu liên quan.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        customerService.delete(id);
    }

    /**
     * Kích hoạt tài khoản khách hàng.
     */
    @PatchMapping("/{id}/activate")
    public CustomerDto activate(@PathVariable UUID id) {
        return customerService.activate(id);
    }

    /**
     * Vô hiệu hóa tài khoản khách hàng.
     */
    @PatchMapping("/{id}/deactivate")
    public CustomerDto deactivate(@PathVariable UUID id) {
        return customerService.deactivate(id);
    }

    /**
     * Khóa tài khoản khách hàng.
     */
    @PatchMapping("/{id}/lock")
    public CustomerDto lock(@PathVariable UUID id) {
        return customerService.lock(id);
    }

    /**
     * Mở khóa tài khoản khách hàng.
     */
    @PatchMapping("/{id}/unlock")
    public CustomerDto unlock(@PathVariable UUID id) {
        return customerService.unlock(id);
    }
}
