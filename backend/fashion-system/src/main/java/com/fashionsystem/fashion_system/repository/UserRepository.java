package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.User;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
