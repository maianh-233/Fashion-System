package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.ModuleDto;
import com.fashionsystem.fashion_system.service.ModuleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Metadata module phục vụ UI; endpoint vẫn yêu cầu JWT theo SecurityConfig. */
@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class ModuleController {
    private final ModuleService moduleService;

    @GetMapping
    public List<ModuleDto> getActiveModules() {
        return moduleService.getActiveModules();
    }
}
