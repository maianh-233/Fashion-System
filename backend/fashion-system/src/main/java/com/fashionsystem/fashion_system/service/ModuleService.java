package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.ModuleDto;
import com.fashionsystem.fashion_system.mapper.ModuleMapper;
import com.fashionsystem.fashion_system.repository.ModuleRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp metadata module cho frontend; không quyết định quyền truy cập. */
@Service
@RequiredArgsConstructor
public class ModuleService {
    private final ModuleRepository moduleRepository;
    private final ModuleMapper moduleMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.ACTIVE_MODULES, key = "'all'")
    public List<ModuleDto> getActiveModules() {
        return new ArrayList<>(moduleRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(moduleMapper::toDto)
                .toList());
    }
}
