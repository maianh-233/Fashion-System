package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.*;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("GOODS_RECEIPT")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GoodsReceiptService {
    private final GoodsReceiptRepository repository;
    private final GoodsReceiptItemRepository itemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final GoodsReceiptMapper mapper;
    private final GoodsReceiptItemMapper itemMapper;
    private final InventoryService inventoryService;
    private final StoreAccessService storeAccessService;
    private static final Set<String> SORT_FIELDS = Set.of("id", "receiptCode", "storeId", "receiptDate", "status", "totalQuantity", "createdAt", "updatedAt");
    private static final Set<String> STATUSES = Set.of("DRAFT", "PENDING_CONFIRMATION", "CONFIRMED", "COMPLETED", "CANCELLED");

    @Transactional
    public GoodsReceiptDto create(UUID actorId, GoodsReceiptRequest request) {
        if (request.getStoreId() == null) throw BusinessException.badRequest("Store is required");
        UUID storeId = storeAccessService.require(actorId, request.getStoreId(), "IMPORT_RECEIPT_CREATE");
        validateHeader(request);
        var document = new GoodsReceipt();
        document.setReceiptCode("IMP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().replace("-", ""));
        document.setStoreId(storeId);
        document.setReceivedBy(actorId);
        document.setStatus("DRAFT");
        document.setCreatedAt(LocalDateTime.now());
        document.setReceiptDate(document.getCreatedAt());
        document.setTotalQuantity(0);
        document.setTotalAmount(BigDecimal.ZERO);
        applyHeader(document, request);
        return mapper.toDto(repository.save(document));
    }

    public GoodsReceiptDto getById(UUID actorId, UUID id) {
        return mapper.toDto(requireDocument(actorId, id, "VIEW", false));
    }

    public Page<GoodsReceiptDto> getList(UUID actorId, String keyword, UUID requestedStoreId, UUID supplierId, String status,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        if (requestedStoreId == null) throw BusinessException.badRequest("Store is required");
        UUID storeId = storeAccessService.require(actorId, requestedStoreId, "IMPORT_RECEIPT_VIEW");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) throw BusinessException.badRequest("Invalid date range");
        if (pageable.getPageSize() > 100) throw BusinessException.badRequest("Page size cannot exceed 100");
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty()))) throw BusinessException.badRequest("Invalid sort field");
        String normalizedStatus = normalize(status);
        if (!normalizedStatus.isEmpty() && !STATUSES.contains(normalizedStatus)) throw BusinessException.badRequest("Invalid receipt status");
        if (LocalDate.MAX.equals(toDate)) throw BusinessException.badRequest("Invalid date range");
        LocalDateTime fromInclusive = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        return repository.search(keyword == null ? "" : keyword.trim(), storeId, supplierId, normalizedStatus,
                fromInclusive, toExclusive, pageable).map(mapper::toDto);
    }

    @Transactional
    public GoodsReceiptDto update(UUID actorId, UUID id, GoodsReceiptRequest request) {
        var document = requireEditable(actorId, id);
        storeAccessService.require(actorId, request.getStoreId(), "IMPORT_RECEIPT_UPDATE");
        if (!document.getStoreId().equals(request.getStoreId())) throw BusinessException.badRequest("Receipt store is immutable");
        validateHeader(request);
        applyHeader(document, request);
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public void delete(UUID actorId, UUID id) { cancel(actorId, id); }

    @Transactional
    public GoodsReceiptDto cancel(UUID actorId, UUID id) {
        var document = requireDocument(actorId, id, "CANCEL", true);
        if (!Set.of("DRAFT", "PENDING_CONFIRMATION").contains(document.getStatus())) throw BusinessException.invalidState("Only draft or pending receipts may be cancelled");
        document.setStatus("CANCELLED");
        document.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public GoodsReceiptDto submit(UUID actorId, UUID id) {
        var document = requireEditable(actorId, id);
        requireStatus(document, "DRAFT");
        validateDocument(document);
        document.setStatus("PENDING_CONFIRMATION");
        document.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public GoodsReceiptDto approve(UUID actorId, UUID id) { return confirm(actorId, id); }

    @Transactional
    public GoodsReceiptDto confirm(UUID actorId, UUID id) {
        var document = requireDocument(actorId, id, "CONFIRM", true);
        requireStatus(document, "PENDING_CONFIRMATION");
        validateDocument(document);
        document.setStatus("CONFIRMED");
        document.setApprovedBy(actorId);
        document.setConfirmedAt(LocalDateTime.now());
        document.setUpdatedAt(document.getConfirmedAt());
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public GoodsReceiptDto complete(UUID actorId, UUID id) {
        // The document lock serializes completion and every item/header edit.
        var document = requireDocument(actorId, id, "COMPLETE", true);
        requireStatus(document, "CONFIRMED");
        var items = validateDocument(document);
        // Stable variant ordering prevents overlapping multi-line receipts from locking stock in reverse order.
        items.stream().sorted(Comparator.comparing(GoodsReceiptItem::getProductVariantId)).forEach(item -> {
            inventoryService.receiveChannel(document.getStoreId(), item.getProductVariantId(), item.getQuantity(), item.getTargetChannel(), id, actorId);
        });
        document.setStatus("COMPLETED");
        document.setCompletedBy(actorId);
        document.setCompletedAt(LocalDateTime.now());
        document.setUpdatedAt(document.getCompletedAt());
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public GoodsReceiptItemDto addItem(UUID actorId, UUID id, GoodsReceiptItemRequest request) {
        var document = requireEditable(actorId, id);
        var item = new GoodsReceiptItem();
        item.setReceiptId(id);
        item.setCreatedAt(LocalDateTime.now());
        applyItem(item, request);
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(document);
        return itemMapper.toDto(item);
    }

    @Transactional
    public GoodsReceiptItemDto updateItem(UUID actorId, UUID id, UUID itemId, GoodsReceiptItemRequest request) {
        var document = requireEditable(actorId, id);
        var item = requireItem(id, itemId);
        applyItem(item, request);
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(document);
        return itemMapper.toDto(item);
    }

    @Transactional
    public void removeItem(UUID actorId, UUID id, UUID itemId) {
        var document = requireEditable(actorId, id);
        itemRepository.delete(requireItem(id, itemId));
        itemRepository.flush();
        recalculate(document);
    }

    public List<GoodsReceiptItemDto> getItems(UUID actorId, UUID id) {
        requireDocument(actorId, id, "VIEW", false);
        return itemRepository.findAllByReceiptIdOrderByCreatedAtAsc(id).stream().map(itemMapper::toDto).toList();
    }

    private GoodsReceipt requireDocument(UUID actorId, UUID id, String action, boolean lock) {
        var document = (lock ? repository.findByIdForUpdate(id) : repository.findById(id))
                .orElseThrow(() -> BusinessException.notFound("Receipt not found"));
        storeAccessService.require(actorId, document.getStoreId(), "IMPORT_RECEIPT_" + action);
        return document;
    }
    private GoodsReceipt requireEditable(UUID actorId, UUID id) {
        var document = requireDocument(actorId, id, "UPDATE", true);
        if (!Set.of("DRAFT", "PENDING_CONFIRMATION").contains(document.getStatus())) throw BusinessException.invalidState("Only unconfirmed receipts may be edited");
        return document;
    }
    private void requireStatus(GoodsReceipt document, String status) {
        if (!status.equals(document.getStatus())) throw BusinessException.invalidState("Expected receipt status " + status);
    }
    private GoodsReceiptItem requireItem(UUID id, UUID itemId) {
        return itemRepository.findByIdAndReceiptId(itemId, id).orElseThrow(() -> BusinessException.notFound("Receipt item not found"));
    }
    private void applyHeader(GoodsReceipt document, GoodsReceiptRequest request) {
        document.setSupplierId(request.getSupplierId());
        document.setNote(request.getNote());

        document.setUpdatedAt(LocalDateTime.now());
    }
    private void validateHeader(GoodsReceiptRequest request) {
        if (request.getSupplierId() == null) throw BusinessException.badRequest("Supplier is required");
        if (request.getSupplierId() != null && !supplierRepository.existsById(request.getSupplierId())) throw BusinessException.notFound("Supplier not found");
    }
    private void applyItem(GoodsReceiptItem item, GoodsReceiptItemRequest request) {
        validateValues(request.getProductId(), request.getProductVariantId(), request.getQuantity(), request.getTargetChannel(), request.getCostPrice());
        ProductVariant variant = variantRepository.findById(request.getProductVariantId()).orElseThrow(() -> BusinessException.notFound("Variant not found"));
        if (!request.getProductId().equals(variant.getProductId())) throw BusinessException.badRequest("Variant does not belong to product");
        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> BusinessException.notFound("Product not found"));
        item.setProductId(product.getId());
        item.setProductVariantId(variant.getId());
        item.setSku(variant.getSku());
        item.setProductName(product.getName());
        item.setQuantity(request.getQuantity());
        item.setTargetChannel(request.getTargetChannel());
        item.setCostPrice(request.getCostPrice());
        BigDecimal total = request.getCostPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        if (total.compareTo(new BigDecimal("999999999999.99")) > 0) throw BusinessException.badRequest("Item total exceeds supported range");
        item.setTotal(total);
    }
    private void validateValues(UUID product, UUID variant, Integer quantity, String channel, BigDecimal cost) {
        if (product == null || variant == null) throw BusinessException.badRequest("Product and variant are required");
        if (quantity == null || quantity <= 0) throw BusinessException.badRequest("Quantity must be positive");
        if (!"ONLINE".equals(channel) && !"OFFLINE".equals(channel)) throw BusinessException.badRequest("Invalid inventory channel");
        if (cost == null || cost.signum() < 0 || cost.scale() > 2 || cost.precision() - cost.scale() > 10) throw BusinessException.badRequest("Invalid cost price");
    }
    private List<GoodsReceiptItem> validateDocument(GoodsReceipt document) {
        var header = GoodsReceiptRequest.builder().storeId(document.getStoreId()).supplierId(document.getSupplierId()).note(document.getNote()).build();
        validateHeader(header);
        var items = itemRepository.findAllByReceiptIdOrderByCreatedAtAsc(document.getId());
        if (items.isEmpty()) throw BusinessException.invalidState("Receipt must contain at least one item");
        for (var item : items) {
            validateValues(item.getProductId(), item.getProductVariantId(), item.getQuantity(), item.getTargetChannel(), item.getCostPrice());
            var variant = variantRepository.findById(item.getProductVariantId()).orElseThrow(() -> BusinessException.notFound("Variant not found"));
            if (!item.getProductId().equals(variant.getProductId())) throw BusinessException.badRequest("Variant does not belong to product");

        }
        return items;
    }
    private void recalculate(GoodsReceipt document) {
        var items = itemRepository.findAllByReceiptIdOrderByCreatedAtAsc(document.getId());
        long quantity = items.stream().mapToLong(item -> item.getQuantity()).sum();
        if (quantity > Integer.MAX_VALUE) throw BusinessException.badRequest("Receipt quantity exceeds supported range");
        document.setTotalQuantity((int) quantity);
        BigDecimal total = items.stream().map(GoodsReceiptItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("999999999999.99")) > 0) throw BusinessException.badRequest("Receipt total exceeds supported range");
        document.setTotalAmount(total);
        document.setUpdatedAt(LocalDateTime.now());
        repository.save(document);
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
