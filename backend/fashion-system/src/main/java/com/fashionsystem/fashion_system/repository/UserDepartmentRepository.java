package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.UserDepartment;
import com.fashionsystem.fashion_system.entity.UserDepartmentId;
import java.util.List;
import java.util.UUID;

public interface UserDepartmentRepository extends BaseRepository<UserDepartment, UserDepartmentId> {
    List<UserDepartment> findAllByUserId(UUID userId);
    List<UserDepartment> findAllByDepartmentId(UUID departmentId);
}
