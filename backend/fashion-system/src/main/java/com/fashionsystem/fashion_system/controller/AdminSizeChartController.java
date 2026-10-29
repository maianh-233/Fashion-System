package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.storefront.SizeAdvisorDtos;
import com.fashionsystem.fashion_system.service.SizeAdvisorService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bảng size của danh mục (quyền danh mục). */
@RestController
@RequestMapping("/api/categories/{categoryId}/size-chart")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminSizeChartController {
    private final SizeAdvisorService sizes;

    @GetMapping
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public SizeAdvisorDtos.Chart get(@PathVariable UUID categoryId) {
        return sizes.chart(categoryId);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    public SizeAdvisorDtos.Chart replace(@PathVariable UUID categoryId, @Valid @RequestBody SizeAdvisorDtos.ChartRequest request) {
        return sizes.replaceChart(categoryId, request);
    }
}
