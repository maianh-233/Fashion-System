package com.fashionsystem.fashion_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Repository nền cung cấp các thao tác CRUD cơ bản cho tất cả entity.
 *
 * @param <T>  kiểu entity được quản lý
 * @param <ID> kiểu khóa chính của entity
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * Tạo mới hoặc cập nhật một bản ghi.
     *
     * @param entity dữ liệu cần lưu
     * @return bản ghi sau khi được lưu
     * @param <S> kiểu entity cụ thể
     */
    @Override
    <S extends T> S save(S entity);

    /**
     * Tìm một bản ghi theo khóa chính.
     *
     * @param id khóa chính của bản ghi
     * @return bản ghi tìm được hoặc Optional rỗng nếu không tồn tại
     */
    @Override
    Optional<T> findById(ID id);

    /**
     * Lấy toàn bộ bản ghi của entity.
     *
     * @return danh sách bản ghi
     */
    @Override
    List<T> findAll();

    /**
     * Kiểm tra một bản ghi có tồn tại theo khóa chính hay không.
     *
     * @param id khóa chính cần kiểm tra
     * @return true nếu bản ghi tồn tại, ngược lại là false
     */
    @Override
    boolean existsById(ID id);

    /**
     * Xóa một bản ghi theo khóa chính.
     *
     * @param id khóa chính của bản ghi cần xóa
     */
    @Override
    void deleteById(ID id);
}
