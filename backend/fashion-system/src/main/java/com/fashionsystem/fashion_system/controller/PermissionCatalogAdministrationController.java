package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.*;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.Catalog;
import com.fashionsystem.fashion_system.service.PermissionCatalogAdministrationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** CRUD catalog permission; toàn bộ business validation nằm ở service. */
@RestController
@RequestMapping("/api/admin/authorization")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN') and hasAuthority('SETTINGS_MANAGE')")
@RequiredArgsConstructor
public class PermissionCatalogAdministrationController {
    private final PermissionCatalogAdministrationService service;

    @GetMapping("/modules")
    public List<ModuleDto> getModules() { return service.getModules(); }

    @GetMapping("/modules/{id}")
    public ModuleDto getModule(@PathVariable UUID id) { return service.getModule(id); }

    @PostMapping("/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleDto createModule(@Valid @RequestBody ModuleDto request) { return service.createModule(request); }

    @PutMapping("/modules/{id}")
    public ModuleDto updateModule(@PathVariable UUID id, @Valid @RequestBody ModuleDto request) {
        return service.updateModule(id, request);
    }

    @DeleteMapping("/modules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(@PathVariable UUID id) { service.deleteModule(id); }

    @GetMapping("/groups")
    public List<PermissionGroupDto> getGroups() { return service.getGroups(); }

    @GetMapping("/groups/{id}")
    public PermissionGroupDto getGroup(@PathVariable UUID id) { return service.getGroup(id); }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionGroupDto createGroup(@Valid @RequestBody PermissionGroupDto request) {
        return service.createGroup(request);
    }

    @PutMapping("/groups/{id}")
    public PermissionGroupDto updateGroup(
            @PathVariable UUID id, @Valid @RequestBody PermissionGroupDto request) {
        return service.updateGroup(id, request);
    }

    @DeleteMapping("/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable UUID id) { service.deleteGroup(id); }

    @GetMapping("/permissions")
    public List<PermissionDto> getPermissions() { return service.getPermissions(); }

    @GetMapping("/permissions/{id}")
    public PermissionDto getPermission(@PathVariable UUID id) { return service.getPermission(id); }

    @PostMapping("/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionDto createPermission(@Valid @RequestBody PermissionDto request) {
        return service.createPermission(request);
    }

    @PutMapping("/permissions/{id}")
    public PermissionDto updatePermission(
            @PathVariable UUID id, @Valid @RequestBody PermissionDto request) {
        return service.updatePermission(id, request);
    }

    @DeleteMapping("/permissions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermission(@PathVariable UUID id) { service.deletePermission(id); }

    @GetMapping("/catalog")
    public Catalog getCatalogTree() { return service.getCatalogTree(); }
}
