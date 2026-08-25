package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Module;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository extends BaseRepository<Module, UUID> {
    Optional<Module> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    List<Module> findAllByOrderBySortOrderAscCodeAsc();
    List<Module> findAllByActiveTrueOrderBySortOrderAscNameAsc();
}
