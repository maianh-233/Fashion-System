package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.dto.promotion.PromotionRequest;
import com.fashionsystem.fashion_system.entity.PromotionStatus;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.PromotionService;
import com.fashionsystem.fashion_system.service.UserScopeService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý khuyến mãi toàn hệ thống. Nhân viên global được tạo, sửa, xóa mềm và khôi phục; nhân viên store chỉ
 * tra cứu và không thấy khuyến mãi đã ngừng hoạt động.
 */
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class PromotionController {
    private final PromotionService promotionService;
    private final ProductAuthorizationService authorization;
    private final UserScopeService userScopeService;

    @GetMapping
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public Page<PromotionDto> getList(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PromotionType promotionType,
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID actor = userId(authentication);
        authorization.requireRead(actor, "PROMOTION_VIEW");
        return promotionService.getList(keyword, promotionType, status, isGlobal(actor), dateFrom, dateTo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public PromotionDto getById(Authentication authentication, @PathVariable UUID id) {
        UUID actor = userId(authentication);
        authorization.requireRead(actor, "PROMOTION_VIEW");
        return visible(actor, promotionService.getById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public PromotionDto getByCode(Authentication authentication, @PathVariable String code) {
        UUID actor = userId(authentication);
        authorization.requireRead(actor, "PROMOTION_VIEW");
        return visible(actor, promotionService.getByCode(code));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROMOTION_CREATE')")
    public PromotionDto create(Authentication authentication, @Valid @RequestBody PromotionRequest request) {
        requireMutation(authentication, "PROMOTION_CREATE");
        return promotionService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public PromotionDto update(
            Authentication authentication, @PathVariable UUID id, @Valid @RequestBody PromotionRequest request) {
        requireMutation(authentication, "PROMOTION_UPDATE");
        return promotionService.update(id, request);
    }

    /** Xóa mềm: chuyển trạng thái về INACTIVE. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_DELETE')")
    public PromotionDto delete(Authentication authentication, @PathVariable UUID id) {
        requireMutation(authentication, "PROMOTION_DELETE");
        return promotionService.delete(id);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public PromotionDto restore(Authentication authentication, @PathVariable UUID id) {
        requireMutation(authentication, "PROMOTION_UPDATE");
        return promotionService.restore(id);
    }

    /** Chỉ nhân viên global có quyền mới được thay đổi khuyến mãi toàn hệ thống. */
    private void requireMutation(Authentication authentication, String permission) {
        UUID actor = userId(authentication);
        authorization.requireRead(actor, permission);
        if (!isGlobal(actor))
            throw BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi khuyến mãi");
    }

    private PromotionDto visible(UUID actor, PromotionDto promotion) {
        if (promotion.getStatus() == PromotionStatus.INACTIVE && !isGlobal(actor))
            throw BusinessException.notFound("Khuyến mãi không tồn tại hoặc đã ngừng hoạt động");
        return promotion;
    }

    private boolean isGlobal(UUID actor) {
        return userScopeService.resolve(actor).isGlobal();
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
