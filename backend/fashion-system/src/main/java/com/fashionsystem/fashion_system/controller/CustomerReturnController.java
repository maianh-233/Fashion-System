package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.order.ReturnDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.ReturnRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Khách yêu cầu đổi / trả hàng cho đơn đã hoàn thành và theo dõi yêu cầu. */
@RestController
@RequestMapping("/api/customers/me/returns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReturnController {
    private final ReturnRequestService returns;

    /** Đơn còn trong hạn không, các sản phẩm và số lượng còn đổi / trả được, phân loại có thể đổi. */
    @GetMapping("/eligibility")
    public ReturnDtos.Eligibility eligibility(@AuthenticationPrincipal AuthenticatedCustomer customer, @RequestParam UUID orderId) {
        return returns.eligibility(customer.customerId(), orderId);
    }

    @GetMapping
    public List<ReturnDtos.ReturnRequestView> mine(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                                   @RequestParam(required = false) UUID orderId) {
        return returns.mine(customer.customerId(), orderId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnDtos.ReturnRequestView create(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                               @Valid @RequestBody ReturnDtos.CreateRequest request) {
        try (var ignored = scope(customer)) {
            return returns.create(customer.customerId(), request);
        }
    }

    @PostMapping("/{id}/cancel")
    public ReturnDtos.ReturnRequestView cancel(@AuthenticationPrincipal AuthenticatedCustomer customer, @PathVariable UUID id) {
        try (var ignored = scope(customer)) {
            return returns.cancel(customer.customerId(), id);
        }
    }

    private static BusinessAuditContextFactory.JobScope scope(AuthenticatedCustomer customer) {
        return BusinessAuditContextFactory.systemJob("CUSTOMER_RETURN:" + customer.customerId());
    }
}
