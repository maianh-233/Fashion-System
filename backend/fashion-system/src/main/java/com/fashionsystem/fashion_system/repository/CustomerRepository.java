package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Truy cập khách hàng độc lập với UserRepository của nhân viên. */
public interface CustomerRepository extends BaseRepository<Customer, UUID> {
    Optional<Customer> findByUsername(String username);
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select c from Customer c where c.username = :username or c.email = :email")
    List<Customer> findRegistrationConflicts(
            @Param("username") String username, @Param("email") String email);
}
