package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.storefront.ReviewDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.service.ProductReviewService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Khách đánh giá sản phẩm trong đơn đã hoàn thành và xem lại đánh giá của mình. */
@RestController
@RequestMapping("/api/customers/me/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReviewController {
    private final ProductReviewService reviews;

    @GetMapping
    public List<ReviewDtos.MyReview> mine(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return reviews.mine(customer.customerId());
    }

    @GetMapping("/pending")
    public List<ReviewDtos.Pending> pending(@AuthenticationPrincipal AuthenticatedCustomer customer) {
        return reviews.pending(customer.customerId());
    }

    /** Multipart: orderId, productId, rating (1..5), content (tùy chọn) và tối đa 3 ảnh {@code images}. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDtos.MyReview create(@AuthenticationPrincipal AuthenticatedCustomer customer,
                                      @RequestParam UUID orderId, @RequestParam UUID productId,
                                      @RequestParam Integer rating, @RequestParam(required = false) String content,
                                      @RequestPart(name = "images", required = false) List<MultipartFile> images) {
        try (var ignored = BusinessAuditContextFactory.systemJob("CUSTOMER_REVIEW:" + customer.customerId())) {
            return reviews.create(customer.customerId(), orderId, productId, rating, content, images);
        }
    }
}
