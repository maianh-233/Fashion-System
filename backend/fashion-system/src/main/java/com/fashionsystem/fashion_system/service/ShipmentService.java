package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ShipmentDto;
import com.fashionsystem.fashion_system.entity.Shipment;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ShipmentMapper;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.ShipmentRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý vận đơn và đồng bộ trạng thái giao hàng với đơn hàng. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("SHIPMENT")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ShipmentService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "orderId", "shippingProvider", "trackingCode", "shippingStatus",
            "shippedAt", "deliveredAt", "createdAt", "updatedAt");
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "PENDING", Set.of("SHIPPING", "FAILED"),
            "SHIPPING", Set.of("DELIVERED", "FAILED", "RETURNED"),
            "DELIVERED", Set.of("RETURNED"));
    private final ShipmentRepository repository;
    private final OrderRepository orderRepository;
    private final ShipmentMapper mapper;
    private final OrderService orderService;

    /** Tạo vận đơn chờ giao cho một đơn hàng. */
    @Transactional
    public ShipmentDto create(ShipmentDto request) {
        if (!orderRepository.existsById(request.getOrderId())) throw BusinessException.notFound("Đơn hàng không tồn tại");
        if (repository.existsByOrderId(request.getOrderId())) throw BusinessException.conflict("Đơn hàng đã có vận đơn");
        Shipment shipment = Shipment.builder().orderId(request.getOrderId())
                .shippingProvider(trimToNull(request.getShippingProvider()))
                .trackingCode(trimToNull(request.getTrackingCode())).shippingStatus("PENDING")
                .createdAt(LocalDateTime.now()).build();
        return mapper.toDto(repository.save(shipment));
    }

    /** Lấy chi tiết vận đơn theo ID. */
    @Transactional(readOnly = true)
    public ShipmentDto getById(UUID id) { return mapper.toDto(requireShipment(id)); }

    /** Lấy vận đơn của một đơn hàng. */
    @Transactional(readOnly = true)
    public ShipmentDto getByOrderId(UUID orderId) {
        return mapper.toDto(repository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound("Vận đơn không tồn tại")));
    }

    /** Lấy danh sách vận đơn với bộ lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getList(UUID orderId, String status, String provider, Pageable pageable) {
        validateSort(pageable);
        return repository.search(orderId, normalize(status), trimToEmpty(provider), pageable).map(mapper::toDto);
    }

    /** Cập nhật nhà vận chuyển và mã tracking khi vận đơn còn chờ giao. */
    @Transactional
    public ShipmentDto update(UUID id, ShipmentDto request) {
        Shipment shipment = repository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Vận đơn không tồn tại"));
        if (!"PENDING".equals(shipment.getShippingStatus()))
            throw BusinessException.invalidState("Chỉ được sửa vận đơn đang chờ giao");
        shipment.setShippingProvider(trimToNull(request.getShippingProvider()));
        shipment.setTrackingCode(trimToNull(request.getTrackingCode()));
        shipment.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(shipment));
    }

    /** Chuyển trạng thái vận đơn theo state machine và đồng bộ Order khi phù hợp. */
    @Transactional
    public ShipmentDto changeStatus(UUID id, String targetStatus, UUID changedBy, String note) {
        Shipment shipment = repository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Vận đơn không tồn tại"));
        String from = normalize(shipment.getShippingStatus());
        String target = normalize(targetStatus);
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(target))
            throw BusinessException.invalidState("Chuyển trạng thái vận đơn không hợp lệ: " + from + " → " + target);
        shipment.setShippingStatus(target);
        shipment.setUpdatedAt(LocalDateTime.now());
        if ("SHIPPING".equals(target)) {
            shipment.setShippedAt(LocalDateTime.now());
            orderService.changeStatus(shipment.getOrderId(), "SHIPPING", changedBy, note);
        } else if ("DELIVERED".equals(target)) {
            shipment.setDeliveredAt(LocalDateTime.now());
            orderService.changeStatus(shipment.getOrderId(), "DELIVERED", changedBy, note);
        } else if ("RETURNED".equals(target)) {
            orderService.changeStatus(shipment.getOrderId(), "RETURNED", changedBy, note);
        }
        return mapper.toDto(repository.save(shipment));
    }

    /** Xóa vận đơn khi vẫn đang chờ giao. */
    @Transactional
    public void delete(UUID id) {
        Shipment shipment = repository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Vận đơn không tồn tại"));
        if (!"PENDING".equals(shipment.getShippingStatus()))
            throw BusinessException.invalidState("Chỉ được xóa vận đơn đang chờ giao");
        repository.delete(shipment);
    }

    private Shipment requireShipment(UUID id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("Vận đơn không tồn tại"));
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp vận đơn không hợp lệ");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
