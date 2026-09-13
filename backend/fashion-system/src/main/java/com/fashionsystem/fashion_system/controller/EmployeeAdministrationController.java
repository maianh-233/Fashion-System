package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.employee.CreateEmployeeRequest;
import com.fashionsystem.fashion_system.dto.employee.CreateEmployeeResponse;
import com.fashionsystem.fashion_system.dto.employee.EmployeeResponse;
import com.fashionsystem.fashion_system.dto.employee.EmployeeScopeResponse;
import com.fashionsystem.fashion_system.dto.employee.EmployeeSummaryResponse;
import com.fashionsystem.fashion_system.dto.employee.UpdateEmployeeRequest;
import com.fashionsystem.fashion_system.dto.employee.EligibleSubordinateResponse;
import com.fashionsystem.fashion_system.dto.hierarchy.HierarchyImpactResponse;
import com.fashionsystem.fashion_system.exception.HierarchyConfirmationRequiredException;
import com.fashionsystem.fashion_system.dto.employee.SubordinateAssignmentRequest;
import com.fashionsystem.fashion_system.dto.StoreDto;
import java.util.List;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.EmployeeAdministrationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API quản trị nhân viên; phạm vi cửa hàng luôn được suy ra từ principal. */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeAdministrationController {
    private final EmployeeAdministrationService service;

    @GetMapping("/{id}/hierarchy-impact")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public HierarchyImpactResponse getHierarchyImpact(
            Authentication authentication, @PathVariable UUID id,
            @RequestParam UUID departmentId, @RequestParam UUID positionId) {
        return service.getHierarchyImpact(userId(authentication), id, departmentId, positionId);
    }

    @GetMapping("/{id}/eligible-subordinates")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public List<EligibleSubordinateResponse> getEligibleSubordinates(
            Authentication authentication, @PathVariable UUID id) {
        return service.getEligibleSubordinates(userId(authentication), id);
    }

    @ExceptionHandler(HierarchyConfirmationRequiredException.class)
    public ProblemDetail hierarchyConfirmationRequired(HierarchyConfirmationRequiredException exception) {
        return exception.getBody();
    }

    @GetMapping("/available-stores")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public List<StoreDto> getAvailableStores(Authentication authentication) {
        return service.getAvailableStores(userId(authentication));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public Page<EmployeeResponse> getList(Authentication authentication,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "fullName") Pageable pageable) {
        return service.getList(userId(authentication), storeId,
                keyword, roleCode, status, pageable);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public EmployeeSummaryResponse getSummary(Authentication authentication,
            @RequestParam(required = false) UUID storeId) {
        return service.getSummary(userId(authentication), storeId);
    }

    @GetMapping("/scope")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public EmployeeScopeResponse getScope(Authentication authentication) {
        return service.getScope(userId(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public EmployeeResponse getById(Authentication authentication, @PathVariable UUID id) {
        return service.getById(userId(authentication), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public CreateEmployeeResponse create(Authentication authentication, @Valid @RequestBody CreateEmployeeRequest request) {
        return service.create(userId(authentication), request);
    }

    @GetMapping("/{id}/subordinates")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public List<EmployeeResponse> getSubordinates(Authentication authentication, @PathVariable UUID id) {
        return service.getSubordinates(userId(authentication), id);
    }

    @PostMapping("/{id}/subordinates")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public EmployeeResponse assignSubordinate(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody SubordinateAssignmentRequest request) {
        return service.assignSubordinate(userId(authentication), id, request.email());
    }

    @DeleteMapping("/{id}/subordinates/{subordinateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public void removeSubordinate(Authentication authentication, @PathVariable UUID id,
            @PathVariable UUID subordinateId) {
        service.removeSubordinate(userId(authentication), id, subordinateId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public EmployeeResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return service.update(userId(authentication), id, request);
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public EmployeeResponse setLocked(Authentication authentication, @PathVariable UUID id,
            @RequestParam boolean locked) {
        return service.setLocked(userId(authentication), id, locked);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public EmployeeResponse restore(Authentication authentication, @PathVariable UUID id) {
        return service.restore(userId(authentication), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        service.delete(userId(authentication), id);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
