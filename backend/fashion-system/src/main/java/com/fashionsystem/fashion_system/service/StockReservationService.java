package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.StockReservationDto;
import com.fashionsystem.fashion_system.entity.StockReservation;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.StockReservationMapper;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StockReservationRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý vòng đời giữ chỗ tồn kho cho đơn hàng. */
@Service
@RequiredArgsConstructor
public class StockReservationService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "orderId", "storeId", "productVariantId", "quantity", "status",
            "expiredAt", "createdAt", "updatedAt");
    private final StockReservationRepository repository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final ProductVariantRepository variantRepository;
    private final StockReservationMapper mapper;
    private final InventoryService inventoryService;

    /** Tạo giữ chỗ và chuyển số lượng từ tồn khả dụng sang tồn reserved. */
    @Transactional
    public StockReservationDto create(StockReservationDto request, UUID createdBy) {
        validateRequest(request);
        if (repository.existsByOrderIdAndStoreIdAndProductVariantIdAndStatus(
                request.getOrderId(), request.getStoreId(), request.getProductVariantId(), "ACTIVE"))
            throw BusinessException.conflict("Đơn hàng đã giữ chỗ biến thể này tại cửa hàng");
        StockReservation entity = mapper.toEntity(request);
        entity.setId(null);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(null);
        entity = repository.save(entity);
        inventoryService.reserve(entity.getStoreId(), entity.getProductVariantId(),
                entity.getQuantity(), entity.getId(), createdBy);
        return mapper.toDto(entity);
    }

    /** Lấy chi tiết một lần giữ chỗ tồn kho. */
    @Transactional(readOnly = true)
    public StockReservationDto getById(UUID id) { return mapper.toDto(requireReservation(id)); }

    /** Lấy danh sách giữ chỗ theo đơn hàng, cửa hàng, biến thể và trạng thái. */
    @Transactional(readOnly = true)
    public Page<StockReservationDto> getList(
            UUID orderId, UUID storeId, UUID variantId, String status, Pageable pageable) {
        validateSort(pageable);
        return repository.search(orderId, storeId, variantId, normalize(status), pageable).map(mapper::toDto);
    }

    /** Giải phóng giữ chỗ và hoàn số lượng về tồn khả dụng. */
    @Transactional
    public StockReservationDto release(UUID id, UUID changedBy) {
        StockReservation entity = requireActiveForUpdate(id);
        inventoryService.release(entity.getStoreId(), entity.getProductVariantId(),
                entity.getQuantity(), entity.getId(), changedBy);
        entity.setStatus("RELEASED");
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(entity));
    }

    /** Đánh dấu giữ chỗ hết hạn và hoàn số lượng về tồn khả dụng. */
    @Transactional
    public StockReservationDto expire(UUID id, UUID changedBy) {
        StockReservation entity = requireActiveForUpdate(id);
        inventoryService.release(entity.getStoreId(), entity.getProductVariantId(),
                entity.getQuantity(), entity.getId(), changedBy);
        entity.setStatus("EXPIRED");
        entity.setExpiredAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(entity));
    }

    private StockReservation requireReservation(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Giữ chỗ tồn kho không tồn tại"));
    }
    private StockReservation requireActiveForUpdate(UUID id) {
        StockReservation entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Giữ chỗ tồn kho không tồn tại"));
        if (!"ACTIVE".equalsIgnoreCase(entity.getStatus()))
            throw BusinessException.invalidState("Giữ chỗ tồn kho không còn hiệu lực");
        return entity;
    }
    private void validateRequest(StockReservationDto request) {
        if (!orderRepository.existsById(request.getOrderId())) throw BusinessException.notFound("Đơn hàng không tồn tại");
        if (!storeRepository.existsById(request.getStoreId())) throw BusinessException.notFound("Cửa hàng không tồn tại");
        if (!variantRepository.existsById(request.getProductVariantId()))
            throw BusinessException.notFound("Biến thể sản phẩm không tồn tại");
        if (request.getQuantity() == null || request.getQuantity() <= 0)
            throw BusinessException.badRequest("Số lượng giữ chỗ phải lớn hơn 0");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp giữ chỗ không hợp lệ");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
