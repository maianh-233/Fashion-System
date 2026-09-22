package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.service.StoreService;
import com.fashionsystem.fashion_system.service.EmployeeAdministrationService;
import com.fashionsystem.fashion_system.service.StoreEmployeeExcelService;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API cửa hàng chỉ dành cho ADMIN và SUPER_ADMIN. */
@RestController
@RequestMapping("/api/admin/stores")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class StoreController {
    private final StoreService service;
    private final EmployeeAdministrationService employeeService;
    private final StoreEmployeeExcelService employeeExcelService;

    @Autowired
    public StoreController(StoreService service, EmployeeAdministrationService employeeService,
            StoreEmployeeExcelService employeeExcelService) {
        this.service = service;
        this.employeeService = employeeService;
        this.employeeExcelService = employeeExcelService;
    }

    /** Compatibility constructor for focused controller tests that only exercise store endpoints. */
    public StoreController(StoreService service) {
        this(service, null, null);
    }

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

    @GetMapping("/{id}/employees/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('USER_VIEW')")
    public ResponseEntity<byte[]> exportEmployees(Authentication authentication, @PathVariable UUID id) {
        StoreDto store = service.getById(id);
        var employees = employeeService.getList(((AuthenticatedUser) authentication.getPrincipal()).userId(),
                id, null, null, null, Pageable.unpaged()).getContent();
        byte[] workbook = employeeExcelService.export(store.getCode(), store.getName(), employees);
        String filename = ("nhan-vien-" + (store.getCode() == null ? id : store.getCode()) + ".xlsx")
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(workbook);
    }

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

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and hasAuthority('STORE_UPDATE')")
    public StoreDto restore(@PathVariable UUID id) { return service.restore(id); }
}
