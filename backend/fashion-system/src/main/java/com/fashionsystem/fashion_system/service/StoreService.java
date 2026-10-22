package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.config.CacheKeys;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.StoreMapper;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** Cung cấp nghiệp vụ quản lý cửa hàng. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("STORE")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoreService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "code", "name", "phone", "active", "createdAt", "updatedAt");
    private final StoreRepository repository;
    private final StoreMapper mapper;
    private final CatalogReadCacheService cacheReads;
    private final StorageService storage;

    /** Tạo mới một cửa hàng. */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.STORE_LIST, allEntries = true)
    public StoreDto create(StoreDto request) {
        ensureCoordinatesAvailable(request.getLatitude(), request.getLongitude(), null);
        Store entity = mapper.toEntity(request);
        entity.setCode(generateStoreCode());
        try {
            return mapper.toDto(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Mã hoặc tọa độ cửa hàng đã tồn tại");
        }
    }

    /** Lấy chi tiết cửa hàng theo ID. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.STORE_DETAIL, key = "#id")
    public StoreDto getById(UUID id) { return mapper.toDto(requireStore(id)); }

    /** Lấy danh sách cửa hàng với tìm kiếm, lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<StoreDto> getList(String keyword, Boolean active, Pageable pageable) {
        validateSort(pageable);
        String normalizedKeyword = trimToEmpty(keyword);
        return cacheReads.stores(CacheKeys.of(normalizedKeyword, active, pageable),
                () -> repository.search(normalizedKeyword, active, pageable).map(mapper::toDto))
                .toPage();
    }

    /** Cập nhật thông tin cửa hàng. */
    @Transactional
    @Caching(put = @CachePut(cacheNames = CacheNames.STORE_DETAIL, key = "#id"), evict =
            @CacheEvict(cacheNames = CacheNames.STORE_LIST, allEntries = true))
    public StoreDto update(UUID id, StoreDto request) {
        Store entity = requireStore(id);
        ensureCoordinatesAvailable(request.getLatitude(), request.getLongitude(), id);
        mapper.updateEntity(request, entity);
        try {
            return mapper.toDto(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Tọa độ cửa hàng đã được sử dụng");
        }
    }

    /** Kiểm tra trước tính duy nhất của một cặp tọa độ để giao diện phản hồi tức thì. */
    @Transactional(readOnly = true)
    public boolean areCoordinatesAvailable(BigDecimal latitude, BigDecimal longitude, UUID excludedId) {
        validateCoordinates(latitude, longitude);
        return excludedId == null
                ? !repository.existsByLatitudeAndLongitude(latitude, longitude)
                : !repository.existsByLatitudeAndLongitudeAndIdNot(latitude, longitude, excludedId);
    }

    /** Xóa mềm cửa hàng để dữ liệu nghiệp vụ đã tham chiếu vẫn truy vết được. */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.STORE_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.STORE_LIST, allEntries = true)})
    public void delete(UUID id) {
        Store store = requireStore(id);
        store.setActive(Boolean.FALSE);
        repository.saveAndFlush(store);
    }

    /** Khôi phục cửa hàng đã xóa mềm về trạng thái hoạt động. */
    @Transactional
    @Caching(put = @CachePut(cacheNames = CacheNames.STORE_DETAIL, key = "#id"), evict =
            @CacheEvict(cacheNames = CacheNames.STORE_LIST, allEntries = true))
    public StoreDto restore(UUID id) {
        Store store = requireStore(id);
        store.setActive(Boolean.TRUE);
        return mapper.toDto(repository.saveAndFlush(store));
    }

    /** Tải ảnh cửa hàng hiển thị cho khách; ảnh cũ chỉ bị xóa khi lưu thành công. */
    @Transactional
    @Caching(put = @CachePut(cacheNames = CacheNames.STORE_DETAIL, key = "#id"), evict =
            @CacheEvict(cacheNames = CacheNames.STORE_LIST, allEntries = true))
    public StoreDto uploadImage(UUID id, MultipartFile file) {
        Store store = requireStore(id);
        StorageUploadResult uploaded = storage.uploadImage(file, "stores/" + id);
        String oldPublicId = store.getImagePublicId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                if (oldPublicId != null && !oldPublicId.isBlank()) safeDeleteImage(oldPublicId);
            }
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safeDeleteImage(uploaded.publicId());
            }
        });
        store.setImageUrl(uploaded.secureUrl());
        store.setImagePublicId(uploaded.publicId());
        return mapper.toDto(repository.saveAndFlush(store));
    }

    private void safeDeleteImage(String publicId) {
        try { storage.deleteImage(publicId); }
        catch (RuntimeException ignored) { /* DB state remains authoritative; provider cleanup can be retried. */ }
    }

    private Store requireStore(UUID id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("Cửa hàng không tồn tại"));
    }
    private void ensureCoordinatesAvailable(BigDecimal latitude, BigDecimal longitude, UUID excludedId) {
        if (!areCoordinatesAvailable(latitude, longitude, excludedId)) {
            throw BusinessException.conflict("Tọa độ cửa hàng đã được sử dụng");
        }
    }
    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw BusinessException.badRequest("Vĩ độ và kinh độ là bắt buộc");
        }
        if (latitude.scale() > 6 || longitude.scale() > 6
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw BusinessException.badRequest("Tọa độ không hợp lệ hoặc có quá 6 chữ số thập phân");
        }
    }
    private String generateStoreCode() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String code;
        do {
            code = "CH" + date + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 8).toUpperCase(Locale.ROOT);
        } while (repository.existsByCode(code));
        return code;
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp cửa hàng không hợp lệ");
    }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
