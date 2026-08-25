package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ModuleDto;
import com.fashionsystem.fashion_system.mapper.ModuleMapper;
import com.fashionsystem.fashion_system.repository.ModuleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp metadata module cho frontend; không quyết định quyền truy cập. */
@Service
@RequiredArgsConstructor
public class ModuleService {
    private final ModuleRepository moduleRepository;
    private final ModuleMapper moduleMapper;

    @Transactional(readOnly = true)
    public List<ModuleDto> getActiveModules() {
        return moduleRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(moduleMapper::toDto)
                .toList();
    }
}
