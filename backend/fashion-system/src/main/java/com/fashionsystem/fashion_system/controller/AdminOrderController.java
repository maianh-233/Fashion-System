package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.OrderStatusHistoryDto;
import com.fashionsystem.fashion_system.dto.order.OfflineCartRequest;
import com.fashionsystem.fashion_system.dto.order.OrderActionRequests;
import com.fashionsystem.fashion_system.dto.order.OfflineOrderCreatedResponse;
import com.fashionsystem.fashion_system.dto.order.OfflineQuoteResponse;
import com.fashionsystem.fashion_system.dto.order.OrderAdminMetaResponse;
import com.fashionsystem.fashion_system.dto.order.OrderDetailResponse;
import com.fashionsystem.fashion_system.dto.order.OrderListRow;
import com.fashionsystem.fashion_system.dto.order.OrderSummaryResponse;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.dto.order.PosProductRow;
import com.fashionsystem.fashion_system.dto.order.PosVariantRow;
import com.fashionsystem.fashion_system.dto.order.VoucherOptionResponse;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.OfflineOrderService;
import com.fashionsystem.fashion_system.service.OrderAdminService;
import com.fashionsystem.fashion_system.service.OrderProcessingService;
import com.fashionsystem.fashion_system.service.StoreAccessService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** API quản trị đơn hàng. Nhân viên và cửa hàng hiệu lực luôn được xác định ở backend. */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AdminOrderController {
    private final OrderAdminService service;
    private final OfflineOrderService offlineService;
    private final OrderProcessingService processingService;
    private final com.fashionsystem.fashion_system.service.OrderShippingService shippingService;

    @GetMapping("/meta")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public OrderAdminMetaResponse meta(Authentication auth) {
        return service.meta(userId(auth));
    }

    /** Cửa hàng nhân viên được chọn trong module đơn hàng (Store chỉ có cửa hàng của mình). */
    @GetMapping("/stores")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public List<StoreAccessService.StoreOption> stores(Authentication auth) {
        return service.stores(userId(auth));
    }

    /** Danh sách đơn của một cửa hàng theo loại; mặc định không gồm đơn đã ẩn. */
    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public Page<OrderListRow> list(Authentication auth,
                                   @RequestParam(required = false) UUID storeId,
                                   @RequestParam OrderType type,
                                   @RequestParam(defaultValue = "false") boolean hidden,
                                   @RequestParam(required = false) OrderStatus status,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                   @RequestParam(defaultValue = "") String keyword,
                                   @RequestParam(required = false) UUID handlerId,
                                   @RequestParam(defaultValue = "") String paymentMethod,
                                   @PageableDefault(size = 10) Pageable pageable) {
        return service.list(userId(auth), new OrderAdminService.ListQuery(storeId, type, hidden, status, fromDate,
                toDate, keyword, handlerId, paymentMethod), pageable);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public OrderSummaryResponse summary(Authentication auth, @RequestParam(required = false) UUID storeId,
                                        @RequestParam OrderType type) {
        return service.summary(userId(auth), storeId, type);
    }

    @GetMapping("/handlers")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public List<OrderDetailResponse.Person> handlers(Authentication auth, @RequestParam(required = false) UUID storeId) {
        return service.handlers(userId(auth), storeId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public OrderDetailResponse detail(Authentication auth, @PathVariable UUID id) {
        return service.detail(userId(auth), id);
    }

    /** Ẩn đơn đã kết thúc (COMPLETED, CANCELLED, RETURNED). */
    @PostMapping("/{id}/hide")
    @PreAuthorize("hasAuthority('ORDER_HIDE')")
    public OrderDetailResponse hide(Authentication auth, @PathVariable UUID id) {
        return service.hide(userId(auth), id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('ORDER_RESTORE')")
    public OrderDetailResponse restore(Authentication auth, @PathVariable UUID id) {
        return service.restore(userId(auth), id);
    }

    /** Xác nhận đơn online (PENDING → CONFIRMED); người xác nhận thành nhân viên xử lý. */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('ORDER_CONFIRM')")
    public OrderDetailResponse confirm(Authentication auth, @PathVariable UUID id) {
        return processingService.confirm(userId(auth), id);
    }

    /** Hủy đơn (bắt buộc lý do, gửi cho khách); cộng lại tồn kho. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ORDER_CANCEL')")
    public OrderDetailResponse cancel(Authentication auth, @PathVariable UUID id,
                                      @Valid @RequestBody OrderActionRequests.Cancel request) {
        return processingService.cancel(userId(auth), id, request.reason());
    }

    /** Giao cho bên vận chuyển: tạo chuyến (cũng dùng để gửi lại yêu cầu giao sau khi chuyến trước thất bại). */
    @PostMapping("/{id}/handover")
    @PreAuthorize("hasAuthority('ORDER_HANDOVER_SHIPPING')")
    public OrderDetailResponse handover(Authentication auth, @PathVariable UUID id) {
        return shippingService.handover(userId(auth), id);
    }

    /** Hàng giao thất bại đã về cửa hàng: DELIVERY_FAILED → RETURNED, cộng lại tồn kho. */
    @PostMapping("/{id}/confirm-return")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public OrderDetailResponse confirmReturn(Authentication auth, @PathVariable UUID id) {
        return processingService.confirmReturn(userId(auth), id);
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ORDER_CONTACT_CUSTOMER')")
    public OrderActionRequests.ContactView contact(Authentication auth, @PathVariable UUID id,
                                                   @Valid @RequestBody OrderActionRequests.Contact request) {
        return processingService.contact(userId(auth), id, request);
    }

    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public List<OrderActionRequests.ContactView> contacts(Authentication auth, @PathVariable UUID id) {
        return processingService.contacts(userId(auth), id);
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public List<OrderStatusHistoryDto> statusHistory(Authentication auth, @PathVariable UUID id) {
        return service.statusHistory(userId(auth), id);
    }

    /** Bán tại quầy: tìm biến thể theo SKU/mã vạch (nhập tay hoặc máy quét dạng bàn phím). */
    @GetMapping("/offline/variants/by-code")
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public PosVariantRow variantByCode(Authentication auth, @RequestParam(required = false) UUID storeId,
                                       @RequestParam String code) {
        return offlineService.findByCode(userId(auth), storeId, code);
    }

    @GetMapping("/offline/products")
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public Page<PosProductRow> products(Authentication auth, @RequestParam(required = false) UUID storeId,
                                        @RequestParam(defaultValue = "") String keyword,
                                        @PageableDefault(size = 12) Pageable pageable) {
        return offlineService.searchProducts(userId(auth), storeId, keyword, pageable);
    }

    @GetMapping("/offline/products/{productId}/variants")
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public List<PosVariantRow> productVariants(Authentication auth, @RequestParam(required = false) UUID storeId,
                                               @PathVariable UUID productId) {
        return offlineService.productVariants(userId(auth), storeId, productId);
    }

    /** Voucher khả dụng cho giỏ hiện tại, đã sắp theo số tiền giảm thực tế. */
    @PostMapping("/offline/vouchers")
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public List<VoucherOptionResponse> vouchers(Authentication auth, @Valid @RequestBody OfflineCartRequest request) {
        return offlineService.vouchers(userId(auth), request);
    }

    /** Tính thử tiền giỏ hàng; frontend gọi mỗi khi giỏ/voucher/khách/tiền khách đưa thay đổi. */
    @PostMapping("/offline/quote")
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public OfflineQuoteResponse quote(Authentication auth, @Valid @RequestBody OfflineCartRequest request) {
        return offlineService.quote(userId(auth), request);
    }

    @PostMapping("/offline")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ORDER_CREATE')")
    public OfflineOrderCreatedResponse createOffline(Authentication auth, @Valid @RequestBody OfflineCartRequest request) {
        return offlineService.create(userId(auth), request);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
