package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.PositionDto;
import com.fashionsystem.fashion_system.dto.hierarchy.HierarchyImpactResponse;
import com.fashionsystem.fashion_system.exception.HierarchyConfirmationRequiredException;
import com.fashionsystem.fashion_system.dto.position.CreatePositionRequest;
import com.fashionsystem.fashion_system.dto.position.UpdatePositionRequest;
import com.fashionsystem.fashion_system.service.PositionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {
    private final PositionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('POSITION_VIEW')")
    public Page<PositionDto> getList(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.getList(keyword, departmentId, active, pageable);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POSITION_VIEW')")
    public PositionDto getById(@PathVariable UUID id) { return service.getById(id); }
    @GetMapping("/{id}/hierarchy-impact")
    @PreAuthorize("hasAuthority('POSITION_UPDATE')")
    public HierarchyImpactResponse getHierarchyImpact(@PathVariable UUID id,
            @RequestParam UUID departmentId, @RequestParam @Positive Integer hierarchyLevel) {
        return service.getHierarchyImpact(id, departmentId, hierarchyLevel);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('POSITION_CREATE')")
    public PositionDto create(@Valid @RequestBody CreatePositionRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('POSITION_UPDATE')")
    public PositionDto update(@PathVariable UUID id, @Valid @RequestBody UpdatePositionRequest request) {
        return service.update(id, request);
    }
    @ExceptionHandler(HierarchyConfirmationRequiredException.class)
    public ProblemDetail hierarchyConfirmationRequired(HierarchyConfirmationRequiredException exception) {
        return exception.getBody();
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('POSITION_DELETE')")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
