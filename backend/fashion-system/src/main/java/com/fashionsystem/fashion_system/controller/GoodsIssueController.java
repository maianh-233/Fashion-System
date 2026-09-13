package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.GoodsIssueDto;
import com.fashionsystem.fashion_system.dto.GoodsIssueItemDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.GoodsIssueService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Store-scoped export receipt API. Actor and effective Store always come from the backend. */
@RestController
@RequestMapping("/api/export-receipts")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class GoodsIssueController {
    private final GoodsIssueService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_CREATE')")
    public GoodsIssueDto create(Authentication auth, @Valid @RequestBody GoodsIssueDto request) {
        return service.create(userId(auth), request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_VIEW')")
    public Page<GoodsIssueDto> list(Authentication auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.getList(userId(auth), keyword, storeId, orderId, issueType, status, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_VIEW')")
    public GoodsIssueDto detail(Authentication auth, @PathVariable UUID id) {
        return service.getById(userId(auth), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_UPDATE')")
    public GoodsIssueDto update(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody GoodsIssueDto request) {
        return service.update(userId(auth), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_DELETE')")
    public void delete(Authentication auth, @PathVariable UUID id) {
        service.delete(userId(auth), id);
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_VIEW')")
    public List<GoodsIssueItemDto> items(Authentication auth, @PathVariable UUID id) {
        return service.getItems(userId(auth), id);
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_UPDATE')")
    public GoodsIssueItemDto addItem(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody GoodsIssueItemDto request) {
        return service.addItem(userId(auth), id, request);
    }

    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_UPDATE')")
    public GoodsIssueItemDto updateItem(Authentication auth, @PathVariable UUID id,
            @PathVariable UUID itemId, @Valid @RequestBody GoodsIssueItemDto request) {
        return service.updateItem(userId(auth), id, itemId, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_UPDATE')")
    public void deleteItem(Authentication auth, @PathVariable UUID id, @PathVariable UUID itemId) {
        service.removeItem(userId(auth), id, itemId);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('EXPORT_RECEIPT_APPROVE')")
    public GoodsIssueDto approve(Authentication auth, @PathVariable UUID id) {
        return service.approve(userId(auth), id);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
