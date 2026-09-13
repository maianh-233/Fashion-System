package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.UserDepartment;
import com.fashionsystem.fashion_system.entity.UserDepartmentId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDepartmentRepository extends BaseRepository<UserDepartment, UserDepartmentId> {
    List<UserDepartment> findAllByUserId(UUID userId);
    List<UserDepartment> findAllByDepartmentId(UUID departmentId);

    @Query("""
            select (count(ud) > 0) from UserDepartment ud, User u
            where ud.userId = u.id
              and ud.departmentId = :departmentId
              and u.active = true
              and u.deletedAt is null
              and upper(coalesce(u.employmentStatus, '')) <> 'TERMINATED'
            """)
    boolean existsActiveEmployeeByDepartmentId(@Param("departmentId") UUID departmentId);
}
