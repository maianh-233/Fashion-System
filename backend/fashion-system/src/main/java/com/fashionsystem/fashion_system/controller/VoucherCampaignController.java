package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.promotion.VoucherCampaignDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.UserScopeService;
import com.fashionsystem.fashion_system.service.VoucherCampaignService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chiến dịch voucher theo nhóm khách. Dùng quyền khuyến mãi: xem với PROMOTION_VIEW, tạo / gửi với PROMOTION_CREATE,
 * hủy với PROMOTION_UPDATE; chỉ nhân viên cấp chuỗi được tạo, xem trước nhóm khách và hủy.
 */
@RestController
@RequestMapping("/api/admin/voucher-campaigns")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class VoucherCampaignController {
    private final VoucherCampaignService campaigns;
    private final UserScopeService scopes;

    @GetMapping
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public Page<VoucherCampaignDtos.Campaign> list(@PageableDefault(size = 20) Pageable pageable) {
        return campaigns.list(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public VoucherCampaignDtos.Campaign detail(@PathVariable UUID id) {
        return campaigns.detail(id);
    }

    /** Số khách khớp nhóm lúc này và vài khách mẫu. */
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('PROMOTION_CREATE')")
    public VoucherCampaignDtos.Preview preview(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody VoucherCampaignDtos.Criteria criteria) {
        requireGlobal(user);
        return campaigns.preview(criteria);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROMOTION_CREATE')")
    public VoucherCampaignDtos.Campaign create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody VoucherCampaignDtos.CreateRequest request) {
        requireGlobal(user);
        return campaigns.create(user.userId(), request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public VoucherCampaignDtos.Campaign cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        requireGlobal(user);
        return campaigns.cancel(user.userId(), id);
    }

    private void requireGlobal(AuthenticatedUser user) {
        if (!scopes.resolve(user.userId()).isGlobal())
            throw BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được gửi chiến dịch voucher");
    }
}
