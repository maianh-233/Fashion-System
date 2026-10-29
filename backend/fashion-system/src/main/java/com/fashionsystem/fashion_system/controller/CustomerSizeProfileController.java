package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.SizeAdvisorDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.SizeAdvisorService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Số đo của khách và gợi ý size có xét lịch sử mua. */
@RestController
@RequestMapping("/api/customers/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerSizeProfileController {
    private final SizeAdvisorService sizes;

    @GetMapping("/measurements")
    public SizeAdvisorDtos.Measurements measurements(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return sizes.measurements(customer.customerId());
    }

    @PutMapping("/measurements")
    public SizeAdvisorDtos.Measurements save(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                             @Valid @RequestBody SizeAdvisorDtos.Measurements request) {
        return sizes.saveMeasurements(customer.customerId(), request);
    }

    /** Không truyền số đo thì dùng số đo đã lưu. */
    @GetMapping("/size-guide")
    public SizeAdvisorDtos.Guide guide(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                       @RequestParam UUID productId,
                                       @RequestParam(required = false) Integer heightCm,
                                       @RequestParam(required = false) BigDecimal weightKg) {
        return sizes.guide(productId, customer.customerId(), heightCm, weightKg);
    }
}
