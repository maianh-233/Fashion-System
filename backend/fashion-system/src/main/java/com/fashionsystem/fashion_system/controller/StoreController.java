package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.service.StoreService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
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

/** API cửa hàng chỉ dành cho ADMIN và SUPER_ADMIN. */
@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class StoreController {
    private final StoreService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('STORE_VIEW')")
    public Page<StoreDto> getList(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return service.getList(keyword, active, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('STORE_VIEW')")
    public StoreDto getById(@PathVariable UUID id) { return service.getById(id); }

    @GetMapping("/coordinate-availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('STORE_VIEW')")
    public Map<String, Boolean> getCoordinateAvailability(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(required = false) UUID excludeId) {
        return Map.of("available", service.areCoordinatesAvailable(latitude, longitude, excludeId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('STORE_CREATE')")
    public StoreDto create(@Valid @RequestBody StoreDto request) { return service.create(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('STORE_UPDATE')")
    public StoreDto update(@PathVariable UUID id, @Valid @RequestBody StoreDto request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('STORE_DELETE')")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
