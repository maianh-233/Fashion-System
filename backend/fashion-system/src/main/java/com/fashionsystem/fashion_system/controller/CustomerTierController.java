package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.service.CustomerTierService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

/** API cấu hình hạng khách hàng. */
@RestController
@RequestMapping("/api/customer-tiers")
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
@RequiredArgsConstructor
public class CustomerTierController {
    private final CustomerTierService tierService;

    /**
     * Tạo mới hạng khách hàng.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerTierDto create(@Valid @RequestBody CustomerTierDto request) {
        return tierService.create(request);
    }

    /**
     * Lấy chi tiết hạng khách hàng.
     */
    @GetMapping("/{id}")
    public CustomerTierDto getById(@PathVariable UUID id) {
        return tierService.getById(id);
    }

    /**
     * Lấy danh sách hạng khách hàng với tìm kiếm, lọc và phân trang.
     */
    @GetMapping
    public Page<CustomerTierDto> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minSpentFrom,
            @RequestParam(required = false) BigDecimal minSpentTo,
            @RequestParam(required = false) BigDecimal discountFrom,
            @RequestParam(required = false) BigDecimal discountTo,
            @PageableDefault(size = 20, sort = "minTotalSpent") Pageable pageable) {
        return tierService.getList(
                keyword, minSpentFrom, minSpentTo, discountFrom, discountTo, pageable);
    }

    /**
     * Cập nhật cấu hình hạng khách hàng.
     */
    @PutMapping("/{id}")
    public CustomerTierDto update(
            @PathVariable UUID id, @Valid @RequestBody CustomerTierDto request) {
        return tierService.update(id, request);
    }

    /**
     * Xóa hạng khách hàng chưa được sử dụng.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tierService.delete(id);
    }

    /**
     * Lấy toàn bộ hạng theo ngưỡng chi tiêu tăng dần.
     */
    @GetMapping("/ordered")
    public List<CustomerTierDto> getOrderedTiers() {
        return tierService.getOrderedTiers();
    }

    /**
     * Xem trước hạng phù hợp với tổng tiền đã chi.
     */
    @GetMapping("/preview")
    public CustomerTierDto preview(@RequestParam BigDecimal totalSpent) {
        return tierService.preview(totalSpent);
    }
}
