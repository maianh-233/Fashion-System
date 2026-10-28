package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.ReviewDtos;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Quản trị đánh giá sản phẩm: xem (kể cả đã ẩn), ẩn/hiện có lý do và trả lời khách. */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminReviewController {
    private final ProductReviewService reviews;

    @GetMapping
    @PreAuthorize("hasAuthority('REVIEW_VIEW')")
    public Page<ReviewDtos.AdminReview> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer rating,
                                             @RequestParam(required = false) Boolean hidden,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return reviews.adminList(keyword, rating, hidden, pageable);
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasAuthority('REVIEW_MANAGE')")
    public ReviewDtos.AdminReview visibility(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                             @Valid @RequestBody ReviewDtos.VisibilityRequest request) {
        return reviews.setVisibility(id, request, user.userId());
    }

    @PutMapping("/{id}/reply")
    @PreAuthorize("hasAuthority('REVIEW_MANAGE')")
    public ReviewDtos.AdminReview reply(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                        @Valid @RequestBody ReviewDtos.ReplyRequest request) {
        return reviews.reply(id, request, user.userId());
    }
}
