package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Supplier;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Supplier.
 */
public interface SupplierRepository extends BaseRepository<Supplier, UUID> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, UUID id);

    @Query("""
            select s from Supplier s
            where (:keyword = ''
                or lower(s.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(s.code, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(s.contactName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(s.phone, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(s.email, '')) like lower(concat('%', :keyword, '%')))
              and (:status = '' or upper(coalesce(s.status, '')) = :status)
            """)
    Page<Supplier> search(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);
}
