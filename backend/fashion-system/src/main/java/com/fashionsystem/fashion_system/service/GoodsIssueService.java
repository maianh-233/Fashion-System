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
@com.fashionsystem.fashion_system.audit.BusinessAudit("GOODS_ISSUE")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GoodsIssueService {
    private final GoodsIssueRepository repository;
    private final GoodsIssueItemRepository itemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final GoodsIssueMapper mapper;
    private final GoodsIssueItemMapper itemMapper;
    private final InventoryService inventoryService;
    private final StoreAccessService storeAccessService;
    private static final Set<String> SORT_FIELDS = Set.of("id", "issueCode", "storeId", "issueDate", "status", "totalQuantity", "createdAt", "updatedAt");

    @Transactional
    public GoodsIssueDto create(UUID actorId, GoodsIssueRequest request) {
        if (request.getStoreId() == null) throw BusinessException.badRequest("Store is required");
        UUID storeId = storeAccessService.require(actorId, request.getStoreId(), "EXPORT_RECEIPT_CREATE");
        validateHeader(request);
        var document = new GoodsIssue();
        document.setIssueCode("EXP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().replace("-", ""));
        document.setStoreId(storeId);
        document.setIssuedBy(actorId);
        document.setStatus("DRAFT");
        document.setCreatedAt(LocalDateTime.now());
        document.setIssueDate(document.getCreatedAt());
        document.setTotalQuantity(0);

        applyHeader(document, request);
        return mapper.toDto(repository.save(document));
    }

    public GoodsIssueDto getById(UUID actorId, UUID id) {
        return mapper.toDto(requireDocument(actorId, id, "VIEW", false));
    }

    public Page<GoodsIssueDto> getList(UUID actorId, String keyword, UUID requestedStoreId, UUID orderId, String issueType, String status,
            LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        if (requestedStoreId == null) throw BusinessException.badRequest("Store is required");
        UUID storeId = storeAccessService.require(actorId, requestedStoreId, "EXPORT_RECEIPT_VIEW");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) throw BusinessException.badRequest("Invalid date range");
        if (pageable.getPageSize() > 100) throw BusinessException.badRequest("Page size cannot exceed 100");
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty()))) throw BusinessException.badRequest("Invalid sort field");
        return repository.search(keyword == null ? "" : keyword.trim(), storeId, orderId, normalize(issueType), normalize(status), fromDate, toDate, pageable).map(mapper::toDto);
    }

    @Transactional
    public GoodsIssueDto update(UUID actorId, UUID id, GoodsIssueRequest request) {
        var document = requireEditable(actorId, id);
        storeAccessService.require(actorId, request.getStoreId(), "EXPORT_RECEIPT_UPDATE");
        if (!document.getStoreId().equals(request.getStoreId())) throw BusinessException.badRequest("Receipt store is immutable");
        validateHeader(request);
        applyHeader(document, request);
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public void delete(UUID actorId, UUID id) { cancel(actorId, id); }

    @Transactional
    public GoodsIssueDto cancel(UUID actorId, UUID id) {
        var document = requireDocument(actorId, id, "CANCEL", true);
        if (!Set.of("DRAFT", "PENDING_CONFIRMATION").contains(document.getStatus())) throw BusinessException.invalidState("Only draft or pending receipts may be cancelled");
        document.setStatus("CANCELLED");
        document.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public GoodsIssueDto submit(UUID actorId, UUID id) {
        var document = requireEditable(actorId, id);
        requireStatus(document, "DRAFT");
        validateDocument(document);
        document.setStatus("PENDING_CONFIRMATION");
        document.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public GoodsIssueDto approve(UUID actorId, UUID id) { return confirm(actorId, id); }

    @Transactional
    public GoodsIssueDto confirm(UUID actorId, UUID id) {
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
    public GoodsIssueDto complete(UUID actorId, UUID id) {
        // The document lock serializes completion and every item/header edit.
        var document = requireDocument(actorId, id, "COMPLETE", true);
        requireStatus(document, "CONFIRMED");
        var items = validateDocument(document);
        // Stable variant ordering prevents overlapping multi-line receipts from locking stock in reverse order.
        items.stream().sorted(Comparator.comparing(GoodsIssueItem::getProductVariantId)).forEach(item -> {
            inventoryService.exportChannel(document.getStoreId(), item.getProductVariantId(), item.getQuantity(), item.getSourceChannel(), document.getIssueType(), id, actorId);
        });
        document.setStatus("COMPLETED");
        document.setCompletedBy(actorId);
        document.setCompletedAt(LocalDateTime.now());
        document.setUpdatedAt(document.getCompletedAt());
        return mapper.toDto(repository.save(document));
    }

    @Transactional
    public GoodsIssueItemDto addItem(UUID actorId, UUID id, GoodsIssueItemRequest request) {
        var document = requireEditable(actorId, id);
        var item = new GoodsIssueItem();
        item.setIssueId(id);
        item.setCreatedAt(LocalDateTime.now());
        validateTransferChannel(document.getIssueType(), request.getSourceChannel());
        applyItem(item, request);
        item = itemRepository.save(item);
        itemRepository.flush();
        recalculate(document);
        return itemMapper.toDto(item);
    }

    @Transactional
    public GoodsIssueItemDto updateItem(UUID actorId, UUID id, UUID itemId, GoodsIssueItemRequest request) {
        var document = requireEditable(actorId, id);
        var item = requireItem(id, itemId);
        validateTransferChannel(document.getIssueType(), request.getSourceChannel());
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

    public List<GoodsIssueItemDto> getItems(UUID actorId, UUID id) {
        requireDocument(actorId, id, "VIEW", false);
        return itemRepository.findAllByIssueIdOrderByCreatedAtAsc(id).stream().map(itemMapper::toDto).toList();
    }

    private GoodsIssue requireDocument(UUID actorId, UUID id, String action, boolean lock) {
        var document = (lock ? repository.findByIdForUpdate(id) : repository.findById(id))
                .orElseThrow(() -> BusinessException.notFound("Receipt not found"));
        storeAccessService.require(actorId, document.getStoreId(), "EXPORT_RECEIPT_" + action);
        return document;
    }
    private GoodsIssue requireEditable(UUID actorId, UUID id) {
        var document = requireDocument(actorId, id, "UPDATE", true);
        if (!Set.of("DRAFT", "PENDING_CONFIRMATION").contains(document.getStatus())) throw BusinessException.invalidState("Only unconfirmed receipts may be edited");
        return document;
    }
    private void requireStatus(GoodsIssue document, String status) {
        if (!status.equals(document.getStatus())) throw BusinessException.invalidState("Expected receipt status " + status);
    }
    private GoodsIssueItem requireItem(UUID id, UUID itemId) {
        return itemRepository.findByIdAndIssueId(itemId, id).orElseThrow(() -> BusinessException.notFound("Receipt item not found"));
    }
    private void applyHeader(GoodsIssue document, GoodsIssueRequest request) {
        document.setSupplierId(request.getSupplierId());
        document.setNote(request.getNote());
        document.setIssueType(normalize(request.getIssueType()));
        document.setReason(request.getReason());
        document.setUpdatedAt(LocalDateTime.now());
    }
    private void validateHeader(GoodsIssueRequest request) {
        if (!Set.of("ONLINE_TO_OFFLINE", "OFFLINE_TO_ONLINE", "DAMAGED", "RETURN_TO_SUPPLIER", "OTHER").contains(normalize(request.getIssueType()))) throw BusinessException.badRequest("Invalid issue type");
        if ("RETURN_TO_SUPPLIER".equals(normalize(request.getIssueType())) && request.getSupplierId() == null) throw BusinessException.badRequest("Supplier is required for return");
        if ("OTHER".equals(normalize(request.getIssueType())) && (request.getReason() == null || request.getReason().isBlank())) throw BusinessException.badRequest("Reason is required for OTHER");
        if (request.getSupplierId() != null && !supplierRepository.existsById(request.getSupplierId())) throw BusinessException.notFound("Supplier not found");
    }
    private void applyItem(GoodsIssueItem item, GoodsIssueItemRequest request) {
        validateValues(request.getProductId(), request.getProductVariantId(), request.getQuantity(), request.getSourceChannel());
        ProductVariant variant = variantRepository.findById(request.getProductVariantId()).orElseThrow(() -> BusinessException.notFound("Variant not found"));
        if (!request.getProductId().equals(variant.getProductId())) throw BusinessException.badRequest("Variant does not belong to product");
        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> BusinessException.notFound("Product not found"));
        item.setProductId(product.getId());
        item.setProductVariantId(variant.getId());
        item.setSku(variant.getSku());
        item.setProductName(product.getName());
        item.setQuantity(request.getQuantity());
        item.setSourceChannel(request.getSourceChannel());

    }
    private void validateValues(UUID product, UUID variant, Integer quantity, String channel) {
        if (product == null || variant == null) throw BusinessException.badRequest("Product and variant are required");
        if (quantity == null || quantity <= 0) throw BusinessException.badRequest("Quantity must be positive");
        if (!"ONLINE".equals(channel) && !"OFFLINE".equals(channel)) throw BusinessException.badRequest("Invalid inventory channel");

    }
    private List<GoodsIssueItem> validateDocument(GoodsIssue document) {
        var header = GoodsIssueRequest.builder().storeId(document.getStoreId()).supplierId(document.getSupplierId()).note(document.getNote()).issueType(document.getIssueType()).reason(document.getReason()).build();
        validateHeader(header);
        var items = itemRepository.findAllByIssueIdOrderByCreatedAtAsc(document.getId());
        if (items.isEmpty()) throw BusinessException.invalidState("Receipt must contain at least one item");
        for (var item : items) {
            validateValues(item.getProductId(), item.getProductVariantId(), item.getQuantity(), item.getSourceChannel());
            var variant = variantRepository.findById(item.getProductVariantId()).orElseThrow(() -> BusinessException.notFound("Variant not found"));
            if (!item.getProductId().equals(variant.getProductId())) throw BusinessException.badRequest("Variant does not belong to product");
            validateTransferChannel(document.getIssueType(), item.getSourceChannel());
        }
        return items;
    }
    private void validateTransferChannel(String type, String channel) {
        if (("ONLINE_TO_OFFLINE".equals(type) && !"ONLINE".equals(channel))
                || ("OFFLINE_TO_ONLINE".equals(type) && !"OFFLINE".equals(channel))) {
            throw BusinessException.badRequest("Transfer source channel does not match issue type");
        }
    }
    private void recalculate(GoodsIssue document) {
        var items = itemRepository.findAllByIssueIdOrderByCreatedAtAsc(document.getId());
        long quantity = items.stream().mapToLong(item -> item.getQuantity()).sum();
        if (quantity > Integer.MAX_VALUE) throw BusinessException.badRequest("Receipt quantity exceeds supported range");
        document.setTotalQuantity((int) quantity);

        document.setUpdatedAt(LocalDateTime.now());
        repository.save(document);
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
}
