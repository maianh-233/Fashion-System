package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.dto.employee.EmployeeSummaryResponse;
import com.fashionsystem.fashion_system.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho User.
 */
public interface UserRepository extends BaseRepository<User, UUID> {

    /**
     * Tìm người dùng theo username để phục vụ đăng nhập và xác thực JWT.
     *
     * @param username tên đăng nhập cần tìm
     * @return người dùng tương ứng hoặc Optional rỗng
     */
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAllByPositionIdAndDeletedAtIsNull(UUID positionId);

    List<User> findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(UUID managerId);

    boolean existsByManagerIdAndDeletedAtIsNull(UUID managerId);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    @Query("""
            select u from User u
             where u.username = :username or u.email = :email
                or (:employeeCode is not null and u.employeeCode = :employeeCode)
                or (:phone is not null and u.phone = :phone)
            """)
    List<User> findRegistrationConflicts(
            @Param("username") String username,
            @Param("email") String email,
            @Param("employeeCode") String employeeCode,
            @Param("phone") String phone);

    @Query("""
            select u from User u
            where (:scopeStoreId is null or exists (
                select scoped.id from StoreStaff scoped
                where scoped.userId = u.id
                  and scoped.storeId = :scopeStoreId
                  and scoped.active = true
            ))
              and (:filterStoreId is null or exists (
                select filtered.id from StoreStaff filtered
                where filtered.userId = u.id
                  and filtered.storeId = :filterStoreId
                  and filtered.active = true
              ))
              and (:keyword = '' or lower(coalesce(u.fullName, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.employeeCode, '')) like lower(concat('%', :keyword, '%')))
              and (:roleCode = '' or exists (
                select ur.userId from UserRole ur, Role r
                where ur.userId = u.id and ur.roleId = r.id and r.code = :roleCode
              ))
              and (:status = ''
                   or (:status = 'ACTIVE' and u.deletedAt is null and u.active = true)
                   or (:status = 'INACTIVE' and u.deletedAt is null and u.active = false)
                   or (:status = 'DELETED' and u.deletedAt is not null))
            """)
    Page<User> searchEmployees(
            @Param("scopeStoreId") UUID scopeStoreId,
            @Param("filterStoreId") UUID filterStoreId,
            @Param("keyword") String keyword,
            @Param("roleCode") String roleCode,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.employee.EmployeeSummaryResponse(
                count(u),
                coalesce(sum(case when u.deletedAt is null and u.active = true then 1 else 0 end), 0),
                coalesce(sum(case when u.deletedAt is null and u.locked = true then 1 else 0 end), 0),
                coalesce(sum(case when u.createdAt >= :monthStart then 1 else 0 end), 0))
            from User u
            where (:scopeStoreId is null or exists (
                select scoped.id from StoreStaff scoped
                where scoped.userId = u.id
                  and scoped.storeId = :scopeStoreId
                  and scoped.active = true
            ))
              and (:filterStoreId is null or exists (
                select filtered.id from StoreStaff filtered
                where filtered.userId = u.id
                  and filtered.storeId = :filterStoreId
                  and filtered.active = true
              ))
            """)
    EmployeeSummaryResponse summarizeEmployees(
            @Param("scopeStoreId") UUID scopeStoreId,
            @Param("filterStoreId") UUID filterStoreId,
            @Param("monthStart") LocalDateTime monthStart);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
