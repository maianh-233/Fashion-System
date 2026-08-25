package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Role;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Role.
 */
public interface RoleRepository extends BaseRepository<Role, UUID> {

    /**
     * Tìm vai trò theo mã để gán quyền hoặc tạo authority.
     *
     * @param code mã vai trò
     * @return vai trò tương ứng hoặc Optional rỗng
     */
    Optional<Role> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    List<Role> findAllByOrderByCodeAsc();

    List<Role> findAllByCodeIn(Collection<String> codes);

    @Query(value = """
            SELECT DISTINCT r.code FROM roles r
              JOIN user_roles ur ON ur.role_id = r.id
             WHERE ur.user_id = :userId
             ORDER BY r.code
            """, nativeQuery = true)
    List<String> findCodesByUserId(@Param("userId") UUID userId);
}
